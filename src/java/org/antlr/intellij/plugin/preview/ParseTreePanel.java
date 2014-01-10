package org.antlr.intellij.plugin.preview;

import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.gui.TreeViewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

public class ParseTreePanel extends JPanel {
	TreeViewer viewer;  // the antlr tree viewer component itself

	public ParseTreePanel() {
		buildGUI();
	}

	public void buildGUI() {
		viewer = new TreeViewer(null, null);
		this.setLayout(new BorderLayout());
		// Wrap viewer in scroll pane
		JScrollPane scrollPane = new JScrollPane(viewer);
		this.add(scrollPane, BorderLayout.CENTER);

		// Add button to bottom
		JPanel bottomPanel = new JPanel(new BorderLayout(0,0));
		this.add(bottomPanel, BorderLayout.SOUTH);

		// Add scale slider
		int sliderValue = (int) ((viewer.getScale()-1.0) * 1000);
		final JSlider scaleSlider = new JSlider(JSlider.HORIZONTAL,
										  -999,1000,sliderValue);
		scaleSlider.addChangeListener(
			new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int v = scaleSlider.getValue();
					viewer.setScale(v / 1000.0 + 1.0);
				}
			}
									 );
		bottomPanel.add(scaleSlider, BorderLayout.CENTER);
	}

	public TreeViewer getViewer() {
		return viewer;
	}

	public void resetTree(String inputText,
						  String combinedGrammarFileName,
						  String startRule)
	{
		ParseTree root = null;
		Parser parser = null;
		try {
			Object[] results =
				ANTLRv4ProjectComponent.parseText(inputText,
												  combinedGrammarFileName,
												  startRule);
			parser = (Parser)results[0];
			root = (ParseTree)results[1];
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		viewer.setRuleNames(Arrays.asList(parser.getRuleNames()));
		viewer.setTree(root);
	}
}
