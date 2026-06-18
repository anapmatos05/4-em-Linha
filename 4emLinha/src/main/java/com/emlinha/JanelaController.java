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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Controlador da Janela Principal do Jogo "4 em Linha".
 * Gere a interface gráfica (JavaFX Canvas), as interações do utilizador,
 * as animações das peças e a sincronização com o módulo de rede.
 */
public class JanelaController implements Initializable {
    
    // ==========================================
    // ANOTAÇÃO: ATRIBUTOS E ESTADO INTERNO DO JOGO
    // ==========================================
    
    private JogoModelo modelo = new JogoModelo(); // Instância que simula a matriz interna do tabuleiro
    private int turnoAtual = 1;                  // Controla o jogador ativo localmente (1 = Amarelo, 2 = Vermelho)
    private boolean jogoTerminado = false;       // Impede novas ações caso o jogo tenha chegado ao fim
    private int colunaHover = -1;                // Controla qual a coluna que tem o rato posicionado por cima (-1 para nenhuma)
    private int pecasEu = 0;                     // Contador de peças jogadas por "Eu"
    private int pecasAdversario = 0;             // Contador de peças jogadas pelo adversário
    
    private String nomeJogador1 = "Eu";          // Nome predefinido do Jogador 1 (Amarelo / Host)
    private String nomeJogador2 = "Adversário";  // Nome predefinido do Jogador 2 (Vermelho / Cliente)
    
    // ==========================================
    // ANOTAÇÃO: CONTROLO DE REDE
    // ==========================================
    
    private GerenteRede gerenteRede;             // Objeto encarregue de gerir a socket TCP/IP
    private boolean meuTurnoDeRede = true;       // Define se o jogador local tem permissão para clicar e jogar no tabuleiro
    private boolean euComecoOJogo = true;        // Memoriza o estado inicial de quem começa para aplicar a lógica correta no reiniciar
    private boolean aRegressarAoMenu = false; 

    // ==========================================
    // ANOTAÇÃO: COMPONENTES INJETADOS PELO FXML
    // ==========================================
    
    @FXML private Canvas canvas;                 // Área gráfica de desenho do tabuleiro
    @FXML private Label labelTurno;              // Texto indicador do jogador que está a jogar
    @FXML private Label labelPecasEu;            // Painel estatístico das peças do jogador local
    @FXML private Label labelPecasAdversario;    // Painel estatístico das peças do adversário
    @FXML private Label labelUltimaJogada;       // Texto com as coordenadas (Coluna e Linha) da jogada anterior
    @FXML private HBox boxEu;                    // Contentor visual que destaca o turno do jogador local
    @FXML private HBox boxAdversario;            // Contentor visual que destaca o turno do adversário
    @FXML private VBox vboxEstatisticas;         // Painel lateral de dados informativos
    @FXML private VBox vboxVitoria;              // Painel de sobreposição exibido no fim do jogo
    @FXML private Label labelVitoriaSubtitulo;   // Texto de felicitação ao vencedor (ou indicação de empate)
    @FXML private Label labelNomeAdversario;     // Etiqueta com o nome do adversário
    @FXML private Label labelAvisoColuna;        // Mensagem de erro temporária caso uma coluna atinja o limite máximo
    
    @FXML private Label labelTopoJogador1;       // Nome do Jogador 1 na barra superior de interface
    @FXML private Label labelTopoJogador2;       // Nome do Jogador 2 na barra superior de interface
    
    @FXML private HBox boxErro;                  // Contentor estrutural para feedback de erros graves de rede
    @FXML private Label labelErro;               // Mensagem textual de erro de rede
    
    @FXML private Button btnHome;                // Botão para voltar ao ecrã inicial
    @FXML private Button btnPausa;               // Botão para pausar a sessão de jogo
    @FXML private VBox vboxPausa;                // Painel visual sobreposto com as opções de pausa
    private boolean jogoPausado = false;         // Flag que sinaliza se a partida está suspensa
    
    // ==========================================
    // ANOTAÇÃO: COMPONENTES DE DESENHO E ANIMAÇÃO
    // ==========================================
    
    private GraphicsContext gc;                  // Motor de renderização do Canvas
    private boolean animando = false;            // Bloqueia a interface enquanto uma peça estiver a cair
    private int animColuna = -1;                 // Alvo da coluna onde a animação ocorre
    private int animLinha = -1;                  // Alvo da linha destino final da peça animada

    /**
     * Método automático do ciclo de vida JavaFX.
     * Prepara o contexto gráfico e renderiza o esqueleto inicial do tabuleiro de jogo.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (canvas != null) {
            gc = canvas.getGraphicsContext2D();
            desenharTabuleiro();
            
            // Inicialização padrão das etiquetas com estilos de cores neutras
            labelPecasEu.setText(nomeJogador1 + " - 0");
            labelPecasAdversario.setText(nomeJogador2 + " - 0");
            labelUltimaJogada.setText("Nenhuma");
            labelUltimaJogada.setTextFill(Color.web("#9abccc"));
        }
    }
    
    /**
     * Atualiza os nomes lógicos e visuais dos jogadores na interface do utilizador.
     * @param p1 Nome associado ao Jogador 1 (Amarelo)
     * @param p2 Nome associado ao Jogador 2 (Vermelho)
     */
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
        
        // Garante a reconfiguração dos nós de texto na barra de navegação de topo
        if (labelTopoJogador1 != null) {
    labelTopoJogador1.setText(nomeJogador1);
}
if (labelTopoJogador2 != null) {
    labelTopoJogador2.setText(nomeJogador2);
}
if (labelNomeAdversario != null) {
    labelNomeAdversario.setText(nomeJogador2);
}
    }
    
    /**
     * Processa a string de identificação do rival ligada via rede.
     * Reordena as posições dependendo se este cliente atua como Host ou Convidado.
     */
    public void receberNomeAdversarioRemoto(String nomeRecebido) {
        Platform.runLater(() -> {
            if (this.euComecoOJogo) {
                // Cenário: Sou o Host (Amarelo). O nome recebido pertence ao Cliente remoto (Vermelho).
                configurarJogadores(this.nomeJogador1, nomeRecebido);
            } else {
                // Cenário: Sou o Cliente (Vermelho). O nome recebido mapeia o Host remoto (Amarelo).
                configurarJogadores(nomeRecebido, this.nomeJogador2);
            }
            
            desenharTabuleiro();
            System.out.println("Nomes perfeitamente sincronizados!");
        });
    }

    /**
     * Vincula a instância do gestor de sockets ao controlador da janela.
     * Configura quem detém a prioridade de início do jogo.
     */
    public void configurarRede(GerenteRede gerente, boolean comecaAJogar) {
        this.gerenteRede = gerente;
        this.meuTurnoDeRede = comecaAJogar;
        this.euComecoOJogo = comecaAJogar; 
        
        if (!comecaAJogar) {
            // Se o utilizador atual é o Cliente Convidado, o seu nome local migra 
            // para a ranhura 2 (Vermelho), libertando a ranhura 1 para o Host.
            this.nomeJogador2 = this.nomeJogador1; 
            this.nomeJogador1 = "Adversário";      
            
            Platform.runLater(() -> {
                configurarJogadores(this.nomeJogador1, this.nomeJogador2);
            });
        }
        System.out.println("Rede configurada no controlador. Sou o Host? " + comecaAJogar);
    }
    
    /**
     * Comunica o identificador local à máquina remota imediatamente após o handshake inicial.
     */
    public void enviarMeuNome() {
        if (gerenteRede != null) {
            String meuNomeReal = this.euComecoOJogo ? this.nomeJogador1 : this.nomeJogador2;
            gerenteRede.enviarComando("NOME:" + meuNomeReal);
        }
    }

    public String getNomeJogador1() {
        return this.nomeJogador1;
    }

    public String getNomeJogador2() {
        return this.nomeJogador2;
    }

    /**
     * Intercepa uma ação enviada pela ligação à rede e introduz a jogada na perspetiva local.
     */
    public void receberJogadaRemota(int coluna) {
        System.out.println("Jogada remota recebida na coluna: " + coluna);
        // Obriga a manipulação gráfica a rodar no segmento gráfico do JavaFX
        Platform.runLater(() -> {
            processarJogada(coluna);
            this.meuTurnoDeRede = true; // Devolve o controlo de interação ao jogador local
        });
    }
    
    /**
     * Deteta o clique do rato sobre a área do Canvas, calculando dinamicamente a coluna visada.
     */
    @FXML
    public void canvasClicked(MouseEvent e) {
        // Cláusula de barreira para invalidar cliques durante animações, pausas ou turnos alheios
        if (jogoTerminado || animando || !meuTurnoDeRede || jogoPausado) {
            return;
        }

        double larguraColuna = canvas.getWidth() / 7;
        int colunaSelecionada = (int) (e.getX() / larguraColuna);
        
        // Valida se a ranhura do topo (linha index 0) está vazia (0) antes de autorizar a jogada
        if (modelo.getTabuleiro()[0][colunaSelecionada] == 0) {
            
            if (boxErro != null) boxErro.setVisible(false);
            if (labelAvisoColuna != null) labelAvisoColuna.setVisible(false);

            // Bloqueio imediato para evitar sobreposições em cliques rápidos consecutivos
            this.meuTurnoDeRede = false;

            if (gerenteRede != null) {
                gerenteRede.enviarComando("JOGADA:" + colunaSelecionada);
            }
            
            processarJogada(colunaSelecionada);
        } else {
            System.out.println("Coluna cheia! Tente outra.");
            if (labelAvisoColuna != null) {
                labelAvisoColuna.setText("A Coluna " + (colunaSelecionada + 1) + " está Cheia!\nEscolha outra.");
                labelAvisoColuna.setVisible(true);
            }
        }
    }

    /**
     * Insere a jogada na matriz lógica e gera a Timeline responsável pela queda suave da peça.
     */
    private void processarJogada(int colunaSelecionada) {
        final int turnoDaJogada = this.turnoAtual;
        int linhaOndeCaiu = modelo.inserirPeca(colunaSelecionada, turnoDaJogada);
        
        if (linhaOndeCaiu != -1) { 
            if (boxErro != null) boxErro.setVisible(false);
            if (labelAvisoColuna != null) labelAvisoColuna.setVisible(false);

            // Inicia os parâmetros da animação de transição descendente
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
                currentY[0] += 25; // Velocidade do vetor de deslocação vertical de queda
                
                if (currentY[0] >= endY) { 
                    // Fim do curso de animação da peça
                    currentY[0] = endY;
                    timeline.stop();
                    
                    animando = false;
                    animColuna = -1;
                    animLinha = -1;
                    desenharTabuleiro(); // Redesenha a tela consolidando a nova peça
                    
                    atualizarEstatisticas(colunaSelecionada, linhaOndeCaiu, turnoDaJogada);
                    
                    // Avalia se o estado atual resultou numa vitória
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
                    // Avalia se o estado atual resultou no preenchimento total (Empate)
                    else if (modelo.verificarEmpate()) {
                        jogoTerminado = true;
                        vboxEstatisticas.setVisible(false);
                        vboxVitoria.setVisible(true);
                        
                        labelVitoriaSubtitulo.setText("Empate!");
                        labelVitoriaSubtitulo.setTextFill(Color.web("#ffffff")); 
                        desenharTabuleiro();
                    } 
                    // Sem condições de término, o turno é alternado
                    else {
                        mudarTurnoVisual(); 
                    }
                    
                } else {
                    // Frame intermédio: Limpa e desenha o círculo a meio do percurso descendente
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
    
    /**
     * Atualiza o realce de previsualização (hover) ao mover o ponteiro do rato pelas colunas.
     */
    @FXML
    public void canvasMoved(MouseEvent e) {
        if (jogoTerminado || animando || !meuTurnoDeRede || jogoPausado) return; 
        
        double larguraColuna = canvas.getWidth() / 7;
        int colunaAtual = (int) (e.getX() / larguraColuna);
        
        if (colunaAtual != colunaHover) {
            colunaHover = colunaAtual;
            desenharTabuleiro();
        }
    }

    /**
     * Reseta e apaga a previsualização da coluna ativa quando o ponteiro sai da área geométrica do Canvas.
     */
    @FXML
    public void canvasExited(MouseEvent e) {
        colunaHover = -1;
        desenharTabuleiro();
    }
    
    /**
     * Motor de Renderização Gráfica do Tabuleiro.
     * reconstrói a matriz, pinta as peças correspondentes e destaca o vetor vencedor.
     */
    private void desenharTabuleiro() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double alturaTopo = 60.0; 
        double larguraColuna = canvas.getWidth() / 7;
        double alturaRestante = canvas.getHeight() - alturaTopo;
        double alturaLinha = alturaRestante / 6;
        double raio = Math.min(larguraColuna, alturaLinha) / 2.5; 

        // Renderização da camada de fundo azul escura do tabuleiro inferior
        gc.setFill(Color.web("#083c54"));
        gc.fillRect(0, alturaTopo, canvas.getWidth(), canvas.getHeight() - alturaTopo);

        // Renderização da barra plana de seleção de topo
        gc.setFill(Color.web("#5a8ca0")); 
        gc.fillRect(0, 0, canvas.getWidth(), alturaTopo);

        gc.setFont(Font.font("System", FontWeight.BOLD, 22));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        int[][] estadoAtual = modelo.getTabuleiro();

        // Desenho da barra interativa superior (Círculos indicadores numerados de 1 a 7)
        for (int i = 0; i < 7; i++) {
            double centroX = (i * larguraColuna) + (larguraColuna / 2);
            double centroY = alturaTopo / 2;

            if (estadoAtual[0][i] != 0) {
                gc.setFill(Color.web("#455a64")); // Bloqueado (Coluna cheia)
            } else if (i == colunaHover && !jogoTerminado && !animando && meuTurnoDeRede) {
                gc.setFill(turnoAtual == 1 ? Color.web("#ffc107") : Color.web("#e53935"));
            } else {
                gc.setFill(Color.web("#9abccc")); 
            }
            
            gc.fillOval(centroX - raio, centroY - raio, raio * 2, raio * 2);
            gc.setFill(Color.web("#083c54"));
            gc.fillText(String.valueOf(i + 1), centroX, centroY); 
        }

        // Renderização iterativa das ranhuras circulares do grelhado principal
        for (int linha = 0; linha < 6; linha++) {
            for (int coluna = 0; coluna < 7; coluna++) {
                double centroX = (coluna * larguraColuna) + (larguraColuna / 2);
                double centroY = alturaTopo + (linha * alturaLinha) + (alturaLinha / 2);
                
                double x = centroX - raio;
                double y = centroY - raio;

                int valor = estadoAtual[linha][coluna];
                
                // Omite a peça que está em trânsito de animação para não aparecer duplicada
                if (linha == animLinha && coluna == animColuna) {
                    valor = 0;
                }

                if (valor == 1) {
                    gc.setFill(Color.web("#ffc107")); // Amarelo
                } else if (valor == 2) {
                    gc.setFill(Color.web("#e53935")); // Vermelho
                } else {
                    gc.setFill(Color.web("#9abccc")); // Ranhura Vazia
                }
                
                gc.fillOval(x, y, raio * 2, raio * 2);
                
                // Caso exista vitória, aplica um contorno branco espesso nas 4 peças vencedoras
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
    
    /**
     * Atualiza as métricas de pontuação, as etiquetas textuais e formata a última coordenada válida.
     */
    private void atualizarEstatisticas(int col, int lin, int turnoDaJogada) {
        if (turnoDaJogada == 1) {
            pecasEu++;
            labelPecasEu.setText(nomeJogador1 + " - " + pecasEu);
        } else {
            pecasAdversario++;
            labelPecasAdversario.setText(nomeJogador2 + " - " + pecasAdversario);
        }
        
        // Converte a linha de indexação 0-5 para uma escala visual percetível de 1 a 6 de baixo para cima
        int linhaVisual = 6 - lin;
        labelUltimaJogada.setText("Col. " + (col + 1) + ", L. " + linhaVisual);
        labelUltimaJogada.setTextFill(turnoDaJogada == 1 ? Color.web("#ffc107") : Color.web("#e53935"));
    }

    /**
     * Aplica as modificações estéticas e as bordas coloridas neon para indicar de quem é a vez atual.
     */
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
    
    /**
     * Ação disparada pelo painel de vitória para iniciar uma nova partida entre os mesmos participantes.
     */
    @FXML
    public void acaoJogarDeNovo(ActionEvent event) {
        if (gerenteRede != null) {
            gerenteRede.enviarComando("RESTART");
        }
        reiniciarJogoLocal();
    }

    /**
     * Desliga as comunicações e termina o processo da aplicação.
     */
    @FXML
    public void acaoSair(ActionEvent event) {
        if (gerenteRede != null) {
            gerenteRede.fecharConexao();
        }
        Platform.exit();
        System.exit(0);
    }
    
    /**
     * Limpa de forma holística todas as variáveis e repõe o estado inicial limpo do ecrã de jogo.
     */
    private void reiniciarJogoLocal() {
        modelo.reiniciarJogo();
        jogoTerminado = false;
        pecasEu = 0;
        pecasAdversario = 0;
        turnoAtual = 1; 

        // Restabelece o direito de jogada inicial àquele que possuía o privilégio original
        this.meuTurnoDeRede = this.euComecoOJogo;

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
        if (vboxPausa != null) vboxPausa.setVisible(false);
        jogoPausado = false;
        
        if (boxErro != null) boxErro.setVisible(false);
        if (labelAvisoColuna != null) labelAvisoColuna.setVisible(false);

        desenharTabuleiro();
    }
    
    /**
     * Evento intercetado da rede a instruir a reativação e limpeza local do jogo.
     */
    public void receberRestartRemoto() {
        Platform.runLater(() -> {
            reiniciarJogoLocal();
        });
    }

    

@FXML
public void acaoVoltarMenu(ActionEvent event) {

    // 1. Impedir chamadas repetidas
    if (aRegressarAoMenu) return;
    aRegressarAoMenu = true;

    System.out.println("A fechar o jogo e a regressar ao Menu Inicial...");

    // 2. Fechar rede
    try {
        if (gerenteRede != null) {
            gerenteRede.fecharConexao();
        }
    } catch (Exception e) {
        System.out.println("Erro ao fechar rede: " + e.getMessage());
    }
    gerenteRede = null;

    // 3. Carregar manualmente o FXML e trocar o root da cena atual
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
        Parent root = loader.load();
        btnHome.getScene().setRoot(root);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    /**
     * Alterna o estado de pausa do jogo, ocultando as estatísticas e mostrando o menu correspondente.
     */
    @FXML
    public void acaoAlternarPausa(ActionEvent event) {
        jogoPausado = !jogoPausado;
        if (jogoPausado) {
            vboxEstatisticas.setVisible(false);
            vboxPausa.setVisible(true);
        } else {
            vboxPausa.setVisible(false);
            vboxEstatisticas.setVisible(true);
        }
    }
}