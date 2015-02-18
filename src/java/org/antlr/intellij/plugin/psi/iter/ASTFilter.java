package org.antlr.intellij.plugin.psi.iter;

import com.intellij.lang.ASTNode;

/**
 * Created by jason on 2/17/15.
 */
public interface ASTFilter {
    boolean acceptNode(ASTNode node);
}
