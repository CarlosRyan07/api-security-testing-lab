# API Security Testing Lab

Projeto de portfólio de QA Engineering para automação black-box de APIs e evolução progressiva até Security Regression Testing e DevSecOps. O sistema sob teste é a [OWASP crAPI](https://github.com/OWASP/crAPI), uma aplicação externa e deliberadamente vulnerável.

Este repositório não contém nem reimplementa a crAPI. A etapa atual cobre o bootstrap, o fluxo funcional mínimo e as baselines negativas permitidas de autenticação e validação de entrada.

## Stack

- Java 17
- Maven
- JUnit 5
- REST Assured
- Jackson
- Docker e Docker Compose para executar a crAPI separadamente

## Arquitetura

O código de teste separa configuração, clientes HTTP, contratos de payload, geração de dados e cenários funcionais:

```text
src/test/java/io/github/apisecurity/
├── client/       # Chamadas HTTP para autenticação e usuário
├── config/       # URL, especificação HTTP comum e testes isolados
├── data/         # Massa dinâmica e independente
├── functional/   # Cenários de happy path
├── model/        # Payloads e respostas necessários
├── security/     # Cenários negativos permitidos
└── tooling/      # Preparação offline e opt-in de artefatos
```

Detalhes das próximas fases estão em [ROADMAP.md](ROADMAP.md), a estratégia de segurança está em [docs/security-test-plan.md](docs/security-test-plan.md), a versão exata da OpenAPI consultada está em [docs/contract-baseline.md](docs/contract-baseline.md) e a preparação da Fase 6 está em [docs/dast-plan.md](docs/dast-plan.md).

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

## Preparação de DAST

A Fase 6 possui apenas planejamento estrutural. O ZAP não foi instalado nem executado. O futuro scan deverá usar safe mode, imagem oficial fixada por digest e uma OpenAPI temporária contendo somente signup, login e dashboard. Mesmo em safe mode, a importação pode gerar tráfego; por isso, a execução depende de orçamento e autorização próprios.

A OpenAPI reduzida pode ser preparada offline pelo grupo Maven opt-in `tooling`, conforme `docs/dast-plan.md`. O gerador valida o SHA-256 da especificação oficial e grava somente em `target/zap/`; ele não inicia containers nem acessa a crAPI.

## Roadmap resumido

As baselines das Fases 1, 2, 4 e 5 estão implementadas e validadas dentro dos limites registrados. A Fase 3 permanece pendente pela restrição do ambiente, a Fase 6 possui somente preparação estrutural e as Fases 7–8 continuam planejadas. Nenhum cenário indisponível, teste destrutivo ou resultado fictício foi adicionado.
