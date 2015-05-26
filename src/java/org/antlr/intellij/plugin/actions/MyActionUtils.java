package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.v4.runtime.atn.AmbiguityInfo;
import org.antlr.v4.runtime.atn.DecisionEventInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class MyActionUtils {
	public static void selectedFileIsGrammar(AnActionEvent e) {
		VirtualFile vfile = getGrammarFileFromEvent(e);
		if ( vfile==null ) {
			e.getPresentation().setEnabled(false);
			return;
		}
		e.getPresentation().setEnabled(true); // enable action if we're looking at grammar file
		e.getPresentation().setVisible(true);
	}

	public static VirtualFile getGrammarFileFromEvent(AnActionEvent e) {
		VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
		if ( files==null || files.length==0 ) return null;
		VirtualFile vfile = files[0];
		if ( vfile!=null && vfile.getName().endsWith(".g4") ) {
			return vfile;
		}
		return null;
	}

	public static int getMouseOffset(MouseEvent mouseEvent, Editor editor) {
		Point point=new Point(mouseEvent.getPoint());
		LogicalPosition pos=editor.xyToLogicalPosition(point);
		return editor.logicalPositionToOffset(pos);
	}

	public static int getMouseOffset(Editor editor) {
		Point mousePosition = editor.getContentComponent().getMousePosition();
		LogicalPosition pos=editor.xyToLogicalPosition(mousePosition);
		int offset = editor.logicalPositionToOffset(pos);
		return offset;
	}

	@NotNull
	public static List<RangeHighlighter> getRangeHighlightersAtOffset(Editor editor, int offset) {
		MarkupModel markupModel = editor.getMarkupModel();
		// collect all highlighters and combine to make a single tool tip
		List<RangeHighlighter> highlightersAtOffset = new ArrayList<RangeHighlighter>();
		for (RangeHighlighter r : markupModel.getAllHighlighters()) {
			int a = r.getStartOffset();
			int b = r.getEndOffset();
//			System.out.printf("#%d: %d..%d %s\n", i, a, b, r.toString());
			if (offset >= a && offset < b) { // cursor is over some kind of highlighting
				highlightersAtOffset.add(r);
			}
		}
		return highlightersAtOffset;
	}

	public static DecisionEventInfo getHighlighterWithDecisionEventType(List<RangeHighlighter> highlighters, Class decisionEventType) {
		for (RangeHighlighter r : highlighters) {
			DecisionEventInfo eventInfo = r.getUserData(ProfilerPanel.DECISION_EVENT_INFO_KEY);
			if (eventInfo != null) {
				if (eventInfo.getClass() == decisionEventType) {
					return eventInfo;
				}
			}
		}
		return null;
	}
}
