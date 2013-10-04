package org.antlr.intellij.plugin.adaptors;

import com.intellij.lang.PsiBuilder;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ParserErrorAdaptor extends BaseErrorListener {
	public PsiBuilder builder;
	public ParserErrorAdaptor(PsiBuilder builder) {
		this.builder = builder;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer,
							Object offendingSymbol,
							int line, int charPositionInLine,
							String msg, RecognitionException e)
	{
		// I don't think IDEA is tracking line, column info, so just send message
		builder.error(msg);
	}
}
