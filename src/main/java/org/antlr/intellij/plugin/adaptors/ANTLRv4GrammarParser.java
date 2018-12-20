package org.antlr.intellij.plugin.adaptors;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.antlr.intellij.adaptor.parser.AntlrParser;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/** A specific kind of parser that knows how to parse ANTLR v4 grammar meta-language */
public class ANTLRv4GrammarParser extends AntlrParser<ANTLRv4Parser> {
	public ANTLRv4GrammarParser() {
		super(ANTLRv4Language.INSTANCE);
	}

	@Override
	protected ANTLRv4Parser createParserImpl(TokenStream tokenStream, IElementType root, PsiBuilder builder) {
		ANTLRv4Parser parser = new ANTLRv4Parser(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(new SyntaxErrorListener());
		return parser;
	}

	@Override
	protected ParseTree parseImpl(ANTLRv4Parser parser, IElementType root, PsiBuilder builder) {
		int startRule;
		if (root instanceof IFileElementType) {
			startRule = ANTLRv4Parser.RULE_grammarSpec;
		}
		else if (root == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF)
			|| root == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF)) {
			startRule = ANTLRv4Parser.RULE_atom;
		}
		else {
			startRule = Token.INVALID_TYPE;
		}

		switch (startRule) {
		case ANTLRv4Parser.RULE_grammarSpec:
			return parser.grammarSpec();

		case ANTLRv4Parser.RULE_atom:
			return parser.atom();

		default:
			String ruleName = ANTLRv4Parser.ruleNames[startRule];
			throw new UnsupportedOperationException(String.format("cannot start parsing using root element %s", root));
		}
	}
}
