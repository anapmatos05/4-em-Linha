package com.emlinha;

import java.io.*;
import java.net.*;
import javafx.application.Platform;

public class GerenteRede {

    private Socket socket;
    private PrintWriter saida;
    private BufferedReader entrada;
    private JanelaController janelaController;
    private ServerSocket servidor; // Movido para variável de instância para fechar corretamente no fim
    private boolean ativo = true;

    // Construtor liga o gerente ao controlador do tabuleiro
    public GerenteRede(JanelaController controller) {
        this.janelaController = controller;
    }

    /**
     * Lógica de Servidor (Host) - CORRIGIDA: Sem try-with-resources para não matar a conexão
     */
    public void iniciarServidor(int porta) {
        new Thread(() -> {
            try {
                servidor = new ServerSocket(porta);
                System.out.println("Servidor à espera na porta " + porta);
                
                // Aguarda o cliente conectar (Bloqueia a thread secundária, mas não o JavaFX)
                socket = servidor.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress().getHostAddress());
                
                inicializarCanais();
                escutarRede();
            } catch (IOException e) {
                System.err.println("Erro no Servidor: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Lógica de Cliente (Join)
     */
    public void conectarAoServidor(String ip, int porta) {
        // CORREÇÃO: No modo Cliente, precisamos que a conexão aconteça na Thread principal 
        // ANTES de mudar de ecrã, para garantir que o Socket está pronto quando o tabuleiro abrir!
        try {
            System.out.println("A tentar conectar a " + ip + ":" + porta);
            socket = new Socket(ip, porta);
            System.out.println("Conectado ao Host com sucesso!");
            
            inicializarCanais();
            
            // A escuta sim, roda numa thread secundária para não travar o jogo
            new Thread(this::escutarRede).start();
        } catch (IOException e) {
            System.err.println("Erro ao conectar ao Servidor: " + e.getMessage());
            // Lança uma exceção para o PrimaryController saber que falhou e não mudar de ecrã
            throw new RuntimeException(e);
        }
    }

    private void inicializarCanais() throws IOException {
        saida = new PrintWriter(socket.getOutputStream(), true);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Implementar Threads de escuta (Protocolo de Mensagens)
     */
    private void escutarRede() {
        try {
            String linha;
            while (ativo && (linha = entrada.readLine()) != null) {
                System.out.println("Mensagem recebida da rede: " + linha);
                
                String mensagem = linha;
                
                if (mensagem.startsWith("JOGADA:")) {
                    int coluna = Integer.parseInt(mensagem.split(":")[1]);
                    
                    // CORREÇÃO: Deixamos o Platform.runLater apenas aqui para disparar o visual em segurança
                    Platform.runLater(() -> {
                        janelaController.receberJogadaRemota(coluna);
                    });
                }
            }
        } catch (IOException e) {
            System.out.println("Conexão de rede encerrada.");
        }
    }

    /**
     * Envia comandos para o outro computador
     */
    public void enviarComando(String comando) {
        new Thread(() -> { // Executa o envio numa Thread rápida para o clique do rato ser instantâneo
            if (saida != null) {
                saida.println(comando);
            }
        }).start();
    }

    public void fecharConexao() {
        ativo = false;
        try {
            if (socket != null) socket.close();
            if (servidor != null) servidor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}