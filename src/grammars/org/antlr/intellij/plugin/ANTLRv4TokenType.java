package org.antlr.intellij.plugin;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ANTLRv4TokenType extends IElementType {
	public int ttype;
	public ANTLRv4TokenType(String debugName) {
		super(debugName, ANTLRv4Language.INSTANCE);
	}

	public ANTLRv4TokenType(int ttype, @NotNull @NonNls String debugName) {
		super(debugName, ANTLRv4Language.INSTANCE);
		this.ttype = ttype;
	}
}
