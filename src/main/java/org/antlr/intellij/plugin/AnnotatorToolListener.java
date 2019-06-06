package org.antlr.intellij.plugin;

import org.antlr.intellij.plugin.validation.GrammarIssue;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;

import java.util.ArrayList;
import java.util.List;

/** Track all issues found in a grammar for use by the external
 *  annotator that puts up messages in grammar editor window.
 *  The annotator looks for semantic errors not syntax errors,
 *  which are indicated with error nodes in the PSI.
 */
class AnnotatorToolListener implements ANTLRToolListener {
    public final List<GrammarIssue> issues = new ArrayList<GrammarIssue>();

    @Override
    public void info(String msg) {
    }

    @Override
    public void error(ANTLRMessage msg) {
        issues.add(new GrammarIssue(msg));
    }

    @Override
    public void warning(ANTLRMessage msg) {
        issues.add(new GrammarIssue(msg));
    }
}
