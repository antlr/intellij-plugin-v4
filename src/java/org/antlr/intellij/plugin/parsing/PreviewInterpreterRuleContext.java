package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.runtime.InterpreterRuleContext;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * This class extends {@link InterpreterRuleContext} to track
 * which outermost alternative was used to recognize
 * the subphrase matched by the entire rule.
 */
public class PreviewInterpreterRuleContext extends InterpreterRuleContext {
	protected int outerAltNum = 1;

	/** Used to mark root of subtree that hits the decision override, if any */
	protected boolean isDecisionOverrideRoot;

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
}
