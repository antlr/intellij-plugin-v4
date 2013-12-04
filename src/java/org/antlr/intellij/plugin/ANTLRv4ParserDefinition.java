package org.antlr.intellij.plugin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.plugin.adaptors.ANTLRUtils;
import org.antlr.intellij.plugin.adaptors.LexerAdaptor;
import org.antlr.intellij.plugin.adaptors.ParserAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypeAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.IdRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;
import org.antlr.intellij.plugin.psi.RulesNode;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.jetbrains.annotations.NotNull;

/** The general interface between IDEA and ANTLR. */
public class ANTLRv4ParserDefinition implements ParserDefinition {
	public static final IFileElementType FILE =
		new IFileElementType(Language.<ANTLRv4Language>findInstance(ANTLRv4Language.class));

	@NotNull
	@Override
	public Lexer createLexer(Project project) {
		final ANTLRv4Lexer lexer = new ANTLRv4Lexer(null);

		LexerATNSimulator sim =
			ANTLRUtils.getLexerATNSimulator(lexer, ANTLRv4Lexer._ATN, lexer.getInterpreter().decisionToDFA,
											lexer.getInterpreter().getSharedContextCache());
		lexer.setInterpreter(sim);
		return new LexerAdaptor(lexer);
	}

	@NotNull
	public PsiParser createParser(final Project project) {
		ANTLRv4Parser parser = new ANTLRv4Parser(null);
		return new ParserAdaptor(parser) {
			@Override
			public void parse(Parser parser, IElementType root, PsiBuilder builder) {
				((ANTLRv4Parser)parser).builder = builder;
				if ( root instanceof IFileElementType ) {
 					((ANTLRv4Parser)parser).grammarSpec();
				}
				else if ( root==ANTLRv4TokenTypes.TOKEN_REF ||
						  root==ANTLRv4TokenTypes.RULE_REF )
				{
					((ANTLRv4Parser)parser).atom();
				}
			}
		};
	}

	@NotNull
	public TokenSet getWhitespaceTokens() {
		return ANTLRv4TokenTypeAdaptor.WHITE_SPACES;
	}

	@NotNull
	public TokenSet getCommentTokens() {
		return ANTLRv4TokenTypeAdaptor.COMMENTS;
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
		IElementType elementType = node.getElementType();
		PsiElement t;
		if ( elementType==ANTLRv4TokenTypes.rules ) {
			t = new RulesNode(node);
		}
		else if ( elementType==ANTLRv4TokenTypes.parserRuleSpec ) {
			t = new ParserRuleSpecNode(node);
		}
		else if ( elementType==ANTLRv4TokenTypes.lexerRule ) {
			t = new LexerRuleSpecNode(node);
		}
		else if ( elementType==ANTLRv4TokenTypes.id ) {
			t = new IdRefNode(node);
		}
		else if ( elementType==ANTLRv4TokenTypes.grammarSpec ) {
			t = new GrammarSpecNode(node);
		}
		else {
			t = new ASTWrapperPsiElement(node);
		}
//		System.out.println("PSI createElement "+t+" from "+elementType);
		return t;
	}
}
