package org.antlr.intellij.plugin.preview;

import com.intellij.ui.components.Magnificator;
import org.antlr.v4.runtime.tree.Tree;

import javax.swing.*;
import java.awt.*;

/**
 * Created by jason on 2/7/15.
 */
public class TrackpadZoomingTreeView extends UberTreeViewer implements Magnificator {
    public TrackpadZoomingTreeView(java.util.List<String> ruleNames, Tree tree, boolean highlightUnreachedNodes) {
        super(ruleNames, tree, highlightUnreachedNodes);
        //TODO: memory leak?
        putClientProperty(Magnificator.CLIENT_PROPERTY_KEY, this);
    }

    public final ScaleModel scaleModel = new ScaleModel(1000);

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
            TrackpadZoomingTreeView.this.doSetScale(computeDoubleValue());

        }

        public void setDoubleValue(double value) {
            setValue(computeIntValue(value));
        }

        public double getDoubleValue() {
            return TrackpadZoomingTreeView.this.getScale();
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
        //route everything through the scale model so that the slider will stay in sync;
        scaleModel.setDoubleValue(scale);
    }
}
