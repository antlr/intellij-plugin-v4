package org.antlr.intellij.plugin.templates;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

public class OutsideRuleContext extends ANTLRLiveTemplateContext {
	public OutsideRuleContext() {
		super("ANTLR_OUTSIDE", "Outside rule", ANTLRGenericContext.class);
	}

	@Override
	public boolean isInContext(@NotNull PsiFile file, PsiElement element, int offset) {
		CommonTokenStream tokens = ParsingUtils.tokenizeANTLRGrammar(file.getText());
		Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(tokens, offset);
		if ( tokenUnderCursor==null ) {
			return false; // sometimes happens at the eof
		}
		int tokenIndex = tokenUnderCursor.getTokenIndex();
		Token nextRealToken = ParsingUtils.nextRealToken(tokens, tokenIndex);
		Token previousRealToken = ParsingUtils.previousRealToken(tokens, tokenIndex);

		if ( nextRealToken==null || previousRealToken==null ) {
			return false;
		}

		int previousRealTokenType = previousRealToken.getType();
		int nextRealTokenType = nextRealToken.getType();

		if ( previousRealTokenType== ANTLRv4Parser.BEGIN_ACTION ) {
			// make sure we're not in a rule; has to be @lexer::header {...} stuff
			Token prevPrevRealToken = ParsingUtils.previousRealToken(tokens, previousRealToken.getTokenIndex());
			if ( prevPrevRealToken==null ) {
				return false;
			}
			Token prevPrevPrevRealToken = ParsingUtils.previousRealToken(tokens, prevPrevRealToken.getTokenIndex());
			if ( prevPrevPrevRealToken==null ) {
				return false;
			}
			if ( prevPrevPrevRealToken.getType()!=ANTLRv4Parser.AT &&
				 prevPrevPrevRealToken.getType()!=ANTLRv4Parser.COLONCOLON )
			{
				return false;
			}
		}

		boolean okBefore =
			previousRealTokenType == ANTLRv4Parser.RBRACE ||
				previousRealTokenType == ANTLRv4Parser.SEMI ||
				previousRealTokenType == ANTLRv4Parser.BEGIN_ACTION;
		boolean okAfter =
			nextRealTokenType == ANTLRv4Parser.TOKEN_REF ||
				nextRealTokenType == ANTLRv4Parser.RULE_REF ||
				nextRealTokenType == Token.EOF;

		return okBefore && okAfter;
	}
}
