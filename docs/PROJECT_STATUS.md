# Estado do projeto

Atualizado em 2026-11-07. Repositório `istwi/Shirokai-Dex`, main validada preservada e retomada em `handoff/crates-checkpoint`; apenas um Codex por vez.

## Publicação solicitada — v0.4.0-alpha.1

Responsável autorizou pré-release experimental no commit funcional `78dfe72f8cef55f20c3645dc132ea6c3d97ad6f0`, preservando main/ambiente. Build remoto [42431184283](https://github.com/istwi/Shirokai-Dex/actions/runs/42431184283) já aprovado. Download de artifact via Cloud recebeu bloqueio no host de armazenamento; publicação usa workflow novo fixado nesse source SHA, sem alterar os workflows históricos. Constrói/testa 304 JUnit, cria draft, anexa quatro assets, baixa/verifica checksums e só então publica. [Notas](releases/v0.4.0-alpha.1.md). Primeiro run 42431952113 falhou antes dos jobs por sintaxe YAML na condição; corrigida sem mudar source/tag, nenhum draft criado. Ainda não declarar publicada sem verificar resultado/anexos.

## Entrega atual — captura/estatísticas iniciais (0.4.0-alpha.1)

Base enviada `26c4472`, branch `handoff/crates-checkpoint`, autorização contínua de commits/push; main preservada `c89cc1b`. Eventos nativos pré-acerto/captura cruzados com evidência de origem PlayerSpawner/pool mundial. Ledger deduplicado por Pokébola+Pokémon, classificação/shiny/regras congeladas, totais permanentes e contagens por espécie/categoria. Estatísticas próprias por CLI e períodos com fuso configurável, sem reconstrução histórica. [Guia e limites](CAPTURES.md).

304 JUnit (14 novos), nove checks nativos de captura e nove de regressão de recuperação/schema6, com reinícios; [evidência](validation/phase4-captures.json). Capturas positivas vieram de colisão/animação nativa de Pokébola com criaturas geradas pelo spawner real; replay não duplicou, obtenção por comando não contou e entidade administrativa capturada ficou UNKNOWN. Migração5→6 preservou plano anterior. Probe é separado e não compõe o JAR distribuído.

Limite real: SpawnCause não permaneceu na entidade após reinício; ela foi capturada como UNKNOWN, sem aumentar o total legítimo. Continuidade de proveniência, batalhas/pesca/outros spawners, limites/revisão/histórico detalhado/recordes e integração do ledger à coleção Shiny continuam pendentes. Fase 4 não concluída nem captura universal declarada. Sem release/main/ambiente publicados. Entrega identificada por `feat: track verified native captures and permanent statistics`.

## Entrega anterior — recuperação segura e executor de testes (alpha.11)

Base COMMAND enviada `73e5d78`; trabalho na branch `handoff/crates-checkpoint`, com autorização contínua de commit/push, main preservada em `c89cc1b`. Retomada administrativa com prévia/confirmação e justificativa, exclusivamente para PENDING/NEEDS_REVIEW sem nenhuma etapa iniciada. ID/plano/componentes originais preservados; permissões do operador e destinatário e identidade do plano revalidadas. INTENT/prefixo entregue continuam bloqueados para revisão, sem retry. [Guia](RECOVERY.md).

290 JUnit e 18 checks de servidor empacotado: nove recuperação/reinício e nove regressão COMMAND. [Evidência](validation/phase3-recovery.json). Executor reutilizável prepara mundos novos a partir de dependências já auditadas, inicializa schema real, semeia fixtures offline, testa, encerra/reinicia, verifica e limpa RCON. Sem mudança de schema nesta entrega; sem release/ambiente/main publicados. Entrega identificada por `feat: add safe supervised reward recovery and reusable server suites`.

Próximo bloco funcional: captura legítima/estatísticas. Auditoria localizou evento e SpawnCause reais, mas a classificação de origem e teste positivo nativo ainda precisam ser comprovados; não declarar eventos injetados/comandos como capturas naturais. Fases de competições/rankings dependem dessa base.

## Bloco local — COMMAND alpha.10

Base enviada `f714c4b`, branch `handoff/crates-checkpoint`; main preservada `c89cc1b`. Envio autorizado na branch de desenvolvimento pelo fluxo contínuo; referência: `feat: add explicitly authorized command reward dispatch`. Modelo/codec/política de template exato, preparação de destinatário congelada no plano, parsing e execução na thread do servidor, PLAYER sem elevação/CONSOLE explicitamente autorizado, prévia/confirmação e revisão obrigatória. Um comando isolado por marco, sem planos mistos. Após dispatch, etapa permanece INTENT e operação NEEDS_REVIEW; não há alegação de persistência externa nem retry. [Guia](COMMAND_REWARDS.md).

Servidores encerrados com mundo salvo, SQLite integrity_check=ok e segredo RCON removido. 282 JUnit, zero falhas/erros/skips; nove checks de servidor empacotado (sete + dois após reinício), [evidência](validation/phase3-commands.json). A suíte Java anterior foi repetida; runners completos dos outros tipos não foram repetidos nesta entrega. CRATE_KEY dedicado/recibos automáticos/planos mistos/recuperação completa e fases posteriores continuam pendentes. Sem release/publicação de ambiente.

## Continuação — envio e viabilidade de CRATE_KEY

Checkpoint alpha.9 enviado e SHA remoto confirmado em `f714c4b216743a239facbf7623fae18dbdb22d42`, somente na branch `handoff/crates-checkpoint`; main continua `c89cc1b`. Árvore ficou limpa após esse envio. Autorização pontual aplicada ao bloco validado; auditoria posterior permanece local.

Auditoria de fonte/binário confirmou ausência de contrato público suficiente para emissão automática dedicada no Crates0.2.1: `give` é void/assíncrono e exige jogador administrativo; instância Tokens é privada. Checkpoint público não resolve emissão/autenticação. [Contrato detalhado](CRATE_KEY_CONTRACT.md). CRATE_KEY continua desabilitado, sem mudanças em outros mods/reflexão privada de produção/elevação de permissão. Nenhuma funcionalidade nova declarada pronta nem teste de runtime repetido nesta auditoria documental.

## Retomada local — checkpoint Crates alpha.9

Branch `handoff/crates-checkpoint`, base `6b97093`; main remota `c89cc1b` e funcional `191a65e` preservadas. Nenhuma alteração local anterior encontrada; checkout/fetch feitos sem descarte. Envio deste bloco validado autorizado pela resposta “continua” à confirmação de commit/push somente nesta branch.

`PlayerSaveCoordinator` e ItemRewardAdapter da passagem exercitados no JAR empacotado fora do Loom: checkpoint Crates0.2.1, chave física real via SAVED_ITEM, autenticidade nativa, espera de 2000ms, itens simples, falha real de escrita/revisão/anti-repetição, reinício e fallback sem Crates. Build/distribuição/probes separados, 274 JUnit e 26 checks de servidor aprovados (8 checkpoint, 10 itens/reinício, 8 Pokémon/reinício). Servidores encerrados, SQLite íntegro, configuração de teste Cobblemon restaurada e RCON desativado com segredo temporário removido. [Evidência](validation/phase3-crates-checkpoint.json), [procedimento](CRATES_CHECKPOINT_HANDOFF.md).

Runner novo e remapeamento do probe Pokémon adicionados para validação/reprodução. Sem mudanças no código de outros mods, APIs fictícias, reenvio automático ou publicação de release/ambiente. CRATE_KEY dedicado continua pendente; cenário não certifica segredos/servidores/versões adicionais nem toda a Fase 3. Histórico abaixo distingue os blocos anteriores.

## Passagem interrompida — WIP Crates separado

Main validada enviada em `191a65e`. Continuação alpha.9 isolada na branch `handoff/crates-checkpoint`: coordenação de save assíncrono/ItemRewardAdapter e probe remapeado. Build/JAR/274 JUnit passaram; nenhuma validação funcional de checkpoint/chaves executada. Usuário pediu parar e trocar de Codex. Seguir [passagem exata](CRATES_CHECKPOINT_HANDOFF.md); não promover WIP como pronto por compilar.

## Sincronização autorizada — menus e CURRENCY

O usuário solicitou commit/push das alterações alpha.6/7/8 validadas. Entrega de referência: commit `feat: add configurable menu presentation and Impactor currency rewards`, sobre a base `7defc0c`. Código, fixtures, evidências e documentação incluídos; 274 JUnit e 8 checks monetários, além dos blocos anteriores de menus. Sem nova release/publicação de ambiente. As seções locais abaixo preservam a história de implementação; a confirmação do envio depende da verificação remota ao concluir a entrega. Próximo trabalho: chaves físicas Crates, inicialmente em escopo local.

## Trabalho local atual — alpha.8: CURRENCY

Base remota publicada `7defc0c`; mudanças alpha.6/7/8 locais, sem commit/push/publicação. Recompensas monetárias isoladas ou mistas com itens/Pokémon. IDs exatos registrados e BigDecimal, sem conversão/fallback. Integração opcional Impactor5.3.5/JSON nativo: resultado da transação, save aguardado e saldo gravado conferido antes do ack; erros ocultados e precisão double do provedor tratados conservadoramente. Planos antigos e schema5 preservados.

**274 JUnit sem falhas/erros/skips; 8 checks reais em servidor empacotado aprovados**, somando 2 sem Impactor, 5 nativos e 1 após reinício. Depósitos reais de dólares/tokens, plano misto e entrega única; moeda ausente/precisão incompatível bloqueiam antes de itens. Falha real de save suprimida pelo provedor detectada, operação permanece em revisão. [Guia](CURRENCY_REWARDS.md), [evidência](validation/phase3-currency.json).

Servidores encerrados, SQLite íntegro, segredo RCON e JAR Impactor temporário removidos. Escopo limitado a JSON nativo auditado com identificação read-only do backend. Outros provedores/formatos/versões, concorrência ampla e carga não certificados; sem idempotência externa nem transação atômica. Próximo bloco: auditar integração atual de chaves físicas Shirokai Crates e demais tipos pendentes da Fase3. Não declarar o mod inteiro pronto.

## Bloco local anterior — alpha.7: apresentação dos menus

Base publicada `7defc0c`; alterações alpha.6/alpha.7 locais, sem commit/push/publicação. Menus existentes agora suportam linhas 3/4..6, título com dados reais, paleta por tela e aparência dos controles (ícone/nome/lore). Registros e textos validados antes de publicar configuração. Nome de ação/estado e lore crítica preservados; templates não alteram prêmios nem criam ações bloqueadas.

**254 JUnit aprovados, sem falhas/erros/skips, e 6 checks novos de servidor empacotado aprovados.** Cobertura das 11 telas ampliadas, slots novos, títulos/paletas, estados bloqueados, prêmio personalizado intacto, entrega única e rejeição de ícone desconhecido com retorno aos defaults. [Evidência](validation/menu-appearance.json). Runners antigos adaptados/sintaxe conferida; não foram repetidos integralmente.

Servidor encerrado, SQLite íntegro, RCON/segredo e LuckPerms temporário removidos. Cliente gráfico/carga, modelos reutilizáveis, condições genéricas e posicionamento arbitrário de conteúdos pendentes; não equivale ao mod completo. Próximo bloco funcional: auditoria/integração Impactor da Fase 3, respeitando journal e ausência de fallback de moeda.

## Bloco local anterior — alpha.6: navegação por tela

Base `7defc0c`, sem commit/push/publicação das mudanças atuais. Onze telas agora aceitam slots/rodapé por JSON `menus`: seletor, coleção, wiki, filtros, detalhes, formas, spawn, grade/prévia de recompensas e confirmação. Movimentação conjunta de ícones/callbacks, trocas sem sobrescrita e proteção de conteúdo; configuração legada preservada. Sem novas APIs externas, dependências ou mudanças nas regras de entrega.

**Build e 234 JUnit aprovados, zero falhas/erros/skips; 7 checks de protocolo em servidor empacotado aprovados.** Cobertura inclui estados reais do journal, confirmação, item personalizado preservado, um único resgate e rejeição de reload/defaults/restauração. Rejeição de slot inválido foi intencional; reload final foi validado por jogador autorizado. Servidor encerrado, SQLite íntegro, segredo RCON e LuckPerms temporário removidos. Fixture e evidência em `validation/layout-navigation.json`.

Ainda pendentes: inspeção em cliente gráfico, templates completos, carga e adaptação/repetição de runners antigos de resgate. Nenhuma funcionalidade adicional do prompt foi declarada pronta por mera compilação. Correção de entrypoint/empacotamento alpha.5 mantida.

## Correção de inicialização — alpha.5

Causa confirmada pelo usuário e reproduzida em Fabric 0.17.2 / Minecraft 1.21.1 / Java 21: instalação acidental do `sources.jar` alpha.4. O descritor do código-fonte declarava `br.com.shirokaidex.core.ShirokaiDex`, mas o pacote continha `.java`, não `.class`. O binário alpha.4 local contém a classe correta; não foi necessário alterar entrypoint, pacote ou sistemas do mod.

Correção de distribuição em `build.gradle` e `.github/workflows/build.yml`: remover `fabric.mod.json` das fontes, verificar bytecode Java 21/ModInitializer/onInitialize e namespace intermediary no JAR final, checar dependências embutidas e ausência de probes. Artefato do Actions passa a conter somente JAR instalável e SHA256SUMS, gerados em `build/server-mod` após build limpo. Versão nova `0.3.0-alpha.5+mc1.21.1-cobblemon1.8.3`. Build limpo e 218 JUnit sem falhas/erros/skips. Servidor Fabric 1.21.1 real fora do Loom chegou a Done e Dex ready, com schema 5 e SQLite íntegro; fontes novas ignoradas. Validação e checksums em `validation/entrypoint-alpha5.json`.

## Entrega para pré-release v0.3.0-alpha.4

Publicação solicitada explicitamente pelo usuário. Snapshot reúne versões de categorias, recompensas ITEM/CUSTOM_ITEM/SAVED_ITEM/POKEMON, desbloqueios permanentes e layout central/submenus. Referência desta entrega: tag `v0.3.0-alpha.4` (commit de preparação). Build e 218 JUnit aprovados, zero falhas/erros/skips. Evidências anteriores de servidor estão em `docs/validation`; a segunda passagem dos submenus ainda não foi repetida em servidor/cliente gráfico. JAR fora do Loom e carga seguem sem certificação. Código enviado em `0989d29`. Rascunho criado; upload dos anexos bloqueado por HTTP 401 Bad credentials em uploads.github.com (API GitHub e push funcionam). Pré-release será publicada com código-fonte e aviso explícito de ausência do JAR; artefatos ficam locais em build/libs. As seções abaixo preservam o histórico local de cada bloco, não o estado de sincronização desta entrega.

## Trabalho local atual — Pokémon e início do layout geral

Sem commit/push/publicação; base remota confirmada continua `593d43f`. Versão local `0.3.0-alpha.4+mc1.21.1-cobblemon1.8.3`.

POKEMON: validação real de espécie/forma/atributos, payload e UUID congelados antes de intenção, capacidade equipe/PC e leitura de gravação NBT antes da confirmação. Operação incerta continua bloqueada após reinício. [Escopo/limites](POKEMON_REWARDS.md), [8 checks reais](validation/phase3-pokemon.json).

Prioridade passou para layout por pedido explícito do usuário. Central de seis linhas, gradiente/paletas/slots JSON, resumo oficial, navegação reversível, grade paginada de recompensas e indicação honesta de áreas não implementadas. /menu externo usa jogador sem elevar permissão e fica indisponível quando ausente. [Guia](LAYOUT.md), [22 checks de layout/regressão](validation/layout-home.json).

**218 JUnit na revisão atual, zero falhas/erros/skips; 30 checks reais históricos somando os dois blocos anteriores.** Servidores encerrados, SQLite íntegro e configuração/propriedades/Cobblemon restaurados; segredos e probes removidos. Instalação do JAR fora do Loom, cliente visual/carga, layouts completos dos submenus e formatos/provedores Pokémon adicionais seguem pendentes. Moedas/Crates/COMMAND e demais sistemas ficam para depois da revisão visual, conforme solicitação.

## Trabalho local anterior — itens salvos/componentes (alpha.2)

Sem commit/push/publicação. Versão local `0.3.0-alpha.2+mc1.21.1-cobblemon1.8.3`; base remota continua `593d43f`, com as alterações anteriores também locais.

ITEM/CUSTOM_ITEM/SAVED_ITEM, captura registry-aware de componentes, save/list/give/delete com permissão própria, confirmação e journal. Migração 5 com checksum/auditoria e reparo explícito preservando evidências; snapshots não mudam com edição/exclusão do modelo. Backup expandido resolve referências e recupera configuração após exclusão. Fila de revisão paginada e IDs de operação disponíveis. [Guia](SAVED_ITEMS.md).

**Build e 179 JUnit aprovados, zero falhas/erros/skips; 10 checks reais de servidor aprovados**, incluindo reinício, componentes, referências apagadas, revisão/permissões e regressão de itens comuns. SQLite íntegro, oito operações desse perfil concluídas com planos originais preservados; servidores encerrados, configuração/propriedades restauradas e segredo removido. [Evidência](validation/phase3-saved-items.json).

Ainda pendentes: Pokémon, moedas/Impactor, Crates, COMMAND, retomada supervisionada e auditoria detalhada navegável. Componentes de mods adicionais, cliente visual/carga e JAR alpha.2 instalado fora do Loom não certificados. Não há atomicidade SQLite/inventário nem eventos históricos inventados.

## Trabalho local anterior — recompensas de itens e resgates

Sem commit/push/publicação, por instrução do usuário. Base remota continua `593d43f`; alterações de categorias anteriores também permanecem locais.

**Build e 159 JUnit aprovados sem falhas/erros/skips; 25 checks reais de servidor nos blocos de recompensas**, incluindo falha real de salvamento e reinícios. Servidores encerrados normalmente, SQLite íntegro, configurações/propriedades restauradas e segredos temporários removidos.

Versão local `0.3.0-alpha.1+mc1.21.1-cobblemon1.8.3`, não publicada. Fase 3: marcos JSON por contagem/percentual exato, prévia paginada, confirmação separada, inventário principal simulado sem descarte/entrega parcial por falta de espaço, plano preservado e unicidade jogador/recompensa. Migração 3 com operações/etapas/auditoria. Recuperação bloqueia PROCESSING em NEEDS_REVIEW; administração pode inspecionar/cancelar/reconhecer evidência, sem reenviar itens. [Guia](REWARDS.md), [evidência](validation/phase3-items.json).

Desbloqueios permanentes acrescentados localmente na migração 4, com primeira evidência e avaliação limitada na thread do servidor. [Evidência](validation/phase3-unlocks.json). Moedas, chaves, Pokémon, componentes/itens salvos e retomada supervisionada continuam pendentes. Não há atomicidade SQLite/inventário. Cliente visual/carga/JAR desta alteração fora do Loom não certificados.

## Trabalho local anterior — versões de categorias (sem commit)

A pedido do usuário, **nenhum commit/push/publicação** nesta etapa. Último estado remoto confirmado: `593d43f`. O prompt integral continua em PROJECT_SPECIFICATION.md; não foi refeito ou resumido em substituição ao original.

CategoryRules gera JSON canônico e SHA-256; migração SQLite 2 preserva conteúdos antigos de forma idempotente. Startup/reload persistem regras validadas antes de publicar configuração; falha impede aplicação. Leitura por fingerprint verifica integridade. Tabela registra observação validada, não ativação/captura. Nenhum histórico de eventos é inventado. [Semântica](CATEGORIES.md), [banco](DATABASE.md).

Build e 130 JUnit sem falhas/erros/skips; 6 checks em servidor + 2 após reinício aprovados. Duas versões e suas datas preservadas; SQLite íntegro; processos encerrados, configuração/propriedades restauradas e segredo removido. Evidência: [relatório local](validation/phase2-category-versions.json). Competições/recálculo, cliente visual, carga e JAR fora do Loom permanecem abertos.

## Entrega anterior — categorias nomeadas (alpha.7)

Base sincronizada `5ac56f2`; commit `feat: add reusable named category rules for collections`. Regras imutáveis por JSON, composição `all_of`/`any_of`/`none_of`, validação de grafo/registro e associação por coleção. Progresso, wiki e reload usam a mesma forma elegível; união não duplica espécies. Configuração antiga preservada; permissões continuam nas coleções. [Schema e limites](CATEGORIES.md).

Build e **124 JUnit aprovados, zero falhas/erros/skips**. Uma regressão inicial de igualdade de Settings com catálogo vazio foi corrigida com igualdade estrutural; suíte final integral aprovada. Validação em servidor e limpeza: consultar [evidência desta entrega](validation/phase2-categories.json).

Histórico, versão persistida das regras, recálculo auditado e SNAPSHOT/LIVE seguem pendentes. Nenhum registro oficial é escrito, nenhuma classificação de captura é inferida e nenhuma release/configuração de ambiente foi publicada. Esta alpha ainda não certificada em cliente visual, carga ou instalação fora do Loom.

## Entrega anterior — resumo de spawn (alpha.6)

Base `a4ecd4f07b4396f63fa1b7a73261ca894821220a`; commit identificado por `feat: show audited world spawn rule summaries in wiki`. Versão `0.2.0-alpha.6+mc1.21.1-cobblemon1.8.3`.

Wiki consulta snapshot somente de leitura do pool mundial, preparado no servidor no lifecycle/reload. Mostra regras/alternativas/exclusões separadas, IDs/tags de biomas, dimensões e intervalos de ticks. Espécies namespaced resolvidas por identificador; formas declaradas preservadas sem inferir aplicação por aspectos. Fonte indisponível distinta de nenhuma regra simples conhecida. [Guia e limites](WIKI.md#aparecimento-e-limites).

**102 JUnit**, zero falhas/erros/skips; **7 checks de spawn + 2 após remoção da fixture entre reinícios + 8 de regressão da wiki**, em servidor dev real. Incluem dados nativos/namespaced, conjunto desabilitado, 19 cards paginados, ticks reais, exclusões, inventário/progresso somente de leitura, permissão e critérios atuais. [Evidência](validation/phase2-spawns.json).

Primeira tentativa corrigida: `getByName` duplicava namespace para espécies de datapack; usado `getByIdentifier`, com teste de regressão. Uma expectativa do runner após reload foi corrigida para preservar a pesquisa que já estava ativa. Nenhuma API fictícia ou registro oficial modificado. Sem teste de nascimento real, interpretação universal das condições/spawners ou garantia por forma. Cliente visual/carga e JAR desta alpha fora do Loom pendentes.

Servidores encerrados com mundos salvos, SQLite íntegro, propriedades anteriores restauradas e RCON temporário removido. JAR local em `build/libs/shirokai-dex-0.2.0-alpha.6+mc1.21.1-cobblemon1.8.3.jar`. Nenhuma nova release/configuração de ambiente publicada; alpha.3 permanece imutável.

## Entrega anterior — filtros e formas da wiki (alpha.5)

Base sincronizada `ef99403dbc2c3463cd1e55e91abe2202046e8a89`, sem mudanças/commits locais pendentes. Versão `0.2.0-alpha.5+mc1.21.1-cobblemon1.8.3`; entrega identificada por `feat: add wiki type filters and complete form pagination`.

Seletores paginados de tipos/labels reais e formas sem corte em 32 linhas. Tipo, label e critérios JSON combinam na mesma forma; estados são calculados nas formas elegíveis. Consulta, estado e ordenação preservados entre lista/seletores/detalhes/formas. Wiki usa quatro linhas vanilla; seletor/progresso permanecem em três. [Guia](WIKI.md).

**95 JUnit**, zero falhas/erros/skips; **8 checks novos e 8 de regressão da wiki** em servidor dev real com LuckPerms. Fixture com 40 variantes + forma padrão comprovou acesso às 41 formas e nenhuma transferência de progresso da padrão às variantes. Inventário, permissões de botões antigos e ajuste de páginas após reload validados; interface global en_us também exercitada. [Evidência](validation/phase2-wiki-facets.json).

Nenhuma alteração de dependências, schema JSON, registro oficial, progresso/denominadores ou placeholders. Filtros são temporários da consulta. Spawn, categorias completas/históricas, cliente visual/carga e esta alpha fora do Loom continuam pendentes; não houve nova certificação de TAB/alias legado.

Servidores encerrados, mundos salvos, SQLite íntegro e configuração local anterior restaurada; segredo temporário RCON removido. JAR `build/libs/shirokai-dex-0.2.0-alpha.5+mc1.21.1-cobblemon1.8.3.jar`. Nenhuma nova release/publicação de ambiente; alpha.3 publicada permanece imutável.

## Entrega anterior — placeholders básicos (alpha.4)

Base sincronizada: `05a75779a5944a59d41ea0392daa537899f3c759`, sem mudanças ou commits locais pendentes. Versão `0.2.0-alpha.4+mc1.21.1-cobblemon1.8.3`; entrega identificada pelo assunto `feat: add optional collection progress placeholders`.

Implementados percent, registered, seen, total e remaining por coleção via Text Placeholder API opcional 2.4.2+1.21. Sintaxe real `%shirokaidex:percent national%` e prévia `/dex <id> placeholder <métrica>`. Valores somente de leitura, permissão atual, idioma global e indisponibilidade explícita. Callback lê snapshots imutáveis preparados em fila limitada no servidor, sem consultar SQLite ou calcular espécies na expansão. [Guia](PLACEHOLDERS.md).

**87 JUnit**, zero falhas/erros/skips; **8 checks normais**, **5 de probe real** e **13 da fundação sem ambas as APIs opcionais**. Confirmados eventos oficiais, reload/revisão/idioma, isolamento por jogador, parser/argumentos, contexto sem jogador/off-thread, conflitos, substituição posterior e limpeza no shutdown preservando outros provedores. [Evidência](validation/phase2-placeholders.json).

Dependência compilada sem inclusão no JAR, obtida da release oficial GitHub com URL/SHA-256 fixados na fixture; Maven Nucleoid estava bloqueado. Não foram relaxados TLS/checksums nem alteradas configurações de rede. API e probe não integram o JAR de distribuição. Sem API instalada, funções existentes continuam funcionando.

TAB/bridge, alias legado positivo, cliente visual, carga e nova alpha fora do Loom continuam pendentes. O Dex não registra o alias legado nesta entrega nem inventa placeholders de capturas/recompensas/competições. Snapshots frios/invalidados podem retornar N/D até o preparo; chamadas off-thread sempre retornam N/D.

Servidores encerrados com stop, mundos salvos e SQLite íntegro; configuração/mod local anterior restaurado e credencial RCON temporária removida. JAR local `build/libs/shirokai-dex-0.2.0-alpha.4+mc1.21.1-cobblemon1.8.3.jar`. Nenhuma nova release ou configuração de ambiente publicada; alpha.3 publicada permanece imutável.

## Entrega anterior — wiki inicial (alpha.3)

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

**Fase 4 — captura legítima e estatísticas**, com Fases2/3 avançadas ainda parciais. Ledger e contadores novos validados no PlayerSpawner/pool mundial com fluxo nativo; origem ausente continua UNKNOWN. Continuidade de proveniência, outros fluxos, limites/histórico/recordes e Shiny% pendentes. Fases5–8 ainda não implementadas; não é mod completo nem release estável.

## Concluído

- Recompensas Pokémon NBT no escopo auditado, com falha real/reinício; central e grade de recompensas com layout configurável. 214 JUnit e 30 checks destes blocos.

- Trabalho local alpha.2: componentes/itens salvos, backup independente, auditoria de modelos e fila de operações; 179 JUnit + 10 checks desta etapa. [Evidência](validation/phase3-saved-items.json).

- Trabalho local: itens simples com prévia/segunda confirmação, journal/migrações 3–4, revisão administrativa e desbloqueios permanentes. Tipos avançados continuam abertos. [Guia](REWARDS.md).

- Categorias nomeadas JSON, composição all/any/none e integração em coleções/wiki/reload: 124 JUnit e 6 checks reais de protocolo aprovados. [Evidência](validation/phase2-categories.json).

- Nova alpha local `0.2.0-alpha.1+mc1.21.1-cobblemon1.8.3`: seletor/páginas Nacional, Lendária e Shiny; status por coleção, porcentagens com duas casas e sem falso 100%. [Comportamento](POKEDEX.md).
- Cache por UUID até 5 segundos, invalidado por evento oficial pós-Pokédex, login/logout/reload/lifecycle; cálculo puro separado do adaptador. Nacional usa agregado oficial; Lendária exige forma elegível obtida.
- Validação histórica de porcentagens: **32 casos JUnit** e regressão/progresso em servidor dev e JAR fora do Loom, incluindo datapack e sua remoção. [Relatório](validation/phase2-percentages.json).


- Build Gradle 8.14.3/Loom 1.11.8 com checksum oficial, helper de proxy/cache e JDK 21 completo verificado. Plugin Kotlin 2.2.20 habilita metadata remapping; ICU 71.1 compõe o runtime dev dedicado.
- Inicializador Fabric com lifecycle/readiness/logs e falhas isoladas do Dex; ausência de LuckPerms não impede iniciar.
- Configuração imutável estrita, reload assíncrono, backup validado atômico e recuperação sem sobrescrever JSON inválido.
- SQLite em worker dedicado; migração transacional, WAL/foreign keys, cadastro por UUID e fechamento.
- `/dex`, `/dex status`, seletor de 27 slots somente de leitura (agora acessado pela central de seis linhas) e consulta oficial Cobblemon. Espécies enumeradas dinamicamente; nenhum registro oficial alterado.
- `/dex admin reload` protegido por LuckPerms sem fallback OP/console; negação sem provedor comprovada, permissão positiva/global e por dimensão agora verificada nos relatórios de coleções/contextos.
- Mensagens em recursos separados pt_br/en_us, com idioma global configurável nesta fase.
- Workflow Java 21 com build/testes e upload de JAR/sources/JUnit condicionado ao sucesso. Checklist e continuidade mantidas.

## Em desenvolvimento

- Evolução da wiki, menus avançados, categorias e integrações adicionais de placeholders da Fase 2. Wiki inicial, coleções, categorias nomeadas e critérios JSON já estão implementados; captura shiny histórica permanece indisponível.


- Testes complementares: aparência/gestos em cliente Minecraft real, contextos LuckPerms adicionais e carga. O cenário de dimensão e os pacotes de inventário descritos acima foram verificados.
- Preparação incremental da Fase 2, respeitando limitações reais de shiny e de espécies/datapacks.

## Pendente

- Fases 2–8: wiki completa/cobertura adicional de spawn, categorias/datapacks adicionais, TAB/alias legado e placeholders avançados, recompensas, capturas/estatísticas, rankings, competições, conquistas/perfis e integrações avançadas.
- Cliente visual e testes de carga; herança/contextos LuckPerms além do cenário de dimensão verificado.
- Layouts JSON completos e documentação das funcionalidades ainda não implementadas.
- Licença do código próprio e release estável. Nenhuma configuração de ambiente foi alterada/publicada nesta entrega.

## Testes e verificações executados

Histórico alpha.4: 214 JUnit e 30 checks agrupados de Pokémon/layout/regressão; provas em phase3-pokemon.json e layout-home.json. Relatórios anteriores abaixo preservam suas próprias versões/escopos.

Histórico alpha.2: 179 JUnit e 10 checks reais sem falhas; [relatório](validation/phase3-saved-items.json). Resultados das etapas anteriores abaixo são históricos e não substituem a evidência desta etapa.

Entrega anterior de categorias: build e 124 JUnit sem falhas/erros/skips; 6 checks de categorias em servidor dev com LuckPerms, sem falhas. Servidor encerrado, SQLite integrity_check=ok, propriedades/configuração restauradas e senha temporária removida. [Relatório](validation/phase2-categories.json). Rejeições esperadas de configuração inválida aparecem nos logs dos testes negativos; não são falhas da suíte.

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
- Comando givekey do Crates por console não é suportado no contrato auditado. Na alpha.9, o runner emite como jogador autorizado e testa a recompensa SAVED_ITEM preservando autenticidade; CRATE_KEY dedicado não implementado.

## Última alteração relevante e sincronização

Branch `handoff/crates-checkpoint`, base enviada `26c4472` (recuperação); entrega atual por assunto `feat: track verified native captures and permanent statistics`. Autorização contínua para commit/push nesta branch; conferir SHA com Git. Main permanece `c89cc1b`; nenhuma release/ambiente publicada nesta entrega. Histórico abaixo preserva versões anteriores.

## Publicação alpha confirmada

Usuário autorizou publicar. [Pré-release v0.1.0-alpha.1](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.1.0-alpha.1) publicada, não draft e não estável, com tag apontando para `c11b3660aaf567d161b03ac91c32d9d1de2501af`. Workflow [42358003410](https://github.com/istwi/Shirokai-Dex/actions/runs/42358003410) terminou com sucesso e executou os 20 casos JUnit novamente.

Cinco anexos: JAR instalável, sources JAR, SHA256SUMS.txt, BUILD_PROVENANCE.json e phase1-foundation.json. Todos foram baixados da release; checksums e proveniência verificados. Evidência em [release-alpha1.json](validation/release-alpha1.json). O relatório phase1 descreve o teste local de servidor e seu hash local; os binários publicados são de CI e têm checksums próprios, sem confundir as duas evidências.

Upload direto da sessão recebeu HTTP 401; resolvido pelo token do próprio GitHub Actions, sem chave pessoal nem alteração do ambiente. O workflow é específico desta alpha e recusa nova publicação se a release já não estiver em rascunho. Não recriar ou sobrescrever a release.

## Última entrega de progresso

Implementação identificada pelo assunto `feat: add verified Pokedex completion percentages`, baseada no remoto `93185ae`. A alpha.1 publicada permanece imutável; esta versão 0.2.0-alpha.1 será disponibilizada pelo artifact de build do commit e JAR local. Não foi publicada nova release nesta tarefa. Confirmar SHA/push ao encerrar.

## Publicação da alpha.3 confirmada

Usuário autorizou publicar o JAR. [Pré-release v0.2.0-alpha.3](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.2.0-alpha.3) publicada, não draft e experimental, com tag confirmada no commit `47817bff13cdc3464ad926791233801a01354b54`. Workflow [42365493365](https://github.com/istwi/Shirokai-Dex/actions/runs/42365493365) aprovado e 78 JUnit executados no build de publicação.

Cinco anexos baixados da release e conferidos: JAR instalável, fontes, `phase2-wiki.json`, `BUILD_PROVENANCE.json` e `SHA256SUMS.txt`. Checksums GitHub e manifesto verificados; versão interna do JAR, proveniência e alvo da tag confirmados. [Evidência](validation/release-alpha3.json). O relatório de servidor dev mantém seu hash local separado dos binários de CI. Limites de cliente visual/carga/JAR fora do Loom continuam explícitos. A release antiga permaneceu inalterada; nenhuma configuração de ambiente foi publicada.

## Última alteração local — submenus

Estilo compartilhado de coleção/wiki/formas/spawn/prévia/confirmação, barra exata e consulta assíncrona de estados reais do journal. Sem mudança das regras de entrega. Testes desta passagem em `validation/layout-submenus.json`; validação de protocolo/cliente desta alteração ainda pendente. Gradle avisa sobre funcionalidades obsoletas incompatíveis com Gradle 9; não houve atualização de dependências. Sem commit/push/publicação.
