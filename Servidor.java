/**
 * ============================================================
 *   SERVIDOR DE CÁLCULO DISTRIBUÍDO
 *   Disciplina: Redes de Computadores / Sistemas Distribuídos
 * ============================================================
 *
 * Descrição:
 *     Servidor TCP que aceita múltiplos clientes simultaneamente.
 *     Para cada cliente conectado, uma thread separada é criada
 *     para processar as operações matemáticas enviadas.
 *
 * Bibliotecas utilizadas:
 *     - java.net.ServerSocket  : aguarda conexões TCP
 *     - java.net.Socket        : representa a conexão com o cliente
 *     - java.lang.Thread       : multithreading
 *     - java.io.*              : leitura e escrita de dados
 *
 * Uso:
 *     javac Servidor.java
 *     java Servidor
 */

import java.io.*;
import java.net.*;

public class Servidor {

    // ─────────────────────────────────────────────
    //  CONFIGURAÇÕES DO SERVIDOR
    // ─────────────────────────────────────────────
    private static final int PORTA        = 65432;  // Porta TCP do servidor
    private static final int MAX_CONEXOES = 10;     // Fila de espera máxima


    // ─────────────────────────────────────────────
    //  PROCESSAMENTO DA OPERAÇÃO MATEMÁTICA
    // ─────────────────────────────────────────────
    /**
     * Recebe uma expressão matemática como String (ex: "10 / 2")
     * e retorna o resultado ou uma mensagem de erro.
     *
     * @param expressao texto enviado pelo cliente
     * @return resultado da operação ou mensagem de erro
     */
    public static String calcular(String expressao) {
        // Remove espaços extras das bordas
        expressao = expressao.trim();

        // Expressão regular: aceita inteiros e decimais com os 4 operadores
        String padrao = "^(-?\\d+(\\.\\d+)?)\\s*([+\\-*/])\\s*(-?\\d+(\\.\\d+)?)$";

        if (!expressao.matches(padrao)) {
            return "ERRO: Expressao invalida. Use o formato: numero operador numero (ex: 5 + 3)";
        }

        // Separa os componentes da expressão
        String[] partes = expressao.split("\\s*([+\\-*/])\\s*");

        // Extrai o operador procurando pelo caractere especial
        char operador = 0;
        for (char c : expressao.toCharArray()) {
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                operador = c;
                break;
            }
        }

        double numero1;
        double numero2;

        try {
            numero1 = Double.parseDouble(partes[0].trim());
            numero2 = Double.parseDouble(partes[1].trim());
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return "ERRO: Expressao invalida. Use o formato: numero operador numero (ex: 5 + 3)";
        }

        double resultado;

        switch (operador) {
            case '+': resultado = numero1 + numero2; break;
            case '-': resultado = numero1 - numero2; break;
            case '*': resultado = numero1 * numero2; break;
            case '/':
                if (numero2 == 0) {
                    return "ERRO: Divisao por zero nao e permitida.";
                }
                resultado = numero1 / numero2;
                break;
            default:
                return "ERRO: Operador desconhecido.";
        }

        // Remove o ".0" se o resultado for inteiro
        if (resultado == (long) resultado) {
            return String.valueOf((long) resultado);
        }
        return String.format("%.6g", resultado);
    }


    // ─────────────────────────────────────────────
    //  THREAD DE ATENDIMENTO DE CADA CLIENTE
    // ─────────────────────────────────────────────
    /**
     * Classe interna que representa a thread de um cliente.
     * Implementa Runnable para que possa ser executada por Thread.
     *
     * Cada instância desta classe cuida de UM cliente específico,
     * rodando em paralelo com as demais graças ao multithreading.
     */
    static class TratadorCliente implements Runnable {

        private final Socket socketCliente;

        public TratadorCliente(Socket socketCliente) {
            this.socketCliente = socketCliente;
        }

        @Override
        public void run() {
            String enderecoCliente = socketCliente.getInetAddress().getHostAddress()
                                   + ":" + socketCliente.getPort();

            System.out.println("[+] Cliente conectado: " + enderecoCliente);

            try (
                // BufferedReader lê texto linha a linha do cliente
                BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socketCliente.getInputStream(), "UTF-8")
                );
                // PrintWriter envia texto ao cliente
                PrintWriter saida = new PrintWriter(
                    new OutputStreamWriter(socketCliente.getOutputStream(), "UTF-8"), true
                )
            ) {
                String mensagem;

                // Fica em loop lendo mensagens até o cliente desconectar
                while ((mensagem = entrada.readLine()) != null) {
                    System.out.println("    [" + enderecoCliente + "] Recebeu: '" + mensagem + "'");

                    String resposta = calcular(mensagem);

                    System.out.println("    [" + enderecoCliente + "] Enviou:  '" + resposta + "'");

                    // Envia a resposta (println já adiciona \n que o cliente usa para saber o fim)
                    saida.println(resposta);
                }

                System.out.println("[-] Cliente desconectado: " + enderecoCliente);

            } catch (IOException e) {
                System.out.println("[!] Erro com cliente " + enderecoCliente + ": " + e.getMessage());
            } finally {
                try {
                    socketCliente.close();
                } catch (IOException e) {
                    // ignora erro ao fechar
                }
            }
        }
    }


    // ─────────────────────────────────────────────
    //  MÉTODO PRINCIPAL — INICIA O SERVIDOR
    // ─────────────────────────────────────────────
    public static void main(String[] args) {

        System.out.println("==================================================");
        System.out.println("  SERVIDOR DE CALCULO DISTRIBUIDO");
        System.out.println("==================================================");
        System.out.println("  Porta    : " + PORTA);
        System.out.println("  Aguardando conexoes... (Ctrl+C para encerrar)");
        System.out.println("==================================================");

        // Cria o ServerSocket que escuta na porta definida
        try (ServerSocket serverSocket = new ServerSocket(PORTA, MAX_CONEXOES)) {

            // Loop infinito: aceita clientes e cria threads
            while (true) {

                // accept() BLOQUEIA até um cliente conectar
                Socket socketCliente = serverSocket.accept();

                // Cria e inicia uma thread para este cliente
                // ─── AQUI ESTÁ O MULTITHREADING ───
                Thread thread = new Thread(new TratadorCliente(socketCliente));
                thread.setDaemon(true); // morre se o servidor encerrar
                thread.start();         // inicia em paralelo — não bloqueia o loop

                System.out.println("[*] Clientes ativos: " + Thread.activeCount());
            }

        } catch (IOException e) {
            System.out.println("[ERRO] Falha ao iniciar servidor: " + e.getMessage());
        }
    }
}
