/**
 * ============================================================
 *   TESTE DE MULTITHREADING — CLIENTES SIMULTÂNEOS
 *   Disciplina: Redes de Computadores / Sistemas Distribuídos
 * ============================================================
 *
 * Descrição:
 *     Cria vários clientes simultaneamente para provar que
 *     o servidor está atendendo múltiplas conexões em paralelo.
 *
 * Uso:
 *     1. Rode o servidor primeiro:  java Servidor
 *     2. Rode este teste:           java TesteMultithread
 */

import java.io.*;
import java.net.*;

public class TesteMultithread {

    // ─────────────────────────────────────────────
    //  CONFIGURAÇÕES
    // ─────────────────────────────────────────────
    private static final String HOST  = "127.0.0.1";
    private static final int    PORTA = 65432;

    // Operações que cada cliente vai enviar
    private static final String[] OPERACOES = {
        "2 + 3",
        "10 / 2",
        "5 * 8",
        "9 - 4",
        "100 / 4",
        "7 * 7",
        "50 - 25",
        "3 + 3"
    };


    // ─────────────────────────────────────────────
    //  CLIENTE SIMULADO (roda em thread separada)
    // ─────────────────────────────────────────────
    static class ClienteSimulado implements Runnable {

        private final int    idCliente;
        private final String operacao;

        public ClienteSimulado(int idCliente, String operacao) {
            this.idCliente = idCliente;
            this.operacao  = operacao;
        }

        @Override
        public void run() {
            try (
                Socket socket = new Socket(HOST, PORTA);
                PrintWriter saida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true
                );
                BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8")
                )
            ) {
                // Envia a operação
                saida.println(operacao);

                // Recebe o resultado
                String resultado = entrada.readLine();

                // Exibe (synchronized para não misturar prints)
                synchronized (System.out) {
                    System.out.printf("  Cliente %02d | Enviou: %-10s | Resultado: %s%n",
                        idCliente, operacao, resultado);
                }

            } catch (ConnectException e) {
                synchronized (System.out) {
                    System.out.println("  [ERRO] Cliente " + idCliente
                        + " — servidor nao encontrado. Rode java Servidor primeiro.");
                }
            } catch (IOException e) {
                synchronized (System.out) {
                    System.out.println("  [ERRO] Cliente " + idCliente + ": " + e.getMessage());
                }
            }
        }
    }


    // ─────────────────────────────────────────────
    //  MÉTODO PRINCIPAL DO TESTE
    // ─────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {

        int totalClientes = OPERACOES.length;

        System.out.println("=======================================================");
        System.out.println("  TESTE DE MULTITHREADING — CLIENTES SIMULTANEOS");
        System.out.println("=======================================================");
        System.out.println("  Servidor  : " + HOST + ":" + PORTA);
        System.out.println("  Clientes  : " + totalClientes + " conectando ao mesmo tempo");
        System.out.println("=======================================================");
        System.out.println();
        System.out.println("  [INICIANDO] Disparando todos os clientes simultaneamente...");
        System.out.println();

        // Cria todas as threads
        Thread[] threads = new Thread[totalClientes];
        for (int i = 0; i < totalClientes; i++) {
            threads[i] = new Thread(new ClienteSimulado(i + 1, OPERACOES[i]));
        }

        long inicio = System.currentTimeMillis();

        // Inicia TODAS ao mesmo tempo
        for (Thread t : threads) {
            t.start();
        }

        // Aguarda TODAS terminarem
        for (Thread t : threads) {
            t.join();
        }

        long fim = System.currentTimeMillis();
        double tempoTotal = (fim - inicio) / 1000.0;

        System.out.println();
        System.out.println("=======================================================");
        System.out.println("  RESULTADO DO TESTE");
        System.out.println("=======================================================");
        System.out.printf("  Clientes atendidos : %d%n", totalClientes);
        System.out.printf("  Tempo total        : %.3f segundos%n", tempoTotal);
        System.out.println();
        System.out.println("  Se todos os clientes acima receberam resultado,");
        System.out.println("  o multithreading esta funcionando corretamente!");
        System.out.println("=======================================================");
    }
}
