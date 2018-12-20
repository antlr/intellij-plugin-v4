package org.antlr.intellij.plugin;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
	public static void main(String[] args) throws IOException {
		Pattern pattern = Pattern.compile(".*?package\\s+(.*?);.*");
		Matcher matcher = pattern.matcher("{ package x.y.z; }");
		if ( matcher.matches() ) {
			System.out.println(matcher.group(1));
		}
	}
}
