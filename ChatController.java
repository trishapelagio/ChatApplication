package Client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.nio.ByteBuffer;
import java.util.Base64;


public class ChatController implements ActionListener{
    private String name;
    private String server;
    private int port;
    private DataInputStream reader;
    private DataOutputStream writer;

    Socket endpoint;
    boolean session = true;

    @FXML private ListView<String> listMessages;
    @FXML private TextArea userMessage;

    public void actionPerformed(ActionEvent ae){

    }

    public void passParams(String name, String server, int port, Socket endpoint, DataInputStream reader, DataOutputStream writer){
        this.name = name;
        this.server = server;
        this.port = port;
        this.endpoint = endpoint;
        this.reader = reader;
        this.writer = writer;
    }

    public static byte[] decodeImage(String imageDataString) {
        return Base64.getDecoder().decode(imageDataString);
    }

    public void start(){
        try{
            Thread readMessage = new Thread (() -> {
                while (session) {
                    try {
                        String msg = reader.readUTF();
                        if(msg.equals("-A TEXT FILE HAS BEEN SENT-")){
                            try {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        listMessages.getItems().add("You have received a text file.");
                                    }
                                });

                            } catch(Exception e) {

                            }
                            JFileChooser save = new JFileChooser();
                            save.setSelectedFile(new File("untitled.txt"));
                            save.showSaveDialog(null);
                            File savepath = save.getSelectedFile();

                            int fileSize = Integer.parseInt(reader.readUTF());
                            int bytesRead;
                            int current = 0;
                            byte[] mybytearray = new byte[fileSize];
                            FileOutputStream fos = new FileOutputStream(savepath);
                            DataOutputStream dosWriter= new DataOutputStream(fos);
                            bytesRead = reader.read(mybytearray,0,mybytearray.length);
                            current = bytesRead;
                            if(bytesRead != fileSize){
                                do {
                                    bytesRead = reader.read(mybytearray, current, (mybytearray.length-current));
                                    if(bytesRead >= 0)
                                        current += bytesRead;
                                } while(bytesRead > -1);
                            }
                            dosWriter.write(mybytearray, 0 , current);
                            dosWriter.flush();
                            dosWriter.close();
                        }
                        else if (msg.equals("-A PHOTO HAS BEEN SENT-")){
                            try {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        listMessages.getItems().add("You have received a photo.");
                                    }
                                });

                            } catch(Exception e) {

                            }
                            byte[] sizeAr = new byte[4];
                            reader.read(sizeAr);
                            int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
                            String type = reader.readUTF();

                            JFileChooser save = new JFileChooser();
                            save.setSelectedFile(new File("untitled." + type));
                            save.showSaveDialog(null);
                            File savepath = save.getSelectedFile();

                            byte[] imageAr = new byte[size];
                            int numTokens = Integer.parseInt(reader.readUTF());
                            StringBuilder imageString = new StringBuilder();
                            for(int i=0; i<numTokens ; i++) {
                                String chunk = reader.readUTF();
                                imageString.append(chunk);
                            }
                            imageAr = decodeImage(imageString.toString());

                            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageAr));
                            ImageIO.write(image, type.replace(".", ""), savepath);
                        }
                        else{
                            try {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        listMessages.getItems().add(msg);
                                    }
                                });
                            } catch(Exception e) {
                            }
                        }
                    }
                    catch(Exception e) {
                    }
                }
            });
//            sendMessage.start();
            readMessage.start();
        }
        catch(Exception e) {

        }

    }

    public void sendMessage(MouseEvent mouseEvent) {
        if(session) {
            String message = name + ": " + userMessage.getText();
            try{
                listMessages.getItems().add(message);
            } catch(Exception e) {

            }

            userMessage.setText("");

            try {
                writer.writeUTF(message);
            } catch (Exception e) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        listMessages.getItems().add("The message was not sent. The user you are talking to is not connected.");
                    }
                });
            }
        }
    }

    public void logout(MouseEvent mouseEvent) {
        try {
            writer.writeUTF("User " + name + " is logging out.");
            writer.writeUTF("-END-SESSION-");
            endpoint.close();
            Stage stageClose = (Stage) listMessages.getScene().getWindow();
            stageClose.close();
            session = false;
        } catch(Exception e) {
        }
    }

    public static String encodeImage(byte[] imageByteArray) {
        return Base64.getEncoder().encodeToString(imageByteArray);
    }

    public void sendFile(MouseEvent mouseEvent) {
        JFileChooser choose = new JFileChooser();
        choose.showOpenDialog(null);
        try {
            File download = choose.getSelectedFile();
            // Check if file is a photo of text
            int flag = 0;
            String type = "";
            String fileName = download.getName();

            for (int i = 0; i< fileName.length() ; i++) {
                if(fileName.charAt(i) =='.' || flag == 1) {
                    flag = 1;
                    type += fileName.charAt(i);
                }
            }
            if(type.equals(".jpg") || type.equals(".png") || type.equals(".gif") || type.equals(".bmp") || type.equals(".JPG") || type.equals(".PNG") || type.equals(".GIF") || type.equals((".BMP"))) {
                try {
                    BufferedImage image = ImageIO.read(download);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    type = type.replace(".", "").toLowerCase();
                    type = type.toLowerCase();
                    ImageIO.write(image,type, byteArrayOutputStream);
                    byte[] size = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array();
                    writer.writeUTF("-A PHOTO HAS BEEN SENT-");
                    writer.write(size);
                    writer.writeUTF(type);
                    String imageString = encodeImage(byteArrayOutputStream.toByteArray());
                    String[] tokens = imageString.split("(?<=\\G.{" + 65534 + "})");
                    int numTokens = tokens.length;
                    writer.writeUTF(Integer.toString(numTokens));
                    for(int i=0; i<numTokens ; i++){
                        writer.writeUTF(tokens[i]);
                    }
                    writer.flush();
                } catch(Exception e) {
                    e.printStackTrace();
                    System.out.println("Image was not sent");
                }
            }
            else if(type.equals(".txt") || type.equals(".TXT")){
                try {
                    byte[] bytearray = new byte[(int) download.length()];
                    FileInputStream fis = new FileInputStream(download);
                    DataInputStream disReader = new DataInputStream(fis);
                    disReader.read(bytearray, 0, bytearray.length);
                    writer.writeUTF("-A TEXT FILE HAS BEEN SENT-");
                    writer.writeUTF(Integer.toString(bytearray.length));
                    writer.write(bytearray, 0, bytearray.length);
                } catch (Exception e) {

                }
            }
        }
        catch(Exception e) {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    listMessages.getItems().add("An error has occurred, item was not sent.");
                }
            });
            
        }
    }
}

