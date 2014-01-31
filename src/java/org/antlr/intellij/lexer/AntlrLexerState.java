package org.antlr.intellij.lexer;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.IntegerStack;
import org.antlr.v4.runtime.misc.MurmurHash;
import org.antlr.v4.runtime.misc.ObjectEqualityComparator;

public class AntlrLexerState {
	private final int mode;
	private final int[] modeStack;

	private int cachedHashCode;

	public AntlrLexerState(int mode, IntegerStack modeStack) {
		this.mode = mode;
		this.modeStack = modeStack != null ? modeStack.toArray() : null;
	}

	public int getMode() {
		return mode;
	}

	public int[] getModeStack() {
		return modeStack;
	}

	public void apply(Lexer lexer) {
		lexer._mode = getMode();
		lexer._modeStack.clear();
		if (getModeStack() != null) {
			lexer._modeStack.addAll(getModeStack());
		}
	}

	@Override
	public final int hashCode() {
		if (cachedHashCode == 0) {
			cachedHashCode = hashCodeImpl();
		}

		return cachedHashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof AntlrLexerState)) {
			return false;
		}

		AntlrLexerState other = (AntlrLexerState)obj;
		return this.mode == other.mode
			&& ObjectEqualityComparator.INSTANCE.equals(this.modeStack, other.modeStack);
	}

	protected int hashCodeImpl() {
		int hash = MurmurHash.initialize();
		hash = MurmurHash.update(hash, mode);
		hash = MurmurHash.update(hash, modeStack);
		return MurmurHash.finish(hash, 2);
	}
}
