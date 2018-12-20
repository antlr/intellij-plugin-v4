package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.runtime.InterpreterRuleContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ATNSerializer;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.GrammarParserInterpreter;

import java.util.HashMap;
import java.util.Map;

public class PreviewParser extends GrammarParserInterpreter {
	/** Map each preview editor token to the grammar ATN state used to match it.
	 *  Saves us having to create special token subclass and token factory.
	 */
	public Map<Token, Integer> inputTokenToStateMap = new HashMap<Token, Integer>();

	protected int lastSuccessfulMatchState = ATNState.INVALID_STATE_NUMBER; // not sure about error nodes

	public PreviewParser(Grammar g, ATN atn, TokenStream input) {
		super(g, atn, input);
	}

	public PreviewParser(Grammar g, TokenStream input) {
		this(g, new ATNDeserializer().deserialize(ATNSerializer.getSerializedAsChars(g.getATN())), input);
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
	protected int visitDecisionState(DecisionState p) {
		int predictedAlt = super.visitDecisionState(p);
		if ( p.getNumberOfTransitions()>1 ) {
//			System.out.println("decision "+p.decision+": "+predictedAlt);
			if ( p.decision==this.overrideDecision &&
			this._input.index()==this.overrideDecisionInputIndex ) {
				((PreviewInterpreterRuleContext)overrideDecisionRoot).isDecisionOverrideRoot = true;
			}
		}
		return predictedAlt;
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
