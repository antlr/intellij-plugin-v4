package org.antlr.intellij.plugin.refactor;

import org.stringtemplate.v4.misc.Misc;

import java.util.HashMap;
import java.util.Map;

public class RefactorUtils {
	public static final Map<String,String> literalToRuleNameMap = new HashMap<String, String>() {{
		put("'('", "LPAREN");
		put("')'", "RPAREN");
		put("'{'", "LBRACE");
		put("'}'", "RBRACE");
		put("'['", "LBRACK");
		put("']'", "RBRACK");
		put("';'", "SEMI");
		put("','", "COMMA");
		put("'.'", "DOT");
		put("'='", "ASSIGN");
		put("'>'", "GT");
		put("'<'", "LT");
		put("'!'", "BANG");
		put("'~'", "TILDE");
		put("'?'", "QUESTION");
		put("':'", "COLON");
		put("'=='", "EQUAL_EQUAL");
		put("'='",  "EQUAL");
		put("'<='", "LE");
		put("'>='", "GE");
		put("'!='", "NOT_EQUAL");
		put("'&&'", "AND");
		put("'||'", "OR");
		put("'++'", "INC");
		put("'--'", "DEC");
		put("'+'", "ADD");
		put("'-'", "SUB");
		put("'*'", "MUL");
		put("'/'", "DIV");
		put("'&'", "BITAND");
		put("'|'", "BITOR");
		put("'^'", "CARET");
		put("'%'", "MOD");
		put("'+='", "ADD_ASSIGN");
		put("'-='", "SUB_ASSIGN");
		put("'*='", "MUL_ASSIGN");
		put("'/='", "DIV_ASSIGN");
		put("'&='", "AND_ASSIGN");
		put("'|='", "OR_ASSIGN");
		put("'^='", "XOR_ASSIGN");
		put("'%='", "MOD_ASSIGN");
		put("'<<='", "LSHIFT_ASSIGN");
		put("'>>='", "RSHIFT_ASSIGN");
		put("'>>>='", "URSHIFT_ASSIGN");
		put("'@'", "AT");
		put("'...'", "ELLIPSIS");
		put("'\\'", "BACKSLASH");
	}};

	public static int lexerRuleNameID = 1;

	public static String getLexerRuleNameFromLiteral(String literal) {
		String name = literalToRuleNameMap.get(literal);
		if ( name!=null ) {
			return name;
		}
		// is it a keyword like true or begin?
		String strippedLiteral = Misc.strip(literal, 1);
		if ( Character.isLetter(strippedLiteral.charAt(0)) ) {
			return strippedLiteral.toUpperCase();
		}
		return "T__"+lexerRuleNameID++;
	}
}
