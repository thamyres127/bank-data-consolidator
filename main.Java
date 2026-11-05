import java.util.UUID;
import java.time.Instant;

// ==========================================
// CAMADA 1: DOMÍNIO (domain) - REGRAS INTOCÁVEIS
// ==========================================

// Modelo imutável da transação bancária.
record Transacao(String id, double valor, Instant timestamp, String tipo) {
    public Transacao {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID inválido.");
        if (tipo == null || tipo.isBlank()) throw new IllegalArgumentException("Tipo inválido.");
    }
}

// Mecanismo Anti-Fraude e Higienização de Dados
class MotorValidacao {
    public static boolean ehValida(Transacao transacao) {
        if (transacao.valor() <= 0) return false;
        if (transacao.timestamp().isAfter(Instant.now())) return false;
        return true;
    }
}

// ==========================================
// ETAPA 2: APLICAÇÃO (application) - ENRIQUECIMENTO E ORQUESTRAÇÃO
// ==========================================

// Objeto de Saída Enriquecido (DTO) para entrega no Data Lake ou Frontend
record TransacaoProcessadaDTO(String id, double valor, String tipo, String complianceRisco) {}

// Serviço que orquestra o pipeline, enriquece dados e calcula volumetria
class PipelineService {
    private double volumeFinanceiroTotal = 0.0;

    public TransacaoProcessadaDTO processarEEnriquecer(Transacao t) {
        // Regra de Negócio de Aplicação: Classificação de Compliance baseada no valor
        String risco = "🔴 ALTO RISCO (Encaminhar para Auditoria)";
        if (t.valor() <= 1000.0) {
            risco = "🟢 BAIXO RISCO (Liberação Automática)";
        } else if (t.valor() <= 10000.0) {
            risco = "🟡 MÉDIO RISCO (Monitoramento Preventivo)";
        }

        // Acumula o volume apenas de transações legítimas que entraram no pipeline
        this.volumeFinanceiroTotal += t.valor();

        // Retorna o dado transformado e enriquecido
        return new TransacaoProcessadaDTO(t.id(), t.valor(), t.tipo(), risco);
    }

    public double getVolumeFinanceiroTotal() {
        return this.volumeFinanceiroTotal;
    }
}

// ==========================================
// ORQUESTRADOR CENTRAL (Main)
// ==========================================
public class MyClass {
    public static void main(String[] args) {
        System.out.println("=== CORE BANKING: PIPELINE DE INGESTÃO & TRANSFORMAÇÃO ===");
        System.out.println("Status: Executando Etapa 2 (Enriquecimento de Payload)...\n");
        
        // Instancia o serviço da camada de aplicação
        PipelineService pipeline = new PipelineService();
        
        // Esteira de entrada com dados heterogêneos
        Transacao[] loteTransacoes = {
            new Transacao(UUID.randomUUID().toString().substring(0, 8), 450.00, Instant.now(), "PIX"),
            new Transacao(UUID.randomUUID().toString().substring(0, 8), -150.00, Instant.now(), "TED"), // Inválida
            new Transacao(UUID.randomUUID().toString().substring(0, 8), 4500.00, Instant.now(), "DOC"),
            new Transacao(UUID.randomUUID().toString().substring(0, 8), 12500.00, Instant.now(), "PIX"), // Alto Risco
            new Transacao(UUID.randomUUID().toString().substring(0, 8), 0.00, Instant.now(), "PIX") // Inválida
        };
        
        int limpas = 0;
        int expurgadas = 0;
        
        for (Transacao t : loteTransacoes) {
            // Camada 1 barra o que está corrompito
            if (MotorValidacao.ehValida(t)) {
                limpas++;
                // Camada 2 transforma, classifica e processa o dado limpo
                TransacaoProcessadaDTO dto = pipeline.processarEEnriquecer(t);
                System.out.println("✅ [PROCESSADA] ID: " + dto.id() + " | " + dto.tipo() + " | R$ " + dto.valor());
                System.out.println("   └─> Compliance: " + dto.complianceRisco());
            } else {
                expurgadas++;
                System.out.println("⚠️ [EXPURGADO]  R$ " + t.valor() + " -> Bloqueado no filtro do Domínio");
            }
            System.out.println("----------------------------------------------------------------");
        }
        
        System.out.println("\n=== INDICADORES COMPLIANCE EM TEMPO REAL ===");
        System.out.println("Volumetria processada com sucesso: " + limpas);
        System.out.println("Volumetria expurgada da esteira:   " + expurgadas);
        System.out.printf("Volume Financeiro Total Consolidado: R$ %.2f\n", pipeline.getVolumeFinanceiroTotal());
    }
}
