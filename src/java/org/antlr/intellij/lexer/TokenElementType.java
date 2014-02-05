package org.antlr.intellij.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TokenElementType extends IElementType {
	private final int type;

	public TokenElementType(int type, @NotNull @NonNls String debugName, @Nullable Language language) {
		super(debugName, language);
		this.type = type;
	}

	public int getType() {
		return type;
	}
}
