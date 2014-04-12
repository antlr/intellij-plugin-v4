package org.antlr.intellij.plugin.tooloutput;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;

import javax.swing.*;

public class ToolOutputWindowFactory  implements ToolWindowFactory {
	public static final String ID = "ANTLR Tool Output";

	// Create the tool window content.
	public void createToolWindowContent(Project project, ToolWindow toolWindow) {
		TextConsoleBuilderFactory factory = TextConsoleBuilderFactory.getInstance();
		TextConsoleBuilder consoleBuilder = factory.createBuilder(project);
		ConsoleView console = consoleBuilder.getConsole();
		ANTLRv4ProjectComponent.getInstance(project).setConsole(console);

		JComponent consoleComponent = console.getComponent();
		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
		Content content = contentFactory.createContent(consoleComponent, "", false);
		toolWindow.getContentManager().addContent(content);
	}
}
