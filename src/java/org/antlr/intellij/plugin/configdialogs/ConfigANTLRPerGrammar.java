package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

public class ConfigANTLRPerGrammar extends DialogWrapper {
	public static final String PROP_AUTO_GEN = "auto-gen";
	public static final String PROP_OUTPUT_DIR = "output-dir";
	public static final String PROP_LIB_DIR = "lib-dir";
	public static final String PROP_ENCODING = "encoding";
	public static final String PROP_PACKAGE = "package";
	public static final String PROP_LANGUAGE = "language";
	public static final String PROP_GEN_LISTENER = "gen-listener";
	public static final String PROP_GEN_VISITOR = "gen-visitor";
	private JPanel dialogContents;
	private JCheckBox generateParseTreeListenerCheckBox;
	private JCheckBox generateParseTreeVisitorCheckBox;
	private JTextField packageField;
	private TextFieldWithBrowseButton outputDirField;
	private TextFieldWithBrowseButton libDirField;
	private JTextField fileEncodingField;
	protected JCheckBox autoGenerateParsersCheckBox;
	protected JTextField languageField;

	public ConfigANTLRPerGrammar(final Project project, String qualFileName) {
		super(project, false);
		init();

		FileChooserDescriptor dirChooser =
			FileChooserDescriptorFactory.createSingleFolderDescriptor();
		outputDirField.addBrowseFolderListener("Select output dir", null, project, dirChooser);
		outputDirField.setTextFieldPreferredWidth(50);

		dirChooser =
			FileChooserDescriptorFactory.createSingleFolderDescriptor();
		libDirField.addBrowseFolderListener("Select lib dir", null, project, dirChooser);
		libDirField.setTextFieldPreferredWidth(50);

		loadValues(project, qualFileName);
	}

	public static String getOutputDirName(Project project, String qualFileName, VirtualFile contentRoot, String package_) {
		String outputDirName = contentRoot.getPath()+File.separator+RunANTLROnGrammarFile.OUTPUT_DIR_NAME;
		outputDirName = getProp(project, qualFileName, PROP_OUTPUT_DIR, outputDirName);
		if ( package_!=RunANTLROnGrammarFile.MISSING ) {
			outputDirName += File.separator+package_.replace('.', File.separatorChar);
		}
		return outputDirName;
	}

	public static String getProp(Project project, String qualFileName, String name, String defaultValue) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		String v = props.getValue(getPropNameForFile(qualFileName, name));
		if ( v==null || v.trim().length()==0 ) return defaultValue;
		return v;
	}

	public static boolean getBooleanProp(Project project, String qualFileName, String name, boolean defaultValue) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		return props.getBoolean(getPropNameForFile(qualFileName, name), defaultValue);
	}

	public static String getParentDir(VirtualFile vfile) {
		return vfile.getParent().getPath();
	}

	public static VirtualFile getContentRoot(Project project, VirtualFile vfile) {
		VirtualFile root =
			ProjectRootManager.getInstance(project)
				.getFileIndex().getContentRootForFile(vfile);
		if ( root!=null ) return root;
		return vfile.getParent();
	}

	public void loadValues(Project project, String qualFileName) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		String s;
		boolean b;
		b = props.getBoolean(getPropNameForFile(qualFileName, PROP_AUTO_GEN), false);
		autoGenerateParsersCheckBox.setSelected(b);

		s = props.getValue(getPropNameForFile(qualFileName, PROP_OUTPUT_DIR), "");
		outputDirField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, PROP_LIB_DIR), "");
		libDirField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, PROP_ENCODING), "");
		fileEncodingField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, PROP_PACKAGE), "");
		packageField.setText(s);
		s = props.getValue(getPropNameForFile(qualFileName, PROP_LANGUAGE), "");
		languageField.setText(s);

		b = props.getBoolean(getPropNameForFile(qualFileName, PROP_GEN_LISTENER), true);
		generateParseTreeListenerCheckBox.setSelected(b);
		b = props.getBoolean(getPropNameForFile(qualFileName, PROP_GEN_VISITOR), false);
		generateParseTreeVisitorCheckBox.setSelected(b);
	}

	public void saveValues(Project project, String qualFileName) {
		String v;
		PropertiesComponent props = PropertiesComponent.getInstance(project);

		props.setValue(getPropNameForFile(qualFileName, PROP_AUTO_GEN),
		               String.valueOf(autoGenerateParsersCheckBox.isSelected()));

		v = outputDirField.getText();
		if ( v.trim().length()>0 ) {
			props.setValue(getPropNameForFile(qualFileName, PROP_OUTPUT_DIR), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, PROP_OUTPUT_DIR));
		}

		v = libDirField.getText();
		if ( v.trim().length()>0 ) {
			props.setValue(getPropNameForFile(qualFileName, PROP_LIB_DIR), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, PROP_LIB_DIR));
		}

		v = fileEncodingField.getText();
		if ( v.trim().length()>0 ) {
			props.setValue(getPropNameForFile(qualFileName, PROP_ENCODING), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, PROP_ENCODING));
		}

		v = packageField.getText();
		if ( v.trim().length()>0 ) {
			props.setValue(getPropNameForFile(qualFileName, PROP_PACKAGE), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, PROP_PACKAGE));
		}

		v = languageField.getText();
		if ( v.trim().length()>0 ) {
			props.setValue(getPropNameForFile(qualFileName, PROP_LANGUAGE), v);
		}
		else {
			props.unsetValue(getPropNameForFile(qualFileName, PROP_LANGUAGE));
		}

		props.setValue(getPropNameForFile(qualFileName, PROP_GEN_LISTENER),
		               String.valueOf(generateParseTreeListenerCheckBox.isSelected()));
		props.setValue(getPropNameForFile(qualFileName, PROP_GEN_VISITOR),
		               String.valueOf(generateParseTreeVisitorCheckBox.isSelected()));
	}

	public static String getPropNameForFile(String qualFileName, String prop) {
		return qualFileName+"::/"+prop;
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return dialogContents;
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}


	@Override
	public String toString() {
		return "ConfigANTLRPerGrammar{"+
			"textField3="+
			", generateParseTreeListenerCheckBox="+generateParseTreeListenerCheckBox+
			", generateParseTreeVisitorCheckBox="+generateParseTreeVisitorCheckBox+
			", packageField="+packageField+
			", outputDirField="+outputDirField+
			", libDirField="+libDirField+
			'}';
	}

	{
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		dialogContents = new javax.swing.JPanel();
		dialogContents.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(8, 2, new java.awt.Insets(0, 0, 0, 0), -1, -1));
		final javax.swing.JLabel label1 = new javax.swing.JLabel();
		dialogContents.add(label1, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		final javax.swing.JLabel label2 = new javax.swing.JLabel();
		dialogContents.add(label2, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		fileEncodingField = new javax.swing.JTextField();
		dialogContents.add(fileEncodingField, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new java.awt.Dimension(150, -1), null, 0, false));
		generateParseTreeVisitorCheckBox = new javax.swing.JCheckBox();
		dialogContents.add(generateParseTreeVisitorCheckBox, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK|com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		generateParseTreeListenerCheckBox = new javax.swing.JCheckBox();
		dialogContents.add(generateParseTreeListenerCheckBox, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK|com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final javax.swing.JLabel label3 = new javax.swing.JLabel();
		dialogContents.add(label3, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		packageField = new javax.swing.JTextField();
		dialogContents.add(packageField, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new java.awt.Dimension(150, -1), null, 0, false));
		final javax.swing.JLabel label4 = new javax.swing.JLabel();
		dialogContents.add(label4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		outputDirField = new com.intellij.openapi.ui.TextFieldWithBrowseButton();
		dialogContents.add(outputDirField, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK|com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK|com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		libDirField = new com.intellij.openapi.ui.TextFieldWithBrowseButton();
		dialogContents.add(libDirField, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK|com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK|com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		autoGenerateParsersCheckBox = new javax.swing.JCheckBox();
		dialogContents.add(autoGenerateParsersCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK|com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final javax.swing.JLabel label5 = new javax.swing.JLabel();
		dialogContents.add(label5, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		languageField = new javax.swing.JTextField();
		dialogContents.add(languageField, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new java.awt.Dimension(150, -1), null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public javax.swing.JComponent $$$getRootComponent$$$() {
		return dialogContents;
	}
}
