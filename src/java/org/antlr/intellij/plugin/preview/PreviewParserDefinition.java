package org.antlr.intellij.plugin.preview;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
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
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.tool.Rule;
import org.jetbrains.annotations.NotNull;

/*  Actually, I just realized that we have a problem. The parser definition
 *  object is not controlled by me and the create lexer and parser methods
 *  might be called by different threads. I'm assuming that only one instance
 *  of that definition object is created or I can't rely on being
 *  different objects per grammar. Ok, I will assume that it is not possible
 *  to switch grammars fast enough that intellij will ask for a lexer,
 *  a new grammar appears, and then it asks for a parser but gets one
 *  for a different grammar.
 */
public class PreviewParserDefinition implements ParserDefinition {
	public static final IFileElementType FILE =	new IFileElementType(PreviewLanguage.INSTANCE);

	SyntaxErrorListener syntaxErrorListener = null;

	@NotNull
	@Override
	public Lexer createLexer(final Project project) {
		if ( syntaxErrorListener==null ) {
			setErrorListener(project);
		}
		syntaxErrorListener.getSyntaxErrors().clear();
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState();

		String inputText = ANTLRv4PluginController.getInstance(project).getInputText();
		ANTLRInputStream input = new ANTLRInputStream(inputText);

		System.out.println("parsing with "+previewState.grammarFileName);

		LexerInterpreter lexEngine = previewState.lg.createLexerInterpreter(input);
		lexEngine.removeErrorListeners();
		lexEngine.addErrorListener(syntaxErrorListener);

		return new PreviewLexer(PreviewLanguage.INSTANCE, lexEngine);
	}

	@Override
	public PsiParser createParser(Project project) {
		if ( syntaxErrorListener==null ) {
			setErrorListener(project);
		}
		PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState();
		Rule start = previewState.g.getRule(previewState.startRuleName);
		return new PreviewParser(project, start.index, syntaxErrorListener);
	}

	public void setErrorListener(final Project project) {
		syntaxErrorListener = new SyntaxErrorListener(project) {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
									int line, int charPositionInLine,
									String msg, RecognitionException e) {
				msg = "line " + line + ":" + charPositionInLine + " " + msg + "\n";
				ANTLRv4PluginController.getInstance(project).getPreviewPanel().parseError(msg);
				super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
			}
		};
	}

	@Override
	public IFileElementType getFileNodeType() {
		return FILE;
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getCommentTokens() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getStringLiteralElements() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public PsiElement createElement(ASTNode node) {
		return new ASTWrapperPsiElement(node);
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider) {
		return new PreviewFileRoot(viewProvider);
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
		return SpaceRequirements.MAY;
	}
}
