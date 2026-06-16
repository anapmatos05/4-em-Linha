package com.emlinha;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
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
    private int turnoAtual = 1; // 1 = Amarelo, 2 = Vermelho
    private boolean jogoTerminado = false;
    private int colunaHover = -1;
    private int pecasEu = 0;
    private int pecasAdversario = 0;
    
    private String nomeJogador1 = "Eu";
    private String nomeJogador2 = "Adversário";
    
    // Controlo de Rede
    private GerenteRede gerenteRede;
    private boolean meuTurnoDeRede = true; 
    private boolean euComecoOJogo = true; // Guarda quem tem a vez inicial no Reset
    
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
    @FXML private Label labelNomeAdversario;
    
    // Injeção do topo
    @FXML private Label labelTopoJogador1;
    @FXML private Label labelTopoJogador2;
    
    // --- COMPONENTES DO BLOCO DE ERRO ---
    @FXML private HBox boxErro; 
    @FXML private Label labelErro; 
    
    private GraphicsContext gc;
    private boolean animando = false; 
    private int animColuna = -1;
    private int animLinha = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (canvas != null) {
            gc = canvas.getGraphicsContext2D();
            desenharTabuleiro();
            labelPecasEu.setText(nomeJogador1 + " - 0");
            labelPecasAdversario.setText(nomeJogador2 + " - 0");
            labelUltimaJogada.setText("Nenhuma");
            labelUltimaJogada.setTextFill(Color.web("#9abccc"));
        }
    }
    
    public void configurarJogadores(String p1, String p2) {
        this.nomeJogador1 = p1;
        this.nomeJogador2 = p2;
        
        labelPecasEu.setText(nomeJogador1 + " - " + pecasEu);
        labelPecasAdversario.setText(nomeJogador2 + " - " + pecasAdversario);
        
        if (turnoAtual == 1) {
            labelTurno.setText(nomeJogador1);
        } else {
            labelTurno.setText(nomeJogador2);
        }
        
        if (labelNomeAdversario != null) {
            labelNomeAdversario.setText(nomeJogador2);
        }
    }
    
    public void receberNomeAdversarioRemoto(String nomeRecebido) {
        Platform.runLater(() -> {
            configurarJogadores(this.nomeJogador1, nomeRecebido);
            System.out.println("Interface atualizada com o nome do adversário: " + nomeRecebido);
        });
    }

    public void configurarRede(GerenteRede gerente, boolean comecaAJogar) {
        this.gerenteRede = gerente;
        this.meuTurnoDeRede = comecaAJogar;
        this.euComecoOJogo = comecaAJogar; // Memoriza quem iniciou a partida
        System.out.println("Rede configurada no controlador. Meu turno de rede: " + comecaAJogar);
        this.gerenteRede.enviarComando("NOME:" + this.nomeJogador1);
    }

    public void atualizarNomeAdversario(String nomeDoOutro) {
        if (this.meuTurnoDeRede) {
            this.nomeJogador2 = nomeDoOutro;
        } else {
            this.nomeJogador1 = nomeDoOutro;
        }
        
        Platform.runLater(() -> {
            labelPecasEu.setText(nomeJogador1 + " - " + pecasEu);
            labelPecasAdversario.setText(nomeJogador2 + " - " + pecasAdversario);
            labelTurno.setText(turnoAtual == 1 ? nomeJogador1 : nomeJogador2);
            
            if (labelTopoJogador1 != null) labelTopoJogador1.setText(nomeJogador1);
            if (labelTopoJogador2 != null) labelTopoJogador2.setText(nomeJogador2);
            
            desenharTabuleiro();
        });
    }

    public String getNomeJogador1() {
        return this.nomeJogador1;
    }

    public String getNomeJogador2() {
        return this.nomeJogador2;
    }

    public void receberJogadaRemota(int coluna) {
        System.out.println("Jogada remota recebida na coluna: " + coluna);
        // Proteção: Executa na Thread do JavaFX para evitar erros visuais na animação
        Platform.runLater(() -> {
            processarJogada(coluna);
            this.meuTurnoDeRede = true;
        });
    }
    
    @FXML
    public void canvasClicked(MouseEvent e) {
        if (jogoTerminado || animando || !meuTurnoDeRede) {
            return;
        }

        double larguraColuna = canvas.getWidth() / 7;
        int colunaSelecionada = (int) (e.getX() / larguraColuna);
        
        // Verifica se o topo da coluna está vazio (Valor 0 significa livre)
        if (modelo.getTabuleiro()[0][colunaSelecionada] == 0) {
            
            // Esconde o bloco de erro pois a jogada é válida ---
            if (boxErro != null) {
                boxErro.setVisible(false);
            }

            // Altera imediatamente o turno de rede para evitar cliques fantasmas rápidos
            this.meuTurnoDeRede = false;

            if (gerenteRede != null) {
                gerenteRede.enviarComando("JOGADA:" + colunaSelecionada);
            }
            
            processarJogada(colunaSelecionada);
        } else {
            // Mostra o bloco vermelho com a mensagem de coluna cheia ---
            if (boxErro != null && labelErro != null) {
                labelErro.setText("A Coluna " + (colunaSelecionada + 1) + " está Cheia! Escolhe outra");
                boxErro.setVisible(true);
            }
            System.out.println("Coluna cheia!");
        }
    }

    private void processarJogada(int colunaSelecionada) {
        final int turnoDaJogada = this.turnoAtual;
        
        int linhaOndeCaiu = modelo.inserirPeca(colunaSelecionada, turnoDaJogada);
        
        if (linhaOndeCaiu != -1) { 
            // Garante que o erro desaparece quando o adversário joga remoto ---
            if (boxErro != null) {
                boxErro.setVisible(false);
            }

            animando = true;
            animColuna = colunaSelecionada;
            animLinha = linhaOndeCaiu;
            colunaHover = -1; 
            
            double larguraColuna = canvas.getWidth() / 7;
            double alturaTopo = 60.0;
            double alturaRestante = canvas.getHeight() - alturaTopo;
            double alturaLinha = alturaRestante / 6;
            double raio = Math.min(larguraColuna, alturaLinha) / 2.5;
            
            final double startY = alturaTopo / 2; 
            final double endY = alturaTopo + (animLinha * alturaLinha) + (alturaLinha / 2);
            final double xPeca = (animColuna * larguraColuna) + (larguraColuna / 2) - raio;
            
            double[] currentY = { startY };
            
            Timeline timeline = new Timeline();
            KeyFrame frame = new KeyFrame(Duration.millis(15), event -> {
                currentY[0] += 25; 
                
                if (currentY[0] >= endY) { 
                    currentY[0] = endY;
                    timeline.stop();
                    
                    animando = false;
                    animColuna = -1;
                    animLinha = -1;
                    desenharTabuleiro(); 
                    
                    atualizarEstatisticas(colunaSelecionada, linhaOndeCaiu, turnoDaJogada);
                    
                    if (modelo.verificarVitoria(turnoDaJogada)) {
                        jogoTerminado = true;
                        vboxEstatisticas.setVisible(false);
                        vboxVitoria.setVisible(true);
                        
                        if (turnoDaJogada == 1) {
                            labelVitoriaSubtitulo.setText(nomeJogador1 + " ganhou!");
                            labelVitoriaSubtitulo.setTextFill(Color.web("#ffc107"));
                        } else {
                            labelVitoriaSubtitulo.setText(nomeJogador2 + " ganhou!");
                            labelVitoriaSubtitulo.setTextFill(Color.web("#e53935"));
                        }
                        desenharTabuleiro(); 
                        
                    } 
                    // --- VERIFICAR EMPATE ---
                    else if (modelo.verificarEmpate()) {
                        jogoTerminado = true;
                        
                        vboxEstatisticas.setVisible(false);
                        vboxVitoria.setVisible(true);
                        
                        labelVitoriaSubtitulo.setText("Empate!");
                        labelVitoriaSubtitulo.setTextFill(Color.web("#ffffff")); 
                        
                        desenharTabuleiro();
                    } 
                    // -------------------------------------
                    else {
                        mudarTurnoVisual(); 
                    }
                    
                } else {
                    desenharTabuleiro(); 
                    gc.setFill(turnoDaJogada == 1 ? Color.web("#ffc107") : Color.web("#e53935"));
                    gc.fillOval(xPeca, currentY[0] - raio, raio * 2, raio * 2);
                }
            });
            
            timeline.getKeyFrames().add(frame);
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
        }
    }
    
    @FXML
    public void canvasMoved(MouseEvent e) {
        if (jogoTerminado || animando || !meuTurnoDeRede) return; 
        
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

        gc.setFill(Color.web("#5a8ca0")); 
        gc.fillRect(0, 0, canvas.getWidth(), alturaTopo);
        gc.setFont(Font.font("System", FontWeight.BOLD, 22));

        for (int i = 0; i < 7; i++) {
            double centroX = (i * larguraColuna) + (larguraColuna / 2);
            double centroY = alturaTopo / 2;

            if (i == colunaHover && !jogoTerminado && !animando && meuTurnoDeRede) {
                gc.setFill(turnoAtual == 1 ? Color.web("#ffc107") : Color.web("#e53935"));
            } else {
                gc.setFill(Color.web("#9abccc")); 
            }
            
            gc.fillOval(centroX - raio, centroY - raio, raio * 2, raio * 2);

            gc.setFill(Color.web("#083c54"));
            gc.fillText(String.valueOf(i + 1), centroX - 7, centroY + 8);
        }

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
                
                if (jogoTerminado && modelo.getPecasVitoriosas() != null) {
                    for (int[] pos : modelo.getPecasVitoriosas()) {
                        if (pos[0] == linha && pos[1] == coluna) {
                            gc.setStroke(Color.web("#ffffff")); 
                            gc.setLineWidth(4.0);
                            gc.strokeOval(x, y, raio * 2, raio * 2);
                        }
                    }
                }
            }
        }
    }
    
    private void atualizarEstatisticas(int col, int lin, int turnoDaJogada) {
        if (turnoDaJogada == 1) {
            pecasEu++;
            labelPecasEu.setText(nomeJogador1 + " - " + pecasEu);
        } else {
            pecasAdversario++;
            labelPecasAdversario.setText(nomeJogador2 + " - " + pecasAdversario);
        }
        
        int linhaVisual = 6 - lin;
        labelUltimaJogada.setText("Col. " + (col + 1) + ", L. " + linhaVisual);
        labelUltimaJogada.setTextFill(turnoDaJogada == 1 ? Color.web("#ffc107") : Color.web("#e53935"));
    }

    private void mudarTurnoVisual() {
        if (turnoAtual == 1) {
            turnoAtual = 2; 
            labelTurno.setText(nomeJogador2); 
            labelTurno.setTextFill(Color.web("#e53935"));
            
            boxEu.setStyle("-fx-border-color: transparent; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
            boxAdversario.setStyle("-fx-border-color: #e53935; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
        } else {
            turnoAtual = 1; 
            labelTurno.setText(nomeJogador1); 
            labelTurno.setTextFill(Color.web("#ffc107"));
            
            boxAdversario.setStyle("-fx-border-color: transparent; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
            boxEu.setStyle("-fx-border-color: #ffc107; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
        }
    }
    
    @FXML
    public void acaoJogarDeNovo(ActionEvent event) {
        if (gerenteRede != null) {
            gerenteRede.enviarComando("RESTART");
        }
        reiniciarJogoLocal();
    }

    @FXML
    public void acaoSair(ActionEvent event) {
        if (gerenteRede != null) {
            gerenteRede.fecharConexao();
        }
        Platform.exit();
        System.exit(0);
    }
    
    private void reiniciarJogoLocal() {
        modelo.reiniciarJogo();
        jogoTerminado = false;
        pecasEu = 0;
        pecasAdversario = 0;
        turnoAtual = 1; // O Amarelo começa sempre estruturalmente no modelo

        // Sincroniza quem joga na rede com base na configuração inicial da partida
        this.meuTurnoDeRede = this.euComecoOJogo;

        labelPecasEu.setText(nomeJogador1 + " - 0");
        labelPecasAdversario.setText(nomeJogador2 + " - 0");
        labelUltimaJogada.setText("Nenhuma");
        labelUltimaJogada.setTextFill(Color.web("#9abccc"));

        boxAdversario.setStyle("-fx-border-color: transparent; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
        boxEu.setStyle("-fx-border-color: #ffc107; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
        labelTurno.setText(nomeJogador1);
        labelTurno.setTextFill(Color.web("#ffc107"));

       boxEu.setStyle("-fx-border-color: #ffc107; -fx-border-radius: 30; -fx-border-width: 2; -fx-padding: 5 15 5 15;");
        labelTurno.setText(nomeJogador1);
        labelTurno.setTextFill(Color.web("#ffc107"));

        vboxVitoria.setVisible(false);
        vboxEstatisticas.setVisible(true);
        
        if (boxErro != null) {
            boxErro.setVisible(false);
        }

        desenharTabuleiro();
    }
    
    public void receberRestartRemoto() {
        Platform.runLater(() -> {
            reiniciarJogoLocal();
        });
    }
    
}  

