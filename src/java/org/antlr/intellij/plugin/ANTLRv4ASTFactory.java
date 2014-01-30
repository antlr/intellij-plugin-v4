package org.antlr.intellij.plugin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.psi.AtAction;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;
import org.antlr.intellij.plugin.psi.RulesNode;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ANTLRv4ASTFactory extends ASTFactory {
	public static Map<IElementType, Class> tokenTypeToPsiNode = new HashMap<IElementType, Class>();
	static {
		// later auto gen with tokens from some spec in grammar?
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_rules), RulesNode.class);
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_parserRuleSpec), ParserRuleSpecNode.class);
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_lexerRule), LexerRuleSpecNode.class);
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_grammarSpec), GrammarSpecNode.class);
		tokenTypeToPsiNode.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_action), AtAction.class);
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
		if ( type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF) ) {
			t = new ParserRuleRefNode(type, text);
		}
		else if ( type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF) ) {
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

	public static String findPackageIfAny(ANTLRv4FileRoot gfile) {
		// Want to gen in package; look for:
		// @header { package org.foo.x; } which is an AtAction
		PsiElement[] hdrActions = MyPsiUtils.collectAtActions(gfile, "header");
		if ( hdrActions.length>0 ) {
			PsiElement h = hdrActions[0];
			PsiElement p = h.getContext();
			PsiElement action = p.getNextSibling();
			if ( action instanceof PsiWhiteSpace) action = action.getNextSibling();
			String text = action.getText();
			Pattern pattern = Pattern.compile("\\{\\s*package\\s+(.*?);\\s*.*");
			Matcher matcher = pattern.matcher(text);
			if ( matcher.matches() ) {
				String pack = matcher.group(1);
				return pack;
			}
		}
		return null;
	}

		// Look for stuff like: options { tokenVocab=ANTLRv4Lexer; superClass=Foo; }
	public static String findTokenVocabIfAny(ANTLRv4FileRoot file) {
		String vocabName = null;
		PsiElement[] options = MyPsiUtils.collectNodesWithName(file, "option");
		for (PsiElement o : options) {
			PsiElement[] tokenVocab = MyPsiUtils.collectChildrenWithText(o, "tokenVocab");
			if ( tokenVocab.length>0 ) {
				PsiElement optionNode = tokenVocab[0].getParent();// tokenVocab[0] is id node
				PsiElement[] ids = MyPsiUtils.collectChildrenOfType(optionNode, ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_optionValue));
				vocabName = ids[0].getText();
			}
		}
		return vocabName;
	}


}
