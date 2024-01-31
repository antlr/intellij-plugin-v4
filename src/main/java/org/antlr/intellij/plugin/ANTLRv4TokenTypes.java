package org.antlr.intellij.plugin;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.intellij.plugin.adaptors.ANTLRv4LexerAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.intellij.lang.annotations.MagicConstant;

import java.util.List;

public class ANTLRv4TokenTypes {
	public static IElementType BAD_TOKEN_TYPE = new IElementType("BAD_TOKEN", ANTLRv4Language.INSTANCE);

	static {
		ANTLRv4LexerAdaptor.initializeElementTypeFactory();
	}

	public static final List<TokenIElementType> TOKEN_ELEMENT_TYPES =
		PSIElementTypeFactory.getTokenIElementTypes(ANTLRv4Language.INSTANCE);
	public static final List<RuleIElementType> RULE_ELEMENT_TYPES =
		PSIElementTypeFactory.getRuleIElementTypes(ANTLRv4Language.INSTANCE);

    public static final TokenSet COMMENTS =
		PSIElementTypeFactory.createTokenSet(
			ANTLRv4Language.INSTANCE,
			ANTLRv4Lexer.DOC_COMMENT,
			ANTLRv4Lexer.BLOCK_COMMENT,
			ANTLRv4Lexer.LINE_COMMENT);

	public static final TokenSet WHITESPACES =
		PSIElementTypeFactory.createTokenSet(
			ANTLRv4Language.INSTANCE,
			ANTLRv4Lexer.WS);

	public static final TokenSet KEYWORDS =
		PSIElementTypeFactory.createTokenSet(
			ANTLRv4Language.INSTANCE,
			ANTLRv4Lexer.LEXER,ANTLRv4Lexer.PROTECTED,ANTLRv4Lexer.IMPORT,ANTLRv4Lexer.CATCH,
			ANTLRv4Lexer.PRIVATE,ANTLRv4Lexer.FRAGMENT,ANTLRv4Lexer.PUBLIC,ANTLRv4Lexer.MODE,
			ANTLRv4Lexer.FINALLY,ANTLRv4Lexer.RETURNS,ANTLRv4Lexer.THROWS,ANTLRv4Lexer.GRAMMAR,
			ANTLRv4Lexer.LOCALS,ANTLRv4Lexer.PARSER);

    public static RuleIElementType getRuleElementType(@MagicConstant(valuesFromClass = ANTLRv4Parser.class)int ruleIndex){
        return RULE_ELEMENT_TYPES.get(ruleIndex);
    }
    public static TokenIElementType getTokenElementType(@MagicConstant(valuesFromClass = ANTLRv4Lexer.class)int ruleIndex){
        return TOKEN_ELEMENT_TYPES.get(ruleIndex);
    }
}
