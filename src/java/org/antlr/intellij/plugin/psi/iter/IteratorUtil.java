package org.antlr.intellij.plugin.psi.iter;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Created by jason on 2/18/15.
 */
public class IteratorUtil {
    @SuppressWarnings("unchecked")
    static <T> Iterable<T> emptyIterable() {
        return EmptyIter.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <T> Iterator<T> emptyIterator() {
        return EmptyIter.INSTANCE;
    }

    enum EmptyIter implements Iterable, Iterator, PeekingIterator {
        INSTANCE;

        @Override
        public Object peek() {
            throw new NoSuchElementException();
        }

        @NotNull
        @Override
        public Iterator iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    static Iterable<ASTNode> directChildrenOf(final ASTNode parent) {
        if (parent == null) return emptyIterable();
        return new ASTIterable() {
            @NotNull
            @Override
            public Iterator<ASTNode> iterator() {
                ASTNode firstChild = parent.getFirstChildNode();
                if (firstChild == null) return emptyIterator();
                return new DirectChildrenIterator(firstChild);
            }
        };
    }

    static class DirectChildrenIterator implements ListIterator<ASTNode>, PeekingIterator<ASTNode> {
        public DirectChildrenIterator(ASTNode node) {
            this.node = node;
        }

        ASTNode node;

        @Override
        public boolean hasNext() {
            return node.getTreeNext() != null;
        }

        @Override
        public ASTNode peek() {
            return node.getTreeNext();
        }

        @Override
        public ASTNode next() {
            return node = node.getTreeNext();
        }

        @Override
        public boolean hasPrevious() {
            return node.getTreePrev() != null;
        }

        @Override
        public ASTNode previous() {
            return node = node.getTreePrev();
        }

        @Override
        public int nextIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int previousIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(ASTNode node) {

        }

        @Override
        public void add(ASTNode node) {

        }
    }

    static Iterable<PsiElement> ast2Psi(final Iterable<ASTNode> astIterable) {
        return new PsiIterable<PsiElement>() {
            @NotNull
            @Override
            public Iterator<PsiElement> iterator() {
                return ast2Psi(astIterable.iterator());
            }
        };
    }

    static Iterator<PsiElement> ast2Psi(final Iterator<ASTNode> astIterator) {
        return new Iterator<PsiElement>() {
            @Override
            public boolean hasNext() {
                return astIterator.hasNext();
            }

            @Override
            public PsiElement next() {
                return astIterator.next().getPsi();
            }

            @Override
            public void remove() {
                astIterator.remove();
            }
        };
    }

    static Iterable<ASTNode> filter(final Iterable<ASTNode> source, final ASTFilter predicate) {
        return new ASTIterable() {
            @NotNull
            @Override
            public Iterator<ASTNode> iterator() {
                return new ASTFilterIterator(source.iterator(), predicate);
            }
        };
    }

    static class ASTFilterIterator extends AbstractIterator<ASTNode> implements PeekingIterator<ASTNode> {

        final Iterator<ASTNode> source;
        final ASTFilter filter;

        ASTFilterIterator(Iterator<ASTNode> source, ASTFilter filter) {
            this.source = source;
            this.filter = filter;
        }

        @Override
        protected ASTNode computeNext() {
            while (source.hasNext()) {
                ASTNode next = source.next();
                if (filter.acceptNode(next)) return next;
            }
            return endOfData();
        }
    }
}
