package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4Language;

public class MyPsiUtils {
	public static PsiElement findRuleSpecNodeAbove(GrammarElementRefNode element, final String ruleName) {
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

	public static PsiElement createLeafFromText(Project project, PsiElement context,
												String text, IElementType type)
	{
		PsiFileFactoryImpl factory = (PsiFileFactoryImpl)PsiFileFactory.getInstance(project);
		PsiElement el = factory.createElementFromText(text,
													  ANTLRv4Language.INSTANCE,
													  type,
													  context);
		return PsiTreeUtil.getDeepestFirst(el); // forces parsing of file!!
		// start rule depends on root passed in
	}

	public static PsiFile createFile(Project project, String text) {
		String fileName = "a.g4"; // random name but must be .g4
		PsiFileFactoryImpl factory = (PsiFileFactoryImpl)PsiFileFactory.getInstance(project);
		return factory.createFileFromText(fileName, ANTLRv4Language.INSTANCE,
										  text, false, false);
	}
}
