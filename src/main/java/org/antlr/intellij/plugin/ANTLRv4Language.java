package org.antlr.intellij.plugin;

import com.intellij.lang.Language;

public class ANTLRv4Language extends Language {
    public static final ANTLRv4Language INSTANCE = new ANTLRv4Language();

    private ANTLRv4Language() {
        super("ANTLRv4");
    }


}
