package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

public class InlineRule extends AnAction {
	/** Only show if selection is a lexer or parser rule */
	@Override
	public void update(AnActionEvent e) {
		Presentation presentation = e.getPresentation();
		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		if ( grammarFile==null ) {
			presentation.setEnabled(false);
			return;
		}

		PsiElement el = MyActionUtils.getSelectedPsiElement(e);
		if ( el==null ) {
			presentation.setEnabled(false);
			return;
		}

		ParserRuleRefNode parserRule = MyActionUtils.getParserRuleSurroundingRef(e);
		LexerRuleRefNode lexerRule = MyActionUtils.getLexerRuleSurroundingRef(e);

		if ( (lexerRule!=null && el instanceof LexerRuleRefNode) ||
			 (parserRule!=null && el instanceof ParserRuleRefNode) )
		{
			presentation.setVisible(true);
			String ruleName = el.getText();
			presentation.setText("Inline and Remove Rule "+ruleName);
		}
		else {
			presentation.setVisible(false);
		}
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		PsiElement el = MyActionUtils.getSelectedPsiElement(e);
		if ( el==null ) return;

		String ruleName = el.getText();
		System.out.println("inline "+ruleName);

		final PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		if (psiFile == null) return;

		String grammarText = psiFile.getText();
		ParsingResult results = ParsingUtils.parseANTLRGrammar(grammarText);
		final Parser parser = results.parser;
		final ParseTree tree = results.tree;

		CommonTokenStream tokens = (CommonTokenStream)parser.getTokenStream();
		TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);

		// find all parser and lexer rule refs
		List<TerminalNode> rrefNodes = MyActionUtils.getAllRuleRefNodes(parser, tree, ruleName);
		if ( rrefNodes==null ) return;

		// find rule def
		ParseTree ruleDefNameNode = MyActionUtils.getRuleDefNameNode(parser, tree, ruleName);
		if ( ruleDefNameNode==null ) return;

		// identify rhs of rule
		ParserRuleContext parent = (ParserRuleContext)ruleDefNameNode.getParent();
		Token start = parent.getStart();
		Token stop = parent.getStop();
		TerminalNode colonNode = parent.getToken(ANTLRv4Parser.COLON, 0);
		Token colon = colonNode.getSymbol();
		Token beforeSemi = tokens.get(stop.getTokenIndex()-1);
		Token afterColon = tokens.get(colon.getTokenIndex()+1);

		// trim whitespace before / after rule text
		if ( afterColon.getType()==ANTLRv4Lexer.WS ) {
			afterColon = tokens.get(afterColon.getTokenIndex()+1);
		}
		if ( beforeSemi.getType()==ANTLRv4Lexer.WS ) {
			beforeSemi = tokens.get(beforeSemi.getTokenIndex()-1);
		}
		String ruleText = tokens.getText(afterColon, beforeSemi);
		System.out.println("ruletext: "+ruleText);

		// if rule has outermost alt, must add (...) around insertion

		// replace rule refs with rule text
		for (TerminalNode t : rrefNodes) {
			Token rrefToken = t.getSymbol();
			rewriter.replace(rrefToken, ruleText);
		}

		// remove the inlined rule (lexer or parser)
		// remove extra whitespace but not trailing comments (if any)
		// javadoc is included in start (if any) as it's not hidden
		List<Token> hiddenTokensToRight = tokens.getHiddenTokensToRight(stop.getTokenIndex());
		if ( hiddenTokensToRight!=null && hiddenTokensToRight.size()>0 ) {
			Token afterSemi = hiddenTokensToRight.get(0);
			if ( afterSemi.getType() == ANTLRv4Lexer.WS ) {
				stop = afterSemi;
			}
		}
		rewriter.delete(start, stop);

		final Project project = e.getProject();
		MyPsiUtils.replacePsiFileFromText(project, psiFile, rewriter.getText());
	}
}
