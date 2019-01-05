package org.antlr.intellij.plugin.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

public class GrammarElementRefTest extends LightCodeInsightFixtureTestCase {

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
		assertResolvedMatches(ANTLRv4FileRoot.class, file -> {
			assertEquals("FooLexer.g4", file.getName());
		});
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			super.tearDown();
		} catch (RuntimeException e) {
			// We don't want to release the editor in the Tool Output tool window, so we ignore
			// ObjectNotDisposedExceptions related to this particular editor
			if (e.getClass().getName().equals("com.intellij.openapi.util.TraceableDisposable.ObjectNotDisposedException")) {
				StringWriter stringWriter = new StringWriter();
				e.printStackTrace(new PrintWriter(stringWriter));
				if (!stringWriter.toString().contains("ANTLRv4PluginController.createToolWindows")) {
					throw e;
				}
			}
		}
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
		PsiElement elementAtCaret = getFile().findElementAt(myFixture.getCaretOffset());

		if (elementAtCaret != null) {
			PsiReference ref = elementAtCaret.getReference();

			if (ref != null) {
				return ref.resolve();
			} else {
				fail("No reference at caret");
			}
		} else {
			fail("No element at caret");
		}

		return null;
	}

}