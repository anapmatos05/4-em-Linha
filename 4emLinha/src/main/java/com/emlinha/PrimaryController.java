package com.emlinha;

import java.io.IOException;
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

    // RF01 - Mapeamento exato dos IDs definidos no Scene Builder
    @FXML
    private TextField txt_player1;

    @FXML
    private TextField txt_player2;

    @FXML
    private Button btn_start;

    /**
     * Método acionado quando o utilizador clica no botão "Iniciar Jogo".
     * Valida os inputs (RF01) e abre o ecrã do tabuleiro do jogo (Janela.fxml).
     */
    @FXML
    private void btnStartOnAction(ActionEvent event) {
        String nomeJ1 = txt_player1.getText().trim();
        String nomeJ2 = txt_player2.getText().trim();

        // 1. Validação de segurança (Não permitir jogar com campos vazios)
        if (nomeJ1.isEmpty() || nomeJ2.isEmpty()) {
            mostrarAviso("Campos Vazios", "Por favor, introduza o nome de ambos os jogadores antes de começar!");
            return;
        }

        try {
            System.out.println("A iniciar partida: " + nomeJ1 + " vs " + nomeJ2);

            // 2. Carregar o ecrã do Tabuleiro (Janela.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Janela.fxml"));
            Parent root = loader.load();

            // 3. INJETAR OS NOMES REAIS NO JANELACONTROLLER (Crucial para o RF01)
            JanelaController janelaController = loader.getController();
            janelaController.configurarJogadores(nomeJ1, nomeJ2);

            // 4. Mudar a cena na janela (Stage) atual para mostrar a Janela.fxml
            Stage stage = (Stage) btn_start.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("4 em Linha - Jogo em Curso");
            stage.show();

        } catch (IOException e) {
            System.err.println("Erro ao carregar o ficheiro Janela.fxml. Verifica se o ficheiro está na pasta certa.");
            e.printStackTrace();
            mostrarAviso("Erro de Carregamento", "Não foi possível abrir o ecrã do tabuleiro do jogo.");
        }
    }

    /**
     * Pop-up de aviso para alertar os utilizadores em caso de erro de preenchimento.
     */
    private void mostrarAviso(String titulo, String mensagem) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}