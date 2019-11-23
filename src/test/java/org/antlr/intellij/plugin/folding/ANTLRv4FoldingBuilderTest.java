package org.antlr.intellij.plugin.folding;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.antlr.intellij.plugin.TestUtils;

public class ANTLRv4FoldingBuilderTest extends LightPlatformCodeInsightFixtureTestCase {

	public void test_folding_should_not_throw_on_incomplete_prequel() {
		// Given
		myFixture.configureByText("foo.g4", "grammar foo;\n @\n");

		// When
		CodeFoldingManager.getInstance(getProject()).buildInitialFoldings(myFixture.getEditor());

		// Then
		FoldRegion[] allFoldRegions = myFixture.getEditor().getFoldingModel().getAllFoldRegions();
		assertEquals(0, allFoldRegions.length);
	}

	public void test_should_not_fold_single_line() {
		// Given
		myFixture.configureByText("foo.g4", "grammar foo;\n @members { int i; }\n");

		// When
		CodeFoldingManager.getInstance(getProject()).buildInitialFoldings(myFixture.getEditor());

		// Then
		FoldRegion[] allFoldRegions = myFixture.getEditor().getFoldingModel().getAllFoldRegions();
		assertEquals(0, allFoldRegions.length);
	}

	@Override
	protected void tearDown() throws Exception {
		TestUtils.tearDownIgnoringObjectNotDisposedException(() -> super.tearDown());
	}
}