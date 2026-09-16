# Changelog

Todas as mudanças relevantes deste projeto serão registradas neste arquivo.

## 1.0.0 — 2026-09-16

Primeira baseline estável do laboratório black-box de testes da OWASP crAPI.

### Entregue

- Projeto Maven com Java 17, JUnit Jupiter 5.14.4, REST Assured 6.0.1 e Jackson Databind 2.22.2.
- Fluxo funcional independente de cadastro, login, captura de JWT e dashboard autenticado.
- Baselines negativas permitidas de autenticação e validação de entrada.
- Cenário opt-in de consumo de recursos, sequencial e limitado.
- DAST passivo controlado com OWASP ZAP, OpenAPI reduzida e gateway de tráfego.
- CI Java, Gitleaks, Dependabot e proteção obrigatória da branch `master`.
- Processo de triagem, catálogo, template, tratamento de evidências e critérios de regressão.

### Validação da release

- 24 testes unitários aprovados.
- 54 testes da suíte padrão aprovados contra a crAPI disponível.
- Nenhuma falha, erro ou skip nas execuções aceitas.
- Gitleaks sem achados na árvore de trabalho e no histórico Git.
- Workflows `Testes unitários` e `Segredos` obrigatórios antes do merge.

### Limitações conhecidas

- A Fase 3 de autorização horizontal/BOLA permanece pendente pela restrição de ambiente/ferramenta já registrada.
- Não existe vulnerabilidade confirmada no catálogo nem teste `security-regression` elegível.
- A baseline DAST foi passiva, limitada a três operações e ao orçamento autorizado; ela não demonstra ausência geral de vulnerabilidades.
- Testes live não executam no runner público porque a crAPI não é provisionada no GitHub Actions.
