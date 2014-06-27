package org.antlr.intellij.plugin;

import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;
import org.antlr.v4.tool.ErrorType;

import java.util.ArrayList;
import java.util.List;

/** Track all issues found in a grammar for use by the external
 *  annotator that puts up messages in grammar editor window.
 *  The annotator looks for semantic errors not syntax errors,
 *  which are indicated with error nodes in the PSI.
 */
class AnnotatorToolListener implements ANTLRToolListener {
    public final String vocabName;
    public final List<ANTLRv4ExternalAnnotator.Issue> issues =
        new ArrayList<ANTLRv4ExternalAnnotator.Issue>();

    public AnnotatorToolListener(String vocabName) {
        this.vocabName = vocabName;
    }

    @Override
    public void info(String msg) {
    }

    @Override
    public void error(ANTLRMessage msg) {
        if ( (msg.getErrorType()!=ErrorType.IMPLICIT_TOKEN_DEFINITION &&
              msg.getErrorType()!=ErrorType.IMPLICIT_STRING_DEFINITION) ||
            vocabName==null )
        {
            issues.add(new ANTLRv4ExternalAnnotator.Issue(msg));
        }
    }

    @Override
    public void warning(ANTLRMessage msg) {
        issues.add(new ANTLRv4ExternalAnnotator.Issue(msg));
    }
}
