package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.runtime.InterpreterRuleContext;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ATNSerializer;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.StarLoopEntryState;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.GrammarParserInterpreter;
import org.antlr.v4.tool.LeftRecursiveRule;
import org.antlr.v4.tool.Rule;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class PreviewParser extends GrammarParserInterpreter {
	/** Map each preview editor token to the grammar ATN state used to match it.
	 *  Saves us having to create special token subclass and token factory.
	 */
	public Map<Token, Integer> inputTokenToStateMap = new HashMap<Token, Integer>();

	protected int lastSuccessfulMatchState = ATNState.INVALID_STATE_NUMBER; // not sure about error nodes

	/** What is the current context when we override a decisions?  This tells
	 *  us what the root of the parse tree is for an ambiguity/lookahead check.
	 */
	protected PreviewInterpreterRuleContext overrideDecisionContext = null;

	public PreviewParser(Grammar g, TokenStream input) {
		super(g, new ATNDeserializer().deserialize(ATNSerializer.getSerializedAsChars(g.getATN())), input);
	}

	@Override
	public void reset() {
		super.reset();
		if ( inputTokenToStateMap!=null ) inputTokenToStateMap.clear();
		lastSuccessfulMatchState = ATNState.INVALID_STATE_NUMBER;
	}

	@Override
	protected InterpreterRuleContext createInterpreterRuleContext(ParserRuleContext parent, int invokingStateNumber, int ruleIndex) {
		return new PreviewInterpreterRuleContext(parent, invokingStateNumber, ruleIndex);
	}

	@Override
	public Token match(int ttype) throws RecognitionException {
//		System.out.println("match ATOM state " + getState() + ": " + _input.LT(1));
		Token t = super.match(ttype);
		// track which ATN state matches each token
		inputTokenToStateMap.put(t, getState());
		lastSuccessfulMatchState = getState();
//		CommonToken tokenInGrammar = previewState.stateToGrammarRegionMap.get(getState());
		return t;
	}


	@Override
	public Token matchWildcard() throws RecognitionException {
//		System.out.println("match anything state "+getState());
		inputTokenToStateMap.put(_input.LT(1), getState());
		lastSuccessfulMatchState = getState();
		return super.matchWildcard();
	}
}
