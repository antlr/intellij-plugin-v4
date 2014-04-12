package org.antlr.intellij.plugin.preview;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class PreviewParserDefinition implements ParserDefinition {
	public static final IFileElementType FILE =
		new IFileElementType(PreviewLanguage.INSTANCE);

	@NotNull
	@Override
	public Lexer createLexer(Project project) {
		return null;
	}

	@Override
	public PsiParser createParser(Project project) {
		return null;
	}

	@Override
	public IFileElementType getFileNodeType() {
		return FILE;
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getCommentTokens() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getStringLiteralElements() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public PsiElement createElement(ASTNode node) {
		return new ASTWrapperPsiElement(node);
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider) {
		return new PreviewFileRoot(viewProvider);
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
		return SpaceRequirements.MAY;
	}
}
