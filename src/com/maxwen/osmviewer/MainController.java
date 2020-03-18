package com.maxwen.osmviewer;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.List;

public class MainController implements Initializable {
    public static final int ROTATE_X_VALUE = 50;
    public static final int PREFETCH_MARGIN_PIXEL = 200;
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
    @FXML
    Label zoomLabel;
    @FXML
    Label posLabel;
    @FXML
    BorderPane borderPane;
    @FXML
    HBox buttons;

    private static final int MIN_ZOOM = 10;
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
    private long mLastMoveHandled;
    private Point2D mMapPos;
    private Popup mContextPopup;
    private Stage mPrimaryStage;
    private boolean mShow3D;
    private Scene mScene;
    private boolean mHeightUpdated;
    private BoundingBox mFetchBBox;
    private BoundingBox mVisibleBBox;
    private Rotate mRotate;
    private Map<Integer, List<Shape>> mPolylines;

    EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mContextPopup != null) {
                mContextPopup.hide();
                mContextPopup = null;
            }

            if (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED) {
                // getX and getY will be transformed pos
                Point2D mapPos = new Point2D(mouseEvent.getX(), mouseEvent.getY());
                Point2D coordPos = getCoordOfPos(mapPos);
                posLabel.setText(String.format("%.6f:%.6f", coordPos.getX(), coordPos.getY()));
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_RELEASED) {
                mMouseMoving = false;
                mMovePoint = null;
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                if (System.currentTimeMillis() - mLastMoveHandled < 100) {
                    return;
                }
                if (!mMouseMoving) {
                    mMovePoint = new Point2D(mouseEvent.getX(), mouseEvent.getY());
                    mMouseMoving = true;
                    mLastMoveHandled = 0;
                } else {
                    if (mMovePoint != null) {
                        int diffX = (int) (mMovePoint.getX() - mouseEvent.getX());
                        int diffY = (int) (mMovePoint.getY() - mouseEvent.getY());

                        moveMap(diffX, diffY);
                        mLastMoveHandled = System.currentTimeMillis();
                        mMovePoint = new Point2D(mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("initialize");
        mPolylines = new LinkedHashMap<>();
        mPolylines.put(-1, new ArrayList<>());
        mPolylines.put(0, new ArrayList<>());
        mPolylines.put(1, new ArrayList<>());
        mPolylines.put(2, new ArrayList<>());
        mPolylines.put(3, new ArrayList<>());

        calcMapCenterPos();

        quitButton.setOnAction(e -> {
            Platform.exit();
        });
        zoomInButton.setOnAction(e -> {
            int zoom = mMapZoom + 1;
            zoom = Math.min(MAX_ZOOM, zoom);
            if (zoom != mMapZoom) {
                mMapZoom = zoom;
                zoomLabel.setText(String.valueOf(mMapZoom));
                calcMapCenterPos();
                loadWays();
            }
        });
        zoomOutButton.setOnAction(e -> {
            int zoom = mMapZoom - 1;
            zoom = Math.max(MIN_ZOOM, zoom);
            if (zoom != mMapZoom) {
                mMapZoom = zoom;
                zoomLabel.setText(String.valueOf(mMapZoom));
                calcMapCenterPos();
                if (mMapZoom < 16) {
                    mShow3D = false;
                    mainPane.getTransforms().remove(mRotate);
                }
                loadWays();
            }
        });
        stepLeftButton.setOnAction(e -> {
            Point2D p = new Point2D(-100, 0);
            moveMap(p.getX(), p.getY());
        });
        stepRightButton.setOnAction(e -> {
            Point2D p = new Point2D(100, 0);
            moveMap(p.getX(), p.getY());
        });
        stepUpButton.setOnAction(e -> {
            Point2D p = new Point2D(0, -100);
            moveMap(p.getX(), p.getY());
        });
        stepDownButton.setOnAction(e -> {
            Point2D p = new Point2D(0, 100);
            moveMap(p.getX(), p.getY());
        });
        zoomLabel.setText(String.valueOf(mMapZoom));
        mainPane.setOnMousePressed(mouseHandler);
        mainPane.setOnMouseReleased(mouseHandler);
        mainPane.setOnMouseDragged(mouseHandler);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem = new MenuItem(" Mouse pos ");
        menuItem.setOnAction(ev -> {
            Point2D coordPos = getCoordOfPos(mMapPos);
            List<Double> bbox = createBBoxAroundPoint(coordPos.getX(), coordPos.getY(), 0.0);
            JsonArray adminAreas = DatabaseController.getInstance().getAdminAreasOnPointWithGeom(coordPos.getX(), coordPos.getY(),
                    bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3), OSMUtils.ADMIN_LEVEL_SET, this);
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < adminAreas.size(); i++) {
                JsonObject area = (JsonObject) adminAreas.get(i);
                JsonObject tags = (JsonObject) area.get("tags");
                if (tags != null && tags.containsKey("name")) {
                    s.append(tags.get("name") + "\n");
                }
            }
            mContextPopup = createPopup(s.toString());
            mContextPopup.show(mPrimaryStage);
        });
        contextMenu.getItems().add(menuItem);
        menuItem = new MenuItem(" Toggle 3D ");
        menuItem.setOnAction(ev -> {
            if (mMapZoom >= 16) {
                if (mShow3D) {
                    mShow3D = false;
                    mainPane.getTransforms().remove(mRotate);
                    loadWays();
                } else {
                    mShow3D = true;
                    mainPane.getTransforms().add(mRotate);
                    loadWays();
                }
            }
        });
        contextMenu.getItems().add(menuItem);
        mainPane.setOnContextMenuRequested((ev) -> {
            mMapPos = new Point2D(ev.getX(), ev.getY());
            contextMenu.show(mainPane, ev.getScreenX(), ev.getScreenY());
        });

        mRotate = new Rotate(-ROTATE_X_VALUE, Rotate.X_AXIS);

        borderPane.setTop(new TextField("Top"));
        borderPane.setTop(buttons);
    }

    protected void setStage(Stage primaryStage) {
        mPrimaryStage = primaryStage;
    }

    protected void setScene(Scene scene) {
        mScene = scene;
    }

    private Popup createPopup(String text) {
        Label label = new Label(text);
        Popup popup = new Popup();
        label.setStyle(" -fx-background-color: white;");
        popup.getContent().add(label);
        label.setMinWidth(250);
        label.setMinHeight(200);
        return popup;
    }

    private List<Integer> getStreetTypeListForZoom() {
        if (mMapZoom <= 12) {
            List<Integer> typeFilterList = new ArrayList<>();
            Collections.addAll(typeFilterList, OSMUtils.STREET_TYPE_MOTORWAY,
                    OSMUtils.STREET_TYPE_MOTORWAY_LINK,
                    OSMUtils.STREET_TYPE_TRUNK,
                    OSMUtils.STREET_TYPE_TRUNK_LINK,
                    OSMUtils.STREET_TYPE_PRIMARY,
                    OSMUtils.STREET_TYPE_PRIMARY_LINK);
            return typeFilterList;
        } else if (mMapZoom <= 14) {
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
                    OSMUtils.STREET_TYPE_TERTIARY_LINK);
            return typeFilterList;
        } else if (mMapZoom <= 15) {
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
        if (mMapZoom <= 12) {
            List<Integer> typeFilterList = new ArrayList<>();
            Collections.addAll(typeFilterList,
                    OSMUtils.AREA_TYPE_RAILWAY);
            return typeFilterList;
        } else if (mMapZoom < 14) {
            List<Integer> typeFilterList = new ArrayList<>();
            Collections.addAll(typeFilterList,
                    OSMUtils.AREA_TYPE_AEROWAY,
                    OSMUtils.AREA_TYPE_RAILWAY,
                    OSMUtils.AREA_TYPE_WATER);
            return typeFilterList;
        } else if (mMapZoom <= 16) {
            List<Integer> typeFilterList = new ArrayList<>();
            Collections.addAll(typeFilterList, OSMUtils.AREA_TYPE_LANDUSE,
                    OSMUtils.AREA_TYPE_NATURAL,
                    OSMUtils.AREA_TYPE_HIGHWAY_AREA,
                    OSMUtils.AREA_TYPE_AEROWAY,
                    OSMUtils.AREA_TYPE_RAILWAY,
                    OSMUtils.AREA_TYPE_TOURISM,
                    OSMUtils.AREA_TYPE_LEISURE,
                    OSMUtils.AREA_TYPE_WATER);
            return typeFilterList;
        } else {
            return null;
        }
    }

    public void loadWays() {
        calcMapZeroPos();
        long t = System.currentTimeMillis();
        //System.out.println("load " + mMapZoom);
        for (List<Shape> polyList : mPolylines.values()) {
            polyList.clear();
        }

        System.out.println(mCenterPosX + " : " + mCenterPosY);
        System.out.println(mMapZeroX + " : " + mMapZeroY);

        mainPane.getChildren().clear();
        mVisibleBBox = getVisibleBBox();
        System.out.println(mVisibleBBox);

        mFetchBBox = getVisibleBBoxWithMargin(mVisibleBBox);
        System.out.println(mFetchBBox);

        List<Double> bbox = getBBoxInDeg(mFetchBBox);
        System.out.println(bbox);

        if (mMapZoom > 12) {
            JsonArray areas = DatabaseController.getInstance().getAreasInBboxWithGeom(bbox.get(0), bbox.get(1),
                    bbox.get(2), bbox.get(3), getAreaTypeListForZoom(), mMapZoom <= 14, mMapZoom <= 14 ? 10.0 : 0.0, mPolylines, this);
        }

        JsonArray ways = DatabaseController.getInstance().getWaysInBboxWithGeom(bbox.get(0), bbox.get(1),
                bbox.get(2), bbox.get(3), getStreetTypeListForZoom(), mPolylines, this);

        if (mMapZoom > 12) {
            // railway rails are above ways if not bridge anyway
            JsonArray lineAreas = DatabaseController.getInstance().getLineAreasInBboxWithGeom(bbox.get(0), bbox.get(1),
                    bbox.get(2), bbox.get(3), getAreaTypeListForZoom(), mMapZoom <= 14, mMapZoom <= 14 ? 10.0 : 0.0, mPolylines, this);
        }

        JsonArray adminLines = DatabaseController.getInstance().getAdminLineInBboxWithGeom(bbox.get(0), bbox.get(1),
                bbox.get(2), bbox.get(3), OSMUtils.ADMIN_LEVEL_SET, mMapZoom <= 14, mMapZoom <= 14 ? 10.0 : 0.0, mPolylines, this);

        for (List<Shape> polyList : mPolylines.values()) {
            mainPane.getChildren().addAll(polyList);
        }

        //System.out.println("load " + mMapZoom + " " + (System.currentTimeMillis() - t));
    }

    private void drawShapes() {
        calcMapZeroPos();
        mainPane.getChildren().clear();

        for (List<Shape> polyList : mPolylines.values()) {
            for (Shape s : polyList) {
                s.setTranslateX(-mMapZeroX);
                s.setTranslateY(-mMapZeroY);
            }
        }
        for (List<Shape> polyList : mPolylines.values()) {
            mainPane.getChildren().addAll(polyList);
        }
    }

    private double getPrefetchBoxMargin() {
        return PREFETCH_MARGIN_PIXEL;
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

    public Polyline displayCoordsPolyline(JsonArray coords) {
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
        return polyline;
    }

    public Polyline clonePolyline(Polyline p) {
        Polyline polyline = new Polyline();
        polyline.getPoints().addAll(p.getPoints());
        polyline.setTranslateX(-mMapZeroX);
        polyline.setTranslateY(-mMapZeroY);
        return polyline;
    }

    public Polygon displayCoordsPolygon(JsonArray coords) {
        Polygon polygon = new Polygon();
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
        polygon.getPoints().addAll(points);
        polygon.setTranslateX(-mMapZeroX);
        polygon.setTranslateY(-mMapZeroY);
        return polygon;
    }


    private void calcMapCenterPos() {
        mCenterPosX = getPixelXPosForLocationDeg(mCenterLon);
        mCenterPosY = getPixelYPosForLocationDeg(mCenterLat);
    }

    private void calcMapZeroPos() {
        if (!mHeightUpdated) {
            mRotate.setPivotY(mainPane.getHeight());
            mHeightUpdated = true;
        }

        mMapZeroX = mCenterPosX - mainPane.getWidth() / 2;
        mMapZeroY = mCenterPosY - mainPane.getHeight() / 2;
    }

    private void calcCenterCoord() {
        mCenterLon = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mCenterPosX));
        mCenterLat = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mCenterPosY));
    }

    private Point2D getCoordOfPos(Point2D mousePos) {
        double lat = GISUtils.pixel2lat(mMapZoom, mMapZeroY + mousePos.getY());
        double lon = GISUtils.pixel2lon(mMapZoom, mMapZeroX + mousePos.getX());
        return new Point2D(GISUtils.rad2deg(lon), GISUtils.rad2deg(lat));
    }

    private void moveMap(double stepX, double stepY) {
        double posX = mCenterPosX - mMapZeroX + stepX;
        double posY = mCenterPosY - mMapZeroY + stepY;

        mCenterPosX = mMapZeroX + posX;
        mCenterPosY = mMapZeroY + posY;

        calcCenterCoord();

        BoundingBox bbox = getVisibleBBox();
        if (!mFetchBBox.contains(bbox)) {
            loadWays();
        } else {
            drawShapes();
        }
    }

    private List<Double> createBBoxAroundPoint(double lon, double lat, double margin) {
        List<Double> bbox = new ArrayList<>();
        double latRangeMax = lat + margin;
        double lonRangeMax = lon + margin * 1.4;
        double latRangeMin = lat - margin;
        double lonRangeMin = lon - margin * 1.4;
        Collections.addAll(bbox, lonRangeMin, latRangeMin, lonRangeMax, latRangeMax);
        return bbox;
    }

    private BoundingBox getVisibleBBox() {
        double deltaY = 0;
        double deltaX = 0;
        if (mShow3D) {
            deltaY = mainPane.getHeight() / Math.cos(ROTATE_X_VALUE);
            deltaX = deltaY;
        }

        double y1 = mMapZeroY - deltaY;
        double x1 = mMapZeroX - deltaX;

        double y2 = mMapZeroY + deltaY;
        double x2 = mMapZeroX + mainPane.getWidth() + 2 * deltaX;

        double y3 = mMapZeroY + mainPane.getHeight();
        double x3 = mMapZeroX;

        double y4 = mMapZeroY + mainPane.getHeight();
        double x4 = mMapZeroX + mainPane.getWidth();

        List<Double> xList = new ArrayList<>();
        Collections.addAll(xList, x1, x2, x3, x4);

        List<Double> yList = new ArrayList<>();
        Collections.addAll(yList, y1, y2, y3, y4);

        double bboxX1 = Collections.min(xList);
        double bboxY1 = Collections.min(yList);
        double bboxX2 = Collections.max(xList);
        double bboxY2 = Collections.max(yList);

        return new BoundingBox(bboxX1, bboxY1, bboxX2 - bboxX1, bboxY2 - bboxY1);
    }

    private BoundingBox getVisibleBBoxWithMargin(BoundingBox bbox) {
        double margin = getPrefetchBoxMargin();
        return new BoundingBox(bbox.getMinX() - margin, bbox.getMinY() - margin,
                bbox.getWidth() + 2 * margin, bbox.getHeight() + 2 * margin);
    }

    private List<Double> getBBoxInDeg(BoundingBox bbox) {
        double lat1 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, bbox.getMinY()));
        double lon1 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, bbox.getMinX()));

        double lat2 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, bbox.getMaxY()));
        double lon2 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, bbox.getMaxX()));

        List<Double> l = new ArrayList<>();
        Collections.addAll(l, lon1, lat1, lon2, lat2);
        return l;
    }

    public int getZoom() {
        return mMapZoom;
    }
}
