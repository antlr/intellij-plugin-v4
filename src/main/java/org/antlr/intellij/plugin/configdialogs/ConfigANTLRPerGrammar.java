package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.util.Objects;

import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getGrammarProperties;
import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getOrCreateGrammarProperties;

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

	public void loadValues(Project project, String qualFileName) {
		ANTLRv4GrammarProperties grammarProperties = getGrammarProperties(project, qualFileName);

		autoGenerateParsersCheckBox.setSelected(grammarProperties.shouldAutoGenerateParser());
		outputDirField.setText(grammarProperties.getOutputDir());
		libDirField.setText(grammarProperties.getLibDir());
		fileEncodingField.setText(grammarProperties.getEncoding());
		packageField.setText(grammarProperties.getPackage());
		languageField.setText(grammarProperties.getLanguage());
		generateParseTreeListenerCheckBox.setSelected(grammarProperties.shouldGenerateParseTreeListener());
		generateParseTreeVisitorCheckBox.setSelected(grammarProperties.shouldGenerateParseTreeVisitor());
	}

    public void saveValues(Project project, String qualFileName) {
		ANTLRv4GrammarProperties grammarProperties = getOrCreateGrammarProperties(project, qualFileName);

		grammarProperties.autoGen = autoGenerateParsersCheckBox.isSelected();
		grammarProperties.outputDir = getOutputDirText();
		grammarProperties.libDir = getLibDirText();
		grammarProperties.encoding = getFileEncodingText();
		grammarProperties.pkg = getPackageFieldText();
		grammarProperties.language = getLanguageText();
		grammarProperties.generateListener = generateParseTreeListenerCheckBox.isSelected();
		grammarProperties.generateVisitor = generateParseTreeVisitorCheckBox.isSelected();
	}

	boolean isModified(ANTLRv4GrammarProperties originalProperties) {
		return !Objects.equals(originalProperties.getOutputDir(), getOutputDirText())
				|| !Objects.equals(originalProperties.getLibDir(), getLibDirText())
				|| !Objects.equals(originalProperties.getEncoding(), getFileEncodingText())
				|| !Objects.equals(originalProperties.getPackage(), getPackageFieldText())
				|| !Objects.equals(originalProperties.getLanguage(), getLanguageText());
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
				" generateParseTreeListenerCheckBox=" + generateParseTreeListenerCheckBox +
				", generateParseTreeVisitorCheckBox=" + generateParseTreeVisitorCheckBox +
				", packageField=" + packageField +
				", outputDirField=" + outputDirField +
				", libDirField=" + libDirField +
				'}';
	}
}
