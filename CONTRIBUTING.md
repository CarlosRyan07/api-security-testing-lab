# Como contribuir

Obrigado pelo interesse em melhorar o API Security Testing Lab. Este repositório testa a OWASP crAPI como sistema externo e black-box; ele não contém nem altera o código da aplicação alvo.

## Antes de começar

- Leia `AGENTS.md`, `README.md` e `docs/security-test-plan.md`.
- Use JDK 17 e Maven 3.9 ou superior.
- Confirme contratos HTTP somente na documentação ou OpenAPI oficial da crAPI.
- Nunca inclua credenciais, JWTs, cookies, dados pessoais ou evidências brutas no Git.
- Não execute cenários opt-in, DAST ou consumo de recursos sem os limites e autorizações documentados.

## Fluxo de contribuição

1. Atualize `master` e crie uma branch curta, como `feat/...`, `fix/...`, `test/...`, `docs/...` ou `chore/...`.
2. Faça uma mudança coesa e preserve a separação entre configuração, clientes, dados, modelos, testes e tooling.
3. Use mensagens de commit em português e descreva o resultado, não apenas a atividade.
4. Execute a menor validação relevante durante o desenvolvimento.
5. Antes do pull request, execute compilação, testes unitários e os testes live aplicáveis.
6. Revise o diff e faça uma varredura de segredos com valores redigidos.
7. Abra o pull request preenchendo o template e aguarde os checks obrigatórios.

## Comandos básicos

```bash
mvn test-compile
mvn test -Dgroups=unit
mvn test
```

Os testes live usam `BASE_URL`, com fallback para `http://localhost:8888`, e falham explicitamente se a crAPI estiver indisponível. O runner público executa somente compilação e testes unitários porque não provisiona o SUT.

Grupos `resource-abuse`, `tooling` e `security-regression` são opt-in. Não remova suas exclusões nem execute esses grupos sem cumprir os pré-requisitos documentados.

## Pull requests

Todo pull request deve:

- explicar objetivo, escopo e validação realizada;
- adicionar somente endpoints e contratos confirmados em fonte oficial;
- atualizar README, roadmap, matriz ou planos quando o comportamento documentado mudar;
- manter `Testes unitários` e `Segredos` aprovados;
- preservar histórico linear e resolver todas as conversas antes do merge.

Um check verde não autoriza mudança major de dependência, expansão de escopo ou resultado de segurança sem evidência.

## Bugs e segurança

Use o formulário de issue somente para defeitos deste projeto, como falhas na suíte, documentação ou automação. Vulnerabilidades no código ou infraestrutura deste repositório devem seguir `SECURITY.md` e ser reportadas de forma privada. Comportamentos da aplicação crAPI pertencem ao projeto upstream quando não forem apenas resultados deste laboratório.
