package org.antlr.intellij.plugin.psi.iter;

import com.intellij.psi.PsiElement;

/**
 * Created by jason on 2/17/15.
 */
public abstract class PsiFilter<E extends PsiElement> {
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> PsiFilter<T> acceptingAll() {
        return ACCEPT_ALL;
    }

    static final PsiFilter ACCEPT_ALL = new PsiFilter() {
        @Override
        public boolean accept(PsiElement node) {
            return true;
        }
    };

    public abstract boolean accept(E node);
}
