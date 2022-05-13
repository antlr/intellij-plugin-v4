package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

/**
 * Checks that a lexer is not stuck trying to match the same thing over and over, for example if the input grammar
 * can match an empty string. This prevents the IDE from crashing from things like {@link OutOfMemoryError}s.
 */
public class LexerWatchdog {

	/**
	 * The number of iterations on the same index after which we kill the interpreter.
	 */
	private static final int THRESHOLD = 50;

	private final TokenStream tokenStream;
	private final PreviewParser previewParser;

	private int currentIndex = -1;
	private int iterationsOnCurrentIndex = 0;

	public LexerWatchdog(TokenStream tokenStream, PreviewParser previewParser) {
		this.tokenStream = tokenStream;
		this.previewParser = previewParser;
	}

	public void checkLexerIsNotStuck() {
		if ( currentIndex==tokenStream.index() ) {
			iterationsOnCurrentIndex++;
		}
		else {
			currentIndex = tokenStream.index();
			iterationsOnCurrentIndex = 1;
		}

		if ( iterationsOnCurrentIndex>THRESHOLD ) {
			final Token token = tokenStream.get(currentIndex);
			final String displayName = token.getType() == Token.EOF
					? token.getText()
					: previewParser.getVocabulary().getDisplayName(token.getType());

			throw new RecognitionException(
					"interpreter was killed after " + THRESHOLD + " iterations on token '" + displayName + "'",
					previewParser,
					tokenStream,
					previewParser.getContext()
			) {
				@Override
				public Token getOffendingToken() {
					return token;
				}
			};
		}
	}
}
