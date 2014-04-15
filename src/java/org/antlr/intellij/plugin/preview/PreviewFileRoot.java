package org.antlr.intellij.plugin.preview;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.plugin.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PreviewFileRoot extends PsiFileBase {
    public PreviewFileRoot(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, PreviewLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return PreviewFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Sample input file";
    }

    @Override
    public Icon getIcon(int flags) {
		return Icons.FILE;
    }

	@NotNull
	@Override
	public PsiElement[] getChildren() {
		return super.getChildren();
	}

//	@Override
//	public PsiElement findElementAt(int offset) {
//		System.out.println("looking for element at " + offset);
//		PsiElement p = dfs(this, offset);
//		if ( p!=null ) {
//			System.out.println("found at "+p+"="+p.getText());
//		}
//		return p;
//	}

//	private PsiElement MyFindElementAt(int offset) {
//		int offsetInElement = offset;
//		PsiElement child = getFirstChild();
//		while (child != null) {
//			final int length = child.getTextLength();
//			if (length <= offsetInElement) {
//				offsetInElement -= length;
//				child = child.getNextSibling();
//				continue;
//			}
//			return child.findElementAt(offsetInElement);
//		}
//		return null;
//	}

	public PsiElement dfs(PsiElement p, int offset) {
		if ( p==null ) return null;
		System.out.println(Thread.currentThread().getName()+": visit root "+p+
							   ", offset="+offset+
							   ", class="+p.getClass().getSimpleName()+
							   ", text="+p.getNode().getText()+
							   ", node range="+p.getTextRange());
		if ( p.getTextRange().contains(offset) && p instanceof PreviewTokenNode ) {
			return p;
		}
		PsiElement c = p.getFirstChild();
		while ( c!=null ) {
//			System.out.println("visit child "+c+", text="+c.getNode().getText());
			PsiElement result = dfs(c, offset);
			if ( result!=null ) {
				return result;
			}
			c = c.getNextSibling();
		}
		return null;
	}

//	@Override
//	public PsiReference findReferenceAt(int offset) {
////		return super.findReferenceAt(offset);
//		System.out.println("looking for reference at "+offset);
//		PsiElement node = findElementAt(offset);
//		if ( node==null ) {
//			return null;
//		}
//		return node.getReference();
//	}
}
