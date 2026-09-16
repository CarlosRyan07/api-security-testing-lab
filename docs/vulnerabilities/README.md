# Catálogo de vulnerabilidades

Este diretório contém somente relatórios de vulnerabilidades confirmadas por execução real e triagem. O arquivo `TEMPLATE.md` não representa um achado.

Nenhum achado confirmado foi registrado até o momento.

| ID | Título | OWASP | Severidade | Status | Teste de regressão |
|---|---|---|---|---|---|

## Regras do catálogo

- Cada relatório usa o nome `SEC-AAAA-NNN.md` e um ID único e imutável.
- Um alerta de ferramenta, drift de contrato, falha de orquestração ou comportamento ainda não reproduzido não recebe ID de vulnerabilidade.
- O catálogo deve apontar para o teste de regressão somente depois que uma correção estiver disponível e o teste tiver sido executado contra o alvo corrigido.
- Evidências brutas ficam em `evidence/`, fora do Git; o relatório contém apenas descrições e hashes sanitizados.
