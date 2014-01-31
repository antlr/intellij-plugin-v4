package org.antlr.intellij.plugin.adaptors;

import com.intellij.lang.Language;
import org.antlr.intellij.lexer.AbstractAntlrAdapter;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.v4.runtime.Lexer;

public class ANTLRv4LexerAdaptor extends AbstractAntlrAdapter<ANTLRv4LexerState> {
	private static final ANTLRv4LexerState INITIAL_STATE = new ANTLRv4LexerState(Lexer.DEFAULT_MODE, null, 0);

	public ANTLRv4LexerAdaptor(Language language, ANTLRv4Lexer lexer) {
		super(language, lexer);
	}

	@Override
	protected ANTLRv4LexerState getInitialState() {
		return INITIAL_STATE;
	}

	@Override
	protected ANTLRv4LexerState getLexerState(Lexer lexer) {
		if (lexer._modeStack.isEmpty()) {
			return new ANTLRv4LexerState(lexer._mode, null, ((ANTLRv4Lexer)lexer).getRuleType());
		}

		return new ANTLRv4LexerState(lexer._mode, lexer._modeStack, ((ANTLRv4Lexer)lexer).getRuleType());
	}
}
