package org.antlr.intellij.plugin.actions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.util.TextRange;
import org.antlr.intellij.plugin.validation.AddTokenDefinitionFix;
import org.antlr.v4.tool.ErrorType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AnnotationIntentActionsFactory {
    @NotNull
    public static Optional<IntentionAction> getFix(TextRange textRange, ErrorType errorType) {
        if (errorType == ErrorType.IMPLICIT_TOKEN_DEFINITION){
            return Optional.of(new AddTokenDefinitionFix(textRange));
        }
        return Optional.empty();
    }
}
