package org.antlr.intellij.plugin;

import java.util.function.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Utils {
	public static <T> List<T> filter(Collection<T> data, Predicate<T> pred) {
		if ( data==null ) return null;
		List<T> filtered = new ArrayList<>();
		for (T x : data) {
			if ( pred.test(x) ) filtered.add(x);
		}
		return filtered;
	}
}
