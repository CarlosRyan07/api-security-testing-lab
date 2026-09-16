# Resultado da baseline DAST passiva

## Autorização e limites

- Data: `2026-09-16`
- Alvo: crAPI oficial `main` em `http://localhost:8888`
- Modo: OWASP ZAP API Scan com `-S`, sem active scan, regras alpha ou autenticação automatizada
- Imagem: `ghcr.io/zaproxy/zaproxy@sha256:781a2bdaea47324e7bab583e2263f21d257b0aee61ed51521a5be45f5f5081ef`
- Orçamento DAST acumulado autorizado: 10 requests
- Duração máxima por tentativa: 2 minutos
- Escopo: somente signup, login e dashboard documentados em `docs/dast-plan.md`

## Tentativas

| Tentativa | Limite disponível | Requests encaminhados | Resultado |
|---|---:|---:|---|
| 1 | 10 | 3 | ZAP concluiu e produziu resumo sem `FAIL` ou `WARN`, mas o cleanup do PowerShell mascarou o código final; não aceita como execução final |
| 2 | 7 | 3 | ZAP e gateway concluíram, mas o PowerShell não materializou os códigos dos processos redirecionados; não aceita como execução final |
| 3 | 4 | 3 | Aceita: gateway `COMPLETED`, código oficial ZAP `0` capturado e relatórios gerados |

O total acumulado foi 9/10 requests. As duas inconsistências de orquestração foram corrigidas antes da tentativa aceita. Nenhuma tentativa ultrapassou seu limite, gerou HTTP 5xx, perdeu a saúde do ambiente ou acessou operação fora da allowlist.

## Resultado aceito

- Gateway: `COMPLETED`, 3/4 requests.
- ZAP: código `0`.
- Resumo no nível `WARN`: `FAIL-NEW: 0`, `WARN-NEW: 0`, `INFO: 0`, `PASS: 118`.
- URLs importadas internamente pelo ZAP: 9; isso não representa tráfego encaminhado. O gateway registrou somente três requests enviados à crAPI.
- Respostas observadas nos itens informativos: dashboard `404`, login `400` e signup `400`.
- Suíte Maven pós-scan: 51 testes, 0 falhas, 0 erros e 0 skips.
- `api.mypremiumdealership.com` e `crapi-identity` permaneceram saudáveis.

`PASS: 118` representa regras passivas avaliadas, não quantidade de requests.

## Revisão dos alertas

O relatório JSON contém quatro grupos de risco informativo (`riskcode 0`), embora o resumo curto filtrado em nível `WARN` não os conte:

| Plugin | Observação | Instâncias | Classificação |
|---|---|---:|---|
| `100000` | Client Error response code | 3 | Comportamento esperado dos payloads genéricos e chamada sem autenticação; não é vulnerabilidade confirmada |
| `10111` | Authentication Request Identified | 1 | Identificação esperada do endpoint de login; não é vulnerabilidade |
| `10049` | Non-Storable Content | 2 | Observação informativa sobre respostas de login e signup; não é vulnerabilidade |
| `10049` | Storable and Cacheable Content | 1 | Observação sobre a resposta `404` não autenticada do dashboard; sem conteúdo sensível observado e sem vulnerabilidade confirmada |

Nenhuma vulnerabilidade foi confirmada por esta baseline. Não foi criada entrada em `docs/vulnerabilities/`.

## Evidências

Os relatórios permanecem fora do Git em `target/zap/`:

- `zap-report.json`: SHA-256 `D7DB8799EFA794771DCA851E8DD5F50F401F26D9243F8FF6146E8C299B88BF02`
- `zap-report.md`: SHA-256 `81080737359E664CCDDB85E9368345AC253F193C8FF20EC0267E3DC3A40FC1B7`

A revisão automática não encontrou padrões de JWT, Bearer token, campo `token` ou campo `password` nos dois relatórios. Os resultados não são evidência de ausência geral de vulnerabilidades: cobrem somente três operações, modo passivo e o orçamento autorizado.
