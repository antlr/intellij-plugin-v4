package org.antlr.intellij.plugin.profiler;

import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.ParseInfo;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;

public class SimpleProfilerTableDataModel extends ProfilerTableDataModel {
	public ParseInfo parseInfo;
    public LinkedHashMap<String, Integer> nameToColumnMap = new LinkedHashMap<String, Integer>();
    public static final String[] columnNames = {
            "Invocations", "Time", "Total k", "Max k", "Ambiguities", "DFA cache miss"
    };

    public static final String[] columnToolTips = {
        "# decision invocations",
		"Rough estimate of time (ms) spent in prediction",
		"Total lookahead symbols examined",
		"Max lookahead symbols examined in any decision event",
		"# of ambiguous input phrases",
		"# of non-DFA transitions during prediction (cache miss)"
    };

	// microsecond decimal precision
	private NumberFormat milliUpToMicroFormatter = new DecimalFormat("#.###");

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
	public String[] getColumnToolTips() {
		return columnToolTips;
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
				return milliUpToMicroFormatter.format(decisionInfo.timeInPrediction / (1000.0 * 1000.0));
			case 2:
				return decisionInfo.LL_TotalLook+decisionInfo.SLL_TotalLook;
			case 3:
				return Math.max(decisionInfo.LL_MaxLook, decisionInfo.SLL_MaxLook);
			case 4:
				return decisionInfo.ambiguities.size();
			case 5:
				return decisionInfo.SLL_ATNTransitions+
					decisionInfo.LL_ATNTransitions;
		}
		return "n/a";
	}
}
