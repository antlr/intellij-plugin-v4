package org.antlr.intellij.plugin.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.jetbrains.annotations.NotNull;

public class AtAction extends ASTWrapperPsiElement {
	public AtAction(@NotNull ASTNode node) {
		super(node);
	}

	@NotNull
	public String getIdText() {
		PsiElement id = findChildByType(ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_identifier));

		return id==null ? "<n/a>" : id.getText();
	}

	@NotNull
	public String getActionBlockText() {
		PsiElement actionBlock = findChildByType(ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_actionBlock));

		if (actionBlock != null) {
			PsiElement openingBrace = actionBlock.getFirstChild();
			PsiElement closingBrace = actionBlock.getLastChild();

			return actionBlock.getText().substring(openingBrace.getStartOffsetInParent() + 1, closingBrace.getStartOffsetInParent());
		}

		return "";
	}

	public static class Factory implements PsiElementFactory {
		public static Factory INSTANCE = new Factory();

		@Override
		public PsiElement createElement(ASTNode node) {
			return new AtAction(node);
		}
	}
}
