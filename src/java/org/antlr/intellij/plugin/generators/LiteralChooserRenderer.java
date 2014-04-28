package org.antlr.intellij.plugin.generators;

import com.intellij.ui.CheckboxTreeBase;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class LiteralChooserRenderer extends CheckboxTreeBase.CheckboxTreeCellRendererBase {
	@Override
	public void customizeRenderer(JTree tree,
								  Object value,
								  boolean selected,
								  boolean expanded,
								  boolean leaf,
								  int row,
								  boolean hasFocus)
	{
		if (value instanceof DefaultMutableTreeNode) {
			Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
			if (userObject instanceof LiteralChooserObject) {
				LiteralChooserObject literalChooserObject = (LiteralChooserObject)userObject;
				literalChooserObject.renderTreeNode(getTextRenderer(), tree);
			}
		}
	}
}
