# Estado do projeto

Atualizado em 2026-11-07. Repositório `istwi/Shirokai-Dex`, branch `main`; apenas um Codex por vez.

## Fase atual

**Início da Fase 2 — porcentagens e coleções iniciais**, sobre a fundação validada. Nacional/Lendária exibem quantidade/total e %, Shiny é explicitamente N/D enquanto não houver evidência confiável de captura. A Fase 2 completa ainda está pendente. Auditoria da Fase 0 é incremental: consulta básica comprovada; captura, criação/entrega de Pokémon e economia não estão funcionalmente verificadas. Não é mod completo nem release estável.

## Concluído

- Nova alpha local `0.2.0-alpha.1+mc1.21.1-cobblemon1.8.3`: seletor/páginas Nacional, Lendária e Shiny; status por coleção, porcentagens com duas casas e sem falso 100%. [Comportamento](POKEDEX.md).
- Cache por UUID até 5 segundos, invalidado por evento oficial pós-Pokédex, login/logout/reload/lifecycle; cálculo puro separado do adaptador. Nacional usa agregado oficial; Lendária exige forma elegível obtida.
- Validação atual: **32 casos JUnit** e regressão/progresso em servidor dev e JAR fora do Loom, incluindo datapack e sua remoção. [Relatório](validation/phase2-percentages.json).


- Build Gradle 8.14.3/Loom 1.11.8 com checksum oficial, helper de proxy/cache e JDK 21 completo verificado. Plugin Kotlin 2.2.20 habilita metadata remapping; ICU 71.1 compõe o runtime dev dedicado.
- Inicializador Fabric com lifecycle/readiness/logs e falhas isoladas do Dex; ausência de LuckPerms não impede iniciar.
- Configuração imutável estrita, reload assíncrono, backup validado atômico e recuperação sem sobrescrever JSON inválido.
- SQLite em worker dedicado; migração transacional, WAL/foreign keys, cadastro por UUID e fechamento.
- `/dex`, `/dex status`, menu inicial de 27 slots somente de leitura e consulta oficial Cobblemon. Espécies enumeradas dinamicamente; nenhum registro oficial alterado.
- `/dex admin reload` protegido por LuckPerms sem fallback OP/console; negação sem provedor comprovada, permissão positiva pendente.
- Mensagens em recursos separados pt_br/en_us, com idioma global configurável nesta fase.
- Workflow Java 21 com build/testes e upload de JAR/sources/JUnit condicionado ao sucesso. Checklist e continuidade mantidas.

## Em desenvolvimento

- Coleções e critérios configuráveis JSON, wiki/menus avançados e placeholders da Fase 2. Esta entrega não implementa esses sistemas nem captura shiny histórica.


- Testes complementares: LuckPerms positivo/contextos e interações/aparência em cliente Minecraft real.
- Preparação incremental da Fase 2, respeitando limitações reais de shiny e de espécies/datapacks.

## Pendente

- Fases 2–8: Pokédex completas, categorias/datapacks/wiki, recompensas, capturas/estatísticas, rankings, competições, conquistas/perfis e integrações avançadas.
- Permissão positiva e reload administrativo de ponta a ponta com LuckPerms instalado, demais gestos de menu e testes de carga.
- Layouts JSON completos e documentação das funcionalidades ainda não implementadas.
- Licença do código próprio e release estável. Nenhuma configuração de ambiente foi alterada/publicada nesta entrega.

## Testes e verificações executados

Atualização de porcentagens: 32 JUnit (16 configuração + 4 SQLite + 12 progresso), zero falhas/erros/skips. Regressão de protocolo: 11 verificações. Progresso: 6 verificações com espécie comum/lendária de datapack e shiny obtido por comando; cache respondeu ao evento antes do TTL. Reconexão/lore: 2 checks; remoção de datapack entre reinícios: 1 check. Progresso/status e lore reais conferidos em servidor de desenvolvimento e no JAR empacotado. Total Nacional 907 com fixture e 905 sem; Lendária 18 com fixture e 17 sem. Fonte de contagem é o registro real, não esses números fixados no código.

Os testes abaixo e `phase1-foundation.json` preservam o histórico da primeira alpha; não representam o último total da suíte. O novo relatório é `phase2-percentages.json`.


Evidência persistida: [relatório da fundação](validation/phase1-foundation.json). Testes em `src/test` e `tests/integration`.

| Verificação | Resultado |
|---|---|
| Gradle help/build com JDK 21, proxy e truststore | Aprovados com checksums ativos |
| JUnit configuração | 16 casos aprovados; 0 falhas/erros/skips |
| JUnit SQLite | 4 casos aprovados; inclui 100 atualizações para um UUID, rollback e reabertura |
| Fabric/Cobblemon dev | Inicializou e Showdown iniciou; 1025 espécies carregadas, 905 implementadas no resumo |
| Cliente de protocolo real | 10 checks: login, consulta, menu, quatro modos de clique, fechamento, não mutação/sem ícones indevidos e negação administrativa |
| Reinício com JSON inválido | Backup recuperado, original preservado, UUIDs/primeiro acesso intactos, uma migração e integrity_check=ok; smoke aprovado novamente |
| JAR instalado fora do Loom | Servidor isolado com releases originais e perfil oficial Maven; mesmos 10 checks aprovados, SQLite incluído carregou |
| GitHub Actions | Workflow preparado; conferir execução do commit antes de afirmar aprovação |

Servidores locais eram offline e limitados a loopback; não validam autenticação online ou capacidade multiplayer. Cliente de protocolo ignora pacotes customizados de metadados/receitas/advancements/árvore de comandos não usados nas assertions; não substitui cliente visual.

## Erros e limitações conhecidos

- Falhas iniciais resolvidas: cache não gravável, JVM sem proxy, checksum transcrito com 66 caracteres, runtime sem compilador, Kotlin metadata sem remapeamento e ICU ausente no dev server. Consulte [diagnóstico](TROUBLESHOOTING.md).
- Matcher de erro vanilla do smoke foi corrigido após observar `Incorrect argument for command`; a autorização não foi alterada para fazer o teste passar.
- Conexão prematura ao servidor empacotado recebeu ECONNREFUSED; execução aprovada ocorreu após readiness `Done`.
- Fabric Installer não concluiu consulta de metadata. Usado perfil estático oficial Maven e artefatos verificados para montar o servidor isolado, sem alterar allowlist/TLS.
- Cobblemon emitiu avisos de mixins opcionais/data fixers e arquivos `.old` de usuários novos, sem histórico prévio. Dex não escreve esses arquivos.
- Shiny visto + espécie obtida não comprovam captura shiny. Registros de espécies Cobblemon não são seguros para hot reload.
- Integração Crates por console não é suportada no contrato auditado. Nenhuma recompensa foi entregue/implementada.

## Última alteração relevante e sincronização

Base anterior enviada/confirmada: `8c1e7f365be918b866ac137c020e2337db14e721` (checklist/checksum). Esta entrega é identificada pelo assunto `feat: implement and validate Fabric foundation alpha`; inclui código, testes, documentos e evidência do JAR. Obter o SHA com Git, sem atualização circular do hash deste documento.

Entrega funcional enviada e confirmada no remoto: `9cab47d7d4dcd6c95793cc57e4b1b60d87b49b45`, com árvore local limpa. Workflows desse commit iniciaram; aprovação remota/artifact ainda dependem do resultado observado no GitHub. Atualização documental posterior apenas registra essa referência e corrige os notices; a próxima sessão deve conferir também esse commit documental e quaisquer posteriores.

## Publicação alpha confirmada

Usuário autorizou publicar. [Pré-release v0.1.0-alpha.1](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.1.0-alpha.1) publicada, não draft e não estável, com tag apontando para `c11b3660aaf567d161b03ac91c32d9d1de2501af`. Workflow [42358003410](https://github.com/istwi/Shirokai-Dex/actions/runs/42358003410) terminou com sucesso e executou os 20 casos JUnit novamente.

Cinco anexos: JAR instalável, sources JAR, SHA256SUMS.txt, BUILD_PROVENANCE.json e phase1-foundation.json. Todos foram baixados da release; checksums e proveniência verificados. Evidência em [release-alpha1.json](validation/release-alpha1.json). O relatório phase1 descreve o teste local de servidor e seu hash local; os binários publicados são de CI e têm checksums próprios, sem confundir as duas evidências.

Upload direto da sessão recebeu HTTP 401; resolvido pelo token do próprio GitHub Actions, sem chave pessoal nem alteração do ambiente. O workflow é específico desta alpha e recusa nova publicação se a release já não estiver em rascunho. Não recriar ou sobrescrever a release.

## Última entrega de progresso

Implementação identificada pelo assunto `feat: add verified Pokedex completion percentages`, baseada no remoto `93185ae`. A alpha.1 publicada permanece imutável; esta versão 0.2.0-alpha.1 será disponibilizada pelo artifact de build do commit e JAR local. Não foi publicada nova release nesta tarefa. Confirmar SHA/push ao encerrar.
