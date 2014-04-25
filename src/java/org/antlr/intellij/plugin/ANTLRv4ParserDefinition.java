package org.antlr.intellij.plugin;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.adaptor.parser.SyntaxError;
import org.antlr.intellij.plugin.adaptors.ANTLRv4GrammarParser;
import org.antlr.intellij.plugin.adaptors.ANTLRv4LexerAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.jetbrains.annotations.NotNull;

/** The general interface between IDEA and ANTLR. */
public class ANTLRv4ParserDefinition implements ParserDefinition {
	public static final IFileElementType FILE =
		new IFileElementType(ANTLRv4Language.INSTANCE);

	@NotNull
	@Override
	public Lexer createLexer(Project project) {
		ANTLRv4Lexer lexer = new ANTLRv4Lexer(null);
		return new ANTLRv4LexerAdaptor(ANTLRv4Language.INSTANCE, lexer);
	}

	@NotNull
	public PsiParser createParser(final Project project) {
		return new ANTLRv4GrammarParser();
	}

	@NotNull
	public TokenSet getWhitespaceTokens() {
		return ANTLRv4TokenTypes.WHITESPACES;
	}

	@NotNull
	public TokenSet getCommentTokens() {
		return ANTLRv4TokenTypes.COMMENTS;
	}

	@NotNull
	public TokenSet getStringLiteralElements() {
		return TokenSet.EMPTY;
	}

	@Override
	public IFileElementType getFileNodeType() {
		return FILE;
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider) {
		return new ANTLRv4FileRoot(viewProvider);
	}

	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
		return SpaceRequirements.MAY;
	}

	/** Convert from internal parse node (AST they call it) to final PSI node. This
	 *  converts only internal rule nodes apparently, not leaf nodes. Leaves
	 *  are just tokens I guess.
	 */
	@NotNull
	public PsiElement createElement(ASTNode node) {
		return ANTLRv4ASTFactory.createInternalParseTreeNode(node);
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

//	public static int previousTokenOnChannel(int i, int channel) {
//		while ( i>=0 && tokens.get(i).getChannel()!=channel ) {
//			i--;
//		}
//		return i;
//	}

	public static Token getTokenUnderCursor(CommonTokenStream tokens, int offset) {
		Token tokenUnderCursor = null;
		for (Token t : tokens.getTokens()) {
			int begin = t.getStartIndex();
			int end = t.getStopIndex()+1;
//				System.out.println("test "+t+" for "+offset);
			if ( offset >= begin && offset < end ) {
				tokenUnderCursor = t;
				break;
			}
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

	public static CommonTokenStream tokenize(String text) {
		ANTLRInputStream input = new ANTLRInputStream(text);
		ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		tokens.fill();
		return tokens;

//		ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
//		parser.removeErrorListeners();
//		SyntaxErrorListener listener = new SyntaxErrorListener();
//		parser.addErrorListener(listener);
//
//		ParseTree t = parser.grammarSpec();
	}
}
