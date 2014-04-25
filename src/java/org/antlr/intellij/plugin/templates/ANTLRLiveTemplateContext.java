package org.antlr.intellij.plugin.templates;

import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiUtilBase;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.antlr.intellij.plugin.ANTLRv4ParserDefinition;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

public class ANTLRLiveTemplateContext extends TemplateContextType {
	public ANTLRLiveTemplateContext() {
		super("OUTSIDE_RULE", "Outside of rule");
	}

	@Override
	public boolean isInContext(@NotNull PsiFile file, int offset) { // offset is where cursor or insertion point is I guess
		if ( !PsiUtilBase.getLanguageAtOffset(file, offset).isKindOf(ANTLRv4Language.INSTANCE) ) {
			return false;
		}
		PsiElement element = file.findElementAt(offset);
		if ( element!=null &&
			 element.getText().startsWith(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED) )
		{
			// i hate this DUMMY_IDENTIFIER thing!!!!!!!!
			return false;
		}

		if ( element==null && offset==file.getTextLength() ) { // allow at EOF
			offset--;
			element = file.findElementAt(offset);
		}

		if ( element==null ) {
			return false;
		}

//		System.out.println("element " + element);
		if (!(element instanceof PsiWhiteSpace)) {
			return false;
		}

//		System.out.println("offset="+offset);
		CommonTokenStream tokens = ANTLRv4ParserDefinition.tokenize(file.getText());
		Token tokenUnderCursor = ANTLRv4ParserDefinition.getTokenUnderCursor(tokens, offset);
//		System.out.println(tokenUnderCursor);
		int tokenIndex = tokenUnderCursor.getTokenIndex();
		Token nextRealToken = ANTLRv4ParserDefinition.nextRealToken(tokens, tokenIndex);
		Token previousRealToken = ANTLRv4ParserDefinition.previousRealToken(tokens, tokenIndex);

		if ( nextRealToken==null || previousRealToken==null ) {
			return false;
		}

		int previousRealTokenType = previousRealToken.getType();
		int nextRealTokenType = nextRealToken.getType();

		if ( previousRealTokenType==ANTLRv4Parser.ACTION ) {
			// make sure we're not in a rule; has to be @lexer::header {...} stuff
			Token prevPrevRealToken = ANTLRv4ParserDefinition.previousRealToken(tokens, previousRealToken.getTokenIndex());
			if ( prevPrevRealToken==null ) {
				return false;
			}
//			System.out.println("prevPrevRealToken="+prevPrevRealToken);
			Token prevPrevPrevRealToken = ANTLRv4ParserDefinition.previousRealToken(tokens, prevPrevRealToken.getTokenIndex());
			if ( prevPrevPrevRealToken==null ) {
				return false;
			}
//			System.out.println("prevPrevPrevRealToken="+prevPrevPrevRealToken);
			if ( prevPrevPrevRealToken.getType()!=ANTLRv4Parser.AT &&
				 prevPrevPrevRealToken.getType()!=ANTLRv4Parser.COLONCOLON )
			{
				return false;
			}
		}

//		System.out.println("next = "+(nextRealTokenType!=Token.EOF?ANTLRv4Parser.tokenNames[nextRealTokenType]:"<EOF>"));
//		System.out.println("prev = "+ANTLRv4Parser.tokenNames[previousRealTokenType]);
//		System.out.println(tokens.getTokens());

		boolean okBefore =
			previousRealTokenType == ANTLRv4Parser.RBRACE ||
				previousRealTokenType == ANTLRv4Parser.SEMI ||
				previousRealTokenType == ANTLRv4Parser.ACTION;
		boolean okAfter =
			nextRealTokenType == ANTLRv4Parser.TOKEN_REF ||
				nextRealTokenType == ANTLRv4Parser.RULE_REF ||
				nextRealTokenType == Token.EOF;
		if ( okBefore && okAfter) {
//			System.out.println("in context");
			return true;
		}
		return false;
	}

	public static boolean canReachTargetWithHiddenTokens(PsiElement element, String targetTypeName) {
		PsiElement p = element;
		while ( true ) {
			if ( p==null ) {
				return targetTypeName == null;
			}
			if ( p.getNode().getElementType().toString().equals(targetTypeName) ) {
				return true;
			}
			if ( !(p instanceof LeafPsiElement) ) {
				if ( p.getChildren().length>0 ) {
					return false;
				}
				continue;
			}
			boolean isCmt = true;
			boolean isWS = p instanceof PsiWhiteSpace;
			if ( !isCmt && !isWS ) {
				return false;
			}
			p = p.getNextSibling();
		}
	}

}
