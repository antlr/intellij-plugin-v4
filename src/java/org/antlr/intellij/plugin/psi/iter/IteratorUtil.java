package org.antlr.intellij.plugin.psi.iter;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Created by jason on 2/18/15.
 */
public class IteratorUtil {

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
