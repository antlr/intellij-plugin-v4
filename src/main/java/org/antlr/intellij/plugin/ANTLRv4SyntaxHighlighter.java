package org.antlr.intellij.plugin;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.adaptors.ANTLRv4LexerAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class ANTLRv4SyntaxHighlighter extends SyntaxHighlighterBase {
	public static final TextAttributesKey KEYWORD =
		createTextAttributesKey("ANTLRv4_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
	public static final TextAttributesKey RULENAME =
		createTextAttributesKey("ANTLRv4_RULENAME", DefaultLanguageHighlighterColors.PARAMETER);
	public static final TextAttributesKey TOKENNAME =
		createTextAttributesKey("ANTLRv4_TOKENNAME", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
	public static final TextAttributesKey STRING =
		createTextAttributesKey("ANTLRv4_STRING", DefaultLanguageHighlighterColors.STRING);
	public static final TextAttributesKey LINE_COMMENT =
		createTextAttributesKey("ANTLRv4_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
	public static final TextAttributesKey DOC_COMMENT =
		createTextAttributesKey("ANTLRv4_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);
	public static final TextAttributesKey BLOCK_COMMENT =
		createTextAttributesKey("ANTLRv4_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);

	private static final TextAttributesKey[] BAD_CHAR_KEYS = pack(HighlighterColors.BAD_CHARACTER);
	private static final TextAttributesKey[] STRING_KEYS = pack(STRING);
	private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[] {LINE_COMMENT, DOC_COMMENT, BLOCK_COMMENT};
	private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

	@NotNull
	@Override
	public Lexer getHighlightingLexer() {
		ANTLRv4Lexer lexer = new ANTLRv4Lexer(null);
		return new ANTLRv4LexerAdaptor(lexer);
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
		if ( ANTLRv4TokenTypes.KEYWORDS.contains(tokenType) ){
			return pack(KEYWORD);
		}

		if ( tokenType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF) ) {
			return pack(TOKENNAME);
		}
		else if ( tokenType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF) ) {
			return pack(RULENAME);
		}
		else if (tokenType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.STRING_LITERAL)
			|| tokenType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.UNTERMINATED_STRING_LITERAL)) {
			return STRING_KEYS;
		}
		else if (tokenType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.BLOCK_COMMENT)) {
			return COMMENT_KEYS;
		}
		else if (tokenType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.DOC_COMMENT)) {
			return COMMENT_KEYS;
		}
		else if (tokenType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.LINE_COMMENT)) {
			return COMMENT_KEYS;
		}
		else if (tokenType == ANTLRv4TokenTypes.BAD_TOKEN_TYPE) {
			return BAD_CHAR_KEYS;
		}
		else {
			return EMPTY_KEYS;
		}
	}
}
