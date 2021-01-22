package org.antlr.intellij.plugin.parsing;

import com.intellij.openapi.util.io.StreamUtil;
import junit.framework.TestCase;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Issue403Test extends TestCase {

	public void test_options_should_not_be_recognized_as_keyword_in_rule() {
		// Given
		String grammar = "grammar Sample;\n" +
				"statement: (OPTIONS options=tablePropertyList);";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
	}

	public void test_options_block_without_spaces() {
		// Given
		String grammar = "grammar Sample;\n" +
				"options{}";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		final ANTLRv4Parser.GrammarSpecContext ctx = parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
		assertNotNull(ctx.prequelConstruct(0).optionsSpec());
	}

	public void test_options_block_with_space() {
		// Given
		String grammar = "grammar Sample;\n" +
				"options {}";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		final ANTLRv4Parser.GrammarSpecContext ctx = parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
		assertNotNull(ctx.prequelConstruct(0).optionsSpec());
	}

	public void test_options_block_with_tab() {
		// Given
		String grammar = "grammar Sample;\n" +
				"options \t\r\n{}";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		final ANTLRv4Parser.GrammarSpecContext ctx = parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
		assertNotNull(ctx.prequelConstruct(0).optionsSpec());
	}

	public void test_tokens_block_with_tab() {
		// Given
		String grammar = "grammar Sample;\n" +
				"tokens \t\r\n{}";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		final ANTLRv4Parser.GrammarSpecContext ctx = parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
		assertNotNull(ctx.prequelConstruct(0).tokensSpec());
	}

	public void test_channels_block_with_tab() {
		// Given
		String grammar = "grammar Sample;\n" +
				"channels \t\r\n{}";

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.addErrorListener(listener);

		// When
		final ANTLRv4Parser.GrammarSpecContext ctx = parser.grammarSpec();

		// Then
		assertTrue(listener.getSyntaxErrors().isEmpty());
		assertNotNull(ctx.prequelConstruct(0).channelsSpec());
	}

	public void test_SqlBase() throws IOException {
		// Given
		String grammar = StreamUtil.readText(getClass().getResourceAsStream("/parser/SqlBase.g4"), StandardCharsets.UTF_8);

		ANTLRv4Parser parser = createParser(grammar);

		SyntaxErrorListener listener = new SyntaxErrorListener();
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