package helper;

import java.io.*;
import java.nio.file.*;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WordFileFormatTest {
    @TempDir Path directory;

    private File binaryWord(String filename) throws Exception {
        File file = directory.resolve(filename).toFile();
        try (InputStream template = getClass().getResourceAsStream("/helper/empty.doc");
             HWPFDocument document = new HWPFDocument(template);
             OutputStream output = new FileOutputStream(file)) {
            document.getRange().insertBefore("Câu 1: Thủ đô Việt Nam?\rA. Hà Nội\rB. Huế\rĐáp án: A\r");
            document.write(output);
        }
        return file;
    }

    @Test void importsBinaryDoc() throws Exception {
        var questions = WordQuestionParser.parse(binaryWord("questions.doc"));
        assertEquals(1, questions.size());
        assertEquals("Thủ đô Việt Nam?", questions.get(0).text);
        assertEquals(java.util.List.of("Hà Nội", "Huế"), questions.get(0).answers);
        assertEquals(java.util.Set.of(0), questions.get(0).correct);
    }

    @Test void importsBinaryWordEvenWhenRenamedToDocx() throws Exception {
        var questions = WordQuestionParser.parse(binaryWord("questions.docx"));
        assertEquals(java.util.List.of("Hà Nội", "Huế"), questions.get(0).answers);
    }

    @Test void binaryWordPreservesBoldForMultipleOptionsInSameRun() throws Exception {
        File file = directory.resolve("bold-label.doc").toFile();
        try (InputStream template = getClass().getResourceAsStream("/helper/empty.doc");
             HWPFDocument document = new HWPFDocument(template);
             OutputStream output = new FileOutputStream(file)) {
            String text = "Câu 1: Chọn?\rA. Một B. Hai C. Ba\r";
            document.getRange().insertBefore(text);
            document.getRange().getCharacterRun(0).setBold(true);
            document.write(output);
        }
        var q = WordQuestionParser.parse(file).get(0);
        assertEquals(java.util.List.of("Một", "Hai", "Ba"), q.answers);
        assertEquals(java.util.Set.of(0, 1, 2), q.correct);
    }

    @Test void detectsOoxmlEvenWithDocExtension() throws Exception {
        File file = directory.resolve("questions.doc").toFile();
        try (var document = new XWPFDocument(); var output = new FileOutputStream(file)) {
            document.createParagraph().createRun().setText("Câu 1: Thủ đô Pháp?");
            document.createParagraph().createRun().setText("Trả lời: Paris");
            document.write(output);
        }
        assertEquals(java.util.List.of("Paris"), WordQuestionParser.parse(file).get(0).answers);
    }

    @Test void rejectsNonWordOle2WithReadableError() throws Exception {
        File file = directory.resolve("spreadsheet.docx").toFile();
        try (var filesystem = new POIFSFileSystem(); var output = new FileOutputStream(file)) {
            filesystem.createDocument(new ByteArrayInputStream(new byte[]{1}), "Workbook");
            filesystem.writeFilesystem(output);
        }
        IOException error = assertThrows(IOException.class, () -> WordQuestionParser.parse(file));
        assertTrue(error.getMessage().contains("không phải tài liệu Word"));
    }

    @Test void rejectsEncryptedPackageWithReadableError() throws Exception {
        File file = directory.resolve("encrypted.docx").toFile();
        try (var filesystem = new POIFSFileSystem(); var output = new FileOutputStream(file)) {
            filesystem.createDocument(new ByteArrayInputStream(new byte[]{1}), "EncryptedPackage");
            filesystem.writeFilesystem(output);
        }
        IOException error = assertThrows(IOException.class, () -> WordQuestionParser.parse(file));
        assertTrue(error.getMessage().contains("mật khẩu"));
    }

    @Test void rejectsTextRenamedToWord() throws Exception {
        File file = directory.resolve("text.doc").toFile();
        Files.writeString(file.toPath(), "This is plain text, not a Word document.");
        IOException error = assertThrows(IOException.class, () -> WordQuestionParser.parse(file));
        assertTrue(error.getMessage().contains("Định dạng không được hỗ trợ"));
    }
}
