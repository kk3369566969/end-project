package com.peerreview.ui.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 通用只读表格模型：用「列定义表」描述表格，避免为每个列表各写一个 TableModel。
 *
 * <p>列定义包含标题、首选宽度与取值函数，新增列表只需声明一组列。</p>
 */
public final class SimpleTableModel<T> extends AbstractTableModel {

    /**
     * @param title 表头文字
     * @param width 首选列宽（像素）
     * @param value 行 → 单元格值；可返回 {@code Badge} 以获得胶囊样式
     */
    public record Column<T>(String title, int width, Function<T, Object> value) {
    }

    private final List<Column<T>> columns;
    private List<T> rows = List.of();

    public SimpleTableModel(List<Column<T>> columns) {
        this.columns = new ArrayList<>(columns);
    }

    public void setRows(List<T> rows) {
        this.rows = rows == null ? List.of() : new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public List<T> rows() {
        return List.copyOf(rows);
    }

    public T rowAt(int index) {
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }

    public List<Column<T>> columns() {
        return columns;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).title();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return columns.get(columnIndex).value().apply(rows.get(rowIndex));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
