package sample;

import com.github.cliftonlabs.json_simple.JsonObject;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OSMStyle {

    private static HashMap<Integer, Color> mStreetColors;
    private static HashMap<String, Color> mAreaColors;

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

        mAreaColors = new HashMap();
        mAreaColors.put("adminAreaColor", Color.rgb(0xac, 0x46, 0xac));
        mAreaColors.put("backgroundColor",  Color.rgb(120, 120, 120, 0.8d));
        mAreaColors.put("mapBackgroundColor",  Color.rgb(0xf1, 0xee, 0xe8));
        mAreaColors.put("wayCasingColor",  Color.rgb(0x70, 0x7d, 0x05));
        mAreaColors.put("tunnelColor", Color.WHITE);
        mAreaColors.put("bridgeCasingColor",  Color.rgb(0x50, 0x50, 0x50));
        mAreaColors.put("accessWaysColor",  Color.rgb(255, 0, 0, 0.4d));
        mAreaColors.put("onewayWaysColor",  Color.rgb(0, 0, 255, 0.4d));
        mAreaColors.put("livingStreeColor",  Color.rgb(0, 255, 0, 0.4d));
        mAreaColors.put("waterColor",  Color.rgb(0xaa, 0xd3, 0xdf));
        mAreaColors.put("adminAreaColor",  Color.rgb(0xac, 0x46, 0xac));
        mAreaColors.put("warningBackgroundColor",  Color.rgb(255, 0, 0, 0.8d));
        mAreaColors.put("naturalColor",  Color.rgb(0x8d, 0xc5, 0x6c));
        mAreaColors.put("forestAreaColor",  Color.rgb(0xad, 0xd1, 0x9e));
        mAreaColors.put("woodAreaColor",  Color.rgb(0xae, 0xd1, 0xa0));
        mAreaColors.put("tourismUndefinedColor", Color.RED);
        mAreaColors.put("tourismCampingAreaColor",  Color.rgb(0xcc, 0xff, 0x99));
        mAreaColors.put("amenityParkingAreaColor",  Color.rgb(0xf7, 0xef, 0xb7));
        mAreaColors.put("amenityUndefinedColor", Color.RED);
        mAreaColors.put("naturalUndefinedColor", Color.RED);
        mAreaColors.put("buildingColor",  Color.rgb(0xbc, 0xa9, 0xa9, 0.8d));
        mAreaColors.put("highwayAreaColor",  Color.rgb(255, 255, 255));
        mAreaColors.put("railwayAreaColor",  Color.rgb(0xdf, 0xd1, 0xd6));
        mAreaColors.put("railwayColor",  Color.rgb(0x90, 0x90, 0x90));
        mAreaColors.put("landuseColor",  Color.rgb(150, 150, 150));
        mAreaColors.put("landuseUndefinedColor", Color.RED);
        mAreaColors.put("placeTagColor",  Color.rgb(78, 167, 255, 0.6d));
        mAreaColors.put("residentialColor",  Color.rgb(0xdd, 0xdd, 0xdd));
        mAreaColors.put("commercialColor",  Color.rgb(0xf2, 0xda, 0xd9));
        mAreaColors.put("farmColor",  Color.rgb(0xee, 0xf0, 0xd5));
        mAreaColors.put("grassColor",  Color.rgb(0xcd, 0xeb, 0xb0));
        mAreaColors.put("greenfieldColor",  Color.rgb(0x9d, 0x9d, 0x6c));
        mAreaColors.put("industrialColor",  Color.rgb(0xdf, 0xd1, 0xd6));
        mAreaColors.put("aerowayColor",  Color.rgb(0xbb, 0xbb, 0xcc));
        mAreaColors.put("aerowayAreaColor",  Color.rgb(0xdf, 0xd1, 0xd6));
        mAreaColors.put("nightModeColor",  Color.rgb(120, 120, 120, 0.4d));
        mAreaColors.put("villageGreenAreaColor",  Color.rgb(0xcd, 0xeb, 0xb0));
        mAreaColors.put("cliffColor",  Color.DARKGRAY);
        mAreaColors.put("militaryColor",  Color.rgb(0xff, 0x55, 0x55, 0.5d));
        mAreaColors.put("leisureUndefinedColor", Color.RED);
        mAreaColors.put("farmyardColor",  Color.rgb(0xf5, 0xdc, 0xba));
        mAreaColors.put("rockColor",  Color.rgb(0xc1, 0xbf, 0xbf));
        mAreaColors.put("glacierColor",  Color.rgb(0xaa, 0xd3, 0xdf));
        mAreaColors.put("beachColor",  Color.rgb(0xfc, 0xd6, 0xa4));

    }

    private static Color getStreetColor(int streetTypeId) {
        if (mStreetColors == null) {
            initStreetColors();
        }
        return mStreetColors.get(streetTypeId);
    }


    public static Paint getAreaColor(JsonObject area) {
        if (mAreaColors == null) {
            initStreetColors();
        }
        int areaType = (int) area.get("areaType");
        if (areaType == OSMUtils.AREA_TYPE_BUILDING) {
            return mAreaColors.get("buildingColor");
        }
        if (areaType == OSMUtils.AREA_TYPE_WATER) {
            return mAreaColors.get("waterColor");
        }
        if (areaType == OSMUtils.AREA_TYPE_RAILWAY) {
            return mAreaColors.get("railwayAreaColor");
        }
        if (areaType == OSMUtils.AREA_TYPE_NATURAL) {
            return getNaturalAreaColor(area);
        }
        if (areaType == OSMUtils.AREA_TYPE_LANDUSE) {
            return getLanduseAreaColor(area);
        }
        return Color.LIGHTGREEN;
    }

    public static Paint getNaturalAreaColor(JsonObject area) {
        JsonObject tags = (JsonObject) area.get("tags");
        if (tags == null) {
            return mAreaColors.get("naturalColor");
        }
        String waterway = (String) tags.get("waterway");
        if (waterway != null) {
            if (!waterway.equals("riverbank")) {
                return mAreaColors.get("waterColor");
            }
        }
        String natural = (String) tags.get("natural");
        if (natural == null) {
            return mAreaColors.get("naturalColor");
        }
        if (OSMUtils.NATURAL_WATER_TYPE_SET.contains(natural))
            return mAreaColors.get("waterColor");
        if (natural.equals("wood"))
            return mAreaColors.get("forestAreaColor");
        if (natural.equals("glacier"))
            return mAreaColors.get("glacierColor");
        if (natural.equals("cliff"))
            return mAreaColors.get("cliffColor");
        if (natural.equals("rock") || natural.equals("scree"))
            return mAreaColors.get("rockColor");
        if (natural.equals("beach"))
            return mAreaColors.get("beachColor");
        if (natural.equals("grassland"))
            return mAreaColors.get("grassColor");

        return mAreaColors.get("naturalColor");
    }

    public static Paint getLanduseAreaColor(JsonObject area) {
        JsonObject tags = (JsonObject) area.get("tags");
        if (tags == null || tags.get("landuse") == null) {
            return mAreaColors.get("landuseColor");
        }
        String landuse = (String) tags.get("landuse");
        if (landuse.equals("railway"))
            return mAreaColors.get("railwayAreaColor");
        if (landuse.equals("residential"))
            return mAreaColors.get("residentialColor");
        if (landuse.equals("commercial") || landuse.equals("retail"))
            return mAreaColors.get("commercialColor");
        if (landuse.equals("field") || landuse.equals("farmland") || landuse.equals("farm"))
            return mAreaColors.get("farmColor");
        if (landuse.equals("grass") || landuse.equals("meadow") || landuse.equals("grassland"))
            return mAreaColors.get("grassColor");
        if (landuse.equals("greenfield") || landuse.equals("brownfield"))
            return mAreaColors.get("greenfieldColor");
        if (landuse.equals("industrial"))
            return mAreaColors.get("industrialColor");
        if (landuse.equals("forest"))
            return mAreaColors.get("forestAreaColor");
        if (landuse.equals("cemetery"))
            return mAreaColors.get("grassColor");
        if (landuse.equals("village_green") || landuse.equals("recreation_ground"))
            return mAreaColors.get("villageGreenAreaColor");
        if (landuse.equals("farmyard"))
            return mAreaColors.get("farmyardColor");
        if (landuse.equals("quarry"))
            return mAreaColors.get("rockColor");
        if (landuse.equals("military"))
            return mAreaColors.get("militaryColor");
        if (OSMUtils.LANDUSE_NATURAL_TYPE_SET.contains(landuse))
            return mAreaColors.get("naturalColor");
        if (OSMUtils.LANDUSE_WATER_TYPE_SET.contains(landuse))
            return mAreaColors.get("waterColor");

        return mAreaColors.get("landuseUndefinedColor");
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

    private static int getRailwayPenWidthForZoom(int zoom) {
        if (zoom == 20) {
            return 12;
        }
        if (zoom == 19) {
            return 8;
        }
        if (zoom == 18) {
            return 4;
        }
        if (zoom == 17) {
            return 3;
        }
        if (zoom == 16) {
            return 2;
        }
        return 1;
    }
    private static int getAdminLinePenWidthForZoom(int zoom) {
        if (zoom == 20) {
            return 8;
        }
        if (zoom == 19) {
            return 8;
        }
        if (zoom == 18) {
            return 4;
        }
        if (zoom == 17) {
            return 3;
        }
        if (zoom == 16) {
            return 2;
        }
        return 1;
    }

    public static void amendWay(JsonObject way, Polyline wayLine, int zoom) {
        int streetTypeId = (int) way.get("streetTypeId");
        int streetTypeInfo = (int) way.get("streetInfo");
        int isTunnel = (streetTypeInfo & 255) >> 7;
        int isBridge = (streetTypeInfo & 511) >> 8;
        int width = getStreetWidth(streetTypeId, zoom);
        if (isBridge == 1) {
            wayLine.setStroke(getStreetColor(streetTypeId).brighter());
        } else if (isTunnel == 1) {
            wayLine.setStroke(getStreetColor(streetTypeId).darker());
        } else {
            wayLine.setStroke(getStreetColor(streetTypeId));
        }

        wayLine.setStrokeWidth(width);
        wayLine.setSmooth(true);
        wayLine.setStrokeLineCap(StrokeLineCap.ROUND);
        wayLine.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }

    public static void amendArea(JsonObject area, Polyline areaLine, int zoom) {
        areaLine.setFill(getAreaColor(area));
        areaLine.setStroke(Color.LIGHTGRAY);
    }

    public static void amendLineArea(JsonObject area, Polyline areaLine, int zoom) {
        areaLine.setStroke(getAreaColor(area));
    }

    public static void amendRailway(JsonObject area, Polyline areaLine, int zoom) {
        areaLine.setStroke(mAreaColors.get("railwayColor"));
        double width = getRailwayPenWidthForZoom(zoom);
        areaLine.getStrokeDashArray().addAll(2 * width);
        areaLine.setStrokeWidth(width);
    }

    public static void amendAdminLine(JsonObject adminLine, Polyline areaLine, int zoom) {
        int adminLevel = (int) adminLine.get("adminLevel");
        double width = getAdminLinePenWidthForZoom(zoom);
        areaLine.getStrokeDashArray().addAll(2 * width);
        areaLine.setStroke(mAreaColors.get("adminAreaColor"));
        areaLine.setStrokeWidth(width);
    }
}
