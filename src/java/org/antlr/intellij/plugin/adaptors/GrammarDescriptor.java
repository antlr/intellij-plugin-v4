package org.antlr.intellij.plugin.adaptors;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.v4.runtime.Lexer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class GrammarDescriptor {
	public static Map<String,Integer> nameToType;
	public static Map<String, IElementType> nameToIDEATokenType =
		new HashMap<String, IElementType>();

	/** ANTLR lexer returns invalid tokens as this type. IDEA does not
	 *  pass these to the parser because I categorize them as white space.
	 *  Not optimal but did make sure everything is in sync.
	 */
	public IElementType BAD_TOKEN;
	public int BAD_TOKEN_TYPE;

	public TokenSet WHITE_SPACES;
	public TokenSet COMMENTS;
	public Map<String,IElementType> ruleNameToIDEAElementType =
		new HashMap<String, IElementType>();

	protected String[] tokenNames;
	protected String[] ruleNames;
	protected Language language;

	public GrammarDescriptor(Language language,
							 Class<? extends Lexer> lexerClass,
							 Class<? extends IElementType> tokenClass)
	{
		this.language = language;
		BAD_TOKEN = new IElementType("BAD_TOKEN", language);
		try {
			Method m = lexerClass.getDeclaredMethod("getTokenNames");
			tokenNames = (String[])m.invoke(null, (Object)null);
			m = lexerClass.getDeclaredMethod("getRuleNames");
			ruleNames = (String[])m.invoke(null, (Object)null);
			Constructor<? extends IElementType> ctor =
				tokenClass.getConstructor(String.class, Language.class);
			nameToType = toMap(tokenNames);
			for (String tname : tokenNames) {

				nameToIDEATokenType.put(tname, ctor.newInstance(tname, language));
			}
			nameToIDEATokenType.put("BAD_TOKEN", BAD_TOKEN);
			BAD_TOKEN_TYPE = tokenNames.length + 1; // invent new token type
			nameToType.put("BAD_TOKEN", BAD_TOKEN_TYPE);
			WHITE_SPACES = TokenSet.create( nameToIDEATokenType.get("CRLF"),
											nameToIDEATokenType.get("WHITE_SPACE"),
											BAD_TOKEN ); // DON'T ALLOW these to get to parser
			COMMENTS = TokenSet.create(nameToIDEATokenType.get("COMMENT"));
			for (String rname : ruleNames) {
				ruleNameToIDEAElementType.put(rname, ctor.newInstance(rname, language));
			}
		}
		catch (Exception e) {
			throw new RuntimeException("can't access token / rule names");
		}
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
}
