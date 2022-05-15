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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.*;
import org.jetbrains.annotations.NotNull;

public class ANTLRv4ASTFactory extends ASTFactory {
	private static final Object2ObjectOpenHashMap<IElementType, PsiElementFactory> ruleElementTypeToPsiFactory = new Object2ObjectOpenHashMap<>();

	static {
		// later auto gen with tokens from some spec in grammar?
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_rules), RulesNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_parserRuleSpec), ParserRuleSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_lexerRule), LexerRuleSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_grammarSpec), GrammarSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_modeSpec), ModeSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_action), AtAction.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_identifier), TokenSpecNode.Factory.INSTANCE);
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
		if ( type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF) ) {
			return new ParserRuleRefNode(type, text);
		}
		else if ( type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF) ) {
			return new LexerRuleRefNode(type, text);
		}
		else if ( type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.STRING_LITERAL) ) {
			return new StringLiteralElement(type, text);
		}
		else {
			return new LeafPsiElement(type, text);
		}
    }

	public static PsiElement createInternalParseTreeNode(ASTNode node) {
		PsiElementFactory factory = ruleElementTypeToPsiFactory.get(node.getElementType());

		if (factory != null) {
			return factory.createElement(node);
		}
		else {
			return new ASTWrapperPsiElement(node);
		}
	}

}
