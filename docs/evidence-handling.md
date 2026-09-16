# Tratamento de evidências

## Armazenamento

Evidências brutas pertencem ao diretório local `evidence/`, que está ignorado pelo Git. Relatórios versionados podem registrar nome lógico, data, origem e SHA-256 do artefato, mas não seu conteúdo sensível.

Arquivos temporários de ferramentas continuam em `target/` ou em outro diretório já ignorado. Nenhuma evidência deve ser movida para `docs/` antes de revisão e sanitização.

## Conteúdo proibido no Git

- JWT completo ou fragmento reutilizável.
- Senha, segredo, chave, cookie ou cabeçalho `Authorization`.
- Dados pessoais completos criados ou retornados pelo SUT.
- Dump integral de request ou response sem revisão.
- Relatório automatizado que ainda não tenha sido triado.

## Checklist de sanitização

1. Substituir valores sensíveis por marcadores descritivos, sem preservar prefixos reutilizáveis.
2. Manter somente os headers e campos necessários para demonstrar o comportamento.
3. Confirmar que o arquivo não contém `Authorization`, `Bearer`, `token`, `password`, cookies ou dados pessoais completos.
4. Executar Gitleaks com `--redact` na árvore de trabalho.
5. Calcular o SHA-256 do artefato local e registrar apenas o hash no relatório.
6. Revisar manualmente o diff antes do commit.

Exemplo para calcular o hash no PowerShell:

```powershell
Get-FileHash -Algorithm SHA256 .\evidence\<arquivo-local>
```

O hash demonstra integridade, não torna seguro publicar o artefato original.

## Retenção e descarte

A retenção deve durar somente enquanto necessária para triagem e revalidação. O responsável pela execução decide o descarte local após confirmar que o relatório sanitizado contém rastreabilidade suficiente. Este repositório não automatiza upload, backup ou exclusão de evidências.
