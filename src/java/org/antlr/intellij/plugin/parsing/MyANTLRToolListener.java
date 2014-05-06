package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.DefaultToolListener;
import org.stringtemplate.v4.ST;

public class MyANTLRToolListener extends DefaultToolListener {
	public String grammarErrorMessage;
	public MyANTLRToolListener(Tool tool) { super(tool); }

	@Override
	public void error(ANTLRMessage msg) {
//			super.error(msg);
		ST msgST = tool.errMgr.getMessageTemplate(msg);
		grammarErrorMessage = msgST.render();
		if (tool.errMgr.formatWantsSingleLineMessage()) {
			grammarErrorMessage = grammarErrorMessage.replace('\n', ' ');
		}
	}
}
