package org.antlr.intellij.plugin.profiler;

import org.antlr.v4.runtime.atn.ParseInfo;

import javax.swing.table.AbstractTableModel;
import java.util.LinkedHashMap;

public class ExpertProfilerTableDataModel extends AbstractTableModel {
    public ParseInfo parseInfo;
    public LinkedHashMap<String, Integer> nameToColumnMap = new LinkedHashMap<String, Integer>();
    public static final String[] columnNames = {
        "Decision", "Invocations", "Time (ms)", "# DFA states", "Full context", "Total k", "Min k", "Max k",
        "DFA k", "SLL-ATN k", "LL-ATN k", "Predicates"
    };

    public static final String[] columnToolTips = {
        "Decision", "Invocations", "Time (ms)", "# DFA states", "Full context", "Total k", "Min k", "Max k",
        "DFA k", "SLL-ATN k", "LL-ATN k", "Predicates"
    };

    public ExpertProfilerTableDataModel(ParseInfo parseInfo) {
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
                return parseInfo.getDecisionInfo()[decision].decision;
            case 1:
                return parseInfo.getDecisionInfo()[decision].invocations;
            case 2:
                return (int) (parseInfo.getDecisionInfo()[decision].timeInPrediction / 1000.0 / 1000.0);
            case 3:
                return parseInfo.getDFASize(decision);
            case 4:
                return parseInfo.getDecisionInfo()[decision].LL_Fallback;
            case 5:
                return parseInfo.getDecisionInfo()[decision].totalLook;
            case 6:
                return parseInfo.getDecisionInfo()[decision].minLook;
            case 7:
                return parseInfo.getDecisionInfo()[decision].maxLook;
            case 8:
                return parseInfo.getDecisionInfo()[decision].DFATransitions;
            case 9:
                return parseInfo.getDecisionInfo()[decision].SLL_ATNTransitions;
            case 10:
                return parseInfo.getDecisionInfo()[decision].LL_ATNTransitions;
            case 11:
                return parseInfo.getDecisionInfo()[decision].predicateEvals.size();
        }
        return "n/a";
    }
}
