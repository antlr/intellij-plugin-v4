package org.antlr.intellij.plugin.parsing;

import com.intellij.openapi.progress.ProgressManager;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.GrammarParserInterpreter;

import java.util.HashMap;
import java.util.Map;

public class PreviewParser extends GrammarParserInterpreter {
	/** Map each preview editor token to the grammar ATN state used to match it.
	 *  Saves us having to create special token subclass and token factory.
	 */
	public Map<Token, Integer> inputTokenToStateMap = new HashMap<>();

	private final LexerWatchdog lexerWatchdog;

	protected int lastSuccessfulMatchState = ATNState.INVALID_STATE_NUMBER; // not sure about error nodes

	private boolean warnedAboutPredicates = false;

	public PreviewParser(Grammar g, ATN atn, TokenStream input) {
		super(g, atn, input);
		lexerWatchdog = new LexerWatchdog(input, this);
	}

	public PreviewParser(Grammar g, TokenStream input) {
		this(g, new ATNDeserializer().deserialize(ATNSerializer.getSerialized(g.getATN()).toArray()), input);
	}

	@Override
	public void reset() {
		super.reset();
		if ( inputTokenToStateMap!=null ) inputTokenToStateMap.clear();
		lastSuccessfulMatchState = ATNState.INVALID_STATE_NUMBER;
		warnedAboutPredicates = false;
	}

	@Override
	protected InterpreterRuleContext createInterpreterRuleContext(ParserRuleContext parent, int invokingStateNumber, int ruleIndex) {
		return new PreviewInterpreterRuleContext(parent, invokingStateNumber, ruleIndex);
	}

	@Override
	protected int visitDecisionState(DecisionState p) {
		ProgressManager.checkCanceled();

		int predictedAlt = super.visitDecisionState(p);
		if ( p.getNumberOfTransitions()>1 ) {
			if ( p.decision==this.overrideDecision &&
			this._input.index()==this.overrideDecisionInputIndex ) {
				((PreviewInterpreterRuleContext)overrideDecisionRoot).isDecisionOverrideRoot = true;
			}
		}
		return predictedAlt;
	}


	@Override
	public Token match(int ttype) throws RecognitionException {
		lexerWatchdog.checkLexerIsNotStuck();

		Token t = super.match(ttype);
		// track which ATN state matches each token
		inputTokenToStateMap.put(t, getState());
		lastSuccessfulMatchState = getState();
		return t;
	}


	@Override
	public Token matchWildcard() throws RecognitionException {
		lexerWatchdog.checkLexerIsNotStuck();

		inputTokenToStateMap.put(_input.LT(1), getState());
		lastSuccessfulMatchState = getState();
		return super.matchWildcard();
	}

	@Override
	public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
		warnAboutPredicatesAndActions();
	}

	@Override
	public boolean sempred(RuleContext _localctx, int ruleIndex, int actionIndex) {
		warnAboutPredicatesAndActions();
		return super.sempred(_localctx, ruleIndex, actionIndex);
	}

	private void warnAboutPredicatesAndActions() {
		if (!warnedAboutPredicates) {
			notifyErrorListeners("WARNING: predicates and actions are not run by this interpreter. " +
					"Results may vary from the actual generated parser!");
			warnedAboutPredicates = true;
		}
	}
}
