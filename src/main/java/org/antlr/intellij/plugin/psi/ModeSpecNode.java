package org.antlr.intellij.plugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.jetbrains.annotations.NotNull;

import static org.antlr.intellij.plugin.psi.MyPsiUtils.findFirstChildOfType;

/**
 * A node representing a lexical {@code mode} definition, and all its child rules.
 *
 * @implNote this is technically not a 'rule', but it has the same characteristics as a named rule so we
 * can extend {@code RuleSpecNode}
 */
public class ModeSpecNode extends RuleSpecNode {

    public ModeSpecNode(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public IElementType getRuleRefType() {
        return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF);
    }

    @Override
    public GrammarElementRefNode getNameIdentifier() {
        PsiElement idNode = findFirstChildOfType(this, ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_identifier));

        if (idNode != null) {
            PsiElement firstChild = idNode.getFirstChild();

            if (firstChild instanceof GrammarElementRefNode) {
                return (GrammarElementRefNode) firstChild;
            }
        }

        return null;
    }

    public static class Factory implements PsiElementFactory {
        public static Factory INSTANCE = new Factory();

        @Override
        public PsiElement createElement(ASTNode node) {
            return new ModeSpecNode(node);
        }
    }
}