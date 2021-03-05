package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.DefaultToolListener;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/** Track errors, warnings from loading grammars. Really just
 *  swallows them for now. The external annotator shows errors.
 */
public class LoadGrammarsToolListener extends DefaultToolListener {
    public List<String> grammarErrorMessages = new ArrayList<>();
    public List<String> grammarWarningMessages = new ArrayList<>();
	public LoadGrammarsToolListener(Tool tool) { super(tool); }

	@Override
	public void error(ANTLRMessage msg) {
		ST msgST = tool.errMgr.getMessageTemplate(msg);
        String s = msgST.render();
		if (tool.errMgr.formatWantsSingleLineMessage()) {
			s = s.replace('\n', ' ');
		}
        grammarErrorMessages.add(s);
	}

	@Override
	public void warning(ANTLRMessage msg) {
		ST msgST = tool.errMgr.getMessageTemplate(msg);
        String s = msgST.render();
        if (tool.errMgr.formatWantsSingleLineMessage()) {
            s = s.replace('\n', ' ');
        }
        grammarWarningMessages.add(s);
	}

	public void clear() {
		grammarErrorMessages.clear();
		grammarWarningMessages.clear();
	}
}
