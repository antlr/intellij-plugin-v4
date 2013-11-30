package org.antlr.intellij.plugin.gen;

import org.antlr.v4.Tool;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

public class Gen {
	static STGroup tokenTypeFile;

	public Gen() {
		tokenTypeFile = new STGroupFile("org/antlr/intellij/plugin/gen/Java.stg");
	}

	public static void main(String[] args) throws IOException {
		Gen gen = new Gen();
		URL lexerurl = gen.getClass().getClassLoader().getResource("org/antlr/intellij/plugin/parser/ANTLRv4Lexer.g4");
		URL parserurl = gen.getClass().getClassLoader().getResource("org/antlr/intellij/plugin/parser/ANTLRv4Parser.g4");

		Tool antlr = new Tool();
		GrammarRootAST ast = antlr.loadGrammar(lexerurl.getFile());
		LexerGrammar lg = (LexerGrammar)antlr.createGrammar(ast);
		antlr.process(lg, false);

		ast = antlr.loadGrammar(parserurl.getFile());
		Grammar g = antlr.createGrammar(ast);
		antlr.process(g, false);

		ST st = getTokenTypeFile(lg, g);
		System.out.println(st.render(80));
	}

	public static ST getTokenTypeFile(LexerGrammar lg, Grammar g) {
		ST st = tokenTypeFile.getInstanceOf("tokenTypeFile");
		//tokenTypeFile(package, grammarName, tokenTypeClassName, commentTokens, whitespaceTokens)
		st.add("package", "org.antlr.intellij.plugin.parser");
		st.add("grammarName", "ANTLRv4");
		st.add("tokenTypeClassName", "ANTLRv4TokenType");
		Set<String> tokenNames = lg.tokenNameToTypeMap.keySet();
		tokenNames.remove("EOF");
		st.add("tokenNames", tokenNames);
		st.add("ruleNames", g.getRuleNames());
		st.add("commentTokens", "DOC_COMMENT");
		st.add("commentTokens", "BLOCK_COMMENT");
		st.add("commentTokens", "LINE_COMMENT");
		st.add("whitespaceTokens", "WS");
		for (String lit : lg.getStringLiterals()) {
			if ( lit.matches("'[a-zA-Z_0-9]+'") ) {
				int ttype = lg.getTokenType(lit);
				String tokenDisplayName = lg.getTokenNames()[ttype];
				st.add("keywords", tokenDisplayName);
			}
		}
		return st;
	}
}

