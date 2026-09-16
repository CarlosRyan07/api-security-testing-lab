# Baseline do contrato oficial

Este documento identifica a versão da fonte oficial usada para implementar e validar os contratos HTTP do projeto. Ele não substitui nem copia a OpenAPI da crAPI.

## Fonte validada

- Repositório: `https://github.com/OWASP/crAPI.git`
- Branch consultada: `main`
- Commit: `700f03d12a392d9e408260b4beae72ed02a4a1a4`
- Arquivo: `openapi-spec/crapi-openapi-spec.json`
- SHA-256 do arquivo: `1CDC4B7D21F44E3EF62293EA8732210341A8AA5CA261F060DC38CB80540801C6`
- Data da conferência: 16 de setembro de 2026

O commit identifica o código e o Docker Compose consultados. As imagens referenciadas como `latest` pelo ambiente oficial podem mudar independentemente desse commit; por isso, os resultados live sempre registram também o comportamento observado.

## Contratos implementados

| Operação | Corpo ou autenticação | Respostas declaradas utilizadas |
|---|---|---|
| `POST /identity/api/auth/signup` | JSON objeto; `email`, `name`, `number` e `password` obrigatórios | `200`, `403`, `500` |
| `POST /identity/api/auth/login` | JSON objeto; `email` e `password` obrigatórios | `200`, `500` |
| `GET /identity/api/v2/user/dashboard` | Bearer JWT | `200`, `404` |

No dashboard, a resposta `200` exige `available_credit`, `email`, `id`, `name`, `number`, `picture_url`, `role`, `video_id`, `video_name` e `video_url`. O projeto valida presença, identidade e os tipos explicitamente declarados, sem impor restrições ausentes do schema.

## Comportamento live observado

Na instância oficial executada localmente:

- signup válido retornou `200`;
- signup duplicado retornou `403`;
- login válido retornou `200` e JWT não vazio;
- dashboard autenticado retornou `200` e os dados do usuário criado;
- credenciais desconhecidas ou senha incorreta retornaram `401`;
- dashboard sem token ou com token malformado retornou `404`;
- payloads inválidos cobertos pela baseline de entrada retornaram `400`.

Os status `400` de signup/login e `401` de login não estão declarados nas respectivas operações da OpenAPI consultada. Eles são tratados como drift de contrato observado, não como vulnerabilidades confirmadas.

## Regra de atualização

Antes de implementar um novo endpoint ou após atualizar a crAPI:

1. conferir o contrato na OpenAPI oficial;
2. registrar o novo commit e o SHA-256 da especificação;
3. revisar endpoints, schemas e respostas afetados;
4. executar primeiro os testes relevantes e depois a suíte completa;
5. atualizar somente resultados realmente observados, sem inferir evidências.
