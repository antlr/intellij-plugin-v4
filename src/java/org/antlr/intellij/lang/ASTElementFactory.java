package org.antlr.intellij.lang;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

/**
 * This interface supports constructing a {@link PsiElement} from an {@link ASTNode}.
 */
public interface ASTElementFactory {
	PsiElement createElement(ASTNode node);
}
