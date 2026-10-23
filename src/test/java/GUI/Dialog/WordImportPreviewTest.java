package GUI.Dialog;

import helper.WordQuestionParser.Question;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WordImportPreviewTest {
    private List<Question> questions(int count) {
        List<Question> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Question q = new Question();
            q.text = "Question " + i;
            q.answers.add("One"); q.answers.add("Two"); q.correct.add(0);
            result.add(q);
        }
        return result;
    }

    private <T> List<T> components(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) result.add(type.cast(child));
            if (child instanceof Container container) result.addAll(components(container, type));
        }
        return result;
    }

    @Test void largeImportMountsOnlyOnePageAndPreservesEdits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var preview = new WordImportPreview.Preview(questions(270), 1);
            assertEquals(16, components(preview, JTextArea.class).size());
            components(preview, JTextArea.class).get(0).setText("Edited question");
            components(preview, JTextArea.class).get(1).setText("Edited one\nEdited two");
            components(preview, JCheckBox.class).get(0).setSelected(false);
            components(preview, JTextField.class).get(0).setText("2");
            preview.showPage(1);
            assertEquals("Question 8", components(preview, JTextArea.class).get(0).getText());
            preview.showPage(0);
            assertEquals("Edited question", components(preview, JTextArea.class).get(0).getText());
            assertEquals("Edited one\nEdited two", components(preview, JTextArea.class).get(1).getText());
            assertFalse(components(preview, JCheckBox.class).get(0).isSelected());
            assertEquals("2", components(preview, JTextField.class).get(0).getText());
            preview.showPage(33);
            assertEquals(12, components(preview, JTextArea.class).size());
            assertEquals("Question 264", components(preview, JTextArea.class).get(0).getText());
            preview.showPage(34);
            assertEquals(12, components(preview, JTextArea.class).size());
        });
    }
}
