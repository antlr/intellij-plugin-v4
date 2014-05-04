package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.Tool;
import org.antlr.v4.analysis.AnalysisPipeline;
import org.antlr.v4.automata.ATNFactory;
import org.antlr.v4.automata.LexerATNFactory;
import org.antlr.v4.codegen.CodeGenPipeline;
import org.antlr.v4.semantics.SemanticPipeline;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;

public class PluginANTLRTool extends Tool {
	public PluginANTLRTool() {
		super();
	}

	public PluginANTLRTool(String[] args) {
		super(args);
	}

	/** Cut/paste of super to force PluginParserATNFactory */
	@Override
	public void processNonCombinedGrammar(Grammar g, boolean gencode) {
		if ( g.ast==null || g.ast.hasErrors ) return;
		if ( internalOption_PrintGrammarTree ) System.out.println(g.ast.toStringTree());

		boolean ruleFail = checkForRuleIssues(g);
		if ( ruleFail ) return;

		int prevErrors = errMgr.getNumErrors();
		// MAKE SURE GRAMMAR IS SEMANTICALLY CORRECT (FILL IN GRAMMAR OBJECT)
		SemanticPipeline sem = new SemanticPipeline(g);
		sem.process();

		if ( errMgr.getNumErrors()>prevErrors ) return;

		// BUILD ATN FROM AST
		ATNFactory factory;
		if ( g.isLexer() ) factory = new LexerATNFactory((LexerGrammar)g);
		else factory = new PluginParserATNFactory(g);
		g.atn = factory.createATN();

		if ( generate_ATN_dot ) generateATNs(g);

		// PERFORM GRAMMAR ANALYSIS ON ATN: BUILD DECISION DFAs
		AnalysisPipeline anal = new AnalysisPipeline(g);
		anal.process();

		//if ( generate_DFA_dot ) generateDFAs(g);

		if ( g.tool.getNumErrors()>prevErrors ) return;

		// GENERATE CODE
		if ( gencode ) {
			CodeGenPipeline gen = new CodeGenPipeline(g);
			gen.process();
		}
	}
}
