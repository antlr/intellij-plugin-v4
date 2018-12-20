package org.antlr.intellij.plugin.templates;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;
import org.jetbrains.annotations.Nullable;

public class ANTLRLiveTemplatesProvider implements DefaultLiveTemplatesProvider {
	// make sure module shows liveTemplates as source dir or whatever dir holds "lexer"
	public static final String[] TEMPLATES = {"liveTemplates/lexer/user"};

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
