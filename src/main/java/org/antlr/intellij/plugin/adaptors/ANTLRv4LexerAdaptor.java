package org.antlr.intellij.plugin.adaptors;

import com.intellij.lang.Language;
import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.v4.runtime.Lexer;

/** Adapt ANTLR needs to intellij */
public class ANTLRv4LexerAdaptor extends ANTLRLexerAdaptor {
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
			return new ANTLRv4LexerState(lexer._mode, null, ((ANTLRv4Lexer)lexer).getCurrentRuleType());
		}

		return new ANTLRv4LexerState(lexer._mode, lexer._modeStack, ((ANTLRv4Lexer)lexer).getCurrentRuleType());
	}
}
