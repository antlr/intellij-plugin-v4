package org.antlr.intellij.plugin.preview;

import com.intellij.lang.ASTFactory;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

public class PreviewASTFactory extends ASTFactory {
	@Nullable
	@Override
	public LeafElement createLeaf(IElementType type, CharSequence text) {
		LeafElement t;
		t = new PreviewTokenNode(type, text);
//		System.out.println("create PreviewPsiLeaf "+t+" from "+type+" "+text);
		return t;
	}
}
