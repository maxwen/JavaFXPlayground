package sample;

import java.util.HashSet;
import java.util.Set;

public class OSMUtils {
    public static final int STREET_TYPE_SERVICE = 0;
    public static final int STREET_TYPE_TERTIARY_LINK = 1;
    public static final int STREET_TYPE_SECONDARY_LINK = 2;
    public static final int STREET_TYPE_PRIMARY_LINK = 3;
    public static final int STREET_TYPE_TRUNK_LINK = 4;
    public static final int STREET_TYPE_MOTORWAY_LINK = 5;
    public static final int STREET_TYPE_ROAD = 6;
    public static final int STREET_TYPE_UNCLASSIFIED = 7;
    public static final int STREET_TYPE_LIVING_STREET = 8;
    public static final int STREET_TYPE_RESIDENTIAL = 9;
    public static final int STREET_TYPE_TERTIARY = 10;
    public static final int STREET_TYPE_SECONDARY = 11;
    public static final int STREET_TYPE_PRIMARY = 12;
    public static final int STREET_TYPE_TRUNK = 13;
    public static final int STREET_TYPE_MOTORWAY = 14;

    public static final int AREA_TYPE_LANDUSE = 1;
    public static final int AREA_TYPE_NATURAL = 2;
    public static final int AREA_TYPE_HIGHWAY_AREA = 3;
    public static final int AREA_TYPE_AEROWAY = 4;
    public static final int AREA_TYPE_RAILWAY = 5;
    public static final int AREA_TYPE_TOURISM = 6;
    public static final int AREA_TYPE_AMENITY = 7;
    public static final int AREA_TYPE_BUILDING = 8;
    public static final int AREA_TYPE_LEISURE = 9;
    public static final int AREA_TYPE_WATER = 10;

    public static final String ADMIN_LEVEL_SET = "(2, 4, 6, 8)";

    public static final Set<String> LANDUSE_NATURAL_TYPE_SET = Set.of("forest","grass","field","farm","farmland","meadow",
            "greenfield","brownfield","farmyard","recreation_ground","village_green","allotments","orchard");
    public static final Set<String> LANDUSE_WATER_TYPE_SET= Set.of("reservoir","basin","water");
    public static final Set<String> NATURAL_WATER_TYPE_SET = Set.of("water", "riverbank", "wetland", "marsh", "mud");
}
