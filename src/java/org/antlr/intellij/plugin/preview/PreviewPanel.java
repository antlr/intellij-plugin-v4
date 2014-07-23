package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.antlr.v4.runtime.tree.gui.TreeViewer;
import org.antlr.v4.tool.Rule;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
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

    public ProfilerPanel profilerPanel;

	public PreviewPanel(Project project) {
		this.project = project;
		createGUI();
	}

	public void createGUI() {
		this.setLayout(new BorderLayout());

		// Had to set min size / preferred size in InputPanel.form to get slider to allow left shift of divider
		Splitter splitPane = new Splitter();
		inputPanel = getEditorPanel();
		splitPane.setFirstComponent(inputPanel.getComponent());
		splitPane.setSecondComponent(createParseTreeAndProfileTabbedPanel());

		this.add(splitPane, BorderLayout.CENTER);
	}

	public InputPanel getEditorPanel() {
		LOG.info("createEditorPanel"+" "+project.getName());
		return new InputPanel(this);
	}

	public ProfilerPanel getProfilerPanel() {
		return profilerPanel;
	}

	public JTabbedPane createParseTreeAndProfileTabbedPanel() {
        JBTabbedPane tabbedPane = new JBTabbedPane();

        tabbedPane.addTab("Parse tree", createParseTreePanel());

        profilerPanel = new ProfilerPanel(project);
        tabbedPane.addTab("Profiler", profilerPanel.$$$getRootComponent$$$());

        return tabbedPane;
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
		ensureStartRuleExists(grammarFile);
		String grammarFileName = grammarFile.getPath();
		LOG.info("switchToGrammar " + grammarFileName+" "+project.getName());
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFileName);

		inputPanel.grammarFileSaved(grammarFile);

		PreviewState parserState = null;
		if ( previewState.startRuleName!=null ) {
			parserState = previewState; // previewState is combined or parser
		}
		else {// are we lexer and have a parser loaded?
			PreviewState associatedParserIfLexer = controller.getAssociatedParserIfLexer(grammarFileName);
			if ( associatedParserIfLexer!=null ) {
				parserState = associatedParserIfLexer; // need to
			}
		}
		if ( parserState!=null ) { // if we can parse
			updateParseTreeFromDoc(parserState.grammarFileName);
		}
		else {
			setParseTree(Collections.<String>emptyList(), null); // blank tree
		}

		profilerPanel.grammarFileSaved(previewState, grammarFile);
	}

	public void ensureStartRuleExists(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState(grammarFileName);
		// if start rule no longer exists, reset display/state.
		if ( previewState.g!=ParsingUtils.BAD_PARSER_GRAMMAR && previewState.startRuleName!=null ) {
			Rule rule = previewState.g.getRule(previewState.startRuleName);
			if ( rule==null ) {
				previewState.startRuleName = null;
				inputPanel.resetStartRuleLabel();
			}
		}
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileChanged(VirtualFile oldFile, VirtualFile newFile) {
		switchToGrammar(oldFile, newFile);
	}

	/** Load grammars and set editor component. */
	public void switchToGrammar(VirtualFile oldFile, VirtualFile grammarFile) {
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

		profilerPanel.switchToGrammar(previewState, grammarFile);
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

	public void indicateInvalidGrammarInParseTreePane() {
		setParseTree(Arrays.asList(new String[0]),
					 new TerminalNodeImpl(new CommonToken(Token.INVALID_TYPE,
														  "Issues with grammar prevents parsing with preview")));
	}

	public void indicateNoStartRuleInParseTreePane() {
		setParseTree(Arrays.asList(new String[0]),
					 new TerminalNodeImpl(new CommonToken(Token.INVALID_TYPE,
														  "No start rule is selected")));
	}

	public void updateParseTreeFromDoc(VirtualFile grammarFile) {
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
		LOG.info("updateParseTreeFromDoc "+previewState.grammarFileName+" rule "+previewState.startRuleName);
		try {
			final String inputText = previewState.getEditor().getDocument().getText();
			ParsingResult results = controller.parseText(grammarFile, inputText);
			if ( results!=null) {
				setParseTree(Arrays.asList(previewState.g.getRuleNames()), results.tree);
			}
			else if ( previewState.startRuleName==null ) {
				indicateNoStartRuleInParseTreePane();
			}
			else {
				indicateInvalidGrammarInParseTreePane();
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
