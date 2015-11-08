package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;

import java.util.List;

public class InlineRuleAction extends AnAction {
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
			String ruleName = el.getText();
			presentation.setText("Inline and Remove Rule "+ruleName);
		}
		else {
			presentation.setEnabled(false);
		}
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		PsiElement el = MyActionUtils.getSelectedPsiElement(e);
		if ( el==null ) return;

		final String ruleName = el.getText();

		final PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		if ( psiFile==null ) return;

		final Project project = e.getProject();

		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		if ( editor==null ) return;
		final Document doc = editor.getDocument();
		int cursorOffset = editor.getCaretModel().getOffset();

		String grammarText = psiFile.getText();
		ParsingResult results = ParsingUtils.parseANTLRGrammar(grammarText);
		Parser parser = results.parser;
		ParseTree tree = results.tree;

		final CommonTokenStream tokens = (CommonTokenStream) parser.getTokenStream();
		TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);

		// find all parser and lexer rule refs
		final List<TerminalNode> rrefNodes = MyActionUtils.getAllRuleRefNodes(parser, tree, ruleName);
		if ( rrefNodes==null ) return;

		// find rule def
		ParseTree ruleDefNameNode = RefactorUtils.getRuleDefNameNode(parser, tree, ruleName);
		if ( ruleDefNameNode==null ) return;

		// identify rhs of rule
		final ParserRuleContext ruleDefNode = (ParserRuleContext) ruleDefNameNode.getParent();
		Token start = ruleDefNode.getStart();
		Token stop = ruleDefNode.getStop();
		Token semi = stop;
		TerminalNode colonNode = ruleDefNode.getToken(ANTLRv4Parser.COLON, 0);
		Token colon = colonNode.getSymbol();
		Token beforeSemi = tokens.get(stop.getTokenIndex()-1);
		Token afterColon = tokens.get(colon.getTokenIndex()+1);

		// trim whitespace/comments before / after rule text
		List<Token> ignoreBefore = tokens.getHiddenTokensToRight(colon.getTokenIndex());
		List<Token> ignoreAfter = tokens.getHiddenTokensToLeft(semi.getTokenIndex());
		Token textStart = afterColon;
		Token textStop = beforeSemi;
		if ( ignoreBefore!=null ) {
			Token lastWSAfterColon = ignoreBefore.get(ignoreBefore.size()-1);
			textStart = tokens.get(lastWSAfterColon.getTokenIndex()+1);
		}
		if ( ignoreAfter!=null ) {
			int firstWSAtEndOfRule = ignoreAfter.get(0).getTokenIndex()-1;
			textStop = tokens.get(firstWSAtEndOfRule); // stop before 1st ignore token at end
		}

		if ( false ) {
			String ruleText = tokens.getText(textStart, textStop);
//		System.out.println("ruletext: "+ruleText);

			// if rule has outermost alt, must add (...) around insertion
			// Look for ruleBlock, lexerRuleBlock
			if ( RefactorUtils.ruleHasMultipleOutermostAlts(parser, ruleDefNode) ) {
				ruleText = "("+ruleText+")";
			}

			boolean ruleIsDirectlyRecursive = false;

			// replace rule refs with rule text
			for (TerminalNode t : rrefNodes) {
				if ( Trees.isAncestorOf(ruleDefNode, t) ) {
					ruleIsDirectlyRecursive = true;
				}
				Token rrefToken = t.getSymbol();
				rewriter.replace(rrefToken, ruleText);
			}

			// remove the inlined rule (lexer or parser)
			List<Token> hiddenTokensToRight = tokens.getHiddenTokensToRight(stop.getTokenIndex());
			if ( hiddenTokensToRight!=null && hiddenTokensToRight.size()>0 ) {
				// remove extra whitespace but not trailing comments (if any)
				// javadoc is included in start (if any) as it's not hidden
				Token afterSemi = hiddenTokensToRight.get(0);
				if ( afterSemi.getType()==ANTLRv4Lexer.WS ) {
					stop = afterSemi;
				}
			}
			if ( !ruleIsDirectlyRecursive ) {
				// don't delete if we made replacements in the rule itself
				rewriter.delete(start, stop);
			}

			grammarText = rewriter.getText();
			RefactorUtils.replaceText(project, doc, 0, doc.getTextLength()-1, grammarText);

			MyActionUtils.moveCursor(editor, cursorOffset);
		}


		// TODO: currently replacing rule; try replacing just what we need to do
		// the following is close. it just doesn't know how to delete the rule.

		String ruleText_ = tokens.getText(textStart, textStop);
//		System.out.println("ruletext: "+ruleText);

		// if rule has outermost alt, must add (...) around insertion
		// Look for ruleBlock, lexerRuleBlock
		if ( RefactorUtils.ruleHasMultipleOutermostAlts(parser, ruleDefNode) ) {
			ruleText_ = "("+ruleText_+")";
		}
		final String ruleText = ruleText_; // we ref from inner class; requires final

		// replace rule refs with rule text
		WriteCommandAction setTextAction = new WriteCommandAction(project) {
			@Override
			protected void run(final Result result) throws Throwable {
				replaceRuleRefs(doc,tokens,ruleName,rrefNodes,ruleText);
			}
		};
		setTextAction.execute();
	}

	public void replaceRuleRefs(Document doc, CommonTokenStream tokens,
	                            String ruleName,
	                            List<TerminalNode> rrefNodes,
	                            String ruleText)
	{
		int base = 0;
		for (TerminalNode t : rrefNodes) { // walk nodes in lexicographic order, replacing as we go
			Token rrefToken = t.getSymbol();
			Token nextToken = tokens.get(rrefToken.getTokenIndex()+1);
			String thisReplacementRuleText = ruleText;
			if ( (nextToken.getType()==ANTLRv4Lexer.STAR ||
				nextToken.getType()==ANTLRv4Lexer.PLUS ||
				nextToken.getType()==ANTLRv4Lexer.QUESTION) &&
				!ruleText.startsWith("(") )
			{
				// need (...) if we replace foo* or foo+ and ruleText doesn't have parens yet
				thisReplacementRuleText = "(" + ruleText + ")";
			}
			doc.replaceString(base+rrefToken.getStartIndex(), base+rrefToken.getStopIndex()+1, thisReplacementRuleText);
			// text shifts underneath us so we adjust token start/stop indexes into doc
			base += thisReplacementRuleText.length() - ruleName.length();
		}

		// reparse to find new rule location
		String grammarText = doc.getText();
		ParsingResult results = ParsingUtils.parseANTLRGrammar(grammarText);
		Parser parser = results.parser;
		ParseTree tree = results.tree;

		results = ParsingUtils.parseANTLRGrammar(grammarText);
		parser = results.parser;
		tree = results.tree;

		tokens = (CommonTokenStream) parser.getTokenStream();

		// find rule def
		TerminalNode ruleDefNameNode = (TerminalNode)RefactorUtils.getRuleDefNameNode(parser, tree, ruleName);
		if ( ruleDefNameNode==null ) return;

		final ParserRuleContext ruleDefNode = (ParserRuleContext) ruleDefNameNode.getParent();
		Token start = ruleDefNode.getStart();
		Token stop = ruleDefNode.getStop();

		// check for direct recursive, in which case we don't delete it
		boolean ruleIsDirectlyRecursive = false;
		for (TerminalNode t : rrefNodes) {
			if ( Trees.isAncestorOf(ruleDefNode, t) ) {
				ruleIsDirectlyRecursive = true;
			}
		}

		// don't delete if we made replacements in the rule itself
		if ( ruleIsDirectlyRecursive ) return;

		// remove the inlined rule (lexer or parser)
		List<Token> hiddenTokensToRight = tokens.getHiddenTokensToRight(stop.getTokenIndex());
		if ( hiddenTokensToRight!=null && hiddenTokensToRight.size()>0 ) {
			// remove extra whitespace but not trailing comments (if any)
			// javadoc is included in start (if any) as it's not hidden
			Token afterSemi = hiddenTokensToRight.get(0);
			if ( afterSemi.getType()==ANTLRv4Lexer.WS ) {
				stop = afterSemi;
			}
		}

		doc.deleteString(start.getStartIndex(), stop.getStopIndex()+1);
	}
}
