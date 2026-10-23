package GUI.Panel;

import BUS.CauHoiBUS;
import BUS.DapAnBUS;
import BUS.DoKhoBUS;
import BUS.LoaiCauHoiBUS;
import BUS.MonHocBUS;
import BUS.NguoiDungBUS;
import DTO.CauHoiDTO;
import DTO.DapAnDTO;
import DTO.DoKhoDTO;
import DTO.LoaiCauHoiDTO;
import DTO.MonHocDTO;
import GUI.Component.IntegratedSearch;
import GUI.Component.MainFunction;
import GUI.Component.PaginatedTable;
import GUI.Component.PanelBorderRadius;
import GUI.Component.TableSorter;
import GUI.Dialog.CauHoiDialog;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.swing.filechooser.FileNameExtensionFilter;

public class CauHoi extends JPanel implements ActionListener, ItemListener {

    PanelBorderRadius pnlMain, functionBar;
    private GUI.Main mainFrame;
    JPanel pnlBorder1, pnlBorder2, pnlBorder3, pnlBorder4, contentCenter;
    JTable table;
    PaginatedTable paginatedTable;
    MainFunction mainFunction;
    IntegratedSearch search;

    private final NguoiDungBUS ndBUS = new NguoiDungBUS();
    private final CauHoiBUS bus = new CauHoiBUS();
    private final MonHocBUS monHocBUS = new MonHocBUS();
    private final DoKhoBUS doKhoBUS = new DoKhoBUS();
    private final LoaiCauHoiBUS loaiCauHoiBUS = new LoaiCauHoiBUS();

    private ArrayList<CauHoiDTO> listHienTai = bus.getAll();
    private final Color backgroundColor = new Color(240, 247, 250);

    public CauHoi(GUI.Main mainFrame) {
        this.mainFrame = mainFrame;
        initComponent();
        loadDataTable(listHienTai);
    }

    private void initComponent() {
        this.setBackground(backgroundColor);
        this.setLayout(new BorderLayout(0, 0));
        this.setOpaque(true);

        String[] header = {"Mã CH", "Nội dung câu hỏi", "Độ khó", "Loại", "Môn học", "Người tạo"};
        paginatedTable = new PaginatedTable(header);
        table = paginatedTable.getTable();

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.setFocusable(false);
        table.setRowHeight(40);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i == 1) {
                table.getColumnModel().getColumn(i).setCellRenderer(new MultiLineTableCellRenderer());
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(450);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(150);

        table.setAutoCreateRowSorter(false);
        Comparator<Object>[] comps = new Comparator[6];
        comps[0] = (Object o1, Object o2) -> {
            int id1 = Integer.parseInt(o1.toString().replace("CH-", ""));
            int id2 = Integer.parseInt(o2.toString().replace("CH-", ""));
            return Integer.compare(id1, id2);
        };
        comps[1] = TableSorter.STRING_COMPARATOR;
        comps[2] = TableSorter.STRING_COMPARATOR;
        comps[3] = TableSorter.STRING_COMPARATOR;
        comps[4] = TableSorter.STRING_COMPARATOR;
        comps[5] = TableSorter.STRING_COMPARATOR;
        paginatedTable.enableFullDataSorting(comps);

        pnlBorder1 = new JPanel();
        pnlBorder1.setPreferredSize(new Dimension(0, 10));
        pnlBorder1.setBackground(backgroundColor);
        pnlBorder2 = new JPanel();
        pnlBorder2.setPreferredSize(new Dimension(0, 10));
        pnlBorder2.setBackground(backgroundColor);
        pnlBorder3 = new JPanel();
        pnlBorder3.setPreferredSize(new Dimension(10, 0));
        pnlBorder3.setBackground(backgroundColor);
        pnlBorder4 = new JPanel();
        pnlBorder4.setPreferredSize(new Dimension(10, 0));
        pnlBorder4.setBackground(backgroundColor);

        this.add(pnlBorder1, BorderLayout.NORTH);
        this.add(pnlBorder2, BorderLayout.SOUTH);
        this.add(pnlBorder3, BorderLayout.EAST);
        this.add(pnlBorder4, BorderLayout.WEST);

        contentCenter = new JPanel(new BorderLayout(10, 10));
        contentCenter.setBackground(backgroundColor);
        this.add(contentCenter, BorderLayout.CENTER);

        functionBar = new PanelBorderRadius();
        functionBar.setPreferredSize(new Dimension(0, 100));
        functionBar.setLayout(new GridLayout(1, 2, 50, 0));
        functionBar.setBorder(new EmptyBorder(10, 10, 10, 10));
        functionBar.setBackground(Color.WHITE);

        String[] action = {"create", "update", "delete", "detail", "import", "export", "importword"};
        mainFunction = new MainFunction(mainFrame.getNguoiDung().getManhomquyen(), "1", action);
        for (String ac : action) {
            mainFunction.btn.get(ac).addActionListener(this);
        }
        functionBar.add(mainFunction);

        search = new IntegratedSearch(new String[]{"Tất cả", "Mã câu hỏi", "Nội dung", "Người tạo"});
        search.txtSearchForm.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                thucHienTimKiem();
            }
        });
        search.cbxChoose.addItemListener(this);
        search.btnReset.addActionListener(e -> {
            search.txtSearchForm.setText("");
            search.cbxChoose.setSelectedIndex(0);
            listHienTai = bus.getAll();
            loadDataTable(listHienTai);
        });
        functionBar.add(search);

        contentCenter.add(functionBar, BorderLayout.NORTH);

        pnlMain = new PanelBorderRadius();
        pnlMain.setLayout(new BorderLayout());
        pnlMain.setBackground(Color.WHITE);
        pnlMain.add(paginatedTable, BorderLayout.CENTER);
        contentCenter.add(pnlMain, BorderLayout.CENTER);

        table.getColumnModel().getColumn(1).addPropertyChangeListener(evt -> {
            if ("width".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(this::adjustRowHeights);
            }
        });

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                SwingUtilities.invokeLater(this::adjustRowHeights);
            }
        });

        // Quan trọng: PaginatedTable tự thay dữ liệu model khi chuyển trang / đổi
        // số dòng mỗi trang / sắp xếp (gọi setRowCount(0) rồi addRow bên trong nó),
        // nên phải lắng nghe trực tiếp trên model để luôn tính lại chiều cao dòng
        // mỗi khi nội dung bảng thay đổi, thay vì chỉ tính khi loadDataTable() ban đầu.
        TableModelListener rowHeightListener = e -> SwingUtilities.invokeLater(this::adjustRowHeights);
        table.getModel().addTableModelListener(rowHeightListener);
    }

    class MultiLineTableCellRenderer extends JTextArea implements TableCellRenderer {

        public MultiLineTableCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setEditable(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            setText(value != null ? value.toString() : "");

            int width = table.getColumnModel().getColumn(column).getWidth();
            if (width > 0) {
                setSize(new Dimension(width, Short.MAX_VALUE));
            }
            return this;
        }
    }

    private void adjustRowHeights() {
        if (!table.isShowing()) return;

        for (int row = 0; row < table.getRowCount(); row++) {
            int maxHeight = 40;
            TableCellRenderer renderer = table.getCellRenderer(row, 1);
            Component comp = table.prepareRenderer(renderer, row, 1);
            int width = table.getColumnModel().getColumn(1).getWidth();
            comp.setSize(width, Short.MAX_VALUE);
            maxHeight = Math.max(maxHeight, comp.getPreferredSize().height);

            if (table.getRowHeight(row) != maxHeight) {
                table.setRowHeight(row, maxHeight);
            }
        }
    }

    public void thucHienTimKiem() {
        String kieu = (String) search.cbxChoose.getSelectedItem();
        String text = search.txtSearchForm.getText();
        listHienTai = bus.search(text, kieu);
        loadDataTable(listHienTai);
    }

    public void loadDataTable(ArrayList<CauHoiDTO> danhSach) {
        this.listHienTai = danhSach;
        List<Object[]> data = new ArrayList<>();
        for (CauHoiDTO ch : danhSach) {
            data.add(new Object[]{
                "CH-" + ch.getMacauhoi(),
                ch.getNoidung(),
                doKhoBUS.getTenDoKho(ch.getMadokho()),
                loaiCauHoiBUS.getTenById(ch.getMaloai()),
                monHocBUS.getTenById(ch.getMamonhoc()),
                ndBUS.getHotenById(ch.getNguoitao())
            });
        }
        paginatedTable.setData(data);
        SwingUtilities.invokeLater(this::adjustRowHeights);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        Object source = e.getSource();

        if (source == mainFunction.btn.get("create")) {
            CauHoiDTO newCH = new CauHoiDTO();
            newCH.setNguoitao(mainFrame.getNguoiDung().getManguoidung());
            new CauHoiDialog(this, owner, "Thêm câu hỏi mới", newCH);
        } else if (source == mainFunction.btn.get("update") || source == mainFunction.btn.get("detail") || source == mainFunction.btn.get("delete")) {
            int index = table.getSelectedRow();
            if (index == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn câu hỏi!");
                return;
            }
            String maStr = table.getValueAt(index, 0).toString();
            int macauhoi = Integer.parseInt(maStr.replace("CH-", ""));
            bus.getAll();
            CauHoiDTO selected = bus.getById(macauhoi);

            if (source == mainFunction.btn.get("update")) {
                new CauHoiDialog(this, owner, "Chỉnh sửa câu hỏi", selected);
            } else if (source == mainFunction.btn.get("detail")) {
                new CauHoiDialog(this, owner, "Thông tin chi tiết câu hỏi", selected);
            } else if (source == mainFunction.btn.get("delete")) {
                if (JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa câu hỏi mã " + macauhoi + "?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if (bus.delete(macauhoi)) {
                        JOptionPane.showMessageDialog(this, "Xóa thành công!");
                        listHienTai = bus.getAll();
                        loadDataTable(listHienTai);
                    }
                }
            }
        } else if (source == mainFunction.btn.get("import")) {
            importExcel();
        } else if (source == mainFunction.btn.get("export")) {
            exportExcel();
        } else if (source == mainFunction.btn.get("importword")) {
            importWord();
        }
    }

    public void importWord() {
        JFileChooser jf = new JFileChooser();
        jf.setFileFilter(new FileNameExtensionFilter("Word Documents (.doc, .docx)", "doc", "docx"));
        jf.setAcceptAllFileFilterUsed(false);
        int result = jf.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = jf.getSelectedFile();

        JPanel pnlConfig = new JPanel(new GridLayout(3, 2, 10, 10));
        pnlConfig.setBorder(new EmptyBorder(10, 10, 10, 10));

        JComboBox<MonHocDTO> cbMonHoc = new JComboBox<>(new DefaultComboBoxModel<>(monHocBUS.getAll().toArray(new MonHocDTO[0])));
        JComboBox<DoKhoDTO> cbDoKho = new JComboBox<>(new DefaultComboBoxModel<>(doKhoBUS.getAll().toArray(new DoKhoDTO[0])));
        JComboBox<LoaiCauHoiDTO> cbLoai = new JComboBox<>(new DefaultComboBoxModel<>(loaiCauHoiBUS.getAll().toArray(new LoaiCauHoiDTO[0])));

        cbMonHoc.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value == null ? "" : value.getTenmonhoc()));
        cbDoKho.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value == null ? "" : value.getTendokho()));
        cbLoai.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value == null ? "" : value.getTenloai()));

        pnlConfig.add(new JLabel("Chọn môn học:"));
        pnlConfig.add(cbMonHoc);
        pnlConfig.add(new JLabel("Chọn độ khó:"));
        pnlConfig.add(cbDoKho);
        pnlConfig.add(new JLabel("Chọn loại CH:"));
        pnlConfig.add(cbLoai);

        int configResult = JOptionPane.showConfirmDialog(this, pnlConfig, "Cấu hình thông tin nhập câu hỏi",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (configResult != JOptionPane.OK_OPTION) {
            return;
        }

        if (cbMonHoc.getSelectedItem() == null || cbDoKho.getSelectedItem() == null || cbLoai.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Cần có môn học, độ khó và loại câu hỏi trước khi nhập.");
            return;
        }
        int selectedMonHoc = ((MonHocDTO) cbMonHoc.getSelectedItem()).getMamonhoc();
        int selectedDoKho = ((DoKhoDTO) cbDoKho.getSelectedItem()).getMadokho();
        int selectedLoai = ((LoaiCauHoiDTO) cbLoai.getSelectedItem()).getMaloai();

        try {
            var questions = helper.WordQuestionParser.parse(file);
            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy văn bản câu hỏi trong file Word.");
                return;
            }
            if (!GUI.Dialog.WordImportPreview.edit(this, questions, selectedLoai)) return;
            int success = 0;
            java.util.List<String> failures = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                var question = questions.get(i);
                CauHoiDTO ch = new CauHoiDTO(0, question.text, selectedDoKho, selectedLoai,
                        selectedMonHoc, mainFrame.getNguoiDung().getManguoidung(), 1);
                ArrayList<DapAnDTO> answers = new ArrayList<>();
                for (int j = 0; j < question.answers.size(); j++) {
                    answers.add(new DapAnDTO(0, 0, question.answers.get(j), question.correct.contains(j)));
                }
                try {
                    bus.importWithAnswers(ch, answers);
                    success++;
                } catch (java.sql.SQLException ex) {
                    failures.add("Câu " + (i + 1) + ": " + ex.getMessage());
                }
            }
            listHienTai = bus.getAll();
            loadDataTable(listHienTai);
            JTextArea report = new JTextArea("Nhập thành công " + success + "/" + questions.size()
                    + " câu hỏi.\n" + String.join("\n", failures), 12, 65);
            report.setEditable(false);
            report.setLineWrap(true);
            report.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this, new JScrollPane(report), "Kết quả nhập Word",
                    failures.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi đọc file Word: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void importExcel() {
        JFileChooser jf = new JFileChooser();
        int result = jf.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File excelFile = jf.getSelectedFile();
                FileInputStream excelFIS = new FileInputStream(excelFile);
                BufferedInputStream excelBIS = new BufferedInputStream(excelFIS);
                XSSFWorkbook excelJTableImport = new XSSFWorkbook(excelBIS);
                XSSFSheet excelSheet = excelJTableImport.getSheetAt(0);
                org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();

                int countSuccess = 0, countError = 0;
                DapAnBUS daBUS = new DapAnBUS();

                for (int row = 1; row <= excelSheet.getLastRowNum(); row++) {
                    XSSFRow excelRow = excelSheet.getRow(row);
                    if (excelRow == null) {
                        continue;
                    }

                    try {
                        String noidung = formatter.formatCellValue(excelRow.getCell(0)).trim();
                        String tenDoKho = formatter.formatCellValue(excelRow.getCell(1)).trim();
                        String tenLoai = formatter.formatCellValue(excelRow.getCell(2)).trim();
                        String tenMonHoc = formatter.formatCellValue(excelRow.getCell(3)).trim();

                        if (noidung.isEmpty()) {
                            continue;
                        }

                        int madokho = -1;
                        for (var dk : doKhoBUS.getAll()) {
                            if (dk.getTendokho().equalsIgnoreCase(tenDoKho)) {
                                madokho = dk.getMadokho();
                                break;
                            }
                        }
                        int maloai = -1;
                        for (var l : loaiCauHoiBUS.getAll()) {
                            if (l.getTenloai().equalsIgnoreCase(tenLoai) || l.getTenloai().replace("/", " ").equalsIgnoreCase(tenLoai)) {
                                maloai = l.getMaloai();
                                break;
                            }
                        }
                        int mamonhoc = -1;
                        for (var mh : monHocBUS.getAll()) {
                            if (mh.getTenmonhoc().equalsIgnoreCase(tenMonHoc)) {
                                mamonhoc = mh.getMamonhoc();
                                break;
                            }
                        }
                        if (madokho == -1 || maloai == -1 || mamonhoc == -1) {
                            System.out.println("Lỗi mapping tại dòng " + row + ":");
                            System.out.println("- Độ khó: " + tenDoKho + " -> " + (madokho != -1 ? "OK" : "FAILED"));
                            System.out.println("- Loại: " + tenLoai + " -> " + (maloai != -1 ? "OK" : "FAILED"));
                            System.out.println("- Môn học: " + tenMonHoc + " -> " + (mamonhoc != -1 ? "OK" : "FAILED"));
                        }
                        if (madokho != -1 && maloai != -1 && mamonhoc != -1) {
                            CauHoiDTO ch = new CauHoiDTO();
                            ch.setNoidung(noidung);
                            ch.setMadokho(madokho);
                            ch.setMaloai(maloai);
                            ch.setMamonhoc(mamonhoc);
                            ch.setNguoitao(mainFrame.getNguoiDung().getManguoidung());
                            ch.setTrangthai(1);

                            int generatedId = bus.addReturnId(ch);

                            if (generatedId != -1) {
                                String loaiLower = tenLoai.toLowerCase();

                                if (loaiLower.contains("trắc")) {
                                    String correctChar = formatter.formatCellValue(excelRow.getCell(8)).trim().toUpperCase();
                                    for (int i = 0; i < 4; i++) {
                                        String textDA = formatter.formatCellValue(excelRow.getCell(4 + i)).trim();
                                        if (!textDA.isEmpty()) {
                                            char label = (char) ('A' + i);
                                            daBUS.add(new DapAnDTO(0, generatedId, textDA, String.valueOf(label).equals(correctChar)));
                                        }
                                    }
                                } else if (loaiLower.contains("đúng")) {
                                    String correctText = formatter.formatCellValue(excelRow.getCell(8)).trim();
                                    daBUS.add(new DapAnDTO(0, generatedId, "Đúng", correctText.equalsIgnoreCase("Đúng")));
                                    daBUS.add(new DapAnDTO(0, generatedId, "Sai", correctText.equalsIgnoreCase("Sai")));
                                } else if (loaiLower.contains("điền")) {
                                    String fillText = formatter.formatCellValue(excelRow.getCell(4)).trim();
                                    if (!fillText.isEmpty()) {
                                        daBUS.add(new DapAnDTO(0, generatedId, fillText, true));
                                    }
                                }

                                countSuccess++;
                            } else {
                                countError++;
                            }
                        } else {
                            countError++;
                        }
                    } catch (Exception e) {
                        countError++;
                    }
                }
                JOptionPane.showMessageDialog(this, "Nhập thành công " + countSuccess + " câu hỏi. Lỗi " + countError + " dòng.");
                listHienTai = bus.getAll();
                loadDataTable(listHienTai);
                excelJTableImport.close();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi đọc file Excel!");
            }
        }
    }

    public void exportExcel() {
        try {
            helper.JTableExporter.exportJTableToExcel(table);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Xuất file Excel thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            thucHienTimKiem();
        }
    }
}
