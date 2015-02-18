package org.antlr.intellij.plugin.psi.iter;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Created by jason on 2/17/15.
 */
public abstract class ASTIterable implements Iterable<ASTNode> {


    private final Iterable<ASTNode> myIterable;

    protected ASTIterable() {
        myIterable = this;
    }

    ASTIterable(@NotNull Iterable<ASTNode> iterable) {
        myIterable = iterable;
    }


    public static ASTIterable from(final Iterable<ASTNode> iterable) {
        return iterable instanceof ASTIterable ? (ASTIterable) iterable :
                new ASTIterable(iterable) {
                    @NotNull
                    @Override
                    public Iterator<ASTNode> iterator() {
                        return iterable.iterator();
                    }
                };
    }

    public static ASTIterable depthFirst(ASTNode start) {
        return from(DepthFirstTreeIterator.ASTIterator.createIterable(start));
    }

    @SuppressWarnings("LoopStatementThatDoesntLoop")
    public ASTNode first() {
        for (ASTNode node : this) return node;
        return null;
    }

    public PeekingIterator<ASTNode> peekingIterator() {
        return Iterators.peekingIterator(iterator());
    }

    public PsiIterable<?> mapToPsi() {
        return PsiIterable.from(new PsiIterable<PsiElement>() {
            @NotNull
            @Override
            public Iterator<PsiElement> iterator() {
                return ast2Psi(myIterable.iterator());
            }
        });
    }

    static Iterator<PsiElement> ast2Psi(final Iterator<ASTNode> ast) {
        return new Iterator<PsiElement>() {
            @Override
            public boolean hasNext() {
                return ast.hasNext();
            }

            @Override
            public PsiElement next() {
                return ast.next().getPsi();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public ASTIterable filter(IElementType type) {
        return filter(CommonFilters.acceptingNodes(type));
    }

    public ASTIterable filter(final TokenSet tokenSet) {
        return filter(CommonFilters.acceptingNodes(tokenSet));
    }

    public ASTIterable filter(ASTFilter filter) {
        return from(ASTFilterIterator.createIterable(myIterable, filter));
    }


    static class ASTFilterIterator extends AbstractIterator<ASTNode> implements PeekingIterator<ASTNode> {

        static Iterable<ASTNode> createIterable(final Iterable<ASTNode> source, final ASTFilter predicate) {
            return new ASTIterable() {
                @NotNull
                @Override
                public Iterator<ASTNode> iterator() {
                    return new ASTFilterIterator(source.iterator(), predicate);
                }
            };
        }


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
