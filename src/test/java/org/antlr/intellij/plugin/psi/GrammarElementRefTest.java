package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.usageView.UsageInfo;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.TestUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public class GrammarElementRefTest extends LightPlatformCodeInsightFixtureTestCase {
	public void testFindUsagesOfLexerRule() {
		Collection<UsageInfo> ruleUsages = myFixture.testFindUsages("SimpleGrammar.g4");
		assertEquals(3, ruleUsages.size());
	}

	public void testFindUsagesOfParserRule() {
		Collection<UsageInfo> ruleUsages = myFixture.testFindUsages("SimpleGrammar2.g4");
		assertEquals(1, ruleUsages.size());
	}

	public void testHighlightUsagesOfLexerRule() {
		RangeHighlighter[] usages = myFixture.testHighlightUsages("SimpleGrammar.g4");

		assertEquals(4, usages.length);
	}

	public void testHighlightUsagesOfParserRule() {
		RangeHighlighter[] usages = myFixture.testHighlightUsages("SimpleGrammar2.g4");

		assertEquals(2, usages.length);
	}

	public void testReferenceToLexerRule() {
		myFixture.configureByFiles("SimpleGrammar.g4");

		moveCaret(40);
		assertResolvedMatches(LexerRuleSpecNode.class, element -> assertEquals("DIGIT", element.getName()));

		moveCaret(75);
		assertResolvedMatches(LexerRuleSpecNode.class, element -> assertEquals("TOKEN1", element.getName()));
	}

	public void testReferenceToParserRule() {
		myFixture.configureByFiles("SimpleGrammar.g4");

		moveCaret(95);

		assertResolvedMatches(ParserRuleSpecNode.class, element -> assertEquals("rule1", element.getName()));
	}

	public void testReferenceToLexerRuleFromFragment() {
		myFixture.configureByFiles("FooLexer.g4");

		moveCaret(130);
		assertResolvedMatches(LexerRuleSpecNode.class, element -> assertEquals("TOKEN1", element.getName()));

		moveCaret(115);
		assertResolvedMatches(LexerRuleSpecNode.class, element -> assertEquals("Fragment2", element.getName()));
	}

	public void testReferenceToTokensSpec() {
		myFixture.configureByFiles("FooLexer.g4");

		moveCaret(225);
		assertResolvedMatches(TokenSpecNode.class, element -> {
			assertEquals("STRING", element.getName());
			assertEquals(34, element.getTextOffset());
		});
	}

	public void testReferenceToChannelsSpec() {
		myFixture.configureByFiles("FooLexer.g4");

		moveCaret(245);
		assertResolvedMatches(ChannelSpecNode.class, element -> {
			assertEquals("MYHIDDEN", element.getName());
			assertEquals(54, element.getTextOffset());
		});
	}

	public void testReferencesInModes() {
		myFixture.configureByFiles("Modes.g4");

		moveCaret(85);
		assertResolvedMatches(ModeSpecNode.class, element -> {
			assertEquals("MY_MODE", element.getName());
			assertEquals(98, element.getTextOffset());
		});

		moveCaret(135);
		assertResolvedMatches(LexerRuleSpecNode.class, element -> {
			assertEquals("TOKEN1", element.getName());
			assertEquals(53, element.getTextOffset());
		});

		moveCaret(190);
		assertResolvedMatches(LexerRuleSpecNode.class, element -> {
			assertEquals("TOKEN2", element.getName());
			assertEquals(108, element.getTextOffset());
		});

		moveCaret(204);
		assertResolvedMatches(TokenSpecNode.class, element -> {
			assertEquals("T1", element.getName());
			assertEquals(31, element.getTextOffset());
		});

		moveCaret(217);
		assertResolvedMatches(ChannelSpecNode.class, element -> {
			assertEquals("C1", element.getName());
			assertEquals(47, element.getTextOffset());
		});

		moveCaret(235);
		assertResolvedMatches(ModeSpecNode.class, element -> {
			assertEquals("MY_MODE", element.getName());
			assertEquals(98, element.getTextOffset());
		});
	}

	public void testReferencesFromParserToLexer() {
		myFixture.configureByFiles("FooParser.g4", "FooLexer.g4");

		moveCaret(75);
		assertResolvedMatches(LexerRuleSpecNode.class, element -> {
			assertEquals("TOKEN1", element.getName());
			assertEquals(66, element.getTextOffset());
			assertEquals("FooLexer.g4", element.getContainingFile().getName());
		});

		moveCaret(85);
		assertResolvesToNothing();

		moveCaret(95);
		assertResolvesToNothing();

		moveCaret(100);
		assertResolvedMatches(TokenSpecNode.class, element -> {
			assertEquals("STRING", element.getName());
			assertEquals(34, element.getTextOffset());
			assertEquals("FooLexer.g4", element.getContainingFile().getName());
		});
	}

	public void testReferencesToTokenVocabFile() {
		myFixture.configureByFiles("FooParser.g4", "FooLexer.g4");

		moveCaret(55);
		assertResolvedMatches(ANTLRv4FileRoot.class, file -> assertEquals("FooLexer.g4", file.getName()));
	}

	public void testReferencesToTokenVocabFileString() {
		myFixture.configureByFiles("FooParser2.g4", "FooLexer.g4");

		moveCaret(55);
		assertResolvedMatches(ANTLRv4FileRoot.class, file -> assertEquals("FooLexer.g4", file.getName()));
	}

	public void testReferenceToImportedFile() {
		myFixture.configureByFiles("importing.g4", "imported.g4");

		moveCaret(35);
		assertResolvedMatches(ANTLRv4FileRoot.class, file -> assertEquals("imported.g4", file.getName()));
	}

	public void testReferenceToRuleInImportedFile() {
		myFixture.configureByFiles("importing.g4", "imported.g4", "imported2.g4", "imported3.g4");

		moveCaret(53);
		assertResolvedMatches(LexerRuleSpecNode.class, node -> assertEquals("Foo", node.getName()));

		moveCaret(66);
		assertResolvedMatches(LexerRuleSpecNode.class, node -> {
			assertEquals("Bar", node.getName());
			assertEquals("imported2.g4", node.getContainingFile().getName());
		});

		moveCaret(80);
		assertResolvedMatches(LexerRuleSpecNode.class, node -> {
			assertEquals("Baz", node.getName());
			assertEquals("imported3.g4", node.getContainingFile().getName());
		});
	}

	@Override
	protected void tearDown() throws Exception {
		// This can avoid exceptions
		ANTLRv4PluginController.getInstance(getProject()).getConsole().setOutputPaused(true);

		TestUtils.tearDownIgnoringObjectNotDisposedException(() -> super.tearDown());
	}

	private <T extends PsiElement> void assertResolvedMatches(Class<T> expectedClass, @Nullable Consumer<T> matcher) {
		PsiElement psiElement = resolveRefAtCaret();

		if (psiElement != null) {
			assertEquals(expectedClass, psiElement.getClass());

			if (matcher != null) {
				matcher.accept((T) psiElement);
			}
		} else {
			fail("Reference resolved to nothing");
		}
	}

	private void assertResolvesToNothing() {
		PsiElement psiElement = resolveRefAtCaret();

		if (psiElement != null) {
			fail("Expected element at offset " + myFixture.getCaretOffset() + " to resolve to nothing, but resolved to "
				+ psiElement);
		}
	}

	@Override
	protected String getTestDataPath() {
		return "src/test/resources/references";
	}

	private void moveCaret(int offset) {
		myFixture.getEditor().getCaretModel().moveToOffset(offset);
	}

	@Nullable
	private PsiElement resolveRefAtCaret() {
		PsiElement elementAtCaret = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

		if (elementAtCaret != null) {
			PsiReference ref = elementAtCaret.getReference();

			if (ref != null) {
				return ref.resolve();
			}
			else {
				fail("No reference at caret");
			}
		}
		else {
			fail("No element at caret");
		}

		return null;
	}

}