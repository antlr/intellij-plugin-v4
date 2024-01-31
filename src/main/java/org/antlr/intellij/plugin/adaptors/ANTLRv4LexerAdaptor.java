package org.antlr.intellij.plugin.adaptors;

import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor;
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.v4.runtime.Lexer;

/** Adapt ANTLR needs to intellij */
public class ANTLRv4LexerAdaptor extends ANTLRLexerAdaptor {

	// In case a lexer was created outside our ParserDefinition
	static {
		initializeElementTypeFactory();
	}

	public static void initializeElementTypeFactory() {
		PSIElementTypeFactory.defineLanguageIElementTypes(
				ANTLRv4Language.INSTANCE,
				ANTLRv4Lexer.tokenNames,
				ANTLRv4Parser.ruleNames
		);
	}

	private static final ANTLRv4LexerState INITIAL_STATE = new ANTLRv4LexerState(Lexer.DEFAULT_MODE, null, 0);

	public ANTLRv4LexerAdaptor(ANTLRv4Lexer lexer) {
		super(ANTLRv4Language.INSTANCE, lexer);
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
