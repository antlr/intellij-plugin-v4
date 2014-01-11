package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.components.JBScrollPane;
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
		this.setLayout(new BorderLayout());

		// wrap tree and slider in panel
		JPanel treePanel = new JPanel(new BorderLayout(0,0));
		treePanel.setBackground(Color.white);
		// Wrap tree viewer component in scroll pane
		viewer = new TreeViewer(null, null);
		JScrollPane scrollPane = new JBScrollPane(viewer); // use Intellij's scroller
		treePanel.add(scrollPane, BorderLayout.CENTER);

		// Add scale slider to bottom, under tree view scroll panel
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
		treePanel.add(scaleSlider, BorderLayout.SOUTH);

		// Get an editor in a panel that lets it resize in splitpane
//		JPanel editorWrapper = new JPanel(new BorderLayout());
//		editorWrapper.setBackground(Color.white);
//		editorWrapper.add(editor);

		String input = "testing\nfddfsasdfasdfasdfasfd\nadskljalsdkfjafd";
		final EditorFactory factory = EditorFactory.getInstance();
		Document doc = factory.createDocument(input);
		Editor ed = factory.createEditor(doc);

		Splitter splitPane = new Splitter();
		splitPane.setFirstComponent(ed.getComponent());
		splitPane.setSecondComponent(treePanel);

		this.add(splitPane, BorderLayout.CENTER);
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
