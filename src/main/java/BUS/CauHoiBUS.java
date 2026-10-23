package BUS;

import DAO.CauHoiDAO;
import DTO.CauHoiDTO;
import java.util.ArrayList;

public class CauHoiBUS {

    private final CauHoiDAO dao = CauHoiDAO.getInstance();
    private final NguoiDungBUS ndBUS = new NguoiDungBUS();
    private ArrayList<CauHoiDTO> listCauHoi;

    public CauHoiBUS() {
        this.listCauHoi = dao.selectAll();
    }

    public ArrayList<CauHoiDTO> getAll() {
        this.listCauHoi = dao.selectAll();
        return this.listCauHoi;
    }

    public boolean add(CauHoiDTO ch) {
        if (dao.checkTrungNoiDung(ch.getNoidung(), -1)) {
            return false;
        }
        boolean check = dao.insert(ch) > 0;
        if (check) {
            getAll();
        }
        return check;
    }

    public boolean update(CauHoiDTO ch) {
        if (dao.checkTrungNoiDung(ch.getNoidung(), ch.getMacauhoi())) {
            return false;
        }
        boolean check = dao.update(ch) > 0;
        if (check) {
            getAll();
        }
        return check;
    }

    public boolean delete(int macauhoi) {
        boolean check = dao.delete(macauhoi) > 0;
        if (check) {
            getAll();
        }
        return check;
    }

    public CauHoiDTO getById(int macauhoi) {
        for (CauHoiDTO ch : listCauHoi) {
            if (ch.getMacauhoi() == macauhoi) {
                return ch;
            }
        }
        return dao.selectById(macauhoi);
    }

    public ArrayList<CauHoiDTO> search(String text, String type) {
        ArrayList<CauHoiDTO> result = new ArrayList<>();
        text = text.toLowerCase().trim();
        for (CauHoiDTO ch : this.listCauHoi) {
            boolean match = false;

            String tenNguoiTao = ndBUS.getHotenById(ch.getNguoitao());
            if (tenNguoiTao == null) {
                tenNguoiTao = "";
            }
            String tenNguoiTaoLower = tenNguoiTao.toLowerCase();

            switch (type) {
                case "Tất cả" ->
                    match = Integer.toString(ch.getMacauhoi()).contains(text)
                            || ch.getNoidung().toLowerCase().contains(text)
                            || tenNguoiTaoLower.contains(text);
                case "Mã câu hỏi" ->
                    match = Integer.toString(ch.getMacauhoi()).contains(text);
                case "Nội dung" ->
                    match = ch.getNoidung().toLowerCase().contains(text);
                case "Người tạo" ->
                    match = tenNguoiTaoLower.contains(text);
            }
            if (match) {
                result.add(ch);
            }
        }
        return result;
    }

    public int addReturnId(CauHoiDTO ch) {
        if (dao.checkTrungNoiDung(ch.getNoidung(), -1)) {
            return -1;
        }
        int id = dao.insertReturnId(ch);
        if (id != -1) {
            getAll();
        }
        return id;
    }
    public void importWithAnswers(CauHoiDTO question, java.util.List<DTO.DapAnDTO> answers) throws java.sql.SQLException {
        helper.WordImportValidation.validate(question.getNoidung(), answers.stream().map(DTO.DapAnDTO::getNoidungtl).toList(),
                (int) answers.stream().filter(DTO.DapAnDTO::getLadapan).count(), question.getMaloai());
        dao.insertWithAnswers(question, answers);
    }
}
