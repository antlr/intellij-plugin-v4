package org.antlr.intellij.plugin.psi.iter;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Created by jason on 2/17/15.
 */
public abstract class PsiIterable<E extends PsiElement> implements Iterable<E> {


    private final Iterable<E> myIterable;

    protected PsiIterable() {
        myIterable = this;
    }

    PsiIterable(@NotNull Iterable<E> iterable) {
        myIterable = iterable;
    }


    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> PsiIterable<T> from(final Iterable<T> iterable) {
        if (iterable instanceof PsiIterable) return (PsiIterable) iterable;
        else return new PsiIterable<T>(iterable) {
            @NotNull
            @Override
            public Iterator<T> iterator() {
                return iterable.iterator();
            }
        };
    }

    public static PsiIterable<?> depthFirst(final PsiElement start) {
        return from(DepthFirstTreeIterator.PsiIterator.createIterable(start));
    }

    @SuppressWarnings("unchecked")
    Iterable<PsiElement> it() {
        return (Iterable<PsiElement>) myIterable;
    }

    public <T extends PsiElement> PsiIterable<T> filter(Class<T> cls) {
        return from(PsiClassFilterIterator.iterable(it(), cls));
    }

    public <T extends PsiElement> PsiIterable<T> filter(Class<T> cls, PsiFilter<T> filter) {
        return from(PsiClassFilterIterator.iterable(it(), cls, filter));
    }


    public PsiIterable<E> filter(PsiFilter<E> filter) {
        return from(PsiFilterIterator.iterable(myIterable, filter));
    }

    @SuppressWarnings("LoopStatementThatDoesntLoop")
    public E first() {
        for (E node : this) return node;
        return null;
    }


    public PeekingIterator<E> peekingIterator() {
        return Iterators.peekingIterator(iterator());
    }

    static class PsiFilterIterator<T extends PsiElement> extends AbstractIterator<T> {
        static <E extends PsiElement> Iterable<E> iterable(final Iterable<E> source, final PsiFilter<E> filter) {
            return new PsiIterable<E>() {
                @NotNull
                @Override
                public Iterator<E> iterator() {
                    return new PsiFilterIterator<E>(source.iterator(), filter);
                }
            };
        }

        final Iterator<T> source;
        final PsiFilter<T> filter;

        PsiFilterIterator(Iterator<T> source, PsiFilter<T> filter) {
            this.source = source;
            this.filter = filter;
        }

        @Override
        protected T computeNext() {
            while (source.hasNext()) {
                T next = source.next();
                if (filter.acceptElement(next())) return next;
            }
            return endOfData();
        }
    }


    static class PsiClassFilterIterator<T extends PsiElement> extends AbstractIterator<T> {
        static <EE extends PsiElement> Iterable<EE> iterable(final Iterable<PsiElement> source, final Class<EE> cls, final PsiFilter<EE> predicate) {
            return new PsiIterable<EE>() {
                @NotNull
                @Override
                public Iterator<EE> iterator() {
                    return new PsiClassFilterIterator<EE>(source.iterator(), cls, predicate);
                }
            };
        }


        static <EE extends PsiElement> Iterable<EE> iterable(final Iterable<PsiElement> source, final Class<EE> cls) {
            return new PsiIterable<EE>() {
                @NotNull
                @Override
                public Iterator<EE> iterator() {
                    return new PsiClassFilterIterator<EE>(source.iterator(), cls);
                }
            };
        }


        final Iterator<? super PsiElement> source;
        final Class<T> type;
        final PsiFilter<T> filter;

        public PsiClassFilterIterator(Iterator<? super PsiElement> source, Class<T> type) {
            this(source, type, CommonFilters.<T>acceptingAllElements());
        }

        protected PsiClassFilterIterator(Iterator<? super PsiElement> source, Class<T> type, PsiFilter<T> filter) {
            this.source = source;
            this.type = type;
            this.filter = filter;
        }

        @Override
        protected T computeNext() {
            while (source.hasNext()) {
                Object next = source.next();
                if (type.isInstance(next)) {
                    return type.cast(next);
                }
            }
            return endOfData();
        }
    }
}
