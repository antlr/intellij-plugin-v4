package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
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
		String outputDirName = getProp(project, qualFileName,
		                               PROP_OUTPUT_DIR,
		                               RunANTLROnGrammarFile.OUTPUT_DIR_NAME);
		File f = new File(outputDirName);
		if ( !f.isAbsolute() ) { // if not absolute file spec, it's relative to project root
			outputDirName = contentRoot.getPath()+File.separator+outputDirName;
		}
		// add package if any
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
		dialogContents = new JPanel();
		dialogContents.setLayout(new GridLayoutManager(8, 2, new Insets(0, 0, 0, 0), -1, -1));
		final JLabel label1 = new JLabel();
		label1.setText("Location of imported grammars");
		dialogContents.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		final JLabel label2 = new JLabel();
		label2.setText("Grammar file encoding; e.g., euc-jp");
		dialogContents.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		fileEncodingField = new JTextField();
		dialogContents.add(fileEncodingField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		generateParseTreeVisitorCheckBox = new JCheckBox();
		generateParseTreeVisitorCheckBox.setText("generate parse tree visitor");
		dialogContents.add(generateParseTreeVisitorCheckBox, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		generateParseTreeListenerCheckBox = new JCheckBox();
		generateParseTreeListenerCheckBox.setSelected(true);
		generateParseTreeListenerCheckBox.setText("generate parse tree listener (default)");
		dialogContents.add(generateParseTreeListenerCheckBox, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label3 = new JLabel();
		label3.setText("Package/namespace for the generated code");
		dialogContents.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		packageField = new JTextField();
		dialogContents.add(packageField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label4 = new JLabel();
		label4.setText("Output directory where all output is generated");
		dialogContents.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		outputDirField = new TextFieldWithBrowseButton();
		dialogContents.add(outputDirField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		libDirField = new TextFieldWithBrowseButton();
		dialogContents.add(libDirField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		autoGenerateParsersCheckBox = new JCheckBox();
		autoGenerateParsersCheckBox.setText("Auto-generate parsers upon save");
		dialogContents.add(autoGenerateParsersCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label5 = new JLabel();
		label5.setText("Language (e.g., Java, Python2, CSharp, ...)");
		dialogContents.add(label5, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		languageField = new JTextField();
		dialogContents.add(languageField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return dialogContents;
	}
}
