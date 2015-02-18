package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SimplifiableIfStatement")
public class MyPsiUtils {

	public static PsiElement findRuleSpecNodeAbove(GrammarElementRefNode element, final String ruleName) {
		RulesNode rules = PsiTreeUtil.getContextOfType(element, RulesNode.class);
		return findRuleSpecNode(ruleName, rules);
	}

	public static PsiElement findRuleSpecNode(final String ruleName, RulesNode rules) {
		PsiElementFilter defnode = new PsiElementFilter() {
			@Override
			public boolean isAccepted(PsiElement element) {
				PsiElement nameNode = element.getFirstChild();
				if ( nameNode==null ) return false;
				return (element instanceof ParserRuleSpecNode || element instanceof LexerRuleSpecNode) &&
					   nameNode.getText().equals(ruleName);
			}
		};
		PsiElement[] ruleSpec = PsiTreeUtil.collectElements(rules, defnode);
		if ( ruleSpec.length>0 ) return ruleSpec[0];
		return null;
	}

	public static PsiElement createLeafFromText(Project project, PsiElement context,
												String text, IElementType type)
	{
		PsiFileFactoryImpl factory = (PsiFileFactoryImpl)PsiFileFactory.getInstance(project);
		PsiElement el = factory.createElementFromText(text,
													  ANTLRv4Language.INSTANCE,
													  type,
													  context);
		return PsiTreeUtil.getDeepestFirst(el); // forces parsing of file!!
		// start rule depends on root passed in
	}

	public static PsiFile createFile(Project project, String text) {
		String fileName = "a.g4"; // random name but must be .g4
		PsiFileFactoryImpl factory = (PsiFileFactoryImpl)PsiFileFactory.getInstance(project);
		return factory.createFileFromText(fileName, ANTLRv4Language.INSTANCE,
										  text, false, false);
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

	public static PsiElement[] collectChildrenOfType(PsiElement root, final IElementType tokenType) {
		List<PsiElement> elems = new ArrayList<PsiElement>();
		for (PsiElement child : root.getChildren()) {
			if ( child.getNode().getElementType() == tokenType ) {
				elems.add(child);
			}
		}
		return elems.toArray(new PsiElement[elems.size()]);
	}

	public static PsiElement findChildOfType(PsiElement root, final IElementType tokenType) {
		List<PsiElement> elems = new ArrayList<PsiElement>();
		for (PsiElement child : root.getChildren()) {
			if ( child.getNode().getElementType() == tokenType ) {
				return child;
			}
		}
		return null;
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

	// Look for stuff like: options { tokenVocab=ANTLRv4Lexer; superClass=Foo; }
	public static String findTokenVocabIfAny(ANTLRv4FileRoot file) {
		String vocabName = null;
		PsiElement[] options = collectNodesWithName(file, "option");
		for (PsiElement o : options) {
			PsiElement[] tokenVocab = collectChildrenWithText(o, "tokenVocab");
			if ( tokenVocab.length>0 ) {
				PsiElement optionNode = tokenVocab[0].getParent();// tokenVocab[0] is id node
				PsiElement[] ids = collectChildrenOfType(optionNode, ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_optionValue));
				vocabName = ids[0].getText();
			}
		}
		return vocabName;
	}

	// Can use this in file root node to change find behavior:
	//	@Override
	//	public PsiElement findElementAt(int offset) {
	//		System.out.println("looking for element at " + offset);
	//		PsiElement p = dfs(this, offset);
	//		if ( p!=null ) {
	//			System.out.println("found at "+p+"="+p.getText());
	//		}
	//		return p;
	//	}

	public static PsiElement findElement(PsiElement startNode, int offset) {
		PsiElement p = startNode;
		if ( p==null ) return null;
		System.out.println(Thread.currentThread().getName()+": visit root "+p+
							   ", offset="+offset+
							   ", class="+p.getClass().getSimpleName()+
							   ", text="+p.getNode().getText()+
							   ", node range="+p.getTextRange());
//		if ( p.getTextRange().contains(offset) && p instanceof PreviewTokenNode) {
//			return p;
//		}
		PsiElement c = p.getFirstChild();
		while ( c!=null ) {
			//			System.out.println("visit child "+c+", text="+c.getNode().getText());
			PsiElement result = findElement(c, offset);
			if ( result!=null ) {
				return result;
			}
			c = c.getNextSibling();
		}
		return null;
	}

}
