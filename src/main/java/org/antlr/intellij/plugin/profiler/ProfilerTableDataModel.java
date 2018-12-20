package org.antlr.intellij.plugin.profiler;

import javax.swing.table.AbstractTableModel;

public abstract class ProfilerTableDataModel extends AbstractTableModel {

	public abstract String[] getColumnNames();
	public abstract String[] getColumnToolTips();

	@Override
    public String getColumnName(int column) {
        return getColumnNames()[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Integer.class;
    }

	@Override
    public int getColumnCount() {
        return getColumnNames().length;
    }
}
