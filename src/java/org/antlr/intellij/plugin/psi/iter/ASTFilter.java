package org.antlr.intellij.plugin.psi.iter;

import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;

/**
 * Created by jason on 2/17/15.
 */
public abstract class ASTFilter {
    public abstract boolean accept(ASTNode node);

    public static ASTFilter acceptingAll() {
        return AcceptAll.INSTANCE;
    }

    public static ASTFilter excludingWhitespace() {
        return ExcludeWhitespace.INSTANCE;
    }

    static class ExcludeWhitespace extends ASTFilter {
        static final ExcludeWhitespace INSTANCE = new ExcludeWhitespace();

        @Override
        public boolean accept(ASTNode node) {
            return node.getElementType()!= TokenType.WHITE_SPACE;
        }
    }

    static class AcceptAll extends ASTFilter {
        static final AcceptAll INSTANCE = new AcceptAll();

        @Override
        public boolean accept(ASTNode node) {
            return true;
        }
    }

}
