package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import org.jetbrains.annotations.NotNull;

public class PreviewCompletionContributor extends CompletionContributor {
	@Override
	public void beforeCompletion(@NotNull CompletionInitializationContext context) {
		super.beforeCompletion(context);
	}
}
