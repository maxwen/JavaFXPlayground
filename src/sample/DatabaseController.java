package sample;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;
import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseController {

    private Connection mEdgeConnection;
    private Connection mAreaConnection;
    private Connection mAddressConnection;
    private Connection mWaysConnection;
    private Connection mNodeConnection;
    private Connection mAdminConnection;
    private static DatabaseController sInstance;
    private boolean mConnected;

    public static DatabaseController getInstance() {
        if (sInstance == null) {
            sInstance = new DatabaseController();
        }
        return sInstance;
    }

    private DatabaseController() {
    }

    public boolean connextAll() {
        try {
            mEdgeConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2.new/edge.db");
            mAreaConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2.new/area.db");
            mAddressConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2.new/adress.db");
            mWaysConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2.new/ways.db");
            mNodeConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2.new/nodes.db");
            mAdminConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2.new/admin.db");
            mConnected = true;
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void disconnectAll() {
        if (!mConnected) {
            return;
        }
        try {
            mEdgeConnection.close();
            mAreaConnection.close();
            mAddressConnection.close();
            mWaysConnection.close();
            mNodeConnection.close();
            mAdminConnection.close();
            mConnected = false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (NullPointerException e) {
        }
    }

    private Connection connect(String url) throws SQLException {
        Connection conn = null;
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        conn = DriverManager.getConnection(url, config.toProperties());
        Statement stmt = conn.createStatement();
        stmt.execute("PRAGMA cache_size=40000");
        stmt.execute("PRAGMA page_size=4096");
        stmt.execute("PRAGMA temp_store=MEMORY");
        stmt.execute("SELECT load_extension('mod_spatialite.so')");
        stmt.close();
        return conn;
    }

    private String filterListToIn(List<Integer> typeFilterList) {
        if (typeFilterList != null) {
            StringBuffer buffer = new StringBuffer();
            typeFilterList.stream().forEach(val -> {
                buffer.append(val + ",");
            });

            String bufferString = buffer.toString().substring(0, buffer.length() - 1);
            bufferString = "(" + bufferString + ")";
            return bufferString;
        }
        return "";
    }

    public JsonArray getWaysInBboxWithGeom(double lonRangeMin, double latRangeMin, double lonRangeMax, double latRangeMax,
                                           List<Integer> typeFilterList, Map<Integer, List<Shape>> polylines,
                                           MainController controller) {
        Statement stmt = null;
        JsonArray ways = new JsonArray();

        try {
            long t = System.currentTimeMillis();
            stmt = mWaysConnection.createStatement();
            ResultSet rs;
            if (typeFilterList != null && typeFilterList.size() != 0) {
                rs = stmt.executeQuery(String.format("SELECT wayId, tags, refs, streetInfo, name, ref, maxspeed, poiList, layer, AsText(geom) FROM wayTable WHERE ROWID IN (SELECT rowid FROM idx_wayTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) AND streetTypeId IN %s ORDER BY streetTypeId", lonRangeMin, latRangeMin, lonRangeMax, latRangeMax, filterListToIn(typeFilterList)));
            } else {
                rs = stmt.executeQuery(String.format("SELECT wayId, tags, refs, streetInfo, name, ref, maxspeed, poiList, layer, AsText(geom) FROM wayTable WHERE ROWID IN (SELECT rowid FROM idx_wayTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) ORDER BY streetTypeId", lonRangeMin, latRangeMin, lonRangeMax, latRangeMax));
            }

            int count = 0;
            while (rs.next()) {
                JsonObject way = new JsonObject();
                way.put("id", rs.getInt(1));
                way.put("name", rs.getString(5));
                way.put("nameRef", rs.getString(6));
                int layer = rs.getInt(9);
                way.put("layer", layer);
                int streetTypeInfo = rs.getInt(4);
                way.put("streetInfo", streetTypeInfo);
                // streetTypeId, oneway, roundabout, tunnel, bridge = osmParserData.decodeStreetInfo2(streetInfo)
                /*oneway=(streetInfo&63)>>4
                roundabout=(streetInfo&127)>>6
                tunnel=(streetInfo&255)>>7
                bridge=(streetInfo&511)>>8
                streetTypeId=(streetInfo&15)*/

                int streetTypeId = streetTypeInfo & 15;
                int isTunnel = (streetTypeInfo & 255) >> 7;
                int isBridge = (streetTypeInfo & 511) >> 8;

                way.put("streetTypeId", streetTypeId);
                String tags = rs.getString(2);
                try {
                    if (tags != null && tags.length() != 0) {
                        way.put("tags", Jsoner.deserialize(tags));
                    }
                } catch (JsonException e) {
                    System.out.println(e.getMessage());
                }
                try {
                    String refs = rs.getString(3);
                    if (refs != null && refs.length() != 0) {
                        way.put("refs", Jsoner.deserialize(refs));
                    }
                } catch (JsonException e) {
                    System.out.println(e.getMessage());
                }
                String poiList = rs.getString(8);
                try {
                    if (poiList != null && poiList.length() != 0) {
                        way.put("poiList", Jsoner.deserialize(poiList));
                    }
                } catch (JsonException e) {
                    System.out.println(e.getMessage());
                }

                ways.add(way);

                boolean showCasing = controller.getZoom() >= 17;
                Polyline polylineCasing = null;
                Polyline polyline = controller.displayCoordsPolyline(createCoordsFromLineString(rs.getString(10)));
                if (showCasing) {
                    polylineCasing = controller.clonePolyline(polyline);
                    OSMStyle.amendWay(way, polylineCasing, controller.getZoom(), true);
                    OSMStyle.amendWay(way, polyline, controller.getZoom(), false);
                } else {
                    OSMStyle.amendWay(way, polyline, controller.getZoom(), false);
                }
                // ways that are tunnels are drawn specific but must still be on same level as any other way
                // cause we want to seem them
                if (layer < 0 || isTunnel == 1) {
                    polylines.get(2).add(polyline);
                } else if (isBridge == 1) {
                    polylines.get(3).add(polyline);
                } else {
                    polylines.get(2).add(polyline);
                }
                if (showCasing) {
                    polylines.get(-1).add(polylineCasing);
                }

                count++;
            }
            System.out.println("getWaysInBboxWithGeom " + count + " " + (System.currentTimeMillis() - t));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
        }
        return ways;
    }

    public JsonArray getAreasInBboxWithGeom(double lonRangeMin, double latRangeMin, double lonRangeMax, double latRangeMax,
                                            List<Integer> typeFilterList, boolean withSimplify, double tolerance,
                                            Map<Integer, List<Shape>> polylines, MainController controller) {
        Statement stmt = null;
        JsonArray areas = new JsonArray();

        try {
            long t = System.currentTimeMillis();
            stmt = mAreaConnection.createStatement();
            ResultSet rs;
            tolerance = GISUtils.degToMeter(tolerance);

            String geom = "AsText(geom)";
            if (withSimplify) {
                geom = String.format("AsText(Simplify(geom, %f))", tolerance);
            }
            System.out.println(withSimplify + " " + tolerance + " " + typeFilterList);
            if (typeFilterList != null && typeFilterList.size() != 0) {
                rs = stmt.executeQuery(String.format("SELECT osmId, type, tags, layer, %s FROM areaTable WHERE ROWID IN (SELECT rowid FROM idx_areaTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) AND type IN %s ORDER BY layer", geom, lonRangeMin, latRangeMin, lonRangeMax, latRangeMax, filterListToIn(typeFilterList)));
            } else {
                rs = stmt.executeQuery(String.format("SELECT osmId, type, tags, layer, %s FROM areaTable WHERE ROWID IN (SELECT rowid FROM idx_areaTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) ORDER BY layer", geom, lonRangeMin, latRangeMin, lonRangeMax, latRangeMax));
            }

            /*osmId=x[0]
            areaType=x[1]
            tags=self.decodeTags(x[2])
            layer=int(x[3])
            polyStr=x[4]*/

            int count = 0;
            while (rs.next()) {
                JsonObject area = new JsonObject();
                area.put("osmId", rs.getInt(1));
                area.put("areaType", rs.getInt(2));
                int layer = rs.getInt(4);
                area.put("layer", layer);
                String tags = rs.getString(3);
                try {
                    if (tags != null && tags.length() != 0) {
                        area.put("tags", Jsoner.deserialize(tags));
                    }
                } catch (JsonException e) {
                    System.out.println(e.getMessage());
                }
                areas.add(area);

                JsonArray coords = createCoordsFromPolygonString(rs.getString(5));
                for (int j = 0; j < coords.size(); j++) {
                    JsonArray innerCoords = (JsonArray) coords.get(j);
                    Polygon polygon = controller.displayCoordsPolygon(innerCoords);
                    OSMStyle.amendArea(area, polygon, controller.getZoom());
                    if (layer < 0) {
                        polylines.get(-1).add(polygon);
                    } else {
                        polylines.get(0).add(polygon);
                    }
                }

                count++;
            }

            System.out.println("getAreasInBboxWithGeom " + count + " " + (System.currentTimeMillis() - t));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
        }
        return areas;
    }

    public JsonArray getLineAreasInBboxWithGeom(double lonRangeMin, double latRangeMin, double lonRangeMax, double latRangeMax,
                                                List<Integer> typeFilterList, boolean withSimplify, double tolerance,
                                                Map<Integer, List<Shape>> polylines, MainController controller) {
        Statement stmt = null;
        JsonArray areas = new JsonArray();

        try {
            long t = System.currentTimeMillis();
            stmt = mAreaConnection.createStatement();
            ResultSet rs;
            tolerance = GISUtils.degToMeter(tolerance);

            String geom = "AsText(geom)";
            if (withSimplify) {
                geom = String.format("AsText(Simplify(geom, %f))", tolerance);
            }
            System.out.println(withSimplify + " " + tolerance + " " + typeFilterList);

            if (typeFilterList != null && typeFilterList.size() != 0) {
                rs = stmt.executeQuery(String.format("SELECT osmId, type, tags, layer, %s FROM areaLineTable WHERE ROWID IN (SELECT rowid FROM idx_areaLineTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) AND type IN %s ORDER BY layer", geom, lonRangeMin, latRangeMin, lonRangeMax, latRangeMax, filterListToIn(typeFilterList)));
            } else {
                rs = stmt.executeQuery(String.format("SELECT osmId, type, tags, layer, %s FROM areaLineTable WHERE ROWID IN (SELECT rowid FROM idx_areaLineTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) ORDER BY layer", geom, lonRangeMin, latRangeMin, lonRangeMax, latRangeMax));
            }
            int count = 0;
            while (rs.next()) {
                JsonObject area = new JsonObject();
                area.put("osmId", rs.getInt(1));
                int areaType = rs.getInt(2);
                area.put("areaType", areaType);
                int layer = rs.getInt(4);
                area.put("layer", layer);
                JsonObject tags = null;
                String tagsStr = rs.getString(3);
                try {
                    if (tagsStr != null && tagsStr.length() != 0) {
                        tags = (JsonObject) Jsoner.deserialize(tagsStr);
                        area.put("tags", tags);
                    }
                } catch (JsonException e) {
                    System.out.println(e.getMessage());
                }
                areas.add(area);
                count++;

                Polyline polyline = controller.displayCoordsPolyline(createCoordsFromLineString(rs.getString(5)));
                if (areaType == OSMUtils.AREA_TYPE_RAILWAY && tags != null) {
                    Object isRailway = tags.get("railway");
                    Object isTunnel =  tags.get("tunnel");
                    Object isBridge = tags.get("bridge");
                    if (isRailway != null && isRailway.equals("rail")) {
                        if (isBridge != null && isBridge.equals("yes")) {
                            polylines.get(3).add(polyline);
                        } else if (isTunnel != null && isTunnel.equals("yes")) {
                            // TODO like ways - do we want to show railway tunnels? if yes change to 2 here
                            polylines.get(-1).add(polyline);
                        } else {
                            polylines.get(2).add(polyline);
                        }
                        OSMStyle.amendRailway(area, polyline, controller.getZoom());
                        continue;
                    } else {
                        polylines.get(0).add(polyline);
                    }
                } else {
                    polylines.get(0).add(polyline);
                }
                OSMStyle.amendLineArea(area, polyline, controller.getZoom());
            }

            System.out.println("getLineAreasInBboxWithGeom " + count + " " + (System.currentTimeMillis() - t));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
        }
        return areas;
    }

    public JsonArray getAdminLineInBboxWithGeom(double lonRangeMin, double latRangeMin, double lonRangeMax, double latRangeMax,
                                                String typeFilterString, boolean withSimplify, double tolerance,
                                                Map<Integer, List<Shape>> polylines, MainController controller) {
        Statement stmt = null;
        JsonArray adminLines = new JsonArray();

        try {
            long t = System.currentTimeMillis();
            stmt = mAdminConnection.createStatement();
            ResultSet rs;
            tolerance = GISUtils.degToMeter(tolerance);

            String geom = "AsText(geom)";
            if (withSimplify) {
                geom = String.format("AsText(Simplify(geom, %f))", tolerance);
            }
            System.out.println(withSimplify + " " + tolerance + " " + typeFilterString);

            if (typeFilterString != null) {
                rs = stmt.executeQuery(String.format("SELECT osmId, adminLevel, %s FROM adminLineTable WHERE ROWID IN (SELECT rowid FROM idx_adminLineTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) AND adminLevel IN %s", geom, lonRangeMin, latRangeMin, lonRangeMax, latRangeMax, typeFilterString));
            } else {
                rs = stmt.executeQuery(String.format("SELECT osmId, adminLevel, %s FROM adminLineTable WHERE ROWID IN (SELECT rowid FROM idx_adminLineTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f))", geom, lonRangeMin, latRangeMin, lonRangeMax, latRangeMax));
            }
            int count = 0;
            while (rs.next()) {
                JsonObject adminLine = new JsonObject();
                adminLine.put("osmId", rs.getInt(1));
                adminLine.put("adminLevel", rs.getInt(2));
                adminLines.add(adminLine);
                Polyline polyline = controller.displayCoordsPolyline(createCoordsFromLineString(rs.getString(3)));
                OSMStyle.amendAdminLine(adminLine, polyline, controller.getZoom());
                polylines.get(1).add(polyline);
                count++;
            }

            System.out.println("getAdminLineInBboxWithGeom " + count + " " + (System.currentTimeMillis() - t));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
        }
        return adminLines;
    }

    private JsonArray createCoordsFromLineString(String lineString) {
        lineString = lineString.substring(11, lineString.length() - 1);
        return parseCoords(lineString);
    }

    private JsonArray parseCoords(String coordsStr) {
        JsonArray coords = new JsonArray();
        String[] pairs = coordsStr.split(",");
        for (String pair : pairs) {
            String[] coord = pair.trim().split(" ");
            JsonArray c = new JsonArray();
            double lon = Double.valueOf(coord[0].trim());
            c.add(lon);
            double lat = Double.valueOf(coord[1].trim());
            c.add(lat);
            coords.add(c);
        }
        return coords;
    }

    private JsonArray createCoordsFromMultiPolygon(String coordsStr) {
        JsonArray coords = new JsonArray();
        String[] polyParts = coordsStr.split("\\)\\), \\(\\(");
        if (polyParts.length == 1) {
            String[] polyParts2 = coordsStr.split("\\), \\(");
            for (String poly : polyParts2) {
                coords.add(parseCoords(poly));
            }
        } else {
            for (String poly : polyParts) {
                String[] polyParts2 = poly.split("\\), \\(");
                for (String innerPoly : polyParts2) {
                    coords.add(parseCoords(innerPoly));
                }
            }
        }
        return coords;
    }

    private JsonArray createCoordsFromPolygonString(String coordsStr) {
        if (coordsStr.startsWith("MULTIPOLYGON(((")) {
            return createCoordsFromMultiPolygon(coordsStr.substring("MULTIPOLYGON(((".length(), coordsStr.length() - 3));
        } else if (coordsStr.startsWith("POLYGON((")) {
            return createCoordsFromMultiPolygon(coordsStr.substring("POLYGON((".length(), coordsStr.length() - 2));
        }
        return new JsonArray();
    }
}
