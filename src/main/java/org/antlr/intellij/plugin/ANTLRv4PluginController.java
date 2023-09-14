package org.antlr.intellij.plugin;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
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
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.antlr.intellij.plugin.preview.PreviewPanel;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
	public static final String PLUGIN_ID = "org.antlr.intellij.plugin";

	public static final Key<GrammarEditorMouseAdapter> EDITOR_MOUSE_LISTENER_KEY = Key.create("EDITOR_MOUSE_LISTENER_KEY");
	public static final Logger LOG = Logger.getInstance("ANTLRv4PluginController");

	public static final String PREVIEW_WINDOW_ID = "ANTLR Preview";
	public static final String CONSOLE_WINDOW_ID = "Tool Output";

	public boolean projectIsClosed = false;

	public Project project;
	public ConsoleView console;
	private ToolWindow consoleWindow;

	public Map<String, PreviewState> grammarToPreviewState =
		Collections.synchronizedMap(new HashMap<>());
	public ToolWindow previewWindow;	// same for all grammar editor
	public PreviewPanel previewPanel;	// same for all grammar editor

	public MyVirtualFileAdapter myVirtualFileAdapter = new MyVirtualFileAdapter();
	public MyFileEditorManagerAdapter myFileEditorManagerAdapter = new MyFileEditorManagerAdapter();

	private ProgressIndicator parsingProgressIndicator;

	private final Map<String, Long> grammarFileMods = new HashMap<>();

	public ANTLRv4PluginController(Project project) {
		this.project = project;
	}

	public static ANTLRv4PluginController getInstance(Project project) {
		if ( project==null ) {
			LOG.error("getInstance: project is null");
			return null;
		}
		if ( project.isDisposed() ) {
			LOG.error("getInstance: project is already disposed");
			return null;
		}
		ANTLRv4PluginController pc = project.getComponent(ANTLRv4PluginController.class);
		if ( pc==null ) {
			LOG.error("getInstance: getComponent() for "+project.getName()+" returns null");
		}
		return pc;
	}

	@Override
	public void initComponent() {
	}

	@Override
	public void projectOpened() {
		IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(PLUGIN_ID));
		String version = "unknown";
		if ( plugin!=null ) {
			version = plugin.getVersion();
		}
		LOG.info("ANTLR 4 Plugin version "+version+", Java version "+ SystemInfo.JAVA_VERSION);
		// make sure the tool windows are created early
		createToolWindows();
		installListeners();
	}

	public void createToolWindows() {
		LOG.info("createToolWindows "+project.getName());
		ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);

		previewPanel = new PreviewPanel(project);

		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

		toolWindowManager.invokeLater(() -> {
			Content content = contentFactory.createContent(previewPanel, "", false);
			content.setCloseable(false);

			previewWindow = toolWindowManager.registerToolWindow(PREVIEW_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
			previewWindow.getContentManager().addContent(content);
			previewWindow.setIcon(Icons.getToolWindow());
		});

		TextConsoleBuilderFactory factory = TextConsoleBuilderFactory.getInstance();
		TextConsoleBuilder consoleBuilder = factory.createBuilder(project);
		this.console = consoleBuilder.getConsole();

		toolWindowManager.invokeLater(() -> {
			JComponent consoleComponent = console.getComponent();
			Content content = contentFactory.createContent(consoleComponent, "", false);
			content.setCloseable(false);
			content.setDisposer(console);

			consoleWindow = toolWindowManager.registerToolWindow(CONSOLE_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
			consoleWindow.getContentManager().addContent(content);
			consoleWindow.setIcon(Icons.getToolWindow());
		});
	}

	@Override
	public void projectClosed() {
		LOG.info("projectClosed " + project.getName());
		//synchronized ( shutdownLock ) { // They should be called from EDT only so no lock
		projectIsClosed = true;
		uninstallListeners();

		for (PreviewState it : grammarToPreviewState.values()) {
			previewPanel.inputPanel.releaseEditor(it);
		}

		previewPanel = null;
		previewWindow = null;
		consoleWindow = null;
		project = null;

		// We can't dispose of the preview state map during unit tests
		if (ApplicationManager.getApplication().isUnitTestMode()) return;
		grammarToPreviewState = null;
	}

	// seems that intellij can kill and reload a project w/o user knowing.
	// a ptr was left around that pointed at a disposed project. led to
	// problem in switchGrammar. Probably was a listener still attached and trigger
	// editor listeners released in editorReleased() events.
	public void uninstallListeners() {
		VirtualFileManager.getInstance().removeVirtualFileListener(myVirtualFileAdapter);

		if ( !project.isDisposed() ) {
			MessageBusConnection msgBus = project.getMessageBus().connect(project);
			msgBus.disconnect();
		}
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
					final Editor editor = event.getEditor();
					final Document doc = editor.getDocument();
					VirtualFile vfile = FileDocumentManager.getInstance().getFile(doc);
					if ( vfile!=null && vfile.getName().endsWith(".g4") ) {
						GrammarEditorMouseAdapter listener = new GrammarEditorMouseAdapter();
						editor.putUserData(EDITOR_MOUSE_LISTENER_KEY, listener);
						editor.addEditorMouseListener(listener);
					}
				}

				@Override
				public void editorReleased(@NotNull EditorFactoryEvent event) {
					Editor editor = event.getEditor();
					if (editor.getProject() != null && editor.getProject() != project) {
						return;
					}
					GrammarEditorMouseAdapter listener = editor.getUserData(EDITOR_MOUSE_LISTENER_KEY);
					if (listener != null) {
						editor.removeEditorMouseListener(listener);
						editor.putUserData(EDITOR_MOUSE_LISTENER_KEY, null);
					}
				}
			}
		);
	}

	/** The test ANTLR rule action triggers this event. This can occur
	 *  only occur when the current editor is showing a grammar, because
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

		Long modCount = grammarFile.getModificationCount();
		String grammarFilePath = grammarFile.getPath();

		if (grammarFileMods.containsKey(grammarFilePath) && grammarFileMods.get(grammarFilePath).equals(modCount)) {
			return;
		}

		grammarFileMods.put(grammarFilePath, modCount);

		LOG.info("grammarFileSavedEvent "+grammarFilePath+" "+project.getName());
		updateGrammarObjectsFromFile(grammarFile, true); // force reload
		if ( previewPanel!=null ) {
			previewPanel.grammarFileSaved(grammarFile);
		}
		else {
			LOG.error("grammarFileSavedEvent called before preview panel created");
		}
	}

	public void currentEditorFileChangedEvent(VirtualFile oldFile, VirtualFile newFile, boolean modified) {
		LOG.info("currentEditorFileChangedEvent "+(oldFile!=null?oldFile.getPath():"none")+
				 " -> "+(newFile!=null?newFile.getPath():"none")+" "+project.getName());
		if ( newFile==null ) { // all files must be closed I guess
			return;
		}

		String newFileExt = newFile.getExtension();

		if (newFileExt == null) {
			return;
		}

		if (newFileExt.equals("g")) {
			LOG.info("currentEditorFileChangedEvent ANTLR 4 cannot handle .g files, only .g4");
			hidePreview();
			return;
		}

		if ( !newFileExt.equals("g4") ) {
			return;
		}

		// When switching from a lexer grammar, update its objects in case the grammar was modified.
		// The updated objects might be needed later by another dependant grammar.
		if ( oldFile != null && "g4".equals(oldFile.getExtension()) && modified) {
			updateGrammarObjectsFromFile(oldFile, true);
		}

		PreviewState previewState = getPreviewState(newFile);
		if ( previewState.g==null && previewState.lg==null ) { // only load grammars if none is there
			updateGrammarObjectsFromFile(newFile, false);
		}
		if ( previewPanel!=null ) {
			previewPanel.grammarFileChanged(newFile);
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
			hidePreview();
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
		hidePreview();
	}

	private void hidePreview() {
		if (previewPanel != null) {
			previewPanel.setEnabled(false);
		}
		if (previewWindow != null) {
			previewWindow.hide(null);
		}
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
	private void updateGrammarObjectsFromFile(VirtualFile grammarFile, boolean generateTokensFile) {
		updateGrammarObjectsFromFile_(grammarFile);

		// if grammarFileName is a separate lexer, we need to look for
		// its matching parser, if any, that is loaded in an editor
		// (don't go looking on disk).
		PreviewState s = getAssociatedParserIfLexer(grammarFile.getPath());
		if ( s!=null ) {
			if (generateTokensFile) {
				// Run the tool to regenerate the .tokens file, which will be
				// needed in the parser grammar
				runANTLRTool(grammarFile);
			}

			// try to load lexer again and associate with this parser grammar.
			// must update parser too as tokens have changed
			updateGrammarObjectsFromFile_(s.grammarFile);
		}
	}

	private String updateGrammarObjectsFromFile_(VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		PreviewState previewState = getPreviewState(grammarFile);
		Grammar[] grammars = ParsingUtils.loadGrammars(grammarFile, project);
		if (grammars != null) {
			synchronized (previewState) { // build atomically
				previewState.lg = (LexerGrammar)grammars[0];
				previewState.g = grammars[1];
			}
		}
		else {
			synchronized (previewState) { // build atomically
				previewState.lg = null;
				previewState.g = null;
			}
		}
		return grammarFileName;
	}

	// TODO there could be multiple grammars importing/tokenVocab'ing this lexer grammar
	public PreviewState getAssociatedParserIfLexer(String grammarFileName) {
		for (PreviewState s : grammarToPreviewState.values()) {
			if ( s!=null && s.lg!=null &&
					(sameFile(grammarFileName, s.lg.fileName)||s.lg==ParsingUtils.BAD_LEXER_GRAMMAR) )
			{
				// s has a lexer with same filename, see if there is a parser grammar
				// (not a combined grammar)
				if ( s.g!=null && s.g.getType()==ANTLRParser.PARSER ) {
					return s;
				}
			}

			if ( s!=null && s.g!=null && s.g.importedGrammars!=null ) {
				for ( Grammar importedGrammar : s.g.importedGrammars ) {
					if (grammarFileName.equals(importedGrammar.fileName)) {
						return s;
					}
				}
			}
		}
		return null;
	}

	private boolean sameFile(String pathOne, String pathTwo) {
		// use new File() to support both / and \ in paths
		return new File(pathOne).equals(new File(pathTwo));
	}

	public void parseText(final VirtualFile grammarFile, String inputText) {
		final PreviewState previewState = getPreviewState(grammarFile);

		// No need to parse empty text during unit tests, yet...
		if (inputText.isEmpty() && ApplicationManager.getApplication().isUnitTestMode()) return;

		// Parse text in a background thread to avoid freezing the UI if the grammar is badly written
		// and takes forever to interpret the input.
		parsingProgressIndicator = BackgroundTaskUtil.executeAndTryWait(
				(indicator) -> {
//					System.out.println("PARSE START "+Thread.currentThread().getName());
					long start = System.nanoTime();

					previewState.parsingResult = ParsingUtils.parseText(
							previewState.g, previewState.lg, previewState.startRuleName,
							grammarFile, inputText, project
					);

//					long parseTime_ns = System.nanoTime() - start;
//					double parseTimeMS = parseTime_ns/(1000.0*1000.0);
//					System.out.println("PARSE STOP "+Thread.currentThread().getName()+" "+parseTimeMS+"ms");
					return () -> previewPanel.onParsingCompleted(previewState, System.nanoTime() - start);
				},
				() -> previewPanel.notifySlowParsing(),
				ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS,
				false
		);
	}

	public void abortCurrentParsing() {
		if ( parsingProgressIndicator!=null ) {
			parsingProgressIndicator.cancel();
			parsingProgressIndicator = null;
			previewPanel.onParsingCancelled();
		}
	}

	public void startParsing() {
		parsingProgressIndicator = null;
		if (previewPanel == null) return;
		previewPanel.inputPanel.clearParseErrors(); // Wipes out the console and also any error annotations
		previewPanel.startParsing();
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
				() -> ANTLRv4PluginController.getInstance(project).getConsoleWindow().show(null)
		);
	}

	public ToolWindow getPreviewWindow() {
		return previewWindow;
	}

	public @NotNull PreviewState getPreviewState(VirtualFile grammarFile) {
		// make sure only one thread tries to add a preview state object for a given file
		String grammarFileName = grammarFile.getPath();
		// Have we seen this grammar before?
		if (grammarToPreviewState != null) {
			PreviewState stateForCurrentGrammar = grammarToPreviewState.get(grammarFileName);
			if ( stateForCurrentGrammar!=null ) {
				return stateForCurrentGrammar; // seen this before
			}
		}
		// not seen, must create state
		PreviewState stateForCurrentGrammar = new PreviewState(project, grammarFile);
		grammarToPreviewState.put(grammarFileName, stateForCurrentGrammar);

		return stateForCurrentGrammar;
	}

	public Editor getEditor(VirtualFile vfile) {
		final FileDocumentManager fdm = FileDocumentManager.getInstance();
		final Document doc = fdm.getDocument(vfile);
		if (doc == null) return null;

		EditorFactory factory = EditorFactory.getInstance();
		final Editor[] editors = factory.getEditors(doc, previewPanel.project);
		if ( editors.length==0 ) {
			// no editor found for this file. likely an out-of-sequence issue
			// where Intellij is opening a project and doesn't fire events
			// in order we'd expect.
			return null;
		}
		return editors[0]; // hope just one
	}


	/** Get the state information associated with the grammar in the current
	 *  editor window. If there is no grammar in the editor window, return null.
	 *  If there is a grammar, return any existing preview state else
	 *  create a new one in store in the map.
	 *
	 *  Too dangerous; turning off but might be useful later.
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
	 */

	// These "get current editor file" routines should only be used
	// when you are sure the user is in control and is viewing the
	// right file (i.e., don't use these during project loading etc...)

	public static VirtualFile getCurrentEditorFile(Project project) {
		FileEditorManager fmgr = FileEditorManager.getInstance(project);
		// "If more than one file is selected (split), the file with most recent focused editor is returned first." from IDE doc on method
		VirtualFile[] files = fmgr.getSelectedFiles();
		if ( files.length == 0 ) {
			return null;
		}
		return files[0];
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
			if ( !projectIsClosed && !ApplicationManager.getApplication().isUnitTestMode()) {
				grammarFileSavedEvent(vfile);
			}
		}
	}

	public class MyFileEditorManagerAdapter implements FileEditorManagerListener {
		@Override
		public void selectionChanged(FileEditorManagerEvent event) {
			if ( !projectIsClosed ) {
				boolean modified = false;

				if (event.getOldEditor() != null) {
					if (event.getOldEditor().isModified()) {
						modified = true;
					} else {
						VirtualFile oldFile = event.getOldEditor().getFile();
						String oldFilePath = oldFile.getPath();
						Long modCount = oldFile.getModificationCount();
						modified = grammarFileMods.containsKey(oldFilePath) &&
								!grammarFileMods.get(oldFilePath).equals(modCount);
					}

				}

				if (modified) {
					PsiDocumentManager psiMgr = PsiDocumentManager.getInstance(project);
					FileDocumentManager docMgr = FileDocumentManager.getInstance();
					Document doc = docMgr.getDocument(event.getOldFile());
					if ( !psiMgr.isCommitted(doc) || docMgr.isDocumentUnsaved(doc) ) {
						psiMgr.commitDocument(doc);
						docMgr.saveDocument(doc);
					}
				}
				currentEditorFileChangedEvent(event.getOldFile(), event.getNewFile(), modified);
			}
		}

		@Override
		public void fileClosed(FileEditorManager source, VirtualFile file) {
			if ( !projectIsClosed && (source != null && source.getSelectedEditor() != null && source.getSelectedEditor().getFile().equals(file)) ) {
				editorFileClosedEvent(file);
			}
		}
	}

}
