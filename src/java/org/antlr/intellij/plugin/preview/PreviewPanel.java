package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseMotionAdapter;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.v4.runtime.Parser;
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

	public static final JLabel placeHolder = new JLabel("");
	public static final String missingStartRuleLabelText =
		"Start rule: <select from navigator or grammar>";
	public static final String startRuleLabelText =	"Start rule: ";

	Project project;

	JPanel editorPanel;
	JTextArea editorConsole;

	JLabel startRuleLabel;
	TreeViewer treeViewer;

	EditorMouseMotionAdapter editorMouseMoveListener = new PreviewEditorMouseListener();
	EditorMouseAdapter editorMouseListener = new EditorMouseAdapter() {
		@Override
		public void mouseExited(EditorMouseEvent e) {
			MarkupModel markupModel = e.getEditor().getMarkupModel();
			markupModel.removeAllHighlighters();
		}
	};

	public PreviewPanel(Project project) {
		this.project = project;
		createGUI();
	}

	public void createGUI() {
		this.setLayout(new BorderLayout());

		Splitter splitPane = new Splitter();
		splitPane.setFirstComponent(createEditorPanel());
		splitPane.setSecondComponent(createParseTreePanel());

		this.add(splitPane, BorderLayout.CENTER);
	}

	public JPanel createEditorPanel() {
		LOG.info("createEditorPanel");
		editorConsole = new JTextArea();
		editorConsole.setRows(3);
		editorConsole.setEditable(false);
		editorConsole.setLineWrap(true);
		JBScrollPane spane = new JBScrollPane(editorConsole); // wrap in scroller
		editorPanel = new JPanel(new BorderLayout(0,0));
		startRuleLabel = new JLabel(missingStartRuleLabelText);
		startRuleLabel.setForeground(JBColor.RED);
		editorPanel.add(startRuleLabel, BorderLayout.NORTH);
		editorPanel.add(placeHolder, BorderLayout.CENTER);
		editorPanel.add(spane, BorderLayout.SOUTH);

		return editorPanel;
	}

	public JPanel createParseTreePanel() {
		LOG.info("createParseTreePanel");
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
					treeViewer.setScale(v / 1000.0 + 1.0);
				}
			}
									 );
		treePanel.add(scaleSlider, BorderLayout.SOUTH);
		return treePanel;
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileSaved(VirtualFile vfile) {
		switchToGrammar(vfile);
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileChanged(VirtualFile oldFile, VirtualFile newFile) {
		switchToGrammar(newFile);
	}

	public void setStartRuleName(String startRuleName) {
		startRuleLabel.setText(startRuleLabelText + startRuleName);
		startRuleLabel.setForeground(JBColor.BLACK);
		// Might have text already, parse it.
		updateParseTreeFromDoc();
	}

	public void resetStartRuleLabel() {
		startRuleLabel.setText(missingStartRuleLabelText); // reset
		startRuleLabel.setForeground(JBColor.RED);
	}

	/** Load grammars and set editor component. */
	public void switchToGrammar(VirtualFile vfile) {
		LOG.info("switchToGrammar " + vfile.getPath());
		String grammarFileName = vfile.getPath();
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState(grammarFileName);

		if ( previewState.editor==null ) { // this grammar is new; no editor yet
			LOG.info("switchToGrammar: create new editor for "+previewState.grammarFileName);
			previewState.editor = createEditor(""); // nothing there, create
		}

		BorderLayout layout = (BorderLayout)editorPanel.getLayout();
		Component editorSpotComp = layout.getLayoutComponent(BorderLayout.CENTER);
		editorPanel.remove(editorSpotComp);
		editorPanel.add(previewState.editor.getComponent(), BorderLayout.CENTER);

		if ( previewState.startRuleName!=null ) {
			setStartRuleName(previewState.startRuleName);
			updateParseTreeFromDoc();
		}
		else {
			resetStartRuleLabel();
			setParseTree(Collections.<String>emptyList(), null); // blank tree
		}
	}

	public void setParseTree(final List<String> ruleNames, final ParseTree tree) {
		ApplicationManager.getApplication().invokeLater(
			new Runnable() {
				@Override
				public void run() {
					treeViewer.setRuleNames(ruleNames);
					treeViewer.setTree(tree);
				}
			}
		);
	}

	public Editor createEditor(String inputText) {
		final EditorFactory factory = EditorFactory.getInstance();
		Document doc = factory.createDocument(inputText);
		doc.addDocumentListener(
			new DocumentAdapter() {
				@Override
				public void documentChanged(DocumentEvent e) {
					editorConsole.setText("");
				}
			}
		);
		doc.addDocumentListener(
			new DocumentAdapter() {
				@Override
				public void documentChanged(DocumentEvent event) {
					updateParseTreeFromDoc();
				}
			});
		final Editor editor = factory.createEditor(doc, project);
		EditorSettings settings = editor.getSettings();
		settings.setWhitespacesShown(true); // hmm...doesn't work.  maybe show when showing token tooltip?

		editor.addEditorMouseMotionListener(editorMouseMoveListener);
		editor.addEditorMouseListener(editorMouseListener);

		return editor;
	}

	public void updateParseTreeFromDoc() {
		try {
			PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState();
			if ( previewState==null ) {
				LOG.error("updateParseTreeFromDoc no state for "+
							  ANTLRv4PluginController.getCurrentEditorFile(project).getPath());
				setParseTree(Arrays.asList(new String[0]), null);
				return;
			}
			final String inputText = previewState.editor.getDocument().getText();
			Object[] results =
				ANTLRv4PluginController.getInstance(project).parseText(inputText);
			if (results != null) {
				Parser parser = (Parser) results[0];
				previewState.parser = parser;
				ParseTree root = (ParseTree) results[1];
				setParseTree(Arrays.asList(previewState.g.getRuleNames()), root);
			}
			else {
				setParseTree(Arrays.asList(new String[0]), null);
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void clearParseErrors() {
		editorConsole.setText("");
//		ApplicationManager.getApplication().invokeLater(
//			new Runnable() {
//				@Override
//				public void run() {
//					editorConsole.setText("");
//				}
//			}
//		);
	}

	public void parseError(final String msg) {
		ApplicationManager.getApplication().invokeLater(
			new Runnable() {
				@Override
				public void run() {
					editorConsole.insert(msg, editorConsole.getText().length());
				}
			}
		);
	}
}
