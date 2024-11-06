package org.antlr.intellij.plugin.templates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.jetbrains.annotations.NotNull;

public abstract class ANTLRLiveTemplateContext extends TemplateContextType {
	public ANTLRLiveTemplateContext(@NotNull String presentableName)
	{
		super(presentableName);
	}

	protected abstract boolean isInContext(@NotNull PsiFile file, @NotNull PsiElement element, int offset);

	@Override
	public boolean isInContext(TemplateActionContext context) {
		var file = context.getFile();
		var offset = context.getStartOffset();

		// offset is where cursor or insertion point is I guess
		if ( !PsiUtilBase.getLanguageAtOffset(file, offset).isKindOf(ANTLRv4Language.INSTANCE) ) {
			return false;
		}
		if ( offset==file.getTextLength() ) { // allow at EOF
			offset--;
		}
		PsiElement element = file.findElementAt(offset);

		if ( element==null ) {
			return false;
		}

		return isInContext(file, element, offset);
	}
}
