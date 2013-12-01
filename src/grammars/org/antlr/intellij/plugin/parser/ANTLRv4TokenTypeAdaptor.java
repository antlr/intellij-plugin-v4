package org.antlr.intellij.plugin.parser;

import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.plugin.ANTLRv4TokenType;

// should be generated with ANTLR4TokenType objects for each token I think.
public class ANTLRv4TokenTypeAdaptor {
	/** ANTLR lexer returns invalid tokens as BAD_TOKEN type. IDEA does not
	 *  pass these to the parser because I categorize them as white space.
	 *  Not optimal but did make sure everything is in sync.
	 */
	public static final TokenSet WHITE_SPACES = ANTLRv4TokenTypes.WHITESPACES;
	public static final TokenSet COMMENTS = ANTLRv4TokenTypes.COMMENTS;
	public static final TokenSet KEYWORDS = ANTLRv4TokenTypes.KEYWORDS;

	public static ANTLRv4TokenType[] typeToIDEATokenType = ANTLRv4TokenTypes.typeToIDEATokenType;
	public static ANTLRv4TokenType[] ruleToIDEATokenType = ANTLRv4TokenTypes.ruleToIDEATokenType;
	public static String[] tokenNames = ANTLRv4Lexer.tokenNames;

	static {
	}
}
