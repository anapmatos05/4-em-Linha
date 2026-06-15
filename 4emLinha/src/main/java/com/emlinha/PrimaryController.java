package com.emlinha;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    // Reaproveitamento exato dos IDs que já tens mapeados do Scene Builder
    @FXML
    private TextField txt_player1; // Será usado para o "Meu Nome"

    @FXML
    private TextField txt_player2; // Será usado para o "IP do Adversário" (Se vazio = Criar Jogo)

    @FXML
    private Button btn_start; // O botão que dispara a ação de conexão/inicialização

    private final int PORTA_JOGO = 12345; // Porta padrão padrão para a comunicação local

    /**
     * Método acionado quando o utilizador clica no botão "Iniciar Jogo".
     * Decide se cria ou entra num jogo com base nos campos preenchidos.
     */
    @FXML
    private void btnStartOnAction(ActionEvent event) {
        String meuNome = txt_player1.getText().trim();
        String ipAdversario = txt_player2.getText().trim();

        // 1. Validação do nome do próprio jogador
        if (meuNome.isEmpty()) {
            mostrarAviso("Campo Vazio", AlertType.WARNING, "Por favor, introduza o seu nome no campo 'Jogador 1'!");
            return;
        }

        try {
            // 2. Carregar o ecrã do Tabuleiro (Janela.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Janela.fxml"));
            Parent root = loader.load();
            JanelaController janelaController = loader.getController();

            // 3. Identificar a intenção: Se o campo do IP (txt_player2) estiver vazio, Criamos Sala (Host)
            if (ipAdversario.isEmpty()) {
                // Obter o IP real da tua máquina para mostrares ao teu colega
                String ipLocal = InetAddress.getLocalHost().getHostAddress();
                System.out.println("A iniciar modo Servidor (Host)...");

                // Configura os nomes iniciais (Tu és o Amarelo/J1, o outro será atualizado ao entrar)
                janelaController.configurarJogadores(meuNome, "Adversário");

                // Instancia o gestor de rede e abre o ServerSocket
                GerenteRede gerente = new GerenteRede(janelaController);
                gerente.iniciarServidor(PORTA_JOGO);

                // O Host começa a jogar primeiro (true)
                janelaController.configurarRede(gerente, true);

                // Transita para o ecrã do tabuleiro
                mudarParaTabuleiro(root);
                
                // Exibe o pop-up com as instruções do IP
                mostrarAviso("Sala Criada com Sucesso", AlertType.INFORMATION, 
                    "Dá este IP ao teu colega: " + ipLocal + "\nO jogo começará assim que ele se conectar!");

            } else {
                // Se o campo de texto tiver um IP, vamos conectar-nos a ele (Cliente/Join)
                System.out.println("A conectar ao Host em: " + ipAdversario);

                // Configura os nomes (Quem se conecta entra na cadeira do Jogador 2)
                janelaController.configurarJogadores("Criador", meuNome);

                // Instancia o gestor de rede e conecta ao Socket remoto
                GerenteRede gerente = new GerenteRede(janelaController);
                gerente.conectarAoServidor(ipAdversario, PORTA_JOGO);

                // O Cliente joga em segundo lugar (false), aguardando a jogada do Host
                janelaController.configurarRede(gerente, false);

                // Transita para o ecrã do tabuleiro
                mudarParaTabuleiro(root);
            }

        } catch (UnknownHostException e) {
            mostrarAviso("Erro de Rede", AlertType.ERROR, "Não foi possível detetar o teu endereço IP local.");
        } catch (IOException e) {
            System.err.println("Erro ao carregar o ficheiro Janela.fxml.");
            e.printStackTrace();
            mostrarAviso("Erro de Carregamento", AlertType.ERROR, "Não foi possível abrir o ecrã do tabuleiro do jogo.");
        }
    }

    /**
     * Realiza a troca de cenas na Stage atual.
     */
    private void mudarParaTabuleiro(Parent root) {
        Stage stage = (Stage) btn_start.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("4 em Linha - Modo Online");
        stage.show();
    }

    /**
     * Pop-up configurável para interações com o utilizador.
     */
    private void mostrarAviso(String titulo, AlertType tipo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}