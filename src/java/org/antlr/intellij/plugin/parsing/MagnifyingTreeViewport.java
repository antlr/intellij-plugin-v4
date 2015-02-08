package org.antlr.intellij.plugin.parsing;

import com.intellij.ui.components.Magnificator;
import com.intellij.ui.components.ZoomableViewport;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Created by jason on 2/5/15.
 */
public class MagnifyingTreeViewport extends JViewport implements ZoomableViewport, Magnificator {

    final BoundedRangeModel model;

    public MagnifyingTreeViewport(BoundedRangeModel model) {
        this.model = model;
    }

    /**
     * if this returns null, no magnify events will be sent.
     * @return this
     */
    @Nullable
    @Override
    public Magnificator getMagnificator() {
        return this;
    }

    boolean firstEvent = true;
    @Override
    public void magnificationStarted(Point at) {
        firstEvent=true;
    }

    @Override
    public void magnificationFinished(double magnification) {
    }
    double prevVal;
    @Override
    public void magnify(double magnification) {
        double m = magnificationToScale(magnification);
        if(firstEvent){
            prevVal=m;
            firstEvent=false;
            return;
        }
        double mag = (m) - prevVal;
      model.setValue((int) (model.getValue() + 50.0 * mag));
    }

    @Override
    public Point magnify(double scale, Point at) {
        System.out.println("magnificator.magnify");
        return at;
    }
    private static double magnificationToScale(double magnification) {
        return magnification < 0 ? 1f / (1 - magnification) : (1 + magnification);
    }
}
