package org.antlr.intellij.plugin.profiler;

import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.ParseInfo;

import java.util.LinkedHashMap;

public class SimpleProfilerTableDataModel extends ProfilerTableDataModel {
    public ParseInfo parseInfo;
    public LinkedHashMap<String, Integer> nameToColumnMap = new LinkedHashMap<String, Integer>();
    public static final String[] columnNames = {
            "Invocations", "Time", "Total k", "Max k", "Ambiguities", "DFA cache miss"
    };

    public static final String[] columnToolTips = {
            "Invocations", "Time", "Total k", "Max k", "Ambiguities", "DFA cache miss"
    };

    public SimpleProfilerTableDataModel(ParseInfo parseInfo) {
        this.parseInfo = parseInfo;
        for (int i = 0; i < columnNames.length; i++) {
            nameToColumnMap.put(columnNames[i], i);
        }
    }

	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	@Override
    public int getRowCount() {
        return parseInfo.getDecisionInfo().length;
    }

	@Override
    public Object getValueAt(int row, int col) {
        int decision = row;
		DecisionInfo decisionInfo = parseInfo.getDecisionInfo()[decision];
		switch (col) { // laborious but more efficient than reflection
            case 0:
				return decisionInfo.invocations;
			case 1:
				return (int) (decisionInfo.timeInPrediction / 1000.0 / 1000.0);
			case 2:
				return decisionInfo.totalLook;
			case 3:
				return decisionInfo.maxLook;
			case 4:
				return decisionInfo.ambiguities.size();
			case 5:
				return decisionInfo.SLL_ATNTransitions+
					decisionInfo.LL_ATNTransitions;
		}
		return "n/a";
	}
}