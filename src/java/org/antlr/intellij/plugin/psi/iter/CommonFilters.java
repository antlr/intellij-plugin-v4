package org.antlr.intellij.plugin.psi.iter;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * Created by jason on 2/18/15.
 */
public class CommonFilters {


    public static ASTFilter acceptingAll() {
        return AcceptAll.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <E extends PsiElement> PsiFilter<E> acceptingAllElements() {
        return AcceptAll.INSTANCE;
    }

    public static ASTFilter excludingWhitespace() {
        return ExcludeWhitespace.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <E extends PsiElement> PsiFilter<E> excludingWhitespaceElements() {
        return ExcludeWhitespace.INSTANCE;
    }

    public static ASTFilter acceptingNodes(TokenSet tokenSet) {
        return new TokenSetFilter(tokenSet);
    }

    @SuppressWarnings("unchecked")
    public static <E extends PsiElement> PsiFilter<E> acceptingElements(TokenSet tokenSet) {
        return new TokenSetFilter(tokenSet);
    }

    public static ASTFilter acceptingNodes(IElementType type) {
        return new ElementTypeFilter(type);
    }

    @SuppressWarnings("unchecked")
    public static <E extends PsiElement> PsiFilter<E> acceptingElements(IElementType type) {
        return new ElementTypeFilter(type);
    }

    static class ElementTypeFilter implements ASTFilter, PsiFilter {
        final IElementType elementType;

        ElementTypeFilter(IElementType elementType) {
            this.elementType = elementType;
        }

        @Override
        public boolean acceptNode(ASTNode node) {
            return node != null && node.getElementType() == elementType;
        }

        @Override
        public boolean acceptElement(PsiElement element) {
            return acceptNode(element.getNode());
        }
    }


    static class TokenSetFilter implements ASTFilter, PsiFilter {
        final TokenSet tokens;

        TokenSetFilter(TokenSet tokens) {
            this.tokens = tokens;
        }

        @Override
        public boolean acceptNode(ASTNode node) {
            return tokens.contains(node.getElementType());
        }

        @Override
        public boolean acceptElement(PsiElement element) {
            ASTNode node = element.getNode();
            return node != null && acceptNode(node);
        }
    }


    enum ExcludeWhitespace implements ASTFilter, PsiFilter {
        INSTANCE;

        @Override
        public boolean acceptNode(ASTNode node) {
            return node.getElementType() != TokenType.WHITE_SPACE;
        }

        @Override
        public boolean acceptElement(PsiElement element) {
            return element instanceof PsiWhiteSpace;
        }
    }

    enum AcceptAll implements ASTFilter, PsiFilter {
        INSTANCE;

        @Override
        public boolean acceptNode(ASTNode node) {
            return true;
        }

        @Override
        public boolean acceptElement(PsiElement element) {
            return true;
        }
    }
}
