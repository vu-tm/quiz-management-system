package DAO;

import DTO.CauHoiDTO;
import config.JDBCUtil;
import java.sql.*;
import java.util.ArrayList;

public class CauHoiDAO {

    public static CauHoiDAO getInstance() {
        return new CauHoiDAO();
    }

    public ArrayList<CauHoiDTO> selectAll() {
        ArrayList<CauHoiDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM cauhoi WHERE trangthai = 1 ORDER BY macauhoi DESC";
        try (Connection c = JDBCUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CauHoiDTO ch = new CauHoiDTO(
                        rs.getInt("macauhoi"),
                        rs.getString("noidung"),
                        rs.getInt("madokho"),
                        rs.getInt("maloai"),
                        rs.getInt("mamonhoc"),
                        rs.getInt("nguoitao"),
                        rs.getInt("trangthai")
                );
                list.add(ch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int insert(CauHoiDTO ch) {
        int result = 0;
        String sql = "INSERT INTO cauhoi(noidung, madokho, maloai, mamonhoc, nguoitao, trangthai) VALUES(?,?,?,?,?,?)";
        try (Connection c = JDBCUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ch.getNoidung());
            ps.setInt(2, ch.getMadokho());
            ps.setInt(3, ch.getMaloai());
            ps.setInt(4, ch.getMamonhoc());
            ps.setInt(5, ch.getNguoitao());
            ps.setInt(6, 1);
            result = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public int update(CauHoiDTO ch) {
        int result = 0;
        String sql = "UPDATE cauhoi SET noidung = ?, madokho = ?, maloai = ?, mamonhoc = ?, nguoitao = ?, trangthai = ? WHERE macauhoi = ?";
        try (Connection c = JDBCUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ch.getNoidung());
            ps.setInt(2, ch.getMadokho());
            ps.setInt(3, ch.getMaloai());
            ps.setInt(4, ch.getMamonhoc());
            ps.setInt(5, ch.getNguoitao());
            ps.setInt(6, ch.getTrangthai());
            ps.setInt(7, ch.getMacauhoi());
            result = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public int delete(int macauhoi) {
        int result = 0;
        String sql = "UPDATE cauhoi SET trangthai = 0 WHERE macauhoi = ?";
        try (Connection c = JDBCUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, macauhoi);
            result = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean checkTrungNoiDung(String noidung, int excludeId) {
        String sql = "SELECT COUNT(*) FROM cauhoi WHERE LOWER(noidung) = ? AND trangthai = 1 AND macauhoi != ?";
        try (Connection c = JDBCUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, noidung.toLowerCase().trim());
            ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public CauHoiDTO selectById(int id) {
        CauHoiDTO ch = null;
        String sql = "SELECT * FROM cauhoi WHERE macauhoi = ?";
        try (Connection c = JDBCUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ch = new CauHoiDTO(
                        rs.getInt("macauhoi"),
                        rs.getString("noidung"),
                        rs.getInt("madokho"),
                        rs.getInt("maloai"),
                        rs.getInt("mamonhoc"),
                        rs.getInt("nguoitao"),
                        rs.getInt("trangthai")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ch;
    }

    public int insertReturnId(CauHoiDTO ch) {
        int lastId = -1;
        String sql = "INSERT INTO cauhoi(noidung, madokho, maloai, mamonhoc, nguoitao, trangthai) VALUES(?,?,?,?,?,?)";
        try (Connection c = config.JDBCUtil.getConnection(); java.sql.PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ch.getNoidung());
            ps.setInt(2, ch.getMadokho());
            ps.setInt(3, ch.getMaloai());
            ps.setInt(4, ch.getMamonhoc());
            ps.setInt(5, ch.getNguoitao());
            ps.setInt(6, 1);
            ps.executeUpdate();

            java.sql.ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                lastId = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lastId;
    }
    /** One question and all its answers are committed together. */
    public void insertWithAnswers(CauHoiDTO question, java.util.List<DTO.DapAnDTO> answers) throws SQLException {
        try (Connection connection = JDBCUtil.getConnection()) {
            if (connection == null) throw new SQLException("Không kết nối được database.");
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement duplicate = connection.prepareStatement(
                        "SELECT macauhoi FROM cauhoi WHERE LOWER(TRIM(noidung)) = LOWER(?) AND trangthai = 1")) {
                    duplicate.setString(1, question.getNoidung().trim());
                    try (ResultSet rs = duplicate.executeQuery()) {
                        if (rs.next()) throw new SQLException("Nội dung câu hỏi đã tồn tại.");
                    }
                }
                int id;
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO cauhoi(noidung, madokho, maloai, mamonhoc, nguoitao, trangthai) VALUES(?,?,?,?,?,1)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, question.getNoidung().trim());
                    ps.setInt(2, question.getMadokho());
                    ps.setInt(3, question.getMaloai());
                    ps.setInt(4, question.getMamonhoc());
                    ps.setInt(5, question.getNguoitao());
                    if (ps.executeUpdate() != 1) throw new SQLException("Không lưu được câu hỏi.");
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không lấy được mã câu hỏi.");
                        id = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dapan(macauhoi, noidungtl, ladapan) VALUES(?,?,?)")) {
                    for (DTO.DapAnDTO answer : answers) {
                        ps.setInt(1, id);
                        ps.setString(2, answer.getNoidungtl().trim());
                        ps.setBoolean(3, answer.getLadapan());
                        if (ps.executeUpdate() != 1) throw new SQLException("Không lưu được đáp án.");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }
}
