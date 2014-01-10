package org.antlr.intellij.plugin;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import org.antlr.intellij.plugin.preview.ParseTreePanel;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.tool.Grammar;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ANTLRv4ProjectComponent implements ProjectComponent {
	public ParseTreePanel treePanel;

	public static ANTLRv4ProjectComponent getInstance(Project project) {
		return project.getComponent(ANTLRv4ProjectComponent.class);
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

	public static Object[] parseText(String inputText,
									 String combinedGrammarFileName,
									 String startRule)
		throws IOException
	{
		final Grammar g = Grammar.load(combinedGrammarFileName);
		LexerInterpreter lexEngine = g.createLexerInterpreter(new ANTLRInputStream(inputText));
		CommonTokenStream tokens = new CommonTokenStream(lexEngine);
		ParserInterpreter parser = g.createParserInterpreter(tokens);
		try {
			ParseTree t = parser.parse(g.getRule(startRule).index);
			System.out.println("parse tree: " + t.toStringTree(parser));
//          ((ParserRuleContext)t).inspect(parser);
			return new Object[] {parser, t};
		}
		catch (RecognitionException re) {
			DefaultErrorStrategy strat = new DefaultErrorStrategy();
			strat.reportError(parser, re);
		}
		return null;
	}

}