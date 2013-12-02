package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GrammarRuleRef extends PsiReferenceBase<ANTLRv4PSIElement> {
	String ruleName;
	public GrammarRuleRef(ANTLRv4PSIElement idNode, String ruleName) {
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
		return MyPsiUtils.findRuleSpecNodeAbove(getElement(), ruleName);
	}
}
