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
    private JPanel dialogContents;
	private JCheckBox generateParseTreeListenerCheckBox;
	private JCheckBox generateParseTreeVisitorCheckBox;
	private JTextField packageField;
	private TextFieldWithBrowseButton outputDirField;
	private TextFieldWithBrowseButton libDirField;
	private JTextField fileEncodingField;
	protected JCheckBox autoGenerateParsersCheckBox;
	protected JTextField languageField;

	private ConfigANTLRPerGrammar(final Project project) {
		super(project, false);
	}

	public static ConfigANTLRPerGrammar getDialogForm(final Project project, String qualFileName) {
		ConfigANTLRPerGrammar grammarFrom = new ConfigANTLRPerGrammar(project);
		grammarFrom.init();
		grammarFrom.initAntlrFields(project, qualFileName);
		return grammarFrom;
	}

	public static ConfigANTLRPerGrammar getProjectSettingsForm(final Project project, String qualFileName) {
		ConfigANTLRPerGrammar grammarFrom = new ConfigANTLRPerGrammar(project);
		grammarFrom.initAntlrFields(project, qualFileName);
		grammarFrom.generateParseTreeListenerCheckBox.setVisible(false);
		grammarFrom.generateParseTreeVisitorCheckBox.setVisible(false);
		grammarFrom.autoGenerateParsersCheckBox.setVisible(false);
		return grammarFrom;
	}

	private void initAntlrFields(Project project, String qualFileName) {
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
		String outputDirName = ANTLRv4GrammarProperties.getProp(project, qualFileName,
				ANTLRv4GrammarProperties.PROP_OUTPUT_DIR,
				RunANTLROnGrammarFile.OUTPUT_DIR_NAME);
		File f = new File(outputDirName);
		if (!f.isAbsolute()) { // if not absolute file spec, it's relative to project root
			outputDirName = contentRoot.getPath() + File.separator + outputDirName;
		}
		// add package if any
		if ( !package_.equals(RunANTLROnGrammarFile.MISSING) ) {
			outputDirName += File.separator + package_.replace('.', File.separatorChar);
		}
		return outputDirName;
	}

	public static String getParentDir(VirtualFile vfile) {
		return vfile.getParent().getPath();
	}

	public static VirtualFile getContentRoot(Project project, VirtualFile vfile) {
		VirtualFile root =
				ProjectRootManager.getInstance(project)
						.getFileIndex().getContentRootForFile(vfile);
		if (root != null) return root;
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
		ANTLRv4GrammarProperties.setOutputDir(props, getOutputDirText(), qualFileName);
		ANTLRv4GrammarProperties.setLibDir(props, getLibDirText(), qualFileName);
		ANTLRv4GrammarProperties.setFileEncoding(props, getFileEncodingText(), qualFileName);
		ANTLRv4GrammarProperties.setPackageName(props, getPackageFieldText(), qualFileName);
		ANTLRv4GrammarProperties.setLanguage(props, getLanguageText(), qualFileName);
		ANTLRv4GrammarProperties.setGenerateParseTreeListener(props, qualFileName, generateParseTreeListenerCheckBox.isSelected());
		ANTLRv4GrammarProperties.setGenerateParseTreeVisitor(props, qualFileName, generateParseTreeVisitorCheckBox.isSelected());
	}

	boolean isModified(PropertiesComponent props, String qualFileName) {
		String defaultOutputDir = ANTLRv4GrammarProperties.getOutputDir(qualFileName, props);
		if (!defaultOutputDir.equals(getOutputDirText())) {
			return true;
		}

		String defaultLibDir = ANTLRv4GrammarProperties.getLibDir(qualFileName, props);
		if (!defaultLibDir.equals(getLibDirText())) {
			return true;
		}

		String defaultEncoding = ANTLRv4GrammarProperties.getEncoding(qualFileName, props);
		if (!defaultEncoding.equals(getFileEncodingText())) {
			return true;
		}

		String defaultPackage = ANTLRv4GrammarProperties.getPackage(qualFileName, props);
		if (!defaultPackage.equals(getPackageFieldText())) {
			return true;
		}

		String defaultLanguage = ANTLRv4GrammarProperties.getLanguage(qualFileName, props);
		if (!defaultLanguage.equals(getLanguageText())) {
			return true;
		}

		return false;
	}

	String getLanguageText() {
		return languageField.getText();
	}

	String getPackageFieldText() {
		return packageField.getText();
	}

	String getFileEncodingText() {
		return fileEncodingField.getText();
	}

	String getLibDirText() {
		return libDirField.getText();
	}

	String getOutputDirText() {
		return outputDirField.getText();
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return dialogContents;
	}

	@Override
	public String toString() {
		return "ConfigANTLRPerGrammar{" +
				"textField3=" +
				", generateParseTreeListenerCheckBox=" + generateParseTreeListenerCheckBox +
				", generateParseTreeVisitorCheckBox=" + generateParseTreeVisitorCheckBox +
				", packageField=" + packageField +
				", outputDirField=" + outputDirField +
				", libDirField=" + libDirField +
				'}';
	}
}
