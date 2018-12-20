package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the adaptor class for implementations of {@link
 * com.intellij.lexer.Lexer} backed by an ANTLR 4 lexer. It supports
 * any ANTLR 4 lexer that does not store extra information for use in
 * custom actions. For lexers that do not store custom state information, this
 * default implementation is sufficient. Otherwise, subclass and override:
 * {#getInitialState} and {#getLexerState}.
 *
 * Intellij lexers need to track state as they must be able to
 * restart lexing in the middle of the input buffer. From
 * <a href="http://www.jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support/implementing_lexer.html">Intellij doc</a>:
 *
 * "A lexer that can be used incrementally may need to return its
 * state, which means the context corresponding to each position in a
 * file. For example, a Java lexer could have separate states for top
 * level context, comment context and string literal context. An
 * important requirement for a syntax highlighting lexer is that its
 * state must be represented by a single integer number returned from
 * Lexer.getState(). That state will be passed to the Lexer.start()
 * method, along with the start offset of the fragment to process,
 * when lexing is resumed from the middle of a file."
 *
 * This implementation supports single- as well as multi-mode lexers.
 *
 * @author Sam Harwell
 */
public class ANTLRLexerAdaptor extends com.intellij.lexer.LexerBase {
	/**
	 * Gets the {@link Language} supported by this lexer. This
	 * value is passed to {@link PSIElementTypeFactory} to ensure the
	 * correct collection of {@link IElementType} is used for
	 * assigning element types to tokens in {@link #getTokenType}.
	 */
	private final Language language;

	/**
	 * This field caches the collection of element types returned
	 * by {@link PSIElementTypeFactory#getTokenIElementTypes} for
	 * optimum efficiency of the {@link #getTokenType} method.
	 */
	private final List<? extends IElementType> tokenElementTypes;

	/**
	 * This is the backing field for {@link #getLexer()}.
	 */
	private final Lexer lexer;

	/**
	 * Provides a map from a {@code State} object &rarr; state
	 * index tracked by IntelliJ. This field provides for an
	 * efficient implementation of {@link #getState}.
	 */
	private final Map<ANTLRLexerState, Integer> stateCacheMap = new HashMap<ANTLRLexerState, Integer>();

	/**
	 * Provides a map from a state index tracked by IntelliJ
	 * &rarr; {@code ANTLRLexerState} object describing the ANTLR lexer
	 * state. This field provides for an efficient implementation
	 * of {@link #toLexerState}.
	 */
	private final List<ANTLRLexerState> stateCache = new ArrayList<ANTLRLexerState>();

	/**
	 * Caches the {@code buffer} provided in the call to {@link
	 * #start}, as required for implementing {@link
	 * #getBufferSequence}.
	 */
	private CharSequence buffer;

	/**
	 * Caches the {@code endOffset} provided in the call to {@link
	 * #start}, as required for implementing {@link
	 * #getBufferEnd}.
	 */
	private int endOffset;

	/**
	 * This field tracks the "exposed" lexer state, which differs
	 * from the actual current state of the lexer returned by
	 * {@link #getLexer()} by one token.
	 *
	 * <p>Due to the way IntelliJ requests token information, the
	 * ANTLR {@link Lexer} is always positioned one token past the
	 * token whose information is returned by calls to {@link
	 * #getTokenType}, {@link #getTokenType}, etc. When {@link
	 * #getState} is called, IntelliJ expects a state which is
	 * able to reproduce the {@link #currentToken}, but the ANTLR
	 * lexer has already moved past it. This field is assigned
	 * based in {@link #advance} based on the lexer state
	 * <em>before</em> the current token, after which {@link
	 * Lexer#nextToken} can be called to obtain {@link
	 * #currentToken}.</p>
	 */
	private ANTLRLexerState currentState;

	/**
	 * This field tracks the "exposed" lexer token. This is the
	 * result of the most recent call to {@link Lexer#nextToken}
	 * on the underlying ANTLR lexer, and is the source of
	 * information for {@link #getTokenStart}, {@link
	 * #getTokenType}, etc.
	 *
	 * @see #currentState
	 */
	private Token currentToken;

	/**
	 * Constructs a new instance of {@link ANTLRLexerAdaptor} with
	 * the specified {@link Language} and underlying ANTLR {@link
	 * Lexer}.
	 *
	 * @param language The language.
	 * @param lexer The underlying ANTLR lexer.
	 */
	public ANTLRLexerAdaptor(Language language, Lexer lexer) {
		this.language = language;
		this.tokenElementTypes = PSIElementTypeFactory.getTokenIElementTypes(language, Arrays.asList(lexer.getTokenNames()));
		this.lexer = lexer;
	}

	/**
	 * Gets the ANTLR {@link Lexer} used for actual tokenization of the input.
	 *
	 * @return the ANTLR {@link Lexer} instance
	 */
	protected Lexer getLexer() {
		return lexer;
	}

	/**
	 * Gets the {@link Token} object providing information for
	 * calls to {@link #getTokenStart}, {@link #getTokenType},
	 * etc.
	 *
	 * @return The current {@link Token} instance.
	 */
	protected Token getCurrentToken() {
		return currentToken;
	}

	@Override
	public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
		this.buffer = buffer;
		this.endOffset = endOffset;

		CharStream in = new CharSequenceCharStream(buffer, endOffset, IntStream.UNKNOWN_SOURCE_NAME);
		in.seek(startOffset);

		ANTLRLexerState state;
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
		return getTokenType(currentToken.getType() );
	}

	@Nullable
	public IElementType getTokenType(int antlrTokenType) {
		if ( antlrTokenType==Token.EOF ) {
			// return null when lexing is finished
			return null;
		}

		return tokenElementTypes.get(antlrTokenType);
	}

	@Override
	public void advance() {
		currentState = getLexerState(lexer);
		currentToken = lexer.nextToken();
	}

	@Override
	public int getState() {
		ANTLRLexerState state = currentState != null ? currentState : getInitialState();
		Integer existing = stateCacheMap.get(state);
		if (existing == null) {
			existing = stateCache.size();
			stateCache.add(state);
			stateCacheMap.put(state, existing);
		}

		return existing;
	}

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

	/**
	 * Update the current lexer to use the specified {@code input}
	 * stream starting in the specified {@code state}.
	 *
	 * <p>The current lexer may be obtained by calling {@link
	 * #getLexer}. The default implementation calls {@link
	 * Lexer#setInputStream} to set the input stream, followed by
	 * {@link ANTLRLexerState#apply} to initialize the state of
	 * the lexer.</p>
	 *
	 * @param input The new input stream for the lexer.
	 * @param state A {@code ANTLRLexerState} instance containing the starting state for the lexer.
	 */
	protected void applyLexerState(CharStream input, ANTLRLexerState state) {
		lexer.setInputStream(input);
		state.apply(lexer);
	}

	/**
	 * Get the initial {@code ANTLRLexerState} of the lexer.
	 *
	 * @return a {@code ANTLRLexerState} instance representing the state of
	 * the lexer at the beginning of an input.
	 */
	protected ANTLRLexerState getInitialState() {
		return new ANTLRLexerState(Lexer.DEFAULT_MODE, null);
	}

	/**
	 * Get a {@code ANTLRLexerState} instance representing the current state
	 * of the specified lexer.
	 *
	 * @param lexer The lexer.
	 * @return A {@code ANTLRLexerState} instance containing the current state of the lexer.
	 */
	protected ANTLRLexerState getLexerState(Lexer lexer) {
		if (lexer._modeStack.isEmpty()) {
			return new ANTLRLexerState(lexer._mode, null);
		}

		return new ANTLRLexerState(lexer._mode, lexer._modeStack);
	}

	/**
	 * Gets the {@code ANTLRLexerState} corresponding to the specified IntelliJ {@code state}.
	 *
	 * @param state The lexer state provided by IntelliJ.
	 * @return The {@code ANTLRLexerState} instance corresponding to the specified state.
	 */
	protected ANTLRLexerState toLexerState(int state) {
		return stateCache.get(state);
	}
}
