package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.dialogs.ConfigANTLRPerGrammar;

public class ConfigureANTLRAction extends AnAction implements DumbAware {
	@Override
	public void update(AnActionEvent e) {
		GenerateAction.selectedFileIsGrammar(e);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		System.out.println("config action");
		Project project = getEventProject(e);
		if ( project==null ) return; // whoa!
		VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
		if ( files==null || files[0]==null ) return; // no files?

		ConfigANTLRPerGrammar configDialog = new ConfigANTLRPerGrammar(project);
		configDialog.getPeer().setTitle("Configure ANTLR for "+files[0].getName());

		configDialog.show();

/*
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
*/
	}
}
