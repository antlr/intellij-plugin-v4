package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InlineRuleAction extends AnAction {
	@Override
	public void update(AnActionEvent e) {
		MyActionUtils.showOnlyIfSelectionIsRule(e, "Inline and Remove Rule %s");
	}

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
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

		String grammarText = psiFile.getText();
		ParsingResult results = ParsingUtils.parseANTLRGrammar(grammarText);
		Parser parser = results.parser;
		ParseTree tree = results.tree;

		final CommonTokenStream tokens = (CommonTokenStream) parser.getTokenStream();

		// find all parser and lexer rule refs
		final List<TerminalNode> rrefNodes = RefactorUtils.getAllRuleRefNodes(parser, tree, ruleName);
		if ( rrefNodes==null ) return;

		// find rule def
		ParseTree ruleDefNameNode = RefactorUtils.getRuleDefNameNode(parser, tree, ruleName);
		if ( ruleDefNameNode==null ) return;

		// identify rhs of rule
		final ParserRuleContext ruleDefNode = (ParserRuleContext) ruleDefNameNode.getParent();
		String ruleText_ = RefactorUtils.getRuleText(tokens, ruleDefNode);

		// if rule has outermost alt, must add (...) around insertion
		// Look for ruleBlock, lexerRuleBlock
		if ( RefactorUtils.ruleHasMultipleOutermostAlts(parser, ruleDefNode) ) {
			ruleText_ = "("+ruleText_+")";
		}
		final String ruleText = ruleText_; // we ref from inner class; requires final

		// replace rule refs with rule text
		WriteCommandAction setTextAction = new WriteCommandAction(project) {
			@Override
			protected void run(final Result result) {
				// do in a single action so undo works in one go
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
