package org.antlr.intellij.plugin.psi.iter;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Created by jason on 2/17/15.
 */
public abstract class DepthFirstTreeIterator<T> extends AbstractIterator<T> implements PeekingIterator<T> {

    static class ASTIterator extends DepthFirstTreeIterator<ASTNode> {
        static Iterable<ASTNode> createIterable(final ASTNode start) {
            return new Iterable<ASTNode>() {
                @NotNull
                @Override
                public Iterator<ASTNode> iterator() {
                    return new ASTIterator(start);
                }
            };
        }

        ASTIterator(ASTNode startFrom) {
            super(startFrom);
        }

        @Override
        protected ASTNode nextSiblingOf(ASTNode element) {
            return element.getTreeNext();
        }

        @Override
        protected ASTNode parentOf(ASTNode element) {
            return element.getTreeParent();
        }

        @Override
        protected ASTNode firstChildOf(ASTNode element) {
            return element.getFirstChildNode();
        }
    }

    static class PsiIterator extends DepthFirstTreeIterator<PsiElement> {
        static Iterable<PsiElement> createIterable(final PsiElement start) {
            return new Iterable<PsiElement>() {
                @NotNull
                @Override
                public Iterator<PsiElement> iterator() {
                    return new PsiIterator(start);
                }
            };
        }

        PsiIterator(PsiElement startFrom) {
            super(startFrom);
        }

        @Override
        protected PsiElement nextSiblingOf(PsiElement element) {
            return element.getNextSibling();
        }

        @Override
        protected PsiElement parentOf(PsiElement element) {
            return element.getParent();
        }

        @Override
        protected PsiElement firstChildOf(PsiElement element) {
            return element.getFirstChild();
        }
    }

    final T startFrom;

    DepthFirstTreeIterator(T startFrom) {
        this.startFrom = this.element = startFrom;

    }

    T element;

    protected abstract T nextSiblingOf(T element);

    protected abstract T parentOf(T element);

    protected abstract T firstChildOf(T element);

    private boolean tryNext(T candidate) {
        if (candidate != null) {
            element = candidate;
            return true;
        } else return false;

    }


    private boolean upAndOver(T parent) {
        while (parent != null && !parent.equals(startFrom)) {
            if (tryNext(nextSiblingOf(parent))) return true;
            else parent = parentOf(parent);
        }
        return false;

    }

    @Override
    protected T computeNext() {
        if (tryNext(firstChildOf(element)) ||
                tryNext(nextSiblingOf(element)) ||
                upAndOver(parentOf(element))) return element;
        return endOfData();

    }

}
