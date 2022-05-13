package org.antlr.intellij.plugin.actions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.validation.AddTokenDefinitionFix;
import org.antlr.intellij.plugin.validation.CreateRuleFix;
import org.antlr.v4.tool.ErrorType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AnnotationIntentActionsFactory {
    @NotNull
    public static Optional<IntentionAction> getFix(TextRange textRange, ErrorType errorType, PsiFile file) {
        if (errorType == ErrorType.IMPLICIT_TOKEN_DEFINITION) {
            return Optional.of(new AddTokenDefinitionFix(textRange));
        }
        else if ( errorType==ErrorType.UNDEFINED_RULE_REF ) {
            return Optional.of(new CreateRuleFix(textRange, file));
        }
        return Optional.empty();
    }
}
