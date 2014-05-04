package org.antlr.intellij.plugin.parsing;

import org.antlr.runtime.CommonToken;
import org.antlr.v4.automata.ParserATNFactory;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.ast.TerminalAST;

import java.util.HashMap;
import java.util.Map;

public class PluginParserATNFactory extends ParserATNFactory {
	public Map<Integer, Interval> stateToGrammarRegionMap =
		new HashMap<Integer, Interval>();

	public PluginParserATNFactory(@NotNull Grammar g) {
		super(g);
	}

	@Override
	public Handle tokenRef(@NotNull TerminalAST node) {
		Handle h = super.tokenRef(node);

		CommonToken t = (CommonToken)node.getToken();
		stateToGrammarRegionMap.put(h.left.stateNumber,
									Interval.of(t.getStartIndex(), t.getStopIndex()));

		return h;
	}
}
