package org.antlr.intellij.plugin.preview;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.lexer.TokenElementType;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.v4.runtime.LexerInterpreter;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class PreviewSyntaxHighlighter extends SyntaxHighlighterBase {
	private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{HighlighterColors.BAD_CHARACTER};
	private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

	public static final TextAttributesKey KEYWORD =
		createTextAttributesKey("KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

	Project project;

	public PreviewSyntaxHighlighter(Project project) {
		this.project = project;
	}

	@NotNull
	@Override
	public Lexer getHighlightingLexer() {
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState();
		System.out.println("syntax highlighting with "+previewState.grammarFileName);
		LexerInterpreter lexEngine = previewState.lg.createLexerInterpreter(null);
		return new PreviewLexer(PreviewLanguage.INSTANCE, lexEngine);
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
		System.out.println("highlight "+tokenType);
//		return new TextAttributesKey[] {KEYWORD};
		if (tokenType == TokenElementType.BAD_TOKEN_TYPE ) {
			return BAD_CHAR_KEYS;
		}
		else {
			return EMPTY_KEYS;
		}
	}
}
