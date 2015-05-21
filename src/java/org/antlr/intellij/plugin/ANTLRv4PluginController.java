package org.antlr.intellij.plugin;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.antlr.intellij.plugin.preview.PreviewPanel;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.tool.Grammar;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** This object is the controller for the ANTLR plug-in. It receives
 *  events and can send them on to its contained components. For example,
 *  saving the grammar editor or flipping to a new grammar sends an event
 *  to this object, which forwards on update events to the preview tool window.
 *
 *  The main components are related to the console tool window forever output and
 *  the main panel of the preview tool window.
 *
 *  This controller also manages the cache of grammar/editor combinations
 *  needed for the preview window. Updates must be made atomically so that
 *  the grammars and editors are consistently associated with the same window.
 */
public class ANTLRv4PluginController implements ProjectComponent {
	public static final Key<GrammarEditorMouseAdapter> EDITOR_MOUSE_LISTENER_KEY = Key.create("EDITOR_MOUSE_LISTENER_KEY");
	public static final Logger LOG = Logger.getInstance("ANTLR ANTLRv4PluginController");

	public static final String PREVIEW_WINDOW_ID = "Preview";
	public static final String CONSOLE_WINDOW_ID = "Tool Output";
	public static final String PROFILER_WINDOW_ID = "Profiler";

	public boolean projectIsClosed = false;

	public Project project;
	public ConsoleView console;
	public ToolWindow consoleWindow;

	public Map<String, PreviewState> grammarToPreviewState =
		Collections.synchronizedMap(new HashMap<String, PreviewState>());
	public ToolWindow previewWindow;	// same for all grammar editor
	public PreviewPanel previewPanel;	// same for all grammar editor

	public MyVirtualFileAdapter myVirtualFileAdapter = new MyVirtualFileAdapter();
	public MyFileEditorManagerAdapter myFileEditorManagerAdapter = new MyFileEditorManagerAdapter();

	public ANTLRv4PluginController(Project project) {
		this.project = project;
	}

	public static ANTLRv4PluginController getInstance(Project project) {
		ANTLRv4PluginController pc = project.getComponent(ANTLRv4PluginController.class);
		return pc;
	}

	@Override
	public void initComponent() {
	}

	@Override
	public void projectOpened() {
		// make sure the tool windows are created early
		createToolWindows();
		installListeners();
	}

	public void createToolWindows() {
		LOG.info("createToolWindows "+project.getName());
		ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);

		previewPanel = new PreviewPanel(project);

		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
		Content content = contentFactory.createContent(previewPanel, "", false);

		previewWindow = toolWindowManager.registerToolWindow(PREVIEW_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
		previewWindow.getContentManager().addContent(content);
		previewWindow.setIcon(Icons.FILE);

		TextConsoleBuilderFactory factory = TextConsoleBuilderFactory.getInstance();
		TextConsoleBuilder consoleBuilder = factory.createBuilder(project);
		this.console = consoleBuilder.getConsole();

		JComponent consoleComponent = console.getComponent();
		content = contentFactory.createContent(consoleComponent, "", false);

		consoleWindow = toolWindowManager.registerToolWindow(CONSOLE_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
		consoleWindow.getContentManager().addContent(content);
		consoleWindow.setIcon(Icons.FILE);

//		ToolWindow profilerWindow = toolWindowManager.registerToolWindow(PROFILER_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
//		JPanel panel = new JPanel(new BorderLayout());
//		profilerWindow.getComponent().add(panel);
//		profilerWindow.setIcon(Icons.FILE);
//		final EditorFactory edfactory = EditorFactory.getInstance();
//		Document doc = edfactory.createDocument("foo\nbar\n");
//		RangeMarker rangeMarker = doc.createRangeMarker(2, 5);
//		final Editor editor = edfactory.createEditor(doc, previewPanel.project);
//		EditorSettings settings = editor.getSettings();
//		settings.setWhitespacesShown(true);
//		settings.setLineNumbersShown(true);
//		settings.setLineMarkerAreaShown(true);
//		MarkupModel markupModel = editor.getMarkupModel();
//		panel.add(editor.getComponent());
//
//
//
//		TextAttributes textAttributes =
//			new TextAttributes(JBColor.BLACK, JBColor.WHITE, JBColor.RED, EffectType.WAVE_UNDERSCORE, 1);
//		textAttributes.setErrorStripeColor(JBColor.RED);
//		final RangeHighlighter lineHighlighter =
//			markupModel.addRangeHighlighter(2,
//											5,
//											HighlighterLayer.ADDITIONAL_SYNTAX, textAttributes,
//											HighlighterTargetArea.EXACT_RANGE);
//		lineHighlighter.setErrorStripeMarkColor(JBColor.BLUE);
	}

	@Override
	public void projectClosed() {
		LOG.info("projectClosed " + project.getName());
		//synchronized ( shutdownLock ) { // They should be called from EDT only so no lock
		projectIsClosed = true;
		uninstallListeners();

		console.dispose();

		for (PreviewState it : grammarToPreviewState.values()) {
			previewPanel.inputPanel.releaseEditor(it);
		}

		previewPanel = null;
		previewWindow = null;
		consoleWindow = null;
		project = null;
		grammarToPreviewState = null;
	}

	// seems that intellij can kill and reload a project w/o user knowing.
	// a ptr was left around that pointed at a disposed project. led to
	// problem in switchGrammar. Probably was a listener still attached and trigger
	// editor listeners released in editorReleased() events.
	public void uninstallListeners() {
		VirtualFileManager.getInstance().removeVirtualFileListener(myVirtualFileAdapter);
		MessageBusConnection msgBus = project.getMessageBus().connect(project);
		msgBus.disconnect();
	}

	@Override
	public void disposeComponent() {
	}

	@NotNull
	@Override
	public String getComponentName() {
		return "antlr.ProjectComponent";
	}

	// ------------------------------

	public void installListeners() {
		LOG.info("installListeners "+project.getName());
		// Listen for .g4 file saves
		VirtualFileManager.getInstance().addVirtualFileListener(myVirtualFileAdapter);

		// Listen for editor window changes
		MessageBusConnection msgBus = project.getMessageBus().connect(project);
		msgBus.subscribe(
			FileEditorManagerListener.FILE_EDITOR_MANAGER,
			myFileEditorManagerAdapter
		);

		EditorFactory factory = EditorFactory.getInstance();
		factory.addEditorFactoryListener(
			new EditorFactoryAdapter() {
				@Override
				public void editorCreated(@NotNull EditorFactoryEvent event) {
					Editor editor = event.getEditor();
					GrammarEditorMouseAdapter listener = new GrammarEditorMouseAdapter();
					editor.putUserData(EDITOR_MOUSE_LISTENER_KEY, listener);
					editor.addEditorMouseListener(listener);
				}

				@Override
				public void editorReleased(@NotNull EditorFactoryEvent event) {
					Editor editor = event.getEditor();
					GrammarEditorMouseAdapter listener = editor.getUserData(EDITOR_MOUSE_LISTENER_KEY);
					if (editor.getProject() != null && editor.getProject() != project) {
						return;
					}
					if (listener != null) {
						editor.removeEditorMouseListener(listener);
						editor.putUserData(EDITOR_MOUSE_LISTENER_KEY, null);
					}
				}
			}
		);
	}

	/** The test ANTLR rule action triggers this event. This can occur
	 *  only occur when the current editor the showing a grammar, because
	 *  that is the only time that the action is enabled. We will see
	 *  a file changed event when the project loads the first grammar file.
	 */
	public void setStartRuleNameEvent(VirtualFile grammarFile, String startRuleName) {
		LOG.info("setStartRuleNameEvent " + startRuleName+" "+project.getName());
		PreviewState previewState = getPreviewState(grammarFile);
		previewState.startRuleName = startRuleName;
		if ( previewPanel!=null ) {
			previewPanel.getInputPanel().setStartRuleName(grammarFile, startRuleName); // notify the view
			previewPanel.updateParseTreeFromDoc(grammarFile);
		}
		else {
			LOG.error("setStartRuleNameEvent called before preview panel created");
		}
	}

	public void grammarFileSavedEvent(VirtualFile grammarFile) {
		LOG.info("grammarFileSavedEvent "+grammarFile.getPath()+" "+project.getName());
		updateGrammarObjectsFromFile(grammarFile); // force reload
		if ( previewPanel!=null ) {
			previewPanel.grammarFileSaved(grammarFile);
		}
		else {
			LOG.error("grammarFileSavedEvent called before preview panel created");
		}
		runANTLRTool(grammarFile);
	}

	public void currentEditorFileChangedEvent(VirtualFile oldFile, VirtualFile newFile) {
		LOG.info("currentEditorFileChangedEvent "+(oldFile!=null?oldFile.getPath():"none")+
				 " -> "+(newFile!=null?newFile.getPath():"none")+" "+project.getName());
		if ( newFile==null ) { // all files must be closed I guess
			return;
		}
		if ( newFile.getName().endsWith(".g") ) {
			LOG.info("currentEditorFileChangedEvent ANTLR 4 cannot handle .g files, only .g4");
			previewWindow.hide(null);
			return;
		}
		if ( !newFile.getName().endsWith(".g4") ) {
			previewWindow.hide(null);
			return;
		}
		PreviewState previewState = getPreviewState(newFile);
		if ( previewState.g==null && previewState.lg==null ) { // only load grammars if none is there
			updateGrammarObjectsFromFile(newFile);
		}
		if ( previewPanel!=null ) {
			previewPanel.grammarFileChanged(oldFile, newFile);
		}
	}

	public void mouseEnteredGrammarEditorEvent(VirtualFile vfile, EditorMouseEvent e) {
		if ( previewPanel!=null ) {
			ProfilerPanel profilerPanel = previewPanel.getProfilerPanel();
			if ( profilerPanel!=null ) {
				profilerPanel.mouseEnteredGrammarEditorEvent(vfile, e);
			}
		}
	}

	public void editorFileClosedEvent(VirtualFile vfile) {
		// hopefully called only from swing EDT
		String grammarFileName = vfile.getPath();
		LOG.info("editorFileClosedEvent "+ grammarFileName+" "+project.getName());
		if ( !vfile.getName().endsWith(".g4") ) {
			previewWindow.hide(null);
			return;
		}

		// Dispose of state, editor, and such for this file
		PreviewState previewState = grammarToPreviewState.get(grammarFileName);
		if ( previewState==null ) { // project closing must have done already
			return;
		}

		previewState.g = null; // wack old ref to the Grammar for text in editor
		previewState.lg = null;

		previewPanel.closeGrammar(vfile);

		grammarToPreviewState.remove(grammarFileName);

		// close tool window
		previewWindow.hide(null);
	}

	/** Make sure to run after updating grammars in previewState */
	public void runANTLRTool(final VirtualFile grammarFile) {
		String title = "ANTLR Code Generation";
		boolean canBeCancelled = true;
		boolean forceGeneration = false;
		Task gen =
			new RunANTLROnGrammarFile(grammarFile,
									  project,
									  title,
									  canBeCancelled,
									  forceGeneration);
		ProgressManager.getInstance().run(gen);
	}

	/** Look for state information concerning this grammar file and update
	 *  the Grammar objects.  This does not necessarily update the grammar file
	 *  in the current editor window.  Either we are already looking at
	 *  this grammar or we will have seen a grammar file changed event.
	 *  (I hope!)
	 */
	public void updateGrammarObjectsFromFile(VirtualFile grammarFile) {
		updateGrammarObjectsFromFile_(grammarFile);

		// if grammarFileName is a separate lexer, we need to look for
		// its matching parser, if any, that is loaded in an editor
		// (don't go looking on disk).
		PreviewState s = getAssociatedParserIfLexer(grammarFile.getPath());
		if ( s!=null ) {
			// try to load lexer again and associate with this parser grammar.
			// must update parser too as tokens have changed
			updateGrammarObjectsFromFile_(s.grammarFile);
		}
	}

	public String updateGrammarObjectsFromFile_(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		PreviewState previewState = getPreviewState(grammarFile);
		Grammar[] grammars = ParsingUtils.loadGrammars(grammarFileName, project);
		if (grammars != null) {
			synchronized (previewState) { // build atomically
				previewState.lg = grammars[0];
				previewState.g = grammars[1];
			}
		}
		return grammarFileName;
	}

	public PreviewState getAssociatedParserIfLexer(String grammarFileName) {
		for (PreviewState s : grammarToPreviewState.values()) {
			if ( s!=null && s.lg!=null &&
				 (grammarFileName.equals(s.lg.fileName)||s.lg==ParsingUtils.BAD_LEXER_GRAMMAR) )
			{
				// s has a lexer with same filename, see if there is a parser grammar
				// (not a combined grammar)
				if ( s.g!=null && s.g.getType()==ANTLRParser.PARSER ) {
					System.out.println(s.lg.fileName+" vs "+grammarFileName+", g="+s.g.name+", type="+s.g.getTypeString());
					return s;
				}
			}
		}
		return null;
	}

	public ParsingResult parseText(final VirtualFile grammarFile, String inputText) throws IOException {
		String grammarFileName = grammarFile.getPath();
		final PreviewState previewState = getPreviewState(grammarFile);
		if (!new File(grammarFileName).exists()) {
			LOG.error("parseText grammar doesn't exit " + grammarFileName);
			return null;
		}

		// Wipes out the console and also any error annotations
		previewPanel.inputPanel.clearParseErrors(grammarFile);

		long start = System.nanoTime();
		previewState.parsingResult = ParsingUtils.parseText(previewState, previewPanel, grammarFile, inputText);
		if ( previewState.parsingResult==null ) {
			return null;
		}
		long stop = System.nanoTime();

		previewPanel.profilerPanel.setProfilerData(previewState, stop-start);

		SyntaxErrorListener syntaxErrorListener = previewState.parsingResult.syntaxErrorListener;
		previewPanel.inputPanel.showParseErrors(grammarFile, syntaxErrorListener.getSyntaxErrors());

		previewPanel.getProfilerPanel().tagAmbiguousDecisionsInGrammar(previewState);

		return previewState.parsingResult;
	}

	public PreviewPanel getPreviewPanel() {
		return previewPanel;
	}

	public ConsoleView getConsole() {
		return console;
	}

	public ToolWindow getConsoleWindow() {
		return consoleWindow;
	}

	public static void showConsoleWindow(final Project project) {
		ApplicationManager.getApplication().invokeLater(
			new Runnable() {
				@Override
				public void run() {
					ANTLRv4PluginController.getInstance(project).getConsoleWindow().show(null);
				}
			}
		);
	}

	public ToolWindow getPreviewWindow() {
		return previewWindow;
	}

	public @NotNull PreviewState getPreviewState(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		// Have we seen this grammar before?
		PreviewState stateForCurrentGrammar = grammarToPreviewState.get(grammarFileName);
		if ( stateForCurrentGrammar!=null ) {
			return stateForCurrentGrammar; // seen this before
		}

		// not seen, must create state
		stateForCurrentGrammar = new PreviewState(grammarFile);
		grammarToPreviewState.put(grammarFileName, stateForCurrentGrammar);

		return stateForCurrentGrammar;
	}

	/** Get the state information associated with the grammar in the current
	 *  editor window. If there is no grammar in the editor window, return null.
	 *  If there is a grammar, return any existing preview state else
	 *  create a new one in store in the map.
	 */
	public @org.jetbrains.annotations.Nullable PreviewState getPreviewState() {
		VirtualFile currentGrammarFile = getCurrentGrammarFile();
		if ( currentGrammarFile==null ) {
			return null;
		}
		String currentGrammarFileName = currentGrammarFile.getPath();
		if ( currentGrammarFileName==null ) {
			return null; // we are not looking at a grammar file
		}
		return getPreviewState(currentGrammarFile);
	}

	// These "get current editor file" routines should only be used
	// when you are sure the user is in control and is viewing the
	// right file (i.e., don't use these during project loading etc...)

	public static VirtualFile getCurrentEditorFile(Project project) {
		FileEditorManager fmgr = FileEditorManager.getInstance(project);
		VirtualFile files[] = fmgr.getSelectedFiles();
		if ( files.length == 0 ) {
			return null;
		}
		return files[0];
	}

	public Editor getCurrentGrammarEditor() {
		FileEditorManager edMgr = FileEditorManager.getInstance(project);
		return edMgr.getSelectedTextEditor();
	}

	public VirtualFile getCurrentGrammarFile() {
		return getCurrentGrammarFile(project);
	}

	public static VirtualFile getCurrentGrammarFile(Project project) {
		VirtualFile f = getCurrentEditorFile(project);
		if ( f==null ) {
			return null;
		}
		if ( f.getName().endsWith(".g4") ) return f;
		return null;
	}

	private class GrammarEditorMouseAdapter extends EditorMouseAdapter {
		@Override
		public void mouseClicked(EditorMouseEvent e) {
			Document doc = e.getEditor().getDocument();
			VirtualFile vfile = FileDocumentManager.getInstance().getFile(doc);
			if ( vfile!=null && vfile.getName().endsWith(".g4") ) {
				mouseEnteredGrammarEditorEvent(vfile, e);
			}
		}
	}

	private class MyVirtualFileAdapter extends VirtualFileAdapter {
		@Override
		public void contentsChanged(VirtualFileEvent event) {
			final VirtualFile vfile = event.getFile();
			if ( !vfile.getName().endsWith(".g4") ) return;
			if ( !projectIsClosed ) grammarFileSavedEvent(vfile);
		}
	}

	private class MyFileEditorManagerAdapter extends FileEditorManagerAdapter {
		@Override
		public void selectionChanged(FileEditorManagerEvent event) {
			if ( !projectIsClosed ) currentEditorFileChangedEvent(event.getOldFile(), event.getNewFile());
		}

		@Override
		public void fileClosed(FileEditorManager source, VirtualFile file) {
			if ( !projectIsClosed ) editorFileClosedEvent(file);
		}
	}

}
