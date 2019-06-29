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

	public static String resolveOutputDirName(Project project, String qualFileName, VirtualFile contentRoot, String package_) {
		String outputDirName = getProp(project, qualFileName,
		                               ANTLRv4GrammarProperties.PROP_OUTPUT_DIR,
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
		String v = props.getValue(ANTLRv4GrammarProperties.getPropNameForFile(qualFileName, name));
		if ( v==null || v.trim().length()==0 ) return defaultValue;
		return v;
	}

	public static boolean getBooleanProp(Project project, String qualFileName, String name, boolean defaultValue) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		return props.getBoolean(ANTLRv4GrammarProperties.getPropNameForFile(qualFileName, name), defaultValue);
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
		b = ANTLRv4GrammarProperties.shouldAutoGenerateParser(qualFileName, props);
		autoGenerateParsersCheckBox.setSelected(b);

		s = ANTLRv4GrammarProperties.getOutputDir(qualFileName, props);
		outputDirField.setText(s);
		s = ANTLRv4GrammarProperties.getLibDir(qualFileName, props);
		libDirField.setText(s);
		s = ANTLRv4GrammarProperties.getEncoding(qualFileName, props);
		fileEncodingField.setText(s);
		s = ANTLRv4GrammarProperties.getPackage(qualFileName, props);
		packageField.setText(s);
		s = ANTLRv4GrammarProperties.getLanguage(qualFileName, props);
		languageField.setText(s);

		b = ANTLRv4GrammarProperties.shouldGenerateParseTreeListener(qualFileName, props);
		generateParseTreeListenerCheckBox.setSelected(b);
		b = ANTLRv4GrammarProperties.shouldGenerateParseTreeVisitor(qualFileName, props);
		generateParseTreeVisitorCheckBox.setSelected(b);
	}

    public void saveValues(Project project, String qualFileName) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		ANTLRv4GrammarProperties.setAutoGen(props, qualFileName, autoGenerateParsersCheckBox.isSelected());
		ANTLRv4GrammarProperties.setOutputDir(props, outputDirField.getText(), qualFileName);
		ANTLRv4GrammarProperties.setLibDir(props, libDirField.getText(), qualFileName);
		ANTLRv4GrammarProperties.setFileEncoding(props, fileEncodingField.getText(), qualFileName);
		ANTLRv4GrammarProperties.setPackageName(props, packageField.getText(), qualFileName);
		ANTLRv4GrammarProperties.setLanguage(props, languageField.getText(), qualFileName);
		ANTLRv4GrammarProperties.setGenerateParseTreeListener(props, qualFileName, generateParseTreeListenerCheckBox.isSelected());
		ANTLRv4GrammarProperties.setGenerateParseTreeVisitor(props, qualFileName, generateParseTreeVisitorCheckBox.isSelected());
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
