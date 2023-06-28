package org.antlr.intellij.plugin.folding;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.antlr.intellij.plugin.TestUtils;

import java.lang.reflect.Method;

public class ANTLRv4FoldingBuilderTest extends LightPlatformCodeInsightFixtureTestCase {

	public void test_folding_should_not_throw_on_incomplete_prequel() {
		// Given
		myFixture.configureByText("foo.g4", "grammar foo;\n @\n");

		// When
		buildInitialFoldings();

		// Then
		FoldRegion[] allFoldRegions = myFixture.getEditor().getFoldingModel().getAllFoldRegions();
		assertEquals(0, allFoldRegions.length);
	}

	public void test_should_not_fold_single_line() {
		// Given
		myFixture.configureByText("foo.g4", "grammar foo;\n @members { int i; }\n");

		// When
		buildInitialFoldings();

		// Then
		FoldRegion[] allFoldRegions = myFixture.getEditor().getFoldingModel().getAllFoldRegions();
		assertEquals(0, allFoldRegions.length);
	}

	@Override
	protected void tearDown() throws Exception {
		TestUtils.tearDownIgnoringObjectNotDisposedException(() -> super.tearDown());
	}

	private void buildInitialFoldings() {
		try {
			Method method = EditorTestUtil.class.getMethod("buildInitialFoldingsInBackground", Editor.class);
			method.invoke(null, myFixture.getEditor());
		} catch (ReflectiveOperationException e) {
			CodeFoldingManager.getInstance(getProject()).buildInitialFoldings(myFixture.getEditor());
		}
	}
}