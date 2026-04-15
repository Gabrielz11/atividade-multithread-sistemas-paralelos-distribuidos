# 🚀 Sistema de Monitoramento de Fluxo Paralelo (Calculadora Distribuída)

Sistema acadêmico desenvolvido para a disciplina de **Sistemas Paralelos e Distribuídos (SPD)**, com foco em demonstrar o processamento multithread de requisições via socket TCP.

## 📝 Descrição
Este projeto consiste em um servidor TCP robusto capaz de lidar com múltiplos clientes simultâneos para a realização de cálculos matemáticos. O diferencial do sistema é o seu **mecanismo de monitoramento de performance**, que registra cada operação com precisão de nanosegundos e gera logs detalhados sobre a saúde das threads e do escalonamento do processador.

## ⚠️ Problema
Em sistemas distribuídos tradicionais com processamento sequencial, o servidor fica bloqueado enquanto atende um único cliente. Isso gera gargalos, subutilização de hardware e latência inaceitável quando muitos usuários tentam acessar o serviço ao mesmo tempo.

## ✅ Solução
A solução implementada utiliza o modelo **Multi-threaded Server**. Para cada nova conexão aceita, o servidor instancia uma nova `Thread` dedicada, isolando o contexto do cliente. 
- **Paralelismo Real**: Permite que cálculos complexos sejam resolvidos sem que um cliente tenha que esperar o outro.
- **Rastreabilidade**: Implementação de *Correlation IDs* e *Thread IDs* para identificar qual thread processou qual pedido.
- **Precisão Cirúrgica**: Uso de `System.nanoTime()` para validar cientificamente a concorrência.

## 🛠️ Stack Técnica
- **Linguagem**: Java 17+
- **Comunicação**: Sockets TCP
- **Concorrência**: Java Threads, Runnable e ReentrantLocks
- **Monitoramento**: Dashboard em tempo real via terminal e Logs persistentes (.txt)

## 🚀 Como rodar

### 1. Compilação
Abra o terminal na pasta do projeto e compile os arquivos:
```bash
javac Servidor.java TesteMultithread.java
```

### 2. Iniciar o Servidor
Mantenha este terminal aberto para visualizar os logs de nanosegundos de cada thread:
```bash
java Servidor
```

### 3. Iniciar o Orquestrador (Teste)
Em um novo terminal, rode o monitor que disparará as threads de cálculo:
```bash
java TesteMultithread
```

### 4. Rodar em Rede Local (Outras Máquinas)
Seus amigos podem conectar na sua máquina. 
1. Primeiro, descubra seu IP (ex: `ipconfig` no Windows).
2. Seus amigos devem rodar:
```bash
# Para o teste de estresse distribuído:
java TesteMultithread SEU_IP_AQUI

# Para o cliente manual simples:
java Cliente SEU_IP_AQUI
```

---
**Desenvolvido para fins didáticos na disciplina de Sistemas Paralelos e Distribuídos.**
