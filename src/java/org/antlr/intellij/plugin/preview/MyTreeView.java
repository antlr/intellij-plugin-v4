package org.antlr.intellij.plugin.preview;

import com.intellij.ui.components.Magnificator;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.gui.TreeViewer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by jason on 2/6/15.
 */
public class MyTreeView extends TreeViewer implements Magnificator {

    public final ScaleModel scaleModel = new ScaleModel(1000);


    public MyTreeView(List<String> ruleNames, Tree tree) {
        super(ruleNames, tree);
        //TODO: memory leak?
        putClientProperty(Magnificator.CLIENT_PROPERTY_KEY, this);
    }

    @Override
    public Point magnify(double magnification, Point at) {
        double s = getScale();
        scaleModel.setDoubleValue(magnification * s);
        return at;
    }

    static final double SCALE_MIN = 0.1;
    static final double SCALE_MAX = 2.5;
    static final double SCALE_RANGE = SCALE_MAX - SCALE_MIN;

    class ScaleModel extends DefaultBoundedRangeModel {
        ScaleModel(int ticks) {
            super(ticks / 2, 0, 1, ticks);
        }

        int range() {
            return getMaximum() - getMinimum();
        }

        //TODO: these methods could use caching if performance becomes an issue;
        double i2dTranslate(double val) {
            return val + (SCALE_MIN - (double) getMinimum());
        }

        double i2dScale(double val) {
            return val * (SCALE_RANGE / ((double) range()));
        }

        double d2iTranslate(double val) {
            return val + (((double) getMinimum()) - SCALE_MIN);
        }

        double d2iScale(double val) {
            return val * ((double) range()) / SCALE_RANGE;
        }


        int computeIntValue(double doubleValue) {
            return Math.round((float) d2iTranslate(d2iScale(doubleValue)));
        }

        double computeDoubleValue() {
            return i2dScale(i2dTranslate((double) getValue()));
        }

        @Override
        public void setValue(int i) {
            super.setValue(i);
            MyTreeView.this.doSetScale(computeDoubleValue());

        }

        public void setDoubleValue(double value) {
            setValue(computeIntValue(value));
        }

        public double getDoubleValue() {
            return MyTreeView.this.getScale();
        }

    }

    static double clamp(double val) {
        if (val <= SCALE_MIN) return SCALE_MIN;
        if (val >= SCALE_MAX) return SCALE_MAX;
        return val;
    }

    void doSetScale(double scale) {
        super.setScale(clamp(scale));
    }

    @Override
    public void setScale(double scale) {
        //re route to the scale model.
        scaleModel.setDoubleValue(scale);
    }
}
