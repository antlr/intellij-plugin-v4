package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.configdialogs.ConfigANTLRPerGrammar;
import org.antlr.v4.Tool;
import org.jetbrains.annotations.NotNull;

public class ConfigureANTLRAction extends AnAction implements DumbAware {
	public static final Logger LOG = Logger.getInstance("ConfigureANTLRAction");

	@Override
	public void update(AnActionEvent e) {
		MyActionUtils.selectedFileIsGrammar(e);
	}

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		if ( e.getProject()==null ) {
			LOG.error("actionPerformed no project for "+e);
			return; // whoa!
		}
		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		if ( grammarFile==null ) return;
		LOG.info("actionPerformed "+grammarFile);

		ConfigANTLRPerGrammar configDialog = ConfigANTLRPerGrammar.getDialogForm(e.getProject(), grammarFile.getPath());
		configDialog.getPeer().setTitle("Configure ANTLR Tool "+ Tool.VERSION+" for "+ grammarFile.getName());

		configDialog.show();

		if ( configDialog.getExitCode()==DialogWrapper.OK_EXIT_CODE ) {
			configDialog.saveValues(e.getProject(), grammarFile.getPath());
		}
	}
}
