# API Security Testing Lab

Projeto de portfólio de QA Engineering para automação black-box de APIs e evolução progressiva até Security Regression Testing e DevSecOps. O sistema sob teste é a [OWASP crAPI](https://github.com/OWASP/crAPI), uma aplicação externa e deliberadamente vulnerável.

Versão estável atual: `1.0.0`. Consulte o [CHANGELOG.md](CHANGELOG.md) para o escopo validado e as limitações conhecidas.

Este repositório não contém nem reimplementa a crAPI. A etapa atual cobre o bootstrap, o fluxo funcional mínimo e as baselines negativas permitidas de autenticação e validação de entrada.

## Stack

- Java 17
- Maven
- JUnit 5
- REST Assured
- Jackson
- Docker e Docker Compose para executar a crAPI separadamente
- GitHub Actions
- Gitleaks

## Arquitetura

O código de teste separa configuração, clientes HTTP, contratos de payload, geração de dados e cenários funcionais:

```text
src/test/java/io/github/apisecurity/
├── client/       # Chamadas HTTP para autenticação e usuário
├── config/       # URL, especificação HTTP comum e testes isolados
├── data/         # Massa dinâmica e independente
├── functional/   # Cenários de happy path
├── model/        # Payloads e respostas necessários
├── reporting/    # Consistência do catálogo e dos relatórios
├── security/     # Cenários negativos permitidos
└── tooling/      # Preparação offline e opt-in de artefatos
```

Detalhes das fases estão em [ROADMAP.md](ROADMAP.md), a estratégia de segurança está em [docs/security-test-plan.md](docs/security-test-plan.md), a versão exata da OpenAPI consultada está em [docs/contract-baseline.md](docs/contract-baseline.md), a execução da Fase 6 está em [docs/dast-results.md](docs/dast-results.md), os controles da Fase 7 estão em [docs/cicd-security.md](docs/cicd-security.md) e a gestão de achados da Fase 8 está em [docs/vulnerability-management.md](docs/vulnerability-management.md).

## Pré-requisitos

- JDK 17
- Maven 3.9 ou superior
- Docker com Docker Compose 1.27 ou superior
- OWASP crAPI da branch estável `main`

Confirme que o Maven está usando o JDK correto:

```bash
mvn -version
```

## Executar a crAPI

Obtenha e execute a crAPI fora deste repositório seguindo a [documentação oficial](https://github.com/OWASP/crAPI#docker-and-docker-compose):

```bash
git clone --depth 1 --branch main https://github.com/OWASP/crAPI.git
cd crAPI/deploy/docker
docker compose pull
docker compose -f docker-compose.yml --compatibility up -d
```

Por padrão, a aplicação fica disponível em `http://localhost:8888`. Aguarde a inicialização completa antes de executar os testes.

## Configuração

Os testes usam a variável de ambiente `BASE_URL` e adotam `http://localhost:8888` quando ela não está definida.

PowerShell:

```powershell
$env:BASE_URL = "http://localhost:8888"
mvn test -Dgroups=functional
```

Bash:

```bash
BASE_URL=http://localhost:8888 mvn test -Dgroups=functional
```

`BASE_URL` deve ser uma URL HTTP ou HTTPS válida. Espaços externos e barras finais são normalizados; valores vazios, malformados, sem host ou com outro protocolo são rejeitados. Nenhum JWT ou credencial preexistente é necessário: cada teste cria seu próprio usuário.

A massa usa uma sequência thread-safe iniciada dinamicamente para garantir, durante cada execução, nome, e-mail `example.com`, telefone de dez dígitos e senha distintos. O mapeamento de signup para login reutiliza somente e-mail e senha.

## Comandos

Compilar o projeto e os testes sem acessar a crAPI:

```bash
mvn test-compile
```

Executar apenas os testes unitários de configuração, sem acessar a crAPI:

```bash
mvn test -Dgroups=unit
```

Executar apenas os testes funcionais:

```bash
mvn test -Dgroups=functional
```

Executar todos os testes de segurança permitidos:

```bash
mvn test -Dgroups=security
```

Executar apenas os testes de autenticação da Fase 2:

```bash
mvn test -Dgroups=authentication
```

Executar apenas a baseline de validação de entrada da Fase 4:

```bash
mvn test -Dgroups=input-validation
```

Executar a baseline controlada de consumo de recursos da Fase 5:

```bash
mvn test -Dgroups=resource-abuse "-Dexcluded.test.groups=__none__"
```

O grupo `resource-abuse` é excluído por padrão, inclusive de `mvn test` e do grupo geral `security`, para impedir repetição acidental do tráfego controlado.

O grupo `security-regression` também é excluído por padrão. Ele só pode receber e executar testes quando houver relatório confirmado e alvo corrigido, conforme [docs/regression-testing.md](docs/regression-testing.md). Atualmente não existe caso elegível; zero testes nunca é aceito como aprovação de segurança.

Executar toda a suíte atual:

```bash
mvn test
```

Se a crAPI estiver indisponível, os testes falham explicitamente em vez de serem ignorados.

## Fluxo funcional atual

1. Gera usuário único.
2. Cadastra o usuário.
3. Autentica com e-mail e senha.
4. Captura o JWT retornado, sem registrá-lo em logs.
5. Acessa o dashboard com Bearer Token.
6. Confirma que o dashboard pertence ao usuário criado, possui papel `ROLE_USER` e contém todas as propriedades obrigatórias declaradas no contrato.

As respostas bem-sucedidas de signup, login e dashboard são validadas como JSON. No dashboard, `id`, `available_credit` e `video_id` devem ser numéricos. As propriedades obrigatórias que podem vir nulas no exemplo oficial (`picture_url`, `video_name` e `video_url`) são verificadas por presença, sem inventar restrições de valor.

Os modelos de resposta mapeiam somente os campos documentados na OpenAPI oficial. Campos adicionais retornados pela aplicação são ignorados na desserialização e não recebem asserções nem significado inventado pelo projeto.

## Authentication Security

A baseline negativa da Fase 2 verifica que credenciais desconhecidas, senha incorreta, ausência de JWT e JWT malformado não concedem acesso. A instância oficial `main` validada retornou `401` nos dois logins rejeitados e `404` nas duas chamadas não autenticadas ao dashboard. As respostas rejeitadas também foram verificadas para não expor token ou os campos de identidade do dashboard.

A OpenAPI usada na validação declara Bearer JWT para o dashboard e inclui `404` entre suas respostas. Para o login, porém, ela lista `200` e `500`, enquanto a aplicação retornou `401` para credenciais desconhecidas; essa divergência está registrada como observação de contrato.

Brute force de OTP, reset da senha de terceiros e manipulações avançadas de JWT não fazem parte desta baseline não destrutiva.

## Input Validation

A baseline permitida da Fase 4 cobre cadastro duplicado; objeto vazio; corpo JSON array, string, número ou booleano onde o schema exige objeto; ausência individual e valor `null` em todos os campos obrigatórios documentados: `email`, `name`, `number` e `password` no signup; `email` e `password` no login. A instância validada retornou `403` para a duplicidade, conforme resposta prevista no signup, e `400` para cada uma das outras vinte e duas variações inválidas. O status `400` observado não aparece entre as respostas declaradas dessas operações na OpenAPI e está registrado como drift de contrato.

Mass assignment, parameter tampering e exploração de propriedades não foram implementados.

A baseline segura da Fase 4 está concluída. A Fase 5 não executará rate limiting, carga ou negação de serviço sem limites e autorização explícitos.

## Resource Abuse controlado

A baseline da Fase 5 é executada sequencialmente, e cada execução autorizada possui orçamento total de dez chamadas: um signup, um login e até oito acessos ao dashboard em uma janela de dez segundos. O teste interrompe imediatamente diante de HTTP 5xx, latência individual acima de dois segundos ou estouro da janela. Não há concorrência, carga ou conclusão automática sobre rate limiting.

Antes do protocolo final, a primeira tentativa foi interrompida no signup após 1/10 chamadas e 2.164 ms; uma segunda tentativa completou 10/10. Depois disso, um protocolo autorizado de três execuções independentes foi concluído com 3/3 aprovações:

| Execução | Chamadas | Maior latência | Tempo acumulado final | Resultado |
|---|---:|---:|---:|---|
| 1/3 | 10/10 | 1.774 ms | 5.078 ms | Pass |
| 2/3 | 10/10 | 1.026 ms | 4.728 ms | Pass |
| 3/3 | 10/10 | 934 ms | 4.352 ms | Pass |

A baseline controlada da Fase 5 está aprovada dentro desses limites. O resultado não demonstra rate limiting, resistência a carga ou ausência de vulnerabilidade, e a variação inicial permanece registrada.

## DAST passivo controlado

A baseline da Fase 6 foi executada em safe mode com imagem oficial fixada por digest e OpenAPI temporária contendo somente signup, login e dashboard. O resultado completo, as tentativas de orquestração e a revisão dos alertas estão em `docs/dast-results.md`.

A OpenAPI reduzida pode ser preparada offline pelo grupo Maven opt-in `tooling`, conforme `docs/dast-plan.md`. O gerador valida o SHA-256 da especificação oficial e grava somente em `target/zap/`; ele não inicia containers nem acessa a crAPI.

O gateway `DastTrafficGate` e o wrapper `scripts/Invoke-ControlledZapScan.ps1` aplicam os controles externos planejados. O gateway processa uma chamada por vez, encaminha somente os três método/paths permitidos, rejeita query strings e cabeçalhos `Authorization`, limita o corpo a 64 KiB e interrompe diante de operação fora do escopo, tentativa acima do orçamento, HTTP 5xx, indisponibilidade do upstream ou duração superior a dois minutos. Seu estado contém apenas motivo e contagem, sem payloads ou credenciais.

O wrapper funciona em modo de planejamento por padrão. A imagem oficial foi fixada e validada offline conforme `docs/zap-image-baseline.md`. Este exemplo apenas valida e exibe o plano, sem iniciar Docker ou acessar a crAPI:

```powershell
.\scripts\Invoke-ControlledZapScan.ps1 `
  -ImagemZap "ghcr.io/zaproxy/zaproxy@sha256:781a2bdaea47324e7bab583e2263f21d257b0aee61ed51521a5be45f5f5081ef" `
  -OrcamentoRequests 10
```

O modo de execução é protegido por `-Executar -Confirmacao AUTORIZO_DAST_PASSIVO`, aceita no máximo cinquenta requests e usa `--pull=never`. A execução autorizada consumiu 9/10 requests ao longo de três tentativas: as duas primeiras revelaram incompatibilidades de captura de processos no Windows PowerShell 5, corrigidas antes da tentativa final. A execução aceita encaminhou 3/4 requests, encerrou com código ZAP `0`, gerou quatro grupos informativos de risco `0` e não confirmou vulnerabilidades. Qualquer nova execução exige autorização e orçamento próprios.

## CI/CD Security

A Fase 7 separa dois checks de GitHub Actions:

- `CI Java / Testes unitários`: configura Java 17, executa `test-compile` e os testes com tag `unit`, sem acessar a crAPI.
- `Gitleaks / Segredos`: examina os commits do evento com Gitleaks 8.30.1 e não publica artefatos ou comentários.

As Actions estão fixadas por SHA completo, o `GITHUB_TOKEN` possui somente permissão de leitura e o Dependabot acompanha semanalmente Maven e GitHub Actions. Os dois checks passaram na primeira execução remota e agora são obrigatórios em `master`, inclusive para o administrador. Os cenários live não rodam no runner público porque a crAPI não é provisionada no pipeline. Consulte [docs/cicd-security.md](docs/cicd-security.md) para os controles, os resultados e a proteção aplicada à branch.

## Vulnerability Reporting e regressão

A Fase 8 define o caminho completo entre sinal, triagem, achado confirmado, correção e teste de regressão. O catálogo em [docs/vulnerabilities/README.md](docs/vulnerabilities/README.md) declara de forma verificável que ainda não há vulnerabilidade confirmada. O [template](docs/vulnerabilities/TEMPLATE.md) registra ambiente, contrato, impacto, evidências sanitizadas, correção e regressão sem armazenar dados sensíveis.

Evidências brutas ficam fora do Git conforme [docs/evidence-handling.md](docs/evidence-handling.md). Três testes unitários garantem a estrutura e ordem do template, a consistência entre catálogo e relatórios `SEC-AAAA-NNN.md`, a exclusão de `evidence/` e o caráter opt-in de `security-regression`.

Nenhum teste de regressão live foi criado: não há achado confirmado nem alvo corrigido que sustente um cenário real. Quando esses pré-requisitos existirem, a admissão e a execução seguirão [docs/regression-testing.md](docs/regression-testing.md).

## Contribuição e segurança

O fluxo de branches, validação e pull requests está em [CONTRIBUTING.md](CONTRIBUTING.md). Use issues públicas somente para defeitos não sensíveis deste repositório. Vulnerabilidades em seu código, workflows ou scripts devem seguir [SECURITY.md](SECURITY.md) e ser enviadas pelo relato privado habilitado no GitHub.

Dependabot alerts e security updates estão habilitados. Propostas automáticas nunca recebem merge automático: continuam sujeitas à política de versões, revisão e checks obrigatórios.

## Roadmap resumido

As baselines das Fases 1, 2, 4, 5 e 6 estão implementadas e validadas dentro dos limites registrados. A Fase 7 possui CI Java, detecção de segredos e atualização automatizada de dependências, com os dois checks validados no GitHub. A estrutura de gestão e regressão da Fase 8 está implementada, sem fabricar achados ou aprovações com zero testes. A Fase 3 permanece pendente pela restrição do ambiente. Nenhum cenário indisponível, teste destrutivo ou resultado fictício foi adicionado.
