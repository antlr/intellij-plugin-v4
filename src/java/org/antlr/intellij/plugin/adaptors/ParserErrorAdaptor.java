package org.antlr.intellij.plugin.adaptors;

import com.intellij.lang.PsiBuilder;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.util.List;
import java.util.Stack;

public class ParserErrorAdaptor extends BaseErrorListener {
	public AdaptorParserBase parser;
	public PsiBuilder builder;
	public ParserErrorAdaptor(AdaptorParserBase parser, PsiBuilder builder) {
		this.parser = parser;
		this.builder = builder;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer,
							Object offendingSymbol,
							int line, int charPositionInLine,
							String msg, RecognitionException e)
	{
		// squiggly lines for errors are sometimes off because we don't
		// IDEA does not pass white states to us and so I generate the error
		// too soon.
		// http://devnet.jetbrains.com/message/5504752#5504752
		// Ter: The biggest problem is that WS is not sent to my parser
		// and so it will likely always be out of sync, right?  The error
		// is detected before I have consumed the invalid token (using
		// lookahead in the ANTLR parser). I might have to get tricky
		// by advance()ing until I see the offending token for no viable alts.

		// I don't think IDEA is tracking line, column info, so just send message
		Stack<PsiBuilder.Marker> markerStack = parser.markerStack;
		if ( markerStack.size()>0 ) {
			PsiBuilder.Marker m = builder.mark();
			m.error(msg);  // this closes the marker we just built
		}
		else {
			builder.error(msg);
		}
	}
}
