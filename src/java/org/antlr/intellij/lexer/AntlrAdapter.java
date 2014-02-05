package org.antlr.intellij.lexer;

import com.intellij.lang.Language;
import org.antlr.v4.runtime.Lexer;

/**
 * This default implementation of {@link AbstractAntlrAdapter} supports any ANTLR 4 lexer that does not store extra
 * information for use in custom actions. The state is maintained in {@link AntlrLexerState} which supports simple as
 * well as multi-mode lexers.
 */
public class AntlrAdapter extends AbstractAntlrAdapter<AntlrLexerState> {

	public AntlrAdapter(Language language, Lexer lexer) {
		super(language, lexer);
	}

	@Override
	protected AntlrLexerState getInitialState() {
		return new AntlrLexerState(Lexer.DEFAULT_MODE, null);
	}

	@Override
	protected AntlrLexerState getLexerState(Lexer lexer) {
		if (lexer._modeStack.isEmpty()) {
			return new AntlrLexerState(lexer._mode, null);
		}

		return new AntlrLexerState(lexer._mode, lexer._modeStack);
	}

}
