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
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ErrorSeverity;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.GrammarSemanticsMessage;
import org.antlr.v4.tool.GrammarSyntaxMessage;
import org.antlr.v4.tool.LeftRecursionCyclesMessage;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ToolMessage;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.antlr.v4.tool.ast.RuleRefAST;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ANTLRv4ExternalAnnotator extends ExternalAnnotator<PsiFile, List<ANTLRv4ExternalAnnotator.Issue>> {
    // NOTE: can't use instance var as only 1 instance

    public static final Logger LOG = Logger.getInstance("ANTLRv4ExternalAnnotator");

	public static class Issue {
		String annotation;
		List<Token> offendingTokens = new ArrayList<Token>();
		ANTLRMessage msg;
		public Issue(ANTLRMessage msg) { this.msg = msg; }
	}

	public static class GrammarInfoMessage extends GrammarSemanticsMessage {
		public GrammarInfoMessage(String fileName, Token offendingToken, Object... args) {
			super(null, fileName, offendingToken, args);
		}
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

			Map<String, GrammarAST> unusedRules = getUnusedParserRules(g);
			if ( unusedRules!=null ) {
				for (String r : unusedRules.keySet()) {
					Token ruleDefToken = unusedRules.get(r).getToken();
					Issue issue = new Issue(new GrammarInfoMessage(g.fileName, ruleDefToken, r));
					listener.issues.add(issue);
				}
			}

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
					ErrorSeverity severity = ErrorSeverity.INFO;
					if ( issue.msg.getErrorType()!=null ) {
						severity = issue.msg.getErrorType().severity;
					}
					switch ( severity ) {
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
		if ( issue.msg.fileName==null ) { // weird, issue doesn't have a file associated with it
			return;
		}
		File issueFile = new File(issue.msg.fileName);
		if ( !grammarFile.getName().equals(issueFile.getName()) ) {
			return; // ignore errors from external files
		}
		ST msgST = null;
		if ( issue.msg instanceof GrammarInfoMessage ) { // not in ANTLR so must hack it in
			Token t = ((GrammarSemanticsMessage)issue.msg).offendingToken;
			issue.offendingTokens.add(t);
			msgST = new ST("unused parser rule <arg>");
			msgST.add("arg", t.getText());
			msgST.impl.name = "info";
		}
		else if ( issue.msg instanceof GrammarSemanticsMessage ) {
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
		if ( msgST==null ) {
			msgST = antlr.errMgr.getMessageTemplate(issue.msg);
		}
		String outputMsg = msgST.render();
		if ( antlr.errMgr.formatWantsSingleLineMessage() ) {
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

	public static Map<String,GrammarAST> getUnusedParserRules(Grammar g) {
		if ( g.ast==null || g.isLexer() ) return null;
		List<GrammarAST> ruleNodes = g.ast.getNodesWithTypePreorderDFS(IntervalSet.of(ANTLRParser.RULE_REF));
		// in case of errors, we walk AST ourselves
		// ANTLR's Grammar object might have bailed on rule defs etc...
		Set<String> ruleRefs = new HashSet<String>();
		Map<String,GrammarAST> ruleDefs = new HashMap<String,GrammarAST>();
		for (GrammarAST x : ruleNodes) {
			if ( x.getParent().getType()==ANTLRParser.RULE ) {
//				System.out.println("def "+x);
				ruleDefs.put(x.getText(), x);
			}
			else if ( x instanceof RuleRefAST ) {
				RuleRefAST r = (RuleRefAST) x;
//				System.out.println("ref "+r);
				ruleRefs.add(r.getText());
			}
		}
		ruleDefs.keySet().removeAll(ruleRefs);
		return ruleDefs;
	}
}
