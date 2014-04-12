package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;

/** The top level contents of the preview tool window created by
 *  intellij automatically.
 */
public class PreviewPanel extends JPanel {
	public PreviewPanel() {
		buildGUI();
	}

	public void buildGUI() {
		this.setLayout(new BorderLayout());
		this.add(new JLabel("testing"), BorderLayout.CENTER);
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileSaved(VirtualFile vfile) {
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileChanged(VirtualFile oldFile, VirtualFile newFile) {
		System.out.println("grammar changed to "+newFile.getName());
	}
}
