import java.io.*;
import java.net.*;

public class Servidor {

    private static final int PORTA = 65432;
    private static final int MAX_CONEXOES = 50;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  SERVIDOR DE CALCULOS DISTRIBUIDOS (SPD)");
        System.out.println("  Status: ONLINE | Porta: " + PORTA);
        System.out.println("  Monitoramento: Thread-ID e Nanosegundos");
        System.out.println("==================================================");

        try (ServerSocket serverSocket = new ServerSocket(PORTA, MAX_CONEXOES)) {
            while (true) {
                Socket socketCliente = serverSocket.accept();
                
                // Cria uma nova Thread dedicada para cada cliente
                Thread thread = new Thread(new TratadorCliente(socketCliente));
                thread.setDaemon(true);
                thread.start();
                
                System.out.println("[MONITOR] Novo Fluxo Conectado. Threads Ativas: " + Thread.activeCount());
            }
        } catch (IOException e) {
            System.err.println("[EXCEÇÃO NO SERVIDOR] " + e.getMessage());
        }
    }

    // Classe interna que lida com o processamento paralelo
    private static class TratadorCliente implements Runnable {
        private final Socket socket;
        private final String correlationId;

        public TratadorCliente(Socket socket) {
            this.socket = socket;
            this.correlationId = "REQ-" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }

        @Override
        public void run() {
            log("CONECTADO (Início do Processamento)");
            try (
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter saida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
            ) {
                String comando;
                while ((comando = entrada.readLine()) != null) {
                    String resultado = realizarCalculo(comando);
                    log("CÁLCULO CONCLUÍDO: " + comando + " -> " + resultado);
                    saida.println(resultado);
                }
            } catch (Exception e) {
                log("TERMINADO COM EXCEÇÃO: " + e.getMessage());
            } finally {
                log("DESCONECTADO (Fim do Fluxo)");
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private String realizarCalculo(String expressao) {
            expressao = expressao.trim();
            String padrao = "^(-?\\d+(\\.\\d+)?)\\s*([+\\-*/])\\s*(-?\\d+(\\.\\d+)?)$";

            if (!expressao.matches(padrao)) {
                return "ERRO: Formato inválido. Use: num op num";
            }

            String[] partes = expressao.split("\\s*([+\\-*/])\\s*");
            char operador = 0;
            for (char ch : expressao.toCharArray()) {
                if (ch == '+' || ch == '-' || ch == '*' || ch == '/') {
                    operador = ch;
                    break;
                }
            }

            try {
                double n1 = Double.parseDouble(partes[0].trim());
                double n2 = Double.parseDouble(partes[1].trim());
                double res = 0;

                switch (operador) {
                    case '+': res = n1 + n2; break;
                    case '-': res = n1 - n2; break;
                    case '*': res = n1 * n2; break;
                    case '/': 
                        if (n2 == 0) return "ERRO: Divisão por zero";
                        res = n1 / n2; 
                        break;
                }
                
                // Simulação de delay para evidenciar o paralelismo nos logs
                Thread.sleep(150); 

                if (res == (long) res) return String.valueOf((long) res);
                return String.format("%.4f", res);
            } catch (Exception e) {
                return "ERRO: Falha Numérica";
            }
        }

        private void log(String msg) {
            long ts = System.nanoTime();
            Thread t = Thread.currentThread();
            System.out.printf("[%d ns] [%s (ID:%d)] [%s] %s%n", 
                ts, t.getName(), t.threadId(), correlationId, msg);
        }
    }
}
