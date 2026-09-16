# Security Test Plan

## Objetivo

Evoluir uma baseline funcional confiável da OWASP crAPI para testes automatizados de segurança de API, com cenários rastreáveis, baixo volume de requests e evidências reproduzíveis.

## Escopo

- APIs HTTP publicadas pela crAPI oficial.
- Autenticação e JWT.
- Autorização em nível de objeto e função.
- Validação de propriedades, tipos e limites.
- Consumo de recursos em volume baixo e controlado.
- DAST orientado pela OpenAPI em fase posterior.
- Security regression tests após correções hipotéticas ou reais.

## Fora de escopo

- Implementação ou alteração do código da crAPI.
- Testes contra sistemas que não sejam explicitamente autorizados.
- Carga, negação de serviço ou tráfego destrutivo.
- Exploração automática sem cenário documentado.
- ZAP, Gitleaks e pipelines durante as Fases 0 e 1.

## Abordagem

1. Confirmar o contrato na documentação e OpenAPI oficiais.
2. Estabelecer o comportamento funcional esperado.
3. Criar cenário de segurança pequeno, independente e nomeado pelo objetivo.
4. Executar com dados dinâmicos e volume mínimo.
5. Registrar resultado e evidência sem expor credenciais ou tokens completos.
6. Classificar separadamente reprodução de vulnerabilidade e regressão de comportamento seguro.

## Ambiente

- crAPI oficial da branch `main`, executada externamente por Docker Compose.
- URL fornecida por `BASE_URL`; fallback local `http://localhost:8888`.
- Java 17, Maven, JUnit 5 e REST Assured.
- Dados de usuário gerados por teste.
- Resolução de `BASE_URL` coberta por testes unitários que não acessam o sistema sob teste.

## Categorias

- Baseline funcional.
- Authentication Security.
- Authorization Security: BOLA e BFLA quando aplicáveis.
- Object Property and Input Security: BOPLA, mass assignment e parameter tampering.
- Resource Consumption e rate limiting.
- DAST orientado pela OpenAPI.

## Execução atual

A baseline funcional valida o fluxo completo de signup, login e dashboard. A resposta autenticada do dashboard é JSON e cobre identidade, papel, presença das dez propriedades obrigatórias e tipos numéricos declarados na OpenAPI oficial.

A baseline não destrutiva de Authentication Security da Fase 2 cobre quatro rejeições de baixo impacto: credenciais desconhecidas, senha incorreta para usuário existente, dashboard sem Bearer Token e dashboard com JWT malformado. Os cenários usam apenas endpoints documentados e foram validados contra a branch oficial `main`. Respostas de login rejeitadas não podem conter token, e respostas rejeitadas do dashboard não podem conter seus campos de identidade.

O retorno `401` observado para credenciais desconhecidas não consta entre as respostas `200` e `500` declaradas pela OpenAPI do login. Essa diferença é tratada como drift de contrato e não como vulnerabilidade confirmada. As chamadas não autenticadas ao dashboard retornaram `404`, resposta prevista pela operação na OpenAPI.

Brute force de OTP, reset de senha de terceiros e manipulação avançada de JWT permanecem excluídos desta baseline por serem técnicas de exploração, e não validações negativas mínimas.

## Restrição da Fase 3

A execução prática de autorização horizontal/BOLA está pendente por limitação do ambiente/ferramenta. Não há teste automatizado, evidência ou vulnerabilidade validada para a Fase 3. Enquanto a restrição permanecer, somente preparação estrutural, documentação e cobertura funcional ou negativa permitida serão realizadas.

## Execução permitida da Fase 4

A baseline de validação de entrada cobre cadastro duplicado; objeto vazio; corpo JSON array, string, número ou booleano; ausência individual e valor `null` em cada campo obrigatório documentado no signup e login. O cadastro duplicado retornou `403`; os dois objetos vazios, as oito variações de tipo raiz, as seis com campo ausente e as seis com valor nulo retornaram `400`. A OpenAPI não lista `400` nas respostas dessas operações, portanto a diferença é registrada como drift de contrato, não como vulnerabilidade confirmada.

Não há cobertura executada de mass assignment, parameter tampering ou autorização de propriedades.

## Preparação da Fase 5

Resource Abuse permanece somente planejada. Antes de qualquer execução serão necessários orçamento máximo de requests, janela de tempo, critério de interrupção, endpoint oficialmente confirmado e autorização explícita. Carga e negação de serviço continuam fora de escopo.

## Ferramentas

- REST Assured e JUnit 5 para automação HTTP.
- Docker Compose para o SUT externo.
- OWASP ZAP, GitHub Actions e Gitleaks apenas nas fases futuras previstas.

## Critérios

- Todo endpoint, método, payload e status esperado deve ter fonte oficial confirmada.
- A indisponibilidade do SUT deve falhar explicitamente.
- Testes funcionais e reproduções de vulnerabilidade devem ter nomes e expectativas distintas.
- Nenhuma evidência pode conter JWT completo, credencial real ou outro segredo.
- Falhas do código de teste devem ser corrigidas antes de ampliar o escopo.

## Riscos

- Mudanças entre versões da crAPI podem alterar contratos e comportamentos.
- A natureza vulnerável do SUT pode produzir resultados diferentes de uma API protegida.
- Estado persistente ou serviços ainda inicializando podem afetar resultados.
- Logs e relatórios podem expor dados sensíveis se não forem revisados.
- Cenários de abuso podem causar impacto se o volume não for estritamente controlado.
