package org.antlr.intellij.plugin;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Utils {
	public static <T, L extends List<T>> L filter(Supplier<L> factory, Collection<T> data, Predicate<T> pred) {
		if ( data==null ) return null;
		final L filtered = factory.get();
		for (final T x : data) {
			if ( pred.test(x) ) filtered.add(x);
		}
		return filtered;
	}
}
