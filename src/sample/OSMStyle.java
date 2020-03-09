package sample;

import com.github.cliftonlabs.json_simple.JsonObject;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

import java.util.HashMap;

public class OSMStyle {

    private static HashMap<Integer, Color> mStreetColors;
    
    private static void initStreetColors() {
        mStreetColors = new HashMap<>();
        mStreetColors.put(OSMUtils.STREET_TYPE_MOTORWAY,  Color.rgb(0xe8, 0x92, 0xa2));
        mStreetColors.put(OSMUtils.STREET_TYPE_MOTORWAY_LINK,  Color.rgb(0xe8, 0x92, 0xa2));
        mStreetColors.put(OSMUtils.STREET_TYPE_TRUNK,  Color.rgb(0xf9, 0xb2, 0x9c));
        mStreetColors.put(OSMUtils.STREET_TYPE_TRUNK_LINK,  Color.rgb(0xf9, 0xb2, 0x9c));
        mStreetColors.put(OSMUtils.STREET_TYPE_PRIMARY,  Color.rgb(0xfc, 0xd6, 0xa4));
        mStreetColors.put(OSMUtils.STREET_TYPE_PRIMARY_LINK,  Color.rgb(0xfc, 0xd6, 0xa4));
        mStreetColors.put(OSMUtils.STREET_TYPE_SECONDARY,  Color.rgb(0xff, 0xfa, 0xbf));
        mStreetColors.put(OSMUtils.STREET_TYPE_SECONDARY_LINK,  Color.rgb(0xff, 0xfa, 0xbf));
        mStreetColors.put(OSMUtils.STREET_TYPE_TERTIARY,  Color.rgb(0xff, 0xff, 0xb3));
        mStreetColors.put(OSMUtils.STREET_TYPE_TERTIARY_LINK,  Color.rgb(0xff, 0xff, 0xb3));
        mStreetColors.put(OSMUtils.STREET_TYPE_RESIDENTIAL,  Color.rgb(0xff, 0xff, 0xff));
        mStreetColors.put(OSMUtils.STREET_TYPE_UNCLASSIFIED,  Color.rgb(0xff, 0xff, 0xff));
        mStreetColors.put(OSMUtils.STREET_TYPE_ROAD,  Color.rgb(0xff, 0xff, 0xff));
        mStreetColors.put(OSMUtils.STREET_TYPE_SERVICE,  Color.rgb(0xff, 0xff, 0xff));
        mStreetColors.put(OSMUtils.STREET_TYPE_LIVING_STREET,  Color.rgb(0xff, 0xff, 0xff));
    }

    private static Color getStreetColor(int streetTypeId) {
        if (mStreetColors == null) {
            initStreetColors();
        }
        return mStreetColors.get(streetTypeId);
    }

    public static Paint getAreaColor(int areaType) {
        if (areaType == OSMUtils.AREA_TYPE_BUILDING) {
            return Color.GRAY;
        }
        if (areaType == OSMUtils.AREA_TYPE_WATER) {
            return Color.BLUE;
        }
        if (areaType == OSMUtils.AREA_TYPE_RAILWAY) {
            return Color.BLACK;
        }
        if (areaType == OSMUtils.AREA_TYPE_NATURAL) {
            return Color.GREEN;
        }
        return Color.LIGHTGREEN;
    }

    private static int getStreetWidth(int streetTypeId, int zoom) {
        if (zoom <= 12) {
            return 2;
        }

        double width = 12;
        if (streetTypeId == OSMUtils.STREET_TYPE_MOTORWAY) {
            width = width * 1.6;
        } else if (streetTypeId == OSMUtils.STREET_TYPE_MOTORWAY_LINK) {
            width = width * 1.2;
        } else if (streetTypeId == OSMUtils.STREET_TYPE_TRUNK) {
            width = width * 1.6;
        } else if (streetTypeId == OSMUtils.STREET_TYPE_TRUNK_LINK) {
            width = width * 1.2;
        } else if (streetTypeId == OSMUtils.STREET_TYPE_PRIMARY_LINK || streetTypeId == OSMUtils.STREET_TYPE_PRIMARY) {
            width = width * 1.4;
        } else if (streetTypeId == OSMUtils.STREET_TYPE_SECONDARY || streetTypeId == OSMUtils.STREET_TYPE_SECONDARY_LINK) {
            width = width * 1.2;
        } else if (streetTypeId == OSMUtils.STREET_TYPE_TERTIARY || streetTypeId == OSMUtils.STREET_TYPE_TERTIARY_LINK) {
            width = width * 1.0;
        } else if (streetTypeId == OSMUtils.STREET_TYPE_LIVING_STREET) {
            width = width * 1.0;
        } else if (streetTypeId == OSMUtils.STREET_TYPE_RESIDENTIAL || streetTypeId == OSMUtils.STREET_TYPE_ROAD || streetTypeId == OSMUtils.STREET_TYPE_UNCLASSIFIED) {
            width = width * 0.8;
        } else {
            width = width * 0.6;
        }
        int penWidth = (int) (getRelativePenWidthForZoom(zoom) * width);
        return Math.max(penWidth, 1);
    }

    private static double getRelativePenWidthForZoom(int zoom) {
        if (zoom == 20) {
            return 6.0;
        }
        if (zoom == 19) {
            return 3.0;
        }
        if (zoom == 18) {
            return 1.4;
        }
        if (zoom == 17) {
            return 0.8;
        }
        if (zoom == 16) {
            return 0.5;
        }
        if (zoom == 15) {
            return 0.4;
        }
        if (zoom == 14) {
            return 0.3;
        }
        return 0.2;
    }

    public static void amendWay(JsonObject way, Polyline wayLine, int zoom) {
        int streetTypeId = (int) way.get("streetTypeId");
        wayLine.setStroke(getStreetColor(streetTypeId));
        wayLine.setStrokeWidth(getStreetWidth(streetTypeId, zoom));
        wayLine.setSmooth(true);
        wayLine.setStrokeLineCap(StrokeLineCap.ROUND);
        wayLine.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }

    public static void amendArea(JsonObject area, Polyline areaLine, int zoom) {
        int areaType = (int) area.get("areaType");
        areaLine.setFill(getAreaColor(areaType));
    }

    public static void amendLineArea(JsonObject area, Polyline areaLine, int zoom) {
        int areaType = (int) area.get("areaType");
        areaLine.setStroke(getAreaColor(areaType));
        if (areaType == OSMUtils.AREA_TYPE_RAILWAY) {
            areaLine.getStrokeDashArray().addAll(4d);
        }
    }
}
