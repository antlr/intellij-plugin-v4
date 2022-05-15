package org.antlr.intellij.plugin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.*;
import org.jetbrains.annotations.NotNull;

public class ANTLRv4ASTFactory extends ASTFactory {
	private static final Int2ObjectOpenHashMap<PsiElementFactory> ruleElementTypeToPsiFactory = new Int2ObjectOpenHashMap<>();

	static {
		// later auto gen with tokens from some spec in grammar?
		ruleElementTypeToPsiFactory.put(ANTLRv4Parser.RULE_rules, RulesNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4Parser.RULE_parserRuleSpec, ParserRuleSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4Parser.RULE_lexerRule, LexerRuleSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4Parser.RULE_grammarSpec, GrammarSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4Parser.RULE_modeSpec, ModeSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4Parser.RULE_action, AtAction.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4Parser.RULE_identifier, TokenSpecNode.Factory.INSTANCE);
	}

	/** Create a FileElement for root or a parse tree CompositeElement (not
	 *  PSI) for the token. This impl is more or less the default.
	 */
    @Override
    public CompositeElement createComposite(@NotNull IElementType type) {
        if (type instanceof IFileElementType) {
            return new FileElement(type, null);
		}
        return new CompositeElement(type);
    }

	/** Create PSI nodes out of tokens so even parse tree sees them as such.
	 *  Does not see whitespace tokens.
	 */
    @Override
    public LeafElement createLeaf(@NotNull IElementType type, @NotNull CharSequence text) {
		LeafElement t;
		if ( type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF) ) {
			t = new ParserRuleRefNode(type, text);
		}
		else if ( type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF) ) {
			t = new LexerRuleRefNode(type, text);
		}
		else if ( type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.STRING_LITERAL) ) {
			t = new StringLiteralElement(type, text);
		}
		else {
			t = new LeafPsiElement(type, text);
		}
		return t;
    }

	public static PsiElement createInternalParseTreeNode(ASTNode node) {
		PsiElement t;
		int typeIndex = node.getElementType().getIndex();
		PsiElementFactory factory = ruleElementTypeToPsiFactory.get(typeIndex);
		if (factory != null) {
			t = factory.createElement(node);
		}
		else {
			t = new ASTWrapperPsiElement(node);
		}

		return t;
	}

}
