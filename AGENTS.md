# Instruções do projeto

- Use Java 17, Maven, JUnit 5, REST Assured e o mínimo de dependências.
- Trate a OWASP crAPI como um sistema externo e black-box; nunca copie nem implemente seu código neste repositório.
- Confirme todo contrato HTTP na documentação ou na especificação OpenAPI oficial de `OWASP/crAPI` antes de programar.
- Resolva o alvo por `BASE_URL`, com fallback para `http://localhost:8888`; nunca fixe credenciais ou JWTs no código.
- Mantenha configuração, clientes HTTP, dados de teste, modelos e testes separados, sem abstrações prematuras.
- Trabalhe incrementalmente: compile, execute o menor grupo relevante de testes, revise o diff e só então prossiga.
- Nunca transforme indisponibilidade do sistema sob teste em sucesso e não registre tokens completos.
- Use os comandos de validação: `mvn test-compile`, `mvn test -Dgroups=unit`, `mvn test -Dgroups=functional`, `mvn test -Dgroups=authentication`, `mvn test -Dgroups=input-validation`, `mvn test -Dgroups=security` e `mvn test`.
- Mantenha a documentação detalhada do projeto e de segurança em `README.md`, `ROADMAP.md` e `docs/`.
