package org.antlr.intellij.plugin.adaptors;


import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypeAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.Nullable;

/** Mimic an IDEA lexer but pull tokens from ANTLR lexer.
 *
 *  Jon Acktar:
 *
 *  Basically the job of the lexer is to turn the entire contents of the file
 *  into a token stream that is used as the representation of the document
 *  from then on you have to emit a stream that covers the entire file.
 *  You can't skip anything in the lexer step..
 */
public class LexerAdaptor extends LexerBase {
	org.antlr.v4.runtime.Lexer lexer;
	CharSequence buffer;
	int startOffset;
	int endOffset;
	int initialState;

	public LexerAdaptor(org.antlr.v4.runtime.Lexer lexer) {
		this.lexer = lexer;
	}

	/** API doc says that the end offset is the offset but in fact it seems
	 * 	to be the length.  Access buffer[endOffset] to enjoy and exception.
	 */
	@Override
	public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
		this.buffer = buffer;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.initialState = initialState;
		String text = buffer.subSequence(startOffset, endOffset).toString();
		System.out.println("start: "+buffer+", "+startOffset+", "+endOffset+", "+initialState);
		CharStream in = new ANTLRInputStream(text);
		lexer.setInputStream(in);
		advance(); // get first token, makes it available to lexer.getToken() in getTokenType()
	}

	@Nullable
	@Override
	public IElementType getTokenType() {
		Token t = lexer.getToken();
		int antlrTokenType = t.getType();

		IElementType type;
		if ( antlrTokenType == Token.EOF ) {
			type = null; // IDEA wants null not EOF.
		}
		else if ( antlrTokenType==Token.INVALID_TYPE ) {
			type = ANTLRv4TokenTypes.BAD_TOKEN;
		}
		else {
			type = ANTLRv4TokenTypeAdaptor.typeToIDEATokenType[antlrTokenType];
		}
		System.out.println("getTokenType: "+type+" from "+t);
		return type;
	}

	@Override
	public void advance() {
//		System.out.println("advance()");
		if ( lexer.getInputStream()!=null ) {
			lexer.nextToken();
		}
	}

	@Override
	public int getState() {
		return initialState;
	}

	/** Intellij wants token types not token objects and so it must ask for
	 *  the start and stop indexes for each token.
	 */
	@Override
	public int getTokenStart() {
		Token t = lexer.getToken();
		return startOffset + t.getStartIndex();
	}

	@Override
	public int getTokenEnd() {
		Token t = lexer.getToken();
		// seems like stop must be one PAST the last char for this token
		return startOffset + t.getStopIndex() + 1;
	}

	@Override
	public CharSequence getBufferSequence() {
		return buffer;
	}

	@Override
	public int getBufferEnd() {
		return endOffset;
	}
}
