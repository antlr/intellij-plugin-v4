package org.antlr.intellij.plugin.parsing;

import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ATNSerializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PreviewParser extends ParserInterpreter {
	/** Map each preview editor token to the grammar ATN state used to match it.
	 *  Saves us having to create special token subclass and token factory.
	 */
	public Map<Token, Integer> inputTokenToStateMap = new HashMap<Token, Integer>();

	public PreviewParser(PreviewState previewState, CommonTokenStream tokens) {
		super(previewState.g.fileName, Arrays.asList(previewState.g.getTokenDisplayNames()),
									   Arrays.asList(previewState.g.getRuleNames()),
									   new ATNDeserializer().deserialize(ATNSerializer.getSerializedAsChars(previewState.g.atn)),
									   tokens);
	}

	@Override
	public Token match(int ttype) throws RecognitionException {
		Token t = super.match(ttype);
		// track which ATN state matches each token
		inputTokenToStateMap.put(t, getState());
//		CommonToken tokenInGrammar = previewState.stateToGrammarRegionMap.get(getState());
//		System.out.println("match ATOM state " + getState() + ": " + tokenInGrammar);
		return t;
	}

	@Override
	public Token matchWildcard() throws RecognitionException {
//		System.out.println("match anything state "+getState());
		inputTokenToStateMap.put(_input.LT(1), getState());
		return super.matchWildcard();
	}
}
