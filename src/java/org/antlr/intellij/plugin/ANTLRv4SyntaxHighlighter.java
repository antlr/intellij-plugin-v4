package org.antlr.intellij.plugin;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.SyntaxHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.adaptors.LexerAdaptor;
import org.antlr.intellij.plugin.adaptors.Utils;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypeAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class ANTLRv4SyntaxHighlighter extends SyntaxHighlighterBase {
	public static final TextAttributesKey ID =
		createTextAttributesKey("SIMPLE_ID", SyntaxHighlighterColors.KEYWORD);
	public static final TextAttributesKey STRING =
		createTextAttributesKey("SIMPLE_STRING", SyntaxHighlighterColors.STRING);
	public static final TextAttributesKey COMMENT =
		createTextAttributesKey("SIMPLE_COMMENT", SyntaxHighlighterColors.LINE_COMMENT);

	static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey("SIMPLE_BAD_CHARACTER",
																		   new TextAttributes(Color.RED, null, null, null, Font.BOLD));

	private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
	private static final TextAttributesKey[] ID_KEYS = new TextAttributesKey[]{ID};
	private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
	private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
	private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

	@NotNull
	@Override
	public Lexer getHighlightingLexer() {
		final ANTLRv4Lexer lexer = new ANTLRv4Lexer(null);
		LexerATNSimulator sim =
			Utils.getLexerATNSimulator(lexer, ANTLRv4Lexer._ATN, lexer.getInterpreter().decisionToDFA,
									   lexer.getInterpreter().getSharedContextCache());
		lexer.setInterpreter(sim);
		return new LexerAdaptor(lexer);
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
		if ( tokenType == ANTLRv4TokenTypes.TOKEN_REF ) {
			return ID_KEYS;
		}
		else if (tokenType == ANTLRv4TokenTypes.STRING_LITERAL) {
			return STRING_KEYS;
		}
		else if (tokenType == ANTLRv4TokenTypes.BLOCK_COMMENT) {
			return COMMENT_KEYS;
		}
		else if (tokenType == ANTLRv4TokenTypes.DOC_COMMENT) {
			return COMMENT_KEYS;
		}
		else if (tokenType == ANTLRv4TokenTypes.LINE_COMMENT) {
			return COMMENT_KEYS;
		}
		else if (tokenType.equals(ANTLRv4TokenTypeAdaptor.BAD_TOKEN)) {
			return BAD_CHAR_KEYS;
		}
		else {
			return EMPTY_KEYS;
		}
	}
}
