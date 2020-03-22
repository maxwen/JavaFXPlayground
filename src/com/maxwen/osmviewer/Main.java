package com.maxwen.osmviewer;

import com.fazecast.jSerialComm.SerialPort;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.maxwen.osmviewer.nmea.GpsSatellite;
import com.maxwen.osmviewer.nmea.NMEAHandler;
import com.maxwen.osmviewer.nmea.NMEAParser;
import com.maxwen.osmviewer.nmea.basic.BasicNMEAHandler;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class Main extends Application {
    public static final String DEV_TTY_ACM_0 = "dev/ttyACM0";
    private MainController mController;
    private Scene mScene;
    private boolean mStopGPSThread;

    @FXML
    Button closeMenu;
    private Thread mGPSTread;

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("start");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();
        //Pane pane = new Pane();

        mController = loader.getController();
        mController.setStage(primaryStage);
        //mController.setPane(pane);
        //mController.init();
        primaryStage.setTitle("Hello World");
        mScene = new Scene(root, 1280, 800, false, SceneAntialiasing.BALANCED);
        PerspectiveCamera camera = new PerspectiveCamera();
        mScene.setCamera(camera);
        mController.setScene(mScene);
        primaryStage.setScene(mScene);
        primaryStage.show();
        mController.loadMapData();

        /*Pane pane = new Pane();
        Rectangle r = new Rectangle(100, 100, 200, 50);
        pane.getChildren().add(r);
        Rotate rotate = new Rotate();
        System.err.println(r.getX() + ":" + r.getY());

        rotate.setAngle(45);
        rotate.setPivotY(r.getLayoutBounds().getCenterY());
        rotate.setPivotX(r.getLayoutBounds().getCenterX());
        r.getTransforms().add(rotate);


        Scene scene = new Scene(pane, 400, 400);
        camera = new PerspectiveCamera();
        scene.setCamera(camera);
        Stage sub = new Stage();
        sub.setTitle("Test");
        sub.setScene(scene);
        sub.show();

        Point2D p = new Point2D(r.getX(), r.getY());
        System.err.println(p);
        p = r.getLocalToSceneTransform().transform(p);
        System.err.println(p);

        p = new Point2D(r.getX() + r.getWidth(), r.getY() + r.getHeight());
        System.err.println(p);
        p = r.getLocalToSceneTransform().transform(p);
        System.err.println(p);

        p = new Point2D(0, 0);
        System.err.println(p);
        p = pane.getLocalToSceneTransform().transform(p);
        System.err.println(p);

        System.err.println(r.localToParent(0, 0));*/

        NMEAHandler handler = new NMEAHandler() {
            @Override
            public void onStart() {
            }

            @Override
            public void onLocation1(double lon, double lat, double altitude) {
                System.err.println("onLocation1 " + lon + " " + lat + " " + altitude);
            }

            @Override
            public void onLocation2(double speed, double bearing) {
                System.err.println("onLocation2 " + speed + " " + bearing);
            }

            @Override
            public void onSatellites(List<GpsSatellite> satellites) {
                System.err.println("onSatellites " + satellites);
            }

            @Override
            public void onUnrecognized(String sentence) {
            }

            @Override
            public void onBadChecksum(int expected, int actual) {
            }

            @Override
            public void onException(Exception e) {
            }

            @Override
            public void onFinish() {
            }
        };
        NMEAParser parser = new NMEAParser(handler);
        mStopGPSThread = false;
        final SerialPort port = SerialPort.getCommPort(DEV_TTY_ACM_0);
        if (port != null && port.openPort()) {
            Task<Void> t = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(port.getInputStream()));
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            //System.out.println(line);
                            parser.parse(line);
                            if (mStopGPSThread) {
                                break;
                            }
                        }
                        reader.close();
                    } catch (Exception e) {
                    }
                    port.closePort();
                    mGPSTread = null;
                    return null;
                }
            };
            mGPSTread = new Thread(t);
            mGPSTread.start();
        } else {
            System.err.println("open port " + DEV_TTY_ACM_0 + " failed");
        }

    }

    @Override
    public void init() throws Exception {
        super.init();
        System.out.println("init");
        DatabaseController.getInstance().connextAll();
    }

    @Override
    public void stop() {
        System.out.println("stop");
        DatabaseController.getInstance().disconnectAll();
        if (mGPSTread != null) {
            mStopGPSThread = true;
            mGPSTread.interrupt();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
