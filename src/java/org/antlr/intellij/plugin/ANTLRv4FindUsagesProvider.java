package org.antlr.intellij.plugin;

import com.intellij.find.impl.HelpID;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ANTLRv4FindUsagesProvider implements FindUsagesProvider {
	@Override
	public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
		return true;
//		return psiElement instanceof PsiNamedElement;
	}

	@Nullable
	@Override
	public WordsScanner getWordsScanner() {
		throw new RuntimeException("wtf?");
		// not called!
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
	public String getHelpId(@NotNull PsiElement psiElement) {
		return HelpID.FIND_OTHER_USAGES;
	}

	@NotNull
	@Override
	public String getType(@NotNull PsiElement element) {
		return "rulellll";
	}

	@NotNull
	@Override
	public String getDescriptiveName(@NotNull PsiElement element) {
		PsiElement rule = PsiTreeUtil.findChildOfAnyType(element,
														 new Class[]{LexerRuleRefNode.class, ParserRuleRefNode.class});
		return rule.getText();
	}

	@NotNull
	@Override
	public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
		return getDescriptiveName(element);
	}


}
