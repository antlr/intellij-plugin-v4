package org.antlr.intellij.plugin.validation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.Token;
import org.antlr.v4.Tool;
import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.codegen.Target;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.tool.*;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.antlr.v4.tool.ast.RuleRefAST;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.*;


public class GrammarIssuesCollector {
    private static final String LANGUAGE_ARG_PREFIX = "-Dlanguage=";

    public static final Logger LOG = Logger.getInstance(GrammarIssuesCollector.class.getName());

    public static List<GrammarIssue> collectGrammarIssues(PsiFile file) {
        String grammarFileName = file.getVirtualFile().getPath();
        LOG.info("doAnnotate "+grammarFileName);
        String fileContents = file.getText();
        List<String> args = RunANTLROnGrammarFile.getANTLRArgsAsList(file.getProject(), file.getVirtualFile());
        GrammarIssuesCollectorToolListener listener = new GrammarIssuesCollectorToolListener();

        String languageArg = findLanguageArg(args);

        if ( languageArg!=null ) {
            String language = languageArg.substring(LANGUAGE_ARG_PREFIX.length());

            if ( !targetExists(language) ) {
                GrammarIssue issue = new GrammarIssue(null);
                issue.setAnnotation("Unknown target language '" + language + "', analysis will be done using the default target language 'Java'");
                listener.getIssues().add(issue);

                args.remove(languageArg);
            }
        }

        final Tool antlr = new Tool(args.toArray(new String[args.size()]));
        if ( !args.contains("-lib") ) {
            // getContainingDirectory() must be identified as a read operation on file system
            ApplicationManager.getApplication().runReadAction(() -> {
                antlr.libDirectory = file.getContainingDirectory().toString();
            });
        }

        antlr.removeListeners();
        antlr.addListener(listener);
        try {
            StringReader sr = new StringReader(fileContents);
            ANTLRReaderStream in = new ANTLRReaderStream(sr);
            in.name = file.getName();
            GrammarRootAST ast = antlr.parse(file.getName(), in);
            if ( ast==null || ast.hasErrors ) {
                for (GrammarIssue issue : listener.getIssues()) {
                    processIssue(file, issue);
                }
                return listener.getIssues();
            }
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
                return listener.getIssues();
            }
            g.fileName = vfile.getPath();
            antlr.process(g, false);

            Map<String, GrammarAST> unusedRules = getUnusedParserRules(g);
            if ( unusedRules!=null ) {
                for (String r : unusedRules.keySet()) {
                    Token ruleDefToken = unusedRules.get(r).getToken();
                    GrammarIssue issue = new GrammarIssue(new GrammarInfoMessage(g.fileName, ruleDefToken, r));
                    listener.getIssues().add(issue);
                }
            }

            for (GrammarIssue issue : listener.getIssues()) {
                processIssue(file, issue);
            }
        }
        catch (Exception e) {
            LOG.error("antlr can't process "+file.getName(), e);
        }
        return listener.getIssues();
    }

    @Nullable
    private static String findLanguageArg(List<String> args) {
        for ( String arg : args ) {
            if ( arg.startsWith(LANGUAGE_ARG_PREFIX) ) {
                return arg;
            }
        }

        return null;
    }

    public static void processIssue(final PsiFile file, GrammarIssue issue) {
        File grammarFile = new File(file.getVirtualFile().getPath());
        if ( issue.getMsg() == null || issue.getMsg().fileName==null ) { // weird, issue doesn't have a file associated with it
            return;
        }
        File issueFile = new File(issue.getMsg().fileName);
        if ( !grammarFile.getName().equals(issueFile.getName()) ) {
            return; // ignore errors from external files
        }
        ST msgST = null;
        if ( issue.getMsg() instanceof GrammarInfoMessage ) { // not in ANTLR so must hack it in
            Token t = ((GrammarSemanticsMessage) issue.getMsg()).offendingToken;
            issue.getOffendingTokens().add(t);
            msgST = new ST("unused parser rule <arg>");
            msgST.add("arg", t.getText());
            msgST.impl.name = "info";
        }
        else if ( issue.getMsg() instanceof GrammarSemanticsMessage ) {
            Token t = ((GrammarSemanticsMessage) issue.getMsg()).offendingToken;
            issue.getOffendingTokens().add(t);
        }
        else if ( issue.getMsg() instanceof LeftRecursionCyclesMessage) {
            List<String> rulesToHighlight = new ArrayList<>();
            LeftRecursionCyclesMessage lmsg = (LeftRecursionCyclesMessage) issue.getMsg();
            Collection<? extends Collection<Rule>> cycles =
                (Collection<? extends Collection<Rule>>)lmsg.getArgs()[0];
            for (Collection<Rule> cycle : cycles) {
                for (Rule r : cycle) {
                    rulesToHighlight.add(r.name);
                    GrammarAST nameNode = (GrammarAST)r.ast.getChild(0);
                    issue.getOffendingTokens().add(nameNode.getToken());
                }
            }
        }
        else if ( issue.getMsg() instanceof GrammarSyntaxMessage) {
            Token t = issue.getMsg().offendingToken;
            issue.getOffendingTokens().add(t);
        }
        else if ( issue.getMsg() instanceof ToolMessage ) {
            issue.getOffendingTokens().add(issue.getMsg().offendingToken);
        }

        Tool antlr = new Tool();
        if ( msgST==null ) {
            msgST = antlr.errMgr.getMessageTemplate(issue.getMsg());
        }
        String outputMsg = msgST.render();
        if ( antlr.errMgr.formatWantsSingleLineMessage() ) {
            outputMsg = outputMsg.replace('\n', ' ');
        }
        issue.setAnnotation(outputMsg);
    }

    private static Map<String,GrammarAST> getUnusedParserRules(Grammar g) {
        if ( g.ast==null || g.isLexer() ) return null;
        List<GrammarAST> ruleNodes = g.ast.getNodesWithTypePreorderDFS(IntervalSet.of(ANTLRParser.RULE_REF));
        // in case of errors, we walk AST ourselves
        // ANTLR's Grammar object might have bailed on rule defs etc...
        Set<String> ruleRefs = new HashSet<>();
        Map<String,GrammarAST> ruleDefs = new HashMap<>();
        for (GrammarAST x : ruleNodes) {
            if ( x.getParent().getType()==ANTLRParser.RULE ) {
                ruleDefs.put(x.getText(), x);
            }
            else if ( x instanceof RuleRefAST) {
                RuleRefAST r = (RuleRefAST) x;
                ruleRefs.add(r.getText());
            }
        }
        ruleDefs.keySet().removeAll(ruleRefs);
        return ruleDefs;
    }

    public static boolean targetExists(String language) {
        String targetName = "org.antlr.v4.codegen.target."+language+"Target";
        try {
            Class<? extends Target> c = Class.forName(targetName).asSubclass(Target.class);
            Constructor<? extends Target> ctor = c.getConstructor(CodeGenerator.class);
            return true;
        }
        catch (Exception e) { // ignore errors; we're detecting presence only
        }
        return false;
    }
}
