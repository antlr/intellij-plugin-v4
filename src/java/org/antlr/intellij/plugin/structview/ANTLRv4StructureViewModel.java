package org.antlr.intellij.plugin.structview;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;
import org.jetbrains.annotations.NotNull;

public class ANTLRv4StructureViewModel extends StructureViewModelBase
	implements StructureViewModel.ElementInfoProvider
{
	ANTLRv4FileRoot rootElement;

	public ANTLRv4StructureViewModel(ANTLRv4FileRoot rootElement) {
		super(rootElement, new ANTLRv4StructureViewElement(rootElement));
		this.rootElement = rootElement;
	}


	@NotNull
	public Sorter[] getSorters() {
		return new Sorter[] {Sorter.ALPHA_SORTER};
	}

	@Override
	protected PsiFile getPsiFile() {
		return rootElement;
	}

	@NotNull
	@Override
	public StructureViewTreeElement getRoot() {
		return new ANTLRv4StructureViewElement(rootElement);
	}

	@Override
	public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
		return false;
	}

	@Override
	public boolean isAlwaysLeaf(StructureViewTreeElement element) {
		return element instanceof ANTLRv4FileRoot;
	}

	/**
	 Intellij: The implementation of StructureViewTreeElement.getChildren()
	 needs to be matched by TextEditorBasedStructureViewModel.getSuitableClasses().
	 The latter method returns an array of PsiElement-derived classes which can
	 be shown as structure view elements, and is used to select the Structure
	 View item matching the cursor position when the structure view is first
	 opened or when the "Autoscroll from source" option is used.
	 */
	@NotNull
	protected Class[] getSuitableClasses() {
		return new Class[] {ANTLRv4FileRoot.class,
			LexerRuleSpecNode.class,
			ParserRuleSpecNode.class};
	}
}
