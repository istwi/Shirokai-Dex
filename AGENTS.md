# Continuidade obrigatória — Shirokai Dex

Estas instruções aplicam-se a todo o repositório. Apenas um Codex trabalha por vez. GitHub é a fonte oficial entre sessões; não dependa da memória de conversas ou de arquivos fora do repositório.

## Regra fixa: checklist do prompt mestre

Leia e atualize [docs/MASTER_CHECKLIST.md](docs/MASTER_CHECKLIST.md) em toda entrega. Ela acompanha o [prompt mestre integral](docs/PROJECT_SPECIFICATION.md). Marque `[x]` e risque com `~~texto~~` somente os itens comprovadamente concluídos; mantenha parciais abertos, com evidências e limitações. Registre criação, alteração, correção ou remoção na tabela de entregas, indicando arquivos/testes e referência do commit. Reabra itens quando uma mudança invalidar sua conclusão. Esta regra não substitui os documentos de estado e próximos passos.

## Antes de trabalhar

1. Confirme a raiz e o remoto com Git, sem revelar credenciais. Inspecione `git status --short --branch`, alterações staged/unstaged, arquivos não rastreados e commits locais ainda não enviados.
2. Consulte o GitHub com `git fetch origin`, branches e últimos commits. Quando existir upstream e a árvore estiver limpa, sincronize com `git pull --ff-only`. Não sobrescreva arquivos locais, não use `reset --hard`, `clean`, checkout destrutivo ou force push para sincronizar. Se houver divergência ou mudanças locais, preserve-as e integre sem descartá-las; explique conflitos reais que impeçam continuar. Um remoto vazio não possui HEAD para buscar: confirme com `git ls-remote origin` antes de concluir que houve falha de autenticação.
3. Leia este arquivo, `docs/PROJECT_STATUS.md` e `docs/NEXT_STEPS.md`; consulte `docs/API_COMPATIBILITY.md` antes de qualquer integração. `docs/PROJECT_SPECIFICATION.md` preserva os requisitos funcionais completos.
4. Compare os documentos com o código, build, testes e últimos commits. Corrija documentação desatualizada com evidências. Continue a primeira tarefa pendente; não reinicie o projeto nem reimplemente partes concluídas.
5. Use o checkout existente. Cada tarefa cloud já é isolada; não crie worktree sem solicitação explícita. Não altere configurações publicadas do ambiente nem publique novas versões do ambiente sem autorização.

## Durante o trabalho

- Respeite as fases do prompt mestre. APIs reais e versões exatas devem ser auditadas antes de uso. Diferencie inspeção estática, compilação, teste automatizado e validação em servidor real.
- Alvo: Minecraft 1.21.1, Fabric, Java 21, Cobblemon 1.8.3, Impactor 5.3.5, SQLite. Não atualizar versões silenciosamente nem inventar classes, comandos ou eventos.
- Preserve a Pokédex oficial; separe obtenção, captura legítima, estatísticas permanentes e pontuação competitiva. Não invente dados históricos.
- Alterações funcionais devem acompanhar testes aplicáveis, documentação e registro de erros/limitações. Não declare conclusão somente porque compilou.
- Não implemente todas as fases simultaneamente. Integrações opcionais ausentes não devem derrubar o servidor. Entregas externas incertas não devem ser repetidas automaticamente.
- Não grave credenciais, logs privados, caches, artefatos temporários ou dependências baixadas no Git. Use autenticação existente; nunca peça valores secretos em chat. Preserve TLS e checksums.
- Preserve mudanças do usuário. Não remova código funcional sem justificativa documentada. Não copie código externo sem verificar a licença.

## Antes de concluir um conjunto coerente ou encerrar

1. Salve e valide o trabalho com os testes realmente aplicáveis. Investigue falhas; registre comando, resultado, ambiente e testes pendentes. Não transforme checks de documentação em alegação de build ou funcionamento do mod.
2. Atualize `docs/PROJECT_STATUS.md`: fase, concluído/em desenvolvimento/pendente, testes, erros conhecidos, APIs auditadas e última alteração relevante.
3. Atualize `docs/NEXT_STEPS.md`: última tarefa, próxima tarefa exata, arquivos importantes, problemas, dependências, testes pendentes, cuidados e referência ao último commit relevante. A próxima instância deve conseguir continuar sem esta conversa.
4. Atualize a documentação afetada, `docs/API_COMPATIBILITY.md` e `CHANGELOG.md` quando apropriado. Distinga fato confirmado de hipótese.
5. Revise `git diff --check`, alterações e arquivos staged. Faça commits descritivos agrupando alterações relacionadas. Não invente autor ou substitua identidade Git configurada.
6. Envie ao GitHub quando autorizado (o usuário autorizou commits e push neste projeto). Respeite proteção de branch e fluxo de PR quando existente. Não use force push nem contorne proteção. Não publique release estável sem testes obrigatórios aprovados.
7. Confirme o SHA remoto com `git ls-remote` e o estado local. Informe branch, commit e sincronização. Se push falhar, documente o erro e diga explicitamente que o GitHub não foi atualizado e quais commits permanecem locais. Não considere acesso de leitura prova de permissão de escrita.

Não deixe trabalho importante apenas no ambiente sem informar. Publicação de ambiente, release e push são ações distintas; nunca alegue uma a partir de evidência de outra.

## Restrição atual do responsável (2026-11-07)

O usuário pediu manter o desenvolvimento somente no ambiente local. **Não criar commits, não fazer push e não publicar releases/configuração de ambiente até nova autorização explícita.** Esta restrição substitui a autorização anterior de commit/push nesta sessão. Manter código, testes, checklist e passagem atualizados localmente; informar que alterações ainda não estão no GitHub.

## Autorização pontual de publicação (2026-11-07)

O usuário pediu uma release no GitHub. Autoriza commit/push do estado atual e publicação da pré-release `v0.3.0-alpha.4`, com JAR e limitações documentadas. Não autoriza publicação de configuração de ambiente. Após esta entrega, mudanças futuras voltam a permanecer locais até nova autorização.

## Autorização pontual — correção de inicialização alpha.5

O usuário autorizou investigar/corrigir a inicialização, atualizar documentação, fazer commit/push e gerar novo artifact JAR. Escopo: empacotamento e distribuição do Shirokai Dex; sem mudanças nos demais mods ou publicação de ambiente.

## Autorização atual de sincronização

O usuário pediu enviar ao GitHub as atualizações já concluídas e depois continuar o projeto. Autoriza commit/push do snapshot alpha.6/7/8 validado; não autoriza nova release nem publicação de ambiente. Trabalho novo após essa sincronização continua local até nova solicitação de envio.

## Autorização pontual — validação de checkpoint alpha.9

Após a entrega de 274 JUnit/26 checks, o responsável respondeu “continua” à confirmação de commit/push somente em `handoff/crates-checkpoint`. Autoriza registrar/enviar esse bloco validado, preservando a main. Não autoriza release, publicação de ambiente nem mudanças em outros mods. Trabalho funcional posterior continua local até nova autorização de envio.

## Autorização contínua — branch de desenvolvimento

O responsável aceitou o fluxo de aceleração (“faz tudo isso então”): commits e push contínuos de blocos validados em `handoff/crates-checkpoint`, com documentação objetiva e testes relevantes. Esta autorização substitui a limitação de envio pontual anterior para trabalho nesta branch. Integração à main, releases e publicação de ambiente continuam exigindo autorização separada. Apenas um Codex; priorizar recuperação segura, captura/estatísticas, depois competições/rankings; acabamento e integrações opcionais ao final.
