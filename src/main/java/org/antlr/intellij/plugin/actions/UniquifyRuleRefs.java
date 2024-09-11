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
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Make every ref to a rule unique by dup'ing the rule and making them
 *  rule1, rule2, etc...
 */
public class UniquifyRuleRefs extends AnAction {
	@Override
	public void update(AnActionEvent e) {
		MyActionUtils.showOnlyIfSelectionIsRule(e, "Dup to Make %s Refs Unique");
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

		// find all parser and lexer rule refs
		final List<TerminalNode> rrefNodes = RefactorUtils.getAllRuleRefNodes(parser, tree, ruleName);
		if ( rrefNodes==null ) return;

		// find rule def
		final TerminalNode ruleDefNameNode = RefactorUtils.getRuleDefNameNode(parser, tree, ruleName);
		if ( ruleDefNameNode==null ) return;

		// alter rule refs and dup rules
		WriteCommandAction setTextAction = new WriteCommandAction(project) {
			@Override
			protected void run(final Result result) {
				// do in a single action so undo works in one go
				dupRuleAndMakeRefsUnique(doc, ruleName, rrefNodes);
			}
		};
		setTextAction.execute();
	}

	public void dupRuleAndMakeRefsUnique(Document doc,
	                                     String ruleName,
	                                     List<TerminalNode> rrefNodes)
	{
		int base = 0;
		int i = 1;
		int nrefs = rrefNodes.size();

		if ( nrefs==1 ) return; // no need to make unique if already unique

		for (TerminalNode t : rrefNodes) { // walk nodes in lexicographic order, replacing as we go
			Token rrefToken = t.getSymbol();
			String uniqueRuleName = ruleName+i;
			doc.replaceString(base+rrefToken.getStartIndex(), base+rrefToken.getStopIndex()+1, uniqueRuleName);
			// text shifts underneath us so we adjust token start/stop indexes into doc
			base += uniqueRuleName.length() - rrefToken.getText().length();
			i++;
		}

		// reparse to find new rule location
		String grammarText = doc.getText();
		ParsingResult results = ParsingUtils.parseANTLRGrammar(grammarText);
		Parser parser = results.parser;
		ParseTree tree = results.tree;
		CommonTokenStream tokens = (CommonTokenStream)parser.getTokenStream();

		// find rule def
		final TerminalNode ruleDefNameNode = RefactorUtils.getRuleDefNameNode(parser, tree, ruleName);
		if ( ruleDefNameNode==null ) return;

		final ParserRuleContext ruleDefNode = (ParserRuleContext)ruleDefNameNode.getParent();
		Token start = ruleDefNode.getStart();
		Token stop = ruleDefNode.getStop();
		int insertionPoint = start.getStartIndex();
		String javadoc = "";
		if ( start.getType()==ANTLRv4Lexer.DOC_COMMENT ) {
			javadoc = start.getText() + "\n";
		}

		// delete original rule
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

		// now insert nrefs copies of ruleText at insertionPoint
		final String ruleText = RefactorUtils.getRuleText(tokens, ruleDefNode);
		for (i=nrefs; i>=1; i--) {
			String uniqueRuleName = ruleName+i;
			final String newRule = javadoc + uniqueRuleName + " : " + ruleText + " ;" + "\n\n";
			doc.insertString(insertionPoint, newRule);
		}
	}
}
