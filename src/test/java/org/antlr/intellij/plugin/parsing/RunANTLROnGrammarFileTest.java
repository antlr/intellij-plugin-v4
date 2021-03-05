package org.antlr.intellij.plugin.parsing;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.VfsTestUtil;
import org.antlr.intellij.plugin.TestUtils;

import java.util.List;

public class RunANTLROnGrammarFileTest extends LightPlatformCodeInsightTestCase {

	public void testPackageOptionShouldNotBeAddedIfDeclaredInHeader() {
		VirtualFile file = VfsTestUtil.createFile(getSourceRoot(), "mypkg/myGrammarWithHeader.g4",
				"grammar myGrammarWithHeader;\n@header { package com.foo.bar; }\nFOO: 'foo';");
		List<String> options = RunANTLROnGrammarFile.getANTLRArgsAsList(getProject(), file);

		assertFalse(options.contains("-package"));
	}

	public void testPackageOptionShouldBeAddedIfNotDeclaredInHeader() {
		VirtualFile file = VfsTestUtil.createFile(getSourceRoot(), "mypkg/myGrammarWithoutHeader.g4",
				"grammar myGrammarWithoutHeader; FOO: 'foo';");
		List<String> options = RunANTLROnGrammarFile.getANTLRArgsAsList(getProject(), file);

		assertTrue(options.contains("-package"));
		assertTrue(options.contains("mypkg"));
	}

	@Override
	protected void tearDown() throws Exception {
		TestUtils.tearDownIgnoringObjectNotDisposedException(() -> super.tearDown());
	}

}