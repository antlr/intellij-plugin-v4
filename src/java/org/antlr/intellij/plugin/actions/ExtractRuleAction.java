package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.PsiElement;

public class ExtractRuleAction extends AnAction {
	/** Only show if selection is a lexer or parser rule */
	@Override
	public void update(AnActionEvent e) {
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		PsiElement el = MyActionUtils.getSelectedPsiElement(e);
		if ( el==null ) return;
	}
}
