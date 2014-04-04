package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;

/** Generate parser from ANTLR grammar;
 *  learned how to do from Grammar-Kit by Gregory Shrago.
 */
public class GenerateAction extends AnAction implements DumbAware {
	@Override
	public void update(AnActionEvent e) {
		selectedFileIsGrammar(e);
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
		Project project = getEventProject(e);
		if ( project==null ) return; // whoa!
		VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
		if ( files==null ) return; // no files?
		String title = "ANTLR Code Generation";
		boolean canBeCancelled = true;

		// commit changes to PSI and file system
		PsiDocumentManager.getInstance(project).commitAllDocuments();
		FileDocumentManager.getInstance().saveAllDocuments();

		Task.Backgroundable gen =
			new RunANTLROnGrammarFile(files,
									  project,
									  title,
									  canBeCancelled,
									  new BackgroundFromStartOption());
		ProgressManager.getInstance().run(gen);
	}

	// TODO:Put this in a better location
	public static void selectedFileIsGrammar(AnActionEvent e) {
		Project project = getEventProject(e);
		if ( project==null ) return; // whoa!
		VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
		if ( files==null ) return; // no files?
		boolean grammarFound = false;
		PsiManager manager = PsiManager.getInstance(project);
		for (VirtualFile file : files) {
			if ( manager.findFile(file) instanceof ANTLRv4FileRoot) {
				grammarFound = true;
				break;
			}
		}
		e.getPresentation().setEnabled(grammarFound); // enable action if we're looking at grammar file
		e.getPresentation().setVisible(grammarFound);
	}

}
