package org.antlr.intellij.plugin.parsing;

import junit.framework.TestCase;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Issue374Test extends TestCase {

	public void test_lexer_rule_should_be_parsed_after_header() {
		// Given
		String grammar = "grammar Sample;\n" +
				"@header {}\n" +
				"WS: [ \\t\\r\\n]+ -> skip ;";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
	}

	public void test_lexer_rule_should_be_parsed_after_options() {
		// Given
		String grammar = "grammar Sample;\n" +
				"options {" +
				" foo = {};" +
				"}\n" +
				"WS: [ \\t\\r\\n]+ -> skip ;";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
	}

	public void test_lexer_rule_should_be_parsed_after_tokens() {
		// Given
		String grammar = "grammar Sample;\n" +
				"tokens {" +
				" foo, bar" +
				"}\n" +
				"WS: [ \\t\\r\\n]+ -> skip ;";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
	}

	public void test_lexer_rule_should_be_parsed_after_channels() {
		// Given
		String grammar = "grammar Sample;\n" +
				"channels {" +
				" foo, bar" +
				"}\n" +
				"WS: [ \\t\\r\\n]+ -> skip ;";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
	}

	public void test_parser_rule_allows_options() {
		// Given
		String grammar = "parser grammar Sample;\n" +
				"options { key = value; }\n" +
				"entry\n" +
				"options { key = value; }\n" +
				": 'text' EOF ;";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		ANTLRv4Lexer lexer = (ANTLRv4Lexer) parser.getInputStream().getTokenSource();
		lexer.addErrorListener(listener);
		parser.addErrorListener(listener);

		// When
		parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
	}

	private ANTLRv4Parser createParser(String grammar) {
		CharStream charStream = CharStreams.fromString(grammar);
		ANTLRv4Lexer lexer = new ANTLRv4Lexer(charStream);
		CommonTokenStream tokenStream = new CommonTokenStream(lexer);

		return new ANTLRv4Parser(tokenStream);
	}
}