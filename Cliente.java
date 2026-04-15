
/**
 * ============================================================
 *   CLIENTE DO SERVIDOR DE CÁLCULO DISTRIBUÍDO
 *   Disciplina: Redes de Computadores / Sistemas Distribuídos
 * ============================================================
 *
 * Descrição:
 *     Cliente TCP interativo que se conecta ao servidor,
 *     envia operações matemáticas e exibe os resultados.
 *
 * Uso:
 *     javac Cliente.java
 *     java Cliente
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {

    // ─────────────────────────────────────────────
    // CONFIGURAÇÕES DE CONEXÃO
    // ─────────────────────────────────────────────
    private static String HOST = "192.168.100.2"; // IP do servidor
    private static final int PORTA = 65432; // Porta do servidor

    // ─────────────────────────────────────────────
    // MÉTODO PRINCIPAL
    // ─────────────────────────────────────────────
    public static void main(String[] args) {

        System.out.println("==================================================");
        System.out.println("  CLIENTE DE CALCULO DISTRIBUIDO");
        System.out.println("==================================================");
        System.out.println("  Conectando em " + HOST + ":" + PORTA + "...");

        try (
                // Cria o socket e conecta ao servidor
                Socket socket = new Socket(HOST, PORTA);

                // PrintWriter envia texto ao servidor (autoFlush = true)
                PrintWriter saida = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                // BufferedReader lê a resposta do servidor
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), "UTF-8"));

                // Scanner lê o que o usuário digita no terminal
                Scanner teclado = new Scanner(System.in)) {
            System.out.println("  Conectado com sucesso!");
            System.out.println("==================================================");
            System.out.println("  Digite uma operacao matematica e pressione Enter.");
            System.out.println("  Exemplos: 2 + 3 | 10 / 2 | 5 * 8 | 9 - 4");
            System.out.println("  Digite 'sair' para encerrar.");
            System.out.println("==================================================");

            while (true) {
                System.out.print("\n  Operacao: ");
                String operacao = teclado.nextLine().trim();

                // Encerra se o usuário digitar sair
                if (operacao.equalsIgnoreCase("sair") ||
                        operacao.equalsIgnoreCase("exit")) {
                    System.out.println("  Encerrando conexao...");
                    break;
                }

                // Ignora linha vazia
                if (operacao.isEmpty()) {
                    System.out.println("  [!] Digite uma operacao valida.");
                    continue;
                }

                // Envia a operação ao servidor
                saida.println(operacao);

                // Recebe e exibe o resultado
                String resultado = entrada.readLine();

                if (resultado == null) {
                    System.out.println("  [!] Conexao com o servidor perdida.");
                    break;
                }

                System.out.println("  Resultado: " + resultado);
            }

        } catch (ConnectException e) {
            System.out.println("  [ERRO] Nao foi possivel conectar em " + HOST + ":" + PORTA);
            System.out.println("  Verifique se o servidor esta em execucao.");
        } catch (IOException e) {
            System.out.println("  [ERRO] Falha na conexao: " + e.getMessage());
        }

        System.out.println("  Conexao encerrada. Ate mais!");
    }
}
