package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The factory that automatically maps all tokens and rule names into
 *  IElementType objects: {@link TokenIElementType} and {@link RuleIElementType}.
 *
 *  This caches all mappings for each Language that use this factory. I.e.,
 *  it's not keeping an instance per plugin/Language.
 */
public class PSIElementTypeFactory {
	private static final Map<Language, List<TokenIElementType>> tokenIElementTypesCache =
		new HashMap<Language, List<TokenIElementType>>();
	private static final Map<Language, List<RuleIElementType>> ruleIElementTypesCache =
		new HashMap<Language, List<RuleIElementType>>();
	private static final Map<Language, TokenIElementType> eofIElementTypesCache =
		new HashMap<Language, TokenIElementType>();

	private PSIElementTypeFactory() {
	}

	public static TokenIElementType getEofElementType(Language language) {
		TokenIElementType result = eofIElementTypesCache.get(language);
		if (result == null) {
			result = new TokenIElementType(Token.EOF, "EOF", language);
			eofIElementTypesCache.put(language, result);
		}

		return result;
	}

	public static List<TokenIElementType> getTokenIElementTypes(Language language) {
		return tokenIElementTypesCache.get(language);
	}

	public static List<TokenIElementType> getTokenIElementTypes(Language language,
	                                                           List<String> tokenNames)
	{
		List<TokenIElementType> types = tokenIElementTypesCache.get(language);
		if (types == null) {
			types = createTokenIElementTypes(language, tokenNames);
			tokenIElementTypesCache.put(language, types);
		}

		return types;
	}

	@NotNull
	private static List<TokenIElementType> createTokenIElementTypes(Language language, List<String> tokenNames) {
		List<TokenIElementType> result;
		TokenIElementType[] elementTypes = new TokenIElementType[tokenNames.size()];
		for (int i = 0; i < tokenNames.size(); i++) {
			if ( tokenNames.get(i)!=null ) {
				elementTypes[i] = new TokenIElementType(i, tokenNames.get(i), language);
			}
		}

		result = Collections.unmodifiableList(Arrays.asList(elementTypes));
		return result;
	}

	public static List<RuleIElementType> getRuleIElementTypes(Language language,
	                                                         List<String> ruleNames)
	{
		List<RuleIElementType> result = ruleIElementTypesCache.get(language);
		if (result == null) {
			result = createRuleIElementTypes(language, ruleNames);
			ruleIElementTypesCache.put(language, result);
		}

		return result;
	}

	@NotNull
	private static List<RuleIElementType> createRuleIElementTypes(Language language, List<String> ruleNames) {
		List<RuleIElementType> result;
		RuleIElementType[] elementTypes = new RuleIElementType[ruleNames.size()];
		for (int i = 0; i < ruleNames.size(); i++) {
			elementTypes[i] = new RuleIElementType(i, ruleNames.get(i), language);
		}

		result = Collections.unmodifiableList(Arrays.asList(elementTypes));
		return result;
	}

	public static TokenSet createTokenSet(Language language,
	                                      List<String> tokenNames,
	                                      int... types)
	{
		List<TokenIElementType> tokenIElementTypes =
			getTokenIElementTypes(language, tokenNames);

		IElementType[] elementTypes = new IElementType[types.length];
		for (int i = 0; i < types.length; i++) {
			if (types[i] == Token.EOF) {
				elementTypes[i] = getEofElementType(language);
			}
			else {
				elementTypes[i] = tokenIElementTypes.get(types[i]);
			}
		}

		return TokenSet.create(elementTypes);
	}

	public static TokenSet createRuleSet(Language language,
	                                     List<String> ruleNames,
	                                     int... rules)
	{
		List<RuleIElementType> tokenElementTypes =
			getRuleIElementTypes(language, ruleNames);

		IElementType[] elementTypes = new IElementType[rules.length];
		for (int i = 0; i < rules.length; i++) {
			if (rules[i] == Token.EOF) {
				elementTypes[i] = getEofElementType(language);
			}
			else {
				elementTypes[i] = tokenElementTypes.get(rules[i]);
			}
		}

		return TokenSet.create(elementTypes);
	}
}
