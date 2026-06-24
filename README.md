# 4 em Linha — Jogo Multijogador em Rede

Este repositório contém o projeto prático desenvolvido para a unidade curricular de **Laboratório de Programação** do curso de **Engenharia da Computação Gráfica e Multimédia (ECGM)** no **Instituto Politécnico de Viana do Castelo (IPVC)**.

O objetivo do projeto foi conceber e programar uma versão digital e moderna do clássico jogo **4 em Linha (Connect Four)**, suportando partidas locais e remotas (via rede), aplicando os conceitos de Programação Orientada a Objetos, Interfaces Gráficas baseadas em Vetores/Canvas, Concorrência (Threads) e Comunicação em Rede (Sockets).

---

## Autores (Grupo 02)
* **Ana Matos** (33138)
* **Ulysse Cancela** (33136)
* **Docente:** Luís Romero
* **Ano Letivo:** 2025/2026

---

## Arquitetura e Estrutura do Sistema

O projeto segue uma arquitetura modular inspirada no padrão MVC (Model-View-Controller), garantindo a separação clara entre a lógica do jogo, a interface e a comunicação.

###  Módulos Principais
* **`App`**: O ponto de entrada da aplicação que inicializa o ciclo de vida do JavaFX e carrega o ecrã inicial.
* **`JogoModelo` (Lógica):** Core do jogo. Controla a matriz do tabuleiro ($6 \times 7$), valida as jogadas, deteta as sequências vitoriosas (horizontais, verticais e diagonais) e gere a alternância de turnos.
* **`PrimaryController` (Menu Inicial):** Responsável pela configuração do jogo, recolha dos nomes dos jogadores, e estabelecimento dos parâmetros de rede (IP e Porta).
* **`JanelaController` (Interface e Jogo):** Controlador do ecrã principal da partida. Utiliza um `Canvas` JavaFX para renderizar dinamicamente o tabuleiro, animações de queda das peças e efeitos visuais.
* **`GerenteRede` (Comunicação):** Gere o ciclo de vida das ligações TCP. Executa threads secundárias para ouvir a rede continuamente sem bloquear a interface de utilizador.

---

##  Funcionalidades em Destaque

###  1. Modo Multijogador em Rede (Sockets TCP)
O jogo adota um modelo descentralizado de **Anfitrião (Host) / Convidado (Cliente)**:
* **Anfitrião:** Abre um `ServerSocket` na porta escolhida e aguarda conexões. Joga com as peças amarelas e tem o primeiro turno.
* **Convidado:** Conecta-se ao IP e Porta do Anfitrião usando um `Socket`. Joga com as peças vermelhas.
* **Protocolo de Comunicação:** Troca de mensagens de texto simples estruturadas em tempo real:
  * `NOME:[texto]` -> Envio do nome do jogador para identificação.
  * `JOGADA:[0-6]` -> Envio do índice da coluna onde a peça foi largada.

###  2. Multi-Threading com JavaFX
A escuta de dados da rede corre em background numa **Thread secundária** controlada pelo `GerenteRede`. Sempre que uma jogada remota é recebida, o método `Platform.runLater()` é invocado para atualizar o tabuleiro e a interface gráfica na **JavaFX Application Thread** de forma segura, prevenindo o congelamento da janela.

###  3. Sistema de Persistência (Save/Load)
O jogo possui a capacidade de gravar e carregar o estado atual da partida local através de manipulação de ficheiros de texto (`save_4emlinha.txt`), guardando a disposição das peças na matriz, o turno atual e as configurações dos jogadores para continuar a partida mais tarde.

---

##  Como Abrir e Executar o Projeto

Como o projeto está configurado via **Apache Maven**, o NetBeans e o Scene Builder conseguem geri-lo de forma totalmente visual, sem necessidade de comandos de consola.

### Pré-requisitos
* **NetBeans IDE** (versão com suporte para Java 21 ou superior).
* **Scene Builder** instalado e integrado no NetBeans.

### 1. Abrir o Projeto no NetBeans
1. Inicia o **NetBeans**.
2. No menu superior, clica em **File** -> **Open Project...**
3. Navega até à pasta onde clonaste/descompactaste este repositório.
4. O NetBeans irá reconhecer automaticamente o ícone de projeto Maven (um triângulo amarelo/maqueta). Seleciona-o e clica em **Open Project**.
5. *Nota: Na primeira vez, o Maven poderá descarregar as dependências do JavaFX automaticamente em background.*

### 2. Executar a Aplicação
* Para correr o jogo, basta clicares no botão **Play (Seta Verde)** na barra de ferramentas superior do NetBeans, ou clicar com o botão direito sobre o nome do projeto na barra lateral esquerda e selecionar **Run**.
* O NetBeans usará a configuração contida no ficheiro `nbactions.xml` para compilar e abrir a interface gráfica instantaneamente.

### 3. Editar a Interface Gráfica (Scene Builder)
A interface gráfica foi desenhada usando ficheiros FXML. Para fazer alterações visuais:
1. Dentro do NetBeans, expande a árvore do projeto até: `Source Packages` -> `com.emlinha`.
2. Procura os ficheiros com extensão `.fxml` (ex: `primary.fxml` ou `secondary.fxml`).
3. Clica com o **botão direito do rato** sobre o ficheiro FXML e escolhe **Open**.
4. O ficheiro abrirá diretamente no **Scene Builder**, permitindo arrastar novos componentes, alterar estilos CSS ou associar novos eventos ao Controller. Ao gravar no Scene Builder, o NetBeans atualiza o código na hora.

---

## 📂 Estrutura de Ficheiros Relevantes

```text
4emLinha/
├── src/
│   └── main/
│       ├── java/com/emlinha/
│       │   ├── App.java                  # Ponto de entrada (Main)
│       │   ├── JogoModelo.java           # Lógica do Tabuleiro e Regras
│       │   ├── GerenteRede.java          # Threads e Sockets TCP
│       │   ├── PrimaryController.java    # Controlo do Menu Inicial
│       │   └── JanelaController.java     # Controlo do Jogo e Canvas
│       └── resources/com/emlinha/
│           ├── primary.fxml              # Design do Menu Inicial
│           └── secondary.fxml            # Design do Ecrã de Jogo
├── pom.xml                               # Configurações do Maven e JavaFX
├── nbactions.xml                         # Ações de execução integradas no NetBeans
└── save_4emlinha.txt                     # Ficheiro de gravação do estado do jogo
