package com.emlinha;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("Janela"));
        
        // 1. Dar um título à Janela do Windows
        stage.setTitle("4 em Linha - Grupo 02");
        
        // 2. Definir o tamanho mínimo para garantir que nada fica cortado
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        
        // 3. Bloquear o redimensionamento da janela (Impede que o design se desformate)
        stage.setResizable(false); 
        
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}