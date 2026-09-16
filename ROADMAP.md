# Roadmap

Release estável atual: `v1.0.0`. O conteúdo da release está consolidado em `CHANGELOG.md`.

| Fase | Objetivo | Status |
|---|---|---|
| 0 | Bootstrap Maven, Java 17, configuração e documentação inicial | Implementada e compilada |
| 1 | Signup, login, JWT, dashboard autenticado e massa dinâmica | Implementada e validada live |
| 2 | Authentication Security | Baseline não destrutiva concluída e validada live |
| 3 | Authorization Security | Pendente por limitação do ambiente/ferramenta |
| 4 | Object and Input Security | Baseline permitida concluída e validada live |
| 5 | Resource Abuse | Baseline controlada concluída; protocolo final 3/3 aprovado |
| 6 | DAST com OWASP ZAP | Baseline passiva concluída; 9/10 requests e nenhum achado confirmado |
| 7 | CI/CD Security com GitHub Actions e Gitleaks | Concluída; checks local e remoto aprovados |
| 8 | Vulnerability Reporting e Security Regression Testing | Estrutura concluída; sem achado corrigido elegível para regressão |

## Estado atual

A Fase 0 compila com JDK 17, possui treze verificações unitárias da configuração, da massa dinâmica e do mapeamento de credenciais e registra a versão exata da OpenAPI oficial usada como baseline. A Fase 1 foi validada contra uma instância da branch oficial `main` da crAPI: os três cenários funcionais passaram, sem falhas, erros ou skips, e as três respostas `200` são confirmadas como JSON. O dashboard autenticado também valida identidade, papel, presença de todas as propriedades obrigatórias e os tipos numéricos declarados pela OpenAPI. A baseline não destrutiva da Fase 2 foi validada com quatro cenários de autenticação. A baseline permitida da Fase 4 possui vinte e três casos: cadastro duplicado; objeto vazio em signup e login; quatro tipos raiz não objeto em ambos; ausência individual e valor `null` nos quatro campos obrigatórios do signup e nos dois campos obrigatórios do login. Os testes continuam falhando explicitamente quando o sistema sob teste está indisponível.

## Próxima etapa

A implementação prática do cenário de autorização horizontal/BOLA da Fase 3 ficou pendente por limitação do ambiente/ferramenta. A fase não está concluída, não possui código implementado e nenhuma vulnerabilidade de autorização foi marcada como validada.

Enquanto essa restrição permanecer, o trabalho pode continuar apenas em organização do projeto, documentação, cobertura funcional e negativa permitida e preparação estrutural das fases futuras. Não serão criados exploits fictícios nem resultados sem execução real.

A baseline segura da Fase 4 está concluída. Mass assignment, parameter tampering e autorização de propriedades permanecem sem implementação enquanto dependerem de conteúdo ou execução indisponível no ambiente.

A Fase 5 foi executada com autorização explícita para, em cada execução, no máximo dez requisições sequenciais em dez segundos, latência máxima individual de dois segundos e interrupção em HTTP 5xx, estouro da janela ou ambiente não saudável. Antes do protocolo final, uma tentativa parou no signup após 1/10 requisições e 2.164 ms, e outra completou 10/10. O protocolo final de três execuções independentes foi aprovado em 3/3, com todas as respostas HTTP 200 e dentro dos limites. A baseline controlada está concluída, mas não confirma rate limiting, resistência a carga ou ausência de vulnerabilidade. Não houve concorrência, carga, exaustão de recursos ou negação de serviço.

A Fase 6 foi concluída dentro da autorização registrada em `docs/dast-results.md`. Três tentativas consumiram 9/10 requests no total; as duas primeiras expuseram incompatibilidades de orquestração do Windows PowerShell 5 e não foram aceitas como resultado final. A terceira encerrou com gateway `COMPLETED`, 3/4 requests e código oficial ZAP `0`. Quatro grupos informativos de risco `0` foram revisados e não confirmam vulnerabilidade. A suíte pós-scan passou com 51/51 testes. Qualquer nova execução DAST exige orçamento e autorização próprios.

A Fase 7 adiciona dois workflows independentes: compilação e testes unitários com Java 17, e detecção de segredos nos commits de cada evento com Gitleaks. As Actions estão fixadas por SHA, usam permissões mínimas e limites de tempo. O Dependabot acompanha semanalmente Maven e GitHub Actions. Os testes live permanecem fora do runner público porque a crAPI não é provisionada no pipeline. A primeira execução remota aprovou os dois checks; a auditoria local complementar também examinou os doze commits existentes sem encontrar leaks. A branch `master` exige ambos os checks, atualização antes do merge, histórico linear e resolução de conversas, inclusive para o administrador; force-push e exclusão estão bloqueados.

A Fase 8 possui processo de triagem, catálogo, template, política de evidências, estados do achado e critérios de admissão para regressões. Três testes unitários protegem a estrutura documental e o grupo `security-regression` é opt-in. Nenhum relatório concreto ou teste live foi criado porque não existe achado confirmado com alvo corrigido; uma execução com zero testes não é tratada como aprovação.

O roadmap 0–8 está implementado até o limite das evidências e autorizações disponíveis. Permanecem pendentes a Fase 3, pela restrição já registrada, e a execução operacional da Fase 8 quando surgir um achado confirmado e corrigido.
