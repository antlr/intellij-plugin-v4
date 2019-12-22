package org.antlr.intellij.plugin.validation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import org.antlr.intellij.plugin.TestUtils;

public class CreateRuleFixTest extends LightPlatformCodeInsightTestCase {

	public CreateRuleFixTest() {
		myTestDataPath = "src/test/resources/quickfixes/CreateRuleFix/";
	}

	public void testQuickFixDescriptionShouldShowRuleName() {
		// Given
		configureByFile("missingRule.g4");
		CreateRuleFix createRuleFix = new CreateRuleFix(TextRange.create(30, 37), getFile());

		// When
		String description = createRuleFix.getText();

		// Then
		assertEquals("Create rule 'newRule'", description);
	}

	public void testQuickFixShouldCreateRuleAfterCurrentRule() {
		// Given
		configureByFile("missingRule.g4");
		CreateRuleFix createRuleFix = new CreateRuleFix(TextRange.create(30, 37), getFile());

		// When
		ApplicationManager.getApplication().runWriteAction(
				() -> createRuleFix.invoke(getProject(), getEditor(), getFile())
		);

		// Then
		assertEquals(
				"grammar missingRule;\n\n" +
				"myRule: newRule;\n\n" +
				"newRule: ' ';",
				getFile().getText()
		);
	}

	@Override
	protected void tearDown() throws Exception {
		TestUtils.tearDownIgnoringObjectNotDisposedException(() -> super.tearDown());
	}
}
