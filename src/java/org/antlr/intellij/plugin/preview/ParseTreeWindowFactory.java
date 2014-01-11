package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;

import javax.swing.*;

public class ParseTreeWindowFactory implements ToolWindowFactory {
	public static final String ID = "ANTLR Parse Tree";

	protected ToolWindow myToolWindow;

	// Create the tool window content.
	public void createToolWindowContent(Project project, ToolWindow toolWindow) {
		myToolWindow = toolWindow;
		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

		JPanel pane = ANTLRv4ProjectComponent.getInstance(project).getViewerPanel();
		Content content = contentFactory.createContent(pane, "", false);
		toolWindow.getContentManager().addContent(content);
	}

	// 	Get filename:
	// 	http://stackoverflow.com/questions/17915688/intellij-plugin-get-code-from-current-open-file
	public static String getCurrentFileName(Project project) {
		FileEditorManager fileMgr = FileEditorManager.getInstance(project);
		Editor editor = fileMgr.getSelectedTextEditor();
		if ( editor!=null ) {
			Document doc = editor.getDocument();
			VirtualFile currentFile = FileDocumentManager.getInstance().getFile(doc);
			if ( currentFile==null ) return null;
			return currentFile.getPath();
		}
		return null;
	}
}
