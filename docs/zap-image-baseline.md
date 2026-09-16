# Baseline da imagem OWASP ZAP

## Fonte e captura

- Registro oficial: `ghcr.io/zaproxy/zaproxy`
- Tag consultada: `stable`
- Data da resolução: `2026-09-16`
- Plataforma local: `linux/amd64`
- Digest imutável do índice OCI: `sha256:781a2bdaea47324e7bab583e2263f21d257b0aee61ed51521a5be45f5f5081ef`
- Manifesto `linux/amd64`: `sha256:71db37cd5b75663b35758d10aaec05bf6fbac23f5020e3046c70e628a5f84efa`

Referência fixada:

```text
ghcr.io/zaproxy/zaproxy@sha256:781a2bdaea47324e7bab583e2263f21d257b0aee61ed51521a5be45f5f5081ef
```

A tag `stable` foi usada somente para descobrir o índice publicado. O pull e toda futura execução devem usar a referência imutável acima.

## Validação local

A imagem foi baixada pelo digest do índice e o Docker selecionou o manifesto `linux/amd64`. Metadados conferidos após o pull:

- image ID: `sha256:6175579a46d477338e4b641dd9c3428936f30c3719809e2f4d36f58e60c4ddb7`;
- sistema/arquitetura: `linux/amd64`;
- criação declarada: `2026-08-07T14:58:54.08095198Z`;
- tamanho local declarado: `2399736675` bytes;
- `RepoDigests`: somente a referência imutável registrada nesta baseline.

O comando `zap-api-scan.py -h` foi executado em container descartável com `--network none` e `--pull=never`. Ele terminou com código `0` e confirmou a presença das opções planejadas `-S`, `-T`, `-O`, `-s`, `-J` e `-w`. Esse comando não iniciou scan, não montou a OpenAPI e não acessou a crAPI.

Uma tentativa adicional com `zap.sh -version` não foi aceita como evidência: mesmo com a rede desativada, o comando iniciou o bootstrap local do ZAP, registrou `UnknownHostException` ao tentar resolver o hostname efêmero e precisou ser interrompido. Nenhum alvo foi configurado ou acessado nessa tentativa.

## Estado

A imagem está disponível localmente e fixada por digest. A baseline passiva autorizada posteriormente está registrada em `docs/dast-results.md`. Qualquer nova execução exige nova autorização e orçamento.
