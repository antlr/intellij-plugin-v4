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
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;
import org.antlr.intellij.plugin.PluginIgnoreMissingTokensFileErrorManager;
import org.antlr.v4.Tool;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.jetbrains.annotations.NotNull;

public class PreviewParserDefinition implements ParserDefinition {
	public static final IFileElementType FILE =
		new IFileElementType(PreviewLanguage.INSTANCE);

	@NotNull
	@Override
	public Lexer createLexer(Project project) {
		Tool antlr = new Tool();
		antlr.errMgr = new PluginIgnoreMissingTokensFileErrorManager(antlr);
		antlr.errMgr.setFormat("antlr");
//		ANTLRv4ProjectComponent.MyANTLRToolListener listener =
//			new ANTLRv4ProjectComponent.MyANTLRToolListener(antlr);
//		antlr.addListener(listener);

		String inputText = ANTLRv4ProjectComponent.getInstance(project).getInputText();
		ANTLRInputStream input = new ANTLRInputStream(inputText);

		String grammarFileName = ANTLRv4ProjectComponent.getInstance(project).getGrammarFileName();

		System.out.println("parsing with "+grammarFileName);

//		if ( grammarFileName==null ) {
//			// they haven't done "test ANTLR rule" event yet so we don't know which grammar to use for input.
//			// just send back any lexer.
//			List<TokenParser> p = new ArrayList<TokenParser>();
//			p.add(new WhitespaceParser());
//			return new AbstractCustomLexer(p);
//		}

		Grammar lg = ANTLRv4ProjectComponent.getInstance(project).getLexerGrammar();
		LexerInterpreter lexEngine = lg.createLexerInterpreter(input);
		final ConsoleErrorListener syntaxErrorListener = new ConsoleErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
				super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
			}
		};
		lexEngine.removeErrorListeners();
		lexEngine.addErrorListener(syntaxErrorListener);

		return new PreviewLexer(PreviewLanguage.INSTANCE, lexEngine);
	}

	@Override
	public PsiParser createParser(Project project) {
		Grammar g = ANTLRv4ProjectComponent.getInstance(project).getParserGrammar();
		PreviewPanel previewPanel = ANTLRv4ProjectComponent.getInstance(project).getPreviewPanel();
		Rule start = g.getRule(previewPanel.startRuleName);
		return new PreviewParser(project, start.index);
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
