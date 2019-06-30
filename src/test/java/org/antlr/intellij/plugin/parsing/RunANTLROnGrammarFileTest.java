package org.antlr.intellij.plugin.parsing;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.VfsTestUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class RunANTLROnGrammarFileTest extends LightPlatformCodeInsightTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

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

}