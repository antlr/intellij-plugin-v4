package org.antlr.intellij.plugin.resolve;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.psi.GrammarElementRefNode;

import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.RULE_ELEMENT_TYPES;
import static org.antlr.intellij.plugin.resolve.TokenVocabResolver.findRelativeFile;

public class ImportResolver {

	public static PsiFile resolveImportedFile(GrammarElementRefNode reference) {
		PsiElement importStatement = PsiTreeUtil.findFirstParent(reference, ImportResolver::isImportStatement);

		if ( importStatement!=null ) {
			return findRelativeFile(reference.getText(), reference.getContainingFile());
		}

		return null;
	}


	private static boolean isImportStatement(PsiElement el) {
		ASTNode node = el.getNode();
		return node != null && node.getElementType() == RULE_ELEMENT_TYPES.get(org.antlr.intellij.plugin.parser.ANTLRv4Parser.RULE_delegateGrammar);
	}

}
