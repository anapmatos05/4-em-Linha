package com.emlinha;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class JanelaController implements Initializable {

    // Declaração do Canvas injetado pelo FXML
    @FXML
    private Canvas canvas;
    
    // Variável para guardar o GraphicsContext e desenhar o tabuleiro depois
    private GraphicsContext gc;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Obter o GraphicsContext assim que a interface é carregada
        if (canvas != null) {
            gc = canvas.getGraphicsContext2D();
            
            // Podes começar a testar o teu canvas aqui
            System.out.println("GraphicsContext carregado com sucesso!");
        }
    }
}