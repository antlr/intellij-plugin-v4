package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.refactoring.IntroduceTargetChooser;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;

/**
 * Extracts an expression to an new parser rule.
 */
public class ExtractRuleAction extends AnAction {

	/**
	 * Enables the action if the caret is in a lexer or parser rule.
	 */
	@Override
	public void update(@NotNull AnActionEvent e) {
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
			PsiElement el = MyActionUtils.getSelectedPsiElement(e);
			if ( el==null || findExtractableRules(el).isEmpty() ) {
				presentation.setEnabled(false);
				return;
			}
		}

		// TODO: disable if selection spans rules
		presentation.setEnabled(true);
	}

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		PsiElement el = MyActionUtils.getSelectedPsiElement(e);
		if ( el==null ) return;

		final PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		if ( psiFile==null ) return;

		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		if ( editor==null ) return;
		SelectionModel selectionModel = editor.getSelectionModel();

		if ( !selectionModel.hasSelection() ) {
			List<PsiElement> expressions = findExtractableRules(el);

			IntroduceTargetChooser.showChooser(editor, expressions, new Pass<PsiElement>() {
				@Override
				public void pass(PsiElement element) {
					selectionModel.setSelection(element.getTextOffset(), element.getTextRange().getEndOffset());
					extractSelection(psiFile, editor, selectionModel);
				}
			}, PsiElement::getText);
		}
		else {
			extractSelection(psiFile, editor, selectionModel);
		}
	}

	@NotNull
	private List<PsiElement> findExtractableRules(PsiElement context) {
		List<PsiElement> expressions = new ArrayList<>();

		Set<IElementType> candidateTypes = Stream.of(ANTLRv4Parser.RULE_element, ANTLRv4Parser.RULE_alternative)
				.map(ANTLRv4TokenTypes::getRuleElementType)
				.collect(Collectors.toSet());

		@Nullable PsiElement parent = context;
		while ( parent!=null ) {
			if ( parent.getNode()!=null && candidateTypes.contains(parent.getNode().getElementType()) ) {
				expressions.add(parent);
			}

			parent = parent.getParent();
		}
		return expressions;
	}

	private void extractSelection(@NotNull PsiFile psiFile, Editor editor, SelectionModel selectionModel) {
		Document doc = editor.getDocument();
		String grammarText = psiFile.getText();
		ParsingResult results = ParsingUtils.parseANTLRGrammar(grammarText);
		final Parser parser = results.parser;
		final ParserRuleContext tree = (ParserRuleContext) results.tree;
		TokenStream tokens = parser.getTokenStream();

		int selStart = selectionModel.getSelectionStart();
		int selStop = selectionModel.getSelectionEnd() - 1; // I'm inclusive and they are exclusive for end offset

		// find appropriate tokens for bounds, don't include WS
		Token start = RefactorUtils.getTokenForCharIndex(tokens, selStart);
		Token stop = RefactorUtils.getTokenForCharIndex(tokens, selStop);
		if ( start==null || stop==null ) {
			return;
		}
		if ( start.getType()==ANTLRv4Lexer.WS ) {
			start = tokens.get(start.getTokenIndex() + 1);
		}
		if ( stop.getType()==ANTLRv4Lexer.WS ) {
			stop = tokens.get(stop.getTokenIndex() - 1);
		}

		selectionModel.setSelection(start.getStartIndex(), stop.getStopIndex() + 1);
		final Project project = psiFile.getProject();
		final ChooseExtractedRuleName nameChooser = new ChooseExtractedRuleName(project);
		nameChooser.show();
		if ( nameChooser.ruleName==null ) return;

		// make new rule string
		final String ruleText = selectionModel.getSelectedText();

		final int insertionPoint = RefactorUtils.getCharIndexOfNextRuleStart(tree, start.getTokenIndex());
		final String newRule = "\n" + nameChooser.ruleName + " : " + ruleText + " ;" + "\n";

		runWriteCommandAction(project, () -> {
			// do all as one operation.
			if ( insertionPoint >= doc.getTextLength() ) {
				doc.insertString(doc.getTextLength(), newRule);
			}
			else {
				doc.insertString(insertionPoint, newRule);
			}
			doc.replaceString(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), nameChooser.ruleName);
		});

		// TODO: only allow selection of fully-formed syntactic entity.
		// E.g., "A (',' A" is invalid grammatically as a rule.
	}

}
