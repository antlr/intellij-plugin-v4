package org.antlr.intellij.plugin.resolve;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.RULE_ELEMENT_TYPES;

public class TokenVocabResolver {

	/**
	 * If this reference is the value of a {@code tokenVocab} option, returns the corresponding
	 * grammar file.
	 */
	@Nullable
	public static PsiFile resolveTokenVocabFile(PsiElement reference) {
		PsiElement optionValue = PsiTreeUtil.findFirstParent(reference, TokenVocabResolver::isOptionValue);

		if (optionValue != null) {
			PsiElement option = optionValue.getParent();

			if (option != null) {
				PsiElement optionName = PsiTreeUtil.getDeepestFirst(option);

				if (optionName.getText().equals("tokenVocab")) {
					String text = StringUtils.strip(reference.getText(), "'");
					return findRelativeFile(text, reference.getContainingFile());
				}
			}
		}

		return null;
	}

	/**
	 * Tries to find a declaration named {@code ruleName} in the {@code tokenVocab} file if it exists.
	 */
	@Nullable
	public static PsiElement resolveInTokenVocab(GrammarElementRefNode reference, String ruleName) {
		String tokenVocab = MyPsiUtils.findTokenVocabIfAny((ANTLRv4FileRoot) reference.getContainingFile());

		if (tokenVocab != null) {
			PsiFile tokenVocabFile = findRelativeFile(tokenVocab, reference.getContainingFile());

			if (tokenVocabFile != null) {
				GrammarSpecNode lexerGrammar = PsiTreeUtil.findChildOfType(tokenVocabFile, GrammarSpecNode.class);
				PsiElement node = MyPsiUtils.findSpecNode(lexerGrammar, ruleName);

				if (node instanceof LexerRuleSpecNode) {
					// fragments are not visible to the parser
					if (!((LexerRuleSpecNode) node).isFragment()) {
						return node;
					}
				}
				if (node instanceof TokenSpecNode) {
					return node;
				}
			}
		}

		return null;
	}

	private static boolean isOptionValue(PsiElement el) {
		ASTNode node = el.getNode();
		return node != null && node.getElementType() == RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_optionValue);
	}

	/**
	 * Looks for an ANTLR grammar file named {@code <baseName>}.g4 next to the given {@code sibling} file.
	 */
	static PsiFile findRelativeFile(String baseName, PsiFile sibling) {
		PsiDirectory parentDirectory = sibling.getParent();

		if (parentDirectory != null) {
			PsiFile candidate = parentDirectory.findFile(baseName + ".g4");

			if (candidate instanceof ANTLRv4FileRoot) {
				return candidate;
			}
		}

		return null;
	}
}
