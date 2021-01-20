package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.icons.AllIcons.Actions.Find;
import static com.intellij.icons.AllIcons.General.AutoscrollFromSource;
import static org.antlr.intellij.plugin.ANTLRv4PluginController.PREVIEW_WINDOW_ID;

/**
 * A Preview sub-tab that displays a token stream as a list of tokens.
 */
class TokenStreamViewer extends JPanel implements ListSelectionListener {

	private final JBList<Token> tokenList = new JBList<>();
	private final List<ParsingResultSelectionListener> selectionListeners = new ArrayList<>();

	private boolean scrollFromSource = false;
	private boolean highlightSource = false;
	private Parser recognizer;

	public TokenStreamViewer() {
		setupComponents();
	}

	public void setParsingResult(Parser parser) {
		BufferedTokenStream tokenStream = (BufferedTokenStream) parser.getTokenStream();
		List<? extends Token> tokens = tokenStream.getTokens();

		this.recognizer = parser;
		tokenList.setListData(tokens.toArray(new Token[0]));
	}

	private void setupComponents() {
		setLayout(new BorderLayout(0, 0));

		tokenList.installCellRenderer(token -> new JBLabel(toString(token)));
		tokenList.addListSelectionListener(this);

		ActionToolbar toolbar = createToolbar();
		add(toolbar.getComponent(), BorderLayout.NORTH);

		JBScrollPane scrollPane = new JBScrollPane(tokenList);
		add(scrollPane, BorderLayout.CENTER);
	}

	// TODO this was copy-pasted from HierarchyViewer
	private ActionToolbar createToolbar() {
		ToggleAction scrollFromSourceBtn = new ToggleAction("Scroll from Source", null, AutoscrollFromSource) {
			@Override
			public boolean isSelected(@NotNull AnActionEvent e) {
				return scrollFromSource;
			}

			@Override
			public void setSelected(@NotNull AnActionEvent e, boolean state) {
				scrollFromSource = state;
			}
		};
		ToggleAction scrollToSourceBtn = new ToggleAction("Highlight Source", null, Find) {
			@Override
			public boolean isSelected(@NotNull AnActionEvent e) {
				return highlightSource;
			}

			@Override
			public void setSelected(@NotNull AnActionEvent e, boolean state) {
				highlightSource = state;
			}
		};

		DefaultActionGroup actionGroup = new DefaultActionGroup(
				scrollFromSourceBtn,
				scrollToSourceBtn
		);

		return ActionManager.getInstance().createActionToolbar(PREVIEW_WINDOW_ID + "aaaa", actionGroup, true);
	}

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

	public void addParsingResultSelectionListener(ParsingResultSelectionListener listener) {
		selectionListeners.add(listener);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if ( !highlightSource ) {
			return;
		}

		Token token = tokenList.getSelectedValue();
		selectionListeners.forEach(l -> l.onLexerTokenSelected(token));
	}
}
