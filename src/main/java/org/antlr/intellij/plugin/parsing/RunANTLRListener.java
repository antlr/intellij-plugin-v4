package org.antlr.intellij.plugin.parsing;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/** Used to track errors during antlr run on a grammar for generation,
 *  not for annotation of grammar.
 */
public class RunANTLRListener implements ANTLRToolListener {
	public final List<String> all = new ArrayList<>();
	public Tool tool;
	public ConsoleView console;
	public boolean hasOutput = false;

	public RunANTLRListener(Tool tool, ConsoleView console) {
		this.tool = tool;
		this.console = console;
	}

	@Override
	public void info(String msg) {
		if (tool.errMgr.formatWantsSingleLineMessage()) {
			msg = msg.replace('\n', ' ');
		}
		console.print(msg+"\n", ConsoleViewContentType.NORMAL_OUTPUT);
		hasOutput = true;
	}

	@Override
	public void error(ANTLRMessage msg) {
		track(msg, ConsoleViewContentType.ERROR_OUTPUT);
	}

	@Override
	public void warning(ANTLRMessage msg) {
		track(msg, ConsoleViewContentType.NORMAL_OUTPUT);
	}

	private void track(ANTLRMessage msg, ConsoleViewContentType errType) {
		ST msgST = tool.errMgr.getMessageTemplate(msg);
		String outputMsg = msgST.render();
		if (tool.errMgr.formatWantsSingleLineMessage()) {
			outputMsg = outputMsg.replace('\n', ' ');
		}
		console.print(outputMsg+"\n", errType);
		hasOutput = true;
	}
}
