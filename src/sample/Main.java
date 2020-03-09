package sample;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class Main extends Application {
    private MainController mController;

    @FXML
    Button closeMenu;
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("start");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();
        mController = loader.getController();
        primaryStage.setTitle("Hello World");
        Scene s = new Scene(root, 1280, 800);
        primaryStage.setScene(s);
        primaryStage.show();
        mController.loadWays();
    }

    @Override
    public void init() throws Exception {
        super.init();
        System.out.println("init");
        DatabaseController.getInstance().connextAll();
    }

    @Override
    public void stop(){
        System.out.println("stop");
        DatabaseController.getInstance().disconnectAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
