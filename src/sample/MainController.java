package sample;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML
    Button quitButton;
    @FXML
    Button zoomInButton;
    @FXML
    Button zoomOutButton;
    @FXML
    Button stepLeftButton;
    @FXML
    Button stepUpButton;
    @FXML
    Button stepDownButton;
    @FXML
    Button stepRightButton;
    @FXML
    Pane mainPane;

    private static final int MIN_ZOOM = 12;
    private static final int MAX_ZOOM = 20;
    private int mMapZoom = 16;
    private double mMapZeroX;
    private double mMapZeroY;
    private double mCenterLat = 47.793938;
    private double mCenterLon = 12.992203;
    private double mCenterPosX;
    private double mCenterPosY;
    private boolean mMouseMoving;
    private Point2D mMovePoint;
    private List<Double> mFetchBBox;
    private long mLastMoveHandled;

    EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED) {
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_RELEASED) {
                mMouseMoving = false;
                mMovePoint = null;
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                if (System.currentTimeMillis() - mLastMoveHandled < 100) {
                    return;
                }
                if (!mMouseMoving) {
                    mMovePoint = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
                    mMouseMoving = true;
                    mLastMoveHandled = 0;
                } else {
                    if (mMovePoint != null) {
                        int diffX = (int) (mMovePoint.getX() - mouseEvent.getSceneX());
                        int diffY = (int) (mMovePoint.getY() - mouseEvent.getSceneY());

                        moveMap(diffX, diffY);
                        mLastMoveHandled = System.currentTimeMillis();
                        mMovePoint = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
                    }
                }
            }
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("initialize");

        calcMapCenterPos();

        quitButton.setOnAction(e -> {
            Platform.exit();
        });
        zoomInButton.setOnAction(e -> {
            mMapZoom++;
            mMapZoom = Math.min(MAX_ZOOM, mMapZoom);
            calcMapCenterPos();
            loadWays();
        });
        zoomOutButton.setOnAction(e -> {
            mMapZoom--;
            mMapZoom = Math.max(MIN_ZOOM, mMapZoom);
            calcMapCenterPos();
            loadWays();
        });
        stepLeftButton.setOnAction(e -> {
            moveMap(-100, 0);
        });
        stepRightButton.setOnAction(e -> {
            moveMap(100, 0);
        });
        stepUpButton.setOnAction(e -> {
            moveMap(0, -100);
        });
        stepDownButton.setOnAction(e -> {
            moveMap(0, 100);
        });
        //mainPane.setOnMousePressed(mouseHandler);
        mainPane.setOnMouseReleased(mouseHandler);
        mainPane.setOnMouseDragged(mouseHandler);
    }


    private List<Integer> getStreetTypeListForZoom() {
        if (mMapZoom <= 13) {
            List<Integer> typeFilterList = new ArrayList<>();
            Collections.addAll(typeFilterList, OSMUtils.STREET_TYPE_MOTORWAY,
                    OSMUtils.STREET_TYPE_MOTORWAY_LINK,
                    OSMUtils.STREET_TYPE_TRUNK,
                    OSMUtils.STREET_TYPE_TRUNK_LINK,
                    OSMUtils.STREET_TYPE_PRIMARY,
                    OSMUtils.STREET_TYPE_PRIMARY_LINK,
                    OSMUtils.STREET_TYPE_SECONDARY,
                    OSMUtils.STREET_TYPE_SECONDARY_LINK);
            return typeFilterList;
        } else if (mMapZoom <= 16) {
            List<Integer> typeFilterList = new ArrayList<>();
            Collections.addAll(typeFilterList, OSMUtils.STREET_TYPE_MOTORWAY,
                    OSMUtils.STREET_TYPE_MOTORWAY_LINK,
                    OSMUtils.STREET_TYPE_TRUNK,
                    OSMUtils.STREET_TYPE_TRUNK_LINK,
                    OSMUtils.STREET_TYPE_PRIMARY,
                    OSMUtils.STREET_TYPE_PRIMARY_LINK,
                    OSMUtils.STREET_TYPE_SECONDARY,
                    OSMUtils.STREET_TYPE_SECONDARY_LINK,
                    OSMUtils.STREET_TYPE_TERTIARY,
                    OSMUtils.STREET_TYPE_TERTIARY_LINK,
                    OSMUtils.STREET_TYPE_RESIDENTIAL,
                    OSMUtils.STREET_TYPE_ROAD,
                    OSMUtils.STREET_TYPE_UNCLASSIFIED);
            return typeFilterList;
        } else {
            return null;
        }
    }

    private List<Integer> getAreaTypeListForZoom() {
        if (mMapZoom <= 14) {
            List<Integer> typeFilterList = new ArrayList<>();
            Collections.addAll(typeFilterList, OSMUtils.AREA_TYPE_HIGHWAY_AREA,
                    OSMUtils.AREA_TYPE_AEROWAY,
                    OSMUtils.AREA_TYPE_RAILWAY);
            return typeFilterList;
        } else if (mMapZoom <= 15) {
            List<Integer> typeFilterList = new ArrayList<>();
            Collections.addAll(typeFilterList, OSMUtils.AREA_TYPE_LANDUSE,
                    OSMUtils.AREA_TYPE_NATURAL,
                    OSMUtils.AREA_TYPE_HIGHWAY_AREA,
                    OSMUtils.AREA_TYPE_AEROWAY,
                    OSMUtils.AREA_TYPE_RAILWAY);
            return typeFilterList;
        } else if (mMapZoom <= 16) {
            List<Integer> typeFilterList = new ArrayList<>();
            Collections.addAll(typeFilterList, OSMUtils.AREA_TYPE_LANDUSE,
                    OSMUtils.AREA_TYPE_NATURAL,
                    OSMUtils.AREA_TYPE_HIGHWAY_AREA,
                    OSMUtils.AREA_TYPE_AEROWAY,
                    OSMUtils.AREA_TYPE_RAILWAY,
                    OSMUtils.AREA_TYPE_TOURISM,
                    OSMUtils.AREA_TYPE_LEISURE);
            return typeFilterList;
        } else {
            return null;
        }
    }

    public void loadWays() {
        calcMapZeroPos();
        long t = System.currentTimeMillis();
        System.out.println("loadWays " + mMapZoom);

        mainPane.getChildren().clear();
        mFetchBBox = getVisibleBBoxDegWithMargin();
        JsonArray ways = DatabaseController.getInstance().getWaysInBboxWithGeom(mFetchBBox.get(0), mFetchBBox.get(1),
                mFetchBBox.get(2), mFetchBBox.get(3), getStreetTypeListForZoom());

        for (int i = 0; i < ways.size(); i++) {
            JsonObject way = (JsonObject) ways.get(i);
            JsonArray coords = (JsonArray) way.get("coords");
            displayCoords(coords, Color.BLACK);
        }

        System.out.println("loadWays " + (System.currentTimeMillis() - t));

        t = System.currentTimeMillis();
        JsonArray areas = DatabaseController.getInstance().getAreasInBboxWithGeom(mFetchBBox.get(0), mFetchBBox.get(1),
                mFetchBBox.get(2), mFetchBBox.get(3), getAreaTypeListForZoom(), mMapZoom <= 14, mMapZoom <= 14 ? 10.0 : 0.0);

        for (int i = 0; i < areas.size(); i++) {
            JsonObject area = (JsonObject) areas.get(i);
            int areaType = (int) area.get("areaType");
            JsonArray coords = (JsonArray) area.get("coords");

            for (int j = 0; j < coords.size(); j++) {
                JsonArray coords2 = (JsonArray) coords.get(j);
                displayCoords(coords2, Color.RED);
            }
        }

        System.out.println("loadAreas " + (System.currentTimeMillis() - t));
    }

    private double getPrefetchBoxMargin() {
        if (mMapZoom < 14) {
            return 0.1;
        } else if (mMapZoom < 17) {
            return 0.02;
        }
        return 0.005;
    }

    private Double getPixelXPosForLocationDeg(double lon) {
        return getPixelXPosForLocationRad(GISUtils.deg2rad(lon));
    }

    private Double getPixelXPosForLocationRad(double lon) {
        return GISUtils.lon2pixel(mMapZoom, lon);
    }

    private Double getPixelYPosForLocationDeg(double lat) {
        return getPixelYPosForLocationRad(GISUtils.deg2rad(lat));
    }

    private Double getPixelYPosForLocationRad(double lat) {
        return GISUtils.lat2pixel(mMapZoom, lat);
    }

    private void displayCoords(JsonArray coords, Paint color) {
        Polyline polyline = new Polyline();
        Double[] points = new Double[coords.size() * 2];
        int j = 0;
        for (int i = 0; i < coords.size(); i++) {
            JsonArray coord = (JsonArray) coords.get(i);
            double lon = coord.getDouble(0);
            double lat = coord.getDouble(1);

            Double posX = getPixelXPosForLocationDeg(lon);
            Double posY = getPixelYPosForLocationDeg(lat);

            points[j] = posX;
            points[j + 1] = posY;
            j += 2;
        }
        polyline.getPoints().addAll(points);
        polyline.setTranslateX(-mMapZeroX);
        polyline.setTranslateY(-mMapZeroY);
        polyline.setStroke(color);
        mainPane.getChildren().add(polyline);
    }

    private void calcMapCenterPos() {
        mCenterPosX = getPixelXPosForLocationDeg(mCenterLon);
        mCenterPosY = getPixelYPosForLocationDeg(mCenterLat);
    }

    private void calcMapZeroPos() {
        mMapZeroX = mCenterPosX - mainPane.getWidth() / 2;
        mMapZeroY = mCenterPosY - mainPane.getHeight() / 2;
    }

    private void calcCenterCoord() {
        mCenterLon = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mCenterPosX));
        mCenterLat = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mCenterPosY));
    }

    private void moveMap(int stepX, int stepY) {
        double posX = mCenterPosX - mMapZeroX + stepX;
        double posY = mCenterPosY - mMapZeroY + stepY;

        mCenterPosX = mMapZeroX + posX;
        mCenterPosY = mMapZeroY + posY;

        calcCenterCoord();
        loadWays();
    }

    private List<Double> getVisibleBBoxDegWithMargin() {
        List<Double> bbox = getVisibleBBoxDeg();
        double margin = getPrefetchBoxMargin();
        bbox.set(3, bbox.get(3) + margin);
        bbox.set(2, bbox.get(2) + margin);
        bbox.set(1, bbox.get(1) - margin);
        bbox.set(0, bbox.get(0) - margin);
        return bbox;
    }

    private List<Double> getVisibleBBoxDeg() {
        double lat1 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mMapZeroY));
        double lon1 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mMapZeroX));

        double lat2 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mMapZeroY));
        double lon2 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mMapZeroX + mainPane.getWidth()));

        double lat3 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mMapZeroY + mainPane.getHeight()));
        double lon3 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mMapZeroX));

        double lat4 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mMapZeroY + mainPane.getHeight()));
        double lon4 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mMapZeroX + mainPane.getWidth()));

        List<Double> lonList = new ArrayList<>();
        Collections.addAll(lonList, lon1, lon2, lon3, lon4);

        List<Double> latList = new ArrayList<>();
        Collections.addAll(latList, lat1, lat2, lat3, lat4);

        double bboxLon1 = Collections.min(lonList);
        double bboxLat1 = Collections.min(latList);
        double bboxLon2 = Collections.max(lonList);
        double bboxLat2 = Collections.max(latList);

        List<Double> l = new ArrayList<>();
        Collections.addAll(l, bboxLon1, bboxLat1, bboxLon2, bboxLat2);
        return l;
    }
}
