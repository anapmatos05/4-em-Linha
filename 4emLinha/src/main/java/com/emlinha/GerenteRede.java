package com.emlinha;

import java.io.*;
import java.net.*;
import javafx.application.Platform;

public class GerenteRede {

    private Socket socket;
    private PrintWriter saida;
    private BufferedReader entrada;
    private JanelaController janelaController;
    private ServerSocket servidor; 
    private boolean ativo = true;

    // Construtor liga o gerente ao controlador do tabuleiro
    public GerenteRede(JanelaController controller) {
        this.janelaController = controller;
    }

    /**
     * Lógica de Servidor (Host) - Envia o seu nome após a conexão
     */
    public void iniciarServidor(int porta) {
        new Thread(() -> {
            try {
                servidor = new ServerSocket(porta);
                System.out.println("Servidor à espera na porta " + porta);
                
                // Aguarda o cliente conectar
                socket = servidor.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress().getHostAddress());
                
                inicializarCanais();
                
                // PROTOCOLO: O Host envia o seu nome real para o Cliente imediatamente
                enviarComando("NOME:" + janelaController.getNomeJogador1());
                
                escutarRede();
            } catch (IOException e) {
                System.err.println("Erro no Servidor: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Lógica de Cliente (Join) - Envia o seu nome após a conexão
     */
    public void conectarAoServidor(String ip, int porta) {
        new Thread(() -> {
            try {
                System.out.println("A tentar conectar a " + ip + ":" + porta);
                socket = new Socket(ip, porta);
                System.out.println("Conectado ao Host com sucesso!");
                
                inicializarCanais();
                
                // PROTOCOLO: O Cliente envia o seu nome real para o Host imediatamente
                enviarComando("NOME:" + janelaController.getNomeJogador2());
                
                escutarRede();
            } catch (IOException e) {
                System.err.println("Erro ao conectar ao Servidor: " + e.getMessage());
            }
        }).start();
    }

    private void inicializarCanais() throws IOException {
        saida = new PrintWriter(socket.getOutputStream(), true);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // NOVO: A ligação foi estabelecida com sucesso! 
        // Agora sim, podemos dizer o nosso nome ao outro computador.
        Platform.runLater(() -> {
            if (janelaController != null) {
                janelaController.enviarMeuNome();
            }
        });
    }

    /**
     * Implementação da escuta ativa (Trata Jogadas e Nomes reais)
     */
    private void escutarRede() {
        try {
            String linha;
            while (ativo && (linha = entrada.readLine()) != null) {
                System.out.println("Mensagem recebida da rede: " + linha);
                
                String mensagem = linha;
                
                // Trata a receção do nome do adversário
                if (mensagem.startsWith("NOME:")) {
                    String nomeRecebido = mensagem.split(":")[1];
                    Platform.runLater(() -> {
                        janelaController.atualizarNomeAdversario(nomeRecebido);
                    });
                }
                
                if (mensagem.startsWith("JOGADA:")) {
                    try {
                        int coluna = Integer.parseInt(mensagem.split(":")[1]);
                        Platform.runLater(() -> {
                            janelaController.receberJogadaRemota(coluna);
                        });
                    } catch (Exception ex) {
                        System.out.println("Erro ao ler jogada.");
                    }
                } 
                else if (mensagem.startsWith("NOME:")) {
                    String nomeAdversario = mensagem.substring(5);
                    janelaController.receberNomeAdversarioRemoto(nomeAdversario);
                } 
                else if (mensagem.equals("RESTART")) {
                    janelaController.receberRestartRemoto();
                }
            }
        } catch (IOException e) {
            System.out.println("Conexão de rede encerrada.");
        }
    }

    /**
     * Envia comandos para o outro computador numa Thread assíncrona rápida
     */
    public void enviarComando(String comando) {
        new Thread(() -> { 
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