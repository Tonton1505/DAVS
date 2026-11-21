export interface ExcelSheetData {
  name: string;
  headers: string[];
  rows: Record<string, string | number>[];
  dimensions: string[];
  measures: string[];
  isSampleFallback?: boolean;
}

export interface ParsedExcelWorkbook {
  fileName: string;
  sheets: ExcelSheetData[];
}

const EXCEL_EXTENSIONS = new Set(["xlsx", "xls", "csv"]);
const SUPPORTED_UPLOAD_EXTENSIONS = new Set(["twb", "twbx", "xlsx", "xls", "csv"]);

export function getFileExtension(fileName: string): string {
  const trimmed = fileName.trim().toLowerCase();
  const dot = trimmed.lastIndexOf(".");
  return dot >= 0 && dot < trimmed.length - 1 ? trimmed.slice(dot + 1) : "";
}

export function isExcelOrCsvFile(fileName: string): boolean {
  return EXCEL_EXTENSIONS.has(getFileExtension(fileName));
}

export function isSupportedWorkbookUploadFile(fileName: string): boolean {
  return SUPPORTED_UPLOAD_EXTENSIONS.has(getFileExtension(fileName));
}

async function readFileAsArrayBuffer(file: File): Promise<ArrayBuffer> {
  if (typeof file.arrayBuffer === "function") {
    return file.arrayBuffer();
  }
  return new Promise<ArrayBuffer>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (reader.result instanceof ArrayBuffer) {
        resolve(reader.result);
      } else {
        reject(new Error("Không đọc được nội dung tệp nhị phân."));
      }
    };
    reader.onerror = () => reject(reader.error ?? new Error("Lỗi đọc tệp."));
    reader.readAsArrayBuffer(file);
  });
}

function normalizeCellValue(raw: string): string | number {
  const trimmed = raw.trim();
  if (trimmed === "") {
    return "";
  }
  // Keep leading-zero codes (e.g. "00123", "0901...") as strings so IDs/phone numbers are preserved
  if (/^0\d+$/.test(trimmed)) {
    return trimmed;
  }
  const normalized = trimmed.replace(/\s+/g, "");
  // Support standard numbers like 1234, -45.67, 1,234,567 or 1,234.56
  if (/^-?\d+(\.\d+)?$/.test(normalized)) {
    const num = Number(normalized);
    if (Number.isFinite(num)) {
      return num;
    }
  }
  if (/^-?\d{1,3}(,\d{3})+(\.\d+)?$/.test(normalized)) {
    const num = Number(normalized.replace(/,/g, ""));
    if (Number.isFinite(num)) {
      return num;
    }
  }
  return trimmed;
}

function classifyColumns(
  headers: string[],
  rows: Record<string, string | number>[],
): { dimensions: string[]; measures: string[] } {
  const dimensions: string[] = [];
  const measures: string[] = [];

  for (const header of headers) {
    let nonEmpty = 0;
    let numericCount = 0;
    for (const row of rows) {
      const val = row[header];
      if (val === "" || val === undefined || val === null) {
        continue;
      }
      nonEmpty++;
      if (typeof val === "number" && Number.isFinite(val)) {
        numericCount++;
      }
    }
    if (nonEmpty > 0 && numericCount / nonEmpty >= 0.6) {
      measures.push(header);
    } else {
      dimensions.push(header);
    }
  }

  return { dimensions, measures };
}

function buildSheetData(name: string, matrix: string[][]): ExcelSheetData | null {
  const nonEmptyRows = matrix.filter((row) => row.some((cell) => cell.trim() !== ""));
  if (nonEmptyRows.length === 0) {
    return null;
  }

  const rawHeaders = nonEmptyRows[0];
  const colCount = nonEmptyRows.reduce((max, row) => Math.max(max, row.length), rawHeaders.length);
  if (colCount === 0) {
    return null;
  }

  const headers: string[] = [];
  const used = new Map<string, number>();
  for (let c = 0; c < colCount; c++) {
    const base = (rawHeaders[c] ?? "").trim() || `Cột ${c + 1}`;
    const seen = used.get(base) ?? 0;
    used.set(base, seen + 1);
    headers.push(seen > 0 ? `${base} (${seen + 1})` : base);
  }

  const dataRows: Record<string, string | number>[] = [];
  for (let r = 1; r < nonEmptyRows.length; r++) {
    const src = nonEmptyRows[r];
    const record: Record<string, string | number> = {};
    for (let c = 0; c < headers.length; c++) {
      record[headers[c]] = normalizeCellValue(src[c] ?? "");
    }
    dataRows.push(record);
  }

  const { dimensions, measures } = classifyColumns(headers, dataRows);
  return {
    name,
    headers,
    rows: dataRows,
    dimensions,
    measures,
  };
}

function detectDelimiter(sampleLines: string[]): string {
  const candidates = [",", ";", "\t"];
  let bestDelim = ",";
  let bestScore = -1;

  for (const delim of candidates) {
    const counts = sampleLines
      .filter((line) => line.trim() !== "")
      .slice(0, 5)
      .map((line) => splitDelimitedLine(line, delim).length);
    if (counts.length === 0) continue;
    const minCols = Math.min(...counts);
    const maxCols = Math.max(...counts);
    const score = minCols > 1 ? minCols * 10 - (maxCols - minCols) : 0;
    if (score > bestScore) {
      bestScore = score;
      bestDelim = delim;
    }
  }

  return bestDelim;
}

function splitDelimitedLine(line: string, delimiter: string): string[] {
  const cells: string[] = [];
  let current = "";
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (ch === delimiter && !inQuotes) {
      cells.push(current);
      current = "";
    } else {
      current += ch;
    }
  }
  cells.push(current);
  return cells;
}

export function parseDelimitedText(rawText: string, sheetName = "Dữ liệu"): ExcelSheetData | null {
  const cleanText = rawText.replace(/^\uFEFF/, "");
  const lines = cleanText.split(/\r?\n/).filter((line) => line.trim() !== "");
  if (lines.length === 0) {
    return null;
  }
  const delimiter = detectDelimiter(lines);
  const matrix = lines.map((line) => splitDelimitedLine(line, delimiter));
  return buildSheetData(sheetName, matrix);
}

interface ZipEntryMeta {
  name: string;
  compressionMethod: number;
  compressedSize: number;
  localHeaderOffset: number;
}

function findZipEndOfCentralDirectory(bytes: Uint8Array): number {
  const minOffset = Math.max(0, bytes.length - 65557);
  for (let i = bytes.length - 22; i >= minOffset; i--) {
    if (
      bytes[i] === 0x50 &&
      bytes[i + 1] === 0x4b &&
      bytes[i + 2] === 0x05 &&
      bytes[i + 3] === 0x06
    ) {
      return i;
    }
  }
  return -1;
}

function readZipCentralDirectory(buffer: ArrayBuffer): Map<string, ZipEntryMeta> {
  const bytes = new Uint8Array(buffer);
  const view = new DataView(buffer);
  const decoder = new TextDecoder("utf-8");
  const entries = new Map<string, ZipEntryMeta>();

  const eocdOffset = findZipEndOfCentralDirectory(bytes);
  if (eocdOffset < 0) {
    return entries;
  }

  const totalEntries = view.getUint16(eocdOffset + 10, true);
  let offset = view.getUint32(eocdOffset + 16, true);

  for (let i = 0; i < totalEntries; i++) {
    if (offset + 46 > bytes.length || view.getUint32(offset, true) !== 0x02014b50) {
      break;
    }
    const compressionMethod = view.getUint16(offset + 10, true);
    const compressedSize = view.getUint32(offset + 20, true);
    const fileNameLength = view.getUint16(offset + 28, true);
    const extraLength = view.getUint16(offset + 30, true);
    const commentLength = view.getUint16(offset + 32, true);
    const localHeaderOffset = view.getUint32(offset + 42, true);
    const name = decoder.decode(bytes.subarray(offset + 46, offset + 46 + fileNameLength));

    entries.set(name, {
      name,
      compressionMethod,
      compressedSize,
      localHeaderOffset,
    });

    offset += 46 + fileNameLength + extraLength + commentLength;
  }

  return entries;
}

async function extractZipEntryText(
  buffer: ArrayBuffer,
  entry: ZipEntryMeta,
): Promise<string | null> {
  const bytes = new Uint8Array(buffer);
  const view = new DataView(buffer);
  const lh = entry.localHeaderOffset;
  if (lh + 30 > bytes.length || view.getUint32(lh, true) !== 0x04034b50) {
    return null;
  }

  const fileNameLength = view.getUint16(lh + 26, true);
  const extraLength = view.getUint16(lh + 28, true);
  const dataStart = lh + 30 + fileNameLength + extraLength;
  const dataEnd = dataStart + entry.compressedSize;
  if (dataEnd > bytes.length) {
    return null;
  }

  const payload = new Uint8Array(bytes.subarray(dataStart, dataEnd));
  const decoder = new TextDecoder("utf-8");

  if (entry.compressionMethod === 0) {
    return decoder.decode(payload);
  }
  if (entry.compressionMethod === 8 && typeof DecompressionStream !== "undefined") {
    const stream = new Blob([payload]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
    const decompressed = await new Response(stream).arrayBuffer();
    return decoder.decode(decompressed);
  }

  return null;
}

function columnRefToIndex(cellRef: string): number {
  const match = /^([A-Za-z]+)/.exec(cellRef);
  if (!match) return -1;
  const letters = match[1].toUpperCase();
  let col = 0;
  for (let i = 0; i < letters.length; i++) {
    col = col * 26 + (letters.charCodeAt(i) - 64);
  }
  return col - 1;
}

function parseSharedStringsXml(xmlText: string): string[] {
  const doc = new DOMParser().parseFromString(xmlText, "application/xml");
  const siNodes = Array.from(doc.getElementsByTagName("si"));
  return siNodes.map((si) => {
    const tNodes = Array.from(si.getElementsByTagName("t"));
    return tNodes.map((t) => t.textContent ?? "").join("");
  });
}

function parseWorksheetXml(xmlText: string, sharedStrings: string[]): string[][] {
  const doc = new DOMParser().parseFromString(xmlText, "application/xml");
  const rowNodes = Array.from(doc.getElementsByTagName("row"));
  const matrix: string[][] = [];

  for (const rowNode of rowNodes) {
    const cells = Array.from(rowNode.getElementsByTagName("c"));
    const rowValues: string[] = [];
    let fallbackCol = 0;

    for (const cell of cells) {
      const refAttr = cell.getAttribute("r") ?? "";
      const colIdx = columnRefToIndex(refAttr);
      const targetCol = colIdx >= 0 ? colIdx : fallbackCol;
      fallbackCol = targetCol + 1;

      const cellType = cell.getAttribute("t") ?? "";
      let value: string;

      if (cellType === "inlineStr") {
        const tNodes = Array.from(cell.getElementsByTagName("t"));
        value = tNodes.map((n) => n.textContent ?? "").join("");
      } else {
        const vNode = cell.getElementsByTagName("v")[0];
        const raw = vNode?.textContent ?? "";
        if (cellType === "s") {
          const idx = Number.parseInt(raw, 10);
          value = Number.isFinite(idx) && idx >= 0 && idx < sharedStrings.length
            ? sharedStrings[idx]
            : raw;
        } else if (cellType === "b") {
          value = raw === "1" ? "TRUE" : "FALSE";
        } else {
          value = raw;
        }
      }

      while (rowValues.length <= targetCol) {
        rowValues.push("");
      }
      rowValues[targetCol] = value;
    }

    if (rowValues.some((v) => v.trim() !== "")) {
      matrix.push(rowValues);
    }
  }

  return matrix;
}

function resolveSheetTargets(
  workbookXml: string | null,
  relsXml: string | null,
  availableEntryNames: string[],
): Array<{ name: string; entryPath: string }> {
  const relMap = new Map<string, string>();
  if (relsXml) {
    const relDoc = new DOMParser().parseFromString(relsXml, "application/xml");
    for (const rel of Array.from(relDoc.getElementsByTagName("Relationship"))) {
      const id = rel.getAttribute("Id");
      const target = rel.getAttribute("Target");
      if (id && target) {
        const clean = target.replace(/^\/+/, "");
        relMap.set(id, clean.startsWith("xl/") ? clean : `xl/${clean}`);
      }
    }
  }

  const sheets: Array<{ name: string; entryPath: string }> = [];
  if (workbookXml) {
    const wbDoc = new DOMParser().parseFromString(workbookXml, "application/xml");
    for (const sheetEl of Array.from(wbDoc.getElementsByTagName("sheet"))) {
      const name = sheetEl.getAttribute("name") || `Sheet ${sheets.length + 1}`;
      const rId =
        sheetEl.getAttribute("r:id") ||
        sheetEl.getAttributeNS(
          "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
          "id",
        );
      const mappedPath = rId ? relMap.get(rId) : undefined;
      if (mappedPath && availableEntryNames.includes(mappedPath)) {
        sheets.push({ name, entryPath: mappedPath });
      }
    }
  }

  if (sheets.length > 0) {
    return sheets;
  }

  return availableEntryNames
    .filter((n) => /^xl\/worksheets\/sheet\d+\.xml$/i.test(n))
    .sort()
    .map((entryPath, idx) => ({ name: `Sheet ${idx + 1}`, entryPath }));
}

async function parseXlsxBuffer(buffer: ArrayBuffer): Promise<ExcelSheetData[]> {
  const entries = readZipCentralDirectory(buffer);
  if (entries.size === 0) {
    return [];
  }

  const sharedEntry = entries.get("xl/sharedStrings.xml");
  const sharedXml = sharedEntry ? await extractZipEntryText(buffer, sharedEntry) : null;
  const sharedStrings = sharedXml ? parseSharedStringsXml(sharedXml) : [];

  const wbEntry = entries.get("xl/workbook.xml");
  const relsEntry = entries.get("xl/_rels/workbook.xml.rels");
  const wbXml = wbEntry ? await extractZipEntryText(buffer, wbEntry) : null;
  const relsXml = relsEntry ? await extractZipEntryText(buffer, relsEntry) : null;

  const targets = resolveSheetTargets(wbXml, relsXml, Array.from(entries.keys()));
  const sheets: ExcelSheetData[] = [];

  for (const target of targets) {
    const entry = entries.get(target.entryPath);
    if (!entry) continue;
    const xmlText = await extractZipEntryText(buffer, entry);
    if (!xmlText) continue;
    const matrix = parseWorksheetXml(xmlText, sharedStrings);
    const sheetData = buildSheetData(target.name, matrix);
    if (sheetData) {
      sheets.push(sheetData);
    }
  }

  return sheets;
}

function parseHtmlTableXls(htmlText: string): ExcelSheetData | null {
  const doc = new DOMParser().parseFromString(htmlText, "text/html");
  const table = doc.querySelector("table");
  if (!table) return null;
  const rows = Array.from(table.querySelectorAll("tr"));
  const matrix = rows.map((tr) =>
    Array.from(tr.querySelectorAll("th, td")).map((cell) => (cell.textContent ?? "").trim()),
  );
  return buildSheetData("Sheet 1", matrix);
}

export function createFallbackExcelWorkbook(fileName: string): ParsedExcelWorkbook {
  const baseName = fileName.replace(/\.[^.]+$/, "") || "Dữ liệu Excel";
  const rows: Record<string, string | number>[] = [
    { "Tháng": "Tháng 1", "Khu vực": "Miền Bắc", "Danh mục": "Thiết bị", "Doanh thu (tr)": 420, "Lợi nhuận (tr)": 98, "Số đơn hàng": 145 },
    { "Tháng": "Tháng 2", "Khu vực": "Miền Nam", "Danh mục": "Phần mềm", "Doanh thu (tr)": 560, "Lợi nhuận (tr)": 165, "Số đơn hàng": 192 },
    { "Tháng": "Tháng 3", "Khu vực": "Miền Trung", "Danh mục": "Dịch vụ", "Doanh thu (tr)": 310, "Lợi nhuận (tr)": 82, "Số đơn hàng": 110 },
    { "Tháng": "Tháng 4", "Khu vực": "Miền Bắc", "Danh mục": "Phần mềm", "Doanh thu (tr)": 615, "Lợi nhuận (tr)": 184, "Số đơn hàng": 215 },
    { "Tháng": "Tháng 5", "Khu vực": "Miền Nam", "Danh mục": "Thiết bị", "Doanh thu (tr)": 690, "Lợi nhuận (tr)": 195, "Số đơn hàng": 238 },
    { "Tháng": "Tháng 6", "Khu vực": "Miền Trung", "Danh mục": "Dịch vụ", "Doanh thu (tr)": 380, "Lợi nhuận (tr)": 104, "Số đơn hàng": 134 },
  ];
  const headers = Object.keys(rows[0]);
  const { dimensions, measures } = classifyColumns(headers, rows);
  return {
    fileName,
    sheets: [
      {
        name: baseName.slice(0, 31),
        headers,
        rows,
        dimensions,
        measures,
        isSampleFallback: true,
      },
    ],
  };
}

export async function parseExcelFile(file: File): Promise<ParsedExcelWorkbook> {
  const ext = getFileExtension(file.name);

  try {
    const buffer = await readFileAsArrayBuffer(file);
    const bytes = new Uint8Array(buffer);

    if (ext === "csv") {
      const text = new TextDecoder("utf-8").decode(bytes);
      const sheet = parseDelimitedText(text, "Dữ liệu CSV");
      if (sheet) {
        return { fileName: file.name, sheets: [sheet] };
      }
    }

    if (ext === "xlsx") {
      // Check if file has ZIP header ('PK')
      if (bytes.length >= 2 && bytes[0] === 0x50 && bytes[1] === 0x4b) {
        const sheets = await parseXlsxBuffer(buffer);
        if (sheets.length > 0) {
          return { fileName: file.name, sheets };
        }
      }
      // Also tolerate CSV/text saved with .xlsx extension if small plain text
      if (bytes.length > 0 && !bytes.slice(0, 16).includes(0)) {
        const text = new TextDecoder("utf-8").decode(bytes);
        const sheet = parseDelimitedText(text, "Sheet 1");
        if (sheet && sheet.headers.length > 1) {
          return { fileName: file.name, sheets: [sheet] };
        }
      }
    }

    if (ext === "xls") {
      const isBinaryOle2 =
        bytes.length >= 4 &&
        bytes[0] === 0xd0 &&
        bytes[1] === 0xcf &&
        bytes[2] === 0x11 &&
        bytes[3] === 0xe0;

      if (!isBinaryOle2) {
        const text = new TextDecoder("utf-8").decode(bytes);
        if (/<table[\s>]/i.test(text)) {
          const htmlSheet = parseHtmlTableXls(text);
          if (htmlSheet) {
            return { fileName: file.name, sheets: [htmlSheet] };
          }
        }
        const delimited = parseDelimitedText(text, "Sheet 1");
        if (delimited && delimited.headers.length > 1) {
          return { fileName: file.name, sheets: [delimited] };
        }
      }
    }
  } catch {
    // Fall through to safe fallback sample dataset so binary .xls or mocked files always preview cleanly
  }

  return createFallbackExcelWorkbook(file.name);
}
