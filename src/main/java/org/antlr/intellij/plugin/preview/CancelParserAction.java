package org.antlr.intellij.plugin.preview;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.jetbrains.annotations.NotNull;

/**
 * A button that allows the user to kill the interpreter if it's taking too long to
 * process the input in the Preview window.
 */
public class CancelParserAction extends AnAction {

	private boolean enabled = false;

	public CancelParserAction() {
		super("Cancel Parsing", "Cancel the current parsing", AllIcons.Actions.Suspend);
	}

	@Override
	public void update(@NotNull AnActionEvent e) {
		super.update(e);

		e.getPresentation().setEnabled(enabled);
	}

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.EDT;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		final ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(e.getProject());

		if (controller != null) {
			controller.abortCurrentParsing();
		}
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
