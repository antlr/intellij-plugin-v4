package org.antlr.intellij.plugin.parsing;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.PluginIgnoreMissingTokensFileErrorManager;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarProperties;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.v4.Tool;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.tool.ErrorType;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getGrammarProperties;

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
		while ( t.getChannel()!=Token.DEFAULT_CHANNEL ) {  // Parser must parse tokens on DEFAULT_CHANNEL
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
		while ( t.getChannel()!=Token.DEFAULT_CHANNEL ) { // Parser must parse tokens on DEFAULT_CHANNEL
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
		Comparator<Token> cmp = (a, b) -> {
			if ( a.getStopIndex() < b.getStartIndex() ) return -1;
			if ( a.getStartIndex() > b.getStopIndex() ) return 1;
			return 0;
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
					new Pair<>(tokenSource, inputStream),
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

	public static CommonTokenStream tokenizeANTLRGrammar(String text) {
		CodePointCharStream input = CharStreams.fromString(text);
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
	    CodePointCharStream input = CharStreams.fromString(text);
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

	public static ParsingResult parseText(Grammar g,
										  LexerGrammar lg,
										  String startRuleName,
										  final VirtualFile grammarFile,
										  String inputText,
										  Project project) {
		if ( g==null || lg==null ) {
			ANTLRv4PluginController.LOG.info("parseText can't parse: missing lexer or parser no Grammar object for " +
					(grammarFile != null ? grammarFile.getName() : "<unknown file>"));
			return null;
		}

		ANTLRv4GrammarProperties grammarProperties = getGrammarProperties(project, grammarFile);
		CharStream input = grammarProperties.getCaseChangingStrategy()
				.applyTo(CharStreams.fromString(inputText, grammarFile.getPath()));
		LexerInterpreter lexEngine;
		lexEngine = lg.createLexerInterpreter(input);
		SyntaxErrorListener syntaxErrorListener = new SyntaxErrorListener();
		lexEngine.removeErrorListeners();
		lexEngine.addErrorListener(syntaxErrorListener);
		CommonTokenStream tokens = new TokenStreamSubset(lexEngine);
		return parseText(g, lg, startRuleName, syntaxErrorListener, tokens, 0);
	}

	private static ParsingResult parseText(Grammar g,
										  LexerGrammar lg,
										  String startRuleName,
										  SyntaxErrorListener syntaxErrorListener,
										  TokenStream tokens,
										  int startIndex) {
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
		ParseTree t = parser.parse(start.index);

		if ( t!=null ) {
			return new ParsingResult(parser, t, syntaxErrorListener);
		}
		return null;
	}

	public static Tool createANTLRToolForLoadingGrammars(ANTLRv4GrammarProperties grammarProperties) {
		Tool antlr = new Tool();
		antlr.errMgr = new PluginIgnoreMissingTokensFileErrorManager(antlr);
		antlr.errMgr.setFormat("antlr");
		LoadGrammarsToolListener listener = new LoadGrammarsToolListener(antlr);
		antlr.removeListeners();
		antlr.addListener(listener);
		antlr.libDirectory = grammarProperties.getLibDir();
		return antlr;
	}

	/** Get lexer and parser grammars */
	public static Grammar[] loadGrammars(VirtualFile grammarFile, Project project) {
		ANTLRv4PluginController.LOG.info("loadGrammars "+grammarFile.getPath()+" "+project.getName());
		Tool antlr = createANTLRToolForLoadingGrammars(getGrammarProperties(project, grammarFile));
		LoadGrammarsToolListener listener = (LoadGrammarsToolListener)antlr.getListeners().get(0);

		ConsoleView console = ANTLRv4PluginController.getInstance(project).getConsole();
		Grammar g = loadGrammar(grammarFile, antlr);
		if (g == null) {
			reportBadGrammar(grammarFile, console);
			return null;
		}

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
					String msg = "No implicit lexer grammar found in combined grammar " + g.name
						+ ". Did you mean to declare a `parser grammar` instead?\n";
					console.print(msg, ConsoleViewContentType.ERROR_OUTPUT);
					lg = BAD_LEXER_GRAMMAR;
				}
				ANTLRv4PluginController.LOG.info("loadGrammars combined: "+lg.name+", "+g.name);
				return new Grammar[] {lg, g};
		}
		ANTLRv4PluginController.LOG.info("loadGrammars invalid grammar type "+g.getTypeString()+" for "+g.name);
		return null;
	}

	private static void reportBadGrammar(VirtualFile grammarFile, ConsoleView console) {
		String msg = "Empty or bad grammar in file "+grammarFile.getName();
		console.print(msg+"\n", ConsoleViewContentType.ERROR_OUTPUT);
	}

	@Nullable
	private static Grammar loadGrammar(VirtualFile grammarFile, Tool antlr) {
		// basically here I am mimicking the loadGrammar() method from Tool
		// so that I can check for an empty AST coming back.
		GrammarRootAST grammarRootAST = parseGrammar(antlr, grammarFile);
		if ( grammarRootAST==null ) {
			return null;
		}

		// Create a grammar from the AST so we can figure out what type it is
		Grammar g = antlr.createGrammar(grammarRootAST);
		g.fileName = grammarFile.getPath();

		return g;
	}

	public static GrammarRootAST parseGrammar(Tool antlr, VirtualFile grammarFile) {
		try {
			Document document = FileDocumentManager.getInstance().getDocument(grammarFile);
			String grammarText = document != null ? document.getText() : new String(grammarFile.contentsToByteArray());

			ANTLRStringStream in = new ANTLRStringStream(grammarText);
			in.name = grammarFile.getPath();
			return antlr.parse(grammarFile.getPath(), in);
		}
		catch (IOException ioe) {
			antlr.errMgr.toolError(ErrorType.CANNOT_OPEN_FILE, ioe, grammarFile);
		}
		return null;
	}

	/** Try to load a LexerGrammar given a parser grammar g. Derive lexer name
	 *  as:
	 *  	V given tokenVocab=V in grammar or
	 *   	XLexer given XParser.g4 filename or
	 *     	XLexer given grammar name X
	 */
	public static LexerGrammar loadLexerGrammarFor(Grammar g, Project project) {
		Tool antlr = createANTLRToolForLoadingGrammars(getGrammarProperties(project, g.fileName));
		LoadGrammarsToolListener listener = (LoadGrammarsToolListener)antlr.getListeners().get(0);
		LexerGrammar lg = null;
		VirtualFile lexerGrammarFile;

		String vocabName = g.getOptionString("tokenVocab");
		if ( vocabName!=null ) {
			VirtualFile grammarFile = LocalFileSystem.getInstance().findFileByIoFile(new File(g.fileName));
			lexerGrammarFile = VfsUtil.findRelativeFile(grammarFile == null ? null : grammarFile.getParent(), vocabName + ".g4");
		}
		else {
			lexerGrammarFile = LocalFileSystem.getInstance().findFileByIoFile(new File(getLexerNameFromParserFileName(g.fileName)));
		}

		if ( lexerGrammarFile != null && lexerGrammarFile.exists() ) {
			ConsoleView console = ANTLRv4PluginController.getInstance(project).getConsole();

			try {
				lg = (LexerGrammar) loadGrammar(lexerGrammarFile, antlr);
				if ( lg!=null ) {
					antlr.process(lg, false);
				}
				else {
					reportBadGrammar(lexerGrammarFile, console);
				}
			}
			catch (ClassCastException cce) {
				ANTLRv4PluginController.LOG.error("File "+lexerGrammarFile+" isn't a lexer grammar", cce);
			}
			catch (Exception e) {
				String msg = null;
				if ( listener.grammarErrorMessages.size()!=0 ) {
					msg = ": "+listener.grammarErrorMessages.toString();
				}
				ANTLRv4PluginController.LOG.error("File "+lexerGrammarFile+" couldn't be parsed as a lexer grammar"+msg, e);
			}
			if ( listener.grammarErrorMessages.size()!=0 ) {
				lg = null;
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

	public static Tree findOverriddenDecisionRoot(Tree ctx) {
		return Trees.findNodeSuchThat(
				ctx,
				t -> t instanceof PreviewInterpreterRuleContext && ((PreviewInterpreterRuleContext) t).isDecisionOverrideRoot()
		);
	}

	public static List<TerminalNode> getAllLeaves(Tree t) {
		List<TerminalNode> leaves = new ArrayList<>();
		_getAllLeaves(t, leaves);
		return leaves;
	}

	private static void _getAllLeaves(Tree t, List<TerminalNode> leaves) {
		int n = t.getChildCount();
		if ( t instanceof TerminalNode ) {
			Token tok = ((TerminalNode)t).getSymbol();
			if ( tok.getType() != Token.INVALID_TYPE ) {
				leaves.add((TerminalNode) t);
			}
			return;
		}
		for (int i = 0 ; i < n ; i++){
			_getAllLeaves(t.getChild(i), leaves);
		}
	}

	/** Get ancestors where the first element of the list is the parent of t */
	public static List<? extends Tree> getAncestors(Tree t) {
		if ( t.getParent()==null ) return Collections.emptyList();
		List<Tree> ancestors = new ArrayList<>();
		t = t.getParent();
		while ( t!=null ) {
			ancestors.add(t); // insert at start
			t = t.getParent();
		}
		return ancestors;
	}

}
