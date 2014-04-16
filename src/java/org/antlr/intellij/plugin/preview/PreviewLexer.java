package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.lang.Language;
import org.antlr.intellij.adaptor.lexer.SimpleAntlrAdapter;
import org.antlr.v4.runtime.Lexer;

public class PreviewLexer extends SimpleAntlrAdapter {
	public PreviewLexer(Language language, Lexer lexer) {
		super(language, lexer);
	}

	@Override
	public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
		CharSequence input = buffer.subSequence(startOffset, endOffset);
		System.out.println("input = "+ input +", all="+buffer);
		if ( isAutoCompleteWeirdString(input.toString()) ) {
			getLexer().removeErrorListeners();
		}

		String input2 = buffer.toString().substring(startOffset);
		if ( input2.substring(endOffset).equals(CompletionInitializationContext.DUMMY_IDENTIFIER) ||
			input2.substring(endOffset).equals(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED) )
		{
			System.err.println("TRAP!!!!!!!!!!!!!! "+input+", endOffset"+endOffset);
		}

		super.start(buffer, startOffset, endOffset, initialState);
	}
}
