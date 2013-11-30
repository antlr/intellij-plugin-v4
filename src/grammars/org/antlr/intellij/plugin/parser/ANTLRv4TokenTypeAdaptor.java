package org.antlr.intellij.plugin.parser;

import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.plugin.ANTLRv4TokenType;
import org.antlr.v4.runtime.Token;

// should be generated with ANTLR4TokenType objects for each token I think.
public class ANTLRv4TokenTypeAdaptor {
	/** ANTLR lexer returns invalid tokens as this type. IDEA does not
	 *  pass these to the parser because I categorize them as white space.
	 *  Not optimal but did make sure everything is in sync.
	 */
	// this is now in gen'd code
	public static final ANTLRv4TokenType BAD_TOKEN = ANTLRv4TokenTypes.BAD_TOKEN;
	public static int BAD_TOKEN_TYPE = Token.INVALID_TYPE;

	public static final TokenSet WHITE_SPACES = ANTLRv4TokenTypes.WHITESPACES;
	public static final TokenSet COMMENTS = ANTLRv4TokenTypes.COMMENTS;
	public static final TokenSet KEYWORDS = ANTLRv4TokenTypes.KEYWORDS;

	public static ANTLRv4TokenType[] typeToIDEATokenType = ANTLRv4TokenTypes.typeToIDEATokenType;
	public static ANTLRv4TokenType[] ruleToIDEATokenType = ANTLRv4TokenTypes.ruleToIDEATokenType;
	public static String[] tokenNames = ANTLRv4Lexer.tokenNames;

	static {
	}
}
