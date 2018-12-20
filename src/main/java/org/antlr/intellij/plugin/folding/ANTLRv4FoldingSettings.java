package org.antlr.intellij.plugin.folding;

/**
 * Created by jason on 2/2/15.
 */
public abstract class ANTLRv4FoldingSettings {
    //TODO ServiceManager,UI,serialization,etc
    private static final ANTLRv4FoldingSettings INSTANCE= new ANTLRv4FoldingSettings() {
        @Override
        public boolean isCollapseFileHeader() {
            return true;
        }

        @Override
        public boolean isCollapseDocComments() {
            return false;
        }

        @Override
        public boolean isCollapseComments() {
            return false;
        }

        @Override
        public boolean isCollapseRuleBlocks() {
            return false;
        }

        @Override
        public boolean isCollapseActions() {
            return true;
        }

        @Override
        public boolean isCollapseTokens() {
            return true;
        }
    };

    public static ANTLRv4FoldingSettings getInstance() {
        return INSTANCE;
    }

    public abstract boolean isCollapseFileHeader();

    public abstract boolean isCollapseDocComments();

    public abstract boolean isCollapseComments();

    public abstract boolean isCollapseRuleBlocks();

    public abstract boolean isCollapseActions();

    public abstract boolean isCollapseTokens();
}
