package org.antlr.intellij.plugin.adaptors;


import com.intellij.lang.PsiBuilder;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypeAdaptor;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import java.util.Stack;

// super of generated parser
public abstract class AdaptorParserBase extends Parser {
	// fields set by parse().
	public PsiBuilder builder;

//	public Stack<String> ruleNameStack = new Stack<String>();
	public Stack<Integer> ruleIndexStack = new Stack<Integer>();
	public Stack<PsiBuilder.Marker> markerStack = new Stack<PsiBuilder.Marker>();

	public AdaptorParserBase(TokenStream input) { // unused; just to compile
		super(input);
	}

	@Override
	public void reset() {
		super.reset();
		if ( ruleIndexStack!=null ) ruleIndexStack.clear();
		if ( markerStack!=null ) markerStack.clear();
	}

	@Override
	public Token consume() {
		Token t = super.consume();
//		System.err.println("consuming current token="+t); // print current token
		builder.advanceLexer();
		return t;
	}

	@Override
	public void enterRule(@org.antlr.v4.runtime.misc.NotNull ParserRuleContext localctx,
						  int state, int ruleIndex)
	{
		super.enterRule(localctx, state, ruleIndex);
		markerStack.push(builder.mark());
		String currentRuleName = getRuleNames()[ruleIndex];
		ruleIndexStack.push(ruleIndex);
//		System.err.println("mark " + currentRuleName);
	}

	// TODO: THIS won't compile w/o ANTLRv4TokenTypeAdaptor, which is genrated
	// so how can this be a library jar?

	@Override
	public void exitRule() {
		super.exitRule();
		Integer currentRuleIndex = ruleIndexStack.pop();
//		System.err.println("done " + ANTLRv4TokenTypeAdaptor.ruleToIDEATokenType[currentRuleIndex]);
		// consume any bad tokens so parser sees them in correct tree
		PsiBuilder.Marker marker = markerStack.pop();
		marker.done(ANTLRv4TokenTypeAdaptor.ruleToIDEATokenType[currentRuleIndex]);
	}
}
