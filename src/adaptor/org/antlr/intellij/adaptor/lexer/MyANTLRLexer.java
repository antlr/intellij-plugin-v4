package org.antlr.intellij.adaptor.lexer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.Token;

public abstract class MyANTLRLexer extends Lexer {
	public MyANTLRLexer(CharStream input) {
		super(input);
	}

	/** Return a token from this source; i.e., match a token on the char
	 *  stream.
	 */
	@Override
	public Token nextToken() {
		if (_input == null) {
			throw new IllegalStateException("nextToken requires a non-null input stream.");
		}

		// Mark start location in char stream so unbuffered streams are
		// guaranteed at least have text of current token
		int tokenStartMarker = _input.mark();
		try{
			outer:
			while (true) {
				if (_hitEOF) {
					emitEOF();
					return _token;
				}

				_token = null;
				_channel = Token.DEFAULT_CHANNEL;
				_tokenStartCharIndex = _input.index();
				_tokenStartCharPositionInLine = getInterpreter().getCharPositionInLine();
				_tokenStartLine = getInterpreter().getLine();
				_text = null;
				do {
					_type = Token.INVALID_TYPE;
//				System.out.println("nextToken line "+tokenStartLine+" at "+((char)input.LA(1))+
//								   " in mode "+mode+
//								   " at index "+input.index());
					int ttype;
					try {
						ttype = getInterpreter().match(_input, _mode);
					}
					catch (LexerNoViableAltException e) {
						notifyListeners(e);		// report error
						recover(e);
						ttype = SKIP;
					}
					if ( _input.LA(1)==IntStream.EOF ) {
						_hitEOF = true;
					}
					if ( _type == Token.INVALID_TYPE ) _type = ttype;
					if ( _type ==SKIP ) {
						continue outer;
					}
				} while ( _type ==MORE );
				if ( _token == null ) emit();
				return _token;
			}
		}
		finally {
			// make sure we release marker after match or
			// unbuffered char stream will keep buffering
			_input.release(tokenStartMarker);
		}
	}
}
