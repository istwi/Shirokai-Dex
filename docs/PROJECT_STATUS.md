# Estado do projeto

Atualizado em 2026-11-07. Repositório `istwi/Shirokai-Dex`, branch `main`; apenas um Codex por vez.

## Entrega atual — wiki inicial (alpha.3)

Base sincronizada: `5ad1b60ddd5c6ce7353e8ce6942b54064e852128`, sem mudanças ou commits locais pendentes na retomada. Versão `0.2.0-alpha.3+mc1.21.1-cobblemon1.8.3`; commit identificado pelo assunto `feat: add searchable paginated species wiki`.

Implementada wiki por coleção via botão e `/dex <id> wiki [pesquisa]`: 18 espécies por página, busca por nome canônico/ID, ordenação nome/ID, filtros todas/vistas/obtidas/não vistas/shiny vistas e detalhes de formas/tipos/estados. Critérios e permissões reutilizados; clique antigo revalida acesso e elegibilidade após revogação/reload. Snapshot compartilhado com o progresso, sem escrita na Pokédex nem varredura por tick. [Guia](WIKI.md).

Validação atual: **78 JUnit**, zero falhas/erros/skips; **8 checks de wiki**, **9 de regressão de coleções com LuckPerms** e **12 de fundação sem LuckPerms**, em servidor dev real com datapack namespaced. Navegação não alterou progresso nem entregou ícones; atualização por evento oficial e revalidação após reload foram testadas. [Evidência](validation/phase2-wiki.json).

Dados de spawn continuam indisponíveis, sem inferir ausência de spawn natural. Captura shiny permanece N/D. Nomes de espécie/tipo/forma são canônicos; detalhes limitam a 32 formas com aviso. Cliente visual, carga e instalação desta nova alpha fora do Loom continuam pendentes. Wiki completa/menus JSON avançados e o restante da Fase 2 não estão concluídos.

Servidores encerrados com `stop`, mundos salvos, SQLite íntegro e configurações/mod local anteriores restaurados; segredo RCON temporário removido. JAR em `build/libs/shirokai-dex-0.2.0-alpha.3+mc1.21.1-cobblemon1.8.3.jar`. Nenhuma release nem configuração de ambiente publicada.

## Retomada — permissões por dimensão e menus protegidos

Desenvolvimento retomado por solicitação do usuário, a partir do remoto `41e0b7b3266af4ff2d2b17c2c352b779f68949f7`, sem alterações ou commits locais pendentes. A descrição de “somente esqueleto” recebida na retomada era histórica: fundação, percentuais e coleções JSON já estavam implementados. Nenhuma dessas funcionalidades foi reimplementada.

Build repetido neste ambiente com JDK 21, Gradle 8.14.3, cache gravável, proxy e TLS/checksums ativos: **69 JUnit aprovados, zero falhas/erros/skips**. Novo runner `tests/integration/contexts.cjs` e fixture `tests/fixtures/contexts/config.json`: **8 checks de protocolo aprovados**, depois repetidos com um novo perfil descartável, em servidor Loom/Fabric/Cobblemon real com LuckPerms 5.4.140. [Evidência](validation/phase2-contexts.json).

Confirmados grant/reload por `world=overworld`, negação/ocultação no Nether, retorno, revogação com GUI já aberta, descarte individual/pilha, double-click, fases de arraste esquerdo/direito, clone criativo e clique externo com cursor forjado. Contagens autoritativas do inventário, ausência de itens largados, reabertura e Pokédex oficial permaneceram intactas. Cliente visual, autenticação online e carga continuam pendentes.

Corrigido o fluxo de desenvolvimento cloud com `scripts/server_proxy.init.gradle`: o `JavaExec` de `runServer` agora pode receber explicitamente proxy/truststore usados pelo Gradle. A tentativa inicial falhou no download de dependências LuckPerms por falta de proxy na JVM filha; o comando opt-in foi validado com downloads verificados e startup real. Sem mudança no código do mod ou em versões de dependências.

Servidor encerrado com `stop`, mundos salvos e SQLite com `integrity_check=ok`. Configuração local anterior restaurada e senha RCON temporária removida. Nenhuma release nem configuração de ambiente foi publicada. Entrega identificada pelo assunto de commit `test: validate contextual permissions and protected inventory menus`, baseada em `41e0b7b`.

## Entrega anterior — coleções JSON

Implementados campo opcional collections no config.json, metadados/ícones/permissões, filtros por whitelist/exclusão/labels/tipos/formas, modos OWNED/SEEN/SHINY_SEEN e SHINY_CAPTURED explicitamente indisponível. 1–64 coleções, seletor paginado em blocos de 18, comandos dinâmicos e revalidação da permissão no clique. Compatibilidade com JSON antigo preservada.

Configuração inteira e backup só são publicados após validação estrutural e semântica contra snapshot imutável do registro; arquivo UTF-8 inválido/referência desconhecida não muda estado ativo. Backup também é validado no startup. UI indica modo de progresso e não confunde shiny visto com capturado.

Validação desta entrega: **69 casos JUnit aprovados**, 11 checks de regressão sem LuckPerms em servidor dev, 9 checks de coleções com LuckPerms Fabric 5.4.140 e JAR instalado fora do Loom, mais 2 checks após reinício com configuração inválida. Grant/revoke global, botão antigo, reload autorizado, rollback de idioma/título/coleções, cache e recuperação passaram. Contextos específicos de dimensão, cliente visual e carga continuam pendentes. [Evidência](validation/phase2-collections.json), [configuração](COLLECTIONS.md).

O primeiro runner sem espaçamento teve timeout em uma resposta repetida; consultas isoladas responderam e o runner com requisições espaçadas passou usando outro perfil descartável, sem alterar o mod para contornar o check. Comportamento em rajadas/carga não foi certificado. Servidores de teste encerrados; configurações locais originais restauradas e credencial RCON temporária removida. Nenhuma nova release ou configuração de ambiente foi publicada.

## Fase atual

**Fase 2 — porcentagens, coleções e wiki inicial**, sobre a fundação validada. Nacional/Lendária exibem quantidade/total e %, Shiny é explicitamente N/D enquanto não houver evidência confiável de captura. A Fase 2 completa ainda está pendente. Auditoria da Fase 0 é incremental: consulta básica comprovada; captura, criação/entrega de Pokémon e economia não estão funcionalmente verificadas. Não é mod completo nem release estável.

## Concluído

- Nova alpha local `0.2.0-alpha.1+mc1.21.1-cobblemon1.8.3`: seletor/páginas Nacional, Lendária e Shiny; status por coleção, porcentagens com duas casas e sem falso 100%. [Comportamento](POKEDEX.md).
- Cache por UUID até 5 segundos, invalidado por evento oficial pós-Pokédex, login/logout/reload/lifecycle; cálculo puro separado do adaptador. Nacional usa agregado oficial; Lendária exige forma elegível obtida.
- Validação histórica de porcentagens: **32 casos JUnit** e regressão/progresso em servidor dev e JAR fora do Loom, incluindo datapack e sua remoção. [Relatório](validation/phase2-percentages.json).


- Build Gradle 8.14.3/Loom 1.11.8 com checksum oficial, helper de proxy/cache e JDK 21 completo verificado. Plugin Kotlin 2.2.20 habilita metadata remapping; ICU 71.1 compõe o runtime dev dedicado.
- Inicializador Fabric com lifecycle/readiness/logs e falhas isoladas do Dex; ausência de LuckPerms não impede iniciar.
- Configuração imutável estrita, reload assíncrono, backup validado atômico e recuperação sem sobrescrever JSON inválido.
- SQLite em worker dedicado; migração transacional, WAL/foreign keys, cadastro por UUID e fechamento.
- `/dex`, `/dex status`, menu inicial de 27 slots somente de leitura e consulta oficial Cobblemon. Espécies enumeradas dinamicamente; nenhum registro oficial alterado.
- `/dex admin reload` protegido por LuckPerms sem fallback OP/console; negação sem provedor comprovada, permissão positiva/global e por dimensão agora verificada nos relatórios de coleções/contextos.
- Mensagens em recursos separados pt_br/en_us, com idioma global configurável nesta fase.
- Workflow Java 21 com build/testes e upload de JAR/sources/JUnit condicionado ao sucesso. Checklist e continuidade mantidas.

## Em desenvolvimento

- Evolução da wiki, menus avançados, categorias e placeholders da Fase 2. Wiki inicial, coleções e critérios JSON já estão implementados; captura shiny histórica permanece indisponível.


- Testes complementares: aparência/gestos em cliente Minecraft real, contextos LuckPerms adicionais e carga. O cenário de dimensão e os pacotes de inventário descritos acima foram verificados.
- Preparação incremental da Fase 2, respeitando limitações reais de shiny e de espécies/datapacks.

## Pendente

- Fases 2–8: wiki completa/dados de spawn, categorias/datapacks adicionais e placeholders, recompensas, capturas/estatísticas, rankings, competições, conquistas/perfis e integrações avançadas.
- Cliente visual e testes de carga; herança/contextos LuckPerms além do cenário de dimensão verificado.
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

## Publicação da alpha.3 confirmada

Usuário autorizou publicar o JAR. [Pré-release v0.2.0-alpha.3](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.2.0-alpha.3) publicada, não draft e experimental, com tag confirmada no commit `47817bff13cdc3464ad926791233801a01354b54`. Workflow [42365493365](https://github.com/istwi/Shirokai-Dex/actions/runs/42365493365) aprovado e 78 JUnit executados no build de publicação.

Cinco anexos baixados da release e conferidos: JAR instalável, fontes, `phase2-wiki.json`, `BUILD_PROVENANCE.json` e `SHA256SUMS.txt`. Checksums GitHub e manifesto verificados; versão interna do JAR, proveniência e alvo da tag confirmados. [Evidência](validation/release-alpha3.json). O relatório de servidor dev mantém seu hash local separado dos binários de CI. Limites de cliente visual/carga/JAR fora do Loom continuam explícitos. A release antiga permaneceu inalterada; nenhuma configuração de ambiente foi publicada.
