package org.antlr.intellij.plugin.adaptors;


import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.jetbrains.annotations.NotNull;

public abstract class ParserAdaptor implements PsiParser {
	protected Parser parser;

	public ParserAdaptor(AdaptorParserBase parser) {
		this.parser = parser;
	}

	@NotNull
	@Override
	public ASTNode parse(IElementType root, PsiBuilder builder) {
		// mark now so we can rewind after grabbing all tokens
		PsiBuilder.Marker rootMarker = builder.mark();

		// Pull from an IDEATokenSourceAdaptor object (which has already
		// cached all tokens using antlr lexer).
		IDEATokenSourceAdaptor src = new IDEATokenSourceAdaptor(builder);
		CommonTokenStream tokens = new CommonTokenStream(src);

		parser.addErrorListener(new ParserErrorAdaptor(builder));

		parser.setTokenStream(tokens);
		parser.setBuildParseTree(false); // don't waste time doing this
		builder.setDebugMode(true);

		tokens.fill();
		rootMarker.rollbackTo();
		System.out.println("tokens=" + tokens.getTokens());

		rootMarker = builder.mark();

		// call ANTLR parser now
		parse(parser, builder);

		if (root != null) {
			rootMarker.done(root);
		}
		return builder.getTreeBuilt();
	}

	public abstract void parse(Parser parser, PsiBuilder builder);
}
