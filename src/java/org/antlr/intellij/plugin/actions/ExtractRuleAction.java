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
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

public class ExtractRuleAction extends AnAction {
	/** Only show if user has selected region and is in a lexer or parser rule */
	@Override
	public void update(AnActionEvent e) {
		Presentation presentation = e.getPresentation();

		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		if ( grammarFile==null ) {
			presentation.setEnabled(false);
			return;
		}

		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		if ( editor==null ) {
			presentation.setEnabled(false);
			return;
		}

		ParserRuleRefNode parserRule = MyActionUtils.getParserRuleSurroundingRef(e);
		LexerRuleRefNode lexerRule = MyActionUtils.getLexerRuleSurroundingRef(e);
		if ( parserRule==null && lexerRule==null ) {
			presentation.setEnabled(false);
			return;
		}

		SelectionModel selectionModel = editor.getSelectionModel();
		if ( !selectionModel.hasSelection() ) {
			presentation.setEnabled(false);
		}

		// TODO: disable if selection spans rules
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		PsiElement el = MyActionUtils.getSelectedPsiElement(e);
		if ( el==null ) return;

		final PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		if (psiFile == null) return;

		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		if ( editor==null ) return;
		final Document doc = editor.getDocument();
		SelectionModel selectionModel = editor.getSelectionModel();

		String grammarText = psiFile.getText();
		ParsingResult results = ParsingUtils.parseANTLRGrammar(grammarText);
		final Parser parser = results.parser;
		final ParserRuleContext tree = (ParserRuleContext)results.tree;
		TokenStream tokens = parser.getTokenStream();

		int selStart = selectionModel.getSelectionStart();
		int selStop = selectionModel.getSelectionEnd()-1; // I'm inclusive and they are exclusive for end offset

		// find appropriate tokens for bounds, don't include WS
		Token start = RefactorUtils.getTokenForCharIndex(tokens, selStart);
		Token stop = RefactorUtils.getTokenForCharIndex(tokens, selStop);
		if ( start==null || stop==null ) {
			return;
		}
		if ( start.getType()==ANTLRv4Lexer.WS ) {
			start = tokens.get(start.getTokenIndex()+1);
		}
		if ( stop.getType()==ANTLRv4Lexer.WS ) {
			stop = tokens.get(stop.getTokenIndex()-1);
		}

		selectionModel.setSelection(start.getStartIndex(), stop.getStopIndex() + 1);
		final Project project = e.getProject();
		final ChooseExtractedRuleName nameChooser = new ChooseExtractedRuleName(project);
		nameChooser.show();
		if ( nameChooser.ruleName==null ) return;

		// make new rule string
		final String ruleText = selectionModel.getSelectedText();

		final int insertionPoint = RefactorUtils.getCharIndexOfNextRuleStart(tree, start.getTokenIndex());
		final String newRule = "\n"+nameChooser.ruleName + " : " + ruleText + " ;" + "\n";

		final Token start_ = start;
		final Token stop_ = stop;
		WriteCommandAction setTextAction = new WriteCommandAction(project) {
			@Override
			protected void run(final Result result) throws Throwable {
				// do all as one operation.
				if ( insertionPoint>=doc.getTextLength() ) {
					doc.insertString(doc.getTextLength(), newRule);
				}
				else {
					doc.insertString(insertionPoint, newRule);
				}
				doc.replaceString(start_.getStartIndex(), stop_.getStopIndex()+1, nameChooser.ruleName);
			}
		};
		setTextAction.execute();

		// TODO: only allow selection of fully-formed syntactic entity.
		// E.g., "A (',' A" is invalid grammatically as a rule.
	}

}
