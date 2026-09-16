# Instruções do projeto

- Use Java 17, Maven, JUnit 5, REST Assured e o mínimo de dependências.
- Trate a OWASP crAPI como um sistema externo e black-box; nunca copie nem implemente seu código neste repositório.
- Confirme todo contrato HTTP na documentação ou na especificação OpenAPI oficial de `OWASP/crAPI` antes de programar.
- Resolva o alvo por `BASE_URL`, com fallback para `http://localhost:8888`; nunca fixe credenciais ou JWTs no código.
- Mantenha configuração, clientes HTTP, dados de teste, modelos e testes separados, sem abstrações prematuras.
- Trabalhe incrementalmente: compile, execute o menor grupo relevante de testes, revise o diff e só então prossiga.
- Nunca transforme indisponibilidade do sistema sob teste em sucesso e não registre tokens completos.
- Use os comandos de validação: `mvn test-compile`, `mvn test -Dgroups=unit`, `mvn test -Dgroups=functional`, `mvn test -Dgroups=authentication`, `mvn test -Dgroups=input-validation`, `mvn test -Dgroups=security` e `mvn test`.
- O cenário controlado da Fase 5 é excluído por padrão e só pode ser executado após autorização explícita com `mvn test -Dgroups=resource-abuse "-Dexcluded.test.groups=__none__"`.
- Não execute OWASP ZAP sem autorização específica, OpenAPI temporária reduzida aos três endpoints permitidos, imagem fixada por digest e controles de interrupção descritos em `docs/dast-plan.md`.
- Gere a OpenAPI reduzida somente pelo teste opt-in documentado, mantendo o grupo `tooling` excluído da suíte padrão.
- Use `scripts/Invoke-ControlledZapScan.ps1` apenas em modo de planejamento até que imagem, orçamento e execução sejam autorizados explicitamente; o modo de execução exige a confirmação literal documentada.
- Mantenha a documentação detalhada do projeto e de segurança em `README.md`, `ROADMAP.md` e `docs/`.
