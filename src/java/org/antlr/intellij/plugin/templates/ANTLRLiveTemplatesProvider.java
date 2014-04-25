package org.antlr.intellij.plugin.templates;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;
import org.jetbrains.annotations.Nullable;

public class ANTLRLiveTemplatesProvider implements DefaultLiveTemplatesProvider {
	public static final String[] TEMPLATES = {"/liveTemplates/lexer", "/liveTemplates/parser"};

	@Override
	public String[] getDefaultLiveTemplateFiles() {
		return TEMPLATES;
	}

	@Nullable
	@Override
	public String[] getHiddenLiveTemplateFiles() {
		return new String[0];
	}
}
