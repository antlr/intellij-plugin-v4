package org.antlr.intellij.plugin.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ConfigANTLRPerGrammar extends DialogWrapper {
	private JPanel dialogContents;
	private JLabel labeltest;

	public ConfigANTLRPerGrammar(Project project) {
		super(project, false);
		init();

		setTitle("ANTLR Tool Config");
		getContentPane().setMinimumSize(new Dimension(400, 300));
	}


	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return dialogContents;
	}
}
