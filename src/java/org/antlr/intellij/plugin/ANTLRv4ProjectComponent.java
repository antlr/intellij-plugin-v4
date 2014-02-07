package org.antlr.intellij.plugin;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.preview.ParseTreePanel;
import org.antlr.v4.Tool;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.DefaultToolListener;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import javax.swing.*;
import java.io.IOException;

public class ANTLRv4ProjectComponent implements ProjectComponent {
	public ParseTreePanel treePanel;
	public Project project;

	public ANTLRv4ProjectComponent(Project project) {
		this.project = project;
	}

	public static ANTLRv4ProjectComponent getInstance(Project project) {
		ANTLRv4ProjectComponent pc = project.getComponent(ANTLRv4ProjectComponent.class);
		return pc;
	}

	public static Project getProjectForFile(VirtualFile virtualFile) {
		Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
		Project project = null;
		for (int i = 0; i < openProjects.length; i++) {
			Project p = openProjects[i];
			ProjectFileIndex fileIndex = ProjectRootManager.getInstance(p).getFileIndex();
			if ( fileIndex.isInContent(virtualFile) ) {
				project = p;
			}
		}
		return project;
	}

	public ParseTreePanel getViewerPanel() {
		return treePanel;
	}

	// -------------------------------------

	@Override
	public void initComponent() {
	}

	@Override
	public void projectOpened() {
		treePanel = new ParseTreePanel();
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

//	private ToolWindow getToolWindow()
//	{
//		ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(_project);
//		ToolWindow toolWindow = toolWindowManager.getToolWindow(ParseTreeWindowFactory.ID);
//		if ( toolWindow!=null ) {
//			return toolWindow;
//		}
//		else {
//			return toolWindowManager.registerToolWindow(ID_TOOL_WINDOW,
//														_viewerPanel,
//														ToolWindowAnchor.RIGHT);
//		}
//	}
//
//	private boolean isToolWindowRegistered()
//	{
//		return ToolWindowManager.getInstance(_project).getToolWindow(ID_TOOL_WINDOW) != null;
//	}

	public static Object[] parseText(ParseTreePanel parseTreePanel,
									 String inputText,
									 String grammarFileName,
									 String startRule)
		throws IOException
	{
		Tool antlr = new Tool();
		antlr.errMgr = new PluginIgnoreMissingTokensFileErrorManager(antlr);
		antlr.errMgr.setFormat("antlr");
		MyANTLRToolListener listener = new MyANTLRToolListener(antlr);
		antlr.addListener(listener);

		String combinedGrammarFileName = null;
		String lexerGrammarFileName = null;
		String parserGrammarFileName = null;
		if ( grammarFileName.contains("Lexer") ) {
			lexerGrammarFileName = grammarFileName;
			int i = grammarFileName.indexOf("Lexer");
			parserGrammarFileName = grammarFileName.substring(0,i)+"Parser.g4";
		}
		else if ( grammarFileName.contains("Parser") ) {
			parserGrammarFileName = grammarFileName;
			int i = grammarFileName.indexOf("Parser");
			lexerGrammarFileName = grammarFileName.substring(0,i)+"Lexer.g4";
		}
		else {
			combinedGrammarFileName = grammarFileName;
		}

		ANTLRInputStream input = new ANTLRInputStream(inputText);
		LexerInterpreter lexEngine;
		Grammar g;
		if ( combinedGrammarFileName!=null ) {
			g = antlr.loadGrammar(grammarFileName);
			if ( listener.grammarErrorMessage!=null ) {
				return null;
			}
			lexEngine = g.createLexerInterpreter(input);
		}
		else {
			LexerGrammar lg = null;
			try {
				lg = (LexerGrammar)Grammar.load(lexerGrammarFileName);
			}
			catch (ClassCastException cce) {
				System.err.println("File "+lexerGrammarFileName+" isn't a lexer grammar");
			}
			if ( listener.grammarErrorMessage!=null ) {
				return null;
			}
			g = loadGrammar(antlr, parserGrammarFileName, lg);
			lexEngine = lg.createLexerInterpreter(input);
		}

		final JTextArea console = parseTreePanel.getConsole();
		final MyConsoleErrorListener syntaxErrorListener = new MyConsoleErrorListener();
		Object[] result = new Object[2];

		CommonTokenStream tokens = new CommonTokenStream(lexEngine);
		ParserInterpreter parser = g.createParserInterpreter(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(syntaxErrorListener);
		ParseTree t = parser.parse(g.getRule(startRule).index);

		// this loop works around a bug in ANTLR 4.2
		// https://github.com/antlr/antlr4/issues/461
		// https://github.com/antlr/intellij-plugin-v4/issues/23
		while (t.getParent() != null) {
			t = t.getParent();
		}

		console.setText(syntaxErrorListener.syntaxError);
		if ( t!=null ) {
			return new Object[] {parser, t};
		}
		return null;
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
		public String syntaxError="";
		@Override
		public void syntaxError(Recognizer<?, ?> recognizer,
								@Nullable Object offendingSymbol,
								int line, int charPositionInLine, String msg,
								@Nullable RecognitionException e)
		{
//			super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
			syntaxError = "line " + line + ":" + charPositionInLine + " " + msg;
		}
	}
}