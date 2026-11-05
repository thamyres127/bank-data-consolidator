import java.util.UUID;
import java.time.Instant;

// =====================================================================
// CAMADA 1: DOMÍNIO (domain) - REGRAS INTOCÁVEIS DE COMPLIANCE
// =====================================================================
record Transacao(String id, double valor, Instant timestamp, String tipo) {
    public Transacao {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID inválido.");
        if (tipo == null || tipo.isBlank()) throw new IllegalArgumentException("Tipo inválido.");
    }
}

class MotorValidacao {
    public static boolean ehValida(Transacao transacao) {
        if (transacao.valor() <= 0) return false;
        if (transacao.timestamp().isAfter(Instant.now())) return false;
        return true;
    }
}

// =====================================================================
// CAMADA 2: APLICAÇÃO (application) - PIPELINE DE ENRIQUECIMENTO
// =====================================================================
record TransacaoProcessadaDTO(String id, double valor, String tipo, String complianceRisco) {}

class PipelineService {
    private double volumeFinanceiroTotal = 0.0;

    public TransacaoProcessadaDTO processarEEnriquecer(Transacao t) {
        String risco = "🔴 ALTO RISCO (Encaminhar para Auditoria)";
        if (t.valor() <= 1000.0) {
            risco = "🟢 BAIXO RISCO (Liberação Automática)";
        } else if (t.valor() <= 10000.0) {
            risco = "🟡 MÉDIO RISCO (Monitoramento Preventivo)";
        }

        this.volumeFinanceiroTotal += t.valor();
        return new TransacaoProcessadaDTO(t.id(), t.valor(), t.tipo(), risco);
    }

    public double getVolumeFinanceiroTotal() {
        return this.volumeFinanceiroTotal;
    }
}

// =====================================================================
// ORQUESTRADOR CENTRAL (Main) - STEWARDSHIP DE DADOS
// =====================================================================
public class PipelineTransacoes {
    public static void main(String[] args) {
        System.out.println("=== CORE BANKING: PIPELINE DE INGESTÃO & TRANSFORMAÇÃO ===");
        
        PipelineService pipeline = new PipelineService();
        
        Transacao[] loteTransacoes = {
            new Transacao(UUID.randomUUID().toString().substring(0, 8), 450.00, Instant.now(), "PIX"),
            new Transacao(UUID.randomUUID().toString().substring(0, 8), -150.00, Instant.now(), "TED"),
            new Transacao(UUID.randomUUID().toString().substring(0, 8), 4500.00, Instant.now(), "DOC"),
            new Transacao(UUID.randomUUID().toString().substring(0, 8), 12500.00, Instant.now(), "PIX"),
            new Transacao(UUID.randomUUID().toString().substring(0, 8), 0.00, Instant.now(), "PIX")
        };
        
        int limpas = 0;
        int expurgadas = 0;
        
        for (Transacao t : loteTransacoes) {
            if (MotorValidacao.ehValida(t)) {
                limpas++;
                TransacaoProcessadaDTO dto = pipeline.processarEEnriquecer(t);
                System.out.println("✅ [PROCESSADA] ID: " + dto.id() + " | " + dto.tipo() + " | R$ " + dto.valor());
                System.out.println("   └─> Compliance: " + dto.complianceRisco());
            } else {
                expurgadas++;
                System.out.println("⚠️ [EXPURGADO] R$ " + t.valor() + " -> Bloqueado no filtro do Domínio");
            }
            System.out.println("----------------------------------------------------------------");
        }
        
        System.out.println("\n=== INDICADORES COMPLIANCE EM TEMPO REAL ===");
        System.out.println("Volumetria processada com sucesso: " + limpas);
        System.out.println("Volumetria expurgada da esteira:   " + expurgadas);
        System.out.println(String.format("Volume Financeiro Total Consolidado: R$ %.2f", pipeline.getVolumeFinanceiroTotal()));
    }
}
