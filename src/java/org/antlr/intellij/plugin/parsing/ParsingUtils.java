package org.antlr.intellij.plugin.parsing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.adaptor.parser.SyntaxError;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.PluginIgnoreMissingTokensFileErrorManager;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.preview.PreviewPanel;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.v4.Tool;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarRootAST;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ParsingUtils {
	public static Grammar BAD_PARSER_GRAMMAR;
	public static LexerGrammar BAD_LEXER_GRAMMAR;

	static {
		try {
			ParsingUtils.BAD_PARSER_GRAMMAR = new Grammar("grammar BAD; a : 'bad' ;");
			ParsingUtils.BAD_PARSER_GRAMMAR.name = "BAD_PARSER_GRAMMAR";
			ParsingUtils.BAD_LEXER_GRAMMAR = new LexerGrammar("lexer grammar BADLEXER; A : 'bad' ;");
			ParsingUtils.BAD_LEXER_GRAMMAR.name = "BAD_LEXER_GRAMMAR";
		}
		catch (org.antlr.runtime.RecognitionException re) {
			ANTLRv4PluginController.LOG.error("can't init bad grammar markers");
		}
	}

	public static Token nextRealToken(CommonTokenStream tokens, int i) {
		int n = tokens.size();
		i++; // search after current i token
		if ( i>=n || i<0 ) return null;
		Token t = tokens.get(i);
		while ( t.getChannel()==Token.HIDDEN_CHANNEL ) {
			if ( t.getType()==Token.EOF ) {
				TokenSource tokenSource = tokens.getTokenSource();
				if ( tokenSource==null ) {
					return new CommonToken(Token.EOF, "EOF");
				}
				TokenFactory<?> tokenFactory = tokenSource.getTokenFactory();
				if ( tokenFactory==null ) {
					return new CommonToken(Token.EOF, "EOF");
				}
				return tokenFactory.create(Token.EOF, "EOF");
			}
			i++;
			if ( i>=n ) return null; // just in case no EOF
			t = tokens.get(i);
		}
		return t;
	}

	public static Token previousRealToken(CommonTokenStream tokens, int i) {
		int size = tokens.size();
		i--; // search before current i token
		if ( i>=size || i<0 ) return null;
		Token t = tokens.get(i);
		while ( t.getChannel()==Token.HIDDEN_CHANNEL ) {
			i--;
			if ( i<0 ) return null;
			t = tokens.get(i);
		}
		return t;
	}

	public static Token getTokenUnderCursor(PreviewState previewState, int offset) {
		if ( previewState==null || previewState.parsingResult == null) return null;

		PreviewParser parser = (PreviewParser) previewState.parsingResult.parser;
		CommonTokenStream tokenStream =	(CommonTokenStream) parser.getInputStream();
		return ParsingUtils.getTokenUnderCursor(tokenStream, offset);
	}

	public static Token getTokenUnderCursor(CommonTokenStream tokens, int offset) {
		Comparator<Token> cmp = new Comparator<Token>() {
			@Override
			public int compare(Token a, Token b) {
				if ( a.getStopIndex() < b.getStartIndex() ) return -1;
				if ( a.getStartIndex() > b.getStopIndex() ) return 1;
				return 0;
			}
		};
		if ( offset<0 || offset >= tokens.getTokenSource().getInputStream().size() ) return null;
		CommonToken key = new CommonToken(Token.INVALID_TYPE, "");
		key.setStartIndex(offset);
		key.setStopIndex(offset);
		List<Token> tokenList = tokens.getTokens();
		Token tokenUnderCursor = null;
		int i = Collections.binarySearch(tokenList, key, cmp);
		if ( i>=0 ) tokenUnderCursor = tokenList.get(i);
		return tokenUnderCursor;
	}

	/*
	[77] = {org.antlr.v4.runtime.CommonToken@16710}"[@77,263:268='import',<25>,9:0]"
	[78] = {org.antlr.v4.runtime.CommonToken@16709}"[@78,270:273='java',<100>,9:7]"
	 */
	public static Token getSkippedTokenUnderCursor(CommonTokenStream tokens, int offset) {
		if ( offset<0 || offset >= tokens.getTokenSource().getInputStream().size() ) return null;
		Token prevToken = null;
		Token tokenUnderCursor = null;
		for (Token t : tokens.getTokens()) {
			int begin = t.getStartIndex();
			int end = t.getStopIndex();
			if ( (prevToken==null || offset > prevToken.getStopIndex()) && offset < begin ) {
				// found in between
				TokenSource tokenSource = tokens.getTokenSource();
				CharStream inputStream = null;
				if ( tokenSource!=null ) {
					inputStream = tokenSource.getInputStream();
				}
				tokenUnderCursor = new org.antlr.v4.runtime.CommonToken(
					new Pair<TokenSource, CharStream>(tokenSource, inputStream),
					Token.INVALID_TYPE,
					-1,
					prevToken!=null ? prevToken.getStopIndex()+1 : 0,
					begin-1
				);
				break;
			}
			if ( offset >= begin && offset <= end ) {
				tokenUnderCursor = t;
				break;
			}
			prevToken = t;
		}
		return tokenUnderCursor;
	}

	public static SyntaxError getErrorUnderCursor(java.util.List<SyntaxError> errors, int offset) {
		for (SyntaxError e : errors) {
			int a, b;
			RecognitionException cause = e.getException();
			if ( cause instanceof LexerNoViableAltException) {
				a = ((LexerNoViableAltException) cause).getStartIndex();
				b = ((LexerNoViableAltException) cause).getStartIndex()+1;
			}
			else {
				Token offendingToken = (Token)e.getOffendingSymbol();
				a = offendingToken.getStartIndex();
				b = offendingToken.getStopIndex()+1;
			}
			if ( offset >= a && offset < b ) { // cursor is over some kind of error
				return e;
			}
		}
		return null;
	}

	public static CommonTokenStream tokenizeANTLRGrammar(String text) {
		ANTLRInputStream input = new ANTLRInputStream(text);
		ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		tokens.fill();
		return tokens;
	}

    public static ParseTree getParseTreeNodeWithToken(ParseTree tree, Token token) {
        if ( tree==null || token==null ) {
            return null;
        }

        Collection<ParseTree> tokenNodes = Trees.findAllTokenNodes(tree, token.getType());
        for (ParseTree t : tokenNodes) {
            TerminalNode tnode = (TerminalNode)t;
            if ( tnode.getPayload() == token ) {
                return tnode;
            }
        }
        return null;
    }

    public static ParsingResult parseANTLRGrammar(String text) {
	    ANTLRInputStream input = new ANTLRInputStream(text);
		ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.removeErrorListeners();
		parser.addErrorListener(listener);
		lexer.removeErrorListeners();
		lexer.addErrorListener(listener);

		ParseTree t = parser.grammarSpec();
		return new ParsingResult(parser, t, listener);
	}

	public static ParsingResult parseText(PreviewState previewState,
										  PreviewPanel previewPanel,
										  final VirtualFile grammarFile,
										  String inputText)
		throws IOException
	{
		ANTLRv4PluginController.LOG.info("parseText("+grammarFile.getName()+
										 ", input="+inputText.subSequence(0,Math.min(30, inputText.length()))+"...)");
		String grammarFileName = grammarFile.getPath();
		if (!new File(grammarFileName).exists()) {
			ANTLRv4PluginController.LOG.info("parseText grammar doesn't exist "+grammarFileName);
			return null;
		}

		if ( previewState.g==null || previewState.lg==null ) {
			ANTLRv4PluginController.LOG.info("parseText can't parse: missing lexer or parser no Grammar object for "+grammarFileName);
			previewPanel.inputPanel.displayErrorInParseErrorConsole("Plugin can only show preview for combined lexer/parser grammars");
			return null;
		}

		ANTLRInputStream input = new ANTLRInputStream(inputText);
		LexerInterpreter lexEngine;
		lexEngine = previewState.lg.createLexerInterpreter(input);
		CommonTokenStream tokens = new CommonTokenStream(lexEngine);
		PreviewParser parser = new PreviewParser(previewState, tokens);
		parser.setProfile(true);

		SyntaxErrorListener syntaxErrorListener = new SyntaxErrorListener();
		parser.removeErrorListeners();
		parser.addErrorListener(syntaxErrorListener);
		lexEngine.removeErrorListeners();
		lexEngine.addErrorListener(syntaxErrorListener);

		Rule start = previewState.g.getRule(previewState.startRuleName);
		if ( start==null ) {
			return null; // can't find start rule
		}
		ParseTree t = parser.parse(start.index);

		if ( t!=null ) {
			return new ParsingResult(parser, t, syntaxErrorListener);
		}
		return null;
	}

	/** Get lexer and parser grammars */
	public static Grammar[] loadGrammars(String grammarFileName, Project project) {
		ANTLRv4PluginController.LOG.info("loadGrammars "+grammarFileName+" "+project.getName());
		Tool antlr = new Tool();

		antlr.errMgr = new PluginIgnoreMissingTokensFileErrorManager(antlr);
		antlr.errMgr.setFormat("antlr");
		LoadGrammarsToolListener listener = new LoadGrammarsToolListener(antlr);
		antlr.removeListeners();
		antlr.addListener(listener);

		// basically here I am mimicking the loadGrammar() method from Tool
		// so that I can check for an empty AST coming back.
		GrammarRootAST grammarRootAST = antlr.parseGrammar(grammarFileName);
		if ( grammarRootAST==null ) {
			ANTLRv4PluginController.LOG.info("Empty or bad grammar "+grammarFileName+" "+project.getName());
			return null;
		}
		// Create a grammar from the AST so we can figure out what kind it is
		Grammar g = antlr.createGrammar(grammarRootAST);
		g.fileName = grammarFileName;
		antlr.process(g, false);
		if ( listener.grammarErrorMessages.size()!=0 ) {
			ANTLRv4PluginController.LOG.info("loadGrammars "+grammarFileName+" has errors");
			return null; // upon error, bail
		}

		// Examine's Grammar AST constructed by v3 for a v4 grammar.
		// Use ANTLR v3's ANTLRParser not ANTLRv4Parser from this plugin
		switch ( g.getType() ) {
			case ANTLRParser.PARSER :
				ANTLRv4PluginController.LOG.info("loadGrammars parser "+g.name);
				return new Grammar[] {null, g};
			case ANTLRParser.LEXER :
				ANTLRv4PluginController.LOG.info("loadGrammars lexer "+g.name);
				return new Grammar[] {g, null};
			case ANTLRParser.COMBINED :
				listener.clear();
				LexerGrammar lg = g.getImplicitLexer();
				if ( listener.grammarErrorMessages.size()!=0 ) {
					ANTLRv4PluginController.LOG.info("loadGrammars implicit lexer from "+g.name+" has errors");
					lg = BAD_LEXER_GRAMMAR;
				}
				ANTLRv4PluginController.LOG.info("loadGrammars combined: "+lg.name+", "+g.name);
				return new Grammar[] {lg, g};
		}
		ANTLRv4PluginController.LOG.info("loadGrammars invalid grammar type "+g.getTypeString()+" for "+g.name);
		return null;
	}

	/** Same as loadGrammar(fileName) except import vocab from existing lexer */
	public static Grammar loadGrammar(Tool tool, String fileName, LexerGrammar lexerGrammar) {
		GrammarRootAST grammarRootAST = tool.parseGrammar(fileName);
		if ( grammarRootAST==null ) return null;
		final Grammar g = tool.createGrammar(grammarRootAST);
		g.fileName = fileName;
		if ( lexerGrammar!=null ) {
            g.importVocab(lexerGrammar);
        }
		tool.process(g, false);
		return g;
	}
}
