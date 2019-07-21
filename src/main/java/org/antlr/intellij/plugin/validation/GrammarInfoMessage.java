package org.antlr.intellij.plugin.validation;

import org.antlr.runtime.Token;
import org.antlr.v4.tool.GrammarSemanticsMessage;

public class GrammarInfoMessage extends GrammarSemanticsMessage {
    public GrammarInfoMessage(String fileName, Token offendingToken, Object... args) {
        super(null, fileName, offendingToken, args);
    }
}
