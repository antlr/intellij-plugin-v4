package org.antlr.intellij.plugin.preview;

import com.intellij.ui.JBColor;
import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.tree.Tree;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class UberTreeViewer extends TreeViewer {
	public boolean highlightUnreachedNodes = false;

	public UberTreeViewer(List<String> ruleNames, Tree tree, boolean highlightUnreachedNodes) {
		super(ruleNames, tree);
		this.highlightUnreachedNodes = highlightUnreachedNodes;
	}

	@Override
	protected void paintBox(Graphics g, Tree tree) {
		super.paintBox(g, tree);
		Rectangle2D.Double box = getBoundsOfNode(tree);
		if ( tree instanceof PreviewInterpreterRuleContext ) {
			PreviewInterpreterRuleContext ctx = (PreviewInterpreterRuleContext)tree;
			if ( highlightUnreachedNodes && !ctx.reached ) {
				g.setColor(JBColor.orange);
				g.drawRoundRect((int) box.x, (int) box.y, (int) box.width - 1,
								(int) box.height - 1, arcSize, arcSize);
			}
		}
	}

	@Override
	public void setTree(Tree root) {
		LookAndFeel theme = UIManager.getLookAndFeel();
		UIDefaults themeDefaults = theme.getDefaults();
		Color textColor = (Color)themeDefaults.get("textText");
		setTextColor(textColor);
		super.setTree(root);
	}

	public boolean hasTree() {
		return treeLayout!=null;
	}
}
