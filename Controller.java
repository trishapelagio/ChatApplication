package Client;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;

import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.util.Scanner;

public class Controller implements ActionListener{
    @FXML private TextField nameInputField;
    @FXML private TextField serverField;
    @FXML private TextField portField;
    @FXML private Label labelAlert;

    private DataInputStream reader;
    private DataOutputStream writer;
    Socket endpoint;
    private String name;
    private String server;
    private int port;

    public Controller(){
    }

    public void actionPerformed(ActionEvent ae){

    }

    public void login(MouseEvent mouseEvent){
        if (!nameInputField.getText().equals("") && !serverField.getText().equals("") && !portField.getText().equals("")){
            name = nameInputField.getText();
            server = serverField.getText();
            try{
                port = Integer.parseInt(portField.getText());
                start();
            }
            catch(Exception e){
                labelAlert.setText("Invalid input.");
            }
        }
        else{
            labelAlert.setText("Invalid input.");
        }
    }

    private void loadSecondController() {
        try {
            endpoint = new Socket(server, port);
            reader = new DataInputStream(endpoint.getInputStream());
            writer = new DataOutputStream(endpoint.getOutputStream());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("chat.fxml"));
            Stage stageClose = (Stage) labelAlert.getScene().getWindow();
            stageClose.close();
            Stage stage = new Stage(StageStyle.DECORATED);
            stage.setScene(new Scene(loader.load()));
            stage.setResizable(false);
            stage.getIcons().add(new Image("Client/photos/icon.png"));
            stage.setTitle("DLSUsap Chat");

            stage.setOnCloseRequest(e -> {
                try {
                    System.out.println("User " + name + " is logging out.");
                    writer.writeUTF("User " + name + " is logging out.");
                    writer.writeUTF("-END-SESSION-");
                    endpoint.close();
                } catch(Exception ea) {

                }
                Platform.exit();
                System.exit(0);
            });

            ChatController chatcontroller = loader.getController();
            chatcontroller.passParams(name, server, port, endpoint, reader, writer);
            chatcontroller.start();

            stage.show();

        }
        catch (Exception e) {
            e.printStackTrace();
            labelAlert.setText("Error. Cannot connect to server.");
        }
    }

    public void start(){
        boolean isActive = true;
        String messageSent;
        String messageReceived;
        
        try {
            loadSecondController();
        }
        catch (Exception e){
            labelAlert.setText("A server error has occurred.");
        }
    }

}
