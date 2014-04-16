package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**  Represents a token in the language of the plug-in. The "token type" of
 *   leaf nodes in PSI tree.
 */
public class TokenElementType extends IElementType {
	public static final int BAD_TOKEN = -99; // a likely safe token type from ANTLR's point of view
	public static TokenElementType BAD_TOKEN_TYPE = new TokenElementType(BAD_TOKEN, "BAD_TOKEN", ANTLRv4Language.INSTANCE);

	private final int type;

	public TokenElementType(int type, @NotNull @NonNls String debugName, @Nullable Language language) {
		super(debugName, language);
		this.type = type;
	}

	public int getType() {
		return type;
	}
}
