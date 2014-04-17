package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseMotionAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
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
	public static final JLabel placeHolder = new JLabel("Open a grammar (.g4) file and select a start rule");
	public static final String missingRuleText = "<select from navigator or grammar>";

	Project project;

	JPanel editorPanel;
	JTextArea editorConsole;

	JLabel startRuleLabel;
	TreeViewer treeViewer;

	EditorMouseMotionAdapter editorMouseListener = new PreviewEditorMouseListener();

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
		editorConsole = new JTextArea();
		editorConsole.setRows(3);
		editorConsole.setEditable(false);
		editorConsole.setLineWrap(true);
		JBScrollPane spane = new JBScrollPane(editorConsole); // wrap in scroller
		editorPanel = new JPanel(new BorderLayout(0,0));
		startRuleLabel = new JLabel("Start rule: "+missingRuleText);
		editorPanel.add(startRuleLabel, BorderLayout.NORTH);
		editorPanel.add(placeHolder, BorderLayout.CENTER);
		editorPanel.add(spane, BorderLayout.SOUTH);

		return editorPanel;
	}

	public JPanel createParseTreePanel() {
		// wrap tree and slider in panel
		JPanel treePanel = new JPanel(new BorderLayout(0,0));
		treePanel.setBackground(Color.white);
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
		System.out.println("grammar saved "+vfile.getName());
		switchToGrammarInCurrentState();
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileChanged(VirtualFile oldFile, VirtualFile newFile) {
		switchToGrammarInCurrentState();
	}

	/** Load grammars and set editor component. Guaranteed to be called only
	 *  after the state object is created in the controller and we
	 *  have already switched to this grammar file. So we can set
	 *  the editor without fear.
	 */
	public void switchToGrammarInCurrentState() {
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState();

		if ( previewState.editor==null ) { // this grammar is new; no editor yet
			previewState.editor = createEditor(""); // nothing there, create
		}

		BorderLayout layout = (BorderLayout)editorPanel.getLayout();
		Component editorSpotComp = layout.getLayoutComponent(BorderLayout.CENTER);
		// Remove whatever is in editor spot of layout (placeholder or previous editor)
		System.out.println("removing previous " + editorSpotComp);
		editorPanel.remove(editorSpotComp);

		// Do not add until we set rulename otherwise it starts parsing
		if ( previewState.startRuleName!=null &&
			 previewState.g!=null &&
			 previewState.lg!=null )
		{
			editorPanel.add(previewState.editor.getComponent(), BorderLayout.CENTER);
			// trigger parse tree refresh by poking text buffer (overwrite itself)
			final Document doc = previewState.editor.getDocument();
			ApplicationManager.getApplication()
				.runWriteAction(new Runnable() {
					@Override
					public void run() {
						doc.setText(doc.getCharsSequence());
					}
				});
		}
		else {
			editorPanel.add(placeHolder, BorderLayout.CENTER); // nothing to show in editor
			setParseTree(Collections.<String>emptyList(), null); // blank tree
			setStartRuleName(missingRuleText);
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

	public void setStartRuleName(String startRuleName) {
		startRuleLabel.setText("Start rule: "+startRuleName);
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
			new DocumentListener() {
				@Override
				public void beforeDocumentChange(DocumentEvent event) {}
				@Override
				public void documentChanged(DocumentEvent event) {
					Document doc = event.getDocument();
					String newText = doc.getText();
//				System.out.println("CHANGED: " + newText);
					setInput(newText);
				}
			});
		Editor editor = factory.createEditor(doc);
		EditorSettings settings = editor.getSettings();
		settings.setWhitespacesShown(true); // hmm...doesn't work.  maybe show when showing token tooltip?

		editor.addEditorMouseMotionListener(editorMouseListener);

		return editor;
	}

	public void setInput(final String inputText) {
		try {
			PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState();
			Object[] results =
				ANTLRv4PluginController.getInstance(project)
					.parseText(inputText);
			if (results != null) {
				ParseTree root = (ParseTree) results[1];
				setParseTree(Arrays.asList(previewState.g.getRuleNames()), root);
			}
			else {
				setParseTree(Arrays.asList(previewState.g.getRuleNames()), null);
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
