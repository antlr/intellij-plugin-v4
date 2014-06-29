package org.antlr.intellij.plugin;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.plugin.psi.RuleSpecNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

public class ANTLRv4LineMarkerProvider implements LineMarkerProvider {
	@Nullable
	@Override
	public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
		final GutterIconNavigationHandler<PsiElement> navHandler =
			new GutterIconNavigationHandler<PsiElement>() {
				@Override
				public void navigate(MouseEvent e, PsiElement elt) {
					System.out.println("don't click on me");
				}
			};
		if ( element instanceof RuleSpecNode ) {
			return new LineMarkerInfo<PsiElement>(element, element.getTextRange(), Icons.FILE,
												  Pass.UPDATE_ALL, null, navHandler,
												  GutterIconRenderer.Alignment.LEFT);
		}
		return null;
	}

	@Override
	public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
	}
}
