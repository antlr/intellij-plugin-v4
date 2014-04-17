package org.antlr.intellij.plugin;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
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
import org.antlr.intellij.plugin.actions.RunANTLROnGrammarFile;
import org.antlr.intellij.plugin.preview.PreviewPanel;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.v4.Tool;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.DefaultToolListener;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
	public static final Logger LOG = Logger.getInstance("org.antlr.intellij.plugin.ANTLRv4ProjectComponent");
	public static final String PREVIEW_WINDOW_ID = "ANTLR Preview";
	public static final String CONSOLE_WINDOW_ID = "ANTLR Tool Output";

	private final Object previewStateLock = new Object();

	public Project project;
	public ConsoleView console;
	public ToolWindow consoleWindow;

	public String grammarFileName;		// Indicates the current grammar editor window
	public Map<String, PreviewState> grammarToPreviewState =
		Collections.synchronizedMap(new HashMap<String, PreviewState>());
	public ToolWindow previewWindow;	// same for all grammar editor
	public PreviewPanel previewPanel;	// same for all grammar editor

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
		ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);

		previewPanel = new PreviewPanel(project);

		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
		Content content = contentFactory.createContent(previewPanel, "", false);

		previewWindow = toolWindowManager.registerToolWindow(PREVIEW_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
		previewWindow.getContentManager().addContent(content);

		TextConsoleBuilderFactory factory = TextConsoleBuilderFactory.getInstance();
		TextConsoleBuilder consoleBuilder = factory.createBuilder(project);
		this.console = consoleBuilder.getConsole();

		JComponent consoleComponent = console.getComponent();
		content = contentFactory.createContent(consoleComponent, "", false);

		consoleWindow = toolWindowManager.registerToolWindow(CONSOLE_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
		consoleWindow.getContentManager().addContent(content);
	}

	@Override
	public void projectClosed() {
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
		// Listen for .g4 file saves
		VirtualFileManager.getInstance().addVirtualFileListener(
			new VirtualFileAdapter() {
				@Override
				public void contentsChanged(VirtualFileEvent event) {
					final VirtualFile vfile = event.getFile();
					if ( !vfile.getName().endsWith(".g4") ) return;
					grammarFileSavedEvent(vfile);
				}
		});

		// Listen for editor window changes
		MessageBusConnection msgBus = project.getMessageBus().connect(project);
		msgBus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
						 new FileEditorManagerAdapter() {
							 @Override
							 public void selectionChanged(FileEditorManagerEvent event) {
								 final VirtualFile vfile = event.getNewFile();
								 if ( vfile==null || !vfile.getName().endsWith(".g4") ) return;
								 grammarFileChangedEvent(event.getOldFile(), event.getNewFile());
							 }
						 });
	}

	/** The test ANTLR rule action triggers this event. This can occur
	 *  only occur when the current editor the showing a grammar, because
	 *  that is the only time that the action is enabled. We will see
	 *  a file changed event when the project loads the first grammar file.
	 */
	public void setStartRuleNameEvent(String startRuleName) {
		System.out.println("setStartRuleNameEvent "+startRuleName);
		PreviewState previewState = grammarToPreviewState.get(grammarFileName);
		assert previewState!=null;
//		if ( previewState==null ) {
//			// If the project opens looking at a grammar, we need to simulate a
//			// grammar save event
//			grammarFileSavedEvent(getCurrentEditorFile(project));
//		}
		assert previewState!=null;
		previewState.startRuleName = startRuleName;
		previewPanel.setStartRuleName(startRuleName); // notify the view
	}

	public void grammarFileSavedEvent(VirtualFile vfile) {
		System.out.println("grammarFileSavedEvent "+vfile.getName());
		updateGrammar(vfile.getPath());
		if ( previewPanel!=null ) {
			previewPanel.grammarFileSaved(vfile);
		}
		//runANTLRTool(vfile);
	}

	public void grammarFileChangedEvent(VirtualFile oldFile, VirtualFile newFile) {
		System.out.println("grammarFileChangedEvent "+newFile.getName());
		switchToGrammar(newFile.getPath());
		if ( previewPanel!=null ) {
			previewPanel.grammarFileChanged(oldFile, newFile);
		}
	}

	public void switchToGrammar(String grammarFileName) {
		synchronized ( previewStateLock ) { // build atomically
			this.grammarFileName = grammarFileName; // switch to grammarFileName
			PreviewState previewState = grammarToPreviewState.get(grammarFileName);
			if (previewState != null) {
				// we have already seen this grammar; state object already built
				return;
			}
			previewState = new PreviewState();
			grammarToPreviewState.put(grammarFileName, previewState);
			previewState.grammarFileName = grammarFileName;
			/* run later */ Grammar[] grammars = ANTLRv4PluginController.loadGrammars(grammarFileName);
			previewState.lg = grammars[0];
			previewState.g = grammars[1];
		}
	}

	/** Look for state information concerning this grammar file and update
	 *  the Grammar objects.  This does not necessarily update the grammar file
	 *  in the current editor window, so do not switch to this grammar file.
	 *  Either we are already looking at this grammar or we will get a
	 *  grammar file changed event.
	 */
	public void updateGrammar(String grammarFileName) {
		synchronized ( previewStateLock ) { // build atomically
			PreviewState previewState = grammarToPreviewState.get(grammarFileName);
			if (previewState == null) {
				// if we have not seen this grammar before, we create
				// a new state object and update that
				previewState = new PreviewState();
				grammarToPreviewState.put(grammarFileName, previewState);
				previewState.grammarFileName = grammarFileName;
			}
			// we have the previous or a properly constructed new state object;
			// update grammars.
			Grammar[] grammars = ANTLRv4PluginController.loadGrammars(grammarFileName);
			previewState.lg = grammars[0];
			previewState.g = grammars[1];
		}
	}

	public void runANTLRTool(final VirtualFile vfile) {
		ApplicationManager.getApplication().invokeLater( // todo runReadAction?
			new Runnable() {
				@Override
				public void run() {
					String title = "ANTLR Code Generation";
					boolean canBeCancelled = true;
					Task.Backgroundable gen =
						new RunANTLROnGrammarFile(new VirtualFile[]{vfile},
												  project,
												  title,
												  canBeCancelled,
												  new BackgroundFromStartOption());
					ProgressManager.getInstance().run(gen);
				}
			}
	   );
	}

	public Object[] parseText(String inputText) throws IOException {
		PreviewState previewState = getPreviewState();
		String grammarFileName = previewState.grammarFileName;
		if (!new File(grammarFileName).exists()) {
			return null;
		}

		previewPanel.clearParseErrors();

		ANTLRInputStream input = new ANTLRInputStream(inputText);
		LexerInterpreter lexEngine;
		lexEngine = previewState.lg.createLexerInterpreter(input);


		CommonTokenStream tokens = new CommonTokenStream(lexEngine);
		ParserInterpreter parser = previewState.g.createParserInterpreter(tokens);

		MyConsoleErrorListener syntaxErrorListener = new MyConsoleErrorListener();
		parser.removeErrorListeners();
		parser.addErrorListener(syntaxErrorListener);
		lexEngine.removeErrorListeners();
		lexEngine.addErrorListener(syntaxErrorListener);

		Rule start = previewState.g.getRule(previewState.startRuleName);
		if ( start==null ) {
			return null; // can't find start rule
		}
		ParseTree t = parser.parse(start.index);

		previewPanel.parseError(
			Utils.join(syntaxErrorListener.syntaxErrors.iterator(), "\n")
		);

		if ( t!=null ) {
			return new Object[] {parser, t};
		}
		return null;
	}

	/** Get lexer and parser grammars */
	public static Grammar[] loadGrammars(String grammarFileName) {
		Tool antlr = new Tool();
		antlr.errMgr = new PluginIgnoreMissingTokensFileErrorManager(antlr);
		antlr.errMgr.setFormat("antlr");
		MyANTLRToolListener listener = new MyANTLRToolListener(antlr);
		antlr.addListener(listener);

		String combinedGrammarFileName = null;
		String lexerGrammarFileName = null;
		String parserGrammarFileName = null;

		Grammar g = antlr.loadGrammar(grammarFileName); // load to examine it
		// examine's Grammar AST from v4 itself;
		// hence use ANTLRParser.X not ANTLRv4Parser from this plugin
		switch ( g.getType() ) {
			case ANTLRParser.PARSER :
				parserGrammarFileName = grammarFileName;
				int i = grammarFileName.indexOf("Parser");
				if ( i>=0 ) {
					lexerGrammarFileName = grammarFileName.substring(0, i) + "Lexer.g4";
				}
				break;
			case ANTLRParser.LEXER :
				lexerGrammarFileName = grammarFileName;
				int i2 = grammarFileName.indexOf("Lexer");
				if ( i2>=0 ) {
					parserGrammarFileName = grammarFileName.substring(0, i2) + "Parser.g4";
				}
				break;
			case ANTLRParser.COMBINED :
				combinedGrammarFileName = grammarFileName;
				lexerGrammarFileName = grammarFileName+"Lexer";
				parserGrammarFileName = grammarFileName+"Parser";
				break;
		}

		if ( lexerGrammarFileName==null ) {
			LOG.error("Can't compute lexer file name from "+grammarFileName, (Throwable)null);
			return null;
		}
		if ( parserGrammarFileName==null ) {
			LOG.error("Can't compute parser file name from "+grammarFileName, (Throwable)null);
			return null;
		}

		LexerGrammar lg = null;

		if ( combinedGrammarFileName!=null ) {
			// already loaded above
			lg = g.getImplicitLexer();
			if ( listener.grammarErrorMessage!=null ) {
				g = null;
			}
		}
		else {
			try {
				lg = (LexerGrammar)Grammar.load(lexerGrammarFileName);
			}
			catch (ClassCastException cce) {
				LOG.error("File " + lexerGrammarFileName + " isn't a lexer grammar", cce);
				lg = null;
			}
			if ( listener.grammarErrorMessage!=null ) {
				lg = null;
			}
			g = loadGrammar(antlr, parserGrammarFileName, lg);
		}

		return new Grammar[] {lg, g};
	}

	/** Same as loadGrammar(fileName) except import vocab from existing lexer */
	public static Grammar loadGrammar(Tool tool, String fileName, LexerGrammar lexerGrammar) {
		GrammarRootAST grammarRootAST = tool.parseGrammar(fileName);
		final Grammar g = tool.createGrammar(grammarRootAST);
		g.fileName = fileName;
		g.importVocab(lexerGrammar);
		tool.process(g, false);
		return g;
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

	public ToolWindow getPreviewWindow() {
		return previewWindow;
	}

	public String getInputText() {
		if ( previewPanel==null ) return "";
		return getPreviewState().editor.getDocument().getText();
	}

	public PreviewState getPreviewState() {
		if ( grammarFileName!=null ) {
			return grammarToPreviewState.get(grammarFileName);
		}
		return null;
	}

	public static VirtualFile getCurrentEditorFile(Project project) {
		FileEditorManager fmgr = FileEditorManager.getInstance(project);
		VirtualFile files[] = fmgr.getSelectedFiles();
		return files[0];
	}

	static class MyANTLRToolListener extends DefaultToolListener {
		public String grammarErrorMessage;
		public MyANTLRToolListener(Tool tool) { super(tool); }

		@Override
		public void error(ANTLRMessage msg) {
//			super.error(msg);
			ST msgST = tool.errMgr.getMessageTemplate(msg);
			grammarErrorMessage = msgST.render();
			if (tool.errMgr.formatWantsSingleLineMessage()) {
				grammarErrorMessage = grammarErrorMessage.replace('\n', ' ');
			}
		}
	}

	/** Traps parser interpreter syntax errors */
	static class MyConsoleErrorListener extends ConsoleErrorListener {
		public List<String> syntaxErrors = new ArrayList<String>();
		@Override
		public void syntaxError(Recognizer<?, ?> recognizer,
								@Nullable Object offendingSymbol,
								int line, int charPositionInLine, String msg,
								@Nullable RecognitionException e)
		{
//			super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
			syntaxErrors.add("line " + line + ":" + charPositionInLine + " " + msg);
		}
	}
}
