package org.antlr.intellij.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RuleElementType extends IElementType {
	private final int ruleIndex;

	public RuleElementType(int ruleIndex, @NotNull @NonNls String debugName, @Nullable Language language) {
		super(debugName, language);
		this.ruleIndex = ruleIndex;
	}

	public int getRuleIndex() {
		return ruleIndex;
	}
}
