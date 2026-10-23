package GUI.Dialog;

import helper.WordQuestionParser.Question;
import helper.WordImportValidation;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** Keeps only one small page of editors mounted, even for large Word files. */
public final class WordImportPreview {
    private WordImportPreview() {}

    private static final class Draft {
        boolean included = true;
        String text, answers, key;
        Draft(Question question, int type) {
            text = question.text;
            answers = question.answers.stream().map(a -> a.replaceAll("\\R", " "))
                    .collect(java.util.stream.Collectors.joining("\n"));
            key = question.correct.size() == 1 ? String.valueOf(question.correct.iterator().next() + 1) : "";
            if (type == 2 && question.answers.size() == 1
                    && (answers.equalsIgnoreCase("Đúng") || answers.equalsIgnoreCase("Sai"))) {
                key = answers.equalsIgnoreCase("Đúng") ? "1" : "2";
                answers = "Đúng\nSai";
            }
        }
    }

    private record Editor(JCheckBox included, JTextArea text, JTextArea answers, JTextField key) {
        void save(Draft draft) {
            draft.included = included.isSelected();
            draft.text = text.getText();
            draft.answers = answers.getText();
            draft.key = key.getText();
        }
    }

    static final class Preview extends JPanel {
        private static final int PAGE_SIZE = 8;
        private final List<Draft> drafts = new ArrayList<>();
        private final List<Editor> editors = new ArrayList<>();
        private final JPanel content = new JPanel();
        private final JScrollPane scroll = new JScrollPane(content);
        private final JLabel pageInfo = new JLabel();
        private final JButton previous = new JButton("‹ Trang trước"), next = new JButton("Trang sau ›");
        private int page;

        Preview(List<Question> questions, int type) {
            super(new BorderLayout(5, 8));
            questions.forEach(q -> drafts.add(new Draft(q, type)));
            JPanel instructions = new JPanel(new GridLayout(0, 1));
            instructions.add(new JLabel("Mỗi đáp án trên một dòng. Nhập số thứ tự đáp án đúng (1, 2, ...)."));
            instructions.add(new JLabel("Bỏ chọn câu không muốn nhập. Nội dung đã sửa được giữ khi chuyển trang."));
            add(instructions, BorderLayout.NORTH);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            scroll.getVerticalScrollBar().setUnitIncrement(28);
            scroll.getVerticalScrollBar().setBlockIncrement(350);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setPreferredSize(new Dimension(830, 510));
            add(scroll, BorderLayout.CENTER);
            JPanel navigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
            navigation.add(previous); navigation.add(pageInfo); navigation.add(next);
            previous.addActionListener(e -> showPage(page - 1));
            next.addActionListener(e -> showPage(page + 1));
            add(navigation, BorderLayout.SOUTH);
            showPage(0);
        }

        void savePage() {
            for (int i = 0; i < editors.size(); i++) editors.get(i).save(drafts.get(page * PAGE_SIZE + i));
        }

        void showPage(int target) {
            if (target < 0 || target >= (drafts.size() + PAGE_SIZE - 1) / PAGE_SIZE) return;
            savePage();
            page = target;
            editors.clear(); content.removeAll();
            int end = Math.min(drafts.size(), (page + 1) * PAGE_SIZE);
            for (int i = page * PAGE_SIZE; i < end; i++) {
                Draft draft = drafts.get(i);
                JPanel panel = new JPanel(new BorderLayout(5, 5));
                panel.setBorder(BorderFactory.createTitledBorder("Câu " + (i + 1)));
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                JCheckBox include = new JCheckBox("Nhập câu này", draft.included);
                JTextArea text = area(draft.text, 3), answers = area(draft.answers, 4);
                JTextField key = new JTextField(draft.key, 5);
                JPanel top = new JPanel(new BorderLayout());
                top.add(include, BorderLayout.WEST);
                JPanel correct = new JPanel();
                correct.add(new JLabel("Đáp án đúng số:")); correct.add(key);
                top.add(correct, BorderLayout.EAST);
                panel.add(top, BorderLayout.NORTH);
                JPanel body = new JPanel(new GridLayout(2, 1, 5, 5));
                body.add(editorScroll(text)); body.add(editorScroll(answers));
                panel.add(body, BorderLayout.CENTER);
                panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
                content.add(panel);
                editors.add(new Editor(include, text, answers, key));
            }
            pageInfo.setText("Trang " + (page + 1) + "/" + ((drafts.size() + PAGE_SIZE - 1) / PAGE_SIZE)
                    + " · Câu " + (page * PAGE_SIZE + 1) + "–" + end + "/" + drafts.size());
            previous.setEnabled(page > 0); next.setEnabled(end < drafts.size());
            content.revalidate(); content.repaint();
            scroll.getViewport().setViewPosition(new Point(0, 0));
            SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new Point(0, 0)));
        }

        private JTextArea area(String text, int rows) {
            JTextArea area = new JTextArea(text, rows, 60);
            area.setLineWrap(true); area.setWrapStyleWord(true); area.setCaretPosition(0);
            return area;
        }

        private JScrollPane editorScroll(JTextArea area) {
            return new JScrollPane(area) {
                @Override protected void processMouseWheelEvent(MouseWheelEvent event) {
                    JScrollBar bar = getVerticalScrollBar();
                    boolean up = event.getPreciseWheelRotation() < 0;
                    boolean canScroll = bar.isVisible() && (up ? bar.getValue() > bar.getMinimum()
                            : bar.getValue() + bar.getVisibleAmount() < bar.getMaximum());
                    if (canScroll) {
                        super.processMouseWheelEvent(event);
                    } else {
                        Point point = SwingUtilities.convertPoint(this, event.getPoint(), scroll);
                        scroll.dispatchEvent(new MouseWheelEvent(scroll, event.getID(), event.getWhen(), event.getModifiersEx(),
                                point.x, point.y, event.getXOnScreen(), event.getYOnScreen(), event.getClickCount(), event.isPopupTrigger(),
                                event.getScrollType(), event.getScrollAmount(), event.getWheelRotation(), event.getPreciseWheelRotation()));
                        event.consume();
                    }
                }
            };
        }
    }

    public static boolean edit(Component parent, List<Question> questions, int type) {
        if (questions.isEmpty()) return false;
        Preview preview = new Preview(questions, type);
        while (JOptionPane.showConfirmDialog(parent, preview, "Xem và sửa câu hỏi trước khi lưu",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            preview.savePage();
            List<Question> accepted = new ArrayList<>();
            int current = 0;
            try {
                for (; current < preview.drafts.size(); current++) {
                    Draft draft = preview.drafts.get(current);
                    if (!draft.included) continue;
                    Question q = new Question();
                    q.text = draft.text.trim();
                    q.answers.addAll(java.util.Arrays.asList(draft.answers.split("\\R", -1)));
                    int key = Integer.parseInt(draft.key.trim()) - 1;
                    if (key < 0 || key >= q.answers.size()) throw new IllegalArgumentException("Số đáp án đúng ngoài phạm vi.");
                    q.correct.add(key);
                    WordImportValidation.validate(q.text, q.answers, q.correct.size(), type);
                    accepted.add(q);
                }
                if (accepted.isEmpty()) throw new IllegalArgumentException("Chưa chọn câu hỏi để nhập.");
                questions.clear(); questions.addAll(accepted);
                return true;
            } catch (IllegalArgumentException ex) {
                if (current < preview.drafts.size()) preview.showPage(current / Preview.PAGE_SIZE);
                String message = ex instanceof NumberFormatException ? "Hãy nhập số thứ tự đáp án đúng." : ex.getMessage();
                JOptionPane.showMessageDialog(parent, "Câu " + Math.min(current + 1, preview.drafts.size()) + ": " + message,
                        "Cần kiểm tra", JOptionPane.WARNING_MESSAGE);
            }
        }
        return false;
    }
}
