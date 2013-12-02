package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParserRuleRef extends PsiReferenceBase<ParserRuleRefNode> {
	String ruleName;
	public ParserRuleRef(ParserRuleRefNode idNode, String ruleName) {
		super(idNode, new TextRange(0, ruleName.length()));
		this.ruleName = ruleName;
	}

	@NotNull
	@Override
	public Object[] getVariants() {
		return ArrayUtil.EMPTY_OBJECT_ARRAY;
	}

	/** Called upon jump to def for this rule ref */
	@Nullable
	@Override
	public PsiElement resolve() {
		// root of all rules is RulesNode node so jump up and scan for ruleName
		RulesNode rules = PsiTreeUtil.getContextOfType(getElement(), RulesNode.class);
		PsiElementFilter defnode = new PsiElementFilter() {
			@Override
			public boolean isAccepted(PsiElement element) {
				PsiElement nameNode = element.getFirstChild();
				if ( nameNode==null ) return false;
				return element instanceof ParserRuleSpecNode && nameNode.getText().equals(ruleName);
			}
		};
		PsiElement[] ruleSpec = PsiTreeUtil.collectElements(rules, defnode);
		if ( ruleSpec.length>0 ) return ruleSpec[0];
		return null;
	}
}
