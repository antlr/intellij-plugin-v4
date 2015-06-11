package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.runtime.InterpreterRuleContext;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ATNSerializer;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.StarLoopEntryState;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LeftRecursiveRule;
import org.antlr.v4.tool.Rule;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class PreviewParser extends ParserInterpreter {
	/** The grammar associated with this interpreter. Unlike the
	 *  {@link ParserInterpreter} from the standard distribution,
	 *  this can reference Grammar, which is in the tools area not
	 *  purely runtime.
	 */
	protected final Grammar g;

	/** Map each preview editor token to the grammar ATN state used to match it.
	 *  Saves us having to create special token subclass and token factory.
	 */
	public Map<Token, Integer> inputTokenToStateMap = new HashMap<Token, Integer>();

	protected BitSet decisionStatesThatSetOuterAltNumInContext;

	/** Cache {@link LeftRecursiveRule#getPrimaryAlts()} and
	 *  {@link LeftRecursiveRule#getRecursiveOpAlts()} for states in
	 *  {@link #decisionStatesThatSetOuterAltNumInContext}.
	 */
	protected final Map<DecisionState, int[]> stateToAltsMap = new HashMap<>();

	protected int lastSuccessfulMatchState = ATNState.INVALID_STATE_NUMBER;

	public PreviewParser(Grammar g, TokenStream input) {
		super(g.fileName, g.getVocabulary(),
			  Arrays.asList(g.getRuleNames()),
			  // must run ATN through serializer to set some state flags:
			  new ATNDeserializer().deserialize(ATNSerializer.getSerializedAsChars(g.atn)),
			  input);
		this.g = g;
		if ( decisionStatesThatSetOuterAltNumInContext==null ) {
			decisionStatesThatSetOuterAltNumInContext = findOuterMostDecisionStates();
		}
	}

	@Override
	public ParserInterpreter copyFrom(ParserInterpreter old) {
		PreviewParser uber = (PreviewParser)old;
		return new PreviewParser(uber.g, old.getTokenStream());
	}

	@Override
	public void reset() {
		super.reset();
		if ( inputTokenToStateMap!=null ) inputTokenToStateMap.clear();
	}

	@Override
	protected InterpreterRuleContext createInterpreterRuleContext(ParserRuleContext parent, int invokingStateNumber, int ruleIndex) {
		return new PreviewInterpreterRuleContext(parent, invokingStateNumber, ruleIndex);
	}

	@Override
	public void enterRule(ParserRuleContext localctx, int state, int ruleIndex) {
		super.enterRule(localctx, state, ruleIndex);
//		System.out.println("enter "+getRuleNames()[ruleIndex]);
	}

	/**In the case of left-recursive rules,
	 * there is typically a decision for the primary alternatives and a
	 * decision to choose between the recursive operator alternatives.
	 * For example, the following left recursive rule has two primary and 2
	 * recursive alternatives.</p>
	 *
	 e : e '*' e
	   | '-' INT
	   | e '+' e
	   | ID
	   ;

	 * <p>ANTLR rewrites that rule to be</p>

	 e[int precedence]
		 : ('-' INT | ID)
		 ( {...}? '*' e[5]
		 | {...}? '+' e[3]
		 )*
	 	;

	 *
	 * <p>So, there are two decisions associated with picking the outermost alt.
	 * This complicates our tracking significantly. The outermost alternative number
	 * is a function of the decision (ATN state) within a left recursive rule and the
	 * predicted alternative coming back from adaptivePredict().
	 */
	@Override
	protected int visitDecisionState(DecisionState p) {
		int predictedAlt = super.visitDecisionState(p);
		PreviewInterpreterRuleContext ctx = (PreviewInterpreterRuleContext)_ctx;
		if ( decisionStatesThatSetOuterAltNumInContext.get(p.stateNumber) ) {
			ctx.outerAltNum = predictedAlt;
			Rule r = g.getRule(p.ruleIndex);
			if ( atn.ruleToStartState[r.index].isLeftRecursiveRule ) {
				int[] alts = stateToAltsMap.get(p);
				LeftRecursiveRule lr = (LeftRecursiveRule) g.getRule(p.ruleIndex);
				if (p.getStateType() == ATNState.BLOCK_START) {
					if ( alts==null ) {
						alts = lr.getPrimaryAlts();
						stateToAltsMap.put(p, alts); // cache it
					}
				}
				else if (p.getStateType() == ATNState.STAR_BLOCK_START) {
					if ( alts==null ) {
						alts = lr.getRecursiveOpAlts();
						stateToAltsMap.put(p, alts); // cache it
					}
				}
				ctx.outerAltNum = alts[predictedAlt];
			}
		}

		return predictedAlt;
	}

	/** identify the ATN states where we need to set the outer alt number.
	 *  For regular rules, that's the block at the target to rule start state.
	 *  For left-recursive rules, we track the primary block, which looks just
	 *  like a regular rule's outer block, and the star loop block (always
	 *  there even if 1 alt).
	 */
	public BitSet findOuterMostDecisionStates() {
		BitSet track = new BitSet(atn.states.size());
		int numberOfDecisions = atn.getNumberOfDecisions();
		for (int i = 0; i < numberOfDecisions; i++) {
			DecisionState decisionState = atn.getDecisionState(i);
			RuleStartState startState = atn.ruleToStartState[decisionState.ruleIndex];
			// Look for StarLoopEntryState that is in any left recursive rule
			if ( decisionState instanceof StarLoopEntryState ) {
				StarLoopEntryState loopEntry = (StarLoopEntryState)decisionState;
				if ( loopEntry.isPrecedenceDecision ) {
					// Recursive alts always result in a (...)* in the transformed
					// left recursive rule and that always has a BasicBlockStartState
					// even if just 1 recursive alt exists.
					ATNState blockStart = loopEntry.transition(0).target;
					// track the StarBlockStartState associated with the recursive alternatives
					track.set(blockStart.stateNumber);
				}
			}
			else if ( startState.transition(0).target == decisionState ) {
				// always track outermost block for any rule if it exists
				track.set(decisionState.stateNumber);
			}
		}
		return track;
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
