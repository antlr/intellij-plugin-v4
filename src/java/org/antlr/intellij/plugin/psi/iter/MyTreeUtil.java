package org.antlr.intellij.plugin.psi.iter;

import com.intellij.lang.ASTNode;

/**
 * Created by jason on 2/18/15.
 */
public class MyTreeUtil {
    public static ASTNode findChild(ASTNode parent, ASTFilter filter) {
        return findSibling(parent.getFirstChildNode(), filter);
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
