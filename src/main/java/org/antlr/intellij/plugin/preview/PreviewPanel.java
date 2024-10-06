package org.antlr.intellij.plugin.preview;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.CaretAdapter;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewParser;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.v4.misc.OrderedHashMap;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.tool.Rule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;

import static com.intellij.icons.AllIcons.Actions.Find;
import static com.intellij.icons.AllIcons.General.AutoscrollFromSource;
import static org.antlr.intellij.plugin.ANTLRv4PluginController.PREVIEW_WINDOW_ID;

/** The top level contents of the preview tool window created by
 *  intellij automatically. Since we need grammars to interpret,
 *  this object creates and caches lexer/parser grammars for
 *  each grammar file it gets notified about.
 */
public class PreviewPanel extends JPanel implements ParsingResultSelectionListener {
	//com.apple.eawt stuff stopped working correctly in java 7 and was only recently fixed in java 9;
	//perhaps in a few more years they will get around to backporting whatever it was they fixed.
	// until then,  the zoomable tree viewer will only be installed if the user is running java 1.6
	private static final boolean isTrackpadZoomSupported =
		SystemInfo.isMac &&
		(SystemInfo.JAVA_VERSION.startsWith("1.6") || SystemInfo.JAVA_VERSION.startsWith("1.9"));

	public static final Logger LOG = Logger.getInstance("ANTLR PreviewPanel");

	public Project project;

	public InputPanel inputPanel;

	private UberTreeViewer treeViewer;
	public HierarchyViewer hierarchyViewer;

	public ProfilerPanel profilerPanel;

	/**
	 * Indicates if the preview should be automatically refreshed after grammar changes.
	 */
	private boolean autoRefresh = true;

	private boolean scrollFromSource = false;
	private boolean highlightSource = false;
	private boolean buildTree = true;
	private boolean buildHierarchy = true;

	private ActionToolbar buttonBar;
	private final CancelParserAction cancelParserAction = new CancelParserAction();

	/** Used to avoid reparsing and also updating the parse tree upon each keystroke. */
	private final MergingUpdateQueue updateQueue;

	public PreviewPanel(Project project) {
		this.project = project;
		createGUI();
		updateQueue =
				new MergingUpdateQueue("(Re-) Parse Queue",
						500,
						true,
						treeViewer
				);
		// If someone is typing, keep resetting timer so parsing doesn't start
		updateQueue.setRestartTimerOnAdd(true);
	}

	private void createGUI() {
		this.setLayout(new BorderLayout());

		// Had to set min size / preferred size in InputPanel.form to get slider to allow left shift of divider
		Splitter splitPane = new Splitter();
		inputPanel = getEditorPanel();
		inputPanel.addCaretListener(new CaretAdapter() {
			@Override
			public void caretPositionChanged(@NotNull CaretEvent event) {
				Caret caret = event.getCaret();

				if ( scrollFromSource && caret != null ) {
					hierarchyViewer.selectNodeAtOffset(caret.getOffset());
				}
			}
		});
		splitPane.setFirstComponent(inputPanel.getComponent());
		splitPane.setSecondComponent(createParseTreeAndProfileTabbedPanel());

		this.add(splitPane, BorderLayout.CENTER);
		this.buttonBar = createButtonBar();
		this.add(buttonBar.getComponent(), BorderLayout.WEST);
	}

	private ActionToolbar createButtonBar() {
		final AnAction refreshAction = new ToggleAction("Refresh Preview Automatically",
				"Refresh preview automatically upon grammar changes", AllIcons.Actions.Refresh) {

			@Override
			public boolean isSelected(@NotNull AnActionEvent e) {
				return autoRefresh;
			}

			@Override
			public void setSelected(@NotNull AnActionEvent e, boolean state) {
				autoRefresh = state;
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}
		};
		ToggleAction scrollFromSourceBtn = new ToggleAction("Scroll from Source", null, AutoscrollFromSource) {
			@Override
			public boolean isSelected(@NotNull AnActionEvent e) {
				return scrollFromSource;
			}

			@Override
			public void setSelected(@NotNull AnActionEvent e, boolean state) {
				scrollFromSource = state;
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}
		};
		ToggleAction scrollToSourceBtn = new ToggleAction("Highlight Source", null, Find) {
			@Override
			public boolean isSelected(@NotNull AnActionEvent e) {
				return highlightSource;
			}

			@Override
			public void setSelected(@NotNull AnActionEvent e, boolean state) {
				highlightSource = state;
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}
		};
		ToggleAction autoBuildTree = new ToggleAction("Build parse tree after parse",null,AllIcons.Toolwindows.ToolWindowHierarchy) {
			@Override
			public boolean isSelected(@NotNull AnActionEvent e) {
				return buildTree;
			}

			@Override
			public void setSelected(@NotNull AnActionEvent e, boolean state) { buildTree = state; }

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}
		};
		ToggleAction autoBuildHier = new ToggleAction("Build hierarchy after parse",null,AllIcons.Actions.ShowAsTree) {
			@Override
			public boolean isSelected(@NotNull AnActionEvent e) {
				return buildHierarchy;
			}

			@Override
			public void setSelected(@NotNull AnActionEvent e, boolean state) {
				buildHierarchy = state;
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}
		};

		DefaultActionGroup actionGroup = new DefaultActionGroup(
				refreshAction,
				cancelParserAction,
				scrollFromSourceBtn,
				scrollToSourceBtn,
				autoBuildTree,
				autoBuildHier
		);

		return ActionManager.getInstance().createActionToolbar(PREVIEW_WINDOW_ID, actionGroup, false);
	}

	private InputPanel getEditorPanel() {
		LOG.info("createEditorPanel"+" "+project.getName());
		return new InputPanel(this);
	}

	public ProfilerPanel getProfilerPanel() {
		return profilerPanel;
	}

	private JTabbedPane createParseTreeAndProfileTabbedPanel() {
		JBTabbedPane tabbedPane = new JBTabbedPane();

		LOG.info("createParseTreePanel" + " " + project.getName());
		Pair<UberTreeViewer, JPanel> pair = createParseTreePanel();
		treeViewer = pair.a;
		setupContextMenu(treeViewer);
		tabbedPane.addTab("Parse tree", pair.b);

		hierarchyViewer = new HierarchyViewer(null);
		hierarchyViewer.addParsingResultSelectionListener(this);
		tabbedPane.addTab("Hierarchy", hierarchyViewer);

		profilerPanel = new ProfilerPanel(project, this);
		tabbedPane.addTab("Profiler", profilerPanel.getComponent());

		return tabbedPane;
	}

	private static void setupContextMenu(final UberTreeViewer treeViewer) {
		treeViewer.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					ParseTreeContextualMenu.showPopupMenu(treeViewer, e);
				}
			}
		});
	}

	private static Pair<UberTreeViewer,JPanel> createParseTreePanel() {
		// wrap tree and slider in panel
		JPanel treePanel = new JPanel(new BorderLayout(0, 0));
		treePanel.setBackground(JBColor.white);

		final UberTreeViewer viewer =
			isTrackpadZoomSupported ?
				new TrackpadZoomingTreeView(null, null, false) :
				new UberTreeViewer(null, null, false);

		JSlider scaleSlider = createTreeViewSlider(viewer);

		// Wrap tree viewer component in scroll pane
		JScrollPane scrollPane = new JBScrollPane(viewer); // use Intellij's scroller

		treePanel.add(scrollPane, BorderLayout.CENTER);

		treePanel.add(scaleSlider, BorderLayout.SOUTH);

		return new Pair<>(viewer, treePanel);
	}

	@NotNull
	private static JSlider createTreeViewSlider(final UberTreeViewer viewer) {
		JSlider scaleSlider;
		if ( isTrackpadZoomSupported ) {
			scaleSlider = new JSlider();
			scaleSlider.setModel(((TrackpadZoomingTreeView) viewer).scaleModel);
		}
		else {
			int sliderValue = (int) ((viewer.getScale() - 1.0) * 1000);
			scaleSlider = new JSlider(JSlider.HORIZONTAL, -999, 1000, sliderValue);
			scaleSlider.addChangeListener(e -> {
				int v = ((JSlider) e.getSource()).getValue();
				if ( viewer.hasTree() ) {
					viewer.setScale(v / 1000.0 + 1.0);
				}
			});
		}
		return scaleSlider;
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileSaved(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("grammarFileSaved " + grammarFileName+" "+project.getName());
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile);

		autoSetStartRule(previewState);

		ensureStartRuleExists(grammarFile);
		inputPanel.grammarFileSaved();

		// if the saved grammar is not a pure lexer and there is a start rule, reparse
		// means that switching grammars must refresh preview
		if ( previewState.g!=null && previewState.startRuleName!=null ) {
			updateParseTreeFromDoc(previewState.grammarFile);
		}
		else {
			clearTabs(null); // blank tree
		}

		profilerPanel.grammarFileSaved(previewState, grammarFile);
	}

	private void ensureStartRuleExists(VirtualFile grammarFile) {
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState(grammarFile);
		// if start rule no longer exists, reset display/state.
		if ( previewState.g!=null &&
			 previewState.g!=ParsingUtils.BAD_PARSER_GRAMMAR &&
			 previewState.startRuleName!=null )
		{
			Rule rule = previewState.g.getRule(previewState.startRuleName);
			if ( rule==null ) {
				previewState.startRuleName = null;
				inputPanel.resetStartRuleLabel();
			}
		}
	}

	/** Notify the preview tool window contents that the grammar file has changed */
	public void grammarFileChanged(VirtualFile newFile) {
		switchToGrammar(newFile);
	}

	/** Load grammars and set editor component. */
	private void switchToGrammar(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("switchToGrammar " + grammarFileName+" "+project.getName());
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile);

		autoSetStartRule(previewState);

		inputPanel.switchToGrammar(previewState, grammarFile);
		profilerPanel.switchToGrammar(previewState, grammarFile);

		if ( previewState.startRuleName!=null ) {
			updateParseTreeFromDoc(grammarFile); // regens tree and profile data
		}
		else {
			clearTabs(null); // blank tree
		}

		setEnabled(previewState.g!=null || previewState.lg==null);
	}

	/** From 1.18, automatically set the start rule name to the first rule in the grammar
	 * if none has been specified
	 */
	protected void autoSetStartRule(PreviewState previewState) {
		if ( previewState.g==null || previewState.g.rules.size()==0 ) {
			// If there is no grammar all of a sudden, we need to unset the previous rule name
			previewState.startRuleName = null;
		}
		else if ( previewState.startRuleName==null ) {
			OrderedHashMap<String, Rule> rules = previewState.g.rules;
			previewState.startRuleName = rules.getElement(0).name;
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		this.setEnabledRecursive(this, enabled);
	}

	private void setEnabledRecursive(Component component, boolean enabled) {
		if (component instanceof JTable) {
			// seems there's a special case
			((JTable) component).getTableHeader().setEnabled(enabled);
		}
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				child.setEnabled(enabled);
				setEnabledRecursive(child, enabled);
			}
		}
	}

	public void closeGrammar(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("closeGrammar "+grammarFileName+" "+project.getName());

		inputPanel.resetStartRuleLabel();
		inputPanel.clearErrorConsole();
		clearParseTree(); // wipe tree

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		PreviewState previewState = controller.getPreviewState(grammarFile);
		inputPanel.releaseEditor(previewState);
	}

	private void clearTabs(@Nullable ParseTree tree) {
		ApplicationManager.getApplication().invokeLater(() -> {
			treeViewer.setRuleNames(Collections.emptyList());
			treeViewer.setTree(tree);
			hierarchyViewer.setTree(null);
			hierarchyViewer.setRuleNames(Collections.emptyList());
		});
	}

	private void updateTreeViewer(final PreviewState preview, final ParsingResult result) {
//		long start = System.nanoTime();
//		System.out.println("START updateTreeViewer "+Thread.currentThread().getName());
		if (result.parser instanceof PreviewParser) {
			AltLabelTextProvider provider = new AltLabelTextProvider(result.parser, preview.g);
			if(buildTree) {
				treeViewer.setTreeTextProvider(provider);
				treeViewer.setTree(result.tree);
			}
			if(buildHierarchy) {
				hierarchyViewer.setTreeTextProvider(provider);
				hierarchyViewer.setTree(result.tree);
			}
		}
		else {
			if(buildTree) {
				treeViewer.setRuleNames(Arrays.asList(preview.g.getRuleNames()));
				treeViewer.setTree(result.tree);
			}
			if(buildHierarchy) {
				hierarchyViewer.setRuleNames(Arrays.asList(preview.g.getRuleNames()));
				hierarchyViewer.setTree(result.tree);
			}
		}
//		long parseTime_ns = System.nanoTime() - start;
//		double parseTimeMS = parseTime_ns/(1000.0*1000.0);
//		System.out.println("STOP updateTreeViewer "+Thread.currentThread().getName()+" "+parseTimeMS+"ms");
	}


	void clearParseTree() {
		clearTabs(null);
	}

	private void indicateInvalidGrammarInParseTreePane() {
		showError("Issues with parser and/or lexer grammar(s) prevent preview; see ANTLR 'Tool Output' pane");
	}

	private void showError(String message) {
		clearTabs(new TerminalNodeImpl(new CommonToken(Token.INVALID_TYPE, message)));
	}

	private void indicateNoStartRuleInParseTreePane() {
		showError("No start rule is selected");
	}

	public void updateParseTreeFromDoc(VirtualFile grammarFile) {
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		if ( controller==null ) return;
		PreviewState previewState = controller.getPreviewState(grammarFile);
		LOG.info("updateParseTreeFromDoc "+grammarFile+" rule "+previewState.startRuleName);
		if ( previewState.g==null || previewState.lg==null ) {
			// likely error in grammar prevents it from loading properly into previewState; bail
			indicateInvalidGrammarInParseTreePane();
			return;
		}

		Editor editor = inputPanel.getInputEditor();
		if ( editor==null ) return;
		final String inputText = editor.getDocument().getText();

		// The controller will call us back when it's done parsing
		// Wipes out the console and also any error annotations
		updateQueue.queue(new Update(this) {
			@Override
			public boolean canEat(Update update) {
				return true; // kill any previous queued up parses; only last keystroke input text matters
			}
			@Override
			public void run() {
				inputPanel.clearParseErrors();
				controller.startParsing();
//				System.out.println("PARSE:\n"+inputText);
				controller.parseText(grammarFile, inputText);
			}
		});
	}

	public InputPanel getInputPanel() {
		return inputPanel;
	}

	public void autoRefreshPreview(VirtualFile virtualFile) {
		final ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);

		if (autoRefresh
				&& controller != null
				&& inputPanel.previewState != null
				&& inputPanel.previewState.startRuleName != null) {
			ApplicationManager.getApplication().invokeLater(() -> controller.grammarFileSavedEvent(virtualFile));
		}
	}

	public void onParsingCompleted(PreviewState previewState, long duration) {
		ApplicationManager.getApplication().invokeLater(() -> { // make sure we're on GUI thread for this block
			cancelParserAction.setEnabled(false);
			buttonBar.updateActionsImmediately();

			if (previewState.parsingResult != null) {
				updateTreeViewer(previewState, previewState.parsingResult);
				profilerPanel.setProfilerData(previewState, duration);
				inputPanel.showParseErrors(previewState.parsingResult.syntaxErrorListener.getSyntaxErrors());
			}
			else if (previewState.startRuleName == null) {
				indicateNoStartRuleInParseTreePane();
			}
			else {
				indicateInvalidGrammarInParseTreePane();
			}
		});
	}

	public void notifySlowParsing() {
		cancelParserAction.setEnabled(true);
		buttonBar.updateActionsImmediately();
	}

	public void onParsingCancelled() {
		cancelParserAction.setEnabled(false);
		buttonBar.updateActionsImmediately();
		showError("Parsing was aborted");
	}

	public void startParsing() {
		cancelParserAction.setEnabled(false);
		buttonBar.updateActionsImmediately();
	}

	@Override
	public void onParserRuleSelected(Tree tree) {
		int startIndex;
		int stopIndex;

		if ( tree instanceof ParserRuleContext ) {
			Token start = ((ParserRuleContext) tree).getStart();
			Token stop = ((ParserRuleContext) tree).getStop();
			if ( start==null || stop==null ) { // stop can be null if start is EOF; nothing to show so return
				return;
			}
			startIndex = start.getStartIndex();
			stopIndex = stop.getStopIndex();
		}
		else if ( tree instanceof TerminalNode ) {
			startIndex = ((TerminalNode) tree).getSymbol().getStartIndex();
			stopIndex = ((TerminalNode) tree).getSymbol().getStopIndex();
		}
		else {
			return;
		}

		// ANTLRv4PluginController.parseText() lazily updates the parse tree so it's possible
		// that we have edited the input and something triggers a click on the Hierarchy pane
		// before the tree is done and therefore the tree parameter to this method.
		// Avoid trying to select text outside of doc[0..stopindex] as a general rule too.
		// It also looks like previous code was triggering an update to hierarchy view when we
		// click in input pane which then tried to select entire token like a string. Now,
		// text is selected in input pane only when a mouse event occurs in hierarchy pane.
		Editor editor = inputPanel.getInputEditor();
		if ( startIndex>=0 && stopIndex+1 <= editor.getDocument().getTextLength() ) {
			SelectionModel selectionModel = editor.getSelectionModel();
			selectionModel.removeSelection();
			selectionModel.setSelection(startIndex, stopIndex + 1);
		}
	}
}
