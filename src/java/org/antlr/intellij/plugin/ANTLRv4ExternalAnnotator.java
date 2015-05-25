package org.antlr.intellij.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.GrammarSemanticsMessage;
import org.antlr.v4.tool.GrammarSyntaxMessage;
import org.antlr.v4.tool.LeftRecursionCyclesMessage;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ToolMessage;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ANTLRv4ExternalAnnotator extends ExternalAnnotator<PsiFile, List<ANTLRv4ExternalAnnotator.Issue>> {
    // NOTE: can't use instance var as only 1 instance

    public static final Logger LOG = Logger.getInstance("ANTLRv4ExternalAnnotator");

	public static class Issue {
		String annotation;
		List<Token> offendingTokens = new ArrayList<Token>();
		ANTLRMessage msg;
		public Issue(ANTLRMessage msg) { this.msg = msg; }
	}

	/** Called first; return file */
	@Override
	@Nullable
	public PsiFile collectInformation(@NotNull PsiFile file) {
		return file;
	}

	/** Called 2nd; run antlr on file */
	@Nullable
	@Override
	public List<ANTLRv4ExternalAnnotator.Issue> doAnnotate(final PsiFile file) {
		String grammarFileName = file.getVirtualFile().getPath();
		LOG.info("doAnnotate "+grammarFileName);
		String fileContents = file.getText();
		List<String> args = RunANTLROnGrammarFile.getANTLRArgsAsList(file.getProject(), file.getVirtualFile());
		final Tool antlr = new Tool(args.toArray(new String[args.size()]));
		if ( !args.contains("-lib") ) {
			// getContainingDirectory() must be identified as a read operation on file system
			ApplicationManager.getApplication().runReadAction(new Runnable() {
				@Override
				public void run() {
					antlr.libDirectory = file.getContainingDirectory().toString();
				}
			});
		}

		antlr.removeListeners();
        AnnotatorToolListener listener = new AnnotatorToolListener();
        antlr.addListener(listener);
		try {
			StringReader sr = new StringReader(fileContents);
			ANTLRReaderStream in = new ANTLRReaderStream(sr);
			in.name = file.getName();
			GrammarRootAST ast = antlr.parse(file.getName(), in);
			if ( ast==null || ast.hasErrors ) return Collections.emptyList();
			Grammar g = antlr.createGrammar(ast);
			g.fileName = grammarFileName;

			String vocabName = g.getOptionString("tokenVocab");
			if ( vocabName!=null ) { // import vocab to avoid spurious warnings
				LOG.info("token vocab file "+vocabName);
				g.importTokensFromTokensFile();
			}

			VirtualFile vfile = file.getVirtualFile();
			if ( vfile==null ) {
				LOG.error("doAnnotate no virtual file for "+file);
				return listener.issues;
			}
			g.fileName = vfile.getPath();
			antlr.process(g, false);

			for (Issue issue : listener.issues) {
				processIssue(file, issue);
			}
		}
		catch (Exception e) {
			LOG.error("antlr can't process "+file.getName(), e);
		}
		return listener.issues;
	}

	/** Called 3rd */
	@Override
	public void apply(@NotNull PsiFile file,
					  List<ANTLRv4ExternalAnnotator.Issue> issues,
					  @NotNull AnnotationHolder holder)
	{
		for (int i = 0; i < issues.size(); i++) {
			Issue issue = issues.get(i);
			for (int j = 0; j < issue.offendingTokens.size(); j++) {
				Token t = issue.offendingTokens.get(j);
				if ( t instanceof CommonToken ) {
					CommonToken ct = (CommonToken)t;
					int startIndex = ct.getStartIndex();
					int stopIndex = ct.getStopIndex();
					TextRange range = new TextRange(startIndex, stopIndex + 1);
					switch (issue.msg.getErrorType().severity) {
					case ERROR:
					case ERROR_ONE_OFF:
					case FATAL:
						holder.createErrorAnnotation(range, issue.annotation);
						break;

					case WARNING:
						holder.createWarningAnnotation(range, issue.annotation);
						break;

					case WARNING_ONE_OFF:
					case INFO:
						holder.createWeakWarningAnnotation(range, issue.annotation);

					default:
						break;
					}
				}
			}
		}
		super.apply(file, issues, holder);
	}

	public void processIssue(final PsiFile file, Issue issue) {
		File grammarFile = new File(file.getVirtualFile().getPath());
		File issueFile = new File(issue.msg.fileName);
		if ( !grammarFile.getName().equals(issueFile.getName()) ) {
			return; // ignore errors from external files
		}
		if ( issue.msg instanceof GrammarSemanticsMessage ) {
			Token t = ((GrammarSemanticsMessage)issue.msg).offendingToken;
			issue.offendingTokens.add(t);
		}
		else if ( issue.msg instanceof LeftRecursionCyclesMessage ) {
			List<String> rulesToHighlight = new ArrayList<String>();
			LeftRecursionCyclesMessage lmsg = (LeftRecursionCyclesMessage)issue.msg;
			Collection<? extends Collection<Rule>> cycles =
				(Collection<? extends Collection<Rule>>)lmsg.getArgs()[0];
			for (Collection<Rule> cycle : cycles) {
				for (Rule r : cycle) {
					rulesToHighlight.add(r.name);
					GrammarAST nameNode = (GrammarAST)r.ast.getChild(0);
					issue.offendingTokens.add(nameNode.getToken());
				}
			}
		}
		else if ( issue.msg instanceof GrammarSyntaxMessage ) {
			Token t = issue.msg.offendingToken;
			issue.offendingTokens.add(t);
		}
		else if ( issue.msg instanceof ToolMessage ) {
			issue.offendingTokens.add(issue.msg.offendingToken);
		}

		Tool antlr = new Tool();
		ST msgST = antlr.errMgr.getMessageTemplate(issue.msg);
		String outputMsg = msgST.render();
		if (antlr.errMgr.formatWantsSingleLineMessage()) {
			outputMsg = outputMsg.replace('\n', ' ');
		}
		issue.annotation = outputMsg;
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
