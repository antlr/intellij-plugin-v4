package org.antlr.intellij.plugin.generators;

import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LiteralChooserObject {
	private final String text;
	private final Icon icon;

	public LiteralChooserObject(final String text, @Nullable final Icon icon) {
		this.text = text;
		this.icon = icon;
	}

	public void renderTreeNode(SimpleColoredComponent component, JTree tree) {
		String literal = getText();
		SimpleTextAttributes attributes =
			new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, tree.getForeground());
		component.append(literal, attributes);
		component.setIcon(icon);
	}

	public String getText() {
		return text;
	}
}
