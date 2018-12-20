package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.runtime.InterpreterRuleContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.tool.GrammarInterpreterRuleContext;

/**
 * This class extends {@link InterpreterRuleContext} to track
 * which outermost alternative was used to recognize
 * the subphrase matched by the entire rule.
 */
public class PreviewInterpreterRuleContext extends GrammarInterpreterRuleContext {
	/** Used to mark root of subtree that hits the decision override, if any */
	protected boolean isDecisionOverrideRoot;

	/** A mark bit used during tree diff walk. If marked, then we reached
	 *  this node.
	 */
	public boolean reached;

	/**
	 * Constructs a new {@link InterpreterRuleContext} with the specified
	 * parent, invoking state, and rule index.
	 *
	 * @param parent The parent context.
	 * @param invokingStateNumber The invoking state number.
	 * @param ruleIndex The rule index for the current context.
	 */
	public PreviewInterpreterRuleContext(ParserRuleContext parent,
										 int invokingStateNumber,
										 int ruleIndex)
	{
		super(parent, invokingStateNumber, ruleIndex);
	}

	/** The predicted outermost alternative for the rule associated
	 *  with this context object.  If left recursive, the true original
	 *  outermost alternative is returned.
	 */
	public int getOuterAltNum() { return outerAltNum; }

	public boolean isDecisionOverrideRoot() {
		return isDecisionOverrideRoot;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !(obj instanceof PreviewInterpreterRuleContext) ) return false;
		PreviewInterpreterRuleContext other = (PreviewInterpreterRuleContext) obj;
		return this==other ||
			   (ruleIndex == other.ruleIndex && outerAltNum == other.outerAltNum);
	}

	@Override
	public int hashCode() {
		return ruleIndex << 7 + outerAltNum;
	}
}
