package org.antlr.intellij.plugin.psi.iter;

import com.intellij.psi.PsiElement;

/**
 * Created by jason on 2/17/15.
 */
public interface PsiFilter<E extends PsiElement> {
    boolean acceptElement(E element);
}
