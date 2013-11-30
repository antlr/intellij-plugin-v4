package org.antlr.intellij.plugin.structview;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;
import org.antlr.intellij.plugin.psi.RuleElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ANTLRv4StructureViewElement implements StructureViewTreeElement, SortableTreeElement {
	private PsiElement element;

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

	@Override
	public String getAlphaSortKey() {
		return element instanceof PsiNamedElement ? ((PsiNamedElement) element).getName() : null;
	}

	@Override
	public ItemPresentation getPresentation() {
		return new ANTLRv4ItemPresentation(element);
	}

	@Override
	public TreeElement[] getChildren() {
		if (element instanceof ANTLRv4FileRoot) {
			// now jump into grammar to look for rules
			Collection<ParserRuleSpecNode> rules = PsiTreeUtil.collectElementsOfType(element, ParserRuleSpecNode.class);
			System.out.println("rules="+rules);
			List<TreeElement> treeElements = new ArrayList<TreeElement>(rules.size());
			for (PsiElement el : rules) {
				RuleElement r = PsiTreeUtil.findChildOfType(el, RuleElement.class);
				treeElements.add(new ANTLRv4StructureViewElement(r));
			}
			return treeElements.toArray(new TreeElement[treeElements.size()]);
		}
		return EMPTY_ARRAY;
	}
}
