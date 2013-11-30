package org.antlr.intellij.plugin.structview;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.plugin.Icons;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ANTLRv4ItemPresentation implements ItemPresentation {
	protected final PsiElement element;

	protected ANTLRv4ItemPresentation(PsiElement element) {
		this.element = element;
	}

	@Nullable
	public String getLocationString() {
		return null;
	}

	@Override
	public String getPresentableText() {
		ASTNode node = element.getNode();
		return node.getText();
//		ANTLRv4TokenType psiNode = (ANTLRv4TokenType)node.getElementType();
//		if ( psiNode == ANTLRv4TokenTypes.ruleSpec ) {
//			RuleElement f = (RuleElement)element;
//			return f.getName();
//		}
//		return psiNode.toString();
	}

	@Nullable
	public Icon getIcon(boolean open) {
		return Icons.PARSER_RULE;
	}
}
