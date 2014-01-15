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
		if ( etype==ErrorType.CANNOT_FIND_TOKENS_FILE ) return; // ignore these
		super.emit(etype, msg);
	}
}
