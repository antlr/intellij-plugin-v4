package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;

import javax.swing.*;

public class PreviewWindowFactory implements ToolWindowFactory {
	public static final String ID = "ANTLR Preview";

	// Create the tool window content.
	public void createToolWindowContent(Project project, ToolWindow toolWindow) {
		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

		JPanel pane = ANTLRv4ProjectComponent.getInstance(project).getTreeViewPanel();
		Content content = contentFactory.createContent(pane, "", false);
		toolWindow.getContentManager().addContent(content);
	}
}
