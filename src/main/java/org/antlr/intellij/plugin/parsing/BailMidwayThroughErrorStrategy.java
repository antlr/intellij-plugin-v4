package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;

public class BailMidwayThroughErrorStrategy extends DefaultErrorStrategy {
	public BailMidwayThroughErrorStrategy() {
	}

	@Override
	protected void consumeUntil(Parser recognizer, IntervalSet set) {
//		System.err.println("consumeUntil("+set.toString(recognizer.getTokenNames())+")");
		int ttype = recognizer.getInputStream().LA(1);
		while (ttype != Token.EOF ) {
            //System.out.println("consume during recover LA(1)="+getTokenNames()[input.LA(1)]);
//			recognizer.getInputStream().consume();
            recognizer.consume();
            ttype = recognizer.getInputStream().LA(1);
        }
	}
}
