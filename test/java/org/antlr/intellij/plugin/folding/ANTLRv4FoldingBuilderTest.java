package org.antlr.intellij.plugin.folding;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import java.io.File;

public class ANTLRv4FoldingBuilderTest extends LightPlatformCodeInsightFixtureTestCase {

   // @Test
    public void testANTLRv4Lexer() throws Exception {
        doTest();
    }

    public void testANTLRv4Parser() throws Exception {
        doTest();
    }

    private void doTest(){
        myFixture.testFoldingWithCollapseStatus(getTestDataPath() + "/" + getTestName(false) + ".folding.g4");
    }

    @Override
    protected boolean isCommunity() {
        return true;
    }

    @Override
    protected String getTestDataPath() {
        File f = new File("test/testData/grammars/folding/").getAbsoluteFile();
        assertTrue(f.exists());
        return f.getAbsolutePath();
    }
}