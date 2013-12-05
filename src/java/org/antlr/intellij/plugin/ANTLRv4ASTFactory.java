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
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.psi.AtAction;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;
import org.antlr.intellij.plugin.psi.RulesNode;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public static String findPackageIfAny(ANTLRv4FileRoot gfile) {
		// Want to gen in package; look for:
		// @header { package org.foo.x; } which is an AtAction
		PsiElement[] hdrActions = collectAtActions(gfile, "header");
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
		PsiElement[] options = collectNodesWithName(file, "option");
		for (PsiElement o : options) {
			PsiElement[] tokenVocab = collectChildrenWithText(o, "tokenVocab");
			if ( tokenVocab.length>0 ) {
				PsiElement optionNode = tokenVocab[0].getParent();// tokenVocab[0] is id node
				PsiElement[] ids = collectChildrenOfType(optionNode, ANTLRv4TokenTypes.optionValue);
				vocabName = ids[0].getText();
			}
		}
		return vocabName;
	}


	public static PsiElement[] collectAtActions(PsiElement root, final String tokenText) {
		return PsiTreeUtil.collectElements(root, new PsiElementFilter() {
			@Override
			public boolean isAccepted(PsiElement element) {
				PsiElement p = element.getContext();
				if (p != null) p = p.getContext();
				return p instanceof AtAction &&
					element instanceof ParserRuleRefNode &&
					element.getText().equals(tokenText);
			}
		});
	}

	/** Search all internal and leaf nodes looking for token or internal node
	 *  with specific text.
	 *  This saves having to create lots of java classes just to identify psi nodes.
	 */
	public static PsiElement[] collectNodesWithName(PsiElement root, final String tokenText) {
		return PsiTreeUtil.collectElements(root, new PsiElementFilter() {
			@Override
			public boolean isAccepted(PsiElement element) {
				String tokenTypeName = element.getNode().getElementType().toString();
				return tokenTypeName.equals(tokenText);
			}
		});
	}

	public static PsiElement[] collectNodesWithText(PsiElement root, final String text) {
		return PsiTreeUtil.collectElements(root, new PsiElementFilter() {
			@Override
			public boolean isAccepted(PsiElement element) {
				return element.getText().equals(text);
			}
		});
	}

	public static PsiElement[] collectChildrenOfType(PsiElement root, final ANTLRv4TokenType tokenType) {
		List<PsiElement> elems = new ArrayList<PsiElement>();
		for (PsiElement child : root.getChildren()) {
			if ( child.getNode().getElementType() == tokenType ) {
				elems.add(child);
			}
		}
		return elems.toArray(new PsiElement[elems.size()]);
	}

	public static PsiElement[] collectChildrenWithText(PsiElement root, final String text) {
		List<PsiElement> elems = new ArrayList<PsiElement>();
		for (PsiElement child : root.getChildren()) {
			if ( child.getText().equals(text) ) {
				elems.add(child);
			}
		}
		return elems.toArray(new PsiElement[elems.size()]);
	}

}
