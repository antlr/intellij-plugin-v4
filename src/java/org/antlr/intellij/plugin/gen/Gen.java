package org.antlr.intellij.plugin.gen;

import org.antlr.v4.Tool;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.io.IOException;
import java.net.URL;

public class Gen {
	static STGroup tokenTypeFile;

	public Gen() {
		tokenTypeFile = new STGroupFile("org/antlr/intellij/plugin/gen/Java.stg");
	}

	public static void main(String[] args) throws IOException {
		Gen gen = new Gen();
		URL url = gen.getClass().getClassLoader().getResource("org/antlr/intellij/plugin/parser/ANTLRv4Lexer.g4");

		Tool antlr = new Tool();
		GrammarRootAST ast = antlr.loadGrammar(url.getFile());
		Grammar g = antlr.createGrammar(ast);
		antlr.process(g, false);
		ST st = getTokenTypeFile(g);
		System.out.println(st.render());
	}

	public static ST getTokenTypeFile(Grammar g) {
		ST st = tokenTypeFile.getInstanceOf("tokenTypeFile");
		//tokenTypeFile(package, grammarName, tokenTypeClassName, commentTokens, whitespaceTokens)
		st.add("package", "org.antlr.intellij.plugin.parser");
		st.add("grammarName", "ANTLRv4");
		st.add("tokenTypeClassName", "ANTLRv4TokenType");
		st.add("tokenNames", g.tokenNameToTypeMap.keySet());
		st.add("commentTokens", "DOC_COMMENT");
		st.add("commentTokens", "BLOCK_COMMENT");
		st.add("commentTokens", "LINE_COMMENT");
		st.add("whitespaceTokens", "WS");
		return st;
	}
}

