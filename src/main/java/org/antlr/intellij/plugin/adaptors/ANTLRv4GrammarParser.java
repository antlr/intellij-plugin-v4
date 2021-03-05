package org.antlr.intellij.plugin.adaptors;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.antlr.intellij.adaptor.parser.ANTLRParserAdaptor;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

/** A specific kind of parser that knows how to parse ANTLR v4 grammar meta-language */
public class ANTLRv4GrammarParser extends ANTLRParserAdaptor {
	public ANTLRv4GrammarParser() {
		super(ANTLRv4Language.INSTANCE, new ANTLRv4Parser(null));
	}

	@Override
	protected ParseTree parse(Parser parser, IElementType root) {
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
			return ((ANTLRv4Parser) parser).grammarSpec();

		case ANTLRv4Parser.RULE_atom:
			return ((ANTLRv4Parser) parser).atom();

		default:
			throw new UnsupportedOperationException(String.format("cannot start parsing using root element %s", root));
		}
	}
}
