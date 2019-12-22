package org.antlr.intellij.plugin.validation;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TextExpression;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.psi.RuleSpecNode;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.getTokenElementType;
import static org.antlr.intellij.plugin.parser.ANTLRv4Lexer.SEMI;

/**
 * A quick fix to create missing rules.
 */
public class CreateRuleFix extends BaseIntentionAction {

	private final TextRange textRange;
	private final String ruleName;

	public CreateRuleFix(TextRange textRange, PsiFile file) {
		this.textRange = textRange;
		ruleName = textRange.substring(file.getText());
	}

	@Nls(capitalization = Nls.Capitalization.Sentence)
	@NotNull
	@Override
	public String getFamilyName() {
		return "ANTLR4";
	}

	@Nls(capitalization = Nls.Capitalization.Sentence)
	@NotNull
	@Override
	public String getText() {
		return "Create rule '" + ruleName + "'";
	}

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
		return true;
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
		String ruleName = editor.getDocument().getText(textRange);

		prepareEditor(project, editor, file);

		Template template = TemplateManager.getInstance(project).createTemplate("", "");
		template.addTextSegment(ruleName + ": ");
		template.addVariable("CONTENT", new TextExpression("' '"), true);
		template.addTextSegment(";");

		TemplateManager.getInstance(project).startTemplate(editor, template);
	}

	private void prepareEditor(@NotNull Project project, Editor editor, PsiFile file) {
		int insertionPoint = findInsertionPoint(editor, file);
		editor.getDocument().insertString(insertionPoint, "\n\n");

		PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

		editor.getCaretModel().moveToOffset(insertionPoint + 2);
	}

	private int findInsertionPoint(Editor editor, PsiFile file) {
		PsiElement atRange = file.findElementAt(textRange.getEndOffset());
		if ( atRange!=null ) {
			RuleSpecNode parentRule = PsiTreeUtil.getParentOfType(atRange, RuleSpecNode.class);

			if ( parentRule!=null ) {
				PsiElement semi = MyPsiUtils.findFirstChildOfType(parentRule, getTokenElementType(SEMI));

				if ( semi!=null ) {
					return semi.getTextOffset() + 1;
				}
				return parentRule.getTextRange().getEndOffset();
			}
		}

		return editor.getDocument().getLineEndOffset(editor.getDocument().getLineCount() - 1);
	}
}
