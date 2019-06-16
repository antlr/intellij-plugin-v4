package org.antlr.intellij.plugin.validation;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Collectors;

class AddTokenDefinitionFix extends LocalQuickFixAndIntentionActionOnPsiElement {

    private final LexerRuleRefNode element;

    AddTokenDefinitionFix(LexerRuleRefNode element) {
        super(element);
        this.element = Objects.requireNonNull(element);
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return "ANTLR4";
    }

    @NotNull
    @Override
    public String getText() {
        return "Add token definition.";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @Nullable("is null when called from inspection") Editor editor, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        appendTokenDefAtLastLine(editor);
    }

    private void appendTokenDefAtLastLine(@Nullable("is null when called from inspection") Editor editor) {
        String tokenName = element.getReference().getCanonicalText();
        int lastLineOffset = editor.getDocument().getLineEndOffset(editor.getDocument().getLineCount() - 1);
        editor.getDocument().insertString(lastLineOffset, "\n" + buildTokenDefinitionText(tokenName));
    }

    @NotNull
    static String buildTokenDefinitionText(String tokenName) {
        String tokenDefRule = tokenName.toUpperCase().chars().mapToObj(c -> (char) c).map(String::valueOf).collect(Collectors.joining(" "));
        return tokenName + " : " + tokenDefRule + ";";
    }
}
