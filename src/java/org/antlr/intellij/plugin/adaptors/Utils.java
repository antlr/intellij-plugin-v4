package org.antlr.intellij.plugin.adaptors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

public class Utils {
	/* My ANTLR parser wants to ignore bad token sequences which works but
		 * then all of the error red squigglies are shifted off target.
		 * Allow all tokens through and then strip bad tokens by adding them
		 * to whitespace IDEA token set.
		 */
	public static LexerATNSimulator getLexerATNSimulator(final org.antlr.v4.runtime.Lexer lexer,
														 ATN atn,
														 DFA[] _decisionToDFA,
														 PredictionContextCache _sharedContextCache)
	{
		return new LexerATNSimulator(lexer,atn,_decisionToDFA,_sharedContextCache) {
			/** Must insert code between Lexer.nextToken() and interpreter match()
			 *  so we can trap exceptions during ATN simulation. We need bad tokens to
			 *  flow to Intellij.
			 */
			@Override
			public int match(@org.antlr.v4.runtime.misc.NotNull CharStream input, int mode) {
				int ttype;
				try {
					ttype = super.match(input, mode);
				}
				catch (LexerNoViableAltException e) {
					// trap bad token errors here and ignore exception
					// so they flow through to Lexer.nextToken() and it
					// returns an actual token for Intellij.
					ttype = ANTLRv4TokenTypeAdaptor.BAD_TOKEN_TYPE;
					lexer.notifyListeners(e);		// report error
					lexer.recover(e);
				}
				return ttype;
			}
		};
	}
}
