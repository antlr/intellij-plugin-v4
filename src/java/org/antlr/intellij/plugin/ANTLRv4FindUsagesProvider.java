package org.antlr.intellij.plugin;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ANTLRv4FindUsagesProvider implements FindUsagesProvider {
	@Override
	public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
//		System.out.println("find usages for "+psiElement+": "+psiElement.getText());
		return psiElement instanceof LexerRuleSpecNode ||
			   psiElement instanceof ParserRuleSpecNode;
//		return psiElement instanceof PsiNamedElement;
	}

	@Nullable
	@Override
	public WordsScanner getWordsScanner() {
		return null; // seems ok as JavaFindUsagesProvider does same thing
//		System.out.println("getWordsScanner()");
//		final ANTLRv4Lexer lexer = new ANTLRv4Lexer(null);
//
//		LexerATNSimulator sim =
//			ANTLRUtils.getLexerATNSimulator(lexer, ANTLRv4Lexer._ATN, lexer.getInterpreter().decisionToDFA,
//											lexer.getInterpreter().getSharedContextCache());
//		lexer.setInterpreter(sim);
//		WordsScanner scanner =
//			new DefaultWordsScanner(new LexerAdaptor(lexer),
//									TokenSet.create(ANTLRv4TokenTypes.RULE_REF,
//													ANTLRv4TokenTypes.TOKEN_REF),
//									ANTLRv4TokenTypes.COMMENTS,
//									TokenSet.create(ANTLRv4TokenTypes.STRING_LITERAL)
//			);
//		return scanner;
	}

	@Nullable
	@Override
	public String getHelpId(@NotNull PsiElement element) {
		return null;
	}

	@NotNull
	@Override
	public String getType(@NotNull PsiElement element) {
		if (element instanceof ParserRuleSpecNode) {
			return "parser rule";
		}
		if (element instanceof LexerRuleSpecNode) {
			return "lexer rule";
		}
		return "n/a";
	}

	@NotNull
	@Override
	public String getDescriptiveName(@NotNull PsiElement element) {
		PsiElement rule = PsiTreeUtil.findChildOfAnyType(element,
														 new Class[]{LexerRuleRefNode.class, ParserRuleRefNode.class});
		if ( rule!=null ) return rule.getText();
		return "n/a";
	}

	@NotNull
	@Override
	public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
		return getDescriptiveName(element);
	}
}
