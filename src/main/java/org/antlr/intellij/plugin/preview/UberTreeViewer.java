package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.DarculaColors;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.DefaultConfiguration;
import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.Tree;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class UberTreeViewer extends TreeViewer {
	private final boolean highlightUnreachedNodes;

	public UberTreeViewer(List<String> ruleNames, Tree tree, boolean highlightUnreachedNodes) {
		super(ruleNames, tree);
		this.highlightUnreachedNodes = highlightUnreachedNodes;
	}

	@Override
	protected void paintBox(Graphics g, Tree tree) {
		customPaintBox(g, tree);

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

	// Customized version of super.paintBox() that supports Darcula colors
	private void customPaintBox(Graphics g, Tree tree) {
		Rectangle2D.Double box = getBoundsOfNode(tree);
		// draw the box in the background
		boolean ruleFailedAndMatchedNothing = false;
		if ( tree instanceof ParserRuleContext ) {
			ParserRuleContext ctx = (ParserRuleContext) tree;
			ruleFailedAndMatchedNothing = ctx.exception != null &&
					ctx.stop != null && ctx.stop.getTokenIndex() < ctx.start.getTokenIndex();
		}
		if ( isHighlighted(tree) || boxColor!=null ||
				tree instanceof ErrorNode ||
				ruleFailedAndMatchedNothing)
		{
			if ( isHighlighted(tree) ) g.setColor(highlightedBoxColor);
			else if ( tree instanceof ErrorNode || ruleFailedAndMatchedNothing ) g.setColor(DarculaColors.RED);
			else g.setColor(boxColor);
			g.fillRoundRect((int) box.x, (int) box.y, (int) box.width,
					(int) box.height, arcSize, arcSize);
		}
		if ( borderColor!=null ) {
			g.setColor(borderColor);
			g.drawRoundRect((int) box.x, (int) box.y, (int) box.width,
					(int) box.height, arcSize, arcSize);
		}

		// draw the text on top of the box (possibly multiple lines)
		if ( tree instanceof ErrorNode || ruleFailedAndMatchedNothing ) {
			g.setColor(Gray._64);
		} else {
			g.setColor(textColor);
		}
		String s = getText(tree);
		String[] lines = s.split("\n");
		FontMetrics m = getFontMetrics(font);
		int x = (int) box.x + arcSize / 2 + nodeWidthPadding;
		int y = (int) box.y + m.getAscent() + m.getLeading() + 1 + nodeHeightPadding;
		for (int i = 0; i < lines.length; i++) {
			text(g, lines[i], x, y);
			y += m.getHeight();
		}
	}

	// Copied from antlr
	private Dimension getScaledTreeSize() {
		Dimension scaledTreeSize =
				treeLayout.getBounds().getBounds().getSize();
		scaledTreeSize = new Dimension((int)(scaledTreeSize.width*scale),
				(int)(scaledTreeSize.height*scale));
		return scaledTreeSize;
	}

	// Copied from antlr
	private void updatePreferredSize() {
		setPreferredSize(getScaledTreeSize());
		invalidate();
		if (getParent() != null) {
			getParent().validate();
		}
		repaint();
	}

	// Copied from antlr and tweaked to repaint after layout (using different thread)
	@Override
	public void setTree(Tree root) {
		setTextColor(JBColor.BLACK);
		if ( root!=null ) {
			boolean useIdentity = true; // compare node identity
			this.treeLayout =
					new TreeLayout<Tree>(getTreeLayoutAdaptor(root),
							new TreeViewer.VariableExtentProvide(this),
							new DefaultConfiguration<Tree>(gapBetweenLevels,
									gapBetweenNodes),
							useIdentity);
			// Let the UI display this new AST.
			ApplicationManager.getApplication().invokeLater(() -> {
				System.out.println("UPDATE SIZE START "+Thread.currentThread().getName());
				updatePreferredSize();
				System.out.println("UPDATE SIZE STOP "+Thread.currentThread().getName());
			});
		}
		else {
			this.treeLayout = null;
			ApplicationManager.getApplication().invokeLater(this::repaint);
		}
	}

	public boolean hasTree() {
		return treeLayout!=null;
	}
}
