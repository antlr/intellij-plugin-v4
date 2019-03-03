package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

class ChooseExtractedRuleName extends DialogWrapper {
	private JBTextField nameField;
	public String ruleName;

	ChooseExtractedRuleName(@Nullable Project project) {
		super(project, true);
		init();
	}

	@Override
	protected void doOKAction() {
		super.doOKAction();
		ruleName = nameField.getText();
	}

	@Override
	protected JComponent createCenterPanel() {
		nameField = new JBTextField("newRule");
		double h = nameField.getSize().getHeight();
		nameField.setPreferredSize(new Dimension(250,(int)h));
		setTitle("Name the Extracted Rule");
		nameField.selectAll();
		return nameField;
	}

	@Nullable
	@Override
	public JComponent getPreferredFocusedComponent() {
		return nameField;
	}
}
