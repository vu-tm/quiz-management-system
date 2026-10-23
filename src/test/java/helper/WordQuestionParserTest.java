package helper;

import java.util.List;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WordQuestionParserTest {
    private void line(XWPFDocument doc, String text) { doc.createParagraph().createRun().setText(text); }
    private void run(XWPFParagraph p, String text, boolean bold) {
        var run = p.createRun(); run.setText(text); run.setBold(bold);
    }

    @Test void recognizesOnlyBoldLabelWithoutRequiringBoldAnswerBody() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Câu 1: Chọn?");
            line(doc, "A. Một");
            var p = doc.createParagraph(); run(p, "B", true); run(p, ". Hai", false);
            assertEquals(java.util.Set.of(1), WordQuestionParser.parse(doc).get(0).correct);
        }
    }

    @Test void recognizesBoldBodyWithPlainLabelAndPunctuation() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Câu 1: Chọn?"); line(doc, "A. Một");
            var p = doc.createParagraph(); run(p, "B. ", false); run(p, "Hai", true); run(p, ". ", false);
            assertEquals(java.util.Set.of(1), WordQuestionParser.parse(doc).get(0).correct);
        }
    }

    @Test void tracksBoldSeparatelyForInlineOptionsIncludingQuestionOnSameLine() throws Exception {
        try (var doc = new XWPFDocument()) {
            var p = doc.createParagraph();
            run(p, "Câu 1: Chọn? A. Một   ", false); run(p, "B. Hai", true); run(p, "   C. Ba D. Bốn", false);
            var q = WordQuestionParser.parse(doc).get(0);
            assertEquals(List.of("Một", "Hai", "Ba", "Bốn"), q.answers);
            assertEquals(java.util.Set.of(1), q.correct);
        }
    }

    @Test void softBreaksDoNotShareBoldBetweenAnswers() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Câu 1: Chọn?");
            var p = doc.createParagraph();
            run(p, "A. Một", false); p.createRun().addBreak();
            run(p, "B. Hai", true); p.createRun().addBreak(); run(p, "C. Ba", false);
            assertEquals(java.util.Set.of(1), WordQuestionParser.parse(doc).get(0).correct);
        }
    }

    @Test void readsBoldInheritedFromCharacterAndParagraphStyles() throws Exception {
        try (var doc = new XWPFDocument()) {
            var styles = doc.createStyles();
            var definition = org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle.Factory.newInstance();
            definition.setStyleId("AnswerBold"); definition.addNewRPr().addNewB();
            styles.addStyle(new XWPFStyle(definition));
            line(doc, "Câu 1: Chọn?"); line(doc, "A. Một");
            var p = doc.createParagraph();
            var label = p.createRun(); label.setStyle("AnswerBold"); label.setText("B.");
            p.createRun().setText(" Hai");
            line(doc, "Câu 2: Chọn?"); line(doc, "A. Một");
            p = doc.createParagraph(); p.setStyle("AnswerBold"); p.createRun().setText("B. Hai");
            var result = WordQuestionParser.parse(doc);
            assertEquals(java.util.Set.of(1), result.get(0).correct);
            assertEquals(java.util.Set.of(1), result.get(1).correct);
        }
    }

    @Test void partialEmphasisInsideBodyDoesNotGuessAnAnswer() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Câu 1: Chọn?"); line(doc, "A. Một");
            var p = doc.createParagraph(); run(p, "B. Chỉ ", false); run(p, "một", true); run(p, " từ nhấn mạnh", false);
            assertTrue(WordQuestionParser.parse(doc).get(0).correct.isEmpty());
        }
    }

    @Test void supportsUnnumberedQuestionsInlineAnswersAndTextKeys() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Thủ đô Việt Nam là gì?");
            line(doc, "A. Hà Nội B. Huế C. Đà Nẵng D. TP HCM");
            line(doc, "Đáp án: Hà Nội");
            line(doc, "Hai cộng hai bằng mấy?");
            line(doc, "A) 3"); line(doc, "B) 4"); line(doc, "Answer: B");
            var result = WordQuestionParser.parse(doc);
            assertEquals(2, result.size());
            assertEquals("Thủ đô Việt Nam là gì?", result.get(0).text);
            assertEquals(List.of("Hà Nội", "Huế", "Đà Nẵng", "TP HCM"), result.get(0).answers);
            assertEquals(java.util.Set.of(0), result.get(0).correct);
            assertEquals(java.util.Set.of(1), result.get(1).correct);
        }
    }
    @Test void preservesBodyTableOrderAndReadsCells() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Câu 1: Trước bảng?"); line(doc, "A. Một"); line(doc, "B. Hai (*)");
            var table = doc.createTable(1, 3);
            table.getRow(0).getCell(0).setText("Câu 2: Trong bảng?");
            table.getRow(0).getCell(1).setText("A. Có");
            table.getRow(0).getCell(2).setText("B. Không");
            line(doc, "Đáp án: A");
            var result = WordQuestionParser.parse(doc);
            assertEquals(2, result.size());
            assertEquals("Trong bảng?", result.get(1).text);
            assertEquals(java.util.Set.of(1), result.get(0).correct);
            assertEquals(java.util.Set.of(0), result.get(1).correct);
        }
    }
    @Test void supportsFillAnswerWithoutOptions() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Câu hỏi: Thủ đô Pháp?"); line(doc, "Trả lời: Paris");
            var q = WordQuestionParser.parse(doc).get(0);
            assertEquals("Thủ đô Pháp?", q.text);
            assertEquals(List.of("Paris"), q.answers);
            assertEquals(java.util.Set.of(0), q.correct);
        }
    }
    @Test void missingOrInvalidKeyDoesNotGuessCorrectAnswer() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "1. Chọn?"); line(doc, "A. Một"); line(doc, "B. Hai");
            line(doc, "Đáp án: H");
            var q = WordQuestionParser.parse(doc).get(0);
            assertTrue(q.correct.isEmpty());
            assertThrows(IllegalArgumentException.class, () -> WordImportValidation.validate(q.text, q.answers, q.correct.size(), 1));
        }
    }
    @Test void handlesWordBreaksBulletsAndContinuation() throws Exception {
        try (var doc = new XWPFDocument()) {
            var run = doc.createParagraph().createRun();
            run.setText("CÂU 1: Chọn?"); run.addBreak(); run.setText("- Một");
            line(doc, "nội dung tiếp"); line(doc, "• Hai [đúng]");
            var q = WordQuestionParser.parse(doc).get(0);
            assertEquals(List.of("Một\nnội dung tiếp", "Hai"), q.answers);
            assertEquals(java.util.Set.of(1), q.correct);
        }
    }
    @Test void explicitKeyOverridesFormatting() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Question 1: Choose?");
            var run = doc.createParagraph().createRun(); run.setBold(true); run.setText("A. One");
            line(doc, "B. Two"); line(doc, "Answer: B");
            assertEquals(java.util.Set.of(1), WordQuestionParser.parse(doc).get(0).correct);
        }
    }
    @Test void supportsPlainAnswersWithoutLettersOrBullets() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Thủ đô Việt Nam?"); line(doc, "Hà Nội"); line(doc, "Huế");
            line(doc, "Đáp án: Hà Nội");
            var q = WordQuestionParser.parse(doc).get(0);
            assertEquals(List.of("Hà Nội", "Huế"), q.answers);
            assertEquals(java.util.Set.of(0), q.correct);
        }
    }
    @Test void offersPlainBlockAsEditableCandidate() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Chọn kết quả phép cộng"); line(doc, "Một"); line(doc, "Hai");
            var q = WordQuestionParser.parse(doc).get(0);
            assertEquals("Chọn kết quả phép cộng", q.text);
            assertEquals(List.of("Một", "Hai"), q.answers);
            assertTrue(q.correct.isEmpty());
        }
    }
    @Test void validatesCompatibilityWithExamAndEditFlows() {
        assertThrows(IllegalArgumentException.class, () -> WordImportValidation.validate("Q", List.of("A", ""), 1, 1));
        assertThrows(IllegalArgumentException.class, () -> WordImportValidation.validate("Q", List.of("A", "B"), 0, 1));
        assertThrows(IllegalArgumentException.class, () -> WordImportValidation.validate("Q", List.of("A", "B"), 2, 1));
        assertThrows(IllegalArgumentException.class, () -> WordImportValidation.validate("Q", List.of("Sai", "Đúng"), 1, 2));
        assertDoesNotThrow(() -> WordImportValidation.validate("Q", List.of("Đúng", "Sai"), 1, 2));
        assertDoesNotThrow(() -> WordImportValidation.validate("Q", List.of("Paris"), 1, 3));
    }

    @Test void readsRealDocxRoundTripAndKeepsMultilineAnswers() throws Exception {
        byte[] bytes;
        try (var doc = new XWPFDocument(); var output = new java.io.ByteArrayOutputStream()) {
            line(doc, "1. Chọn một đáp án?");
            line(doc, "A. Dòng một"); line(doc, "nối tiếp đáp án A");
            line(doc, "B. Dòng hai"); line(doc, "Đáp án: B");
            doc.write(output);
            bytes = output.toByteArray();
        }
        try (var doc = new XWPFDocument(new java.io.ByteArrayInputStream(bytes))) {
            var q = WordQuestionParser.parse(doc).get(0);
            assertEquals(List.of("Dòng một\nnối tiếp đáp án A", "Dòng hai"), q.answers);
            assertEquals(java.util.Set.of(1), q.correct);
        }
    }

    @Test void supportsAutomaticWordQuestionAndAnswerLists() throws Exception {
        try (var doc = new XWPFDocument()) {
            var numbering = doc.createNumbering();
            var definition = org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum.Factory.newInstance();
            definition.setAbstractNumId(java.math.BigInteger.ZERO);
            var questions = definition.addNewLvl();
            questions.setIlvl(java.math.BigInteger.ZERO);
            questions.addNewNumFmt().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat.DECIMAL);
            questions.addNewLvlText().setVal("%1.");
            var options = definition.addNewLvl();
            options.setIlvl(java.math.BigInteger.ONE);
            options.addNewNumFmt().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat.UPPER_LETTER);
            options.addNewLvlText().setVal("%2.");
            var numId = numbering.addNum(numbering.addAbstractNum(new XWPFAbstractNum(definition)));
            for (int i = 0; i < 3; i++) {
                var p = doc.createParagraph();
                p.setNumID(numId);
                p.setNumILvl(i == 0 ? java.math.BigInteger.ZERO : java.math.BigInteger.ONE);
                p.createRun().setText(i == 0 ? "Chọn kết quả?" : i == 1 ? "Một" : "Hai");
            }
            line(doc, "Đáp án: B");
            var result = WordQuestionParser.parse(doc);
            assertEquals(1, result.size());
            assertEquals(List.of("Một", "Hai"), result.get(0).answers);
            assertEquals(java.util.Set.of(1), result.get(0).correct);
        }
    }

    @Test void keepsOrdinaryVietnameseWordCauInQuestionContent() throws Exception {
        try (var doc = new XWPFDocument()) {
            line(doc, "Câu chuyện nào phù hợp?"); line(doc, "A. Một"); line(doc, "B. Hai");
            assertEquals("Câu chuyện nào phù hợp?", WordQuestionParser.parse(doc).get(0).text);
        }
    }
}
