package com.emlinha;

import java.io.*;
import java.net.*;
import javafx.application.Platform;

public class GerenteRede {

    private Socket socket;
    private PrintWriter saida;
    private BufferedReader entrada;
    private JanelaController janelaController;
    private boolean ativo = true;

    // Construtor liga o gerente ao controlador do tabuleiro
    public GerenteRede(JanelaController controller) {
        this.janelaController = controller;
    }

    /**
     * TAREFA: Criar lógica de Servidor (Host)
     */
    public void iniciarServidor(int porta) {
        new Thread(() -> {
            try (ServerSocket servidor = new ServerSocket(porta)) {
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
     * TAREFA: Criar lógica de Cliente (Join)
     */
    public void conectarAoServidor(String ip, int porta) {
        new Thread(() -> {
            try {
                System.out.println("A tentar conectar a " + ip + ":" + porta);
                socket = new Socket(ip, porta);
                System.out.println("Conectado ao Host com sucesso!");
                
                inicializarCanais();
                escutarRede();
            } catch (IOException e) {
                System.err.println("Erro ao conectar ao Cliente: " + e.getMessage());
            }
        }).start();
    }

    private void inicializarCanais() throws IOException {
        saida = new PrintWriter(socket.getOutputStream(), true);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * TAREFA: Implementar Threads de escuta (Protocolo de Mensagens)
     */
    private void escutarRede() {
        try {
            String linha;
            while (ativo && (linha = entrada.readLine()) != null) {
                System.out.println("Mensagem recebida da rede: " + linha);
                
                final String mensagem = linha;
                
                // TAREFA: Integração com o Controller (Obrigatoriamente via Platform.runLater)
                if (mensagem.startsWith("JOGADA:")) {
                    int coluna = Integer.parseInt(mensagem.split(":")[1]);
                    
                    Platform.runLater(() -> {
                        // Faz a jogada recebida acontecer visualmente no ecrã do adversário
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
        if (saida != null) {
            saida.println(comando);
        }
    }

    public void fecharConexao() {
        ativo = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
