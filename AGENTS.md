# Project instructions

- Use Java 17, Maven, JUnit 5, REST Assured, and minimal dependencies.
- Treat OWASP crAPI as an external black-box system; never copy or implement it here.
- Confirm every HTTP contract against the official `OWASP/crAPI` documentation or OpenAPI specification before coding.
- Resolve the target from `BASE_URL`, falling back to `http://localhost:8888`; never hardcode credentials or JWTs.
- Keep configuration, HTTP clients, test data, models, and tests separated without premature abstractions.
- Work incrementally: compile, run the smallest relevant test set, review the diff, then continue.
- Never mask an unavailable SUT as a passing test and do not log complete tokens.
- Validation commands: `mvn test-compile`, `mvn test -Dgroups=unit`, `mvn test -Dgroups=functional`, `mvn test -Dgroups=authentication`, `mvn test -Dgroups=input-validation`, `mvn test -Dgroups=security`, and `mvn test`.
- Keep detailed project and security documentation in `README.md`, `ROADMAP.md`, and `docs/`.
