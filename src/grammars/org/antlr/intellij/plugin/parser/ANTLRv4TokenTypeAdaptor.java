package org.antlr.intellij.plugin.parser;

import com.intellij.psi.tree.TokenSet;

// should be generated with ANTLR4TokenType objects for each token I think.
public class ANTLRv4TokenTypeAdaptor {
	/** ANTLR lexer returns invalid tokens as BAD_TOKEN type. IDEA does not
	 *  pass these to the parser because I categorize them as white space.
	 *  Not optimal but did make sure everything is in sync.
	 */
	public static final TokenSet WHITE_SPACES = ANTLRv4TokenTypes.WHITESPACES;
	public static final TokenSet COMMENTS = ANTLRv4TokenTypes.COMMENTS;
	public static final TokenSet KEYWORDS = ANTLRv4TokenTypes.KEYWORDS;

	static {
	}
}
