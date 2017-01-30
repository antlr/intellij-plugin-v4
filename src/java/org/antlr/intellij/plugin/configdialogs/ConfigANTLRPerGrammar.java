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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static com.google.common.base.MoreObjects.firstNonNull;

public class ConfigANTLRPerGrammar extends DialogWrapper {
	public static final String PROP_LANGUAGE = "language";
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
		String outputDirName = firstNonNull(
				getSettings(project, qualFileName).outputDir,
			    RunANTLROnGrammarFile.OUTPUT_DIR_NAME
		);
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

	@Deprecated
	private static String getPropNameForFile(String qualFileName, String prop) {
		return qualFileName+"::/"+prop;
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

	public static PerGrammarGenerationSettings getSettings(Project project, String qualFileName) {
		PerGrammarGenerationSettings settings = ANTLRGenerationSettingsComponent.getInstance(project)
				.getSettings().findSettingsForFile(qualFileName);

		migrateOldSettingsIfNeeded(project, qualFileName, settings);
		return settings;
	}

	@SuppressWarnings("deprecation")
	private static void migrateOldSettingsIfNeeded(Project project, String qualFileName,
												   PerGrammarGenerationSettings newSettings) {

		PropertiesComponent props = PropertiesComponent.getInstance(project);

		String v;

		if ((v = props.getValue(getPropNameForFile(qualFileName, "output-dir"))) != null) {
			newSettings.outputDir = v;
		}
		props.unsetValue(getPropNameForFile(qualFileName, "output-dir"));
		if ((v = props.getValue(getPropNameForFile(qualFileName, "lib-dir"))) != null) {
			newSettings.libDir = v;
		}
		props.unsetValue(getPropNameForFile(qualFileName, "lib-dir"));
		if ((v = props.getValue(getPropNameForFile(qualFileName, "encoding"))) != null) {
			newSettings.encoding = v;
		}
		props.unsetValue(getPropNameForFile(qualFileName, "encoding"));
		if ((v = props.getValue(getPropNameForFile(qualFileName, "package"))) != null) {
			newSettings.pkg = v;
		}
		props.unsetValue(getPropNameForFile(qualFileName, "package"));
		if ((v = props.getValue(getPropNameForFile(qualFileName, "language"))) != null) {
			newSettings.language = v;
		}
		props.unsetValue(getPropNameForFile(qualFileName, "language"));

		if (props.isValueSet(getPropNameForFile(qualFileName, "auto-gen"))) {
			newSettings.autoGen = props.getBoolean(getPropNameForFile(qualFileName, "auto-gen"));
			props.unsetValue(getPropNameForFile(qualFileName, "auto-gen"));
		}
		if (props.isValueSet(getPropNameForFile(qualFileName, "gen-listener"))) {
			newSettings.generateListener = props.getBoolean(getPropNameForFile(qualFileName, "gen-listener"));
			props.unsetValue(getPropNameForFile(qualFileName, "gen-listener"));
		}
		if (props.isValueSet(getPropNameForFile(qualFileName, "gen-visitor"))) {
			newSettings.generateVisitor = props.getBoolean(getPropNameForFile(qualFileName, "gen-visitor"));
			props.unsetValue(getPropNameForFile(qualFileName, "gen-visitor"));
		}
	}

	private void loadValues(Project project, String qualFileName) {
		PerGrammarGenerationSettings settings = getSettings(project, qualFileName);

		autoGenerateParsersCheckBox.setSelected(settings.autoGen);
		outputDirField.setText(settings.outputDir);
		libDirField.setText(settings.libDir);
		fileEncodingField.setText(settings.encoding);
		packageField.setText(settings.pkg);
		languageField.setText(settings.language);
		generateParseTreeListenerCheckBox.setSelected(settings.generateListener);
		generateParseTreeVisitorCheckBox.setSelected(settings.generateVisitor);
	}

	@Nullable
	private String trim(@NotNull String val) {
		return val.trim().equals("") ? null : val;
	}

	public void saveValues(Project project, String qualFileName) {
		PerGrammarGenerationSettings settings = getSettings(project, qualFileName);

		settings.autoGen = autoGenerateParsersCheckBox.isSelected();
		settings.outputDir = trim(outputDirField.getText());
		settings.libDir = trim(libDirField.getText());
		settings.encoding = trim(fileEncodingField.getText());
		settings.pkg = trim(packageField.getText());
		settings.language = trim(languageField.getText());
		settings.generateListener = generateParseTreeListenerCheckBox.isSelected();
		settings.generateVisitor = generateParseTreeVisitorCheckBox.isSelected();
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
