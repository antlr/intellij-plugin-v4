package org.antlr.intellij.plugin.preview;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A Preview sub-tab that displays a token stream as a list of tokens.
 */
class TokenStreamViewer extends JPanel implements ListSelectionListener {

	private final JBList<Token> tokenList = new JBList<>();
	private final List<ParsingResultSelectionListener> selectionListeners = new ArrayList<>();

	private Parser recognizer;

	public TokenStreamViewer() {
		setupComponents();
	}

	/**
	 * Updates the view with the token stream contained in the given {@code parser}.
	 */
	public void setParsingResult(Parser parser) {
		BufferedTokenStream tokenStream = (BufferedTokenStream) parser.getTokenStream();
		List<? extends Token> tokens = tokenStream.getTokens();

		this.recognizer = parser;
		tokenList.setListData(tokens.toArray(new Token[0]));
	}

	/**
	 * Registers a new token selection listener.
	 */
	public void addParsingResultSelectionListener(ParsingResultSelectionListener listener) {
		selectionListeners.add(listener);
	}

	/**
	 * Fired when a token is selected in the view, and propagates the event to {@code selectionListeners}.
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) {
		Token token = tokenList.getSelectedValue();
		selectionListeners.forEach(l -> l.onLexerTokenSelected(token));
	}

	/**
	 * Fired when the caret is moved in the input editor to let us know that we should select the corresponding
	 * token.
	 */
	public void onInputTextSelected(int caretPosition) {
		for ( int i = 0; i<tokenList.getModel().getSize(); i++ ) {
			Token token = tokenList.getModel().getElementAt(i);
			if ( token.getStartIndex()<=caretPosition && caretPosition<=token.getStopIndex() ) {
				tokenList.setSelectedIndex(i);
				break;
			}
		}
	}

	private void setupComponents() {
		setLayout(new BorderLayout(0, 0));

		tokenList.installCellRenderer((Token token) -> new JBLabel(toString(token)));
		tokenList.addListSelectionListener(this);

		JBScrollPane scrollPane = new JBScrollPane(tokenList);
		add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Custom version of {@link Token#toString()} that shows the token name instead of its numeric value.
	 */
	private String toString(Token t) {

		String channelStr = "";
		if ( t.getChannel()>0 ) {
			channelStr = ",channel=" + t.getChannel();
		}
		String txt = t.getText();
		if ( txt!=null ) {
			txt = txt.replace("\n", "\\n");
			txt = txt.replace("\r", "\\r");
			txt = txt.replace("\t", "\\t");
		} else {
			txt = "<no text>";
		}
		String typeString = recognizer.getVocabulary().getSymbolicName(t.getType());

		return MessageFormat.format(
				"[@{0},{1}:{2}=''{3}'',<{4}>{5},{6}:{7}]",
				t.getTokenIndex(), t.getStartIndex(), t.getStopIndex(), txt, typeString, channelStr,
				t.getLine(), t.getCharPositionInLine()
		);
	}
}
