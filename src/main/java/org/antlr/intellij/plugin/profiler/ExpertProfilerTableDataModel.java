package org.antlr.intellij.plugin.profiler;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.ParseInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;

public class ExpertProfilerTableDataModel extends ProfilerTableDataModel {
    public ParseInfo parseInfo;
    public LinkedHashMap<String, Integer> nameToColumnMap = new LinkedHashMap<>();
    public static final String[] columnNames = {
        "Decision", "Invocations", "Time (ms)", "# DFA states", "LL failover", "Total k",
		"Min SLL k", "Min LL k",
		"Max SLL k", "Max LL k",
        "DFA k", "SLL-ATN k", "LL-ATN k", "Full context", "Ambiguities", "Predicates"
    };

    public static final String[] columnToolTips = {
        "Decision index",
		"# decision invocations",
		"Rough estimate of time (ms) spent in prediction",
		"# DFA states",
		"# of SLL -> LL prediction failovers",
		"Total lookahead symbols examined",
		"Fewest SLL lookahead symbols examined in any decision event",
		"Fewest LL lookahead symbols examined in any decision event",
		"Max SLL lookahead symbols examined in any decision event",
		"Max LL lookahead symbols examined in any decision event",
        "# of DFA transitions during prediction (cache hit)",
		"# of conventional SLL ATN (non-DFA) transitions during prediction (cache miss)",
		"# of full-context LL ATN (non-DFA) transitions during prediction (cache miss)",
		"# of context-sensitive phrases found (not certain to be all)",
		"# of ambiguous input phrases",
		"# of predicate evaluations"
    };

	private final String[] ruleNamesByDecision ;

    public ExpertProfilerTableDataModel(ParseInfo parseInfo, Parser parser) {
        this.parseInfo = parseInfo;
		/*copying rule names to not hold ref to parser object*/
		ruleNamesByDecision  = new String[parser.getATN().decisionToState.size()];
		for(int i = 0; i < ruleNamesByDecision .length; i++) {
			ruleNamesByDecision [i] = parser.getRuleNames()[parser.getATN().getDecisionState(i).ruleIndex];
		}
        for (int i = 0; i < columnNames.length; i++) {
            nameToColumnMap.put(columnNames[i], i);
        }
    }

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
				return  String.format("%s (%d)",ruleNamesByDecision [decision],decision);
            case 1:
                return decisionInfo.invocations;
            case 2:
	            return BigDecimal.valueOf(decisionInfo.timeInPrediction / (1000.0 * 1000.0)).setScale(3, RoundingMode.HALF_DOWN);
            case 3:
                return parseInfo.getDFASize(decision);
            case 4:
				return decisionInfo.LL_Fallback;
			case 5:
				return decisionInfo.LL_TotalLook+decisionInfo.SLL_TotalLook;
			case 6:
				return decisionInfo.SLL_MinLook;
			case 7:
				return decisionInfo.LL_MinLook;
			case 8:
				return decisionInfo.SLL_MaxLook;
			case 9:
				return decisionInfo.LL_MaxLook;
            case 10:
				return decisionInfo.SLL_DFATransitions;
            case 11:
                return decisionInfo.SLL_ATNTransitions;
			case 12:
				return decisionInfo.LL_ATNTransitions;
			case 13:
				return decisionInfo.contextSensitivities.size();
			case 14:
				return decisionInfo.ambiguities.size();
			case 15:
				return decisionInfo.predicateEvals.size();
		}
		return "n/a";
	}
}
