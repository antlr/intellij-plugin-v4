package org.antlr.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowContentUiType;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerListener;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class MockToolWindow implements ToolWindow {

    private Boolean visible = true;

    public @NonNls @NotNull String getId() {
        return null;
    }

    public boolean isActive() {
        return false;
    }

    public void activate(@Nullable Runnable runnable) {}

    public void activate(@Nullable Runnable runnable, boolean autoFocusContents) {}

    public void activate(@Nullable Runnable runnable, boolean autoFocusContents, boolean forced) {}

    public boolean isVisible() {
        return this.visible;
    }

    public void show(@Nullable Runnable runnable) {
        this.visible = true;
    }

    public void hide(@Nullable Runnable runnable) {
        this.visible = false;
    }

    public @NotNull ToolWindowAnchor getAnchor() {
        return null;
    }

    public boolean isVisibleOnLargeStripe() {
        return false;
    }

    public void setVisibleOnLargeStripe(boolean visible) {}    public void setOrderOnLargeStripe(int order) {}

    public int getOrderOnLargeStripe() {
        return 0;
    }

    public @NotNull ToolWindowAnchor getLargeStripeAnchor() {
        return null;
    }

    public void setLargeStripeAnchor(@NotNull ToolWindowAnchor anchor) {}

    public void setAnchor(@NotNull ToolWindowAnchor anchor, @Nullable Runnable runnable) {}

    public boolean isSplitMode() {
        return false;
    }

    public void setSplitMode(boolean split, @Nullable Runnable runnable) {}

    public boolean isAutoHide() {
        return false;
    }

    public void setAutoHide(boolean value) {}

    public @NotNull ToolWindowType getType() {
        return null;
    }

    public void setType(@NotNull ToolWindowType type, @Nullable Runnable runnable) {}

    public @Nullable Icon getIcon() {
        return null;
    }

    public void setIcon(@NotNull Icon icon) {}

    public @NlsContexts.TabTitle @Nullable String getTitle() {
        return null;
    }

    public void setTitle(@NlsContexts.TabTitle String title) {}

    public @NlsContexts.TabTitle @NotNull String getStripeTitle() {
        return null;
    }

    public @NotNull Supplier<@NlsContexts.TabTitle String> getStripeTitleProvider() {
        return null;
    }

    public void setStripeTitle(@NlsContexts.TabTitle @NotNull String title) {}

    public void setStripeTitleProvider(@NotNull Supplier<@NlsContexts.TabTitle @NotNull String> supplier) {

    }

    public boolean isAvailable() {
        return false;
    }

    public void setAvailable(boolean value, @Nullable Runnable runnable) {}

    public void setAvailable(boolean value) {}

    public void setContentUiType(@NotNull ToolWindowContentUiType type, @Nullable Runnable runnable) {}

    public void setDefaultContentUiType(@NotNull ToolWindowContentUiType type) {}

    public @NotNull ToolWindowContentUiType getContentUiType() {
        return null;
    }

    public void installWatcher(ContentManager contentManager) {}

    public @NotNull JComponent getComponent() {
        return null;
    }

    public @NotNull ContentManager getContentManager() {
        return null;
    }

    public @Nullable ContentManager getContentManagerIfCreated() {
        return null;
    }

    public void addContentManagerListener(@NotNull ContentManagerListener listener) {}

    public void setDefaultState(@Nullable ToolWindowAnchor anchor, @Nullable ToolWindowType type, @Nullable Rectangle floatingBounds) {}

    public void setToHideOnEmptyContent(boolean hideOnEmpty) {}

    public void setShowStripeButton(boolean value) {}

    public boolean isShowStripeButton() {
        return false;
    }

    public boolean isDisposed() {
        return false;
    }

    public void showContentPopup(@NotNull InputEvent inputEvent) {}

    public @NotNull Disposable getDisposable() {
        return null;
    }

    public void remove() {}

    public void setTitleActions(@NotNull List actions) {}

    public @NotNull ActionCallback getReady(@NotNull Object requestor) {
        return null;
    }

    public void setAdditionalGearActions(@Nullable ActionGroup additionalGearActions) {}    public @NotNull Project getProject() {
        return null;
    }

}
