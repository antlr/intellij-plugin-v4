package org.antlr.intellij.plugin.structview;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ANTLRv4StructureViewElement implements StructureViewTreeElement {
	private final PsiElement element;

	public ANTLRv4StructureViewElement(PsiElement element) {
		this.element = element;
	}

	@Override
	public Object getValue() {
		return element;
	}

	@Override
	public void navigate(boolean requestFocus) {
		if (element instanceof NavigationItem) {
			((NavigationItem) element).navigate(requestFocus);
		}
	}

	@Override
	public boolean canNavigate() {
		return element instanceof NavigationItem &&
			   ((NavigationItem)element).canNavigate();
	}

	@Override
	public boolean canNavigateToSource() {
		return element instanceof NavigationItem &&
			   ((NavigationItem)element).canNavigateToSource();
	}

	@NotNull
	@Override
	public ItemPresentation getPresentation() {
		return new ANTLRv4ItemPresentation(element);
	}

	@NotNull
	@Override
	public TreeElement[] getChildren() {
		List<TreeElement> treeElements = new ArrayList<>();

		if (element instanceof ANTLRv4FileRoot) {
			new PsiRecursiveElementVisitor() {
				@Override
				public void visitElement(PsiElement element) {
					if ( element instanceof ModeSpecNode ) {
						treeElements.add(new ANTLRv4StructureViewElement(element));
						return;
					}

					if ( element instanceof LexerRuleSpecNode || element instanceof ParserRuleSpecNode ) {
						PsiElement rule = PsiTreeUtil.findChildOfAnyType(element, LexerRuleRefNode.class, ParserRuleRefNode.class);
						if (rule != null) {
							treeElements.add(new ANTLRv4StructureViewElement(rule));
						}
					}

					super.visitElement(element);
				}
			}.visitElement(element);
		}
		else if ( element instanceof ModeSpecNode ) {
			LexerRuleSpecNode[] lexerRules = PsiTreeUtil.getChildrenOfType(element, LexerRuleSpecNode.class);

			if ( lexerRules != null ) {
				for ( LexerRuleSpecNode lexerRule : lexerRules ) {
					treeElements.add(new ANTLRv4StructureViewElement(PsiTreeUtil.findChildOfType(lexerRule, LexerRuleRefNode.class)));
				}
			}
		}

		return treeElements.toArray(new TreeElement[0]);
	}

	// probably not critical
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ANTLRv4StructureViewElement that = (ANTLRv4StructureViewElement)o;

		return Objects.equals(element, that.element);
	}

	@Override
	public int hashCode() {
		return element != null ? element.hashCode() : 0;
	}

}
