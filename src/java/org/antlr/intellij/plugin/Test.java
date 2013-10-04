package org.antlr.intellij.plugin;

import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Test {
	public static void main(String[] args) throws IOException {
		ANTLRv4Lexer lexer = new ANTLRv4Lexer(new ANTLRFileStream(args[0]));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		tokens.fill();
//		System.out.println(tokens.getTokens());
		ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
		ParseTree t = parser.grammarSpec();
		System.out.println(t.toStringTree(parser));
	}
}
