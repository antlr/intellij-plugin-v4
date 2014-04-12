package org.antlr.intellij.plugin.preview;

import com.intellij.lang.Language;
import org.antlr.intellij.adaptor.lexer.SimpleAntlrAdapter;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.Interval;

public class PreviewLexer extends SimpleAntlrAdapter {
	public PreviewLexer(Language language, Lexer lexer) {
		super(language, lexer);
		System.out.println("preview lexer input="+
							   lexer.getInputStream().getText(new Interval(0,lexer.getInputStream().size()-1)));
	}
}
