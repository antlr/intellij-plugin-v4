package org.antlr.intellij.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the base class for implementations of {@link com.intellij.lexer.Lexer} backed by an ANTLR 4 lexer.
 *
 * @author Sam Harwell
 */
public abstract class AbstractAntlrAdapter<State extends AntlrLexerState> extends com.intellij.lexer.LexerBase {
	private final Language language;
	private final List<? extends IElementType> tokenElementTypes;
	private final Lexer lexer;

	private final Map<State, Integer> stateCacheMap = new HashMap<State, Integer>();
	private final List<State> stateCache = new ArrayList<State>();

	private CharSequence buffer;
	private int endOffset;

	private State currentState;
	private Token currentToken;

	public AbstractAntlrAdapter(Language language, Lexer lexer) {
		this.language = language;
		this.tokenElementTypes = ElementTypeFactory.getTokenElementTypes(language, Arrays.asList(lexer.getTokenNames()));
		this.lexer = lexer;
	}

	protected Lexer getLexer() {
		return lexer;
	}

	protected Token getCurrentToken() {
		return currentToken;
	}

	@Override
	public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
		this.buffer = buffer;
		this.endOffset = endOffset;

		CharStream in = new CharSequenceCharStream(buffer, IntStream.UNKNOWN_SOURCE_NAME);
		in.seek(startOffset);

		State state;
		if (startOffset == 0 && initialState == 0) {
			state = getInitialState();
		} else {
			state = toLexerState(initialState);
		}

		applyLexerState(in, state);
		advance();
	}

	@Nullable
	@Override
	public IElementType getTokenType() {
		int type = currentToken.getType();
		if (type == Token.EOF) {
			// return null when lexing is finished
			return null;
		}

		return tokenElementTypes.get(type);
	}

	@Override
	public void advance() {
		currentState = getLexerState();
		currentToken = lexer.nextToken();
	}

	@Override
	public int getState() {
		State state = currentState != null ? currentState : getLexerState();
		Integer existing = stateCacheMap.get(state);
		if (existing == null) {
			existing = stateCache.size();
			stateCache.add(state);
			stateCacheMap.put(state, existing);
		}

		return existing;
	}

	/** Intellij wants token types not token objects and so it must ask for
	 *  the start and stop indexes for each token.
	 */
	@Override
	public int getTokenStart() {
		return currentToken.getStartIndex();
	}

	@Override
	public int getTokenEnd() {
		return currentToken.getStopIndex() + 1;
	}

	@Override
	public CharSequence getBufferSequence() {
		return buffer;
	}

	@Override
	public int getBufferEnd() {
		return endOffset;
	}

	protected void applyLexerState(CharStream input, State state) {
		lexer.setInputStream(input);
		lexer._mode = state.getMode();
		lexer._modeStack.clear();
		if (state.getModeStack() != null) {
			lexer._modeStack.addAll(state.getModeStack());
		}
	}

	protected abstract State getInitialState();

	protected abstract State getLexerState();

	protected State toLexerState(int state) {
		return stateCache.get(state);
	}

	private static class CharSequenceCharStream implements CharStream {
		private final CharSequence buffer;
		private final String sourceName;

		private int position;

		public CharSequenceCharStream(CharSequence buffer, String sourceName) {
			this.buffer = buffer;
			this.sourceName = sourceName;
		}

		@Override
		public String getText(@NotNull Interval interval) {
			return buffer.subSequence(interval.a, interval.b + 1).toString();
		}

		@Override
		public void consume() {
			if (position == buffer.length()) {
				throw new IllegalStateException("attempted to consume EOF");
			}

			position++;
		}

		@Override
		public int LA(int i) {
			if (i > 0) {
				int index = position + i - 1;
				if (index >= buffer.length()) {
					return IntStream.EOF;
				}

				return buffer.charAt(index);
			}
			else if (i < 0) {
				int index = position + i;
				if (index < 0) {
					return 0;
				}

				return buffer.charAt(index);
			}
			else {
				return 0;
			}
		}

		@Override
		public int mark() {
			return 0;
		}

		@Override
		public void release(int marker) {
		}

		@Override
		public int index() {
			return position;
		}

		@Override
		public void seek(int index) {
			if (index < 0) {
				throw new IllegalArgumentException("index cannot be negative");
			}

			index = Math.min(index, buffer.length());
			position = index;
		}

		@Override
		public int size() {
			return buffer.length();
		}

		@Override
		public String getSourceName() {
			return sourceName;
		}
	}
}
