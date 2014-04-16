package org.antlr.intellij.plugin;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleElementType;
import org.antlr.intellij.adaptor.lexer.TokenElementType;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;

import java.util.Arrays;
import java.util.List;

public class ANTLRv4TokenTypes {
	public static IElementType BAD_TOKEN_TYPE = new IElementType("BAD_TOKEN", ANTLRv4Language.INSTANCE);

	public static final List<TokenElementType> TOKEN_ELEMENT_TYPES =
		ElementTypeFactory.getTokenElementTypes(ANTLRv4Language.INSTANCE,
												Arrays.asList(ANTLRv4Parser.tokenNames));
	public static final List<RuleElementType> RULE_ELEMENT_TYPES =
		ElementTypeFactory.getRuleElementTypes(ANTLRv4Language.INSTANCE,
											   Arrays.asList(ANTLRv4Parser.ruleNames));

    public static final TokenSet COMMENTS =
		ElementTypeFactory.createTokenSet(
			ANTLRv4Language.INSTANCE,
			Arrays.asList(ANTLRv4Lexer.tokenNames),
			ANTLRv4Lexer.DOC_COMMENT,
			ANTLRv4Lexer.BLOCK_COMMENT,
			ANTLRv4Lexer.LINE_COMMENT);

	public static final TokenSet WHITESPACES =
		ElementTypeFactory.createTokenSet(
			ANTLRv4Language.INSTANCE,
			Arrays.asList(ANTLRv4Lexer.tokenNames),
			ANTLRv4Lexer.WS);

	public static final TokenSet KEYWORDS =
		ElementTypeFactory.createTokenSet(
			ANTLRv4Language.INSTANCE,
			Arrays.asList(ANTLRv4Lexer.tokenNames),
			ANTLRv4Lexer.LEXER,ANTLRv4Lexer.PROTECTED,ANTLRv4Lexer.IMPORT,ANTLRv4Lexer.CATCH,
			ANTLRv4Lexer.PRIVATE,ANTLRv4Lexer.FRAGMENT,ANTLRv4Lexer.PUBLIC,ANTLRv4Lexer.MODE,
			ANTLRv4Lexer.FINALLY,ANTLRv4Lexer.RETURNS,ANTLRv4Lexer.THROWS,ANTLRv4Lexer.GRAMMAR,
			ANTLRv4Lexer.LOCALS,ANTLRv4Lexer.PARSER);
}
