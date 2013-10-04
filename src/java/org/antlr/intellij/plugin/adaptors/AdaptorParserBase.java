package org.antlr.intellij.plugin.adaptors;


import com.intellij.lang.PsiBuilder;
import com.simpleplugin.ANTLRv4TokenTypeAdaptor;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import java.util.Stack;

// super of generated parser
public abstract class AdaptorParserBase extends Parser {
	// fields set by parse().
	public PsiBuilder builder;

	public Stack<String> ruleNameStack = new Stack<String>();
	public Stack<PsiBuilder.Marker> markerStack = new Stack<PsiBuilder.Marker>();

	public AdaptorParserBase(TokenStream input) { // unused; just to compile
		super(input);
	}

	@Override
	public void reset() {
		super.reset();
		if ( ruleNameStack!=null ) ruleNameStack.clear();
		if ( markerStack!=null ) markerStack.clear();
	}

	@Override
	public Token consume() {
		Token t = super.consume();
		System.out.println("consuming current token="+t); // print current token
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
		ruleNameStack.push(currentRuleName);
//		System.out.println("mark " + currentRuleName);
	}

	// TODO: THIS won't compile w/o ANTLRv4TokenTypeAdaptor, which is genrated
	// so how can this be a library jar?

	@Override
	public void exitRule() {
		super.exitRule();
		String currentRuleName = ruleNameStack.pop();
//		System.out.println("done " + currentRuleName);
		// consume any bad tokens so parser sees them in correct tree
		PsiBuilder.Marker marker = markerStack.pop();
		marker.done(ANTLRv4TokenTypeAdaptor.ruleNameToIDEAElementType.get(currentRuleName));
	}
}
