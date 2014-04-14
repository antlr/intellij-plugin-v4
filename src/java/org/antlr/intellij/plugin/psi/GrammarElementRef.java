package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class GrammarElementRef extends PsiReferenceBase<GrammarElementRefNode> {
	String ruleName;
	public GrammarElementRef(GrammarElementRefNode idNode, String ruleName) {
		super(idNode, new TextRange(0, ruleName.length()));
		this.ruleName = ruleName;
	}

	/** Using for completion. Returns list of rules and tokens; the prefix
	 *  of current element is used as filter by IDEA later.
	 */
	@NotNull
	@Override
	public Object[] getVariants() {
		String prefix = myElement.getText();
		RulesNode rules = PsiTreeUtil.getContextOfType(myElement, RulesNode.class);
		// find all rule defs (token, parser)
		Collection<? extends RuleSpecNode> ruleSpecNodes =
			PsiTreeUtil.findChildrenOfAnyType(rules,
											  new Class[] {
												ParserRuleSpecNode.class,
											  	LexerRuleSpecNode.class}
											 );

		return ruleSpecNodes.toArray();
//
//		final ArrayList<LookupElement> list = new ArrayList<LookupElement>();
//		PsiFile containingFile = myElement.getContainingFile();
//		List<BnfRule> rules = containingFile instanceof BnfFile ? ((BnfFile)containingFile).getRules() : Collections.<BnfRule>emptyList();
//		for (BnfRule rule : rules) {
//			boolean fakeRule = ParserGeneratorUtil.Rule.isFake(rule);
//			boolean privateRule = ParserGeneratorUtil.Rule.isPrivate(rule);
//			list.add(LookupElementBuilder.createWithIcon(rule).withBoldness(!privateRule).withStrikeoutness(fakeRule));
//		}
//		if (GrammarUtil.isExternalReference(myElement)) {
//			BnfRule rule = PsiTreeUtil.getParentOfType(myElement, BnfRule.class);
//			String parserClass = ParserGeneratorUtil.getAttribute(rule, KnownAttribute.PARSER_UTIL_CLASS);
//			if (StringUtil.isNotEmpty(parserClass)) {
//				JavaHelper javaHelper = JavaHelper.getJavaHelper(myElement.getProject());
//				for (NavigatablePsiElement element : javaHelper.getClassMethods(parserClass, true)) {
//					List<String> methodTypes = javaHelper.getMethodTypes(element);
//					if (methodTypes.size() > 3 &&
//						methodTypes.get(0).equals("boolean") &&
//						methodTypes.get(1).equals("com.intellij.lang.PsiBuilder") &&
//						methodTypes.get(3).equals("int")) {
//						list.add(LookupElementBuilder.createWithIcon((PsiNamedElement)element));
//					}
//				}
//			}
//		}
//		return ArrayUtil.toObjectArray(list);
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
														ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF)));
		return myElement;
	}
}
