package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.ui.components.JBList;
import org.antlr.intellij.plugin.ANTLRv4ParserDefinition;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.configdialogs.LiteralChooser;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.pattern.ParseTreeMatch;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.antlr.v4.runtime.tree.xpath.XPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class DefineLexerRulesForLiterals extends AnAction {
	public static final Logger LOG = Logger.getInstance("ANTLR DefineLexerRuleForLiteral");

	/** Only show if selection is a literal */
	@Override
	public void update(AnActionEvent e) {
//		Presentation presentation = e.getPresentation();
//		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
//		if ( grammarFile==null ) {
//			presentation.setEnabled(false);
//			return;
//		}
//		Project project = e.getProject();
//		PsiFile file = e.getData(LangDataKeys.PSI_FILE);
//		Editor editor = e.getData(PlatformDataKeys.EDITOR);
//		PsiElement selectedElement = getElementAtCaret(editor, file);
//		if ( selectedElement==null ) { // we clicked somewhere outside text
//			presentation.setEnabled(false);
//			return;
//		}
//
//		IElementType tokenType = selectedElement.getNode().getElementType();
//		if ( tokenType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Parser.STRING_LITERAL) ) {
//			presentation.setEnabled(true);
//			presentation.setVisible(true);
//		}
//		else {
//			presentation.setEnabled(false);
//		}
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		final JBList list = new JBList("apple", "boy", "dog");

		final Runnable callback = new Runnable() {
			@Override
			public void run() {
				String selectedValue = (String)list.getSelectedValue();
				if (selectedValue != null) {
//					textField.setText(selectedValue);
					System.out.println("selected: "+selectedValue);
				}
			}
		};

		Project project = e.getProject();

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PsiFile file = e.getData(LangDataKeys.PSI_FILE);
		String inputText = file.getText();
		Pair<Parser, ParseTree> pair = ANTLRv4ParserDefinition.parse(inputText);

		ParseTree tree = pair.b;
		Parser parser = pair.a;
		Collection<ParseTree> literalNodes = XPath.findAll(tree, "//ruleBlock//STRING_LITERAL", parser);
		LinkedHashMap<String, String> lexerRules = new LinkedHashMap<String, String>();
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
			if ( lexerRules.containsKey(lit.getText()) ) { // we have rule for this literal already
				lexerRules.remove(lit.getText());
			}
		}

		final LiteralChooser chooser =
			new LiteralChooser(project, new ArrayList<String>(lexerRules.values()));
		chooser.show();

		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		final Editor editor = e.getData(PlatformDataKeys.EDITOR);
		final Document doc = editor.getDocument();
		final CommonTokenStream tokens = (CommonTokenStream)parser.getTokenStream();

		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			@Override
			public void run() {
				List<String> selectedElements = chooser.getSelectedElements();
				System.out.println(selectedElements);
				if ( selectedElements!=null ) {
					int cursorOffset = editor.getCaretModel().getOffset();
//					Token tokenUnderCursor = ANTLRv4ParserDefinition.getTokenUnderCursor(tokens, cursorOffset);
//					ANTLRv4ParserDefinition.nextRealToken()
					String text = doc.getText();
					String allRules = Utils.join(selectedElements.iterator(), "\n");
					text = text.substring(0,cursorOffset) + allRules + text.substring(cursorOffset, text.length());
					doc.setText(text);
				}
			}
		});

//		final PopupChooserBuilder builder = JBPopupFactory.getInstance().createListPopupBuilder(list);
//		final JBPopup popup =
//			builder.setMovable(false).setResizable(false)
//    			.setRequestFocus(true).setItemChoosenCallback(callback).createPopup();
//
//		popup.show(e.getInputEvent().getComponent());

//		Project project = e.getProject();
//		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
//		if ( grammarFile==null ) return;
//		LOG.info("actionPerformed "+grammarFile);
//
//		PsiFile file = e.getData(LangDataKeys.PSI_FILE);
//		Editor editor = e.getData(PlatformDataKeys.EDITOR);
//		PsiElement selectedElement = getElementAtCaret(editor, file);
//		System.out.println("literal = "+selectedElement);
//		if ( selectedElement==null ) return; // we clicked somewhere outside text
//
//		FileDocumentManager docMgr = FileDocumentManager.getInstance();
//		Document doc = docMgr.getDocument(grammarFile);
//		if ( doc!=null ) {
//			docMgr.saveDocument(doc);
//		}
	}

	public static PsiElement getElementAtCaret(final Editor editor, final PsiFile file) {
		final int offset = fixCaretOffset(editor);
		PsiElement element = file.findElementAt(offset);
		if (element == null && offset == file.getTextLength()) {
			element = file.findElementAt(offset - 1);
		}

		if (element instanceof PsiWhiteSpace) {
			element = file.findElementAt(element.getTextRange().getStartOffset() - 1);
		}
		return element;
	}

	private static int fixCaretOffset(final Editor editor) {
		final int caret = editor.getCaretModel().getOffset();
		if (editor.getSelectionModel().hasSelection() && !editor.getSelectionModel().hasBlockSelection()) {
			if (caret == editor.getSelectionModel().getSelectionEnd()) {
				return Math.max(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd() - 1);
			}
		}

		return caret;
	}

}
