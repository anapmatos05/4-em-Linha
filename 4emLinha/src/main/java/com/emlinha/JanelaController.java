package com.emlinha;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class JanelaController implements Initializable {
    
    // Lógica do Jogo
    private JogoModelo modelo = new JogoModelo();
    private int turnoAtual = 1; // 1 = Jogador 1 (Amarelo), 2 = Jogador 2 (Vermelho)
    private boolean jogoTerminado = false;
    private int colunaHover = -1;
    private int pecasEu = 0;
    private int pecasAdversario = 0;
    
    // Variáveis para guardar os nomes reais vindos do ecrã inicial
    private String nomeJogador1 = "Eu";
    private String nomeJogador2 = "Adversário";
    
    // Declarações injetadas pelo FXML
    @FXML private Canvas canvas;
    @FXML private Label labelTurno;
    @FXML private Label labelPecasEu;
    @FXML private Label labelPecasAdversario;
    @FXML private Label labelUltimaJogada;
    @FXML private HBox boxEu;
    @FXML private HBox boxAdversario;
    @FXML private VBox vboxEstatisticas;
    @FXML private VBox vboxVitoria;
    @FXML private Label labelVitoriaSubtitulo;
    
    // Variável para guardar o GraphicsContext e desenhar o tabuleiro depois
    private GraphicsContext gc;
    private boolean animando = false; 
    private int animColuna = -1;
    private int animLinha = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Obter o GraphicsContext assim que a interface é carregada
        if (canvas != null) {
            gc = canvas.getGraphicsContext2D();
            System.out.println("GraphicsContext carregado com sucesso!");
            // Desenhar o tabuleiro inicial
            desenharTabuleiro();
            labelPecasEu.setText(nomeJogador1 + " - 0");
            labelPecasAdversario.setText(nomeJogador2 + " - 0");
            labelUltimaJogada.setText("Nenhuma");
            labelUltimaJogada.setTextFill(Color.web("#9abccc"));
        }
    }
    
    /**
     * MÉTODOS PARA CONFIGURAR OS NOMES REAIS (Resolve o erro de compilação)
     */
    public void configurarJogadores(String p1, String p2) {
        this.nomeJogador1 = p1;
        this.nomeJogador2 = p2;
        
        // Atualiza as labels imediatamente com os nomes digitados
        labelPecasEu.setText(nomeJogador1 + " - " + pecasEu);
        labelPecasAdversario.setText(nomeJogador2 + " - " + pecasAdversario);
        
        if (turnoAtual == 1) {
            labelTurno.setText(nomeJogador1);
        } else {
            labelTurno.setText(nomeJogador2);
        }
    }
    
    @FXML
    public void canvasClicked(MouseEvent e) {
        // Ignora o clique se o jogo acabou ou se uma peça já está a cair
        if (jogoTerminado || animando) {
            return;
        }

        double larguraColuna = canvas.getWidth() / 7;
        int colunaSelecionada = (int) (e.getX() / larguraColuna);
        
        int linhaOndeCaiu = modelo.inserirPeca(colunaSelecionada, turnoAtual);
        
        if (linhaOndeCaiu != -1) { 
            // Iniciar o bloqueio e guardar as coordenadas da peça a animar
            animando = true;
            animColuna = colunaSelecionada;
            animLinha = linhaOndeCaiu;
            
            // Remove a bola do hover para ela não ficar parada no topo durante a queda
            colunaHover = -1; 
            
            double alturaTopo = 60.0;
            double alturaRestante = canvas.getHeight() - alturaTopo;
            double alturaLinha = alturaRestante / 6;
            double raio = Math.min(larguraColuna, alturaLinha) / 2.5;
            
            // Calcular de onde a peça sai (Topo) e onde vai parar
            final double startY = alturaTopo / 2; 
            final double endY = alturaTopo + (animLinha * alturaLinha) + (alturaLinha / 2);
            final double xPeca = (animColuna * larguraColuna) + (larguraColuna / 2) - raio;
            
            double[] currentY = { startY };
            
            Timeline timeline = new Timeline();
            KeyFrame frame = new KeyFrame(Duration.millis(15), event -> {
                currentY[0] += 25; // Aumentar este valor faz a peça cair mais rápido
                
                if (currentY[0] >= endY) { // A peça chegou ao fundo
                    currentY[0] = endY;
                    timeline.stop();
                    
                    // Repor o estado normal e desenhar o tabuleiro final
                    animando = false;
                    animColuna = -1;
                    animLinha = -1;
                    desenharTabuleiro(); 
                    
                    // 1. Atualizar as estatísticas laterais COM os dados da jogada
                    atualizarEstatisticas(colunaSelecionada, linhaOndeCaiu);
                    
                    // 2. Verificar a vitória ou mudar o turno
                    if (modelo.verificarVitoria(turnoAtual)) {
                        jogoTerminado = true;
                        
                        // Ocultar estatísticas e mostrar tela de vitória
                        vboxEstatisticas.setVisible(false);
                        vboxVitoria.setVisible(true);
                        
                        // Atualizar textos e cores da vitória com os nomes reais
                        if (turnoAtual == 1) {
                            labelVitoriaSubtitulo.setText(nomeJogador1 + " ganhou!");
                            labelVitoriaSubtitulo.setTextFill(Color.web("#ffc107"));
                        } else {
                            labelVitoriaSubtitulo.setText(nomeJogador2 + " ganhou!");
                            labelVitoriaSubtitulo.setTextFill(Color.web("#e53935"));
                        }
                        
                        // Redesenhar o tabuleiro uma última vez para mostrar o destaque
                        desenharTabuleiro(); 
                        
                    } else {
                        mudarTurnoVisual(); 
                    }
                    
                } else {
                    // ---- A ANIMAÇÃO ACONTECE AQUI ----
                    desenharTabuleiro(); 
                    gc.setFill(turnoAtual == 1 ? Color.web("#ffc107") : Color.web("#e53935"));
                    gc.fillOval(xPeca, currentY[0] - raio, raio * 2, raio * 2);
                }
            });
            
            timeline.getKeyFrames().add(frame);
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play(); // Dispara a animação
            
        } else {
            System.out.println("Coluna cheia!");
        }
    }
    
    @FXML
    public void canvasMoved(MouseEvent e) {
        if (jogoTerminado || animando) return; 
        
        double larguraColuna = canvas.getWidth() / 7;
        int colunaAtual = (int) (e.getX() / larguraColuna);
        
        if (colunaAtual != colunaHover) {
            colunaHover = colunaAtual;
            desenharTabuleiro();
        }
    }

    @FXML
    public void canvasExited(MouseEvent e) {
        colunaHover = -1;
        desenharTabuleiro();
    }
    
    private void desenharTabuleiro() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double alturaTopo = 60.0; 
        double larguraColuna = canvas.getWidth() / 7;
        double alturaRestante = canvas.getHeight() - alturaTopo;
        double alturaLinha = alturaRestante / 6;
        double raio = Math.min(larguraColuna, alturaLinha) / 2.5; 

        // 1. Desenhar a barra do topo e as bolas numéricas/hover
        gc.setFill(Color.web("#5a8ca0")); 
        gc.fillRect(0, 0, canvas.getWidth(), alturaTopo);
        gc.setFont(Font.font("System", FontWeight.BOLD, 22));

        for (int i = 0; i < 7; i++) {
            double centroX = (i * larguraColuna) + (larguraColuna / 2);
            double centroY = alturaTopo / 2;

            if (i == colunaHover && !jogoTerminado && !animando) {
                gc.setFill(turnoAtual == 1 ? Color.web("#ffc107") : Color.web("#e53935"));
            } else {
                gc.setFill(Color.web("#9abccc")); 
            }
            
            gc.fillOval(centroX - raio, centroY - raio, raio * 2, raio * 2);

            gc.setFill(Color.web("#083c54"));
            gc.fillText(String.valueOf(i + 1), centroX - 7, centroY + 8);
        }

        // 2. Desenhar a grelha
        int[][] estadoAtual = modelo.getTabuleiro();

        for (int linha = 0; linha < 6; linha++) {
            for (int coluna = 0; coluna < 7; coluna++) {
                
                double centroX = (coluna * larguraColuna) + (larguraColuna / 2);
                double centroY = alturaTopo + (linha * alturaLinha) + (alturaLinha / 2);
                double x = centroX - raio;
                double y = centroY - raio;

                int valor = estadoAtual[linha][coluna];
                
                if (linha == animLinha && coluna == animColuna) {
                    valor = 0;
                }

                if (valor == 1) {
                    gc.setFill(Color.web("#ffc107")); 
                } else if (valor == 2) {
                    gc.setFill(Color.web("#e53935")); 
                } else {
                    gc.setFill(Color.web("#9abccc")); 
                }
                
                gc.fillOval(x, y, raio * 2, raio * 2);
                
                // --- DESTACAR AS 4 PEÇAS VENCEDORAS ---
                if (jogoTerminado && modelo.getPecasVitoriosas() != null) {
                    for (int[] pos : modelo.getPecasVitoriosas()) {
                        if (pos[0] == linha && pos[1] == coluna) {
                            gc.setStroke(Color.web("#ffffff")); // Mudado para Branco para fazer contraste com Amarelo/Vermelho
                            gc.setLineWidth(4.0);
                            gc.strokeOval(x, y, raio * 2, raio * 2);
                        }
                    }
                }
            }
        }
    }
    
    private void atualizarEstatisticas(int col, int lin) {
        if (turnoAtual == 1) {
            pecasEu++;
            labelPecasEu.setText(nomeJogador1 + " - " + pecasEu);
        } else {
            pecasAdversario++;
            labelPecasAdversario.setText(nomeJogador2 + " - " + pecasAdversario);
        }
        
        int linhaVisual = 6 - lin;
        labelUltimaJogada.setText("Col. " + (col + 1) + ", L. " + linhaVisual);
        labelUltimaJogada.setTextFill(turnoAtual == 1 ? Color.web("#ffc107") : Color.web("#e53935"));
    }

    private void mudarTurnoVisual() {
        if (turnoAtual == 1) {
            turnoAtual = 2; 
            
            labelTurno.setText(nomeJogador2); // Nome real do J2
            labelTurno.setTextFill(Color.web("#e53935"));
            
            boxEu.setStyle("-fx-border-color: transparent; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
            boxAdversario.setStyle("-fx-border-color: #e53935; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
            
        } else {
            turnoAtual = 1; 
            
            labelTurno.setText(nomeJogador1); // Nome real do J1
            labelTurno.setTextFill(Color.web("#ffc107"));
            
            boxAdversario.setStyle("-fx-border-color: transparent; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
            boxEu.setStyle("-fx-border-color: #ffc107; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
        }
    }
    
    @FXML
    public void acaoJogarDeNovo(ActionEvent event) {
        modelo.reiniciarJogo();
        
        jogoTerminado = false;
        pecasEu = 0;
        pecasAdversario = 0;
        turnoAtual = 1;

        labelPecasEu.setText(nomeJogador1 + " - 0");
        labelPecasAdversario.setText(nomeJogador2 + " - 0");
        labelUltimaJogada.setText("Nenhuma");
        labelUltimaJogada.setTextFill(Color.web("#9abccc"));

        boxAdversario.setStyle("-fx-border-color: transparent; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
        boxEu.setStyle("-fx-border-color: #ffc107; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
        labelTurno.setText(nomeJogador1);
        labelTurno.setTextFill(Color.web("#ffc107"));

        vboxVitoria.setVisible(false);
        vboxEstatisticas.setVisible(true);

        desenharTabuleiro();
    }

    @FXML
    public void acaoSair(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }
}