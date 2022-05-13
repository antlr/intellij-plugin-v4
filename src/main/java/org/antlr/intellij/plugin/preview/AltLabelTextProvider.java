package org.antlr.intellij.plugin.preview;

import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.v4.gui.TreeTextProvider;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.AltAST;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AltLabelTextProvider implements TreeTextProvider {
	protected final Parser parser;
	protected final Grammar g;

	public AltLabelTextProvider(Parser parser, Grammar g) {
		this.parser = parser;
		this.g = g;
	}

	public String[] getAltLabels(Rule r) {
		String[] altLabels = null;
		Map<String, List<Pair<Integer, AltAST>>> altLabelsMap = r.getAltLabels();
		if ( altLabelsMap!=null ) {
			altLabels = new String[r.getOriginalNumberOfAlts() + 1];
			for (String altLabel : altLabelsMap.keySet()) {
				List<Pair<Integer, AltAST>> pairs = altLabelsMap.get(altLabel);
				for (Pair<Integer, AltAST> pair : pairs) {
					altLabels[pair.a] = altLabel;
				}
			}
		}
		return altLabels;
	}

	@Override
	public String getText(Tree node) {
		if ( node instanceof PreviewInterpreterRuleContext) {
			PreviewInterpreterRuleContext inode = (PreviewInterpreterRuleContext)node;
			Rule r = g.getRule(inode.getRuleIndex());
			String[] altLabels = getAltLabels(r);
			String name = r.name;
			int outerAltNum = inode.getOuterAltNum();
			if ( altLabels!=null ) {
				if ( outerAltNum>=0 && outerAltNum<altLabels.length ) {
					return name+":"+altLabels[outerAltNum];
				}
				else {
					return name;
				}
			}
			else if ( r.getOriginalNumberOfAlts()>1 ) {
				return name + ":" +outerAltNum;
			}
			else {
				return name; // don't display an alternative number if there's only one
			}
		}
		else if (node instanceof TerminalNode) {
			return getLabelForToken( ((TerminalNode)node).getSymbol() );
		}
		return Trees.getNodeText(node, Arrays.asList(parser.getRuleNames()));
	}

	private String getLabelForToken(Token token) {
		String text = token.getText();
		if (text.equals("<EOF>")) {
			return text;
		}

		String symbolicName = parser.getVocabulary().getSymbolicName(token.getType());
		if ( symbolicName==null ) { // it's a literal like ';' or 'return'
			return text;
		}
		if ( text.toUpperCase().equals(symbolicName) ) { // IMPORT:import
			return symbolicName;
		}
		return symbolicName + ":" + text;
	}
}
