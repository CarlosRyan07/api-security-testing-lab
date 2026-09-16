# CI/CD Security

## Escopo

A Fase 7 adiciona verificações automáticas reproduzíveis para compilação, testes unitários e detecção de segredos. Os workflows usam permissões mínimas de leitura, limite de tempo, cancelamento de execuções obsoletas e Actions fixadas pelo SHA completo do commit.

## CI Java

O workflow `.github/workflows/ci-java.yml` executa em `push` para `master`, pull requests e acionamento manual. Ele configura Temurin 17, compila os testes e executa somente o grupo `unit`:

```bash
mvn --batch-mode --no-transfer-progress test-compile
mvn --batch-mode --no-transfer-progress test -Dgroups=unit
```

Os testes funcionais e de segurança que acessam a crAPI não são executados no runner público, pois o sistema sob teste não é provisionado no workflow. Essa separação evita tanto acesso externo não autorizado quanto resultados mascarados por ausência do ambiente. A validação live continua local, explícita e sujeita aos limites documentados neste repositório.

## Detecção de segredos

O workflow `.github/workflows/gitleaks.yml` usa `gitleaks/gitleaks-action` v3, fixada por SHA, e Gitleaks 8.30.1. O checkout inclui todo o histórico para que a varredura não fique limitada ao snapshot atual. Comentários e upload de artefatos foram desabilitados para reduzir permissões e evitar publicação desnecessária de evidências.

O `GITHUB_TOKEN` é fornecido automaticamente pelo GitHub Actions e recebe apenas `contents: read`. Não há token, licença ou credencial persistida no repositório. Em repositórios pessoais, a Action não exige `GITLEAKS_LICENSE`; se o projeto for transferido para uma organização, essa exigência deve ser reavaliada conforme a documentação oficial vigente.

## Dependências automatizadas

O arquivo `.github/dependabot.yml` solicita atualizações semanais para Maven e GitHub Actions, limitado a cinco pull requests abertos por ecossistema. Toda atualização deve passar pelos mesmos checks e ser revisada antes do merge.

## Proteção recomendada da branch

Após a primeira execução bem-sucedida no GitHub, configure manualmente a proteção de `master` para exigir os checks:

- `CI Java / Testes unitários`
- `Gitleaks / Segredos`

Essa configuração pertence ao repositório remoto e não é presumida como aplicada por estes arquivos.

## Validação local

Antes de publicar alterações de pipeline:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn test-compile
mvn test -Dgroups=unit
```

Os workflows devem ser verificados com `actionlint`. A árvore de trabalho e o histórico Git devem ser examinados com Gitleaks usando `--redact`; nenhum achado deve ser ignorado ou convertido em sucesso.

## Referências oficiais

- [GitHub Actions: permissões do `GITHUB_TOKEN`](https://docs.github.com/en/actions/tutorials/authenticate-with-github_token)
- [actions/checkout](https://github.com/actions/checkout)
- [actions/setup-java](https://github.com/actions/setup-java)
- [Gitleaks Action](https://github.com/gitleaks/gitleaks-action)
- [Gitleaks](https://github.com/gitleaks/gitleaks)
- [Dependabot](https://docs.github.com/en/code-security/dependabot/dependabot-version-updates/configuration-options-for-the-dependabot.yml-file)
