package org.antlr.intellij.plugin.validation;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Collectors;

public class AddTokenDefinitionFix extends BaseIntentionAction {

    private final TextRange textRange;

    public AddTokenDefinitionFix(TextRange textRange) {
        this.textRange = Objects.requireNonNull(textRange);
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return "ANTLR4";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        appendTokenDefAtLastLine(editor);
    }

    @NotNull
    @Override
    public String getText() {
        return "Add token definition built from letter fragments.";
    }

    private void appendTokenDefAtLastLine(@Nullable("is null when called from inspection") Editor editor) {
        String tokenName = editor.getDocument().getText(textRange);
        int lastLineOffset = editor.getDocument().getLineEndOffset(editor.getDocument().getLineCount() - 1);
        editor.getDocument().insertString(lastLineOffset, "\n" + buildTokenDefinitionText(tokenName));
    }

    @NotNull
    static String buildTokenDefinitionExpressionText(String tokenName) {
        return tokenName.toUpperCase().chars().mapToObj(c -> (char) c).map(c -> getCharacterFragment(c)).collect(Collectors.joining(" "));
    }

    private static String getCharacterFragment(Character c) {
        String fragment = String.valueOf(c);
        if (Character.isLetter(c)) {
            return fragment;
        } else {
            return "'" + fragment + "'";
        }
    }
}
