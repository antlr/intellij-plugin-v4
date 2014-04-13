package org.antlr.intellij.plugin.preview;

import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.parser.AntlrParser;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.tool.Grammar;

public class PreviewParser extends AntlrParser<ParserInterpreter> {
	public Project project;
	public int ruleIndex;
	public SyntaxErrorListener errListener = new SyntaxErrorListener();

	public PreviewParser(Project project, int ruleIndex) {
		super(PreviewLanguage.INSTANCE);
		this.project = project;
		this.ruleIndex = ruleIndex;
	}

	@Override
	protected ParserInterpreter createParserImpl(TokenStream tokenStream, IElementType root, PsiBuilder builder) {
		String grammarFileName = ANTLRv4ProjectComponent.getInstance(project).getGrammarFileName();
		System.out.println("create parse for "+grammarFileName);
		if ( !grammarFileName.endsWith(".g4") ) {
			System.err.println("not a grammar!!!!!!");
		}

		Grammar[] grammars = ANTLRv4ProjectComponent.loadGrammars(grammarFileName);
		Grammar lg = grammars[0];
		Grammar g = grammars[1];

		ParserInterpreter parser = g.createParserInterpreter(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(errListener);
		return parser;
	}

	@Override
	protected ParseTree parseImpl(ParserInterpreter parser, IElementType root, PsiBuilder builder) {
		ParseTree t = parser.parse(ruleIndex);
		if ( errListener.getSyntaxErrors().size()>0 ) {
			System.err.println("errors="+errListener.getSyntaxErrors());
		}
		System.out.println("parse tree: "+t.toStringTree(parser));
		return t;
	}
}
