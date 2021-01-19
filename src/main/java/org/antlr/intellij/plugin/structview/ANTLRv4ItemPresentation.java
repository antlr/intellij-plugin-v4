package org.antlr.intellij.plugin.structview;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.GrammarElementRefNode;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.ModeSpecNode;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
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
			PsiElement id = MyPsiUtils.findChildOfType(gnode, ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_identifier));
			if ( id!=null ) {
				return id.getText();
			}
			return "<n/a>";
		}
		if ( element instanceof ModeSpecNode ) {
			ModeSpecNode mode = (ModeSpecNode) element;
			GrammarElementRefNode modeId = mode.getNameIdentifier();
			if ( modeId!=null ) {
				return modeId.getName();
			}
			return "<n/a>";
		}
		ASTNode node = element.getNode();
		return node.getText();
	}

	@Nullable
	public Icon getIcon(boolean open) {
		return element.getIcon(0);
	}
}
