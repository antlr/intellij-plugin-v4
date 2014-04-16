package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseMotionAdapter;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;

import java.awt.*;
import java.awt.event.MouseEvent;

public class PreviewEditorMouseListener extends EditorMouseMotionAdapter {
//	protected Balloon lastBalloon;
	protected Point lastPoint;

	@Override
	public void mouseMoved(EditorMouseEvent e){
		MouseEvent mouseEvent=e.getMouseEvent();
		if(mouseEvent.isMetaDown()){
			Point point=new Point(mouseEvent.getPoint());
			Editor editor=e.getEditor();
			LogicalPosition pos=editor.xyToLogicalPosition(point);
			int offset=editor.logicalPositionToOffset(pos);
			System.out.println("offset="+offset);
			int selStart=editor.getSelectionModel().getSelectionStart();
			int selEnd=editor.getSelectionModel().getSelectionEnd();
			int caret = editor.getCaretModel().getOffset();
			Point above = new Point(point);
			above.translate(0, -editor.getLineHeight());
			RelativePoint where = new RelativePoint(mouseEvent.getComponent(), above);


//						highlighter.setErrorStripeTooltip(highlightInfo);
//			ToolTipManager toolTipManager=ToolTipManager.sharedInstance();
//						toolTipManager.setEnabled(true);
//						editor.getComponent().setToolTipText("fooo");
//						toolTipManager.mouseEntered(mouseEvent);

//			if ( lastPoint==null || Math.abs(lastPoint.getX() - point.getX())>=8 ) {
				MarkupModel markupModel=editor.getMarkupModel();
				markupModel.removeAllHighlighters();
//				if ( lastBalloon!=null ) {
//					lastBalloon.hide();
//				}

				CaretModel caretModel = editor.getCaretModel();
				if ( offset >= editor.getDocument().getTextLength() ) return;

				// Underline
				final TextAttributes attr=new TextAttributes();
				attr.setForegroundColor(JBColor.BLUE);
				attr.setEffectColor(JBColor.BLUE);
				attr.setEffectType(EffectType.LINE_UNDERSCORE);
				RangeHighlighter rangehighlighter=
					markupModel.addRangeHighlighter(offset,offset+1,0,attr, HighlighterTargetArea.EXACT_RANGE);

				// try HINT
				caretModel.moveToOffset(offset);
				HintManager.getInstance().showInformationHint(editor, "fooooo");
//				HintManager.getInstance().showQuestionHint(editor, "Type: foo\\nick: 8", offset, offset+1,
//														   new QuestionAction() {
//															   @Override
//															   public boolean execute() {
//																   return false;
//															   }
//														   });
//				HintManager.getInstance().showErrorHint(editor, "blort", HintManager.ABOVE);

				// try raw Hint
//				int flags =
//					HintManager.HIDE_BY_ANY_KEY |
//					HintManager.HIDE_BY_TEXT_CHANGE |
//					HintManager.HIDE_BY_SCROLLING |
//					HintManager.ABOVE;
//				int timeout = 1000; // 1s?
//				HintManager.getInstance().showHint(new JBLabel("Type: foo\nick: 8"), where, flags, timeout);

				// try Tooltip
//				editor.getComponent().setToolTipText("HI");
//				String toolTipText = editor.getComponent().getToolTipText();
//				JToolTip toolTip = editor.getComponent().createToolTip();
//				toolTip.setToolTipText("fubar");
//				toolTip.show();

//				HighlightInfo.Builder highlightInfo = HighlightInfo.newHighlightInfo(HighlightInfoType.WARNING);
//				highlighter.setErrorStripeTooltip(highlightInfo);
				BalloonBuilder builder =
					JBPopupFactory.getInstance().createHtmlTextBalloonBuilder("hello", MessageType.INFO, null);
				Balloon balloon = builder.createBalloon();
				//balloon.show(where, Balloon.Position.above);
//				lastBalloon = balloon;
				lastPoint = point;
//			}
		}
	}
}
