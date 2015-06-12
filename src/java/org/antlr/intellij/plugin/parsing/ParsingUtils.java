package org.antlr.intellij.plugin.parsing;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.Predicate;
import org.antlr.intellij.adaptor.parser.SyntaxError;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.PluginIgnoreMissingTokensFileErrorManager;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.intellij.plugin.test.InterpreterRuleContextTextProvider;
import org.antlr.v4.Tool;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.UnbufferedTokenStream;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

		PreviewParser parser = (PreviewParser)previewState.parsingResult.parser;
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
		CommonTokenStream tokens = new TokenStreamSubset(lexer);
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
		CommonTokenStream tokens = new TokenStreamSubset(lexer);
		ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

		SyntaxErrorListener listener = new SyntaxErrorListener();
		parser.removeErrorListeners();
		parser.addErrorListener(listener);
		lexer.removeErrorListeners();
		lexer.addErrorListener(listener);

		ParseTree t = parser.grammarSpec();
		return new ParsingResult(parser, t, listener);
	}

	/** Parse grammar text into v4 parse tree then look for tokenVocab=X */
	public static String getTokenVocabFromGrammar(String text) {
		// TODO: unneeded. use antlr Tool. Kill?
//		ParsingResult r = parseANTLRGrammar(text);
//		if ( r.tree!=null ) { //&& r.syntaxErrorListener.getSyntaxErrors().size()==0 ) {
//			// option : id ASSIGN optionValue ;
//			Collection<ParseTree> options = XPath.findAll(r.tree, "//option", r.parser);
//			for (Iterator<ParseTree> it = options.iterator(); it.hasNext(); ) {
//				ANTLRv4Parser.OptionContext option = (ANTLRv4Parser.OptionContext)it.next();
//				if ( option.id().getText().equals("tokenVocab") ) {
//					/*
//					optionValue
//						:	id (DOT id)*
//						|	STRING_LITERAL
//						|	ACTION
//						|	INT
//						;
//					 */
//					ANTLRv4Parser.OptionValueContext optionValue = option.optionValue();
//					if ( optionValue.STRING_LITERAL()!=null ) {
//						String s = optionValue.STRING_LITERAL().getText();
//						return RefactorUtils.getLexerRuleNameFromLiteral(s);
//					}
//					if ( optionValue.id(0)!=null ) {
//						return optionValue.id(0).getText();
//					}
//				}
//			}
//		}
		return null;
	}

	public static ParsingResult parseText(Grammar g,
										  LexerGrammar lg,
										  String startRuleName,
										  final VirtualFile grammarFile,
										  String inputText)
		throws IOException
	{
		ANTLRInputStream input = new ANTLRInputStream(inputText);
		LexerInterpreter lexEngine;
		lexEngine = lg.createLexerInterpreter(input);
		SyntaxErrorListener syntaxErrorListener = new SyntaxErrorListener();
		lexEngine.removeErrorListeners();
		lexEngine.addErrorListener(syntaxErrorListener);
		CommonTokenStream tokens = new TokenStreamSubset(lexEngine);
		return parseText(g, lg, startRuleName, grammarFile, syntaxErrorListener, tokens, 0);
	}

	public static ParsingResult parseText(Grammar g,
										  LexerGrammar lg,
										  String startRuleName,
										  final VirtualFile grammarFile,
										  SyntaxErrorListener syntaxErrorListener,
										  TokenStream tokens,
										  int startIndex)
		throws IOException
	{
		if ( g==null || lg==null ) {
			ANTLRv4PluginController.LOG.info("parseText can't parse: missing lexer or parser no Grammar object for " +
											 (grammarFile != null ? grammarFile.getName() : "<unknown file>"));
			return null;
		}

		String grammarFileName = g.fileName;
		if (!new File(grammarFileName).exists()) {
			ANTLRv4PluginController.LOG.info("parseText grammar doesn't exist "+grammarFileName);
			return null;
		}

		if ( g==BAD_PARSER_GRAMMAR || lg==BAD_LEXER_GRAMMAR ) {
			return null;
		}

		tokens.seek(startIndex);

		PreviewParser parser = new PreviewParser(g, tokens);
		parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
		parser.setProfile(true);

		parser.removeErrorListeners();
		parser.addErrorListener(syntaxErrorListener);

		Rule start = g.getRule(startRuleName);
		if ( start==null ) {
			return null; // can't find start rule
		}
//		System.out.println("parse test ----------------------------");
		ParseTree t = parser.parse(start.index);

		if ( t!=null ) {
			return new ParsingResult(parser, t, syntaxErrorListener);
		}
		return null;
	}

	/*
	public static ATNConfigSet getReachableParsePositions(Grammar g,
														  LexerGrammar lg,
														  String startRuleName,
														  String inputText)
	{
		ParsingResult parsingResult;
		try {
			parsingResult = parseText(g, lg, startRuleName, null, inputText);
		}
		catch (IOException ioe) {
			ANTLRv4PluginController.LOG.info("getReachableParsePositions can't parse: "+ioe.getMessage());
			return null;
		}
		SyntaxErrorListener errs = parsingResult.syntaxErrorListener;
		System.out.println("errors="+errs);
		// presumption is that we'll get either InputMismatch or NoViableAlt since we're clipping input
		int nerrors = errs.getSyntaxErrors().size();
		if ( nerrors>0 ) {
			SyntaxError lastError = errs.getSyntaxErrors().get(nerrors - 1);
			SyntaxError error = lastError;
			RecognitionException e = lastError.getException();
			if ( e instanceof InputMismatchException ) { //  && ((NoViableAltException)e).getStartToken().getType()==Token.EOF ) {
				error = errs.getSyntaxErrors().get(nerrors - 2); // skip this one
				e = error.getException();
			}
			// it will always be NoViableAltException because we are asking it to
			// parse (until last token) within a lookahead decision.
			ATNConfigSet deadEndConfigs = ((NoViableAltException) e).getDeadEndConfigs();
			deadEndConfigs.getAlts();
			System.out.println("noviable "+deadEndConfigs);
			return deadEndConfigs;
		}

		return null;

//		try {
//			// Create a new parser interpreter to parse the ambiguous subphrase
//			ParserInterpreter parser;
//			if (originalParser instanceof ParserInterpreter) {
//				parser = ((ParserInterpreter) originalParser).copyFrom((ParserInterpreter) originalParser);
//			}
//			else {
//				char[] serializedAtn = ATNSerializer.getSerializedAsChars(originalParser.getATN());
//				ATN deserialized = new ATNDeserializer().deserialize(serializedAtn);
//				parser = new ParserInterpreter(originalParser.getGrammarFileName(),
//											   originalParser.getVocabulary(),
//											   Arrays.asList(originalParser.getRuleNames()),
//											   deserialized,
//											   tokens);
//			}
//
//			// Make sure that we don't get any error messages from using this temporary parser
//			parser.removeErrorListeners();
//			parser.removeParseListeners();
//			SyntaxErrorListener syntaxErrorListener = new SyntaxErrorListener();
//			parser.addErrorListener(syntaxErrorListener);
//			parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
//
//			ParserRuleContext t = parser.parse(startRuleIndex);
//			System.out.println(t.toStringTree(parser));
//
//			System.out.println("errors="+syntaxErrorListener);
//			// presumption is that we'll get either InputMismatch or NoViableAlt since we're clipping input
//			int nerrors = syntaxErrorListener.getSyntaxErrors().size();
//			if ( nerrors>0 ) {
//				SyntaxError lastError = syntaxErrorListener.getSyntaxErrors().get(nerrors - 1);
//				SyntaxError error = lastError;
//				RecognitionException e = lastError.getException();
//				if ( e instanceof InputMismatchException ) { //  && ((NoViableAltException)e).getStartToken().getType()==Token.EOF ) {
//					error = syntaxErrorListener.getSyntaxErrors().get(nerrors - 2); // skip this one
//					e = error.getException();
//				}
//				// it will always be NoViableAltException because we are asking it to
//				// parse (until last token) within a lookahead decision.
//				ATNConfigSet deadEndConfigs = ((NoViableAltException) e).getDeadEndConfigs();
//				deadEndConfigs.getAlts();
//				System.out.println("noviable "+deadEndConfigs);
//				return deadEndConfigs;
//			}
//		}
//		finally {
//			tokens.undoClip();
//			tokens.seek(saveTokenInputPosition);
//		}
//
//		return null;
	}
	*/

	public static Tool createANTLRToolForLoadingGrammars() {
		Tool antlr = new Tool();
		antlr.errMgr = new PluginIgnoreMissingTokensFileErrorManager(antlr);
		antlr.errMgr.setFormat("antlr");
		LoadGrammarsToolListener listener = new LoadGrammarsToolListener(antlr);
		antlr.removeListeners();
		antlr.addListener(listener);
		return antlr;
	}

	/** Get lexer and parser grammars */
	public static Grammar[] loadGrammars(String grammarFileName, Project project) {
		ANTLRv4PluginController.LOG.info("loadGrammars "+grammarFileName+" "+project.getName());
		Tool antlr = createANTLRToolForLoadingGrammars();
		LoadGrammarsToolListener listener = (LoadGrammarsToolListener)antlr.getListeners().get(0);

		// basically here I am mimicking the loadGrammar() method from Tool
		// so that I can check for an empty AST coming back.
		ConsoleView console = ANTLRv4PluginController.getInstance(project).getConsole();
		GrammarRootAST grammarRootAST = antlr.parseGrammar(grammarFileName);
		if ( grammarRootAST==null ) {
			File f = new File(grammarFileName);
			String msg = "Empty or bad grammar in file "+f.getName();
			console.print(msg+"\n", ConsoleViewContentType.ERROR_OUTPUT);
			return null;
		}
		// Create a grammar from the AST so we can figure out what type it is
		Grammar g = antlr.createGrammar(grammarRootAST);
		g.fileName = grammarFileName;

		// see if a lexer is hanging around somewhere; don't want implicit token defs to make us bail
		LexerGrammar lg = null;
		if ( g.getType()==ANTLRParser.PARSER ) {
			lg = loadLexerGrammarFor(g, project);
			if ( lg!=null ) {
				g.importVocab(lg);
			}
			else {
				lg = BAD_LEXER_GRAMMAR;
			}
		}

		antlr.process(g, false);
		if ( listener.grammarErrorMessages.size()!=0 ) {
			String msg = Utils.join(listener.grammarErrorMessages.iterator(), "\n");
			console.print(msg+"\n", ConsoleViewContentType.ERROR_OUTPUT);
			return null; // upon error, bail
		}

		// Examine's Grammar AST constructed by v3 for a v4 grammar.
		// Use ANTLR v3's ANTLRParser not ANTLRv4Parser from this plugin
		switch ( g.getType() ) {
			case ANTLRParser.PARSER :
				ANTLRv4PluginController.LOG.info("loadGrammars parser "+g.name);
				return new Grammar[] {lg, g};
			case ANTLRParser.LEXER :
				ANTLRv4PluginController.LOG.info("loadGrammars lexer "+g.name);
				lg = (LexerGrammar)g;
				return new Grammar[] {lg, null};
			case ANTLRParser.COMBINED :
				lg = g.getImplicitLexer();
				if ( lg==null ) {
					lg = BAD_LEXER_GRAMMAR;
				}
				ANTLRv4PluginController.LOG.info("loadGrammars combined: "+lg.name+", "+g.name);
				return new Grammar[] {lg, g};
		}
		ANTLRv4PluginController.LOG.info("loadGrammars invalid grammar type "+g.getTypeString()+" for "+g.name);
		return null;
	}

	/** Try to load a LexerGrammar given a parser grammar g. Derive lexer name
	 *  as:
	 *  	V given tokenVocab=V in grammar or
	 *   	XLexer given XParser.g4 filename or
	 *     	XLexer given grammar name X
	 */
	public static LexerGrammar loadLexerGrammarFor(Grammar g, Project project) {
		Tool antlr = createANTLRToolForLoadingGrammars();
		LoadGrammarsToolListener listener = (LoadGrammarsToolListener)antlr.getListeners().get(0);
		LexerGrammar lg = null;
		String lexerGrammarFileName;

		String vocabName = g.getOptionString("tokenVocab");
		if ( vocabName!=null ) {
			File f = new File(g.fileName);
			File lexerF = new File(f.getParentFile(), vocabName + ".g4");
			lexerGrammarFileName = lexerF.getAbsolutePath();
		}
		else {
			lexerGrammarFileName = getLexerNameFromParserFileName(g.fileName);
		}

		File lf = new File(lexerGrammarFileName);
		if ( lf.exists() ) {
			try {
				lg = (LexerGrammar)antlr.loadGrammar(lexerGrammarFileName);
			}
			catch (ClassCastException cce) {
				ANTLRv4PluginController.LOG.error("File "+lexerGrammarFileName+" isn't a lexer grammar", cce);
			}
			catch (Exception e) {
				String msg = null;
				if ( listener.grammarErrorMessages.size()!=0 ) {
					msg = ": "+listener.grammarErrorMessages.toString();
				}
				ANTLRv4PluginController.LOG.error("File "+lexerGrammarFileName+" couldn't be parsed as a lexer grammar"+msg, e);
			}
			if ( listener.grammarErrorMessages.size()!=0 ) {
				lg = null;
				ConsoleView console = ANTLRv4PluginController.getInstance(project).getConsole();
				String msg = Utils.join(listener.grammarErrorMessages.iterator(), "\n");
				console.print(msg+"\n", ConsoleViewContentType.ERROR_OUTPUT);
			}
		}
		return lg;
	}

	@NotNull
	public static String getLexerNameFromParserFileName(String parserFileName) {
		String lexerGrammarFileName;
		int i = parserFileName.indexOf("Parser.g4");
		if ( i>=0 ) { // is filename XParser.g4?
			lexerGrammarFileName = parserFileName.substring(0, i) + "Lexer.g4";
		}
		else { // if not, try using the grammar name, XLexer.g4
			File f = new File(parserFileName);
			String fname = f.getName();
			int dot = fname.lastIndexOf(".g4");
			String parserName = fname.substring(0, dot);
			File parentDir = f.getParentFile();
			lexerGrammarFileName = new File(parentDir, parserName+"Lexer.g4").getAbsolutePath();
		}
		return lexerGrammarFileName;
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


	/** Given an AmbiguityInfo object that contains information about an
	 *  ambiguous decision event, return the list of ambiguous parse trees.
	 *  An ambiguity occurs when a specific token sequence can be recognized
	 *  in more than one way by the grammar. These ambiguities are detected only
	 *  at decision points.
	 *
	 *  The list of trees includes the actual interpretation (that for
	 *  the minimum alternative number) and all ambiguous alternatives.
	 *  The actual interpretation is always first.
	 *
	 *  This method reuses the same physical input token stream used to
	 *  detect the ambiguity by the original parser in the first place.
	 *  This method resets/seeks within but does not alter originalParser.
	 *  The input position is restored upon exit from this method.
	 *  Parsers using a {@link UnbufferedTokenStream} may not be able to
	 *  perform the necessary save index() / seek(saved_index) operation.
	 *
	 *  The trees are rooted at the node whose start..stop token indices
	 *  include the start and stop indices of this ambiguity event. That is,
	 *  the trees returns will always include the complete ambiguous subphrase
	 *  identified by the ambiguity event.
	 *
	 *  Be aware that this method does NOT notify error or parse listeners as
	 *  it would trigger duplicate or otherwise unwanted events.
	 *
	 *  This uses a temporary ParserATNSimulator and a ParserInterpreter
	 *  so we don't mess up any statistics, event lists, etc...
	 *  The parse tree constructed while identifying/making ambiguityInfo is
	 *  not affected by this method as it creates a new parser interp to
	 *  get the ambiguous interpretations.
	 *
	 *  Nodes in the returned ambig trees are independent of the original parse
	 *  tree (constructed while identifying/creating ambiguityInfo).
	 *
	 *  @since 4.5.1
	 *
	 *  @param originalParser The parser used to create ambiguityInfo; it
	 *                        is not modified by this routine and can be either
	 *                        a generated or interpreted parser. It's token
	 *                        stream *is* reset/seek()'d.
	 *  @param ambiguityInfo  The information about an ambiguous decision event
	 *                        for which you want ambiguous parse trees.
	 *  @param startRuleIndex The start rule for the entire grammar, not
	 *                        the ambiguous decision. We re-parse the entire input
	 *                        and so we need the original start rule.
	 *
	 *  @return               The list of all possible interpretations of
	 *                        the input for the decision in ambiguityInfo.
	 *                        The actual interpretation chosen by the parser
	 *                        is always given first because this method
	 *                        retests the input in alternative order and
	 *                        ANTLR always resolves ambiguities by choosing
	 *                        the first alternative that matches the input.
	 *
	 *  @throws RecognitionException Throws upon syntax error while matching
	 *                               ambig input.
	 */
	public static List<ParserRuleContext> getAllPossibleParseTrees(PreviewParser originalParser,
																   TokenStream tokens,
																   int decision,
																   BitSet alts,
																   int startIndex,
																   int stopIndex,
																   int startRuleIndex)
		throws RecognitionException
	{
		List<ParserRuleContext> trees = new ArrayList<ParserRuleContext>();
//		int saveTokenInputPosition = tokens.index();
		try {
			// Create a new parser interpreter to parse the ambiguous subphrase
			PreviewParser parser = originalParser.copyFrom(originalParser);

			parser.setInputStream(tokens);

			// Make sure that we don't get any error messages from using this temporary parser
			parser.setErrorHandler(new BailErrorStrategy());
			parser.removeErrorListeners();
			parser.removeParseListeners();
			parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);

			// get ambig trees
			int alt = alts.nextSetBit(0);
			while (alt >= 0) {
				// re-parse entire input for all ambiguous alternatives
				// (don't have to do first as it's been parsed, but do again for simplicity
				//  using this temp parser.)
				parser.reset();
				parser.getTokenStream().seek(0); // rewind the input all the way for re-parsing
				parser.addDecisionOverride(decision, startIndex, alt);
				ParserRuleContext t = parser.parse(startRuleIndex);
				ParserRuleContext ambigSubTree =
					Trees.getRootOfSubtreeEnclosingRegion(t, startIndex, stopIndex);
				// Use higher of overridden decision tree or tree enclosing all tokens
				if ( isAncestorOf(parser.overrideDecisionContext, ambigSubTree) ) {
					ambigSubTree = parser.overrideDecisionContext;
				}
				trees.add(ambigSubTree);
				alt = alts.nextSetBit(alt + 1);
			}
		}
		finally {
//			originalParser.setInputStream(saveStream);
//			tokens.seek(saveTokenInputPosition);
		}

		return trees;
	}

		// we must parse the entire input now with decision overrides
		// we cannot parse a subset because it could be that a decision
		// above our decision of interest needs to read way past
		// lookaheadInfo.stopIndex. It seems like there is no escaping
		// the use of a full and complete token stream if we are
		// resetting to token index 0 and re-parsing from the start symbol.
		// It's not easy to restart parsing somewhere in the middle like a
		// continuation because our call stack does not match the
		// tree stack because of left recursive rule rewriting. grrrr!

	@NotNull
	public static List<ParserRuleContext> getLookaheadParseTrees(ParserInterpreter originalParser,
																 int startRuleIndex,
																 int decision,
																 int startIndex,
																 int stopIndex)
	{
		PreviewParser parser = (PreviewParser)originalParser.copyFrom(originalParser);
		TokenStreamSubset tokens = (TokenStreamSubset) parser.getTokenStream();
//		parser.setErrorHandler(new BailErrorStrategy());
//		parser.removeErrorListeners();
		parser.removeParseListeners();
		parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);

		DecisionState decisionState = originalParser.getATN().decisionToState.get(decision);
		Set<Integer> altSet = new HashSet<>();
		for (int i=1; i<=decisionState.getTransitions().length; i++) {
			altSet.add(i);
		}

		// First, figure out which alts are viable at the start of the lookahead
		// We can figure that out by simply looking at the alternatives within
		// the start state of the decision DFA.

		System.out.println("dfa alts = "+altSet+", range = "+startIndex+".."+stopIndex);

		tokens.seek(0); // rewind the input all the way for re-parsing

		// Now, we re-parse from the beginning until
		// the last lookahead token.

		List<ParserRuleContext> trees = new ArrayList<>();
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		InterpreterRuleContextTextProvider nodeTextProvider =
			new InterpreterRuleContextTextProvider(parser.getRuleNames());
		for (Integer alt : altSet) {
			// re-parse entire input for all ambiguous alternatives
			// (don't have to do first as it's been parsed, but do again for simplicity
			//  using this temp parser.)
			parser.reset();
			TrackingErrorStrategy errorHandler = new TrackingErrorStrategy();
			parser.setErrorHandler(errorHandler);
			System.out.print("parsing alt " + alt);
			parser.addDecisionOverride(decision, startIndex, alt);
			ParserRuleContext tt = parser.parse(startRuleIndex);
			System.out.println("first error "+errorHandler.firstErrorTokenIndex);
			System.out.println("ctx at override: "+Trees.toStringTree(parser.overrideDecisionContext, nodeTextProvider));
			System.out.print("\t\t" + Trees.toStringTree(tt, nodeTextProvider));
			int stopTreeAt = stopIndex;
			if ( errorHandler.firstErrorTokenIndex>=0 ) {
				stopTreeAt = errorHandler.firstErrorTokenIndex; // cut off rest at first error
			}
			ParserRuleContext subtree =
				Trees.getRootOfSubtreeEnclosingRegion(tt,
													  startIndex,
													  stopTreeAt);
			// Use higher of overridden decision tree or tree enclosing all tokens
			if ( isAncestorOf(parser.overrideDecisionContext, subtree) ) {
				subtree = parser.overrideDecisionContext;
			}
			System.out.println("\t\t"+Trees.toStringTree(subtree, nodeTextProvider));
			stripChildrenOutOfRange(tokens, subtree, startIndex, stopTreeAt);
//			System.out.println("\t\t" + Trees.toStringTree(subtree, nodeTextProvider));
//			Interval range = subtree.getSourceInterval();
//			if ( errorHandler.firstErrorTokenIndex>=0 ) {
//				range.b = errorHandler.firstErrorTokenIndex; // cut off rest at first error
//			}
//			System.out.println("range after strip: " + range);
//			if ( range.a>=0 ) min = Math.min(min, range.a);
//			if ( range.b>=0 ) max = Math.max(max, range.b);

//			trees.add(tt); // add unmolested tree as we'll adjust below; we just compute max range here
			trees.add(subtree);
//			System.out.println("alt "+alt+": "+Trees.toStringTree(subtree, nodeTextProvider));
		}

		return trees;
//		System.out.println("min/max = "+min+"/"+max);
//
//		List<ParserRuleContext> strippedTrees = new ArrayList<>();
//		for (int i = 0; i < trees.size(); i++) {
//			ParserRuleContext t = trees.get(i);
//
//			ParserRuleContext subtree =
//				Trees.getRootOfSubtreeEnclosingRegion(t,
//													  min,
//													  max);
//			System.out.println("enclosing "+Trees.toStringTree(subtree, nodeTextProvider));
////			stripChildrenOutOfRange(tokens,	subtree, startIndex, stopIndex);
//			System.out.println("stripped "+Trees.toStringTree(subtree, nodeTextProvider));
//			strippedTrees.add(subtree);
//		}
//		return strippedTrees;
	}

	/** Replace any subtree siblings of root that are completely to left
	 *  or right of lookahead range with a "..." node. The source interval
	 *  for t is reset if needed.
	 *
	 *  WARNING: destructive to t.
	 */
	public static void stripChildrenOutOfRange(TokenStreamSubset tokens,
											   ParserRuleContext t,
											   int startIndex,
											   int stopIndex)
	{
		if ( t==null ) return;

//		for (int i = 0; i < t.getChildCount(); i++) {
//			ParseTree child = t.getChild(i);
//			Interval range = child.getSourceInterval();
//			if ( child instanceof ParserRuleContext && (range.b < startIndex || range.a > stopIndex) ) {
//				CommonToken abbrev = new CommonToken(Token.INVALID_TYPE, "...");
//				if ( range.b < startIndex ) { // set to first token of next child so we ignore "..."
////					t.start = t.getChild(i + 1)
////					abbrev.setTokenIndex(t.start);
//				}
//				else { // range.a > stopIndex so set to last token of previous child so we ignore "..."
////					abbrev.setTokenIndex(t.getChild(i-1).getSourceInterval().b);
//				}
//				t.children.set(i, new TerminalNodeImpl(abbrev));
//			}
//		}
//
		for (int i = 0; i < t.getChildCount(); i++) {
			ParseTree child = t.getChild(i);
			Interval range = child.getSourceInterval();
			if ( child instanceof ParserRuleContext && (range.b < startIndex || range.a > stopIndex) ) {
				if ( findOverriddenDecisionRoot(child)==null ) { // replace only if subtree doesn't have displayed root
					CommonToken abbrev = new CommonToken(Token.INVALID_TYPE, "...");
					t.children.set(i, new TerminalNodeImpl(abbrev));
				}
			}
		}

//		// strip on left first (and separately from right)
//		int lastAbbrev = -1;
//		for (int i = 0; i < t.getChildCount(); i++) {
//			ParseTree child = t.getChild(i);
//			Interval range = child.getSourceInterval();
//			if ( child instanceof ParserRuleContext && range.b < startIndex ) {
//				CommonToken abbrev = new CommonToken(Token.INVALID_TYPE, "...");
//				t.children.set(i, new TerminalNodeImpl(abbrev));
//				lastAbbrev = i;
//			}
//		}
//		// strip away everything but one "..." on left
//		if ( lastAbbrev>=0 ) t.children = t.children.subList(lastAbbrev, t.getChildCount());
//
//		// strip on right
//		int firstAbbrev = Integer.MAX_VALUE;
//		int firstError = Integer.MAX_VALUE;
//		for (int i = t.getChildCount()-1; i>=0; --i) {
//			ParseTree child = t.getChild(i);
//			if ( child instanceof ErrorNode ) {
//				firstError = i;
//			}
//			Interval range = child.getSourceInterval();
//			if ( child instanceof ParserRuleContext && range.a > stopIndex ) {
//				CommonToken abbrev = new CommonToken(Token.INVALID_TYPE, "...");
//				t.children.set(i, new TerminalNodeImpl(abbrev));
//				firstAbbrev = i;
//			}
//		}
//		// strip away everything but one "..." on right
////		int last = Math.min(firstAbbrev, firstError);
//		int last = firstAbbrev;
//		if ( last!=Integer.MAX_VALUE ) t.children = t.children.subList(0, last + 1);
//
//		int min = Integer.MAX_VALUE;
//		int max = Integer.MIN_VALUE;
//		for (int i = 0; i < t.getChildCount(); i++) {
//			ParseTree child = t.getChild(i);
//			Interval range = child.getSourceInterval();
//			if ( range.a>=0 ) min = Math.min(min, range.a);
//			if ( range.b>=0 ) max = Math.max(max, range.b);
//		}
//
//		if ( min != Integer.MAX_VALUE ) t.start = tokens.get(min);
//		if ( max != Integer.MIN_VALUE ) t.stop = tokens.get(max);
	}

	/** Return true if t is u's parent or a node on path to root from u.
	 *  Use == not equals().
	 */
	public static boolean isAncestorOf(Tree t, Tree u) {
		if ( t==null || u==null || t.getParent()==null ) return false;
		Tree p = u.getParent();
		while ( p!=null ) {
			if ( t == p ) return true;
			p = p.getParent();
		}
		return false;
	}

	public static Tree findTreeSuchThat(Tree t, Predicate<Tree> pred) {
		if ( pred.apply(t) ) return t;

		int n = t.getChildCount();
		for (int i = 0 ; i < n ; i++){
			Tree u = findTreeSuchThat(t.getChild(i), pred);
			if ( u!=null ) return u;
		}
		return null;
	}

	public static Tree findOverriddenDecisionRoot(Tree ctx) {
		return findTreeSuchThat(ctx, new Predicate<Tree>() {
			@Override
			public boolean apply(Tree t) {
				return t instanceof PreviewInterpreterRuleContext ?
					((PreviewInterpreterRuleContext) t).isDecisionOverrideRoot() :
					false;
			}
		});
	}

	public static class TrackingErrorStrategy extends DefaultErrorStrategy {
		public int firstErrorTokenIndex = -1;
		@Override
		public void recover(Parser recognizer, RecognitionException e) {
			int errIndex = recognizer.getInputStream().index();
			if ( firstErrorTokenIndex == -1 ) {
				firstErrorTokenIndex = errIndex; // latch
			}
			System.err.println("recover: error at " + errIndex);
			TokenStream input = recognizer.getInputStream();
			if ( input.index()<input.size()-1 ) { // don't consume() eof
				recognizer.consume(); // just kill this bad token and let it continue.
			}
		}

		@Override
		public Token recoverInline(Parser recognizer) throws RecognitionException {
			int errIndex = recognizer.getInputStream().index();
			if ( firstErrorTokenIndex == -1 ) {
				firstErrorTokenIndex = errIndex; // latch
			}
			System.err.println("recoverInline: error at " + errIndex);
			InputMismatchException e = new InputMismatchException(recognizer);
			TokenStream input = recognizer.getInputStream(); // seek EOF
			input.seek(input.size() - 1);
			throw e; // throw after seek so exception has bad token
		}

		@Override
		public void sync(Parser recognizer) { }
	}
}
