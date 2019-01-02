package org.antlr.intellij.plugin.parsing;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.ui.AntlrOutputView;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;

import java.util.ArrayList;
import java.util.List;

/** Used to track errors during antlr run on a grammar for generation,
 *  not for annotation of grammar.
 */
public class RunANTLRListener implements ANTLRToolListener {
	private Tool tool;
	private AntlrOutputView outputView;
	private boolean hasOutput = false;
	private boolean hasErrors = false;

	public RunANTLRListener(Tool tool, AntlrOutputView outputView) {
		this.tool = tool;
		this.outputView = outputView;
	}

	@Override
	public void info(String msg) {
		if (tool.errMgr.formatWantsSingleLineMessage()) {
			msg = msg.replace('\n', ' ');
		}
		outputView.addInfo(msg);
		hasOutput = true;
	}

	@Override
	public void error(ANTLRMessage msg) {
		outputView.addError(renderMessage(msg), getVirtualFile(msg), msg.line - 1, msg.charPosition);
		hasOutput = true;
		hasErrors = true;
	}

	@Override
	public void warning(ANTLRMessage msg) {
		outputView.addWarning(renderMessage(msg), getVirtualFile(msg), msg.line - 1, msg.charPosition);
		hasOutput = true;
	}

	public boolean hasOutput() {
		return hasOutput;
	}

	public boolean hasErrors() {
		return hasErrors;
	}

	private String renderMessage(ANTLRMessage msg) {
		String outputMsg = msg.getMessageTemplate(true).render();

		if (tool.errMgr.formatWantsSingleLineMessage()) {
			outputMsg = outputMsg.replace('\n', ' ');
		}

		return outputMsg;
	}

	private VirtualFile getVirtualFile(ANTLRMessage msg) {
		return LocalFileSystem.getInstance().findFileByPath(msg.fileName);
	}
}
