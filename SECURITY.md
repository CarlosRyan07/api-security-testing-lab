# Política de segurança

## Versões suportadas

| Versão | Suporte |
|---|---|
| `1.0.x` | Sim |
| `< 1.0` | Não |

## Escopo desta política

Reporte por este canal problemas de segurança no conteúdo deste repositório, por exemplo:

- exposição acidental de segredo ou dado sensível;
- workflow com permissões excessivas ou execução insegura;
- script capaz de ultrapassar os limites de tráfego documentados;
- dependência vulnerável que afete a execução do laboratório;
- falha nos controles de sanitização ou armazenamento de evidências.

A OWASP crAPI é deliberadamente vulnerável e executada fora deste repositório. Vulnerabilidades ou correções no código da crAPI devem ser tratadas com o projeto [OWASP/crAPI](https://github.com/OWASP/crAPI). Um resultado de teste deste laboratório só entra no catálogo local depois da triagem definida em `docs/vulnerability-management.md`.

## Como reportar

Não abra issue pública para vulnerabilidades, segredos ou evidências sensíveis. Use o botão **Report a vulnerability** na área de [Security Advisories](https://github.com/CarlosRyan07/api-security-testing-lab/security/advisories); o relato privado está habilitado no GitHub.

Inclua somente o necessário para análise:

- versão ou commit afetado;
- descrição e impacto observado;
- passos mínimos e seguros para reprodução;
- sugestão de correção, quando disponível;
- evidência sanitizada, sem JWT, senha, cookie ou dado pessoal completo.

Não teste contra sistemas de terceiros nem amplie volume, escopo ou impacto para demonstrar o problema. O recebimento de um relato não autoriza novas execuções contra a crAPI.

## Tratamento

O mantenedor fará a triagem pelo advisory privado e solicitará contexto adicional quando necessário. Não há SLA formal. Confirmação, correção e divulgação serão registradas somente após validação; sinais inconclusivos não serão apresentados como vulnerabilidades confirmadas.
