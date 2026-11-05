# Core Banking: Monitor de Compliance & Pipeline Antifraude

Este repositório integra o ecossistema de soluções de engenharia voltadas para o setor financeiro e bancário. O projeto consiste em um **Dashboard de Compliance em Tempo Real** projetado para monitorar, classificar e expurgar transações financeiras de alto risco antes da fase de liquidação final (Core Banking).

## 🚀 Funcionalidades Chave

*   **Ingestão Contínua de Dados:** Monitoramento de transações de alta concorrência simulando fluxos reais de PIX, TED e DOC.
*   **Motor de Regras de Compliance:** Avaliação instantânea do payload da transação com classificação de risco em três níveis (Baixo, Médio e Crítico/Alto).
*   **Volumetria Expurgada (Pipeline Antifraude):** Lógica integrada que barra e expulsa transações de risco crítico do fluxo de balanço financeiro, garantindo propriedades ACID e isolamento de fraudes.
*   **Alta Disponibilidade (Resiliência Híbrida):** Arquitetura com mecanismo de *Fallback* automático. Se a API Java local estiver offline (ou rodando em ambiente estático como GitHub Pages), um motor interno em JavaScript assume o processamento contínuo para manter o dashboard funcional.

## 🛠️ Tecnologias, Dependências e Ferramentas

### Backend (Processamento e Persistência)
*   **Java:** Linguagem core utilizada para a construção do motor de regras de negócio e processamento pesado das transações.
*   **API REST nativa:** Exposição dos endpoints na porta `8080` (`/api/transacoes`) para comunicação assíncrona.
*   **Gerenciamento de Dependências:** Configurado para arquiteturas escaláveis de microsserviços bancários.

### Frontend (Painel Analítico)
*   **HTML5 & CSS3:** Interface responsiva customizada em modo escuro (*Dark Mode*), otimizada para visualização executiva via desktop ou dispositivos móveis.
*   **JavaScript (Vanilla JS):** Manipulação dinâmica do DOM, consumo de APIs via `Fetch API`, tratamento assíncrono de erros (`try/catch`) e motor de simulação de concorrência com `setInterval`.

## 📁 Estrutura do Repositório

*   `/index.html` - Interface do usuário e lógica de renderização/fallback em tempo real.
*   `/README.md` - Documentação técnica do sistema.

## 🔧 Como Executar o Projeto

### Modo Produção (Web / GitHub Pages)
O projeto possui deploy ativo e pode ser visualizado operando em tempo real diretamente pelo navegador, sem necessidade de dependências locais:
👉 [Acesse o Monitor de Compliance](https://github.io)

### Modo Desenvolvimento Local (Integração Java)
1. Certifique-se de ter o ambiente **Java** configurado localmente.
2. Inicie o servidor backend para rodar e expor a API na porta `8080`.
3. Abra o arquivo `index.html` no navegador. O script fará o consumo direto do endpoint `http://localhost:8080/api/transacoes`.
