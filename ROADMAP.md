# Roadmap

| Fase | Objetivo | Status |
|---|---|---|
| 0 | Bootstrap Maven, Java 17, configuração e documentação inicial | Implementada e compilada |
| 1 | Signup, login, JWT, dashboard autenticado e massa dinâmica | Implementada e validada live |
| 2 | Authentication Security | Baseline não destrutiva concluída e validada live |
| 3 | Authorization Security | Pendente por limitação do ambiente/ferramenta |
| 4 | Object and Input Security | Baseline permitida concluída e validada live |
| 5 | Resource Abuse | Planejada |
| 6 | DAST com OWASP ZAP | Planejada |
| 7 | CI/CD Security com GitHub Actions e Gitleaks | Planejada |
| 8 | Vulnerability Reporting e Security Regression Testing | Planejada |

## Estado atual

A Fase 0 compila com JDK 17 e possui treze verificações unitárias da configuração, da massa dinâmica e do mapeamento de credenciais. A Fase 1 foi validada contra uma instância da branch oficial `main` da crAPI: os três cenários funcionais passaram, sem falhas, erros ou skips. O dashboard autenticado valida `Content-Type` JSON, identidade, papel, presença de todas as propriedades obrigatórias e os tipos numéricos declarados pela OpenAPI. A baseline não destrutiva da Fase 2 foi validada com quatro cenários de autenticação. A baseline permitida da Fase 4 possui vinte e três casos: cadastro duplicado; objeto vazio em signup e login; quatro tipos raiz não objeto em ambos; ausência individual e valor `null` nos quatro campos obrigatórios do signup e nos dois campos obrigatórios do login. Os testes continuam falhando explicitamente quando o sistema sob teste está indisponível.

## Próxima etapa

A implementação prática do cenário de autorização horizontal/BOLA da Fase 3 ficou pendente por limitação do ambiente/ferramenta. A fase não está concluída, não possui código implementado e nenhuma vulnerabilidade de autorização foi marcada como validada.

Enquanto essa restrição permanecer, o trabalho pode continuar apenas em organização do projeto, documentação, cobertura funcional e negativa permitida e preparação estrutural das fases futuras. Não serão criados exploits fictícios nem resultados sem execução real.

A baseline segura da Fase 4 está concluída. Mass assignment, parameter tampering e autorização de propriedades permanecem sem implementação enquanto dependerem de conteúdo ou execução indisponível no ambiente.

A Fase 5 permanece planejada. Nenhum teste de rate limiting, carga, exaustão de recursos ou negação de serviço será executado sem orçamento de requests, janela, critério de interrupção e autorização explícitos.
