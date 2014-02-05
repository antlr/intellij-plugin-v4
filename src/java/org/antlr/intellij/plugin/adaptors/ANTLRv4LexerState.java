package org.antlr.intellij.plugin.adaptors;

import org.antlr.intellij.lexer.AntlrLexerState;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.IntegerStack;
import org.antlr.v4.runtime.misc.MurmurHash;

public class ANTLRv4LexerState extends AntlrLexerState {
	private final int ruleType;

	public ANTLRv4LexerState(int mode, IntegerStack modeStack, int ruleType) {
		super(mode, modeStack);
		this.ruleType = ruleType;
	}

	public int getRuleType() {
		return ruleType;
	}

	@Override
	public void apply(Lexer lexer) {
		super.apply(lexer);
		if (lexer instanceof ANTLRv4Lexer) {
			((ANTLRv4Lexer)lexer).setRuleType(getRuleType());
		}
	}

	@Override
	protected int hashCodeImpl() {
		int hash = MurmurHash.initialize();
		hash = MurmurHash.update(hash, getMode());
		hash = MurmurHash.update(hash, getModeStack());
		hash = MurmurHash.update(hash, getRuleType());
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
		return this.getRuleType() == other.getRuleType();
	}
}
