package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;

public class TestRuleAction extends AnAction implements DumbAware {
	public static final Logger LOG = Logger.getInstance("ANTLR TestRuleAction");

	/** Only show if selection is a grammar */
	@Override
	public void update(AnActionEvent e) {
		Presentation presentation = e.getPresentation();
		PsiElement selectedPsiRuleNode = e.getData(LangDataKeys.PSI_ELEMENT);
		if ( selectedPsiRuleNode==null ) { // we clicked somewhere outside text
			presentation.setEnabled(false);
			presentation.setVisible(false);
			return;
		}

		String ruleName = selectedPsiRuleNode.getText();
		boolean parserRuleFound;
		if ( ruleName==null ) {
			parserRuleFound = false;
		}
		else {
			parserRuleFound = Character.isLowerCase(ruleName.charAt(0));
		}

		// enable action if we're looking at grammar file and we got a good rule name
		VirtualFile file = ANTLRv4PluginController.getCurrentGrammarFile(e.getProject());
		boolean grammarFound = file!=null;

		presentation.setEnabled(grammarFound && parserRuleFound);
		presentation.setVisible(grammarFound);
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
		if ( e.getProject()==null ) {
			LOG.error("actionPerformed no project for "+e);
			return; // whoa!
		}
		VirtualFile currentGrammarFile = ANTLRv4PluginController.getCurrentGrammarFile(e.getProject());
		LOG.info("actionPerformed "+currentGrammarFile);
		PsiElement selectedPsiRuleNode = e.getData(LangDataKeys.PSI_ELEMENT);
		if ( selectedPsiRuleNode==null ) return; // we clicked somewhere outside text
		String ruleName = selectedPsiRuleNode.getText();
		if ( selectedPsiRuleNode instanceof ParserRuleSpecNode ) {
			ParserRuleRefNode r = PsiTreeUtil.findChildOfType(selectedPsiRuleNode,
															  ParserRuleRefNode.class);
			ruleName = r.getText();
		}

		VirtualFile file = ANTLRv4PluginController.getCurrentGrammarFile(e.getProject());
		if ( file==null ) return;

		FileDocumentManager docMgr = FileDocumentManager.getInstance();
		Document doc = docMgr.getDocument(file);
		if ( doc!=null ) {
			docMgr.saveDocument(doc);
		}

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(e.getProject());
		controller.getPreviewWindow().show(null);
		controller.setStartRuleNameEvent(ruleName);
	}

}
