# Roadmap

| Fase | Objetivo | Status |
|---|---|---|
| 0 | Bootstrap Maven, Java 17, configuração e documentação inicial | Implementada e compilada |
| 1 | Signup, login, JWT, dashboard autenticado e massa dinâmica | Implementada e validada live |
| 2 | Authentication Security | Baseline não destrutiva concluída e validada live |
| 3 | Authorization Security | Pendente por limitação do ambiente/ferramenta |
| 4 | Object and Input Security | Baseline permitida concluída e validada live |
| 5 | Resource Abuse | Baseline controlada concluída; protocolo final 3/3 aprovado |
| 6 | DAST com OWASP ZAP | Preparação estrutural concluída; execução não autorizada |
| 7 | CI/CD Security com GitHub Actions e Gitleaks | Planejada |
| 8 | Vulnerability Reporting e Security Regression Testing | Planejada |

## Estado atual

A Fase 0 compila com JDK 17, possui treze verificações unitárias da configuração, da massa dinâmica e do mapeamento de credenciais e registra a versão exata da OpenAPI oficial usada como baseline. A Fase 1 foi validada contra uma instância da branch oficial `main` da crAPI: os três cenários funcionais passaram, sem falhas, erros ou skips, e as três respostas `200` são confirmadas como JSON. O dashboard autenticado também valida identidade, papel, presença de todas as propriedades obrigatórias e os tipos numéricos declarados pela OpenAPI. A baseline não destrutiva da Fase 2 foi validada com quatro cenários de autenticação. A baseline permitida da Fase 4 possui vinte e três casos: cadastro duplicado; objeto vazio em signup e login; quatro tipos raiz não objeto em ambos; ausência individual e valor `null` nos quatro campos obrigatórios do signup e nos dois campos obrigatórios do login. Os testes continuam falhando explicitamente quando o sistema sob teste está indisponível.

## Próxima etapa

A implementação prática do cenário de autorização horizontal/BOLA da Fase 3 ficou pendente por limitação do ambiente/ferramenta. A fase não está concluída, não possui código implementado e nenhuma vulnerabilidade de autorização foi marcada como validada.

Enquanto essa restrição permanecer, o trabalho pode continuar apenas em organização do projeto, documentação, cobertura funcional e negativa permitida e preparação estrutural das fases futuras. Não serão criados exploits fictícios nem resultados sem execução real.

A baseline segura da Fase 4 está concluída. Mass assignment, parameter tampering e autorização de propriedades permanecem sem implementação enquanto dependerem de conteúdo ou execução indisponível no ambiente.

A Fase 5 foi executada com autorização explícita para, em cada execução, no máximo dez requisições sequenciais em dez segundos, latência máxima individual de dois segundos e interrupção em HTTP 5xx, estouro da janela ou ambiente não saudável. Antes do protocolo final, uma tentativa parou no signup após 1/10 requisições e 2.164 ms, e outra completou 10/10. O protocolo final de três execuções independentes foi aprovado em 3/3, com todas as respostas HTTP 200 e dentro dos limites. A baseline controlada está concluída, mas não confirma rate limiting, resistência a carga ou ausência de vulnerabilidade. Não houve concorrência, carga, exaustão de recursos ou negação de serviço.

A preparação estrutural da Fase 6 está documentada em `docs/dast-plan.md`. O gerador Java opt-in da OpenAPI temporária reduzida aos três endpoints permitidos está implementado e valida o hash da fonte oficial. Nenhuma imagem ZAP foi baixada e nenhum scan foi executado. A execução futura ainda depende de imagem oficial fixada por digest, controles externos de interrupção, orçamento de requests e autorização específica.
