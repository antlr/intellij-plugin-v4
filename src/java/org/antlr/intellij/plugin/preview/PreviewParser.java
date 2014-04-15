package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.parser.AntlrParser;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Arrays;

public class PreviewParser extends AntlrParser<ParserInterpreter> {
	public Project project;
	public int ruleIndex;
	public SyntaxErrorListener errListener;

	public PreviewParser(Project project, int ruleIndex, SyntaxErrorListener errListener) {
		super(PreviewLanguage.INSTANCE);
		this.project = project;
		this.ruleIndex = ruleIndex;
		this.errListener = errListener;
	}

	@Override
	protected ParserInterpreter createParserImpl(TokenStream tokenStream, IElementType root, PsiBuilder builder) {
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState();
		System.out.println("create parse for "+previewState.grammarFileName);
		if ( !previewState.grammarFileName.endsWith(".g4") ) {
			System.err.println("not a grammar!!!!!!");
		}

		ParserInterpreter parser = previewState.g.createParserInterpreter(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(errListener);
		return parser;
	}

	@Override
	protected ParseTree parseImpl(final ParserInterpreter parser, IElementType root, PsiBuilder builder) {
		final ParseTree t = parser.parse(ruleIndex);
		System.out.println("parse tree: " + t.toStringTree(parser));
		String input = builder.getOriginalText().toString();
		if ( !(input.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER) ||
			   input.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)) )
		{
			ApplicationManager.getApplication().invokeLater(
				new Runnable() {
					@Override
					public void run() {
						ANTLRv4PluginController.getInstance(project).getPreviewPanel()
							.setParseTree(Arrays.asList(parser.getRuleNames()), t);
					}
				}
														   );
		}
		return t;
	}
}
