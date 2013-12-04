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
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.psi.AtAction;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;
import org.antlr.intellij.plugin.psi.RulesNode;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class ANTLRv4ASTFactory extends ASTFactory {
	public static Map<ANTLRv4TokenType, Class> tokenTypeToPsiNode = new HashMap<ANTLRv4TokenType, Class>();
	static { // later auto gen with tokens from some spec in grammar?
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.rules, RulesNode.class);
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.parserRuleSpec, ParserRuleSpecNode.class);
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.lexerRule, LexerRuleSpecNode.class);
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.grammarSpec, GrammarSpecNode.class);
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.action, AtAction.class);
	}

	/** Create a FileElement for root or a parse tree CompositeElement (not
	 *  PSI) for the token. This impl is more or less the default.
	 */
    @Override
    public CompositeElement createComposite(IElementType type) {
        if (type instanceof IFileElementType) {
            return new FileElement(type, null);
		}
        return new CompositeElement(type);
    }

	/** Create PSI nodes out of tokens so even parse tree sees them as such.
	 *  Does not see whitespace tokens.
	 */
    @Override
    public LeafElement createLeaf(IElementType type, CharSequence text) {
		LeafElement t;
		if ( type == ANTLRv4TokenTypes.RULE_REF ) {
			t = new ParserRuleRefNode(type, text);
		}
		else if ( type == ANTLRv4TokenTypes.TOKEN_REF ) {
			t = new LexerRuleRefNode(type, text);
		}
		else {
			t = new LeafPsiElement(type, text);
		}
//		System.out.println("createLeaf "+t+" from "+type+" "+text);
		return t;
    }

	// refactored from ANTLRv4ParserDefinition
	public static PsiElement createInternalParseTreeNode(ASTNode node) {
		IElementType tokenType = node.getElementType();
		Class clazz = tokenTypeToPsiNode.get(tokenType);
		PsiElement t;
		if ( clazz!=null ) {
			try {
				Constructor ctor = clazz.getConstructor(ASTNode.class);
				t = (PsiElement)ctor.newInstance(node);
			}
			catch (Exception e) {
				System.err.println("can't create psi node");
				t = new ASTWrapperPsiElement(node);
			}
		}
		else {
			t = new ASTWrapperPsiElement(node);
		}
//		System.out.println("PSI createElement "+t+" from "+elementType);
		return t;
	}

}
