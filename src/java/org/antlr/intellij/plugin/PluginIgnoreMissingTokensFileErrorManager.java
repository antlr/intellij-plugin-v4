package org.antlr.intellij.plugin;

import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ErrorManager;
import org.antlr.v4.tool.ErrorType;

public class PluginIgnoreMissingTokensFileErrorManager extends ErrorManager {
	public PluginIgnoreMissingTokensFileErrorManager(Tool tool) {
		super(tool);
	}

	@Override
	public void emit(ErrorType etype, ANTLRMessage msg) {
		if ( etype==ErrorType.CANNOT_FIND_TOKENS_FILE_REFD_IN_GRAMMAR ||
			 etype==ErrorType.CANNOT_FIND_TOKENS_FILE_GIVEN_ON_CMDLINE )
		{
			return; // ignore these
		}
		super.emit(etype, msg);
	}
}
