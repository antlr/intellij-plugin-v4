package org.antlr.intellij.plugin.psi.iter;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Created by jason on 2/17/15.
 */
public abstract class Tokens implements ASTFilter, PsiFilter {
    public abstract boolean contains(IElementType type);

    @Override
    public boolean acceptNode(ASTNode node) {
        return contains(node.getElementType());
    }

    @Override
    public boolean acceptElement(PsiElement element) {
        ASTNode node = element.getNode();
        return node != null && contains(node.getElementType());
    }

    static final Comparator<IElementType> ELEMENT_TYPE_COMPARATOR = new Comparator<IElementType>() {
        @Override
        public int compare(@NotNull IElementType a, @NotNull IElementType b) {
            return a.getIndex() - b.getIndex();
        }
    };


    public static Tokens of(IElementType... types) {
        TreeSet<IElementType> typeSet = new TreeSet<IElementType>(ELEMENT_TYPE_COMPARATOR);
        Collections.addAll(typeSet, types);
        IElementType[] arr = typeSet.isEmpty() ? IElementType.EMPTY_ARRAY : typeSet.toArray(new IElementType[typeSet.size()]);
        switch (typeSet.size()) {
            case 0:
                return NONE;
            case 1:
                return one(arr[0]);
            case 2:
                return two(arr[0], arr[1]);
            case 3:
                return three(arr[0], arr[1], arr[2]);
            case 4:
                return four(arr[0], arr[1], arr[2], arr[3]);
            default:
                return fromTokenSet(arr);

        }
    }

    static final Tokens NONE = new Tokens() {
        @Override
        public boolean contains(IElementType type) {
            return false;
        }
    };


    static Tokens one(final IElementType t) {
        return new Tokens() {
            @Override
            public boolean contains(IElementType type) {
                return type == t;
            }
        };
    }

    static Tokens two(final IElementType t0, final IElementType t1) {
        return new Tokens() {
            @Override
            public boolean contains(IElementType type) {
                return type == t0 || type == t1;
            }
        };
    }

    static Tokens three(final IElementType t0, final IElementType t1, final IElementType t2) {
        return new Tokens() {
            @Override
            public boolean contains(IElementType type) {
                return type == t0 || type == t1 || type == t2;
            }
        };
    }

    static Tokens four(final IElementType t0, final IElementType t1, final IElementType t2, final IElementType t3) {
        return new Tokens() {
            @Override
            public boolean contains(IElementType type) {
                return type == t0 || type == t1 || type == t2 || type == t3;
            }
        };
    }

    static Tokens fromTokenSet(final IElementType[] types) {
        return new Tokens() {
            final TokenSet tokenSet = TokenSet.create(types);

            @Override
            public boolean contains(IElementType type) {
                return tokenSet.contains(type);
            }
        };
    }

}
