# Próximos passos

## Cabeça do jogador e atalho da wiki — dev.3 (2026-11-15)

Base dev.2/4ac4069 preservada. Cabeça de info usa DataComponents.PROFILE com ResolvableProfile(player.getGameProfile()), incluindo propriedades da skin da sessão. Botão wiki no slot19 (abaixo da cabeça) abre via callback a wiki da coleção selecionada, com validação de acesso existente; Nacional/Special/Shiny mantêm geometria e Fechar/comando. JSON antigo sem wiki ganha padrão, evitando slots ocupados. Build/serverDistribution/verifyModJar e 415 JUnit aprovados; seis checks de protocolo com dois jogadores e três coleções aprovados no JAR empacotado. [Evidência](validation/head-wiki-dev3.json). Servidor encerrado normalmente, SQLite íntegro, RCON/segredo removidos. Renderização da textura em cliente gráfico não certificada. Teste adicional aprovado com duas texturas públicas assinadas distintas: valor e assinatura exatos preservados nas seis cabeças, sem skin fixa no produto. Usuário autorizou envio do JAR após verificação; commit `feat: show personal heads and matching wiki shortcuts`, branch fix/latest-menu-command. Sem merge na main/publicação de ambiente. Próximo passo: conferir artifact do Actions; renderização gráfica permanece não testada.

## Pedido atual — 0.7.0-dev.2 (2026-11-15)

Base correta: tag v0.7.0-dev.1, commit dea5e5e. Botão menu.buttons.close.command nos JSONs de Dex Nacional/Special/Shiny; padrão fecha, comando executa como jogador sem elevação. /dex sozinho permanece inerte. Build/serverDistribution/verifyModJar e 413 JUnit aprovados. Cinco checks nativos do clique aprovados no JAR empacotado: fechar padrão, outro menu, sem elevação, destino ausente e reload. Evidência: [close-command-dev2.json](validation/close-command-dev2.json). Servidor encerrado normalmente, SQLite íntegro, RCON desativado e segredo removido. /menu e /pokedex externos não instalados neste teste. Trabalho na branch fix/latest-menu-command, envio de código/JAR autorizado pelo pedido atual; sem merge na main ou publicação de ambiente. Próximo passo: conferir teste nativo e artifact do commit no Actions. API reutilizada: performPrefixedCommand/createCommandSourceStack do Minecraft1.21.1.

## Envio 0.7.0-dev.1 autorizado

Responsável solicitou o JAR no GitHub. Preparando commit/push na branch handoff/crates-checkpoint e pré-release com JAR SHA256 cb493e8d1ea128b89231a0fb662e2bd389d98bd685b4856262ddc1e72f4e4c88, configurações e evidência dos410 JUnit/15 checks nativos. CI exige igualdade com o binário testado e verifica anexos antes de publicar. Sem merge de main ou publicação de ambiente. Referência: `release: distribute v0.7.0-dev.1 snapshot`.

## Menus diretos — validados localmente (0.7.0-dev.1)

Esta seção substitui as direções históricas abaixo. Implementados `/dex nacional|special|shiny|forms`, `/dexrank captura|derrotados|dex` e `/wiki nacional|special|shiny|forms`; `/dex` sozinho não abre tela. Eventos desativados e antigos painéis públicos sem entrada por comando. Doze JSONs separados, wiki de54 posições com modelos oficiais/relógio/livro/funil, pesquisa pelo chat,18 pacotes tipados e layout do ZIP. **Somente configurações: nenhum jogador/resgate do ZIP importado**, conforme escolha do responsável. [Guia e placeholders](DIRECT_MENUS.md).

410 JUnit, build/serverDistribution e15 checks nativos (12 +3 após reinício) aprovados no JAR SHA256 `cb493e8d1ea128b89231a0fb662e2bd389d98bd685b4856262ddc1e72f4e4c88`. [Evidência](validation/direct-menus.json). Um pacote real entregou12 Poké Balls/4 Potions/$500/1 token e nó LP; concluído uma única vez após reinício. Uma captura PvE nativa atualizou Top Geral/Shiny; captura administrativa e replay não pontuaram/promoveram origem. Zero edições de eventos; SQLite íntegro, dois encerramentos normais e segredo removido. Testados parser real Text Placeholder API, reload independente e rejeição de slots conflitantes; recuperação de snapshot completo também coberta por JUnit. Não certificar cliente gráfico/carga nem GUI externa sem teste próprio.

Origem da falha de ranking no servidor do usuário continua desconhecida; não afirmar causa resolvida com base só no teste local. Código, JAR e ZIP de configuração permanecem locais, sem commit/push/release/merge/ambiente. Base local891df62, branch handoff/crates-checkpoint. Artefatos em build/server-mod e build/direct-menu-config.zip. Nenhum players/*.json externo foi copiado.

A suíte vigente de navegação é `direct`; fixtures históricas dos menus retirados não representam os comandos atuais.

Próxima ação: aguardar ajustes do responsável sobre estes menus; não retomar telas/fases desativadas. Se solicitar envio, distribuir o JAR validado e configurações, com evidência/proveniência e confirmação de SHA remoto. Para o ranking de produção, consultar a origem e os recibos/logs desse servidor antes de inferir causa. Não importar players do ZIP nem repetir onboarding/publicação do ambiente.


## Histórico do pedido — comandos diretos e ZIP DexRewards

O responsável substituiu o hub: /dex sozinho não abre nada; /dex nacional|special|shiny|forms abre cada menu, /dexrank captura|derrotados|dex abre rankings, /wiki <coleção> abre wiki. Wiki6 linhas igual imagem: borda cinza, conteúdo interno28 slots, relógio48 filtro por clique, livro49 pesquisa capturada pelo chat, funil50 limpa. Menus separados configuráveis (nome/item/lore), layout de recompensas5 linhas igual reward_gui.json do ZIP. Eventos ficam desativados; retirar o fluxo público antigo, preservando banco/journal e backend de evento para futuro.

ZIP local: /workspace/attachments/34397f8e-ff24-468f-bfc8-fab8caf6a6c3/DexRewards.zip. Usar só config.json, dex_types/*.json, reward_gui.json e messages.json. O usuário respondeu explicitamente NÃO importar dados/resgates de jogadores. NÃO copiar players/*.json para Git. Nacional/Special têm5,15,30,50,75,100%; Shiny1,3,10,25,50,100%.18 pacotes com itens, impactor:dollars/tokens e permissões shirokai.dex.* (sem gruposVIP). Converter dinheiro em ações Currency existentes e tags em nova ação tipada LuckPerms; preservar journal/ack e compatibilidade de planos antigos. Não habilitar comandos mistos cegos.

Bug relatado: uma captura não atualizou ranking mesmo após reabrir. Origem/método ainda não informado. Cache do banco já é invalidado ao gravar; hipótese inicial de bolsa não confirmada e descartada no teste auditado: Master Ball não tem BagItemLike nativo em1.8.3. API POKE_BALL_CAPTURE_CALCULATED possui thrower/PokemonEntity/PokeBallEntity e permite nova correlação antes da captura com as mesmas regras de origem e dedup. Validar projétil físico em PvE, sem promover UNKNOWN antigo.

Implementados catálogo separado, comandos, pesquisa e18 pacotes; progress ratio/alias nacional.410 JUnit/build aprovados. Próximo: concluir suíte direct com Impactor/LP/API reais e reinício; documentar resultado/JAR. Scripts corrigidos após expectativa de cartão não presente no ZIP, tentativa incorreta de BagItem, CRLF na fixture Impactor, reload usado por console sem permissão e projétil lançado antes de terminar a abertura da batalha. Raiz atual /tmp/shirokai-direct-native-6; log /tmp/shirokai-direct-native-6.log. Origem da captura no servidor do usuário continua desconhecida. Última versão publicada dev.2, branch limpa no início deste pedido (891df62). Nenhuma publicação nova autorizada neste pedido de implementação.

## Dev.2 publicada e verificada

[Pré-release/JAR](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.6.0-dev.2) publicada na fonte/tag d4f1965 por solicitação do responsável. [Actions43620027225](https://github.com/istwi/Shirokai-Dex/actions/runs/43620027225) aprovado; cinco anexos baixados e manifesto conferido localmente. JAR coincide com o validado nos402 JUnit/10 checks nativos, SHA256 0dc0401af48479de0fdd118ddd9c5b9fda5bc985996717cc18c52c557a4003d8. Configuração de exemplo anexada para instalações existentes, preservadas automaticamente. [Evidência](validation/release-focused-dev2.json). Main e ambiente não publicados. Não ampliar as demais fases sem novo direcionamento: modo reduzido permanece a prioridade vigente.

## Envio dev.2 autorizado

Responsável solicitou enviar o JAR ao GitHub. Snapshot em preparação na branch handoff/crates-checkpoint:402 JUnit/10 checks nativos e JAR SHA256 0dc0401af48479de0fdd118ddd9c5b9fda5bc985996717cc18c52c557a4003d8. Workflow recompila/testa e exige coincidência com o binário nativo antes de anexar/publicar; configuração focused-server.json incluída. Sem merge de main ou publicação de ambiente. Referência: commit `release: distribute v0.6.0-dev.2 snapshot`.

## Menus reduzidos — validados localmente

Pedido implementado: hub Dex/Rankings/Eventos, coleções Nacional/Special/Shiny + Forms futura, seis marcos configuráveis com exemplo de1 papel, resgate único, Top10 na lore, eventoTOTAL com inscrição, modelos oficiais e filtro esquerdo/direito, contagem verificada de derrotas selvagens/schema16.402 JUnit e dez checks nativos (nove + reinício) aprovados. Uma captura e uma derrota legítimas persistiram, Pokémon administrativo não pontuou, um único prêmio foi entregue. SQLite íntegro, dois encerramentos normais e segredo de teste removido. [Evidência](validation/focused-menus.json), [configuração e limites](FOCUSED_MENUS.md).

O JAR local coincide por SHA256 com o testado nativamente. Nada deste bloco foi enviado ou publicado; a release dev.1 permanece intacta. Configuração existente não é sobrescrita: o preset revisável está em docs/examples/focused-server.json. Não voltar ao desenvolvimento das demais fases por conta própria: o responsável pediu deixá-las desativadas neste modo. Próximo trabalho, quando solicitado: ajustar prêmios reais e revisar a aparência no cliente Cobblemon. Não declarar capacidade/carga ou renderização visual manual a partir deste teste.

## Direção vigente — menus públicos reduzidos (2026-11-14)

O responsável pediu implementar agora: hub com Dex/Rankings de captura/Eventos; Nacional sem legendary/mythical/ultra_beast, Special somente esses grupos, Shiny por capturas verificadas e Forms apenas ícone futuro. Cada Dex junta Wiki com seis marcos percentuais configuráveis e resgate ao clicar. Percentuais iniciais10/25/50/75/90/100; exemplo1 papel aos10% Nacional. Rankings Top10 na lore (três Dex + Forms bloqueado; capturas geral/shiny/lendários; derrotados). Eventos apenasTOTAL, entrada inscreve, tela mostra tempo/score/Top10; fechado sem ação. Wiki usa PokemonItem oficial e filtro esquerdo/direito. Rolagem exige cliente adicional e foi substituída por cliques a pedido do responsável. Implementação local em compilação/teste nativo, ainda não certificar concluído.

Schema16 adiciona recibos e totais de derrotas nativas PvE. Não conta PvP/NPC/multijogador/origem não comprovada nem inventa histórico. Retransmissão battle+pokemon deduplicada. Novo preset config/focused.json aplicado somente em instalações sem config; configurações existentes preservadas e exemplo em docs/examples/focused-server.json. Modos clássicos permanecem disponíveis por home.focused=false; com focused=true os painéis profile/settings/achievements/prizes/history ficam bloqueados.

Bloco anterior avançado: schema15/notificações e placeholders/layouts passaram398 JUnit e13 checks nativos com API real/sons/title/actionbar/reinício; evidência phase7-public-placeholders.json. Nova base de menus chegou a402 JUnit/build aprovados antes do último ajuste de captura shiny e horários. Próximo exato: concluir build /tmp/shirokai-focused-final-build.log e executar suite focused em raiz /tmp nova; primeiro preparo falhou antes de iniciar por ausência de remapCaptureProbeJar, corrigido e segredo limpo. Servidor não iniciado naquela tentativa. Depois corrigir os testes reais e registrar evidência. Nada publicado após dev.1.

## Continuação dev.2 — em desenvolvimento

Novos placeholders públicos (stats/rank/medals/victories/claimed_rewards) com cache limitado e privacidade por revisão; cinco testes adicionais passaram no primeiro build (387 totais). Prévia pelo parser e alias legado em teste nativo com API2.4.2+1.21. Novas telas conectadas ao layout JSON existente, com slots protegidos e regressão adicional em compilação. Nada deste bloco foi publicado; release dev.1 permanece imutável. Próximo: concluir essas suítes, documentar evidências e seguir para notificações/idioma/recálculo e testes finais.

## Publicação dev.1 confirmada

[Pré-release com JAR](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.6.0-dev.1) publicada pelo [Actions aprovado](https://github.com/istwi/Shirokai-Dex/actions/runs/43585022162), fonte 9714b99. Quatro anexos baixados e manifesto SHA256 conferido localmente. Binário publicado idêntico ao JAR validado nativamente. [Evidência](validation/release-development-0.6.0-dev.1.json). Código/perfis/validação complementar enviados na branch handoff/crates-checkpoint; main e ambiente preservados. Pendências funcionais continuam abertas.

## Validação complementar de perfis — 2026-11-14

Suíte nativa concluída: nove checks e um de reinício aprovados, três inicializações/encerramentos normais e segredo RCON removido. [Evidência](validation/phase6-profiles.json), incluindo falha inicial e repetição corrigida. Perfis offline, privacidade pública, Pokédex oficial, conquistas/favoritos e ausência de concessões materiais indevidas verificados por protocolo. Próximo: placeholders avançados/menus/notificações e demais pendências; cliente gráfico/carga não certificados. Esta atualização substitui as menções anteriores de repetição pendente.

## Passagem 0.6.0-dev.1 — estado vigente

Última tarefa: preparar snapshot publicável na branch handoff/crates-checkpoint por solicitação explícita do responsável. Build e 382 JUnit aprovados; schema14/perfis/conquistas estão ativos (as notas históricas de preparação abaixo estão superadas).

Próxima tarefa exata: concluir scripts/integration_server.py --base /tmp/shirokai-packaged-base-v2 --suite profiles --root /tmp/shirokai-profiles-native-v2; investigar qualquer falha sem enfraquecer expectativas. A primeira execução falhou em clique de janela obsoleta; correção de token da tela e estabilização do cliente aplicadas. Registrar resultado antes de considerar Fase6 validada nativamente.

Depois: placeholders avançados com cache limitado e privacidade, integração real da API opcional/alias legado, menus JSON para novas telas, notificações, preferências de idioma, recálculo administrativo de categorias com prévia/motivo/auditoria, carga e validação final. CRATE_KEY dedicado e versões externas não auditadas permanecem sem suporte comprovado. Não certificar cliente gráfico ou modpack de produção a partir de bots.

Arquivos centrais: core/ShirokaiDex.java, database/Database.java, profiles/, achievements/, menus/, placeholders/, V014, Profile*Test/AchievementTest e tests/integration/profiles.cjs. Preservar migrações e fixtures legadas; nenhuma alteração na Pokédex oficial. Publicação desta versão não autoriza alterar main/configuração do ambiente.

## Trabalho ativo — continuidade sem parar

363 JUnit e blocos nativos 15+6+9 aprovados; ver relatórios phase5*. Complemento de histórico/auditoria tem duas novas classes de cenário em CompetitionTest ainda aguardando próxima execução integral após última edição. Perfil/privacidade/Fase6 em preparação: StatisticsPrivacy/ProfileProgress/CapturedForms/V014 existem no fonte, mas schema14 e lifecycle ainda não ativados. Próximo: requisito/conquistas persistentes, snapshots oficiais de perfil e privacidade aplicada a ranking/perfil/placeholders. API org.sqlite.Collation.create/ xCompare conferida por javap no sqlite-jdbc3.47.1.0 para comparação racional exata (sem índice persistente com collation customizada). Preservar todos os arquivos locais; não publicar e não encerrar a cada fase.


## Trabalho ativo — competições

Correção de SNAPSHOT shiny em CompetitionStore.matches, teste shinyCaptureCanEvaluateFrozenCategory..., build em /tmp/shirokai-competition-shiny-fix.log. Próxima ação: após build, repetir scripts/integration_server.py --base /tmp/shirokai-packaged-base-v2 --suite captures --competitions --root /tmp/shirokai-competition-native-v2. Primeira tentativa falhou na classificação shiny; mundo descartável encerrado e senha removida. Fonte completa/evidência de falha em build/integration-captures.json (preservar em relatório final). Depois ativar/testar prêmios e resgate: PrizePolicy/RewardSnapshot/CompetitionAwards/V013 existem no fonte, mas schema13/modelo de política ainda não integrados. Store já implementa adjust/revert; falta exposição admin. Retenção protege edições pendentes/auditorias.


## Continuação contínua solicitada — 2026-11-14

O responsável pediu executar todas as fases sem encerrar a cada bloco, com testes normais e sequência otimizada. Sem publicação. Recordes/rankings de capturas validados com 344 JUnit/7 checks nativos. Em desenvolvimento: calendário/inscrição/cancelamento/pontuação/revisão de competições, CompetitionDefinition e V012 ainda não integrados ao runtime. Preservar schema11 e registros permanentes; integrar holds de histórico antes de ativar pontos competitivos.


## Continuação local — recibos e retenção (2026-11-14)

Base remota continua 88f923e; histórico e novo bloco permanecem locais. Schema10 com capture_receipts, migração/backfill sem inventar eventos, histórico verificado opcional e cleanup limitado. Build/JAR e 337 JUnit aprovados; migração/limpeza/reinício nativos aprovados ([evidência](validation/phase4-retention.json)). Próximo: recordes e rankings permanentes com data de atingimento/UUID para desempate, sem inventar data histórica indisponível. Antes de competições/revisão de capturas, adicionar proteção contra limpeza dos detalhes necessários; ver RETENTION.md. Não publicar nada nesta retomada.


## Retomada local — 2026-11-14

Base real 88f923e, branch handoff/crates-checkpoint. Novo histórico administrativo `/dex admin captures <player_uuid> [page]`, permissão shirokaidex.admin.captures; guia CAPTURE_HISTORY.md. Build/JAR e 328 JUnit aprovados; seis checks do comando/ACL em servidor Loom passaram ([evidência](validation/phase4-history.json)); fora do Loom pendente. Próximo bloco: implementar projeções permanentes de períodos/recordes/rankings e deduplicação independente ANTES de retenção: summary hoje consulta capture_events e event_key é sua barreira contra replay. Não apagar eventos sem preservar ambos. Alterações desta retomada locais.


**Estado atual de desenvolvimento:** alpha.5, coleção Shiny ligada à captura verificada; release pública alpha.1 preservada. Próximo: histórico, recordes/rankings e retenção. Histórico: captura/estatísticas iniciais 0.4.0-alpha.1 sobre `26c4472`, branch `handoff/crates-checkpoint`, autorização contínua. Main preservada `c89cc1b`. [Guia](CAPTURES.md), [evidência](validation/phase4-captures.json). Referência: `feat: track verified native captures and permanent statistics`. Sem release/merge/ambiente.

## Publicação concluída

[v0.4.0-alpha.1](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.4.0-alpha.1) publicada e verificada, tag/source `78dfe72`. Workflow [42432155833](https://github.com/istwi/Shirokai-Dex/actions/runs/42432155833) passou e quatro anexos foram baixados/conferidos também neste ambiente. JAR coincide com o binário nativo validado; 304 JUnit/18 checks documentados. [Evidência](validation/release-captures-alpha1.json). Não alterar essa release/workflow de source fixo para distribuir desenvolvimento posterior. Proveniência persistente já avançou na alpha.2; próximo bloco audita demais fluxos nativos; main/ambiente não publicados.

## Última tarefa realizada

Alpha.5: coleção Shiny verificada, schema9 e cache/projeção permanente; 326 JUnit, suítes nativas no [relatório](validation/phase4-shiny.json). Entrega `feat: derive shiny collection progress from verified captures`, base b359a12. Próxima tarefa é histórico/rankings/recordes/retenção. Release somente após toda a Fase4, conforme esclarecimento do responsável.

### Histórico alpha.4

Alpha.4: pesca nativa, fonte persistente e schema8; 317 JUnit, suite fishing e regressões registradas no [relatório](validation/phase4-fishing.json). Entrega `feat: track verified native fishing capture provenance`, base d967c98. CaptureAccess/NativeSpawnProof/NativeProvenance/V008/Database, quatro testes novos, probe/fixture/runner fishing e seeder/executor. Sem nova release. Próxima instância começa a ponte de capturas shiny, preservando prova/denominador e permissões.

### Histórico alpha.3

Alpha.3: captura PvE nativa/LIVE_NATIVE, replay e reinício aprovados; 313 JUnit e dez checks nativos. Auditoria estática da pesca identificou causa/spawner próprios, sem ampliar origem aceita. Entrega `test: validate native battle capture provenance`, base3f4adaf; [evidência](validation/phase4-battle.json). Arquivos novos/alterados: CaptureProbe, captures.cjs, build.gradle e documentação. Próxima instância deve continuar a pesca, sem refazer batalha/ledger. Sem nova release.

### Histórico alpha.2

Alpha.2: proveniência persistente, nove JUnit novos (313 totais), captura real após reinício e regressão de recuperação no schema7. Entrega `feat: preserve observed native spawn provenance across reloads`, base a082358. [Evidência](validation/phase4-provenance.json). Sem nova release.

### Histórico alpha.1

Captura nativa comprovada no escopo PlayerSpawner/pool mundial, correlação pré-hit/pós-captura e identidade Pokébola/Pokémon/jogador. Schema6, dedup persistente, snapshots de categorias/regras e agregados permanentes; resumo próprio `/dex profile captures` e consultas por espécie/categoria. Períodos respeitam statistics_timezone (UTC padrão). 304 JUnit e 18 checks nativos/reinício, incluindo regressão de recuperação no schema6.

Caso positivo usou Pokébola/motor reais em criaturas realmente geradas pelo pool; probe não atribui causa nem emite a primeira captura artificialmente. Caso administrativo sem SpawnCause ficou UNKNOWN, e replay não duplicou. Proveniência é perdida no reload da entidade: criatura natural preservada após reinício ficou UNKNOWN, conforme limite explícito. Shiny% antiga segue N/D; estatística shiny nova é separada. Testes encerram/salvam servidores, conferem SQLite e removem segredo RCON automaticamente.

COMMAND enviado em `73e5d78`, recuperação/executor em `26c4472`. Não refazer blocos já validados. CRATE_KEY dedicado continua sem contrato público suficiente no Crates0.2.1.

## Histórico de distribuição

O bloqueio antigo de uploads da alpha.4 permaneceu limitado àquela tentativa. A release0.4.0-alpha.1 foi publicada pelo Actions com anexos e validada independentemente; não permanece bloqueada. Download de artifacts do Actions via host de armazenamento continua bloqueado no Cloud, mas download dos anexos da release pública funcionou. Não solicitar credencial em chat nem alterar ambiente automaticamente.

## Próxima tarefa exata

1. Protocolo Git/AGENTS, preservar trabalho e confirmar envio. Autorização contínua somente na branch; main/release/ambiente separados. Blocos maiores, testes relevantes e documentação objetiva.
2. Proveniência persistente concluída no escopo PlayerSpawner/reload na alpha.2: não reimplementar. Consultar [evidência](validation/phase4-provenance.json), NativeProvenance/CaptureObservation/V007 e nove testes novos. Provas consumidas não reabrem; eventos anteriores não são reclassificados.
3. Captura PvE/PlayerSpawner e pesca nativa validadas no escopo dos relatórios alpha.3/alpha.4. Não reimplementar. Pesca usa singleton/causa/vara/jogador observados, native_source e schema8; [guia](FISHING.md). Outros pools/iscas/varas, batalhas duplas e extensões continuam pendentes. UNKNOWN nunca vira origem legítima por suposição.
4. Próxima tarefa funcional: histórico paginado próprio/administrativo com permissões, rankings permanentes e recordes; depois retenção que preserve agregados/períodos/provas consumidas. A ponte Shiny já foi implementada: não reimplementar. Consultar [guia](SHINY_CAPTURES.md), CapturedShinyForm/ProgressCalculator, projeção V009/CaptureLedger/Database e cache assíncrono em CobblemonAccess/core. Capturas repetidas são ações múltiplas, mas uma espécie na coleção. Nunca inferir prova de visto/obtido.
5. Reutilizar `scripts/integration_server.py --suite captures|fishing|recovery|commands`, base com assets oficiais. Captures requer build/remapCaptureProbeJar, instala probe/fixture só em /tmp. Schema9: testes de recuperação/seeder já adaptados. Não instalar probes em servidores de usuários.
6. Atualizar checklist/status/next/evidência, commitar/push na branch e confirmar SHA. Usuário esclareceu: publicar release somente após toda a Fase4 (histórico/retenção/recordes/rankings incluídos). Main e ambiente não autorizados.

## Arquivos importantes

- Capturas: pacote captures (Record/Tracker/Access/Ledger), V006, Database, timezone em Settings/ConfigManager, eventos e comandos em ShirokaiDex. CaptureTest, captureProbe separado/remapeado, fixture/runner captures e executor.

- Alpha.11: RewardJournal.resumable/authorizeResume, Database.authorizeRewardResume, RewardDelivery.resume, previewRewardResume/deliverReward no core, MainMenu.confirmRecovery, RewardRecoveryTest; fixture/runner/seeder recovery e `scripts/integration_server.py`.

- COMMAND alpha.10: CommandReward/CommandPolicy/CommandRewardAdapter, CommandRewardsCodec/Settings, RewardPlan.definition/reuse/commands, RewardJournal.steps/RewardDelivery.ManualReview, core/menus/traduções; CommandRewardsTest e fixture/runner commands em dois modos. Nenhuma migração SQLite nova.

- Checkpoint: PlayerSaveCoordinator/ItemRewardAdapter, `src/checkpointProbe`, `tests/integration/crates-checkpoint.cjs` (quatro modos), manifestos/fixture crates-checkpoint. Remapeamento Pokémon de regressão em build.gradle; ver relatório e passagem atualizados.

- Moedas: CurrencyReward, CurrencyRewardAdapter/Port, ImpactorCurrencyPort, CurrencyPersistence; RewardPlan/RewardsCodec/RewardJournal.steps, core/menus/localização. CurrencyRewardsTest/CurrencyPersistenceTest, fixture e runner currency.cjs (normal/without-impactor/after-restart), manifesto oficial runtime-mods.json. Guia CURRENCY_REWARDS.md.

- Aparência: `MenuAppearance`, `MenuNavigation.Layout`, `MenuStyle.appearance`, `MenuAppearanceTest` e CollectionValidator (snapshot de ícones). Runner/fixture `menu-appearance`, seeder com `--fixture menu-appearance`; exporta metadados de teste, não dados de usuários.
- Nova navegação: `menus/layout/MenuNavigation`, `config/MenuNavigationCodec`, `Settings.menus`, `MenuStyle.navigation`, `MenuNavigationTest`; fixture/runner `submenus`, seeder reservado para servidor descartável parado.
- Layout: `HomeMenu`, `menus/layout/HomeLayout`, `HomeLayoutCodec`, `MainMenu.show`/tamanhos e callbacks em ShirokaiDex; HomeLayoutTest, runner home-layout.cjs, fixture layout e probe separado layoutProbe. Helpers antigos de teste agora entram no seletor clicando na central.
- Pokémon: `PokemonReward`, `PokemonRewardsCodec`, `PokemonPayload`, `PokemonRewardAdapter`, RewardPlan.steps/definition/reuse e journal compatível. `pokemonProbe` só de testes; nunca incluir no JAR. Leitura assíncrona de NBT não escreve stores nem consulta registries fora da thread do servidor.

- `SavedItem`, `ItemPayload`, `RewardPlan.Item.data`, `RewardsCodec`, `ConfigManager.resolvedBackup`, migração V005 e APIs salvas no Database. Core agenda validação de payloads na thread do servidor; shutdown cancela esperas. `SavedItems*Test`, runner `saved-items.cjs` (normal/after-restart/review-list) e fixture saved-items.

- Pacote `rewards`, `RewardsCodec`, migrações V003/V004, core/MainMenu/idiomas, `Reward*Test`, runners `rewards.cjs`/`reward-unlocks.cjs` e fixtures de recompensas. Versão local 0.3.0-alpha.1; 159 JUnit e 25 checks de servidor. Adaptador relê inventário NBT porque save vanilla suprime exceções; falha real foi testada e ficou em NEEDS_REVIEW após reinício.

- `categories/CategoryCatalog`, `config/CategoriesCodec`, `CollectionDefinition.CategorySelection`, Settings/ConfigManager/CollectionValidator; `CategoryCatalogTest`, `CategoryIntegrationTest`, runner `tests/integration/categories.cjs` e fixture `tests/fixtures/categories/config.json`. Categoria é regra sem ACL própria; acesso pertence à coleção.

- `spawning/SpawnAccess.java`, `SpawnCatalog.java`, `SpawnCatalogTest`, lifecycle em `ShirokaiDex`, `MainMenu.spawnItem`, `tests/integration/spawns.cjs` e `tests/fixtures/spawning`: entrega alpha.6.

- `Wiki.java` (Request/Facet/Slice), `MainMenu.openWiki`/`formItem`, `WikiFacetsTest`, `tests/integration/wiki-facets.cjs` e `tests/fixtures/wiki-facets`: entrega atual.

- Pacote `placeholders`, `PlaceholderTest`, `TextPlaceholderAccess`, lifecycle em `ShirokaiDex`, runner `tests/integration/placeholders.cjs`, fixture `tests/fixtures/placeholders/runtime.json` e probe separado em `src/placeholderProbe`: entrega atual. [Sintaxe e limites](PLACEHOLDERS.md).

- `wiki/Wiki.java`, `WikiTest`, `MainMenu.openWiki`, `CobblemonAccess.wiki`, comandos em `ShirokaiDex`, `tests/integration/wiki.cjs` e [guia](WIKI.md): entrega atual.

- `scripts/server_proxy.init.gradle`, `tests/integration/contexts.cjs`, `tests/fixtures/contexts/config.json` e [evidência de contextos](validation/phase2-contexts.json): complemento de validação mais recente.

- [Coleções JSON](COLLECTIONS.md), CollectionDefinition, CollectionsCodec, CollectionValidator e os testes CollectionsConfigTest/ConfiguredProgressTest; runtime em tests/integration/collections.cjs e fixture tests/fixtures/collections.


- [Porcentagens e coleções](POKEDEX.md), `src/main/java/br/com/shirokaidex/pokedex`, `DexProgressTest`, `tests/integration/progress.cjs` e `tests/fixtures/datapacks/dex-progress`: trabalho atual. A forma padrão precisa ser incluída mesmo quando Species.forms está vazio.


- [Checklist](MASTER_CHECKLIST.md), [prompt integral](PROJECT_SPECIFICATION.md), [auditoria](API_COMPATIBILITY.md), [estado](PROJECT_STATUS.md).
- `src/main/java/br/com/shirokaidex/core/ShirokaiDex.java`: lifecycle, comandos e integração assíncrona.
- Pacotes `config`, `database`, `cobblemon`, `menus`, `permissions`, `localization`: fundação existente.
- Recursos `database/V001__foundation.sql`, traduções e `fabric.mod.json`.
- `src/test`, `tests/integration/foundation.cjs`, `scripts/gradle_cloud.py`: testes e helper.
- [Instalação](INSTALLATION.md), [testes](TESTING.md), [configuração](CONFIGURATION.md), [banco](DATABASE.md), [diagnóstico](TROUBLESHOOTING.md).

## Dependências e problemas ainda não resolvidos

Minecraft 1.21.1; JDK 21; Fabric Loader 0.17.2; Fabric API 0.116.6+1.21.1; Cobblemon 1.8.3+1.21.1; Fabric Kotlin 1.13.6+kotlin.2.2.20; Gradle 8.14.3; Loom 1.11.8; plugin Kotlin 2.2.20; ICU dev 71.1; SQLite JDBC 3.47.1.0; LuckPerms API compileOnly 5.4; JUnit 5.11.4.

Na máquina atual, JDK verificado em `/workspace/.tools/jdk-21.0.13+9`. Build usado:

```sh
JAVA_HOME=/workspace/.tools/jdk-21.0.13+9 python3 scripts/gradle_cloud.py -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts --no-daemon build
```

Não presumir caminhos/caches/processos em outra máquina. Para `runServer` cloud, acrescente `--init-script scripts/server_proxy.init.gradle` ao comando acima, trocando `build` por `runServer`; a JVM filha precisa do proxy para dependências LuckPerms. `runServer` requer terminal interativo e configuração descartável com EULA aceita pelo responsável; veja TESTING. Diretórios ignorados e `/tmp` não são fontes de continuidade.

Crates indicado: `https://github.com/istwi/Shirokai-Crates`, revisão `05b3995c4d4466ef17c6171ad07d4f0eab512db9`, versão declarada 0.2.1. Impactor 5.3.5/JSON nativo foi integrado e testado na alpha.8; demais backends não certificados. Não inventar métodos/coordenadas nem comandos console de chaves.

## Testes pendentes e cuidados

- Fixture atual tem 3 espécies de teste (2 implementadas e 1 não implementada). Runner de coleções exige perfil descartável novo; MC_COLLECTIONS_PLAYER permite nome diferente. RCON usa arquivo de senha temporário fora do Git e somente loopback. Não reutilizar dados de jogadores reais nem registrar credenciais.


- Capturas, competições, datapacks reais adicionais e carga ainda não validados funcionalmente; recompensas/economia apenas nos escopos dos relatórios. Duas espécies de fixture namespaced foram testadas; não generalizar para todo ecossistema. Percentual shiny deve permanecer N/D até haver fonte de captura verificável.
- Não modificar a Pokédex oficial nem chamar concessões para fabricar progresso.
- Não prometer atomicidade SQLite/inventário/APIs externas.
- Não depender de processos vivos; servidores de teste devem ser encerrados e dados locais ficam fora do Git.
- Preservar inscrição, cancelamento definitivo por edição, calendário, faixas exclusivas e estatísticas permanentes independentes.
- Conferir Actions pelo commit; existência do workflow não prova artifact publicado.

## Publicação da alpha.3 confirmada

Usuário autorizou publicar o JAR. [Pré-release v0.2.0-alpha.3](https://github.com/istwi/Shirokai-Dex/releases/tag/v0.2.0-alpha.3) publicada, não draft e experimental, com tag confirmada no commit `47817bff13cdc3464ad926791233801a01354b54`. Workflow [42365493365](https://github.com/istwi/Shirokai-Dex/actions/runs/42365493365) aprovado e 78 JUnit executados no build de publicação.

Cinco anexos baixados da release e conferidos: JAR instalável, fontes, `phase2-wiki.json`, `BUILD_PROVENANCE.json` e `SHA256SUMS.txt`. Checksums GitHub e manifesto verificados; versão interna do JAR, proveniência e alvo da tag confirmados. [Evidência](validation/release-alpha3.json). O relatório de servidor dev mantém seu hash local separado dos binários de CI. Limites de cliente visual/carga/JAR fora do Loom continuam explícitos. A release antiga permaneceu inalterada; nenhuma configuração de ambiente foi publicada.
