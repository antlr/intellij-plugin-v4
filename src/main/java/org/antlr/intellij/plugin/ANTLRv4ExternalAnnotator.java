package org.antlr.intellij.plugin;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.validation.GrammarIssue;
import org.antlr.intellij.plugin.validation.GrammarIssuesCollector;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.v4.tool.ErrorSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.antlr.intellij.plugin.actions.AnnotationIntentActionsFactory.getFix;

public class ANTLRv4ExternalAnnotator extends ExternalAnnotator<PsiFile, List<GrammarIssue>> {

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
		return ApplicationManager.getApplication().runReadAction((Computable<List<GrammarIssue>>) () ->
				GrammarIssuesCollector.collectGrammarIssues(file)
		);
	}

    /** Called 3rd */
	@Override
	public void apply(@NotNull PsiFile file,
					  List<GrammarIssue> issues,
					  @NotNull AnnotationHolder holder)
	{
		for ( GrammarIssue issue : issues ) {
			if ( issue.getOffendingTokens().isEmpty() ) {
				annotateFileIssue(file, holder, issue);
			}
			else {
				annotateIssue(file, holder, issue);
			}
		}

		final ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(file.getProject());
		if ( controller!=null && !ApplicationManager.getApplication().isUnitTestMode() ) {
			controller.getPreviewPanel().autoRefreshPreview(file.getVirtualFile());
		}
	}

	private void annotateFileIssue(@NotNull PsiFile file, @NotNull AnnotationHolder holder, GrammarIssue issue) {
		holder.newAnnotation(HighlightSeverity.WARNING, issue.getAnnotation())
			.fileLevel()
			.range(file)
			.create();
	}

	private void annotateIssue(@NotNull PsiFile file, @NotNull AnnotationHolder holder, GrammarIssue issue) {
		for ( Token t : issue.getOffendingTokens() ) {
			if ( t instanceof CommonToken && tokenBelongsToFile(t, file) ) {
				TextRange range = getTokenRange((CommonToken) t, file);
				ErrorSeverity severity = getIssueSeverity(issue);

				annotate(holder, issue, range, severity, file);
			}
		}
	}

	private ErrorSeverity getIssueSeverity(GrammarIssue issue) {
		if ( issue.getMsg().getErrorType()!=null ) {
			return issue.getMsg().getErrorType().severity;
		}

		return ErrorSeverity.INFO;
	}

	@NotNull
	private TextRange getTokenRange(CommonToken ct, @NotNull PsiFile file) {
		int startIndex = ct.getStartIndex();
		int stopIndex = ct.getStopIndex();

		if ( startIndex >= file.getTextLength() ) {
			// can happen in case of a 'mismatched input EOF' error
			startIndex = stopIndex = file.getTextLength() - 1;
		}

		if ( startIndex<0 ) {
			// can happen on empty files, in that case we won't be able to show any error :/
			startIndex = 0;
		}

		return new TextRange(startIndex, stopIndex + 1);
	}

	private boolean tokenBelongsToFile(Token t, @NotNull PsiFile file) {
		CharStream inputStream = t.getInputStream();
		if ( inputStream instanceof ANTLRFileStream ) {
			// Not equal if the token belongs to an imported grammar
			return inputStream.getSourceName().equals(file.getVirtualFile().getCanonicalPath());
		}

		return true;
	}

	private void annotate(@NotNull AnnotationHolder holder, GrammarIssue issue, TextRange range, ErrorSeverity severity, @NotNull PsiFile file) {
		AnnotationBuilder annotationBuilder = null;
		switch ( severity ) {
		case ERROR:
		case ERROR_ONE_OFF:
		case FATAL:
			annotationBuilder = holder.newAnnotation(HighlightSeverity.ERROR, issue.getAnnotation()).range(range);
			break;
		case WARNING:
			annotationBuilder = holder.newAnnotation(HighlightSeverity.WARNING, issue.getAnnotation()).range(range);
			break;
		case WARNING_ONE_OFF:
		case INFO:
			/* When trying to remove the deprecation warning, you will need something like this:
			AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.WEAK_WARNING, issue.getAnnotation()).range(range);
			 */
			annotationBuilder = holder.newAnnotation(HighlightSeverity.WEAK_WARNING, issue.getAnnotation()).range(range);
		default:
			break;
		}

		if (annotationBuilder != null) {
			getFix(range, issue.getMsg().getErrorType(), file)
					.ifPresent(annotationBuilder::withFix);
			annotationBuilder.create();
		}
	}
}
