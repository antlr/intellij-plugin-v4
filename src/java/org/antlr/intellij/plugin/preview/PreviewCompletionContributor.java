package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import org.jetbrains.annotations.NotNull;

/** Avoid dummy ID that intellij always puts in; messes up ANTLR parse tree view. */
public class PreviewCompletionContributor extends CompletionContributor {
	/** appears as space but I don't flip to a dot like real space char in parse tree */
	public static final char DUMMY_IDENTIFIER = '\u001F';

	@Override
	public void beforeCompletion(@NotNull CompletionInitializationContext context) {
		context.setDummyIdentifier(String.valueOf(DUMMY_IDENTIFIER));
	}
}
