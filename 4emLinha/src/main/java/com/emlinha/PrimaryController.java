package com.emlinha;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.net.InetAddress;
import javafx.scene.control.Alert;

public class PrimaryController {

    @FXML private TextField campoNome;
    @FXML private TextField campoIP;

    @FXML
    public void acaoCriarJogo(ActionEvent event) throws IOException {
        String meuNome = campoNome.getText().trim();
        
        if (meuNome.isEmpty()) {
            marcarErroCampo(campoNome, "Nome obrigatório!");
            return; 
        }
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Janela.fxml"));
        Parent root = loader.load();
        JanelaController janelaController = loader.getController();
        
        GerenteRede gerente = new GerenteRede(janelaController);
        gerente.iniciarServidor(12345);
        
        String meuIP = "Desconhecido";
        try {
            meuIP = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            System.out.println("Não foi possível obter o IP.");
        }
        
        // 1. Volta a colocar "A aguardar..." para não estragar a interface!
        janelaController.configurarJogadores(meuNome, "A aguardar...");
        janelaController.configurarRede(gerente, true);
        
        // 2. Coloca o IP no título da Janela (barra superior do Windows)
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("4 em Linha - A aguardar na sala (O teu IP: " + meuIP + ")");
        
        mudarParaJanelaJogo(event, root);
        
        // 3. Mostra um Pop-up claro e bonito com o IP mal a janela abre
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Sala Criada com Sucesso!");
        alerta.setHeaderText("À espera do adversário...");
        alerta.setContentText("Diz este IP ao adversario para ele se conectar:\n" + meuIP);
        alerta.show();
    }

    @FXML
    public void acaoEntrarJogo(ActionEvent event) throws IOException {
        String meuNome = campoNome.getText().trim();
        String ip = campoIP.getText().trim();
        
        boolean encontrouErro = false;

        // Validação obrigatória do Nome
        if (meuNome.isEmpty()) {
            marcarErroCampo(campoNome, "Nome obrigatório!");
            encontrouErro = true;
        }

        // Validação obrigatória do IP
        if (ip.isEmpty()) {
            marcarErroCampo(campoIP, "IP obrigatório!");
            encontrouErro = true;
        }

        if (encontrouErro) return; // Bloqueia se faltar algum dos campos

        FXMLLoader loader = new FXMLLoader(getClass().getResource("Janela.fxml"));
        Parent root = loader.load();
        JanelaController janelaController = loader.getController();
        
        GerenteRede gerente = new GerenteRede(janelaController);
        gerente.conectarAoServidor(ip, 12345);
        
        janelaController.configurarJogadores(meuNome, "Adversário");
        janelaController.configurarRede(gerente, false); 
        
        mudarParaJanelaJogo(event, root);
    }
    
    /**
     * Pinta a caixa de texto de vermelho para avisar o utilizador
     */
    private void marcarErroCampo(TextField campo, String mensagemErro) {
        campo.setText(""); // Limpa espaços em branco caso existam
        campo.setPromptText(mensagemErro);
        campo.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-background-radius: 5; -fx-prompt-text-fill: #ffcdd2;");
        
        // Retira a cor vermelha assim que o utilizador clicar para voltar a escrever
        campo.setOnMouseClicked(e -> {
            campo.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 5;");
            campo.setPromptText("");
        });
    }

    private void mudarParaJanelaJogo(ActionEvent event, Parent root) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}