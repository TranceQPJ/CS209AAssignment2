package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private FXMLLoader fxmlLoader;
    private Controller controller;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.setTitle("Chatting Client");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("client stopped!");
        controller = fxmlLoader.getController();
        if (controller != null)
            controller.disconnect();
        super.stop();
        // release the socket connection in the controller here
    }
}
