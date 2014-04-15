package org.antlr.intellij.plugin.preview;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** See "How can I use DocumentationProvider for all tokens?"
 *  http://devnet.jetbrains.com/message/5237260#5237260
 */
public class InputWindowDocumentationProvider extends AbstractDocumentationProvider {
	@Nullable
	@Override
	public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
		System.out.println("getQuickNavigateInfo");
		return "type: ID\nLine 1";
	}

	@Nullable
	@Override
	public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
		return null;
	}

	@Nullable
	@Override
	public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
		System.out.println("generateDoc");
		return "FOO";
	}

	@Nullable
	@Override
	public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
		System.out.println("getDocumentationElementForLookupItem");
		return null;
	}

	@Nullable
	@Override
	public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
		return null;
	}
}
