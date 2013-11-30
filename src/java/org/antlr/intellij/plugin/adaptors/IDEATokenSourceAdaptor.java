package org.antlr.intellij.plugin.adaptors;


import com.intellij.lang.PsiBuilder;
import org.antlr.intellij.plugin.ANTLRv4TokenType;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenFactory;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;

/** Have an IDEA lexer mimic an ANTLR v4 lexer so parser can pull tokens back
 *  from IDEA.  IDEA sucks them from ANTLR via ANTLRv4LexerAdaptor which
 *  pulls from actual lexer.
 *
 *  Suck tokens from antlr lexer via builder that wraps it. It caches
 *  all tokens so I need to get them back for inserting into a TokenStream.
 *
 *  We don't know what the original InputStream source is since Intellij
 *  won't give it back to us.
 *
 *  A highlighting lexer tokenizes everything and returns bad tokens even.
 *  For the parsing interface, I use a variation on the lexer that does
 *  not return bad tokens. I have an adapter then that asks the builder
 *  for tokens via advanceLexer() which I then convert back to ANTLR tokens.
 */
public class IDEATokenSourceAdaptor implements TokenSource {
	protected PsiBuilder builder;
	protected TokenFactory factory = CommonTokenFactory.DEFAULT;

	public IDEATokenSourceAdaptor(PsiBuilder builder) {
		this.builder = builder;
	}

	@Override
	public int getCharPositionInLine() {
		return 0;
	}

	/* Colin: "the parsing lexer still has to return tokens that completely
	 cover the file (i.e. no gaps). This is one of the most significant
	 differences from a traditional compiler parser/lexer."

	  after lots of trial and error I finally just put the BAD_TOKEN
	  into the white space class so that they do not come to the parser
	  but that IDEA still knows about them.
	 */
	@Override
	public Token nextToken() {
		ANTLRv4TokenType ideaTType = (ANTLRv4TokenType)builder.getTokenType();
		int ttype;
		int channel = Token.DEFAULT_CHANNEL;
		if ( ideaTType==null ) {
			ttype = Token.EOF;
		}
		else {
			ttype = ideaTType.ttype;
		}
		Pair<TokenSource, CharStream> source =
			new Pair<TokenSource, CharStream>(this, null);
		// I don't think IDEA is tracking line, column info, so leave out.
		String text = builder.getTokenText();
		Token t = factory.create(source, ttype, text,
								 channel, 0, 0, 0, 0);
		if ( t instanceof CommonToken ) {
			CommonToken ct = (CommonToken)t;
			int start = builder.getCurrentOffset();
			ct.setStartIndex(start);
			int tl = 0;
			if ( text!=null ) tl = text.length();
			int stop = start+tl-1;
			ct.setStopIndex(stop);
		}
		builder.advanceLexer();
		return t;
	}

	@Override
	public int getLine() {
		return 0;
	}

	@Override
	public CharStream getInputStream() {
		return null;
	}

	@Override
	public String getSourceName() {
		return "IDEA PsiBuilder";
	}

	@Override
	public void setTokenFactory(@org.antlr.v4.runtime.misc.NotNull TokenFactory<?> factory) {
		this.factory = factory;
	}

	@Override
	public TokenFactory<?> getTokenFactory() {
		return factory;
	}
}
