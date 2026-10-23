package helper;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.*;

/** Extracts editable candidates; never guesses the correct answer. */
public final class WordQuestionParser {
    private WordQuestionParser() {}
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern QUESTION = Pattern.compile("^(?:(?:câu(?:\\s+hỏi)?|question)(?:\\s*\\d+\\s*[:.)-]?|\\s*[:：-])|\\d+\\s*[.)：:])\\s*", FLAGS);
    private static final Pattern ANSWER = Pattern.compile("^\\s*(?:[✓*]\\s*)?(?:([A-H])\\s*[.):：-]|[-•▪–])\\s*", FLAGS);
    private static final Pattern INLINE = Pattern.compile("(?<!\\S)(?:[✓*]\\s*)?[A-H][.):：]\\s+", FLAGS);
    private static final Pattern KEY = Pattern.compile("^(?:đáp\\s*án(?:\\s*đúng)?|dap\\s*an|answer|trả\\s*lời)\\s*[:：=-]\\s*(.+)$", FLAGS);
    private static final Pattern MARK = Pattern.compile("^\\s*[✓*]\\s*|\\s*\\((?:đúng|correct|đ|\\*)\\)\\s*$|\\s*\\[(?:đúng|correct|đ|\\*)]\\s*$|\\s+[✓*]\\s*$", FLAGS);

    public static final class Question {
        public String text = "";
        public final List<String> answers = new ArrayList<>();
        public final Set<Integer> correct = new LinkedHashSet<>();
        private final List<String> labels = new ArrayList<>();
        private String key;
        private boolean plainAnswers;
    }
    private record Line(String text, String numbering, BitSet emphasis) {
        Line slice(int start, int end) {
            return new Line(text.substring(start, end), numbering, emphasis.get(start, end));
        }
        Line trimmed() {
            String normalized = text.replace('\u00a0', ' ');
            int start = 0, end = normalized.length();
            while (start < end && normalized.charAt(start) <= ' ') start++;
            while (end > start && normalized.charAt(end - 1) <= ' ') end--;
            return new Line(normalized.substring(start, end), numbering, emphasis.get(start, end));
        }
        boolean marked(int start, int end) {
            Line part = slice(start, end);
            Matcher label = ANSWER.matcher(part.text);
            int bodyStart = 0;
            if (label.find()) {
                bodyStart = label.end();
                // A bold A/B/C/D label alone is a common answer-key convention.
                if (label.group(1) != null && part.emphasis.get(label.start(1))) return true;
            }
            boolean hasText = false;
            for (int i = bodyStart; i < part.text.length(); i++) {
                if (!Character.isLetterOrDigit(part.text.charAt(i))) continue;
                hasText = true;
                if (!part.emphasis.get(i)) return false;
            }
            return hasText;
        }
    }

    private static void append(StringBuilder text, BitSet emphasis, String value, boolean marked) {
        int start = text.length();
        text.append(value);
        if (marked) emphasis.set(start, text.length());
    }

    private static void addLines(List<Line> lines, String text, String format, BitSet emphasis) {
        Matcher breaks = Pattern.compile("\\R").matcher(text);
        int start = 0;
        while (breaks.find()) {
            lines.add(new Line(text.substring(start, breaks.start()), format, emphasis.get(start, breaks.start())));
            format = null;
            start = breaks.end();
        }
        lines.add(new Line(text.substring(start), format, emphasis.get(start, text.length())));
    }

    /** Detect the actual file signature, including files with an incorrect extension. */
    public static List<Question> parse(File file) throws IOException {
        try (InputStream input = FileMagic.prepareToCheckMagic(new FileInputStream(file))) {
            FileMagic magic = FileMagic.valueOf(input);
            if (magic == FileMagic.OOXML) {
                try (XWPFDocument document = new XWPFDocument(input)) {
                    return parse(document);
                }
            }
            if (magic == FileMagic.OLE2) {
                try (POIFSFileSystem filesystem = new POIFSFileSystem(input)) {
                    if (filesystem.getRoot().hasEntry("EncryptedPackage"))
                        throw new IOException("File Word được mã hóa. Hãy bỏ mật khẩu bảo vệ rồi nhập lại.");
                    if (!filesystem.getRoot().hasEntry("WordDocument"))
                        throw new IOException("File OLE2 này không phải tài liệu Word .doc.");
                    try (HWPFDocument document = new HWPFDocument(filesystem)) {
                        return parse(document);
                    }
                }
            }
            throw new IOException("Định dạng không được hỗ trợ. Hãy chọn tài liệu Word .doc hoặc .docx.");
        }
    }

    public static List<Question> parse(HWPFDocument document) {
        List<Line> lines = new ArrayList<>();
        var range = document.getRange();
        for (int i = 0; i < range.numParagraphs(); i++) {
            var paragraph = range.getParagraph(i);
            StringBuilder text = new StringBuilder();
            BitSet emphasis = new BitSet();
            for (int j = 0; j < paragraph.numCharacterRuns(); j++) {
                var run = paragraph.getCharacterRun(j);
                append(text, emphasis, run.text(), run.isBold());
            }
            String format = null;
            if (paragraph.isInList()) {
                int numberFormat = paragraph.getList().getNumberFormat((char) paragraph.getIlvl());
                format = switch (numberFormat) {
                    case 0 -> "decimal";
                    case 3 -> "upperLetter";
                    case 4 -> "lowerLetter";
                    case 23 -> "bullet";
                    default -> null;
                };
            }
            // Binary Word uses BEL for cell ends and vertical tab for soft breaks.
            addLines(lines, text.toString().replace('\u0007', '\n').replace('\u000b', '\n'), format, emphasis);
        }
        return parseLines(lines);
    }

    public static List<Question> parse(XWPFDocument document) {
        List<Line> lines = new ArrayList<>();
        read(document.getBodyElements(), lines);
        return parseLines(lines);
    }

    private static List<Question> parseLines(List<Line> lines) {
        List<Question> result = new ArrayList<>();
        Question current = null;
        boolean boundary = false;
        for (Line line : lines) {
            line = line.trimmed();
            String text = line.text();
            if (text.isEmpty()) { boundary = true; continue; }
            Matcher key = KEY.matcher(text);
            if (key.matches() && current != null) {
                current.key = key.group(1).trim();
                boundary = true;
                continue;
            }
            Matcher q = QUESTION.matcher(text);
            Matcher a = ANSWER.matcher(text);
            boolean explicit = q.find() || "decimal".equals(line.numbering()) || "decimalZero".equals(line.numbering());
            boolean option = a.find() || "upperLetter".equals(line.numbering()) || "lowerLetter".equals(line.numbering()) || "bullet".equals(line.numbering());
            if (explicit || current == null || (!option && !current.answers.isEmpty() && (boundary || current.key != null || text.endsWith("?")))) {
                current = new Question();
                result.add(current);
                Matcher prefix = QUESTION.matcher(text);
                if (prefix.find()) line = line.slice(prefix.end(), text.length());
                text = line.text();
                current.text = text;
                splitInline(current, line, true);
            } else if (option) {
                int before = current.answers.size();
                splitInline(current, line, false);
                if ("upperLetter".equals(line.numbering()) || "lowerLetter".equals(line.numbering())) {
                    for (int i = before; i < current.answers.size(); i++) {
                        if (current.labels.get(i).isEmpty()) current.labels.set(i, String.valueOf((char) ('A' + i)));
                    }
                }
            } else if (current.plainAnswers || (current.answers.isEmpty() && current.text.endsWith("?"))) {
                current.plainAnswers = true;
                addAnswer(current, text, line.marked(0, text.length()));
            } else if (current.answers.isEmpty()) {
                String prefix = current.text + "\n";
                BitSet combined = new BitSet();
                for (int bit = line.emphasis.nextSetBit(0); bit >= 0; bit = line.emphasis.nextSetBit(bit + 1))
                    combined.set(prefix.length() + bit);
                current.text = prefix + text;
                splitInline(current, new Line(current.text, null, combined), true);
            } else {
                int last = current.answers.size() - 1;
                current.answers.set(last, current.answers.get(last) + "\n" + text);
            }
            boundary = false;
        }
        for (Question question : result) {
            // Plain blocks remain editable candidates when no structural markers exist.
            if (question.answers.isEmpty() && question.key == null && question.text.contains("\n")) {
                String[] block = question.text.split("\\R");
                question.text = block[0];
                for (int i = 1; i < block.length; i++) addAnswer(question, block[i], false);
            }
            applyKey(question);
        }
        return result;
    }

    private static void splitInline(Question question, Line line, boolean includesQuestion) {
        String text = line.text();
        Matcher matcher = INLINE.matcher(text);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) starts.add(matcher.start());
        if (!starts.isEmpty()) {
            if (includesQuestion) question.text = text.substring(0, starts.get(0)).trim();
            for (int i = 0; i < starts.size(); i++) {
                int start = starts.get(i), end = i + 1 < starts.size() ? starts.get(i + 1) : text.length();
                addAnswer(question, text.substring(start, end), line.marked(start, end));
            }
        } else if (!includesQuestion) addAnswer(question, text, line.marked(0, text.length()));
    }

    private static void addAnswer(Question q, String raw, boolean marked) {
        Matcher prefix = ANSWER.matcher(raw);
        String label = "";
        String text = raw;
        if (prefix.find()) {
            label = prefix.group(1) == null ? "" : prefix.group(1).toUpperCase(Locale.ROOT);
            text = raw.substring(prefix.end());
        }
        if (marked || MARK.matcher(raw).find()) q.correct.add(q.answers.size());
        q.answers.add(MARK.matcher(text).replaceAll("").trim());
        q.labels.add(label);
    }

    private static void applyKey(Question q) {
        if (q.key == null) return;
        q.correct.clear();
        String key = q.key.trim();
        if (q.answers.isEmpty()) {
            q.answers.add(key);
            q.correct.add(0);
            return;
        }
        for (int i = 0; i < q.answers.size(); i++) {
            String label = q.labels.get(i).isEmpty() ? String.valueOf((char) ('A' + i)) : q.labels.get(i);
            if (key.equalsIgnoreCase(q.answers.get(i)) || key.matches("(?i)" + label + "[.)]?")
                    || key.equals(String.valueOf(i + 1))) q.correct.add(i);
        }
    }

    private static void read(List<IBodyElement> elements, List<Line> lines) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph p) {
                List<XWPFRun> runs = p.getRuns();
                StringBuilder text = new StringBuilder();
                BitSet emphasis = new BitSet();
                for (XWPFRun run : runs) {
                    String color = run.getColor();
                    boolean colored = color != null && !color.equalsIgnoreCase("000000") && !color.equalsIgnoreCase("auto");
                    append(text, emphasis, run.text(), effectiveBold(run, p) || colored);
                }
                addLines(lines, text.toString(), p.getNumFmt(), emphasis);
            } else if (element instanceof XWPFTable table) {
                for (XWPFTableRow row : table.getRows()) for (XWPFTableCell cell : row.getTableCells()) read(cell.getBodyElements(), lines);
            }
        }
    }

    private static Boolean bold(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr properties) {
        if (properties == null || properties.sizeOfBArray() == 0) return null;
        var value = properties.getBArray(0).getVal();
        return value == null || !Set.of("false", "0", "off").contains(value.toString().toLowerCase(Locale.ROOT));
    }

    private static Boolean styleBold(XWPFStyles styles, String id) {
        Set<String> visited = new HashSet<>();
        while (id != null && visited.add(id)) {
            XWPFStyle style = styles.getStyle(id);
            if (style == null) break;
            Boolean value = bold(style.getCTStyle().getRPr());
            if (value != null) return value;
            id = style.getBasisStyleID();
        }
        return null;
    }

    private static boolean effectiveBold(XWPFRun run, XWPFParagraph paragraph) {
        Boolean direct = bold(run.getCTR().getRPr());
        if (direct != null) return direct;
        XWPFStyles styles = paragraph.getDocument().getStyles();
        if (styles == null) return false;
        Boolean value = styleBold(styles, run.getStyle());
        if (value == null) value = styleBold(styles, paragraph.getStyle());
        if (value == null && paragraph.getStyle() == null) value = styleBold(styles, "Normal");
        return Boolean.TRUE.equals(value);
    }
}
