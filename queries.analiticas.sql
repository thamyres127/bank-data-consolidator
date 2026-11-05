-- 1. CRIAÇÃO DA TABELA DE COMPLIANCE BANCÁRIO
CREATE TABLE IF NOT EXISTS transacoes_processadas (
    id VARCHAR(50) PRIMARY KEY,
    valor DECIMAL(10, 2),
    tipo VARCHAR(10),
    compliance_risco VARCHAR(100),
    timestamp_processamento VARCHAR(50)
);

-- 2. POPULANDO O BANCO (Simulando a inserção de alta volumetria)
INSERT INTO transacoes_processadas (id, valor, tipo, compliance_risco, timestamp_processamento) VALUES
('f89da4e1', 450.00, 'PIX', '🟢 BAIXO RISCO (Liberação Automática)', '2026-11-05T13:48:00Z'),
('8505f6ef', 12500.00, 'PIX', '🔴 ALTO RISCO (Encaminhar para Auditoria)', '2026-11-05T13:48:10Z');

-- 3. EXTRAÇÃO ANALÍTICA (A query de Engenharia de Dados)
SELECT 
    COUNT(*) as total_transacoes,
    SUM(valor) as volume_financeiro_total
FROM transacoes_processadas;
