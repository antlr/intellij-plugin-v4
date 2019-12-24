package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.runtime.CharStream;

/**
 * All the case transformations that can be applied in the ANTLR Preview
 * window when lexing the input.
 *
 * @see CaseChangingCharStream
 */
public enum CaseChangingStrategy {
	LEAVE_AS_IS {
		@Override
		public CharStream applyTo(CharStream source) {
			return source;
		}

		@Override
		public String toString() {
			return "Leave as-is";
		}
	},
	FORCE_UPPERCASE {
		@Override
		public CharStream applyTo(CharStream source) {
			return new CaseChangingCharStream(source, true);
		}

		@Override
		public String toString() {
			return "Transform to uppercase when lexing";
		}
	},
	FORCE_LOWERCASE {
		@Override
		public CharStream applyTo(CharStream source) {
			return new CaseChangingCharStream(source, false);
		}

		@Override
		public String toString() {
			return "Transform to lowercase when lexing";
		}
	};

	public abstract CharStream applyTo(CharStream source);

}
