package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RuleRef extends PsiReferenceBase<RuleRefNode> {
	String ruleName;
	public RuleRef(RuleRefNode idNode, String ruleName) {
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

	@Override
	public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
		Project project = getElement().getProject();
		myElement.replace(MyPsiUtils.createLeafFromText(project,
														myElement.getContext(),
														newElementName,
														ANTLRv4TokenTypes.TOKEN_REF));
		return myElement;
	}
}
