package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;
import org.antlr.intellij.plugin.preview.ParseTreePanel;
import org.antlr.intellij.plugin.preview.ParseTreeWindowFactory;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;

public class TestRuleAction extends AnAction implements DumbAware {
	/** Only show if selection is a grammar */
	@Override
	public void update(AnActionEvent e) {
		VirtualFile file = getGrammarFile(e);
		boolean grammarFound = file!=null;

		PsiElement selectedPsiRuleNode = e.getData(LangDataKeys.PSI_ELEMENT);
		String ruleName = selectedPsiRuleNode.getText();
		boolean parserRuleFound = Character.isLowerCase(ruleName.charAt(0));

		// enable action if we're looking at grammar file
		e.getPresentation().setEnabled(grammarFound&&parserRuleFound);
		e.getPresentation().setVisible(grammarFound); // grey out of lexer rule
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
//		System.out.println("exec "+e);
		Project project = getEventProject(e);
		if ( project==null ) return; // whoa!
		PsiElement selectedPsiRuleNode = e.getData(LangDataKeys.PSI_ELEMENT);
		String ruleName = selectedPsiRuleNode.getText();
		if ( selectedPsiRuleNode instanceof ParserRuleSpecNode ) {
			ParserRuleRefNode r = PsiTreeUtil.findChildOfType(selectedPsiRuleNode,
															  ParserRuleRefNode.class);
			ruleName = r.getText();
		}

		VirtualFile file = getGrammarFile(e);
		if ( file==null ) return; // no files?

		FileDocumentManager docMgr = FileDocumentManager.getInstance();
		Document doc = docMgr.getDocument(file);
		docMgr.saveDocument(doc);

		ParseTreePanel viewerPanel = ANTLRv4ProjectComponent.getInstance(project).getViewerPanel();
		String inputText = viewerPanel.getInputText(); // reuse input if any is around already
		viewerPanel.setInputAndGrammar(inputText, file.getPath(), ruleName);

		// make sure tool window is showing
		ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
		ToolWindow toolWindow = toolWindowManager.getToolWindow(ParseTreeWindowFactory.ID);
		toolWindow.show(null);
	}

	public VirtualFile getGrammarFile(AnActionEvent e) {
		Project project = getEventProject(e);
		if ( project==null ) return null; // whoa!
		VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
		if ( files==null ) return null; // no files?
		PsiManager manager = PsiManager.getInstance(project);
		for (VirtualFile file : files) {
//			System.out.println(file);
			if ( manager.findFile(file) instanceof ANTLRv4FileRoot) {
				return file;
			}
		}
		return null;
	}

}
