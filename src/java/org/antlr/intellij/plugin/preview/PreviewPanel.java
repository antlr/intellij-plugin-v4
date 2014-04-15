package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.gui.TreeViewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
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
		String grammarFileName = vfile.getPath();
		switchToGrammar(grammarFileName);
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileChanged(VirtualFile oldFile, VirtualFile newFile) {
		String grammarFileName = newFile.getPath();
		switchToGrammar(grammarFileName);
	}

	/** Load grammars and set editor component. Guaranteed to be called only
	 *  after the state object is created in the controller and we
	 *  have already switched to this grammar file. So we can set
	 *  the editor without fear.
	 */
	public void switchToGrammar(String grammarFileName) {
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

	public void setParseTree(List<String> ruleNames, ParseTree tree) {
		treeViewer.setRuleNames(ruleNames);
		treeViewer.setTree(tree);
	}

	public void setStartRuleName(String startRuleName) {
		startRuleLabel.setText("Start rule: "+startRuleName);
	}

	public Editor createEditor(String inputText) {
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState();
		LightVirtualFile vf =
			new LightVirtualFile(previewState.g.name + ".input",
								 PreviewFileType.INSTANCE,
								 inputText);
		return createEditor(vf);
	}

	public Editor createEditor(VirtualFile vfile) {
		final EditorFactory factory = EditorFactory.getInstance();
		String inputText = null;
		try {
			inputText = new String(vfile.contentsToByteArray());
		}
		catch (IOException ioe) {
			System.err.println("Can't get contents of vfile?");
			return null;
		}
		final Document doc = factory.createDocument(inputText);
		// create editor in read action

		Computable<Editor> c = new Computable<Editor>() {
			@Override
			public Editor compute() {
				return factory.createEditor(doc, project, PreviewFileType.INSTANCE, false);
			}
		};
		Editor editor = ApplicationManager.getApplication().runReadAction(c);

		doc.addDocumentListener(
			new DocumentAdapter() {
				@Override
				public void documentChanged(DocumentEvent e) {
					editorConsole.setText("");
				}
			}
		);

		FileDocumentManagerImpl.registerDocument(doc, vfile);

		return editor;
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
