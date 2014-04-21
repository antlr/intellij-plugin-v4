package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.antlr.intellij.plugin.ANTLRv4PluginController;

/** Generate parser from ANTLR grammar;
 *  learned how to do from Grammar-Kit by Gregory Shrago.
 */
public class GenerateAction extends AnAction implements DumbAware {
	public static final Logger LOG = Logger.getInstance("ANTLR GenerateAction");

	@Override
	public void update(AnActionEvent e) {
		selectedFileIsGrammar(e);
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
		if ( e.getProject()==null ) {
			LOG.error("actionPerformed no project for "+e);
			return; // whoa!
		}
		VirtualFile currentGrammarFile = ANTLRv4PluginController.getCurrentGrammarFile(e.getProject());
		LOG.info("actionPerformed "+currentGrammarFile);
		VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
		if ( files==null ) return; // no files?
		String title = "ANTLR Code Generation";
		boolean canBeCancelled = true;

		// commit changes to PSI and file system
		PsiDocumentManager.getInstance(e.getProject()).commitAllDocuments();
		FileDocumentManager.getInstance().saveAllDocuments();

		Task.Backgroundable gen =
			new RunANTLROnGrammarFile(files,
									  e.getProject(),
									  title,
									  canBeCancelled,
									  new BackgroundFromStartOption());
		ProgressManager.getInstance().run(gen);
	}

	public static void selectedFileIsGrammar(AnActionEvent e) {
		VirtualFile currentGrammarFile = ANTLRv4PluginController.getCurrentGrammarFile(e.getProject());
		boolean grammarFound = currentGrammarFile!=null;
		e.getPresentation().setEnabled(grammarFound); // enable action if we're looking at grammar file
		e.getPresentation().setVisible(grammarFound);
	}
}
