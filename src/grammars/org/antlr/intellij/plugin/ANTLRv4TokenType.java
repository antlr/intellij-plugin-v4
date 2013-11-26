package org.antlr.intellij.plugin;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ANTLRv4TokenType extends IElementType {
	public ANTLRv4TokenType(@NotNull @NonNls String debugName) {
		super(debugName, ANTLRv4Language.INSTANCE);
	}
}
