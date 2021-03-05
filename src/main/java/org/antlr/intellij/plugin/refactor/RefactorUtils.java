package org.antlr.intellij.plugin.refactor;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.runtime.tree.xpath.XPath;
import org.antlr.v4.tool.Grammar;
import org.stringtemplate.v4.misc.Misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

	public static TerminalNode getRuleDefNameNode(Parser parser, ParseTree tree, String ruleName) {
		Collection<ParseTree> ruleDefRuleNodes;
		if ( Grammar.isTokenName(ruleName) ) {
			ruleDefRuleNodes = XPath.findAll(tree, "//lexerRule/TOKEN_REF", parser);
		}
		else {
			ruleDefRuleNodes = XPath.findAll(tree, "//parserRuleSpec/RULE_REF", parser);
		}
		for (ParseTree node : ruleDefRuleNodes) {
			String r = node.getText(); // always a TerminalNode; just get rule name of this def
			if ( r.equals(ruleName) ) {
				return (TerminalNode)node;
			}
		}
		return null;
	}

	public static boolean ruleHasMultipleOutermostAlts(Parser parser, ParseTree ruleTree) {
		Collection<ParseTree> ors = XPath.findAll(ruleTree, "/parserRuleSpec/ruleBlock/ruleAltList/OR", parser);
		if ( ors.size()>=1 ) return true;
		ors = XPath.findAll(ruleTree, "/lexerRule/lexerRuleBlock/lexerAltList/OR", parser);
		return ors.size()>=1;
	}

	public static Token getTokenForCharIndex(TokenStream tokens, int charIndex) {
		for (int i=0; i<tokens.size(); i++) {
			Token t = tokens.get(i);
			if ( charIndex>=t.getStartIndex() && charIndex<=t.getStopIndex() ) {
				return t;
			}
		}
		return null;
	}

	public static ParseTree getAncestorWithType(ParseTree t, Class<? extends ParseTree> clazz) {
		if ( t==null || clazz==null || t.getParent()==null ) return null;
		Tree p = t.getParent();
		while ( p!=null ) {
			if ( p.getClass()==clazz ) return (ParseTree)p;
			p = p.getParent();
		}
		return null;
	}

	public static int childIndexOf(ParseTree t, ParseTree child) {
		if ( t==null || child==null ) return -1;
		for (int i = 0; i < t.getChildCount(); i++) {
			if ( child==t.getChild(i) ) return i;
		}
		return -1;
	}

	public static void replaceText(final Project project, final Document doc,
	                               final int start, final int stop, // inclusive
	                               final String text)
	{
		WriteCommandAction setTextAction = new WriteCommandAction(project) {
			@Override
			protected void run(final Result result) {
				doc.replaceString(start, stop+1, text);
			}
		};
		setTextAction.execute();
	}

	public static void insertText(final Project project, final Document doc,
	                              final int where,
	                              final String text)
	{
		WriteCommandAction setTextAction = new WriteCommandAction(project) {
			@Override
			protected void run(final Result result) {
				doc.insertString(where, text);
			}
		};
		setTextAction.execute();
	}

	/** Get start/stop of an entire rule including semi and then clean up
	 *  WS at end.
	 */
	public static String getRuleText(CommonTokenStream tokens, ParserRuleContext ruleDefNode) {
		Token stop = ruleDefNode.getStop();
		Token semi = stop;
		TerminalNode colonNode = ruleDefNode.getToken(ANTLRv4Parser.COLON, 0);
		Token colon = colonNode.getSymbol();
		Token beforeSemi = tokens.get(stop.getTokenIndex()-1);
		Token afterColon = tokens.get(colon.getTokenIndex()+1);

		// trim whitespace/comments before / after rule text
		List<Token> ignoreBefore = tokens.getHiddenTokensToRight(colon.getTokenIndex());
		List<Token> ignoreAfter = tokens.getHiddenTokensToLeft(semi.getTokenIndex());
		Token textStart = afterColon;
		Token textStop = beforeSemi;
		if ( ignoreBefore!=null ) {
			Token lastWSAfterColon = ignoreBefore.get(ignoreBefore.size()-1);
			textStart = tokens.get(lastWSAfterColon.getTokenIndex()+1);
		}
		if ( ignoreAfter!=null ) {
			int firstWSAtEndOfRule = ignoreAfter.get(0).getTokenIndex()-1;
			textStop = tokens.get(firstWSAtEndOfRule); // stop before 1st ignore token at end
		}

		return tokens.getText(textStart, textStop);
	}

	public static List<TerminalNode> getAllRuleRefNodes(Parser parser, ParseTree tree, String ruleName) {
		List<TerminalNode> nodes = new ArrayList<>();
		Collection<ParseTree> ruleRefs;
		if ( Grammar.isTokenName(ruleName) ) {
			ruleRefs = XPath.findAll(tree, "//lexerRuleBlock//TOKEN_REF", parser);
		}
		else {
			ruleRefs = XPath.findAll(tree, "//ruleBlock//RULE_REF", parser);
		}
		for (ParseTree node : ruleRefs) {
			TerminalNode terminal = (TerminalNode)node;
			Token rrefToken = terminal.getSymbol();
			String r = rrefToken.getText();
			if ( r.equals(ruleName) ) {
				nodes.add(terminal);
			}
		}
		if ( nodes.size()==0 ) return null;
		return nodes;
	}

	/** Given a token index into buffer, find surrounding rule then return
	 *  char position of start of next rule.
	 */
	public static int getCharIndexOfNextRuleStart(ParserRuleContext tree, int tokenIndex) {
		final ParserRuleContext selNode =
			Trees.getRootOfSubtreeEnclosingRegion(tree, tokenIndex, tokenIndex);
		final ParserRuleContext ruleRoot = (ParserRuleContext)
			getAncestorWithType(selNode, ANTLRv4Parser.RuleSpecContext.class);

		return ruleRoot.getStop().getStopIndex() + 2; // insert after '\n' following ';'
	}
}
