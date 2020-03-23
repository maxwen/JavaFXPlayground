package com.maxwen.osmviewer;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.maxwen.osmviewer.nmea.NMEAHandler;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.List;

public class MainController implements Initializable, NMEAHandler {
    public static final int ROTATE_X_VALUE = 50;
    public static final int PREFETCH_MARGIN_PIXEL = 800;
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
    @FXML
    Button rotateLeftButton;
    @FXML
    Button rotateRightButton;
    @FXML
    Button gpsPosButton;
    @FXML
    CheckBox trackModeButton;

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
    private Rotate mZRotate;
    private OSMShape mSelectdShape;
    private long mSelectdOSMId = -1;
    private Map<Long, JsonObject> mOSMObjects;
    private OSMShape mPressedShape;
    private int mRotateAnggle;
    private Point2D mGPSPos = new Point2D(0, 0);
    private Circle mGPSDot;
    private JsonObject mGPSData;
    private boolean mTrackMode;
    private GPSThread mGPSThread;

    public static final int TUNNEL_LAYER_LEVEL = -1;
    public static final int AREA_LAYER_LEVEL = 0;
    public static final int ADMIN_AREA_LAYER_LEVEL = 1;
    public static final int BUILDING_AREA_LAYER_LEVEL = 2;
    public static final int STREET_LAYER_LEVEL = 3;
    public static final int RAILWAY_LAYER_LEVEL = 4;
    public static final int BRIDGE_LAYER_LEVEL = 5;


    EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mContextPopup != null) {
                mContextPopup.hide();
                mContextPopup = null;
            }

            if (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED) {
                mPressedShape = null;
                if (mouseEvent.isPrimaryButtonDown()) {
                    if (mContextMenu.isShowing()) {
                        mContextMenu.hide();
                        return;
                    }

                    // getX and getY will be transformed pos
                    Point2D mapPos = new Point2D(mouseEvent.getX(), mouseEvent.getY());
                    Point2D scenePos = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());

                    Point2D coordPos = getCoordOfPos(mapPos);
                    posLabel.setText(String.format("%.6f:%.6f", coordPos.getX(), coordPos.getY()));

                    Point2D mapPosNormalized = new Point2D(mapPos.getX() + mMapZeroX, mapPos.getY() + mMapZeroY);
                    mPressedShape = findShapeAtPoint(mapPosNormalized);
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_RELEASED) {
                mMouseMoving = false;
                mMovePoint = null;

                if (mPressedShape != null) {
                    mSelectdShape = mPressedShape;
                    mSelectdShape.setSelected();
                    mSelectdOSMId = mSelectdShape.getOSMId();

                    Task<Void> t = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            JsonObject osmObject = mOSMObjects.get(mSelectdOSMId);
                            if (osmObject != null) {
                                System.err.println(osmObject);
                            }
                            //Platform.runLater(() -> drawShapes());

                            return null;
                        }
                    };
                    t.stateProperty().addListener(new ChangeListener<Worker.State>() {
                        @Override
                        public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State oldState, Worker.State newState) {
                            System.out.println(oldState + " -> " + newState);
                            if (newState == Worker.State.SUCCEEDED) {
                                drawShapes();
                            }
                        }
                    });
                    t.setOnFailed(e -> {
                        Throwable exception = e.getSource().getException();
                        if (exception != null) {
                        }
                    });
                    new Thread(t).start();
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                if (mouseEvent.isPrimaryButtonDown()) {
                    mContextMenu.hide();
                    mPressedShape = null;
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
        }
    };
    private ContextMenu mContextMenu;
    private Point2D mMapGPSPos;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("initialize");
        init();
    }

    public void init() {
        System.out.println("initialize");
        mPolylines = new LinkedHashMap<>();
        mPolylines.put(TUNNEL_LAYER_LEVEL, new ArrayList<>());
        mPolylines.put(AREA_LAYER_LEVEL, new ArrayList<>());
        mPolylines.put(ADMIN_AREA_LAYER_LEVEL, new ArrayList<>());
        mPolylines.put(BUILDING_AREA_LAYER_LEVEL, new ArrayList<>());
        mPolylines.put(STREET_LAYER_LEVEL, new ArrayList<>());
        mPolylines.put(RAILWAY_LAYER_LEVEL, new ArrayList<>());
        mPolylines.put(BRIDGE_LAYER_LEVEL, new ArrayList<>());
        mOSMObjects = new HashMap<>();
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
                loadMapData();
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
                loadMapData();
            }
        });
        rotateLeftButton.setOnAction(event -> {
            int newAngle = mRotateAnggle - 15;
            if (newAngle < 0) {
                newAngle += 360;
            }
            mRotateAnggle = newAngle;
            rotateMap();
        });
        rotateRightButton.setOnAction(event -> {
            int newAngle = mRotateAnggle + 15;
            if (newAngle >= 360) {
                newAngle -= 360;
            }
            mRotateAnggle = newAngle;
            rotateMap();
        });
        gpsPosButton.setOnAction(event -> {
            moveToGPSPos();
        });
        trackModeButton.setOnAction(event -> {
            mTrackMode = trackModeButton.isSelected();
            if (mTrackMode) {
                mGPSData = null;
                mGPSThread = new GPSThread();
                if (!mGPSThread.startThread(MainController.this)) {
                    System.err.println("open port " + GPSThread.DEV_TTY_ACM_0 + " failed");
                    mMapGPSPos = new Point2D(0, 0);
                }
            } else {
                if (mGPSThread != null) {
                    mGPSThread.stopThread();
                    mMapGPSPos = new Point2D(0, 0);
                    mGPSData = null;
                    drawShapes();
                }
            }
        });
        zoomLabel.setText(String.valueOf(mMapZoom));
        mainPane.setOnMousePressed(mouseHandler);
        mainPane.setOnMouseReleased(mouseHandler);
        mainPane.setOnMouseDragged(mouseHandler);

        mContextMenu = new ContextMenu();
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
        mContextMenu.getItems().add(menuItem);
        menuItem = new MenuItem(" Toggle 3D ");
        menuItem.setOnAction(ev -> {
            if (mMapZoom >= 16) {
                if (mShow3D) {
                    mShow3D = false;
                    mainPane.getTransforms().clear();
                    mainPane.getTransforms().add(mZRotate);
                    loadMapData();

                } else {
                    mShow3D = true;
                    mainPane.getTransforms().clear();
                    mainPane.getTransforms().add(mRotate);
                    mainPane.getTransforms().add(mZRotate);
                    loadMapData();
                }
            }
        });
        mContextMenu.getItems().add(menuItem);
        mainPane.setOnContextMenuRequested((ev) -> {
            mMapPos = new Point2D(ev.getX(), ev.getY());
            mContextMenu.show(mainPane, ev.getScreenX(), ev.getScreenY());
        });

        mGPSDot = new Circle();
        mGPSDot.setFill(Color.TRANSPARENT);
        mGPSDot.setRadius(10);
        mGPSDot.setStroke(Color.BLACK);
        mGPSDot.setStrokeWidth(2);

        mRotate = new Rotate(-ROTATE_X_VALUE, Rotate.X_AXIS);
        mZRotate = new Rotate();

        borderPane.setTop(new TextField("Top"));
        borderPane.setTop(buttons);
    }

    public void stop() {
        if (mGPSThread != null) {
            mGPSThread.stopThread();
            try {
                mGPSThread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    private void rotateMap() {
        mZRotate.setAngle(mRotateAnggle);
    }

    protected void setStage(Stage primaryStage) {
        mPrimaryStage = primaryStage;
    }

    protected void setScene(Scene scene) {
        mScene = scene;

        mScene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    case ESCAPE:
                        mSelectdShape = null;
                        mSelectdOSMId = -1;
                        drawShapes();
                        break;
                }
            }
        });
    }

    public void addToOSMCache(long osmId, JsonObject osmObject) {
        mOSMObjects.put(osmId, osmObject);
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

    public void loadMapData() {
        calcMapZeroPos();
        long t = System.currentTimeMillis();
        System.out.println("loadMapData " + mMapZoom);
        mOSMObjects.clear();
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

        if (mSelectdOSMId != -1) {
            mSelectdShape = findShapeOfOSMId(mSelectdOSMId);
            if (mSelectdShape == null) {
                mSelectdOSMId = -1;
            } else {
                mSelectdShape.setSelected();
            }
        }

        for (List<Shape> polyList : mPolylines.values()) {
            mainPane.getChildren().addAll(polyList);
        }
        if (mSelectdShape != null) {
            mainPane.getChildren().add(mSelectdShape.getShape());
        }

        if (isPositionVisible(mMapGPSPos)) {
            addGPSDot();
        }
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
        if (mSelectdShape != null) {
            mSelectdShape.getShape().setTranslateX(-mMapZeroX);
            mSelectdShape.getShape().setTranslateY(-mMapZeroY);
        }
        for (List<Shape> polyList : mPolylines.values()) {
            mainPane.getChildren().addAll(polyList);
        }
        if (mSelectdShape != null) {
            mainPane.getChildren().add(mSelectdShape.getShape());
        }
        if (isPositionVisible(mMapGPSPos)) {
            addGPSDot();
        }
    }

    private double getPrefetchBoxMargin() {
        if (mShow3D) {
            return PREFETCH_MARGIN_PIXEL * 2;
        }
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

    public Polyline displayCoordsPolyline(long osmId, JsonArray coords) {
        OSMPolyline polyline = new OSMPolyline(osmId);
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

    public Polyline clonePolyline(long osmId, Polyline p) {
        OSMPolyline polyline = new OSMPolyline(osmId);
        polyline.getPoints().addAll(p.getPoints());
        polyline.setTranslateX(-mMapZeroX);
        polyline.setTranslateY(-mMapZeroY);
        return polyline;
    }

    public Polygon displayCoordsPolygon(long osmId, JsonArray coords) {
        OSMPolygon polygon = new OSMPolygon(osmId);
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
            mHeightUpdated = true;
            mRotate.setPivotY(mainPane.getLayoutBounds().getCenterY());
            mZRotate.setPivotY(mainPane.getLayoutBounds().getCenterY());
            mZRotate.setPivotX(mainPane.getLayoutBounds().getCenterX());
            mainPane.getTransforms().add(mZRotate);
        }
        mMapZeroX = mCenterPosX - mScene.getWidth() / 2;
        mMapZeroY = mCenterPosY - mScene.getHeight() / 2;
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
            loadMapData();
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
        return new BoundingBox(mMapZeroX, mMapZeroY, mScene.getWidth(), mScene.getHeight());
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

    // returns a copy
    private OSMShape findShapeAtPoint(Point2D pos) {
        // from top layer down
        List<Integer> keyList = new ArrayList<>();
        keyList.addAll(mPolylines.keySet());
        Collections.reverse(keyList);

        for (int layer : keyList) {
            List<Shape> polyList = mPolylines.get(layer);
            // again reverse by layer order
            for (int i = polyList.size() - 1; i >= 0; i--) {
                Shape s = polyList.get(i);
                if (s instanceof OSMPolygon) {
                    if (s.contains(pos)) {
                        OSMPolygon polygon = new OSMPolygon(((OSMPolygon) s).getOSMId(), (OSMPolygon) s);
                        polygon.getPoints().addAll(((OSMPolygon) s).getPoints());
                        polygon.setTranslateX(-mMapZeroX);
                        polygon.setTranslateY(-mMapZeroY);
                        return polygon;
                    }
                }
                if (s instanceof OSMPolyline) {
                    if (s.contains(pos)) {
                        OSMPolyline polyline = new OSMPolyline(((OSMPolyline) s).getOSMId(), (OSMPolyline) s);
                        polyline.getPoints().addAll(((OSMPolyline) s).getPoints());
                        polyline.setTranslateX(-mMapZeroX);
                        polyline.setTranslateY(-mMapZeroY);
                        return polyline;
                    }
                }
            }
        }
        return null;
    }

    private OSMShape findShapeOfOSMId(long osmId) {
        for (List<Shape> polyList : mPolylines.values()) {
            for (Shape s : polyList) {
                if (s instanceof OSMPolygon) {
                    if (((OSMPolygon) s).getOSMId() == osmId) {
                        OSMPolygon polygon = new OSMPolygon(((OSMPolygon) s).getOSMId(), (OSMPolygon) s);
                        polygon.getPoints().addAll(((OSMPolygon) s).getPoints());
                        polygon.setTranslateX(-mMapZeroX);
                        polygon.setTranslateY(-mMapZeroY);
                        return polygon;
                    }
                }
            }
        }
        return null;
    }

    private void updateGPSPos(JsonObject gpsData) {
        double lat = (double) gpsData.get("lat");
        double lon = (double) gpsData.get("lon");
        boolean hasMoved = false;

        if (mGPSData != null) {
            if (lat != (double) mGPSData.get("lat") || lon != (double) mGPSData.get("lon")) {
                hasMoved = true;
                // TODO also use speed
            }
        } else {
            hasMoved = true;
        }
        mGPSData = gpsData;

        if (hasMoved && mTrackMode) {
            mGPSPos = new Point2D(lon, lat);
            mMapGPSPos = new Point2D(getPixelXPosForLocationDeg(mGPSPos.getX()),
                    getPixelYPosForLocationDeg(mGPSPos.getY()));
            moveToGPSPos();
        }
    }

    private void moveToGPSPos() {
        if (mGPSData == null) {
            return;
        }
        System.out.println("moveToGPSPos");
        mCenterLat = mGPSPos.getY();
        mCenterLon = mGPSPos.getX();
        double bearing = (double) mGPSData.get("bearing");
        if (bearing != -1) {
            mZRotate.setAngle(360 - bearing);
        }

        posLabel.setText(String.format("%.6f:%.6f", mCenterLon, mCenterLat));

        calcMapCenterPos();
        calcMapZeroPos();

        BoundingBox bbox = getVisibleBBox();
        if (!mFetchBBox.contains(bbox)) {
            loadMapData();
        } else {
            drawShapes();
        }
    }

    private boolean isPositionVisible(Point2D mapPos) {
        return mFetchBBox.contains(mapPos);
    }

    private void addGPSDot() {
        mGPSDot.setCenterX(mMapGPSPos.getX() - mMapZeroX);
        mGPSDot.setCenterY(mMapGPSPos.getY() - mMapZeroY);
        mainPane.getChildren().add(mGPSDot);
    }

    @Override
    public void onLocation(JsonObject gpsData) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                updateGPSPos(gpsData);
            }
        });
    }
}
