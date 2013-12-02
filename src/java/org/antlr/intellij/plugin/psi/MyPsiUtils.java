package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;

public class MyPsiUtils {
	public static PsiElement findRuleSpecNodeAbove(ANTLRv4PSIElement element, final String ruleName) {
		RulesNode rules = PsiTreeUtil.getContextOfType(element, RulesNode.class);
		return findRuleSpecNode(ruleName, rules);
	}

	public static PsiElement findRuleSpecNode(final String ruleName, RulesNode rules) {
		PsiElementFilter defnode = new PsiElementFilter() {
			@Override
			public boolean isAccepted(PsiElement element) {
				PsiElement nameNode = element.getFirstChild();
				if ( nameNode==null ) return false;
				return (element instanceof ParserRuleSpecNode || element instanceof LexerRuleSpecNode) &&
					   nameNode.getText().equals(ruleName);
			}
		};
		PsiElement[] ruleSpec = PsiTreeUtil.collectElements(rules, defnode);
		if ( ruleSpec.length>0 ) return ruleSpec[0];
		return null;
	}
}
