package org.antlr.intellij.plugin.parser;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.plugin.ANTLRv4TokenType;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;

// should be generated with ANTLR4TokenType objects for each token I think.
public class ANTLRv4TokenTypeAdaptor {
	public static Map<String,Integer> nameToType;
	public static Map<String,IElementType> nameToIDEATokenType =
		new HashMap<String, IElementType>();

	/** ANTLR lexer returns invalid tokens as this type. IDEA does not
	 *  pass these to the parser because I categorize them as white space.
	 *  Not optimal but did make sure everything is in sync.
	 */
	public static final ANTLRv4TokenType BAD_TOKEN = new ANTLRv4TokenType("BAD_TOKEN");
	public static int BAD_TOKEN_TYPE;

	public static final TokenSet WHITE_SPACES;
	public static final TokenSet COMMENTS;
	public static Map<String,IElementType> ruleNameToIDEAElementType =
		new HashMap<String, IElementType>();

	protected static String[] tokenNames = ANTLRv4Lexer.tokenNames;

	static {
		nameToType = toMap(tokenNames);
		for (String tname : tokenNames) {
			nameToIDEATokenType.put(tname, new ANTLRv4TokenType(tname));
		}
		nameToIDEATokenType.put("BAD_TOKEN", BAD_TOKEN);
		BAD_TOKEN_TYPE = tokenNames.length + 1; // invent new token type
		nameToType.put("BAD_TOKEN", BAD_TOKEN_TYPE);
		WHITE_SPACES = TokenSet.create( nameToIDEATokenType.get("CRLF"),
										nameToIDEATokenType.get("WHITE_SPACE"),
										BAD_TOKEN ); // DON'T ALLOW these to get to parser
		COMMENTS = TokenSet.create(nameToIDEATokenType.get("COMMENT"));

		for (String rname : ANTLRv4Parser.ruleNames) {
			ruleNameToIDEAElementType.put(rname, new ANTLRv4TokenType(rname));
		}
	}

	public static IElementType createElement(int ttype) {
		if ( ttype==Token.EOF ) return null; // IDEA wants null for EOF
		if ( ttype==BAD_TOKEN_TYPE ) return BAD_TOKEN;
		return nameToIDEATokenType.get(tokenNames[ttype]);
	}

	/** Convert array of strings to string->index map. Useful for
	 *  converting rulenames to name->ruleindex map.
	 */
	public static Map<String, Integer> toMap(String[] keys) {
		Map<String, Integer> m = new HashMap<String, Integer>();
		for (int i=0; i<keys.length; i++) {
			m.put(keys[i], i);
		}
		return m;
	}

	public static IElementType getTokenElementType(String name) {
		return nameToIDEATokenType.get(name);
	}

	public static int getTokenType(String name) {
		return nameToType.get(name);
	}
}
