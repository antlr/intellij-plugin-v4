package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;

public class TestRuleAction extends AnAction implements DumbAware {
	BulkFileListener.Adapter fileSaveListener;

	/** Only show if selection is a grammar */
	@Override
	public void update(AnActionEvent e) {
		PsiElement selectedPsiRuleNode = e.getData(LangDataKeys.PSI_ELEMENT);
		if ( selectedPsiRuleNode==null ) return; // we clicked somewhere outside text

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

		Presentation presentation = e.getPresentation();
		presentation.setEnabled(grammarFound && parserRuleFound);
		presentation.setVisible(grammarFound);
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
//		System.out.println("exec "+e);
		Project project = getEventProject(e);
		if ( project==null ) return; // whoa!
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
		docMgr.saveDocument(doc);

//		final ParseTreePanel viewerPanel =
//			ANTLRv4ProjectComponent.getInstance(project).getTreeViewPanel();
//		String inputText = viewerPanel.getInputText(); // reuse input if any is around already
//		viewerPanel.setInputAndGrammar(inputText, file.getPath(), ruleName);
//
//		if ( fileSaveListener==null ) {
//			fileSaveListener = new BulkFileListener.Adapter() {
//				@Override
//				public void after(List<? extends VFileEvent> events) {
////					System.out.println("file changed");
//					viewerPanel.refresh();
//				}
//			};
//			MessageBusConnection msgBus = project.getMessageBus().connect(project);
//			msgBus.subscribe(VirtualFileManager.VFS_CHANGES, fileSaveListener);
//		}
////

		ANTLRv4PluginController.getInstance(project).getPreviewWindow().show(null);
		ANTLRv4PluginController.getInstance(project).setStartRuleNameEvent(ruleName);
	}

//	public VirtualFile getGrammarFile(AnActionEvent e) {
//		Project project = getEventProject(e);
//		if ( project==null ) return null; // whoa!
//		VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
//		if ( files==null ) return null; // no files?
//		PsiManager manager = PsiManager.getInstance(project);
//		for (VirtualFile file : files) {
////			System.out.println(file);
//			if ( manager.findFile(file) instanceof ANTLRv4FileRoot) {
//				return file;
//			}
//		}
//		return null;
//	}

}
