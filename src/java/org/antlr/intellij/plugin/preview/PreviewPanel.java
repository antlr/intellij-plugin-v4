package org.antlr.intellij.plugin.preview;

import javax.swing.*;
import java.awt.*;

public class PreviewPanel extends JPanel {
	public PreviewPanel() {
		buildGUI();
	}

	public void buildGUI() {
		this.setLayout(new BorderLayout());
		this.add(new JLabel("testing"), BorderLayout.CENTER);
	}
}
