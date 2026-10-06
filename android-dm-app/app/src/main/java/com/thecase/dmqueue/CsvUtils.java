package com.thecase.dmqueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CsvUtils {
    public static class ImportResult {
        public final List<Contact> valid = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
    }

    public static ImportResult importText(String text) {
        ImportResult result = new ImportResult();
        if (text == null || text.trim().isEmpty()) {
            result.errors.add("Файл или вставка пустые.");
            return result;
        }

        char delimiter = detectDelimiter(text);
        List<List<String>> rows = parse(text, delimiter);
        if (rows.isEmpty()) {
            result.errors.add("Не удалось прочитать строки.");
            return result;
        }

        List<String> first = rows.get(0);
        Map<String,Integer> header = headerMap(first);
        boolean hasHeader = header.containsKey("instagram") || header.containsKey("handle") || header.containsKey("profile_url");

        int start = hasHeader ? 1 : 0;
        for (int r = start; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.isEmpty() || row.stream().allMatch(String::isBlank)) continue;

            String instagram;
            String message;
            String brand = "";
            String language = "";
            String note = "";

            if (hasHeader) {
                instagram = get(row, firstIndex(header, "instagram", "handle", "profile_url"));
                message = get(row, firstIndex(header, "message", "letter", "text"));
                brand = get(row, firstIndex(header, "brand", "brand_name", "name"));
                language = get(row, firstIndex(header, "language", "lang"));
                note = get(row, firstIndex(header, "note", "comment"));
            } else {
                instagram = get(row, 0);
                message = get(row, 1);
                brand = get(row, 2);
                language = get(row, 3);
                note = get(row, 4);
            }

            instagram = ContactStore.normalizeHandle(instagram);
            if (!isValidHandle(instagram)) {
                result.errors.add("Строка " + (r + 1) + ": некорректный Instagram — " + instagram);
                continue;
            }
            if (message == null || message.trim().isEmpty()) {
                result.errors.add("Строка " + (r + 1) + ": нет текста письма.");
                continue;
            }

            Contact c = new Contact();
            c.instagram = instagram;
            c.message = message.trim();
            c.brand = brand == null ? "" : brand.trim();
            c.language = language == null ? "" : language.trim();
            c.note = note == null ? "" : note.trim();
            result.valid.add(c);
        }
        return result;
    }

    public static String exportCsv(List<Contact> contacts) {
        StringBuilder sb = new StringBuilder();
        sb.append("instagram,brand,message,language,note,status,imported_at,updated_at\n");
        for (Contact c : contacts) {
            sb.append(csv(c.instagram)).append(',')
              .append(csv(c.brand)).append(',')
              .append(csv(c.message)).append(',')
              .append(csv(c.language)).append(',')
              .append(csv(c.note)).append(',')
              .append(csv(c.status)).append(',')
              .append(c.importedAt).append(',')
              .append(c.updatedAt).append('\n');
        }
        return sb.toString();
    }

    public static String sampleCsv() {
        return "instagram,brand,message,language,note\n"
                + "brand_example,Brand Example,\"Здравствуйте! Ваш продукт вызывает симпатию.\\n\\nЯ создаю презентации и сайты для бизнеса и брендов. Если буду полезна — пишите.\",ru,пример\n"
                + "brand_example_en,Brand Example EN,\"Hi! I really like the product.\\n\\nIf you ever need a brand presentation, I’d be happy to collaborate.\",en,example\n";
    }

    private static char detectDelimiter(String text) {
        String head = text.substring(0, Math.min(text.length(), 2000));
        int tabs = count(head, '\t');
        int semis = count(head, ';');
        int commas = count(head, ',');
        if (tabs >= semis && tabs >= commas && tabs > 0) return '\t';
        if (semis > commas) return ';';
        return ',';
    }

    private static int count(String s, char ch) {
        int n = 0;
        for (int i=0;i<s.length();i++) if (s.charAt(i)==ch) n++;
        return n;
    }

    public static List<List<String>> parse(String text, char delimiter) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == delimiter && !quoted) {
                row.add(cell.toString().trim());
                cell.setLength(0);
            } else if ((ch == '\n' || ch == '\r') && !quoted) {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cell.toString().trim());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                cell.append(ch);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString().trim());
            rows.add(row);
        }
        return rows;
    }

    private static Map<String,Integer> headerMap(List<String> row) {
        HashMap<String,Integer> map = new HashMap<>();
        for (int i=0;i<row.size();i++) map.put(row.get(i).trim().toLowerCase(Locale.ROOT), i);
        return map;
    }

    private static int firstIndex(Map<String,Integer> map, String... keys) {
        for (String k : keys) if (map.containsKey(k)) return map.get(k);
        return -1;
    }

    private static String get(List<String> row, int i) {
        if (i < 0 || i >= row.size()) return "";
        return row.get(i);
    }

    private static boolean isValidHandle(String h) {
        if (h == null || h.length() < 1 || h.length() > 30) return false;
        return h.matches("[A-Za-z0-9._]+");
    }

    private static String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
