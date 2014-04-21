package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.configdialogs.ConfigANTLRPerGrammar;

public class ConfigureANTLRAction extends AnAction implements DumbAware {
	public static final Logger LOG = Logger.getInstance("ANTLR ConfigureANTLRAction");

	@Override
	public void update(AnActionEvent e) {
		GenerateAction.selectedFileIsGrammar(e);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		if ( e.getProject()==null ) {
			LOG.error("actionPerformed no project for "+e);
			return; // whoa!
		}
		VirtualFile currentGrammarFile = ANTLRv4PluginController.getCurrentGrammarFile(e.getProject());
		LOG.info("actionPerformed "+currentGrammarFile);
		VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
		if ( files==null || files[0]==null ) return; // no files?

		String name = files[0].getPath();
		ConfigANTLRPerGrammar configDialog = new ConfigANTLRPerGrammar(e.getProject(), name);
		String fileName = files[0].getName();
		configDialog.getPeer().setTitle("Configure ANTLR for "+ fileName);

		configDialog.show();

		if ( configDialog.getExitCode()==DialogWrapper.OK_EXIT_CODE ) {
			configDialog.saveValues(e.getProject(), name);
		}
	}
}
