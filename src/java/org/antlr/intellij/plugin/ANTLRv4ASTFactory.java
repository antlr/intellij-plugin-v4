package org.antlr.intellij.plugin;

import com.intellij.lang.ASTFactory;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;

public class ANTLRv4ASTFactory extends ASTFactory {
	/** Create a FileElement for root or a parse tree CompositeElement (not
	 *  PSI) for the token. This impl is more or less the default.
	 */
    @Override
    public CompositeElement createComposite(IElementType type) {
        if (type instanceof IFileElementType) {
            return new FileElement(type, null);
		}
        return new CompositeElement(type);
    }

	/** Create PSI nodes out of tokens so even parse tree sees them as such.
	 *  Does not see whitespace tokens.
	 */
    @Override
    public LeafElement createLeaf(IElementType type, CharSequence text) {
//		if ( type.toString().equals("ID") ) {
//			return new IDRefElement(type, text);
//		}
		return new LeafPsiElement(type, text);
    }
}
