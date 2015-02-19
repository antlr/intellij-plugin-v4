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

    interface TreeNavigator<T> {
        T nextSiblingOf(T element);

        T parentOf(T element);

        T firstChildOf(T element);

        enum ForAST implements TreeNavigator<ASTNode> {
            INSTANCE;

            @Override
            public ASTNode nextSiblingOf(ASTNode element) {
                return element.getTreeNext();
            }

            @Override
            public ASTNode parentOf(ASTNode element) {
                return element.getTreeParent();
            }

            @Override
            public ASTNode firstChildOf(ASTNode element) {
                return element.getFirstChildNode();
            }
        }

        enum ForPsi implements TreeNavigator<PsiElement> {
            INSTANCE;

            @Override
            public PsiElement nextSiblingOf(PsiElement element) {
                return element.getNextSibling();
            }

            @Override
            public PsiElement parentOf(PsiElement element) {
                return element.getParent();
            }

            @Override
            public PsiElement firstChildOf(PsiElement element) {
                return element.getFirstChild();
            }
        }
    }

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
            super(startFrom, TreeNavigator.ForAST.INSTANCE);
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
            super(startFrom, TreeNavigator.ForPsi.INSTANCE);
        }

    }


    DepthFirstTreeIterator(T startFrom, TreeNavigator<T> navigator) {
        this.navigator = navigator;
        this.startFrom = this.element = startFrom;
    }

    final T startFrom;

    T element;

    final TreeNavigator<T> navigator;

    protected T nextSiblingOf(T element) {
        return navigator.nextSiblingOf(element);
    }

    protected T parentOf(T element) {
        return navigator.parentOf(element);
    }

    protected T firstChildOf(T element) {
        return navigator.firstChildOf(element);
    }

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
