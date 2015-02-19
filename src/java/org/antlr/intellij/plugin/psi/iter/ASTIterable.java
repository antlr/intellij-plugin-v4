package org.antlr.intellij.plugin.psi.iter;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.intellij.lang.ASTNode;
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
        return PsiIterable.from(IteratorUtil.ast2Psi(myIterable));
    }

    public ASTIterable filter(IElementType type) {
        return filter(CommonFilters.acceptingNodesWithElementType(type));
    }

    public ASTIterable filter(final TokenSet tokenSet) {
        return filter(CommonFilters.acceptingNodesWithElementTypes(tokenSet));
    }

    public ASTIterable filter(ASTFilter filter) {
        return from(IteratorUtil.filter(myIterable, filter));
    }

    public ASTIterable excludingWhitespace() {
        return filter(CommonFilters.excludingWhitespace());
    }
}
