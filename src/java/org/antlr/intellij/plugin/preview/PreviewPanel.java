package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.gui.TreeViewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The top level contents of the preview tool window created by
 *  intellij automatically. Since we need grammars to interpret,
 *  this object creates and caches lexer/parser grammars for
 *  each grammar file it gets notified about.
 */
public class PreviewPanel extends JPanel {
	public static final Logger LOG = Logger.getInstance("ANTLR PreviewPanel");

	public Project project;

	public InputPanel inputPanel;

	public TreeViewer treeViewer;
	public ParseTree lastTree;

	public PreviewPanel(Project project) {
		this.project = project;
		createGUI();
	}

	public void createGUI() {
		this.setLayout(new BorderLayout());

		// Had to set min size / preferred size in InputPanel.form to get slider to allow left shift of divider
		Splitter splitPane = new Splitter(false,0.65f);
		inputPanel = getEditorPanel();
		splitPane.setFirstComponent(inputPanel.getComponent());
		splitPane.setSecondComponent(createParseTreePanel());

		this.add(splitPane, BorderLayout.CENTER);
	}

	public InputPanel getEditorPanel() {
		LOG.info("createEditorPanel"+" "+project.getName());
		return new InputPanel(this);
	}

	public JPanel createParseTreePanel() {
		LOG.info("createParseTreePanel"+" "+project.getName());
		// wrap tree and slider in panel
		JPanel treePanel = new JPanel(new BorderLayout(0,0));
		treePanel.setBackground(JBColor.white);
		// Wrap tree viewer component in scroll pane
		treeViewer = new TreeViewer(null, null);
		JScrollPane scrollPane = new JBScrollPane(treeViewer); // use Intellij's scroller
		treePanel.add(scrollPane, BorderLayout.CENTER);

		// Add scale slider to bottom, under tree view scroll panel
		int sliderValue = (int) ((treeViewer.getScale()-1.0) * 1000);
		final JSlider scaleSlider = new JSlider(JSlider.HORIZONTAL,
										  -999,1000,sliderValue);
		scaleSlider.addChangeListener(
			new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int v = scaleSlider.getValue();
					if ( lastTree!=null ) {
						treeViewer.setScale(v / 1000.0 + 1.0);
					}
				}
			}
									 );
		treePanel.add(scaleSlider, BorderLayout.SOUTH);
		return treePanel;
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileSaved(VirtualFile grammarFile) {
		switchToGrammar(grammarFile);
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileChanged(VirtualFile oldFile, VirtualFile newFile) {
		switchToGrammar(newFile);
	}

	/** Load grammars and set editor component. */
	public void switchToGrammar(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("switchToGrammar " + grammarFileName+" "+project.getName());
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState(grammarFileName);

		inputPanel.switchToGrammar(grammarFile);

		if ( previewState.startRuleName!=null ) {
			updateParseTreeFromDoc(grammarFile);
		}
		else {
			setParseTree(Collections.<String>emptyList(), null); // blank tree
		}
	}

	public void closeGrammar(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("closeGrammar "+grammarFileName+" "+project.getName());

		inputPanel.resetStartRuleLabel();
		inputPanel.clearErrorConsole();
		clearParseTree(); // wipe tree

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
		inputPanel.releaseEditor(previewState);
	}

	public void setParseTree(final List<String> ruleNames, final ParseTree tree) {
		ApplicationManager.getApplication().invokeLater(
			new Runnable() {
				@Override
				public void run() {
					lastTree = tree;
					treeViewer.setRuleNames(ruleNames);
					treeViewer.setTree(tree);
				}
			}
		);
	}

	public void clearParseTree() {
		setParseTree(Arrays.asList(new String[0]), null);
	}

	public void updateParseTreeFromDoc(VirtualFile grammarFile) {
		try {
			ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
			PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
			final String inputText = previewState.getEditor().getDocument().getText();
			ParsingResult results = controller.parseText(grammarFile, inputText);
			if (results != null) {
				setParseTree(Arrays.asList(previewState.g.getRuleNames()), results.tree);
			}
			else {
				clearParseTree();
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public InputPanel getInputPanel() {
		return inputPanel;
	}
}
