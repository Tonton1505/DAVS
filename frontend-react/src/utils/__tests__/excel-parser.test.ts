import { describe, expect, it } from "vitest";
import {
  buildValidPdfBlob,
  buildValidXlsxBlob,
  parseDelimitedText,
  parseExcelFile,
} from "../excel-parser";

describe("excel-parser cross-platform & macOS compatibility", () => {
  it("round-trips generated OpenXML .xlsx files with Vietnamese headers and numbers", async () => {
    const blob = buildValidXlsxBlob(
      "Doanh thu Q4",
      ["Khu vực", "Doanh thu", "Tăng trưởng"],
      [
        ["Miền Bắc", 1250, 14.5],
        ["Miền Nam", 1890, 21.2],
      ],
    );
    const file = new File([blob], "bao_cao_macos.xlsx", {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });

    const parsed = await parseExcelFile(file);
    expect(parsed.sheets).toHaveLength(1);
    expect(parsed.sheets[0].isSampleFallback).toBeFalsy();
    expect(parsed.sheets[0].headers).toEqual(["Khu vực", "Doanh thu", "Tăng trưởng"]);
    expect(parsed.sheets[0].rows).toEqual([
      { "Khu vực": "Miền Bắc", "Doanh thu": 1250, "Tăng trưởng": 14.5 },
      { "Khu vực": "Miền Nam", "Doanh thu": 1890, "Tăng trưởng": 21.2 },
    ]);
    expect(parsed.sheets[0].dimensions).toEqual(["Khu vực"]);
    expect(parsed.sheets[0].measures).toEqual(["Doanh thu", "Tăng trưởng"]);
  });

  it("parses Classic Mac CR-delimited CSV and Vietnamese/European formatted numbers", () => {
    const macCsv = "Chi nhánh;Doanh thu;Tỷ lệ\rHà Nội;1.250.000;12,5\rTP.HCM;2.400.000;18,75";
    const sheet = parseDelimitedText(macCsv, "CSV Mac");
    expect(sheet).not.toBeNull();
    expect(sheet?.rows).toEqual([
      { "Chi nhánh": "Hà Nội", "Doanh thu": 1250000, "Tỷ lệ": 12.5 },
      { "Chi nhánh": "TP.HCM", "Doanh thu": 2400000, "Tỷ lệ": 18.75 },
    ]);
  });

  it("generates spec-compliant PDF 1.4 with xref table and EOF marker", async () => {
    const pdfBlob = buildValidPdfBlob("Báo cáo Q4", ["Tháng", "Doanh số"], [["T1", 120]]);
    const text = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result ?? ""));
      reader.onerror = () => reject(reader.error);
      reader.readAsText(pdfBlob);
    });
    expect(text.startsWith("%PDF-1.4\n")).toBe(true);
    expect(text).toContain("xref\n");
    expect(text.trimEnd().endsWith("%%EOF")).toBe(true);
  });
});
