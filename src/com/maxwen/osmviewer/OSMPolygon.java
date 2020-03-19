package com.maxwen.osmviewer;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

public class OSMPolygon extends Polygon implements OSMShape {
    private long mOSMId;

    public OSMPolygon(long osmId) {
        mOSMId = osmId;
    }

    public OSMPolygon(long osmId, OSMPolygon parent) {
        mOSMId = osmId;
        setStrokeWidth(parent.getStrokeWidth());
        setStrokeLineCap(parent.getStrokeLineCap());
        setStrokeLineJoin(parent.getStrokeLineJoin());
        setSmooth(parent.isSmooth());
    }

    @Override
    public long getOSMId() {
        return mOSMId;
    }

    @Override
    public void setSelected() {
        setStroke(Color.RED);
        setFill(Color.TRANSPARENT);
        setStrokeWidth(4);
    }

    @Override
    public Shape getShape() {
        return this;
    }
}
