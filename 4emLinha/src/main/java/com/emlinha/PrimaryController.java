package com.emlinha;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PrimaryController {

    @FXML
    private TextField txt_player1; // "Meu Nome"

    @FXML
    private TextField txt_player2; // "IP do Adversário"

    @FXML
    private Button btn_start; 

    private final int PORTA_JOGO = 12345; 

    @FXML
    private void btnStartOnAction(ActionEvent event) {
        String meuNome = txt_player1.getText().trim();
        String ipAdversario = txt_player2.getText().trim();

        if (meuNome.isEmpty()) {
            mostrarAviso("Campo Vazio", AlertType.WARNING, "Por favor, introduza o seu nome!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Janela.fxml"));
            Parent root = loader.load();
            JanelaController janelaController = loader.getController();

            if (ipAdversario.isEmpty()) {
                // ---------- MODO SERVIDOR (HOST) ----------
                String ipLocal = "127.0.0.1";
                try {
                    ipLocal = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    System.out.println("Não foi possível obter o IP externo, a usar fallback.");
                }
                
                System.out.println("A iniciar modo Servidor (Host)...");
                janelaController.configurarJogadores(meuNome, "A aguardar...");

                GerenteRede gerente = new GerenteRede(janelaController);
                janelaController.configurarRede(gerente, true); // Começa a jogar (Amarelo)

                // Transita para o tabuleiro primeiro para a interface não congelar
                mudarParaTabuleiro(root);

                // Mostra o aviso e, ao fechar o pop-up, o servidor abre a porta em segundo plano
                String finalIp = ipLocal;
                Platform.runLater(() -> {
                    mostrarAviso("Sala Criada", AlertType.INFORMATION, 
                        "Dá este IP ao teu colega: " + finalIp + "\nO jogo começará assim que ele se conectar!");
                    
                    // Dispara a inicialização do ServerSocket (deve correr numa Thread dentro do GerenteRede)
                    gerente.iniciarServidor(PORTA_JOGO);
                });

            } else {
                // ---------- MODO CLIENTE (JOIN) ----------
                System.out.println("A conectar ao Host em: " + ipAdversario);
                janelaController.configurarJogadores("Criador", meuNome);

                GerenteRede gerente = new GerenteRede(janelaController);
                
                // Tenta estabelecer a ligação física antes de mudar de ecrã
                gerente.conectarAoServidor(ipAdversario, PORTA_JOGO);
                
                janelaController.configurarRede(gerente, false); // Joga em segundo (Vermelho)

                mudarParaTabuleiro(root);
            }

        } catch (IOException e) {
            System.err.println("Erro ao carregar o ficheiro Janela.fxml.");
            e.printStackTrace();
            mostrarAviso("Erro de Conexão", AlertType.ERROR, 
                "Não foi possível estabelecer ligação. Garante que o IP está correto e a sala já foi criada!");
        }
    }

    private void mudarParaTabuleiro(Parent root) {
        Stage stage = (Stage) btn_start.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("4 em Linha - Modo Online");
        stage.show();
    }

    private void mostrarAviso(String titulo, AlertType tipo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}