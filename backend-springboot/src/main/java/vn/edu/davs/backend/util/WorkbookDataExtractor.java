package vn.edu.davs.backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * Extracts real schema, sample rows, and chart aggregations from uploaded workbook files
 * (.xlsx, .csv, .tsv, .json, .twb, .twbx) so each uploaded file renders its own dashboard data.
 */
public final class WorkbookDataExtractor {

    private static final int MAX_PARSED_ROWS = 500;
    private static final int MAX_SAMPLE_ROWS = 20;
    private static final int MAX_CHART_ITEMS = 12;
    private static final int MAX_SHARED_STRINGS = 20_000;
    private static final int MAX_JSON_PARSE_BYTES = 5 * 1024 * 1024;

    private WorkbookDataExtractor() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String buildDefinitionJson(
            String fileName,
            byte[] fileBytes,
            long sizeBytes,
            ObjectMapper objectMapper
    ) {
        String ext = FileUtil.getFileExtension(fileName);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("fileName", fileName);
        root.put("extension", ext);
        root.put("sizeBytes", sizeBytes);

        try {
            switch (ext) {
                case "csv", "tsv" -> populateTabular(
                        root,
                        parseDelimited(fileBytes, "tsv".equals(ext) ? '\t' : ',')
                );
                case "xlsx" -> populateTabular(root, parseXlsx(fileBytes));
                case "json" -> populateTabular(root, parseJsonArray(fileBytes, objectMapper));
                case "twb" -> populateTableau(root, parseTwbXml(new ByteArrayInputStream(fileBytes)));
                case "twbx" -> populateTableau(root, parseTwbx(fileBytes));
                default -> root.put("previewType", "FILE");
            }
        } catch (Exception ex) {
            root.put("previewType", "FILE");
            root.put("parseWarning", ex.getClass().getSimpleName());
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            return "{\"fileName\":\"" + fileName.replace("\"", "") + "\",\"previewType\":\"FILE\"}";
        }
    }

    private static void populateTabular(Map<String, Object> root, TabularData data) {
        if (data == null || data.columns.isEmpty()) {
            root.put("previewType", "FILE");
            return;
        }
        root.put("previewType", "TABULAR");
        root.put("columns", data.columns);
        root.put("totalRows", data.totalRows);
        root.put("sampleRows", data.rows.subList(0, Math.min(MAX_SAMPLE_ROWS, data.rows.size())));
        buildChartSeries(root, data);
    }

    private static void populateTableau(Map<String, Object> root, TableauMeta meta) {
        root.put("previewType", "TABLEAU");
        root.put("sheets", meta.sheets);
        root.put("dashboards", meta.dashboards);
        root.put("datasources", meta.datasources);
    }

    private static void buildChartSeries(Map<String, Object> root, TabularData data) {
        String labelCol = null;
        String secondaryLabelCol = null;
        String metricCol = null;

        for (String col : data.columns) {
            boolean isNumeric = isColumnNumeric(data.rows, col);
            if (isNumeric && metricCol == null) {
                metricCol = col;
            } else if (!isNumeric) {
                if (labelCol == null) {
                    labelCol = col;
                } else if (secondaryLabelCol == null) {
                    secondaryLabelCol = col;
                }
            }
        }
        if (labelCol == null) {
            labelCol = data.columns.get(0);
        }

        root.put("labelColumn", labelCol);
        root.put("metricColumn", metricCol != null ? metricCol : "Số dòng");
        root.put("primarySeries", aggregateSeries(data.rows, labelCol, metricCol));

        if (secondaryLabelCol != null) {
            root.put("secondaryLabelColumn", secondaryLabelCol);
            root.put("secondarySeries", aggregateSeries(data.rows, secondaryLabelCol, metricCol));
        } else {
            root.put("secondarySeries", Collections.emptyList());
        }
    }

    private static boolean isColumnNumeric(List<Map<String, Object>> rows, String col) {
        int checked = 0;
        int numeric = 0;
        for (Map<String, Object> row : rows) {
            Object val = row.get(col);
            if (val == null || val.toString().isBlank()) {
                continue;
            }
            checked++;
            if (parseDoubleOrNull(val.toString()) != null) {
                numeric++;
            }
            if (checked >= 20) {
                break;
            }
        }
        return checked > 0 && numeric * 2 >= checked;
    }

    private static List<Map<String, Object>> aggregateSeries(
            List<Map<String, Object>> rows,
            String groupCol,
            String metricCol
    ) {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object rawKey = row.get(groupCol);
            String key = rawKey != null && !rawKey.toString().isBlank() ? rawKey.toString().trim() : "(Trống)";
            double inc = 1.0;
            if (metricCol != null) {
                Object rawVal = row.get(metricCol);
                Double parsed = rawVal != null ? parseDoubleOrNull(rawVal.toString()) : null;
                inc = parsed != null ? parsed : 0.0;
            }
            totals.merge(key, inc, Double::sum);
        }
        List<Map<String, Object>> series = new ArrayList<>();
        totals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(MAX_CHART_ITEMS)
                .forEach(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", Math.round(entry.getValue() * 100.0) / 100.0);
                    series.add(item);
                });
        return series;
    }

    private static Double parseDoubleOrNull(String raw) {
        try {
            String normalized = raw.trim().replace(",", "");
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static TabularData parseDelimited(byte[] bytes, char delimiter) throws Exception {
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        int totalRows = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new TabularData(columns, rows, 0);
            }
            headerLine = headerLine.replace("\uFEFF", "");
            List<String> headers = splitCsvLine(headerLine, delimiter);
            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i).trim();
                columns.add(h.isEmpty() ? "Col_" + (i + 1) : h);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                totalRows++;
                if (rows.size() < MAX_PARSED_ROWS) {
                    List<String> vals = splitCsvLine(line, delimiter);
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 0; i < columns.size(); i++) {
                        row.put(columns.get(i), i < vals.size() ? vals.get(i).trim() : "");
                    }
                    rows.add(row);
                }
            }
        }
        return new TabularData(columns, rows, totalRows);
    }

    private static List<String> splitCsvLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == delimiter && !inQuotes) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        tokens.add(current.toString());
        return tokens;
    }

    private static TabularData parseJsonArray(byte[] bytes, ObjectMapper mapper) throws Exception {
        if (bytes.length > MAX_JSON_PARSE_BYTES) {
            return null;
        }
        JsonNode root = mapper.readTree(bytes);
        if (!root.isArray() || root.isEmpty()) {
            return null;
        }
        List<String> columns = new ArrayList<>();
        JsonNode first = root.get(0);
        if (!first.isObject()) {
            return null;
        }
        first.fieldNames().forEachRemaining(columns::add);
        List<Map<String, Object>> rows = new ArrayList<>();
        int limit = Math.min(root.size(), MAX_PARSED_ROWS);
        for (int i = 0; i < limit; i++) {
            JsonNode item = root.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            for (String col : columns) {
                JsonNode cell = item.get(col);
                row.put(col, cell == null || cell.isNull() ? "" : cell.asText());
            }
            rows.add(row);
        }
        return new TabularData(columns, rows, root.size());
    }

    private static TabularData parseXlsx(byte[] bytes) throws Exception {
        List<String> sharedStrings = Collections.emptyList();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase(Locale.ROOT);
                if ("xl/sharedstrings.xml".equals(name)) {
                    sharedStrings = parseSharedStrings(zis);
                    break;
                }
            }
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase(Locale.ROOT);
                if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                    return parseXlsxSheet(zis, sharedStrings);
                }
            }
        }
        return null;
    }

    private static List<String> parseSharedStrings(InputStream is) throws Exception {
        List<String> strings = new ArrayList<>();
        XMLInputFactory factory = createSafeXmlFactory();
        XMLStreamReader reader = factory.createXMLStreamReader(is);
        StringBuilder current = null;
        try {
            while (reader.hasNext() && strings.size() < MAX_SHARED_STRINGS) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("si".equals(local)) {
                        current = new StringBuilder();
                    } else if ("t".equals(local) && current != null) {
                        current.append(reader.getElementText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT && "si".equals(reader.getLocalName())) {
                    strings.add(current != null ? current.toString() : "");
                    current = null;
                }
            }
        } finally {
            reader.close();
        }
        return strings;
    }

    private static TabularData parseXlsxSheet(InputStream is, List<String> sharedStrings) throws Exception {
        XMLInputFactory factory = createSafeXmlFactory();
        XMLStreamReader reader = factory.createXMLStreamReader(is);
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> currentRow = null;
        String currentType = null;
        int currentColIdx = -1;
        int totalRows = 0;

        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("row".equals(local)) {
                        currentRow = new ArrayList<>();
                        currentColIdx = -1;
                    } else if ("c".equals(local) && currentRow != null) {
                        currentType = reader.getAttributeValue(null, "t");
                        String ref = reader.getAttributeValue(null, "r");
                        int targetIdx = ref != null ? columnIndexFromRef(ref) : currentColIdx + 1;
                        while (currentRow.size() <= targetIdx) {
                            currentRow.add("");
                        }
                        currentColIdx = targetIdx;
                    } else if ("v".equals(local) && currentRow != null && currentColIdx >= 0) {
                        String rawVal = reader.getElementText();
                        if ("s".equals(currentType)) {
                            int idx = Integer.parseInt(rawVal);
                            rawVal = idx >= 0 && idx < sharedStrings.size() ? sharedStrings.get(idx) : "";
                        }
                        currentRow.set(currentColIdx, rawVal);
                    } else if ("is".equals(local) && currentRow != null && currentColIdx >= 0) {
                        String inlineVal = readInlineString(reader);
                        currentRow.set(currentColIdx, inlineVal);
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT && "row".equals(reader.getLocalName())) {
                    if (currentRow != null && !currentRow.isEmpty()) {
                        if (columns.isEmpty()) {
                            for (int i = 0; i < currentRow.size(); i++) {
                                String h = currentRow.get(i).trim();
                                columns.add(h.isEmpty() ? "Col_" + (i + 1) : h);
                            }
                        } else {
                            totalRows++;
                            if (rows.size() < MAX_PARSED_ROWS) {
                                Map<String, Object> map = new LinkedHashMap<>();
                                for (int i = 0; i < columns.size(); i++) {
                                    map.put(columns.get(i), i < currentRow.size() ? currentRow.get(i) : "");
                                }
                                rows.add(map);
                            } else {
                                break;
                            }
                        }
                    }
                    currentRow = null;
                }
            }
        } finally {
            reader.close();
        }
        return new TabularData(columns, rows, totalRows);
    }

    private static String readInlineString(XMLStreamReader reader) throws Exception {
        StringBuilder sb = new StringBuilder();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "t".equals(reader.getLocalName())) {
                sb.append(reader.getElementText());
            } else if (event == XMLStreamConstants.END_ELEMENT && "is".equals(reader.getLocalName())) {
                break;
            }
        }
        return sb.toString();
    }

    private static int columnIndexFromRef(String ref) {
        int col = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = ref.charAt(i);
            if (Character.isLetter(ch)) {
                col = col * 26 + (Character.toUpperCase(ch) - 'A' + 1);
            } else {
                break;
            }
        }
        return Math.max(0, col - 1);
    }

    private static TableauMeta parseTwbx(byte[] bytes) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().toLowerCase(Locale.ROOT).endsWith(".twb")) {
                    return parseTwbXml(zis);
                }
            }
        }
        return new TableauMeta(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private static TableauMeta parseTwbXml(InputStream is) throws Exception {
        List<String> sheets = new ArrayList<>();
        List<String> dashboards = new ArrayList<>();
        List<String> datasources = new ArrayList<>();
        XMLInputFactory factory = createSafeXmlFactory();
        XMLStreamReader reader = factory.createXMLStreamReader(is);
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                String local = reader.getLocalName();
                String name = reader.getAttributeValue(null, "name");
                String caption = reader.getAttributeValue(null, "caption");
                if ("worksheet".equals(local) && name != null && !sheets.contains(name)) {
                    sheets.add(name);
                } else if ("dashboard".equals(local) && name != null && !dashboards.contains(name)) {
                    dashboards.add(name);
                } else if ("datasource".equals(local)) {
                    String dsName = caption != null && !caption.isBlank() ? caption : name;
                    if (dsName != null && !"Parameters".equals(dsName) && !datasources.contains(dsName)) {
                        datasources.add(dsName);
                    }
                }
            }
        }
        return new TableauMeta(sheets, dashboards, datasources);
    }

    private static XMLInputFactory createSafeXmlFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }

    private record TabularData(List<String> columns, List<Map<String, Object>> rows, int totalRows) {
    }

    private record TableauMeta(List<String> sheets, List<String> dashboards, List<String> datasources) {
    }
}
