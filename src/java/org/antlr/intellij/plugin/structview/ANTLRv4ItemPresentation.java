package org.antlr.intellij.plugin.structview;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.IdRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
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
		if (element instanceof ANTLRv4FileRoot) {
			GrammarSpecNode gnode = PsiTreeUtil.findChildOfType(element, GrammarSpecNode.class);
			IdRefNode id = PsiTreeUtil.findChildOfType(gnode, IdRefNode.class);
			return id.getFirstChild().getText();
		}
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
		if ( element instanceof ParserRuleRefNode ) {
			return Icons.PARSER_RULE;
		}
		return Icons.LEXER_RULE;
	}
}
