import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

// =========================================================================
// CAMADA 1: DOMÍNIO (domain) - REGRAS DE NEGÓCIO E FILTROS
// =========================================================================
record Transacao(String id, double valor, String tipo, Instant timestamp) {
    public Transacao {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID inválido");
        if (tipo == null || tipo.isBlank()) throw new IllegalArgumentException("Tipo inválido");
    }
}

class MotorValidacao {
    public static boolean ehValida(Transacao transacao) {
        if (transacao.valor() <= 0) return false;
        return true;
    }

    public static String classificarRisco(Transacao transacao) {
        if (transacao.valor() > 10000.00) return "🔴 ALTO RISCO";
        if (transacao.valor() > 2000.00) return "🟡 MÉDIO RISCO";
        return "🟢 BAIXO RISCO";
    }

    public static String obtenerAcao(String risco) {
        return switch (risco) {
            case "🔴 ALTO RISCO" -> "(Encaminhar para Auditoria)";
            case "🟡 MÉDIO RISCO" -> "(Monitoramento Preventivo)";
            default -> "(Liberação Automática)";
        };
    }
}

// =========================================================================
// ETAPA 2 & 3: APLICAÇÃO & INFRAESTRUTURA (Pipeline e API)
// =========================================================================
class PipelineService {
    private Connection conexao;
    private static int volumetriaExpurgada = 0;
    private static List<String> transacoesJsonList = new ArrayList<>();

    public PipelineService() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.conexao = DriverManager.getConnection("jdbc:sqlite:banco_infra_banking.db");
            criarTabela();
        } catch (Exception e) {
            try {
                Class.forName("org.h2.Driver");
                this.conexao = DriverManager.getConnection("jdbc:h2:mem:banco_infra_banking;DB_CLOSE_DELAY=-1");
                criarTabela();
            } catch (Exception ex) {
                System.err.println("Erro ao inicializar banco: " + ex.getMessage());
            }
        }
    }

    private void criarTabela() {
        if (this.conexao == null) return;
        String sql = """
            CREATE TABLE IF NOT EXISTS transacoes_processadas (
                id VARCHAR(50) PRIMARY KEY,
                valor DECIMAL(10, 2),
                tipo VARCHAR(10),
                compliance_risco VARCHAR(100),
                timestamp_processamento VARCHAR(50)
            );
        """;
        try (Statement stmt = this.conexao.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            System.err.println("Erro ao criar tabela: " + e.getMessage());
        }
    }

    public void processarEsteira(List<Transacao> lote) {
        for (Transacao t : lote) {
            if (!MotorValidacao.ehValida(t)) {
                volumetriaExpurgada++;
                continue;
            }

            String risco = MotorValidacao.classificarRisco(t);
            salvarNoBanco(t, risco);

            // Monta uma string JSON simplificada para o front-end ler de forma nativa
            String json = String.format("{\"id\":\"%s\",\"tipo\":\"%s\",\"valor\":%.2f,\"risco\":\"%s\"}", 
                t.id(), t.tipo(), t.valor(), risco);
            transacoesJsonList.add(json);
        }
    }

    private void salvarNoBanco(Transacao t, String risco) {
        if (this.conexao == null) return;
        String sql = "INSERT INTO transacoes_processadas (id, valor, tipo, compliance_risco, timestamp_processamento) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = this.conexao.prepareStatement(sql)) {
            pstmt.setString(1, t.id());
            pstmt.setDouble(2, t.valor());
            pstmt.setString(3, t.tipo());
            pstmt.setString(4, risco);
            pstmt.setString(5, t.timestamp().toString());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Erro ao salvar no SQL: " + e.getMessage());
        }
    }

    public static String obterDadosDashboardJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"expurgados\":").append(volumetriaExpurgada).append(",");
        sb.append("\"transacoes\":[");
        for (int i = 0; i < transacoesJsonList.size(); i++) {
            sb.append(transacoesJsonList.get(i));
            if (i < transacoesJsonList.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }
}

// =========================================================================
// SERVIDOR HTTP & PONTO DE ENTRADA
// =========================================================================
public class Main {
    public static void main(String[] args) throws IOException {
        PipelineService pipeline = new PipelineService();

        // Carga inicial de dados para teste na esteira
        List<Transacao> loteDeEntrada = new ArrayList<>();
        loteDeEntrada.add(new Transacao("3e950370", 450.00, "PIX", Instant.now()));
        loteDeEntrada.add(new Transacao("expurgada1", -150.00, "PIX", Instant.now()));
        loteDeEntrada.add(new Transacao("91c6e5c5", 4500.00, "DOC", Instant.now()));
        loteDeEntrada.add(new Transacao("78d4a7cc", 12500.00, "PIX", Instant.now()));
        loteDeEntrada.add(new Transacao("expurgada2", 0.0, "TED", Instant.now()));

        pipeline.processarEsteira(loteDeEntrada);

        // Inicializa o servidor HTTP na porta 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Endpoint que servirá o JSON para o front-end index.html
        server.createContext("/api/transacoes", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = PipelineService.obterDadosDashboardJson();
                
                // Habilita o CORS para permitir que o arquivo HTML acione a API localmente
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                
                byte[] bytes = response.getBytes("UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        server.setExecutor(null);
        System.out.println("🚀 Servidor de Core Banking rodando com sucesso na porta 8080!");
        System.out.println("API disponível em: http://localhost:8080/api/transacoes");
        server.start();
    }
}
