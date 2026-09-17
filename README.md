# API Security Testing Lab

Projeto de portfólio de QA Engineering para testes automatizados de API. O alvo é a [OWASP crAPI](https://github.com/OWASP/crAPI), executada separadamente como uma aplicação externa e deliberadamente vulnerável.

Este repositório não implementa nem contém o código da crAPI. Ele envia requisições para uma instância da aplicação e verifica respostas funcionais e controles de segurança dentro de um escopo autorizado.

Versão estável atual: `1.0.0`.

## O que este projeto faz

O fluxo funcional principal é:

1. Criar um usuário novo.
2. Cadastrá-lo na crAPI.
3. Fazer login com e-mail e senha.
4. Receber um JWT, sem gravá-lo em logs.
5. Acessar o dashboard com esse token.
6. Confirmar que o dashboard pertence ao usuário criado e possui o papel `ROLE_USER`.

Cada teste cria dados próprios. Assim, não depende de credenciais fixas nem de usuários cadastrados anteriormente.

Além do fluxo normal, o projeto cobre rejeições de autenticação, validação de entradas inválidas, uma verificação controlada de consumo de recursos, DAST passivo com OWASP ZAP e controles de CI/CD.

## Estrutura

```text
src/test/java/io/github/apisecurity/
├── client/       chamadas HTTP para a crAPI
├── config/       URL da aplicação e configuração HTTP comum
├── data/         criação de usuários de teste únicos
├── functional/   fluxo normal de cadastro, login e dashboard
├── model/        formatos dos dados enviados e recebidos
├── reporting/    verificações da documentação de vulnerabilidades
├── security/     cenários negativos permitidos
└── tooling/      preparação e controles do DAST passivo
```

Como é um projeto de testes, todo o código Java fica em `src/test/java`.

## Pré-requisitos

- JDK 17
- Maven 3.9 ou superior
- Docker e Docker Compose 1.27 ou superior
- Uma instância da OWASP crAPI da branch `main`

Confirme o Java usado pelo Maven:

```bash
mvn -version
```

## Executar a crAPI

Siga a [documentação oficial da OWASP](https://github.com/OWASP/crAPI#docker-and-docker-compose) para executar a aplicação fora deste repositório:

```bash
git clone --depth 1 --branch main https://github.com/OWASP/crAPI.git
cd crAPI/deploy/docker
docker compose pull
docker compose -f docker-compose.yml --compatibility up -d
```

Por padrão, a crAPI fica disponível em `http://localhost:8888`. Espere a inicialização completa antes de executar os testes.

## Configurar o endereço da API

Os testes usam a variável de ambiente `BASE_URL`. Sem ela, usam `http://localhost:8888`.

PowerShell:

```powershell
$env:BASE_URL = "http://localhost:8888"
```

Bash:

```bash
export BASE_URL=http://localhost:8888
```

O valor precisa ser uma URL HTTP ou HTTPS válida. Se a crAPI não estiver disponível, os testes live falham explicitamente; indisponibilidade não é tratada como sucesso.

## Comandos principais

Compilar sem acessar a crAPI:

```bash
mvn test-compile
```

Executar testes unitários, sem acessar a crAPI:

```bash
mvn test -Dgroups=unit
```

Executar o fluxo funcional de cadastro, login e dashboard:

```bash
mvn test -Dgroups=functional
```

Executar as verificações de segurança permitidas por padrão:

```bash
mvn test -Dgroups=security
```

Executar a suíte completa atual:

```bash
mvn test
```

Há grupos opcionais que não são executados por padrão:

- `resource-abuse`: verificação de baixo volume da Fase 5. Só execute com autorização explícita:

  ```bash
  mvn test -Dgroups=resource-abuse "-Dexcluded.test.groups=__none__"
  ```

- `tooling`: prepara artefatos locais para o DAST; não inicia a crAPI nem containers.
- `security-regression`: reservado para uma vulnerabilidade real, confirmada e corrigida. No momento não há caso elegível.

## O que cada teste verifica

| Grupo | Verificação |
|---|---|
| `functional` | Cadastro, login, JWT e acesso ao próprio dashboard. |
| `authentication` | Login com credenciais inválidas, dashboard sem token e token malformado. |
| `input-validation` | Campos obrigatórios ausentes ou nulos, JSON com formato inválido e cadastro duplicado. |
| `resource-abuse` | Até dez requisições sequenciais em até dez segundos, com interrupção diante de erro 5xx ou latência acima do limite. Não é teste de carga. |
| `tooling` | Geração offline de uma OpenAPI reduzida para o DAST. |
| `security-regression` | Testes adicionados apenas após uma correção real de vulnerabilidade. |

## Estado do trabalho

| Fase | Situação |
|---|---|
| 0 — Base do projeto | Concluída: Maven, Java 17, configuração e documentação inicial. |
| 1 — Fluxo funcional | Concluída e validada contra a crAPI. |
| 2 — Autenticação | Concluída: rejeições de credenciais e tokens inválidos. |
| 3 — Autorização/BOLA | Pendente por restrição de ambiente e ferramenta. |
| 4 — Validação de entrada | Concluída dentro do escopo permitido. |
| 5 — Consumo de recursos | Concluída com baseline controlada; não incluiu carga, concorrência, negação de serviço nem validação de rate limiting. |
| 6 — DAST passivo | Concluída dentro do orçamento autorizado; não houve vulnerabilidade confirmada. |
| 7 — CI/CD | Concluída: testes unitários e detecção de segredos no GitHub Actions. |
| 8 — Relato e regressão | Estrutura concluída; depende de um achado confirmado e corrigido para gerar uma regressão real. |

### Sobre BOLA

BOLA significa *Broken Object Level Authorization* — uma falha em que uma API permite que uma pessoa acesse ou altere um objeto que pertence a outra, como o pedido, perfil ou documento de outro usuário. A Fase 3 existe para investigar esse tipo de controle de acesso, mas não foi implementada por causa da restrição registrada no [ROADMAP.md](ROADMAP.md). Nenhuma vulnerabilidade foi inventada ou marcada como confirmada.

## Segurança e limites

- Não há JWTs, senhas ou credenciais fixas no código.
- Requests e respostas não são registrados automaticamente para não expor tokens.
- A Fase 5 foi executada somente com volume baixo e limites de tempo definidos.
- Uma nova execução de DAST requer autorização e orçamento próprios.
- Resultados sem execução real não são tratados como vulnerabilidades confirmadas.

## Documentação detalhada

- [ROADMAP.md](ROADMAP.md): status e histórico das fases.
- [Plano de testes de segurança](docs/security-test-plan.md): escopo, exclusões e critérios.
- [Baseline do contrato](docs/contract-baseline.md): referência à OpenAPI oficial usada.
- [Resultado do DAST](docs/dast-results.md): execução passiva da Fase 6.
- [CI/CD e Gitleaks](docs/cicd-security.md): controles automatizados do repositório.
- [Gestão de vulnerabilidades](docs/vulnerability-management.md): como registrar um achado real.

## Contribuir ou relatar um problema

As regras de contribuição estão em [CONTRIBUTING.md](CONTRIBUTING.md). Para uma vulnerabilidade no código, workflow ou script deste repositório, siga [SECURITY.md](SECURITY.md) e use o relato privado do GitHub. Não publique tokens, senhas ou evidências sensíveis em issues públicas.
