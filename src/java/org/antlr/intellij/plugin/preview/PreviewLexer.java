package org.antlr.intellij.plugin.preview;

import com.intellij.lang.Language;
import org.antlr.intellij.adaptor.lexer.SimpleAntlrAdapter;
import org.antlr.v4.runtime.Lexer;

public class PreviewLexer extends SimpleAntlrAdapter {
	public PreviewLexer(Language language, Lexer lexer) {
		super(language, lexer);
	}
}
