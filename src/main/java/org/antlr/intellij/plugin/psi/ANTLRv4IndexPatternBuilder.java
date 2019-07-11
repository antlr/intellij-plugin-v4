package org.antlr.intellij.plugin.psi;

import com.intellij.lexer.Lexer;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.search.IndexPatternBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.adaptors.ANTLRv4LexerAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.getTokenElementType;

public class ANTLRv4IndexPatternBuilder implements IndexPatternBuilder {

	@Nullable
	@Override
	public Lexer getIndexingLexer(@NotNull PsiFile file) {
		if ( file instanceof ANTLRv4FileRoot ) {
			ANTLRv4Lexer lexer = new ANTLRv4Lexer(null);
			return new ANTLRv4LexerAdaptor(lexer);
		}
		return null;
	}

	@Nullable
	@Override
	public TokenSet getCommentTokenSet(@NotNull PsiFile file) {
		if ( file instanceof ANTLRv4FileRoot ) {
			return TokenSet.create(getTokenElementType(ANTLRv4Lexer.LINE_COMMENT));
		}
		return null;
	}

	@Override
	public int getCommentStartDelta(IElementType tokenType) {
		return tokenType==getTokenElementType(ANTLRv4Lexer.LINE_COMMENT) ? 2 : 0;
	}

	@Override
	public int getCommentEndDelta(IElementType tokenType) {
		return 0;
	}
}
