package sample;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseController {

    private Connection mEdgeConnection;
    private Connection mAreaConnection;
    private Connection mAddressConnection;
    private Connection mWaysConnection;
    private Connection mNodeConnection;
    private Connection mAdminConnection;

    private Pattern mCoordsMatcher;

    public DatabaseController() {
        mCoordsMatcher = Pattern.compile("[0-9.]+|[^0-9.]+");
    }

    public boolean connextAll() {
        try {
            mEdgeConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/edge.db");
            mAreaConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/area.db");
            mAddressConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/adress.db");
            mWaysConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/ways.db");
            mNodeConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/nodes.db");
            mAdminConnection = connect("jdbc:sqlite:/home/maxl/workspaces/car-dash/data2/admin.db");
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void disconnectAll() {
        try {
            mEdgeConnection.close();
            mAreaConnection.close();
            mAddressConnection.close();
            mWaysConnection.close();
            mNodeConnection.close();
            mAdminConnection.close();
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

    public JsonArray getWaysInBboxWithGeom(double lonRangeMin, double latRangeMin, double lonRangeMax, double latRangeMax) {
        // self.cursorWay.execute('SELECT wayId, tags, refs, streetInfo, name, ref, maxspeed, poiList, layer, AsText(geom) FROM wayTable WHERE ROWID IN (SELECT rowid FROM idx_wayTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) ORDER BY streetTypeId, layer'%(lonRangeMin, latRangeMin, lonRangeMax, latRangeMax))
        Statement stmt = null;
        JsonArray ways = new JsonArray();

        try {
            long t = System.currentTimeMillis();
            stmt = mWaysConnection.createStatement();
            ResultSet rs = stmt.executeQuery(String.format("SELECT wayId, tags, refs, streetInfo, name, ref, maxspeed, poiList, layer, AsText(geom) FROM wayTable WHERE ROWID IN (SELECT rowid FROM idx_wayTable_geom WHERE rowid MATCH RTreeIntersects(%f, %f, %f, %f)) ORDER BY streetTypeId, layer", lonRangeMin, latRangeMin, lonRangeMax, latRangeMax));
            System.out.println(System.currentTimeMillis() - t);


            /*def wayFromDBWithCoordsString(self, x):
            wayId=int(x[0])
            refs=pickle.loads(x[2])
            tags=self.decodeTags(x[1])
            streetInfo=int(x[3])
            name=x[4]
            nameRef=x[5]
            maxspeed=int(x[6])
            poiList=None
            if x[7]!=None:
            poiList=pickle.loads(x[7])
            layer=int(x[8])
            coordsStr=x[9]
            return (wayId, tags, refs, streetInfo, name, nameRef, maxspeed, poiList, layer, coordsStr)*/

            int count = 0;
            while (rs.next()) {
                //System.out.println(rs.toString());
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
                //System.out.println(way.toString());
                ways.add(way);
                count++;
            }
            System.out.println(count);

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
}
