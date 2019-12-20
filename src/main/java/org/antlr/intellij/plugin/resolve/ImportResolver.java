package org.antlr.intellij.plugin.resolve;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.GrammarElementRefNode;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
		return node != null && node.getElementType() == RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_delegateGrammar);
	}

	public static PsiElement resolveInImportedFiles(@NotNull PsiFile grammarFile, @NotNull String ruleName) {
		return resolveInImportedFiles(grammarFile, ruleName, new ArrayList<>());
	}

	private static PsiElement resolveInImportedFiles(PsiFile grammarFile, String ruleName, List<PsiFile> visitedFiles) {
		DelegateGrammarsVisitor visitor = new DelegateGrammarsVisitor();
		grammarFile.accept(visitor);

		for ( PsiFile importedGrammar : visitor.importedGrammars ) {
			if ( visitedFiles.contains(importedGrammar) ) {
				continue;
			}
			visitedFiles.add(importedGrammar);

			GrammarSpecNode grammar = PsiTreeUtil.getChildOfType(importedGrammar, GrammarSpecNode.class);
			PsiElement specNode = MyPsiUtils.findSpecNode(grammar, ruleName);

			if ( specNode!=null ) {
				return specNode;
			}

			// maybe the imported grammar also imports other grammars itself?
			specNode = resolveInImportedFiles(importedGrammar, ruleName, visitedFiles);
			if ( specNode!=null ) {
				return specNode;
			}
		}

		return null;
	}

	private static class DelegateGrammarsVisitor extends PsiRecursiveElementVisitor {

		List<PsiFile> importedGrammars = new ArrayList<>();

		@Override
		public void visitElement(PsiElement element) {
			if ( isImportStatement(element) ) {
				PsiFile importedGrammar = findRelativeFile(element.getText(), element.getContainingFile());

				if (importedGrammar != null) {
					importedGrammars.add(importedGrammar);
				}
			}
			super.visitElement(element);
		}
	}
}
