package com.emlinha;

public class JogoModelo {
    private int[][] tabuleiro;
    private int[][] pecasVitoriosas; // Guarda as coordenadas [linha, coluna] das 4 peças

    public JogoModelo() {
        tabuleiro = new int[6][7];
    }

    public int[][] getTabuleiro() { return tabuleiro; }
    public int[][] getPecasVitoriosas() { return pecasVitoriosas; }

    public int inserirPeca(int coluna, int jogador) {
        for (int linha = 5; linha >= 0; linha--) {
            if (tabuleiro[linha][coluna] == 0) {
                tabuleiro[linha][coluna] = jogador;
                return linha;
            }
        }
        return -1;
    }

    public boolean verificarVitoria(int jogador) {
        // Horizontal
        for (int linha = 0; linha < 6; linha++) {
            for (int col = 0; col < 4; col++) {
                if (tabuleiro[linha][col] == jogador && tabuleiro[linha][col+1] == jogador &&
                    tabuleiro[linha][col+2] == jogador && tabuleiro[linha][col+3] == jogador) {
                    pecasVitoriosas = new int[][] {{linha, col}, {linha, col+1}, {linha, col+2}, {linha, col+3}};
                    return true;
                }
            }
        }
        // Vertical
        for (int linha = 0; linha < 3; linha++) {
            for (int col = 0; col < 7; col++) {
                if (tabuleiro[linha][col] == jogador && tabuleiro[linha+1][col] == jogador &&
                    tabuleiro[linha+2][col] == jogador && tabuleiro[linha+3][col] == jogador) {
                    pecasVitoriosas = new int[][] {{linha, col}, {linha+1, col}, {linha+2, col}, {linha+3, col}};
                    return true;
                }
            }
        }
        // Diagonal \
        for (int linha = 0; linha < 3; linha++) {
            for (int col = 0; col < 4; col++) {
                if (tabuleiro[linha][col] == jogador && tabuleiro[linha+1][col+1] == jogador &&
                    tabuleiro[linha+2][col+2] == jogador && tabuleiro[linha+3][col+3] == jogador) {
                    pecasVitoriosas = new int[][] {{linha, col}, {linha+1, col+1}, {linha+2, col+2}, {linha+3, col+3}};
                    return true;
                }
            }
        }
        // Diagonal /
        for (int linha = 3; linha < 6; linha++) {
            for (int col = 0; col < 4; col++) {
                if (tabuleiro[linha][col] == jogador && tabuleiro[linha-1][col+1] == jogador &&
                    tabuleiro[linha-2][col+2] == jogador && tabuleiro[linha-3][col+3] == jogador) {
                    pecasVitoriosas = new int[][] {{linha, col}, {linha-1, col+1}, {linha-2, col+2}, {linha-3, col+3}};
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Verifica se o tabuleiro está completamente cheio (Empate).
     * @return true se for empate, false se ainda houver espaço.
     */
    public boolean verificarEmpate() {
        int[][] tabuleiro = getTabuleiro();
        
        // Basta verificar a linha do topo (índice 0) de todas as colunas
        for (int coluna = 0; coluna < 7; coluna++) {
            if (tabuleiro[0][coluna] == 0) {
                return false; // Encontrou um espaço vazio, o jogo pode continuar
            }
        }
        return true; // Não há mais espaços na linha do topo
    }

    // Método para recomeçar o jogo
    public void reiniciarJogo() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                tabuleiro[i][j] = 0;
            }
        }
        pecasVitoriosas = null;
    }
}