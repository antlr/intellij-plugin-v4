package org.antlr.intellij.plugin;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.antlr.intellij.plugin.psi.GrammarElementRef;
import org.antlr.intellij.plugin.psi.GrammarElementRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.jetbrains.annotations.NotNull;

// thought this might help find usages. nope. not invoked. oh, duh. Dmitry
// says: "PsiReferenceContributor is supposed to be used for injecting references
// into languages that you don't control (Java, XML etc.), or if you want other
// people to be able to inject references into your language."
public class ANTLRv4ReferenceContributor extends PsiReferenceContributor {
	@Override
	public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
		PsiReferenceProvider provider = new PsiReferenceProvider() {
			@NotNull
			@Override
			public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
														 @NotNull ProcessingContext context)
			{
				ParserRuleRefNode ruleRef = (ParserRuleRefNode)element;
				GrammarElementRef ref = new GrammarElementRef((GrammarElementRefNode)element, ruleRef.getText());
				return new PsiReference[]{ref};
			}
		};
		registrar.registerReferenceProvider(PlatformPatterns.psiElement(ParserRuleRefNode.class),
											provider);

		provider = new PsiReferenceProvider() {
			@NotNull
			@Override
			public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
														 @NotNull ProcessingContext context)
			{
				LexerRuleRefNode ruleRef = (LexerRuleRefNode)element;
				GrammarElementRef ref = new GrammarElementRef((GrammarElementRefNode)element, ruleRef.getText());
				return new PsiReference[]{ref};
			}
		};
		registrar.registerReferenceProvider(PlatformPatterns.psiElement(LexerRuleRefNode.class),
											provider);
	}
}
