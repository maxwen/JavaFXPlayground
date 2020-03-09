package sample;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import javafx.scene.chart.XYChart;
import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.util.Collections;
import java.util.List;
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
            mEdgeConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/edge.db");
            mAreaConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/area.db");
            mAddressConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/adress.db");
            mWaysConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/ways.db");
            mNodeConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/nodes.db");
            mAdminConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/admin.db");
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
        StringBuffer buffer = new StringBuffer();
        buffer.append('(');
        typeFilterList.stream().forEach(val -> {
            buffer.append(val + ",");
        });
        String bufferString = buffer.toString().substring(0, buffer.length() - 3);
        bufferString += ')';
        return bufferString;
    }

    public JsonArray getWaysInBboxWithGeom(double lonRangeMin, double latRangeMin, double lonRangeMax, double latRangeMax, List<Integer> typeFilterList) {
        Statement stmt = null;
        JsonArray ways = new JsonArray();

        try {
            long t = System.currentTimeMillis();
            stmt = mWaysConnection.createStatement();
            ResultSet rs;
            if (typeFilterList != null && typeFilterList.size() != 0) {
                rs = stmt.executeQuery(String.format("SELECT wayId, tags, refs, streetInfo, name, ref, maxspeed, poiList, layer, AsText(geom) FROM wayTable WHERE ROWID IN (SELECT rowid FROM idx_wayTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) AND streetTypeId IN %s ORDER BY streetTypeId, layer", lonRangeMin, latRangeMin, lonRangeMax, latRangeMax, filterListToIn(typeFilterList)));
            } else {
                rs = stmt.executeQuery(String.format("SELECT wayId, tags, refs, streetInfo, name, ref, maxspeed, poiList, layer, AsText(geom) FROM wayTable WHERE ROWID IN (SELECT rowid FROM idx_wayTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) ORDER BY streetTypeId, layer", lonRangeMin, latRangeMin, lonRangeMax, latRangeMax));
            }

            int count = 0;
            while (rs.next()) {
                JsonObject way = new JsonObject();
                way.put("id", rs.getInt(1));
                way.put("name", rs.getString(5));
                way.put("nameRef", rs.getString(6));
                way.put("layer", rs.getString(9));
                way.put("coords", createCoordsFromLineString(rs.getString(10)));
                way.put("streetInfo", rs.getString(4));
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

    public JsonArray getAreasInBboxWithGeom(double lonRangeMin, double latRangeMin, double lonRangeMax, double latRangeMax, List<Integer> typeFilterList, boolean withSimplify, double tolerance) {
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
                rs = stmt.executeQuery(String.format("SELECT osmId, type, tags, layer, %s FROM areaTable WHERE ROWID IN (SELECT rowid FROM idx_areaTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) AND type IN %s ORDER BY type", geom, lonRangeMin, latRangeMin, lonRangeMax, latRangeMax, filterListToIn(typeFilterList)));
            } else {
                rs = stmt.executeQuery(String.format("SELECT osmId, type, tags, layer, %s FROM areaTable WHERE ROWID IN (SELECT rowid FROM idx_areaTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) ORDER BY type", geom, lonRangeMin, latRangeMin, lonRangeMax, latRangeMax));
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
                area.put("layer", rs.getInt(4));
                area.put("coords", createCoordsFromPolygonString(rs.getString(5)));
                String tags = rs.getString(3);
                try {
                    if (tags != null && tags.length() != 0) {
                        area.put("tags", Jsoner.deserialize(tags));
                    }
                } catch (JsonException e) {
                    System.out.println(e.getMessage());
                }
                areas.add(area);
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
