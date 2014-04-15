package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreviewTokenRef extends PsiReferenceBase<PreviewTokenNode> {
//	public static final IElementType dummyType = new IElementType("DummyType", PreviewLanguage.INSTANCE);
//	public static class DummyNode extends LeafPsiElement implements PsiNamedElement {
//		public DummyNode(IElementType type, CharSequence text) {
//			super(type, text);
//		}
//		@Override
//		public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
//			throw new IncorrectOperationException();
//		}
//	}

	public PreviewTokenRef(PreviewTokenNode element, TextRange range) {
		super(element, range);
	}

	@Nullable
	@Override
	public PsiElement resolve() {
//		DummyNode dummy = new DummyNode(dummyType, "dummy");
		return getElement().getParent().getParent();
//		return getElement(); // return same node
	}


	@NotNull
	@Override
	public Object[] getVariants() {
		return new PsiElement[] {getElement()};
//		return new String[] {"hi","mom"};
//		return new Object[0];
	}
}
