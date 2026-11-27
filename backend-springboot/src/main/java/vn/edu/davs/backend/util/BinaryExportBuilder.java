package vn.edu.davs.backend.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/**
 * Generates standards-compliant OpenXML (.xlsx), PDF 1.4 (.pdf), and PNG (.png) binary payloads
 * without native OS tools so exported files open cleanly in macOS Preview, Excel, and Numbers.
 */
public final class BinaryExportBuilder {

    private static final byte[] MINIMAL_VALID_PNG = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE,
        0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
        0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xFF, (byte) 0xFF,
        0x3F, 0x00, 0x05, (byte) 0xFE, 0x02, (byte) 0xFE,
        (byte) 0xDC, (byte) 0xCC, 0x59, (byte) 0xE7,
        0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
        (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    private BinaryExportBuilder() {
    }

    public static byte[] buildXlsx(
            String sheetTitle,
            List<String> headers,
            List<Map<String, Object>> rows,
            List<String> keys
    ) {
        String safeSheetName = sanitizeSheetName(sheetTitle);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            writeZipEntry(zos, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml"
                        ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml"
                        ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """.stripIndent());

            writeZipEntry(zos, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                        Target="xl/workbook.xml"/>
                    </Relationships>
                    """.stripIndent());

            writeZipEntry(zos, "xl/workbook.xml", ("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="%s" sheetId="1" r:id="rId1"/>
                      </sheets>
                    </workbook>
                    """).stripIndent().formatted(escapeXml(safeSheetName)));

            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                        Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """.stripIndent());

            StringBuilder sheetXml = new StringBuilder(2048);
            sheetXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            sheetXml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
            sheetXml.append("  <sheetData>\n");

            sheetXml.append("    <row r=\"1\">");
            for (int c = 0; c < headers.size(); c++) {
                String ref = colLetters(c) + "1";
                sheetXml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                        .append(escapeXml(headers.get(c)))
                        .append("</t></is></c>");
            }
            sheetXml.append("</row>\n");

            for (int r = 0; r < rows.size(); r++) {
                int excelRow = r + 2;
                Map<String, Object> row = rows.get(r);
                sheetXml.append("    <row r=\"").append(excelRow).append("\">");
                for (int c = 0; c < keys.size(); c++) {
                    String ref = colLetters(c) + excelRow;
                    Object val = row.get(keys.get(c));
                    if (val instanceof Number num) {
                        sheetXml.append("<c r=\"").append(ref).append("\"><v>")
                                .append(num)
                                .append("</v></c>");
                    } else {
                        String str = val != null ? String.valueOf(val) : "";
                        sheetXml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                                .append(escapeXml(str))
                                .append("</t></is></c>");
                    }
                }
                sheetXml.append("</row>\n");
            }

            sheetXml.append("  </sheetData>\n</worksheet>\n");
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml.toString());
            zos.finish();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build XLSX export", ex);
        }
    }

    public static byte[] buildPdf(
            String title,
            List<String> headers,
            List<Map<String, Object>> rows,
            List<String> keys
    ) {
        StringBuilder stream = new StringBuilder(1024);
        stream.append("BT\n/F2 16 Tf\n50 540 Td\n(")
                .append(escapePdfText("DAVS Report: " + toAsciiLabel(title)))
                .append(") Tj\n");

        stream.append("/F2 11 Tf\n0 -28 Td\n(")
                .append(escapePdfText(String.join("   |   ", headers.stream().map(BinaryExportBuilder::toAsciiLabel).toList())))
                .append(") Tj\n");

        stream.append("/F1 10 Tf\n");
        int maxLines = Math.min(rows.size(), 22);
        for (int i = 0; i < maxLines; i++) {
            Map<String, Object> row = rows.get(i);
            List<String> cells = new ArrayList<>(keys.size());
            for (String k : keys) {
                Object val = row.get(k);
                cells.add(toAsciiLabel(val != null ? String.valueOf(val) : ""));
            }
            stream.append("0 -18 Td\n(")
                    .append(escapePdfText(String.join("   |   ", cells)))
                    .append(") Tj\n");
        }
        stream.append("ET\n");
        byte[] contentBytes = stream.toString().getBytes(StandardCharsets.US_ASCII);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeAscii(out, "%PDF-1.4\n");
            List<Integer> offsets = new ArrayList<>();

            offsets.add(out.size());
            writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            offsets.add(out.size());
            writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            offsets.add(out.size());
            writeAscii(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] "
                    + "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>\nendobj\n");

            offsets.add(out.size());
            writeAscii(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            offsets.add(out.size());
            writeAscii(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

            offsets.add(out.size());
            writeAscii(out, "6 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
            out.write(contentBytes);
            writeAscii(out, "endstream\nendobj\n");

            int xrefOffset = out.size();
            writeAscii(out, "xref\n0 " + (offsets.size() + 1) + "\n");
            writeAscii(out, "0000000000 65535 f \n");
            for (int offset : offsets) {
                writeAscii(out, String.format("%010d 00000 n \n", offset));
            }
            writeAscii(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n");
            writeAscii(out, "startxref\n" + xrefOffset + "\n%%EOF\n");
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build PDF export", ex);
        }
    }

    public static byte[] buildPng(String title, List<Map<String, Object>> rows) {
        try {
            int width = 880;
            int height = 480;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);

                g.setColor(new Color(24, 24, 27));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                g.drawString("DAVS - " + (title != null ? title : "Dashboard"), 40, 42);

                int count = Math.min(rows.size(), 12);
                if (count > 0) {
                    double maxVal = 1.0;
                    for (int i = 0; i < count; i++) {
                        Object v = rows.get(i).get("value");
                        if (v instanceof Number n) {
                            maxVal = Math.max(maxVal, n.doubleValue());
                        }
                    }
                    int chartLeft = 50;
                    int chartBottom = 420;
                    int chartHeight = 320;
                    int slotWidth = (width - 100) / count;
                    int barWidth = Math.max(18, slotWidth - 16);

                    g.setColor(new Color(228, 228, 231));
                    g.drawLine(chartLeft, chartBottom, width - 40, chartBottom);

                    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                    for (int i = 0; i < count; i++) {
                        Map<String, Object> row = rows.get(i);
                        double val = row.get("value") instanceof Number n ? n.doubleValue() : 0.0;
                        int barH = (int) Math.round((val / maxVal) * chartHeight);
                        int x = chartLeft + i * slotWidth + 8;
                        int y = chartBottom - barH;

                        g.setColor(new Color(37, 99, 235));
                        g.fillRoundRect(x, y, barWidth, barH, 6, 6);

                        g.setColor(new Color(82, 82, 91));
                        String label = String.valueOf(row.getOrDefault("month", "M" + (i + 1)));
                        g.drawString(label, x + 4, chartBottom + 20);
                    }
                }
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (ImageIO.write(image, "png", baos) && baos.size() > 8) {
                return baos.toByteArray();
            }
        } catch (Throwable ignored) {
            // Fall back to minimal valid PNG below if headless AWT font subsystem is unavailable
        }
        return MINIMAL_VALID_PNG.clone();
    }

    private static void writeZipEntry(ZipOutputStream zos, String name, String xml) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(xml.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    private static String colLetters(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = (n / 26) - 1;
        }
        return sb.toString();
    }

    private static String sanitizeSheetName(String title) {
        if (title == null || title.isBlank()) {
            return "Data";
        }
        String cleaned = title.replaceAll("[\\\\/?*\\[\\]:]", "_").trim();
        if (cleaned.isEmpty()) {
            return "Data";
        }
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private static String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String toAsciiLabel(String input) {
        if (input == null) {
            return "";
        }
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^\\x20-\\x7E]", "");
    }

    private static String escapePdfText(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
