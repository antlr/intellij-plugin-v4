package org.antlr.intellij.plugin;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.validation.GrammarIssue;
import org.antlr.intellij.plugin.validation.GrammarIssuesCollector;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.v4.tool.ErrorSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ANTLRv4ExternalAnnotator extends ExternalAnnotator<PsiFile, List<GrammarIssue>> {
    // NOTE: can't use instance var as only 1 instance

    public static final Logger LOG = Logger.getInstance("ANTLRv4ExternalAnnotator");

    /** Called first; return file */
	@Override
	@Nullable
	public PsiFile collectInformation(@NotNull PsiFile file) {
		return file;
	}

	/** Called 2nd; run antlr on file */
	@Nullable
	@Override
	public List<GrammarIssue> doAnnotate(final PsiFile file) {
		return GrammarIssuesCollector.collectGrammarIssues(file);
	}

    /** Called 3rd */
	@Override
	public void apply(@NotNull PsiFile file,
					  List<GrammarIssue> issues,
					  @NotNull AnnotationHolder holder)
	{
		for (int i = 0; i < issues.size(); i++) {
			GrammarIssue issue = issues.get(i);

			if ( issue.getOffendingTokens().isEmpty() ) {
				Annotation annotation = holder.createWarningAnnotation(file, issue.getAnnotation());
				annotation.setFileLevelAnnotation(true);
				continue;
			}

			for (int j = 0; j < issue.getOffendingTokens().size(); j++) {
				Token t = issue.getOffendingTokens().get(j);
				if ( t instanceof CommonToken ) {
					CommonToken ct = (CommonToken)t;
					int startIndex = ct.getStartIndex();
					int stopIndex = ct.getStopIndex();

					if (startIndex >= file.getTextLength()) {
						// can happen in case of a 'mismatched input EOF' error
						startIndex = stopIndex = file.getTextLength() - 1;
					}

					if (startIndex < 0) {
						// can happen on empty files, in that case we won't be able to show any error :/
						startIndex = 0;
					}

					TextRange range = new TextRange(startIndex, stopIndex + 1);
					ErrorSeverity severity = ErrorSeverity.INFO;
					if ( issue.getMsg().getErrorType()!=null ) {
						severity = issue.getMsg().getErrorType().severity;
					}
					switch ( severity ) {
					case ERROR:
					case ERROR_ONE_OFF:
					case FATAL:
						holder.createErrorAnnotation(range, issue.getAnnotation());
						break;

					case WARNING:
						holder.createWarningAnnotation(range, issue.getAnnotation());
						break;

					case WARNING_ONE_OFF:
					case INFO:
						holder.createWeakWarningAnnotation(range, issue.getAnnotation());

					default:
						break;
					}
				}
			}
		}
		super.apply(file, issues, holder);
	}

    public static String getFindVocabFileNameFromGrammarFile(PsiFile file) {
		final FindVocabFileRunnable findVocabAction = new FindVocabFileRunnable(file);
		ApplicationManager.getApplication().runReadAction(findVocabAction);
		return findVocabAction.vocabName;
	}

	protected static class FindVocabFileRunnable implements Runnable {
		public String vocabName;
		private final PsiFile file;

		public FindVocabFileRunnable(PsiFile file) {
			this.file = file;
		}

		@Override
		public void run() {
			vocabName = MyPsiUtils.findTokenVocabIfAny((ANTLRv4FileRoot) file);
		}
	}

}
