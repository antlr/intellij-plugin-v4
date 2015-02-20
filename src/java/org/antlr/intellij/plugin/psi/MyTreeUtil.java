package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.psi.iter.ASTFilter;

/**
 * Created by jason on 2/18/15.
 */
public class MyTreeUtil {

    public static ASTNode nextNonWhitespaceNode(ASTNode node) {
        for (ASTNode next = node.getTreeNext(); next != null; next = next.getTreeNext()) {
            if (!(next instanceof PsiWhiteSpace)) return next;
        }
        return null;
    }

    public static ASTNode findChild(ASTNode parent, ASTFilter filter) {
        return findSibling(parent.getFirstChildNode(), filter);
    }

    public static ASTNode findChild(ASTNode parent, IElementType type) {
        return findSibling(parent.getFirstChildNode(), type);
    }

    public static ASTNode findSibling(ASTNode start, IElementType type) {
        ASTNode child = start;
        while (true) {
            if (child == null) return null;
            if (child.getElementType() == type) return child;
            child = child.getTreeNext();
        }
    }

    public static ASTNode findSibling(ASTNode start, ASTFilter filter) {
        ASTNode child = start;
        while (true) {
            if (child == null) return null;
            if (filter.acceptNode(child)) return child;
            child = child.getTreeNext();
        }
    }
}
