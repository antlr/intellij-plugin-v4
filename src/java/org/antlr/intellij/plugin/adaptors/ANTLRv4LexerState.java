package org.antlr.intellij.plugin.adaptors;

import org.antlr.intellij.adaptor.lexer.ANTLRLexerState;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.IntegerStack;
import org.antlr.v4.runtime.misc.MurmurHash;

public class ANTLRv4LexerState extends ANTLRLexerState {
	/** Tracks whether we are in a lexer rule, a parser rule or neither;
	 *  managed by the ANTLRv4Lexer grammar.
	 */
	private final int currentRuleType;

	public ANTLRv4LexerState(int mode, IntegerStack modeStack, int currentRuleType) {
		super(mode, modeStack);
		this.currentRuleType = currentRuleType;
	}

	public int getCurrentRuleType() {
		return currentRuleType;
	}

	@Override
	public void apply(Lexer lexer) {
		super.apply(lexer);
		if (lexer instanceof ANTLRv4Lexer) {
			((ANTLRv4Lexer)lexer).setCurrentRuleType(getCurrentRuleType());
		}
	}

	@Override
	protected int hashCodeImpl() {
		int hash = MurmurHash.initialize();
		hash = MurmurHash.update(hash, getMode());
		hash = MurmurHash.update(hash, getModeStack());
		hash = MurmurHash.update(hash, getCurrentRuleType());
		return MurmurHash.finish(hash, 3);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof ANTLRv4LexerState)) {
			return false;
		}

		if (!super.equals(obj)) {
			return false;
		}

		ANTLRv4LexerState other = (ANTLRv4LexerState)obj;
		return this.getCurrentRuleType() == other.getCurrentRuleType();
	}
}
