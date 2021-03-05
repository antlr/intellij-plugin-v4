package org.antlr.intellij.plugin;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ANTLRv4FindUsagesProvider implements FindUsagesProvider {
	@Override
	public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
		return psiElement instanceof RuleSpecNode;
	}

	@Nullable
	@Override
	public WordsScanner getWordsScanner() {
		return null; // seems ok as JavaFindUsagesProvider does same thing
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
		if (element instanceof ModeSpecNode) {
			return "mode";
		}
		if (element instanceof TokenSpecNode) {
			return "token";
		}
		if (element instanceof ChannelSpecNode) {
			return "channel";
		}
		return "n/a";
	}

	@NotNull
	@Override
	public String getDescriptiveName(@NotNull PsiElement element) {
		PsiElement rule = PsiTreeUtil.findChildOfAnyType(element, LexerRuleRefNode.class, ParserRuleRefNode.class);
		if ( rule!=null ) return rule.getText();
		return "n/a";
	}

	@NotNull
	@Override
	public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
		return getDescriptiveName(element);
	}
}
