package org.antlr.intellij.plugin.folding;

/**
 * Created by jason on 2/2/15.
 */
public abstract class ANTLRv4FoldingSettings {
    //TODO ServiceManager
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
        public boolean isCollapseEndOfLineComments() {
            return true;
        }

        @Override
        public boolean isCollapseRuleBlocks() {
            return false;
        }
    };

    public static ANTLRv4FoldingSettings getInstance() {
        return INSTANCE;
    }

    public abstract boolean isCollapseFileHeader();

    public abstract boolean isCollapseDocComments();

    public abstract boolean isCollapseEndOfLineComments();

    public abstract boolean isCollapseRuleBlocks();
}
