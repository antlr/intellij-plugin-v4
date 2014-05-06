package org.antlr.intellij.plugin.parsing;

import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;

public class ParsingResult {
	public Parser parser;
	public ParseTree tree;
	public SyntaxErrorListener syntaxErrorListener;

	public ParsingResult(Parser parser, ParseTree tree, SyntaxErrorListener syntaxErrorListener) {
		this.parser = parser;
		this.tree = tree;
		this.syntaxErrorListener = syntaxErrorListener;
	}
}
