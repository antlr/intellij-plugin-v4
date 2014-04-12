package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;

public class PreviewWindowFactory implements ToolWindowFactory {
	public static final String ID = "ANTLR Preview";

	// Create the tool window content.
	public void createToolWindowContent(Project project, ToolWindow toolWindow) {
		PreviewPanel previewPanel = new PreviewPanel(project);
		ANTLRv4ProjectComponent.getInstance(project).setPreviewPanel(previewPanel);

		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
		Content content = contentFactory.createContent(previewPanel, "", false);
		toolWindow.getContentManager().addContent(content);
	}
}
