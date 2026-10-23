<!-- author: Trương Minh Vũ -->
# Đồ án Java: Xây dựng ứng dụng quản lý thi trắc nghiệm

## Getting Started
1. Tải source code về:

    ```bash
    https://github.com/vu-tm/QuanLyQuiz.git
   ```

2. Mở xampp và vào trang http://localhost/phpmyadmin/ tạo 1 database mới có tên là quiz và import cơ sở dữ liệu trong folder Database -> file quiz.sql trong \database.

3. Sử dụng netbeans để chạy source code.

4. Sử dụng các tài khoản với Admin, Giảng viên<i>(tạo đề)</i>, Sinh Viên<i>(làm bài thi)</i>.
#### Tài khoản Admin
- Username: admin
- Password: 123
#### Tài khoản Giảng viên
- Username: gv1
- Password: 123
#### Tài khoản Sinh viên
- Username: sv1
- Password: 123
## Nhập câu hỏi từ Word

Trong màn hình **Câu hỏi → NHẬP WORD**, chọn file `.doc` hoặc `.docx`, môn học, độ khó và loại câu hỏi. Ứng dụng nhận diện định dạng theo nội dung file (kể cả khi đặt sai phần mở rộng), rồi mở màn hình xem/sửa trước khi lưu; không cần dùng một mẫu Word cố định.

- Hỗ trợ câu hỏi có hoặc không đánh số, lựa chọn A–H, gạch đầu dòng, lựa chọn cùng dòng và văn bản trong bảng Word theo thứ tự tài liệu.
- Nhận diện dòng `Đáp án: B`, `Đáp án: nội dung`, `Answer: B`, `Trả lời: nội dung`; giữ hỗ trợ đánh dấu đúng bằng chữ đậm, màu chữ hoặc dấu `(*)`, `[đúng]`. Các dấu định dạng chỉ là gợi ý và cần được kiểm tra trong màn hình xem trước.
- Chữ đậm được kiểm tra riêng theo từng lựa chọn, kể cả nhiều lựa chọn chung dòng, xuống dòng mềm, chỉ tô đậm nhãn A/B/C/D hoặc tô đậm nội dung nhưng để nhãn thường. Với `.docx`, đọc thêm định dạng đậm từ style của chữ/đoạn. Nếu nhiều lựa chọn cùng được đánh dấu, người dùng cần xác nhận một đáp án đúng ở màn hình xem trước.
- Với đoạn văn bản không có nhãn rõ ràng, hệ thống đưa ra kết quả tách dự kiến để người dùng sửa. Mỗi đáp án hiển thị trên một dòng; nhập số thứ tự đáp án đúng, hoặc bỏ chọn câu không muốn nhập. Hệ thống không tự giải câu hỏi để đoán đáp án đúng.
- Trắc nghiệm hỗ trợ 2–4 lựa chọn và một đáp án đúng; Đúng/Sai dùng hai lựa chọn theo thứ tự Đúng, Sai; Điền khuyết dùng một câu trả lời. Các giới hạn này giữ tương thích với giao diện sửa câu hỏi và chấm thi hiện có.
- Mỗi câu hỏi và toàn bộ đáp án được lưu trong cùng transaction. Câu trùng hoặc lỗi database được báo riêng; lỗi ghi đáp án sẽ rollback câu hỏi đó.

Hiện hỗ trợ văn bản trong `.doc` (Word 97–2003) và `.docx`, chưa hỗ trợ file có mật khẩu, ảnh chụp đề/OCR, công thức hoặc nội dung chỉ có trong ảnh. Với `.doc`, hỗ trợ chữ đậm và dấu/dòng chỉ đáp án; màu chữ chưa được dùng để suy ra đáp án đúng. Tài liệu có bố cục mơ hồ có thể cần sửa kết quả nhận diện trước khi lưu. Một lần nhập dùng chung môn học, độ khó và loại câu hỏi đã chọn.

Chạy kiểm tra: `mvn test`.

## Giao diện

<h4 align="center">Đăng nhập</h4>
<img width="1231" height="515" alt="image" src="https://github.com/user-attachments/assets/07dd02a7-9d05-4067-a45e-cb1dcf190dc2" />
<br>

<h4 align="center">Trang chủ</h4>
<img width="1919" height="1104" alt="image" src="https://github.com/user-attachments/assets/db6ef0dd-b22e-4b15-9d06-fa551d235ff2" />
<br>

<h4 align="center">Môn học</h4>
<img width="1917" height="1106" alt="image" src="https://github.com/user-attachments/assets/c338293d-7a44-428a-a873-e9d6278e55c0" />
<br>

<h4 align="center">Lớp học</h4>
<img width="1919" height="1101" alt="image" src="https://github.com/user-attachments/assets/ade4c095-2e58-463c-87fc-1e7a3c4c64d8" />
<br>

<h4 align="center">Phân công</h4>
<img width="1918" height="1097" alt="image" src="https://github.com/user-attachments/assets/7ef0a0d1-1e25-46a0-82ff-564b16978c75" />
<br>

<h4 align="center">Độ khó</h4>
<img width="1918" height="1107" alt="image" src="https://github.com/user-attachments/assets/dcfe64fe-28a2-4c1d-a8e7-d6621bc4eb06" />
<br>

<h4 align="center">Câu hỏi</h4>
<img width="1919" height="1102" alt="image" src="https://github.com/user-attachments/assets/639b4e59-8fde-4d2e-aca8-b3bcfb300267" />
<br>

<h4 align="center">Kỳ thi</h4>
<img width="1919" height="1096" alt="image" src="https://github.com/user-attachments/assets/7d1d80be-ffd5-4604-8ccc-8516e5598432" />
<br>

<h4 align="center">Đề thi</h4>
<img width="1918" height="1102" alt="image" src="https://github.com/user-attachments/assets/229a739d-4ede-4b6a-871d-c620a6135713" />
<br>

<h4 align="center">Bài thi</h4>
<img width="1919" height="1096" alt="image" src="https://github.com/user-attachments/assets/c418c580-350d-4b83-ae18-bb57d37b885c" />
<br>

<h4 align="center">Người dùng</h4>
<img width="1919" height="1101" alt="image" src="https://github.com/user-attachments/assets/c1474d0c-3f02-4964-9c6c-283a898ab5c4" />
<br>

<h4 align="center">Nhóm quyền</h4>
<img width="1919" height="1097" alt="image" src="https://github.com/user-attachments/assets/48b07289-78d5-465a-8ac4-d710ea792468" />
<br>

<h4 align="center">Thống kê</h4>
<img width="1918" height="1104" alt="image" src="https://github.com/user-attachments/assets/5fb16e85-9a00-4965-8a70-4cf9f66ec409" />
<br>

<h4 align="center">Giao diện Sinh viên làm bài thi</h4>
<img width="1919" height="1106" alt="image" src="https://github.com/user-attachments/assets/74cd3827-1fc4-45fa-b0d5-2bf24f92a9df" />
<img width="1477" height="919" alt="image" src="https://github.com/user-attachments/assets/c76aabae-081e-45c8-8ff6-53d2a1c982da" />
<img width="1480" height="924" alt="image" src="https://github.com/user-attachments/assets/4bb24130-d427-4cb1-9b3f-9368510dae00" />

<i><b>Phím tắt: </b></i> 
- Bấm `enter` để chuyển sang câu kế tiếp
- Đối với trắc nghiệm bấm `1, 2, 3, 4` tương ứng với câu `A, B, C, D`
<br>

<h4 align="center">Giao diện Giảng viên tạo đề thi</h4>
<img width="1417" height="862" alt="image" src="https://github.com/user-attachments/assets/5ee7706d-ef15-4a06-8b97-013c55958d6f" />
<br>
  
