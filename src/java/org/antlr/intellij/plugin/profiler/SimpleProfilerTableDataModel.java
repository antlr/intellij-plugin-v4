package org.antlr.intellij.plugin.profiler;

import org.antlr.v4.runtime.atn.ParseInfo;

import javax.swing.table.AbstractTableModel;
import java.util.LinkedHashMap;

public class SimpleProfilerTableDataModel extends AbstractTableModel {
    public ParseInfo parseInfo;
    public LinkedHashMap<String, Integer> nameToColumnMap = new LinkedHashMap<String, Integer>();
    public static final String[] columnNames = {
            "Invocations", "Time (ms)", "Total k", "Max k"
    };

    public static final String[] columnToolTips = {
            "Invocations", "Time (ms)", "Total k", "Max k"
    };

    public SimpleProfilerTableDataModel(ParseInfo parseInfo) {
        this.parseInfo = parseInfo;
        for (int i = 0; i < columnNames.length; i++) {
            nameToColumnMap.put(columnNames[i], i);
        }
    }

    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Integer.class;
    }

    public int getRowCount() {
        return parseInfo.getDecisionInfo().length;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public Object getValueAt(int row, int col) {
        int decision = row;
        switch (col) { // laborious but more efficient than reflection
            case 0:
                return parseInfo.getDecisionInfo()[decision].invocations;
            case 1:
                return (int) (parseInfo.getDecisionInfo()[decision].timeInPrediction / 1000.0 / 1000.0);
            case 2:
                return parseInfo.getDecisionInfo()[decision].totalLook;
            case 3:
                return parseInfo.getDecisionInfo()[decision].maxLook;
        }
        return "n/a";
    }
}