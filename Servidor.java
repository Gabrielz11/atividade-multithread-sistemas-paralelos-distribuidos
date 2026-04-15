import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Servidor {

    private static final int PORTA = 65432;
    private static final int MAX_CONEXOES = 50;

    // RECURSOS COMPARTILHADOS PARA DEMONSTRAÇÃO
    private static double saldoCompartilhado = 1000.0;
    private static final ReentrantLock lockConta = new ReentrantLock();
    private static final Semaphore semaforoProcessamento = new Semaphore(2); // Limita a 2 acessos simultâneos

    // Para simular Deadlock
    private static final Object RecursoA = new Object();
    private static final Object RecursoB = new Object();

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  SERVIDOR SPD - MÓDULO DE CONCORRÊNCIA AVANÇADA");
        System.out.println("==================================================");
        System.out.println("  Porta       : " + PORTA);
        System.out.println("  Precisão    : Nanosegundos (High-Res)");
        System.out.println("==================================================");

        try (ServerSocket serverSocket = new ServerSocket(PORTA, MAX_CONEXOES)) {
            while (true) {
                Socket socketCliente = serverSocket.accept();
                Thread thread = new Thread(new TratadorCliente(socketCliente));
                thread.setDaemon(true);
                thread.start();

                // Monitoramento de saúde do servidor
                System.out.println("[MONITOR] Conexão aceita. Threads Ativas: " + Thread.activeCount());
            }
        } catch (IOException e) {
            System.err.println("[EXCEÇÃO NO SERVIDOR] " + e.getMessage());
        }
    }

    static class TratadorCliente implements Runnable {
        private final Socket socket;
        private final String correlationId;

        public TratadorCliente(Socket socket) {
            this.socket = socket;
            // Correlation ID único baseado no endereço do cliente
            this.correlationId = "REQ-" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }

        @Override
        public void run() {
            log("CONECTADO");
            try (
                    BufferedReader entrada = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    PrintWriter saida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"),
                            true)) {
                String comando;
                while ((comando = entrada.readLine()) != null) {
                    processarComando(comando, saida);
                }
            } catch (Exception e) {
                log("ERRO NA THREAD: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            } finally {
                log("DESCONECTADO");
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        private void processarComando(String comando, PrintWriter saida) throws InterruptedException {
            String c = comando.trim().toUpperCase();
            log("COMANDO RECEBIDO: " + c);

            // Se for comando de sistema, executa a simulação
            if (c.startsWith("SAQUE_INSEGURO")) {
                demonstrarRaceCondition(saida);
            } else if (c.startsWith("SAQUE_LOCK")) {
                demonstrarLock(saida);
            } else if (c.startsWith("SAQUE_SEMAFORO")) {
                demonstrarSemaforo(saida);
            } else if (c.equals("DEADLOCK")) {
                simularDeadlock(saida);
            } else if (c.equals("EXECUTAR_JOB")) {
                simularProcessamento(saida);
            } else {
                // Caso contrário, trata como uma expressão matemática de cálculo
                String resultado = realizarCalculo(comando);
                log("CÁLCULO CONCLUÍDO: " + comando + " = " + resultado);
                saida.println(resultado);
            }
        }

        private String realizarCalculo(String expressao) {
            expressao = expressao.trim();
            // Regex para validar: numero operador numero
            String padrao = "^(-?\\d+(\\.\\d+)?)\\s*([+\\-*/])\\s*(-?\\d+(\\.\\d+)?)$";

            if (!expressao.matches(padrao)) {
                return "ERRO: Formato inválido. Use: num op num (ex: 5 + 3)";
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
                    case '+':
                        res = n1 + n2;
                        break;
                    case '-':
                        res = n1 - n2;
                        break;
                    case '*':
                        res = n1 * n2;
                        break;
                    case '/':
                        if (n2 == 0)
                            return "ERRO: Divisão por zero";
                        res = n1 / n2;
                        break;
                }

                // Simula um tempo de CPU pequeno para evidenciar o paralelismo nos logs
                Thread.sleep(100);

                if (res == (long) res)
                    return String.valueOf((long) res);
                return String.format("%.4f", res);
            } catch (Exception e) {
                return "ERRO: Falha no processamento numérico";
            }
        }

        private void log(String msg) {
            long ts = System.nanoTime();
            Thread t = Thread.currentThread();
            // Exibe: [Tempo] [Thread INFO] [ID do Pedido] Mensagem
            System.out.printf("[%d ns] [%s (ID:%d)] [%s] %s%n",
                    ts, t.getName(), t.threadId(), correlationId, msg);
        }

        // --- 1. DEMONSTRAÇÃO DE RACE CONDITION (SEM SINCRONIZAÇÃO) ---
        private void demonstrarRaceCondition(PrintWriter saida) throws InterruptedException {
            saida.println("DEBUG: Lendo saldo atual: " + saldoCompartilhado);
            double v = 100.0;
            log("RACE_CONDITION: Lendo Saldo = " + saldoCompartilhado);

            // Simula um delay para forçar a preempção no meio da operação (leitura ->
            // escrita)
            Thread.sleep(500);

            saldoCompartilhado -= v;
            log("RACE_CONDITION: Salvo Saldo = " + saldoCompartilhado);
            saida.println("RESULTADO: Saque realizado. Saldo atual: " + saldoCompartilhado);
        }

        // --- 2. DEMONSTRAÇÃO DE MUTEX (LOCK) ---
        private void demonstrarLock(PrintWriter saida) {
            lockConta.lock();
            try {
                log("LOCK: Seção Crítica Adquirida");
                double v = 100.0;
                saldoCompartilhado -= v;
                saida.println("RESULTADO_LOCK: Sucesso. Saldo: " + saldoCompartilhado);
            } finally {
                log("LOCK: Seção Crítica Liberada");
                lockConta.unlock();
            }
        }

        // --- 3. DEMONSTRAÇÃO DE SEMÁFORO ---
        private void demonstrarSemaforo(PrintWriter saida) throws InterruptedException {
            saida.println("INFO: Aguardando permissão do Semáforo...");
            if (semaforoProcessamento.tryAcquire(5, TimeUnit.SECONDS)) {
                try {
                    log("SEMAFORO: Permissão obtida. Processando...");
                    Thread.sleep(2000); // Simulando carga
                    saida.println("RESULTADO_SEMAFORO: Processamento concluído.");
                } finally {
                    log("SEMAFORO: Permissão devolvida.");
                    semaforoProcessamento.release();
                }
            } else {
                saida.println("ERRO_SEMAFORO: Timeout. Servidor ocupado.");
            }
        }

        // --- 4. DEMONSTRAÇÃO DE DEADLOCK ---
        private void simularDeadlock(PrintWriter saida) {
            saida.println("INFO: Iniciando simulação de Deadlock...");
            // Gera um deadlock dependendo do IP/Porta para cruzar os Locks
            boolean ordemReversa = (socket.getPort() % 2 == 0);

            new Thread(() -> {
                Object primeiro = ordemReversa ? RecursoB : RecursoA;
                Object segundo = ordemReversa ? RecursoA : RecursoB;

                synchronized (primeiro) {
                    log("DEADLOCK: Bloqueou " + (ordemReversa ? "B" : "A"));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    log("DEADLOCK: Tentando bloquear " + (ordemReversa ? "A" : "B") + "...");
                    synchronized (segundo) {
                        log("DEADLOCK: Sucesso improvável!");
                    }
                }
            }).start();
        }

        private void simularProcessamento(PrintWriter saida) throws InterruptedException {
            int progress = 0;
            while (progress < 100) {
                progress += (int) (Math.random() * 10) + 5;
                if (progress > 100)
                    progress = 100;
                saida.println("JOB_STATUS:" + progress);
                Thread.sleep(300);
            }
            saida.println("JOB_COMPLETED");
        }
    }
}
