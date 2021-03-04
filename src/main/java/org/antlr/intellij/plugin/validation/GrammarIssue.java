package org.antlr.intellij.plugin.validation;

import org.antlr.runtime.Token;
import org.antlr.v4.tool.ANTLRMessage;

import java.util.ArrayList;
import java.util.List;

public class GrammarIssue {
    private String annotation;
    private final List<Token> offendingTokens = new ArrayList<>();
    private final ANTLRMessage msg;

    public GrammarIssue(ANTLRMessage msg) { this.msg = msg; }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public List<Token> getOffendingTokens() {
        return offendingTokens;
    }

    public ANTLRMessage getMsg() {
        return msg;
    }
}
