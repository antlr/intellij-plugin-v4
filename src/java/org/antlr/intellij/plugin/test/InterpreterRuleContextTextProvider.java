package org.antlr.intellij.plugin.test;

import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.runtime.tree.gui.TreeTextProvider;

import java.util.Arrays;
import java.util.List;

public class InterpreterRuleContextTextProvider implements TreeTextProvider {
	public List<String> ruleNames;
	public InterpreterRuleContextTextProvider(String[] ruleNames) {this.ruleNames = Arrays.asList(ruleNames);}

	@Override
	public String getText(Tree node) {
		if ( node==null ) return "null";
		String nodeText = Trees.getNodeText(node, ruleNames);
		if ( node instanceof PreviewInterpreterRuleContext) {
			PreviewInterpreterRuleContext ctx = (PreviewInterpreterRuleContext) node;
			return nodeText+":"+ctx.getOuterAltNum();
		}
		if ( node instanceof ErrorNode) {
			return "<error "+nodeText+">";
		}
		return nodeText;
	}
}
