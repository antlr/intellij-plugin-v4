package org.antlr.intellij.plugin.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ConfigANTLRPerGrammar extends DialogWrapper {
	private JPanel dialogContents;
	private JTextField textField1;
	private JTextField textField2;
	private JTextField textField3;
	private JCheckBox checkBox1;
	private JCheckBox checkBox2;
	private JTable argsTable;

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

	private void createUIComponents() {
		// TODO: place custom component creation code here
		String[] columnNames = {"First Name", "Last Name"};
		Object[][] data = {{"Kathy", "Smith"},{"John", "Doe"}};
		argsTable = new JTable(data, columnNames);
		if ( argsTable!=null ) {
			argsTable.add(new JButton("flflf"));
		}
	}
}
