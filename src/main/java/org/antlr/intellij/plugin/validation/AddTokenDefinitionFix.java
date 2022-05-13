package org.antlr.intellij.plugin.validation;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
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
        appendTokenDefAtLastLine(editor, file, project);
    }

    @NotNull
    @Override
    public String getText() {
        return "Add token definition built from letter fragments.";
    }

    private void appendTokenDefAtLastLine(@Nullable("is null when called from inspection") Editor editor, PsiFile file, Project project) {
        String tokenName = editor.getDocument().getText(textRange);
        String tokenDefLeftSide = tokenName + " : ";
        String tokenDefinitionExpression = buildTokenDefinitionExpressionText(tokenName);

        writeTokenDef(editor, project, tokenDefLeftSide + tokenDefinitionExpression + ";");

        editor.getScrollingModel().scrollTo(new LogicalPosition(editor.getDocument().getLineCount() - 1, 0), ScrollType.MAKE_VISIBLE);

        int newLastLineStart = editor.getDocument().getLineStartOffset(editor.getDocument().getLineCount() - 1);
        editor.getCaretModel().moveToOffset(newLastLineStart);
        runTemplate(editor, project, tokenDefLeftSide, tokenDefinitionExpression, getRefreshedFile(editor, file, project), newLastLineStart);
    }

    private void runTemplate(@NotNull Editor editor, Project project, String tokenDefLeftSide, String tokenDefinitionExpression, PsiFile psiFile, int newLastLineStart) {
        PsiElement elementAt = Objects.requireNonNull(psiFile.findElementAt(newLastLineStart), "Unable to find element at position " + newLastLineStart).getParent();
        Template template = buildTemplate(tokenDefinitionExpression, elementAt, tokenDefLeftSide.length());
        TemplateManager.getInstance(project).startTemplate(editor, template);
    }

    private PsiFile getRefreshedFile(@NotNull Editor editor, PsiFile file, Project project) {
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        return Objects.requireNonNull(psiDocumentManager.getPsiFile(editor.getDocument()), "Unable to resolve file (" + file.getName() + ") for document.");
    }

    private void writeTokenDef(@NotNull Editor editor, Project project, String tokenDefinition) {
        int lastLineOffset = editor.getDocument().getLineEndOffset(editor.getDocument().getLineCount() - 1);
        editor.getDocument().insertString(lastLineOffset, "\n" + tokenDefinition);
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
    }

    private Template buildTemplate(String tokenDefinitionExpression, PsiElement elementAt, int tokenExprTextOffset) {
        TemplateBuilderImpl templateBuilder = new TemplateBuilderImpl(elementAt);
        templateBuilder.replaceRange(getRange(elementAt, tokenExprTextOffset), tokenDefinitionExpression);
        return templateBuilder.buildInlineTemplate();
    }

    static TextRange getRange(PsiElement elementAt, int tokenExprTextOffset) {
        return new TextRange(tokenExprTextOffset, elementAt.getTextLength() - 1);
    }

    @NotNull
    static String buildTokenDefinitionExpressionText(String tokenName) {
        return tokenName.toUpperCase().chars().mapToObj(c -> (char) c).map(AddTokenDefinitionFix::getCharacterFragment).collect(Collectors.joining(" "));
    }

    private static String getCharacterFragment(Character c) {
        String fragment = String.valueOf(c);
        if (Character.isLetter(c)) {
            return fragment;
        }
        else {
            return "'" + fragment + "'";
        }
    }
}
