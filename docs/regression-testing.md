# Security Regression Testing

## Estado atual

Não existe teste de regressão de vulnerabilidade neste repositório. Nenhum achado foi confirmado e nenhuma versão corrigida correspondente está disponível para comparação. Uma execução com zero testes nunca será registrada como aprovação de segurança.

## Critérios de admissão

Um teste só entra no grupo `security-regression` quando:

1. existe relatório `SEC-AAAA-NNN.md` confirmado e catalogado;
2. o comportamento seguro esperado está definido sem inventar contrato;
3. existe alvo corrigido identificável e autorizado;
4. a reprodução pode ser automatizada com volume baixo, dados dinâmicos e sem conteúdo destrutivo;
5. o teste falha pelo motivo correto no alvo vulnerável aplicável e passa no alvo corrigido;
6. relatório, matriz OWASP e documentação de execução apontam para a classe e o método do teste.

## Convenção

- Package: `io.github.apisecurity.regression`.
- Tags: `@Tag("security")` e `@Tag("security-regression")`.
- Nome: comportamento protegido, por exemplo `shouldReject...`, nunca o nome de uma técnica de ataque.
- Dados: usuário dinâmico e independente; nenhum JWT ou identificador fixo.
- Asserções: status e propriedades sustentadas pelo contrato e pela correção verificada.
- Falha ambiental: explícita, sem skip ou fallback para sucesso.

O grupo `security-regression` é excluído da suíte padrão porque a crAPI oficial deliberadamente vulnerável não equivale a um alvo corrigido. Quando houver ao menos um caso elegível, a execução explícita será:

```bash
mvn test -Dgroups=security-regression "-Dexcluded.test.groups=__none__"
```

O resultado só é aceito quando o Surefire informar pelo menos um teste executado. O CI público não executa esse grupo sem provisionamento explícito do SUT corrigido.

## Evidência de regressão

O relatório deve registrar versões dos dois alvos, comando, quantidade de testes, resultado e data. Logs completos permanecem fora do Git quando contiverem dados sensíveis. Se a validação contra o alvo vulnerável não for segura ou autorizada, essa lacuna deve ser documentada e o status não pode avançar para `Encerrada`.
