# Plano de DAST com OWASP ZAP

## Estado

Baseline passiva concluída conforme `docs/dast-results.md`. A imagem oficial permanece fixada conforme `docs/zap-image-baseline.md`. Novas execuções exigem autorização e orçamento próprios.

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

- Imagem oficial resolvida a partir de `stable` e fixada como `ghcr.io/zaproxy/zaproxy@sha256:781a2bdaea47324e7bab583e2263f21d257b0aee61ed51521a5be45f5f5081ef`.
- `zap-api-scan.py` com formato `openapi` e opção `-S`.
- Sem active scan, regras alpha, spider adicional, fuzzing ou autenticação automatizada.
- Host local acessado pelo container via `host.docker.internal` e opção oficial de override de hostname.
- Limite planejado de dois minutos para inicialização e passive scan.
- Saída curta, sem exemplos de URLs, para reduzir exposição acidental.
- Relatórios JSON e Markdown somente em `target/zap/`, já ignorado pelo Git.
- Nenhum JWT, senha ou credencial será incluído em arquivos, argumentos ou relatórios versionados.

## Gateway e orquestração

`DastTrafficGate` atua entre o ZAP e a crAPI. Ele processa uma chamada por vez, possui allowlist exata para os três método/paths autorizados, não aceita query strings nem `Authorization`, limita cada corpo a 64 KiB e não grava payloads. Toda tentativa recebida entra na contagem; a chamada que ultrapassa o orçamento é rejeitada sem chegar à crAPI. A URL upstream deve apontar para a raiz HTTP/HTTPS e não pode conter credenciais.

O gateway encerra com motivo próprio diante de orçamento excedido, operação fora do escopo, cabeçalho de autenticação, HTTP 5xx, falha de conexão ou timeout. O estado gravado em `target/zap/gate-status.json` contém somente motivo, total recebido e orçamento. A interface de controle é vinculada ao loopback, enquanto a porta de tráfego fica disponível ao Docker.

O wrapper `scripts/Invoke-ControlledZapScan.ps1`:

- opera em modo de planejamento por padrão, sem iniciar processos ou fazer chamadas HTTP;
- exige imagem GHCR fixada por digest, orçamento explícito de 1 a 50 requests e duração de no máximo dois minutos;
- nunca baixa imagens e usa `docker run --pull=never`;
- exige `-Executar -Confirmacao AUTORIZO_DAST_PASSIVO` para iniciar o fluxo;
- verifica antes e durante a execução os containers informados em `-ContainersSaude`;
- executa somente `zap-api-scan.py -S`, sem regras alpha ou autenticação automatizada;
- interrompe exclusivamente o container ZAP nomeado pela própria execução;
- remove somente os oito artefatos temporários conhecidos antes de executar, evitando confusão com resultados antigos;
- registra o código oficial em `target/zap/zap-exit-code.txt` antes do cleanup;
- preserva os códigos de saída `0`, `1`, `2` e `3` do API Scan;
- mantém status, logs e relatórios apenas em `target/zap/`.

Exemplo seguro de planejamento com o digest fixado:

```powershell
.\scripts\Invoke-ControlledZapScan.ps1 `
  -ImagemZap "ghcr.io/zaproxy/zaproxy@sha256:781a2bdaea47324e7bab583e2263f21d257b0aee61ed51521a5be45f5f5081ef" `
  -OrcamentoRequests 10
```

O modo live não deve ser usado enquanto orçamento e autorização específica não forem registrados.

## Controles obrigatórios antes da execução

1. Confirmar que a crAPI local e seus containers estão saudáveis.
2. Fixar e registrar o digest da imagem ZAP oficial.
3. Gerar a OpenAPI reduzida sem alterar a fonte oficial usando o grupo opt-in `tooling`.
4. Validar automaticamente que existem somente três paths e três operações; este controle já é aplicado pelo gerador.
5. Usar o gateway e o wrapper já implementados para interromper o container diante de HTTP 5xx, ambiente não saudável, duração acima de dois minutos ou orçamento de requests excedido.
6. Definir e autorizar explicitamente o orçamento máximo de requests.
7. Revisar os relatórios quanto a tokens, credenciais e dados de usuário antes de registrar qualquer resultado.

Sem esses controles, o scan não deve ser iniciado.

Os itens 2, 3, 4 e 5 foram implementados e validados offline. Os itens 1 e 6 devem ser novamente confirmados para cada execução live; o item 7 só pode ocorrer depois de uma execução autorizada. Uma futura atualização da imagem exige nova resolução e revisão explícitas, sem substituir silenciosamente o digest desta baseline.

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

Todos os critérios foram atendidos pela execução registrada em `docs/dast-results.md`. A conclusão se limita à baseline passiva dos três endpoints permitidos e não demonstra ausência geral de vulnerabilidades.
