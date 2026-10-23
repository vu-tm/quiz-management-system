package helper;

import java.util.List;

public final class WordImportValidation {
    private WordImportValidation() {}
    public static void validate(String question, List<String> answers, int correctCount, int type) {
        if (question == null || question.isBlank()) throw new IllegalArgumentException("Thiếu nội dung câu hỏi.");
        if (answers.isEmpty() || answers.stream().anyMatch(a -> a == null || a.isBlank()))
            throw new IllegalArgumentException("Thiếu nội dung đáp án.");
        if (type < 1 || type > 3) throw new IllegalArgumentException("Loại câu hỏi chưa được hỗ trợ trong luồng làm bài.");
        if (type == 1 && (answers.size() < 2 || answers.size() > 4))
            throw new IllegalArgumentException("Trắc nghiệm cần 2–4 lựa chọn để tương thích giao diện sửa câu hỏi.");
        if (type == 2 && (answers.size() != 2 || !answers.get(0).equalsIgnoreCase("Đúng") || !answers.get(1).equalsIgnoreCase("Sai")))
            throw new IllegalArgumentException("Đúng/Sai cần hai đáp án theo thứ tự: Đúng, Sai.");
        if (type == 3 && answers.size() != 1) throw new IllegalArgumentException("Điền khuyết cần một câu trả lời.");
        if (correctCount != 1) throw new IllegalArgumentException("Cần xác định đúng một đáp án đúng.");
    }
}
