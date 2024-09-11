package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import org.antlr.intellij.plugin.generators.LiteralChooser;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.pattern.ParseTreeMatch;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.antlr.v4.runtime.tree.xpath.XPath;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class GenerateLexerRulesForLiteralsAction extends AnAction {
	public static final Logger LOG = Logger.getInstance("GenerateLexerRulesForLiterals");

	/** Only show if selection is a literal */
	@Override
	public void update(AnActionEvent e) {
		Presentation presentation = e.getPresentation();
		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		PsiFile file = e.getData(LangDataKeys.PSI_FILE);
		Editor editor = e.getData(PlatformDataKeys.EDITOR);

		if (grammarFile == null || file == null || editor == null ) {
			presentation.setEnabled(false);
			return;
		}
		PsiElement selectedElement = BaseRefactoringAction.getElementAtCaret(editor, file);
		if ( selectedElement==null ) { // we clicked somewhere outside text
			presentation.setEnabled(false);
		}
	}

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		LOG.info("actionPerformed GenerateLexerRulesForLiteralsAction");
		final Project project = e.getProject();

		final PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		if (psiFile == null) {
			return;
		}
		String inputText = psiFile.getText();
		ParsingResult results = ParsingUtils.parseANTLRGrammar(inputText);

		final Parser parser = results.parser;
		final ParseTree tree = results.tree;
		Collection<ParseTree> literalNodes = XPath.findAll(tree, "//ruleBlock//STRING_LITERAL", parser);
		LinkedHashMap<String, String> lexerRules = new LinkedHashMap<>();
		for (ParseTree node : literalNodes) {
			String literal = node.getText();
			String ruleText = String.format("%s : %s ;",
											RefactorUtils.getLexerRuleNameFromLiteral(literal), literal);
			lexerRules.put(literal, ruleText);
		}

		// remove those already defined
		String lexerRulesXPath = "//lexerRule";
		String treePattern = "<TOKEN_REF> : <STRING_LITERAL>;";
		ParseTreePattern p = parser.compileParseTreePattern(treePattern, ANTLRv4Parser.RULE_lexerRule);
		List<ParseTreeMatch> matches = p.findAll(tree, lexerRulesXPath);

		for (ParseTreeMatch match : matches) {
			ParseTree lit = match.get("STRING_LITERAL");
			if (lexerRules.containsKey(lit.getText())) { // we have rule for this literal already
				lexerRules.remove(lit.getText());
			}
		}

		final LiteralChooser chooser =
			new LiteralChooser(project, new ArrayList<>(lexerRules.values()));
		chooser.show();
		List<String> selectedElements = chooser.getSelectedElements();
		// chooser disposed automatically.

		final Editor editor = e.getData(PlatformDataKeys.EDITOR);
		final Document doc = editor.getDocument();
		final CommonTokenStream tokens = (CommonTokenStream) parser.getTokenStream();
		if (selectedElements != null) {
			String text = doc.getText();
			int cursorOffset = editor.getCaretModel().getOffset();
			// make sure it's not in middle of rule; put between.
			Collection<ParseTree> allRuleNodes = XPath.findAll(tree, "//ruleSpec", parser);
			for (ParseTree r : allRuleNodes) {
				Interval extent = r.getSourceInterval(); // token indexes
				int start = tokens.get(extent.a).getStartIndex();
				int stop = tokens.get(extent.b).getStopIndex();
				if (cursorOffset < start) {
					// before this rule, so must be between previous and this one
					cursorOffset = start; // put right before this rule
					break;
				}
				else if (cursorOffset >= start && cursorOffset <= stop) {
					// cursor in this rule
					cursorOffset = stop + 2; // put right before this rule (after newline)
					if (cursorOffset >= text.length()) {
						cursorOffset = text.length();
					}
					break;
				}
			}

			String allRules = Utils.join(selectedElements.iterator(), "\n");
			text =
				text.substring(0, cursorOffset) +
					"\n" + allRules + "\n" +
					text.substring(cursorOffset, text.length());
			MyPsiUtils.replacePsiFileFromText(project, psiFile, text);
		}
	}

}
