import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class TesteMultithread {

    private static final String HOST = "127.0.0.1";
    private static final int PORTA = 65432;
    private static final int QUANTIDADE_CLIENTES = 15;
    private static final Random random = new Random();

    private static final AtomicIntegerArray progressoStatus = new AtomicIntegerArray(QUANTIDADE_CLIENTES);
    private static final String[] resultadosFinais = new String[QUANTIDADE_CLIENTES];
    private static final boolean[] finalizado = new boolean[QUANTIDADE_CLIENTES];
    
    // Matrizes para Log Detalhado
    private static final long[] startTimes = new long[QUANTIDADE_CLIENTES];
    private static final long[] endTimes = new long[QUANTIDADE_CLIENTES];
    private static final String[] clientInfo = new String[QUANTIDADE_CLIENTES];

    public static void main(String[] args) throws InterruptedException {
        System.out.println("\033[H\033[2J");
        System.out.println("======================================================================");
        System.out.println("          SISTEMA DE MONITORAMENTO DE FLUXO PARALELO (SPD)");
        System.out.println("======================================================================");
        System.out.println("  Orquestrando " + QUANTIDADE_CLIENTES + " fluxos de processamento simultâneos...");
        System.out.println("----------------------------------------------------------------------");

        Thread[] threads = new Thread[QUANTIDADE_CLIENTES];
        long tempoInicioGeral = System.nanoTime();

        for (int i = 0; i < QUANTIDADE_CLIENTES; i++) {
            final int id = i;
            threads[i] = new Thread(() -> executarFluxo(id));
            threads[i].start();
        }

        Thread uiThread = new Thread(() -> {
            while (!todosFinalizados()) {
                desenharDashboard();
                try { Thread.sleep(100); } catch (Exception e) {}
            }
            desenharDashboard();
        });
        uiThread.start();

        for (Thread t : threads) t.join();
        uiThread.join();

        long tempoFimGeral = System.nanoTime();
        double duracaoSegundos = (tempoFimGeral - tempoInicioGeral) / 1_000_000_000.0;
        
        gerarLogExtraDetalhado(duracaoSegundos);

        System.out.println("\n----------------------------------------------------------------------");
        System.out.printf("  PROCESSAMENTO CONCLUÍDO EM: %.4f segundos%n", duracaoSegundos);
        System.out.println("  LOG DETALHADO GERADO EM: 'log_execucao.txt'");
        System.out.println("======================================================================");
    }

    private static void executarFluxo(int id) {
        // Gera conta aleatória
        int n1 = random.nextInt(50) + 1;
        int n2 = random.nextInt(50) + 1;
        char[] ops = {'+', '-', '*', '/'};
        char op = ops[random.nextInt(ops.length)];
        String expressao = n1 + " " + op + " " + n2;
        
        startTimes[id] = System.nanoTime();
        
        try (
            Socket socket = new Socket(HOST, PORTA);
            PrintWriter saida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))
        ) {
            clientInfo[id] = socket.getLocalAddress() + ":" + socket.getLocalPort();
            
            for(int p=0; p<=70; p+=10) {
                progressoStatus.set(id, p);
                Thread.sleep(50 + (int)(Math.random() * 50));
            }

            saida.println(expressao);
            String resposta = entrada.readLine();
            endTimes[id] = System.nanoTime();
            
            progressoStatus.set(id, 100);
            resultadosFinais[id] = String.format("%s -> Resp: %s", expressao, resposta);
            
        } catch (Exception e) {
            progressoStatus.set(id, -1);
            resultadosFinais[id] = "ERRO: " + e.getMessage();
            endTimes[id] = System.nanoTime();
        } finally {
            synchronized (finalizado) { finalizado[id] = true; }
        }
    }

    private static synchronized void desenharDashboard() {
        System.out.print("\r"); 
        for (int i = 0; i < QUANTIDADE_CLIENTES; i++) {
            int p = progressoStatus.get(i);
            String barra = montarBarra(p);
            String res = (p == 100) ? resultadosFinais[i] : (p == -1) ? "FALHA" : "Em execução...";
            System.out.printf("  [Thread-%02d] %s %3d%% | %s%n", (i+1), barra, (p == -1 ? 0 : p), res);
        }
        System.out.format("\033[%dA", QUANTIDADE_CLIENTES);
    }

    private static String montarBarra(int progresso) {
        if (progresso == -1) progresso = 0;
        int casas = 10; 
        int preenchido = (progresso * casas) / 100;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < casas; i++) {
            if (i < preenchido) sb.append("█");
            else sb.append("░");
        }
        sb.append("]");
        return sb.toString();
    }

    private static boolean todosFinalizados() {
        synchronized (finalizado) {
            for (boolean f : finalizado) if (!f) return false;
            return true;
        }
    }

    private static void gerarLogExtraDetalhado(double duracaoTotal) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        
        try (FileWriter fw = new FileWriter("log_execucao.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            pw.println("######################################################################");
            pw.println("#  DUMP DE EXECUÇÃO MULTITHREAD - SPD");
            pw.println("#  DATA: " + dtf.format(now));
            pw.println("######################################################################");
            pw.printf("Tempo Total de Parelelismo: %.6f s%n", duracaoTotal);
            pw.printf("Vazão (Throughput): %.2f req/s%n", QUANTIDADE_CLIENTES / duracaoTotal);
            pw.println("----------------------------------------------------------------------");
            pw.printf("%-10s | %-15s | %-20s | %-20s | %-10s | %s%n", 
                      "THREAD", "ORIGEM/PORTA", "INÍCIO (ns)", "FIM (ns)", "LATÊNCIA", "RESULTADO");
            pw.println("-----------|-----------------|----------------------|----------------------|------------|-----------");

            for (int i = 0; i < QUANTIDADE_CLIENTES; i++) {
                double latenciaMs = (endTimes[i] - startTimes[i]) / 1_000_000.0;
                pw.printf("Thread-%02d  | %-15s | %-20d | %-20d | %-8.2f ms | %s%n", 
                          (i+1), clientInfo[i], startTimes[i], endTimes[i], latenciaMs, resultadosFinais[i]);
            }
            pw.println("######################################################################\n");
            
        } catch (IOException e) {
            System.err.println("Erro ao gravar log: " + e.getMessage());
        }
    }
}





