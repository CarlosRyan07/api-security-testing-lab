# Plano de DAST com OWASP ZAP

## Estado

Preparação estrutural concluída. Nenhuma imagem ZAP foi baixada e nenhum scan foi executado.

## Fontes oficiais

- [ZAP API Scan](https://www.zaproxy.org/docs/docker/api-scan/)
- [ZAP Docker User Guide](https://www.zaproxy.org/docs/docker/about/)
- [ZAP Automation Framework](https://www.zaproxy.org/docs/automate/automation-framework/)

O `zap-api-scan.py` executa active scan por padrão. A opção `-S` pula o active scan e realiza uma baseline passiva. A importação da OpenAPI ainda pode enviar chamadas ao alvo; portanto, safe mode não significa execução sem tráfego.

## Escopo permitido

Somente os contratos já implementados e confirmados na baseline oficial:

- `POST /identity/api/auth/signup`
- `POST /identity/api/auth/login`
- `GET /identity/api/v2/user/dashboard`

A OpenAPI completa da crAPI não será fornecida ao ZAP, pois contém operações fora do escopo atual. Antes do scan, uma especificação temporária deve ser gerada em `target/zap/scoped-openapi.json` a partir da fonte oficial identificada em `docs/contract-baseline.md`. O arquivo não será versionado e deverá conter exatamente os três paths e métodos listados acima.

O gerador offline está em `ScopedOpenApiGeneratorTest`, marcado com `@Tag("tooling")` e excluído da suíte padrão. Ele grava somente em `target/zap/scoped-openapi.json`, valida o SHA-256 da fonte oficial e falha se algum dos três contratos esperados estiver ausente.

Exemplo, sem executar o ZAP:

```powershell
mvn test -Dgroups=tooling "-Dexcluded.test.groups=__none__" `
  "-Dopenapi.spec=C:\caminho\para\crAPI\openapi-spec\crapi-openapi-spec.json"
```

## Modo planejado

- Imagem oficial `ghcr.io/zaproxy/zaproxy:stable`, fixada pelo digest obtido no momento autorizado.
- `zap-api-scan.py` com formato `openapi` e opção `-S`.
- Sem active scan, regras alpha, spider adicional, fuzzing ou autenticação automatizada.
- Host local acessado pelo container via `host.docker.internal` e opção oficial de override de hostname.
- Limite planejado de dois minutos para inicialização e passive scan.
- Saída curta, sem exemplos de URLs, para reduzir exposição acidental.
- Relatórios JSON e Markdown somente em `target/zap/`, já ignorado pelo Git.
- Nenhum JWT, senha ou credencial será incluído em arquivos, argumentos ou relatórios versionados.

## Controles obrigatórios antes da execução

1. Confirmar que a crAPI local e seus containers estão saudáveis.
2. Fixar e registrar o digest da imagem ZAP oficial.
3. Gerar a OpenAPI reduzida sem alterar a fonte oficial usando o grupo opt-in `tooling`.
4. Validar automaticamente que existem somente três paths e três operações; este controle já é aplicado pelo gerador.
5. Implementar monitor externo para interromper o container diante de HTTP 5xx, ambiente não saudável, duração acima de dois minutos ou orçamento de requests excedido.
6. Definir e autorizar explicitamente o orçamento máximo de requests.
7. Revisar os relatórios quanto a tokens, credenciais e dados de usuário antes de registrar qualquer resultado.

Sem esses controles, o scan não deve ser iniciado.

## Resultado e códigos de saída

Segundo a documentação oficial do API Scan:

- `0`: sucesso;
- `1`: ao menos um alerta configurado como falha;
- `2`: ao menos um alerta e nenhuma falha;
- `3`: erro de execução.

Warnings não serão convertidos artificialmente em sucesso. Alertas serão classificados somente após revisão manual, e nenhum alerta isolado será registrado como vulnerabilidade confirmada.

## Critério de conclusão da Fase 6

- execução explicitamente autorizada e limitada;
- nenhuma operação fora dos três endpoints permitidos;
- relatório gerado sem segredo ou JWT;
- alertas revisados e classificados como confirmado, falso positivo ou pendente;
- comandos, digest, limites e resultado documentados;
- suíte Maven padrão ainda aprovada após a execução.
