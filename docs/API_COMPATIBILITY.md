# Auditoria de compatibilidade — Fase 0

## Snapshot 0.6.0-dev.1

Schema14/perfis/conquistas integrados às APIs existentes; comparação racional usa org.sqlite.Collation.create/xCompare em sqlite-jdbc3.47.1.0, sem índice persistente dependente da collation. Build e 382 JUnit aprovados. API opcional de placeholders permanece isolada; alias dexrewards:player_dex_percent e máscara pública acrescentados, integração positiva ainda pendente. Não certificar TAB/SkiesGUIs ou CRATE_KEY sem versão/contrato e teste real. Ver [perfis](PROFILES.md) e relatório development-0.6.0-dev.1.json.

## Rankings e recordes — schema11 local

APIs de menus e SQLite já auditadas, sem nova dependência externa. 344 JUnit e 7 checks nativos em Loom, inclusive migração10→11/menus/privacidade/ACL/recordes. Datas históricas incompletas ficam N/D; ver [contrato](RANKINGS.md) e [evidência](validation/phase4-rankings.json).


## Recibos/retenção — schema10 local (2026-11-14)

Sem novas APIs externas. JDBC/SQLite e lifecycle/tick Fabric existentes: agenda no máximo um lote a cada 60s e executa somente no worker. Migração transacional, recibo e counters na mesma transação. Nove testes novos, 337 totais aprovados; scheduler/migração/limpeza/reinício nativos aprovados em Loom. [Evidência](validation/phase4-retention.json). Não certifica nova instalação fora do Loom nem captura real neste bloco. Competições/revisões futuras precisam integrar proteção de retenção antes de ativação. [Contrato](RETENTION.md).


## Histórico administrativo — 2026-11-14 (local)

Consulta JDBC paginada usa schema9 existente, sem nova API externa. Comando reutiliza Brigadier e ACL LuckPerms já utilizados. Leitura no worker SQLite; callback na thread do servidor revalida permissão/sessão. VERIFIED no escopo: 328 JUnit, seis checks de comando/ACL em servidor Loom/Fabric/Cobblemon com LuckPerms5.4.140. Histórico populado/reinício cobertos por JUnit; comando em JAR fora do Loom/cliente visual/carga pendentes. [Evidência](validation/phase4-history.json).


Data: 2026-11-07. Esta é uma auditoria estática inicial; não uma certificação de funcionamento em servidor. O repositório estava vazio, sem HEAD, fontes, build ou testes. Java local confirmado: OpenJDK 21.0.13.

## Atualização — checkpoint Crates alpha.9 (branch local)

Crates0.2.1, fonte `05b3995c4d4466ef17c6171ad07d4f0eab512db9` e release SHA-256 `49a82904ef3fa971c535191657a592b9ee5affad5577913f1bb537ca1274adfd`. `ShirokaiCrates.service().checkpoints()`, `healthy()`, `blocked(UUID)` e `save(ServerPlayer, true): CompletableFuture` exercitados no binário real remapeado. Mixin de save vanilla de jogadores managed enfileira escrita; retorno de saveAll sozinho não é comprovação. Produção usa somente métodos públicos por reflexão opcional e versão auditada; sem Crates usa fallback vanilla, provedor carregado inválido/unhealthy bloqueia itens.

Writer nativo atrasado 2000ms: journal permaneceu PROCESSING após mutação, completando após future/readback (ACK observado em 2009/2007ms). SAVED_ITEM de chave física real preservou componentes e passou por `Tokens.matches`; apenas o probe de teste acessa campos privados para atraso e verificação nativa, sem ler/expor segredo. Falha real de gravação leva a revisão; reinícios e fallback testados. Não implementa CRATE_KEY dedicado nem certifica outras versões/segredos. [Evidência](validation/phase3-crates-checkpoint.json), [procedimento](CRATES_CHECKPOINT_HANDOFF.md).

## Estados e evidências

- `VERIFIED`: apenas o fato explicitamente acompanhado de evidência foi confirmado.
- `PENDING_TEST`: API localizada, mas integração, compilação ou comportamento em servidor ainda não testados.
- `UNSUPPORTED`: contrato solicitado não demonstrado pela API examinada; não implica impossibilidade de todas as alternativas.

A auditoria estática inicial não executou testes funcionais. As validações posteriores da fundação estão separadas abaixo; captura, resgate e economia continuam sem testes funcionais.

## Atualização: resumos de spawn — alpha.6

Pool mundial auditado e exercitado em servidor: resumos parciais de regras simples, IDs namespaced, biomas/tags, dimensões e ticks, sem avaliar spawn ou inferir forma. APIs e limites na seção de menus abaixo; [17 checks de protocolo e 102 JUnit](validation/phase2-spawns.json).

## Atualização: filtros e formas da wiki — alpha.5

Nenhum novo getter Cobblemon: tipos, labels, formas e estados vêm do snapshot imutável já auditado. `MenuType.GENERIC_9x4` foi conferido por `javap` no Minecraft mapeado 1.21.1, compilado e exercitado pelo protocolo real; wiki passa a quatro linhas, enquanto seletor/progresso permanecem em três.

Tipos e categorias (labels) secundários são aplicados junto aos critérios JSON **na mesma forma**. Estados visto/obtido/shiny visto são calculados somente nas formas elegíveis quando existe esse escopo, sem transferir obtenção da forma normal para uma variante. Sem escopo por forma, o agregado oficial da espécie é preservado. Navegação não modifica dados oficiais/configuração/denominadores.

Schema da fixture própria conferido nos dados originais `data/cobblemon/species/generation2/unown.json` de Cobblemon 1.8.3: campos name, primaryType, labels, aspects e battleOnly, além das dimensões. Nenhum código ou asset de Pokémon foi copiado. Fixture `dex_test:variants` adiciona 40 formas alternativas e a padrão: **41 formas enumeradas e visitadas no servidor**, incluindo a última além do corte antigo de 32 linhas. O Unown original tem 27 alternativas e não bastaria para testar esse limite.

`VERIFIED`: opções de tipo/label derivadas da coleção, paginação de opções/formas, cruzamento na mesma forma, estados por forma, preservação de consulta/filtros, inventário somente de leitura, revogação com botões antigos e ajuste de página/idioma após reload. **95 JUnit, 8 checks novos e 8 de regressão da wiki** em servidor dev real com LuckPerms. [Evidência](validation/phase2-wiki-facets.json).

Spawn, categorias completas/históricas, cliente visual, carga e novo JAR instalado fora do Loom permanecem pendentes. Esta entrega não altera placeholders nem certifica novamente integrações externas. [Guia](WIKI.md).

## Atualização: placeholders básicos — alpha.4

Text Placeholder API **2.4.2+1.21**, revisão `2427627df3e73ec00a2d3623c7e60e1d076bdb67`, auditada nas classes `Placeholders`, `PlaceholderContext`, `PlaceholderResult` e `PlaceholderHandler` da tag oficial. Artefato original da release GitHub, sem código copiado nem dependência aninhada; URL/SHA-256 em `tests/fixtures/placeholders/runtime.json`. Maven Nucleoid respondeu negação do proxy; o mesmo artefato oficial foi resolvido via Ivy/GitHub com TLS ativo, sem alterar rede/versões.

Assinaturas Yarn remapeadas pelo Loom ao ambiente Mojang: `Placeholders.register(ResourceLocation, PlaceholderHandler)`, `getPlaceholders()`, `remove(...)`, `parseText(Component, PlaceholderContext)`, `PlaceholderContext.of(ServerPlayer)`/`of(MinecraftServer)`, `context.player()`, `context.server()` e `hasPlayer()`. `PlaceholderResult.value(Component.literal(...))` evita o overload String que interpreta formatação. O parser real confirmou `%shirokaidex:percent national%` e argumento omitido para Nacional.

`VERIFIED` para cinco métricas e contexto de jogador na thread do servidor: percent, registered, seen, total e remaining; critérios por coleção, permissões atuais, idioma, eventos oficiais e reload. Expansões leem snapshots preparados por uma fila limitada a quatro jogadores solicitados/invalidados por fim de tick, sem calcular espécies ou acessar SQLite no handler. Contexto ausente/off-thread e dados sem evidência produzem N/D; não há captura shiny reconstruída nem estatísticas fictícias.

87 JUnit (nove novos), oito checks de integração normal, cinco de probe pelo parser real e 13 da fundação sem as duas dependências opcionais. A fixture externa registrou colisão de namespace e alias legado antes do startup, substituiu outro handler depois e conferiu limpeza no SERVER_STOPPED: os provedores externos foram preservados. O probe não integra o JAR distribuído. [Evidência](validation/phase2-placeholders.json), [sintaxe/limites](PLACEHOLDERS.md).

`PENDING_TEST`: TAB/bridge Fabric, compatibilidade positiva do alias legado, desempenho, cliente visual e nova alpha instalada fora do Loom. O alias `dexrewards:player_dex_percent` não é registrado nesta entrega; foi verificada apenas a preservação de outro provedor existente.

## Atualização: wiki inicial — alpha.3

API nova utilizada: `com.cobblemon.mod.common.pokemon.Species.getName(): String`, conferida com `javap` no artefato Maven fixado `com.cobblemon:fabric:1.8.3+1.21.1`, depois compilada e executada no servidor real. `PokemonSpecies.getByIdentifier(ResourceLocation)` resolve o ID do snapshot para obter o nome canônico. Não se presume tradução por cliente. Formas, tipos, labels, conhecimento e shiny visto reutilizam os getters auditados anteriormente e o mesmo snapshot imutável/cache do progresso.

A wiki usa somente espécies implementadas e os critérios reais da coleção, incluindo a forma padrão. Testes com a fixture namespaced confirmaram nome, IDs, tipos e estados. A primeira assertion de integração presumiu que a categoria lendária/normal conteria só a fixture; o registro real também contém Regigigas. O teste passou a restringir a consulta ao namespace da fixture, preservando a classificação dinâmica do mod.

`VERIFIED` neste escopo: pesquisa por nome/ID, paginação e ordenação, filtros visto/obtido/não visto/shiny visto, detalhes, atualização por eventos oficiais, isolamento de ícones, permissões e revalidação após reload. 78 JUnit, 8 checks de wiki, 9 de coleções e 12 de fundação sem LuckPerms em servidor dev. [Evidência](validation/phase2-wiki.json).

Spawn continua `PENDING_TEST`: não foi integrada uma consulta de biomas/horários/dimensões. A GUI informa indisponibilidade; não afirma ausência de spawn natural. Captura shiny permanece indisponível, sem inferência a partir de shiny visto/obtido. Cliente visual, carga e instalação do novo JAR fora do Loom não foram validados nesta entrega. [Comportamento e limites](WIKI.md).

## Atualização: contextos por dimensão e protocolo de inventário

Complemento sobre `41e0b7b`, sem alteração nas APIs utilizadas pelo mod: build atual com 69 JUnit aprovado; 8 checks em servidor Loom/Fabric/Cobblemon real com LuckPerms Fabric 5.4.140, repetidos com um perfil novo. [Evidência](validation/phase2-contexts.json).

`User.getCachedData().getPermissionData().checkPermission(...)` considerou o contexto ativo do jogador: grant `world=overworld` autorizou coleção e reload, mudança ao Nether negou/ocultou e o retorno restaurou acesso. A saída de `lp user <uuid> info` confirmou `world=overworld` (não `minecraft:overworld`). Revogação contextual com botão aberto foi rejeitada no clique. Isso verifica esse cenário; não todas as combinações de herança/contextos.

`MainMenu.clicked`, `quickMoveStack` e `canTakeItemForPickAll` foram exercitados por pacotes vanilla reais: descarte unitário/pilha, double-click com cursor forjado, fases de drag esquerdo/direito, clone criativo e clique externo. Nenhum ícone foi transferido/largado; contagens server-side de itens próprios e progresso oficial foram preservados. Predição local do cliente não foi usada como evidência de estado final. Cliente visual e carga permanecem pendentes.

No desenvolvimento cloud, `scripts/server_proxy.init.gradle` repassa propriedades JVM de proxy/truststore para `JavaExec`. A primeira tentativa sem isso falhou no download dinâmico de dependências LuckPerms; com o init script opt-in, as verificações do provedor e o startup concluíram sem desabilitar TLS/checksums. O erro não era uma API inexistente nem um defeito funcional do Dex. As seções antigas abaixo preservam a evolução da auditoria e devem ser lidas com essas atualizações.

## Atualização: coleções JSON e LuckPerms real

Verificados em servidor Fabric 1.21.1: FormData.getTypes() / ElementalType.getName(), labels e formas para filtros; ícones conferidos em BuiltInRegistries.ITEM antes da publicação da candidata. O validador recebe somente dados imutáveis capturados na thread do servidor; não acessa Minecraft no executor de configuração. Modos OWNED/SEEN/SHINY_SEEN foram executados; SHINY_CAPTURED não ganhou fonte histórica e permanece N/D.

LuckPerms Fabric **5.4.140**, distribuição CDN Modrinth fixada no manifesto de testes do Shirokai Crates, SHA-512 verificado (relatório). JAR declara dependência fabric-permissions-api-v0 >=0.3.1 e inclui a versão 0.3.1 aninhada. API checkPermission já compilada foi agora exercitada com grants/revokes globais reais, reload autorizado/negado e GUI antiga após revogação. Contextos específicos de dimensão continuam PENDING_TEST. Nenhum segredo foi necessário para obter o artefato. [Evidência](validation/phase2-collections.json).

## Atualização: porcentagens e formas — verificação funcional inicial

Fonte: mesma tag Cobblemon 1.8.3 fixada nesta auditoria. Consulta de `Species.getStandardForm()`, `getForms()`, `FormData.getLabels()`, `SpeciesDexRecord.getFormRecord(name)` e `FormDexRecord.getKnowledge()` compilada e executada em servidor real. **Species.forms pode estar vazio quando não há formas alternativas**; incluir a forma padrão explicitamente é necessário. Labels da forma padrão usam as da espécie quando não há override, como em `Pokemon.isLegendary()`.

`CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Consumer<Post>)` e `Post.getPlayerUUID()` usados para invalidar snapshot na thread correta. Testes de obtenção real via comando oficial givepokemon observaram nova contagem antes da expiração de 5 segundos do cache, sem chamar métodos de concessão de Pokédex. Esses Pokémon concedidos não foram classificados como capturas legítimas nem evidência shiny capturada.

Fixture própria com `dex_test:ordinary` e `dex_test:legend`, incluindo espécie lendária sem formas alternativas, foi carregada. Nacional: 907 espécies implementadas com fixture, 905 após removê-la e reiniciar; Lendária: 18/17. Apenas classificação/progresso atuais foram derivados, sem inventar histórico. O JAR fora do Loom passou nos testes. [Relatório](validation/phase2-percentages.json). Isso não certifica datapacks adicionais reais.

Shiny capturado continua **UNSUPPORTED pelo registro oficial isolado** e a UI mostra N/D, não 0% nem a inferência inválida de shiny visto + normal obtido.

## Atualização: fundação experimental validada

`VERIFIED` no escopo básico: build Java 21, 20 casos JUnit sem falhas e servidor Fabric 0.17.2 com Cobblemon 1.8.3, tanto em desenvolvimento quanto com JAR remapeado instalado isoladamente fora do Loom. Evidência: [relatório](validation/phase1-foundation.json).

`Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(ServerPlayer)`, `getSpeciesRecord`, `hasAtLeast(SEEN/OWNED)` e `PokemonSpecies.INSTANCE.getImplemented()` foram compilados e executados no servidor: 1025 espécies carregadas pelo Cobblemon, 905 implementadas no catálogo usado pelo resumo. Não há lista fixa no código. Datapacks adicionais não foram testados.

Menu básico usa o protocolo vanilla, sem SGUI: assinaturas de `ChestMenu`, `AbstractContainerMenu`, `SimpleMenuProvider`, componentes `CUSTOM_NAME` e `LORE` verificadas no Minecraft mapeado. Smoke real confirmou abertura e bloqueios de pickup, clique direito, Shift/hotbar, fechamento e ausência de alteração do progresso oficial/entrega de ícones. Aparência em cliente Minecraft real e demais gestos continuam pendentes. SGUI não é dependência da fundação.

API LuckPerms 5.4 resolveu e compilou; ausência do mod e negação de reload foram testadas no servidor. Permissão positiva/contextos ainda são `PENDING_TEST`. Impactor, Placeholders, SkiesGUIs, TAB e Crates não são integrações funcionais desta alpha.

Cuidados reais do ambiente de desenvolvimento: Loom 1.11.8 só disponibiliza remapeamento de metadata Kotlin quando o plugin Kotlin está aplicado (`KotlinClasspathService.createOptions`, fonte oficial do Loom). Plugin Kotlin 2.2.20 corrigiu o `ClassNotFoundException: net.minecraft.class_2960` por reflexão do Cobblemon. `minecraftServerLibraries` com ICU 71.1, como no build oficial Cobblemon, corrigiu o runtime Showdown. Nenhum JAR oficial foi alterado.

O próprio Cobblemon informa no startup que seus registros de espécies são carregados uma vez por instância e não são seguros para reload. Não chamar `PokemonSpecies.reload` para forçar hot reload. Alterações de datapacks/espécies devem ser validadas entre reinícios; reload de JSON do Dex é um fluxo distinto.

## Fontes fixadas

| Dependência | Revisão consultada | Evidência |
|---|---|---|
| Cobblemon 1.8.3 | `ffe005511dd0d3ba68377fa3f95906b3837d9a0e` | [tag oficial](https://gitlab.com/cable-mc/cobblemon/-/tree/1.8.3), `gradle.properties`, `gradle/libs.versions.toml` |
| Impactor 5.3.5 | `da9a2fd55cf6f0acb6e55fe157745f3bdc17ab84` | [tag oficial](https://github.com/NickImpact/Impactor/tree/5.3.5), `gradle.properties` |
| Impactor API, submódulo da versão 5.3.5 | `dbcce5b992bfb6f94e82f2f3b60e81f61ae4cc2e` | [fonte da API](https://github.com/NickImpact/ImpactorAPI/tree/dbcce5b992bfb6f94e82f2f3b60e81f61ae4cc2e) |
| Text Placeholder API 2.4.2+1.21 | `2427627df3e73ec00a2d3623c7e60e1d076bdb67` | [tag](https://github.com/Patbox/TextPlaceholderAPI/tree/2.4.2%2B1.21), `fabric.mod.json` |
| LuckPerms API 5.4 | `99c3d68d3a618677800fd674363855ae143f4452` | [tag](https://github.com/LuckPerms/LuckPerms/tree/v5.4); não fixa a versão do mod instalado |
| SGUI 1.6.1+1.21.1 | `5d1662183504f2f8cf0359d9c6f3af037699edba` | [tag](https://github.com/Patbox/sgui/tree/1.6.1%2B1.21.1), `gradle.properties` |

As fontes foram obtidas com Git via HTTPS. Não foi copiado código de DexRewards. As versões opcionais acima são candidatas à integração, não prova de compatibilidade binária do conjunto.

## Plataforma e artefato Cobblemon

`VERIFIED` (metadados): Cobblemon 1.8.3 declara Minecraft 1.21.1 e Java 21; catálogo Fabric Loader 0.17.2, Fabric API 0.116.6+1.21.1, Fabric Language Kotlin 1.13.6+kotlin.2.2.20.

Coordenada real: `com.cobblemon:fabric:1.8.3+1.21.1`, repositório `https://artefacts.cobblemon.com/releases/`. O [POM](https://artefacts.cobblemon.com/releases/com/cobblemon/fabric/1.8.3+1.21.1/fabric-1.8.3+1.21.1.pom) retornou HTTP 200 e contém o mesmo `gitCommit` da tag auditada. Não substituir por latest: o catálogo já anuncia versões posteriores.

## Capturas e origem — PENDING_TEST

Pacote `com.cobblemon.mod.common`:

- `api.events.CobblemonEvents.POKEMON_CAPTURED`: `EventObservable<PokemonCapturedEvent>`.
- `api.events.pokemon.PokemonCapturedEvent`: propriedades `pokemon`, `player`, `pokeBallEntity`; não possui um ID exclusivo de evento nem enum de origem.
- Única emissão encontrada na árvore auditada: `entity/pokeball/EmptyPokeBallEntity.kt`, `shakeBall`, aproximadamente linhas 343–372. Exige captura bem-sucedida, Pokémon selvagem vivo e future ainda não concluído; adiciona ao armazenamento e então publica o evento.
- O evento acontece depois da atribuição ao jogador: **não exigir `pokemon.isWild()` no listener**, pois a captura já ocorreu.
- Troca, criação, transferência e concessão direta não devem ser inferidas a partir de eventos genéricos de armazenamento. Não há emissão deste evento nesses caminhos na pesquisa da versão auditada.

Limite: outro mod pode publicar o evento ou alterar o fluxo. A observação de um evento não é uma autenticação contra mods hostis. Eventos especiais precisam de adaptador explícito. Pokémon criados administrativamente e depois capturados de fato não têm necessariamente uma origem administrativa reconhecível neste contrato.

Estratégia candidata: snapshot imutável na thread do servidor; persistir chave de deduplicação composta pelo UUID do Pokémon e UUID da entidade de Poké Ball, com unicidade em SQLite. Validar recaptura do mesmo Pokémon, repetição do mesmo callback, reinício e captura simultânea antes de adotar. Não usar somente espécie/horário nem UUID do Pokémon isolado, pois pode haver recaptura legítima após soltura.

## Pokédex oficial — consulta básica VERIFIED; variantes PENDING_TEST

- `api.storage.player.PlayerInstancedDataStoreManager.getPokedexData(ServerPlayer)` e sobrecarga UUID retornam `api.pokedex.PokedexManager`.
- `AbstractPokedexManager.getSpeciesRecord(ResourceLocation)` consulta sem criar registro; `getHighestKnowledgeForSpecies` e `getKnowledgeForSpecies` também estão presentes.
- `SpeciesDexRecord.getKnowledge()` / `hasAtLeast(...)`, registros por forma; enum `PokedexEntryProgress`: usar os valores atuais, não presumir o antigo `CAUGHT`.
- `FormDexRecord` guarda `knowledge` e estados **vistos** shiny/normal; `hasSeenShinyState(boolean)` confirma observação, não captura daquela variante.
- `CobblemonEvents.POKEDEX_DATA_CHANGED_POST` pode invalidar cache; `PRE` é cancelável. A aplicação deve somente consultar, sem chamar `obtain`, `encounter`, `getOrCreateSpeciesRecord` ou comandos de concessão para fabricar progresso.

**UNSUPPORTED pelo registro isolado:** reconstrução de “shiny capturado” pelo cruzamento `OWNED && shiny visto`. Ver um shiny e obter um normal produz essa combinação sem comprovar captura shiny. Alternativa correta: evidência própria de capturas shiny a partir da instalação, mantendo o progresso oficial separado; não reconstruir histórico inexistente. A Pokédex oficial também registra obtenção, que não equivale a captura selvagem competitiva.

## Registro dinâmico e categorias — enumeração básica VERIFIED; datapacks/categorias PENDING_TEST

- `api.pokemon.PokemonSpecies.species`, `.implemented`, `getByIdentifier(ResourceLocation)` e `reload(Map<ResourceLocation, Species>)` existem. Dados são carregados do caminho de recursos `species`.
- `pokemon.Species` fornece identificador, formas, tipos e labels; `Pokemon.isLegendary()`, `isMythical()`, `isUltraBeast()` usam labels da forma.
- Resolver IDs com namespace; não converter todos para `cobblemon:`. Enumerar após carregamento/reload; não manter catálogo fixo. Política de inclusão de espécies não implementadas precisa ser explícita.
- Datapacks podem definir ou omitir labels: permitir inclusão/exclusão JSON e informar classificação indisponível. Não presumir Paradox ou uma lista universal de lendários.
- Registro real do ecossistema Complete Cobblemon Collection ainda não instalado/testado. Testar adição/remoção e preservação de desbloqueios.

## Criação e entrega de Pokémon — PENDING_TEST

- `api.pokemon.PokemonProperties.parse(String, delimiter, assigner)`; propriedades reais incluem `species`, `form`, `level`, `shiny`, `ivs`, `evs`, `nature`, `ability`, `gender`, `pokeball`, `moves`, `heldItem`.
- `create(ServerPlayer?)` cria, aplica propriedades, inicializa e executa `roll`. Campo shiny omitido usa a regra aleatória configurada do Cobblemon.
- A existência do parser **não valida integralmente configurações**: conferir valores, formas, habilidades e movimentos nos registros antes de entregar; alguns resolvedores usam fallback, por exemplo `Species.getFormByName` retorna forma padrão se não encontrar.
- `api.storage.party.PlayerPartyStore.add(Pokemon): Boolean` tenta equipe e overflow no PC; tratar resultado falso. Não assumir espaço ilimitado nem sucesso só por chamar o método.
- Inventário, armazenamento Pokémon e banco não participam de uma transação atômica comum. Não repetir automaticamente entrega cujo resultado ficou incerto.

## Economia Impactor — PENDING_TEST

Impactor 5.3.5 declara Minecraft 1.21.1, Fabric Loader 0.16.9 e Fabric API 0.107.0+1.21.1. A versão do plugin e a versão Maven de cada módulo de API precisam ser distinguidas.

No submódulo auditado, pacote `net.impactdev.impactor.api.economy`:

- `EconomyService.instance()`; `currencies()`; `account(Currency, UUID): CompletableFuture<Account>`.
- `currency.CurrencyProvider.currency(net.kyori.adventure.key.Key): Optional<Currency>`.
- `accounts.Account.balance(): BigDecimal`, `deposit(BigDecimal): EconomyTransaction`, alternativas assíncronas documentadas na interface.

Resolver `impactor:dollars` e `impactor:tokens` em runtime; nomes configuráveis não comprovam que essas moedas existem. Se ausentes, rejeitar recompensa de forma explícita, sem usar moeda principal como fallback. Verificar resultado da transação e persistência da implementação. Não foi demonstrada idempotência externa por ID de operação fornecido pelo chamador; falhas incertas exigem `NEEDS_REVIEW`.

## Permissões — cenário global e dimensão VERIFIED; demais contextos PENDING_TEST

LuckPerms API 5.4: `LuckPermsProvider.get()`, `LuckPerms.getUserManager()`, `UserManager.getUser(UUID)`, `User.getCachedData().getPermissionData().checkPermission(String)`. Usuário pode ser nulo; considerar contextos atuais e negar operação sensível se indisponível. Não substituir LuckPerms ausente por autorização administrativa aberta. Distribuição Fabric 5.4.140 verificada nos relatórios de coleções e contextos; versão instalada em produção não foi certificada.

## Placeholders — básicos VERIFIED; TAB/alias legado PENDING_TEST

Text Placeholder API 2.4.2+1.21 declara Minecraft `>=1.21-` (não é prova de funcionamento em 1.21.1).

`eu.pb4.placeholders.api.Placeholders.register(Identifier, PlaceholderHandler)` e `getPlaceholders()` existem. **register sobrescreve a chave**: verificar conflito antes de registrar qualquer alias, especialmente `dexrewards:player_dex_percent`.

A documentação do método descreve `%namespace:placeholder argument%`; assim a candidata é `%shirokaidex:percent national%`, registrando `shirokaidex:percent`, não presumindo que `/national` seja argumento automaticamente. Parser básico agora verificado na alpha.4 acima; TAB continua separado e pendente. Placeholder não deve causar consulta pesada ao banco, carregamento bloqueante ou execução de comando.

## Menus e integrações externas

SGUI 1.6.1+1.21.1 (`PENDING_TEST`): `eu.pb4.sgui.api.gui.SimpleGui(ScreenHandlerType<?>, ServerPlayerEntity, boolean)`, `setTitle(Text)`, `setSlot(...)`, `open(): boolean`. Fonte usa Yarn, enquanto Cobblemon usa nomes Mojang: remapeamento pelo Loom deve ser validado. GUI de inventário evita renderização personalizada; comportamento real de cliques/arraste e proteção ainda precisa ser testado com cliente.

SkiesGUIs (`PENDING_TEST`): versão/artefato não fornecidos. Integração prevista por comandos públicos; nenhum uso de API interna. O comando `/menu` pode não existir: informar falha em vez de presumir sucesso.

TAB (`PENDING_TEST`): distribuição, versão e bridge Fabric não fornecidas. Suporte da Placeholder API não comprova automaticamente suporte no TAB.

Shirokai Crates 0.2.1 (`PENDING_TEST`): fonte indicada pelo usuário em [istwi/Shirokai-Crates](https://github.com/istwi/Shirokai-Crates), HEAD auditado `05b3995c4d4466ef17c6171ad07d4f0eab512db9`; `build.gradle` declara 0.2.1. Isso não confirma a versão instalada no servidor.

Em `dev.shirokaicrates.ShirokaiCrates`, o registro real é `/crates givekey <player> <id> <count>`, count entre 1 e 2304; `permitted` exige entidade `ServerPlayerEntity` e permissão `shirokaicrates.give` ou `shirokaicrates.admin`. O helper `run` também trabalha com jogador. **UNSUPPORTED neste fluxo:** executar a entrega como console. Não conceder permissão administrativa ao destinatário como alternativa.

`service.CrateService.give(ServerPlayerEntity admin, ServerPlayerEntity target, String id, int count, boolean fragment)` confere permissão, saúde, crate existente, espaço e estado busy; cria token via `tokens.create`, insere e solicita checkpoint. Retorna `void`, sem recibo idempotente para o Dex. Não há contrato de integração pública verificado para recompensas automáticas. Alternativa futura: contrato público com ID de operação e confirmação persistente no Crates, ou adaptador de item salvo somente após validar componentes e semântica dos tokens. `CRATE_KEY` permanece desabilitado até testes e definição do contrato; nenhuma alteração foi feita no Crates.

Wiki e resumos de spawn do pool mundial (`VERIFIED` no escopo da alpha.6): `CobblemonSpawnPools.INSTANCE.getWORLD_SPAWN_POOL().getDetails()`, `PokemonSpawnDetail.getPokemon()`, `SpawnDetail.getConditions()/getAnticonditions()/getCompositeCondition()`. Snapshot somente na thread do servidor, sem avaliar ou alterar regras. Outros tipos/destinos não resolvidos são contados como ignorados.

`PokemonProperties.getSpecies()/getForm()` preservam a declaração. Resolver espécie por `PokemonSpecies.getByIdentifier(ResourceLocation)`; `getByName` prefixa o namespace Cobblemon e falhou na primeira tentativa com ID já namespaced. Correção coberta por teste unitário e fixture real `dex_test:ordinary`.

`SpawningCondition.getBiomes()/getDimensions()/getTimeRange()`: seletores conhecidos `RegistryLikeIdentifierCondition.getIdentifier()` e `RegistryLikeTagCondition.getTag().location()`, preservando `#`; `TimeRange.getRanges()` fornece `IntRange`. Bytecode e servidor confirmaram **day = 23460..23999 e 0..12541**, **night = 12542..23459**, não faixas inventadas. Condições positivas são alternativas; anticondições rejeitam; composição adicional não é simplificada.

[Validação](validation/phase2-spawns.json): regras nativas e namespaced, exclusões, conjunto desabilitado, paginação, permissões/reload e remoção entre reinícios. Não testa o nascimento real de Pokémon. Formas por aspectos, regras globais, custom selectors, herd/NPC/outros spawners, pesos/clima/luz/altura permanecem fora do resumo. Não é consulta universal nem prova de ausência de spawn natural. [Escopo da interface](WIKI.md#aparecimento-e-limites).

## Acesso e testes executados

- Histórico do bootstrap: propriedades JVM de proxy permitiram baixar a distribuição Gradle. O checksum inicialmente salvo tinha 66 caracteres e foi corrigido após consulta ao SHA-256 oficial de 64 caracteres (`https://services.gradle.org/distributions/gradle-8.14.3-bin.zip.sha256`). A execução corrigida já passou nas entregas posteriores, inclusive no build de 69 testes desta retomada; não permanece bloqueada. TLS/checksum continuam ativos.

- Java 21 executado; checkout vazio e ausência de instruções AGENTS confirmados.
- Tags Cobblemon, Impactor e submódulo, LuckPerms, Placeholder API e SGUI obtidas e inspecionadas.
- POM Cobblemon e catálogo Maven oficial consultados com TLS ativo; commit do POM corresponde à tag.
- `api.modrinth.com` e `maven.cobblemon.com` retornaram bloqueio HTTP 403 no proxy. O Maven oficial alternativo acima respondeu; não desabilitar TLS nem ampliar rede automaticamente.
- Na auditoria inicial, compilação e testes eram pendentes. O escopo básico foi posteriormente validado conforme a seção de atualização e o relatório; demais integrações continuam pendentes. Inspeção de fonte não é teste funcional.

## Critérios para avançar

Fundação pode começar com as APIs identificadas, mantendo integrações opcionais isoladas. Antes de concluir a Fase 1: resolver artefatos fixados, compilar, executar testes reais de configuração/migração e inicializar servidor Fabric com Cobblemon 1.8.3. Antes da Fase 2: resolver a limitação shiny sem falsificar dados; validar datapacks e eventos no servidor. Recompensas, competições e chaves permanecem fora da fundação.

## Categorias nomeadas — alpha.7

Nenhuma API externa nova. Catálogo puro reutiliza Criteria e snapshots oficiais já auditados; validação semântica usa CollectionValidator. Wiki/progresso compartilham escopo de forma e reload valida o candidato integral. Não grava Pokédex, captura, histórico ou competição. [Schema](CATEGORIES.md), [evidência](validation/phase2-categories.json).

## Versões de categorias — trabalho local

Sem API de mod nova: SHA-256 JDK, serialização Gson já existente e SQLite JDBC já fixado. Migração 2 e persistência no worker existente; lifecycle aguarda banco antes de validar/persistir configuração. Não bloqueia a thread do servidor aguardando SQLite. [Evidência](validation/phase2-category-versions.json).

## Recompensas de itens — trabalho local

API Minecraft 1.21.1 conferida por javap no JAR mapeado fixado: ItemStack.copy/getMaxStackSize/isSameItemSameComponents, ServerPlayer.hasDisconnected, PlayerList.saveAll público (save(ServerPlayer) é protegido). Adaptador usa somente inventário principal, pilhas com mesmos componentes e thread do servidor; saveAll antecede confirmação final, sem promessa de atomicidade com SQLite. Não há integração nova com economia/Crates/Pokémon. [Evidência](validation/phase3-items.json).

### Falha de salvamento vanilla

PlayerDataStorage.save captura/loga Exception sem propagá-la (bytecode da versão fixada inspecionado). O adaptador não trata retorno de saveAll como prova: usa LevelResource.PLAYER_DATA_DIR, NbtIo.readCompressed(Path,NbtAccounter.create), ItemStack.parseOptional e ItemStack.matches para conferir slots/componentes. Falha/arquivo divergente resulta em revisão. Não garante fsync nem atomicidade com SQLite.

## Componentes e itens salvos — local alpha.2

API 1.21.1 conferida no JAR mapeado: ItemStack.save(HolderLookup.Provider), STRICT_SINGLE_ITEM_CODEC, TagParser.parseTag e RegistryOps.create(NbtOps.INSTANCE, provider). Capture/restore e validação de componentes executam na thread do servidor; somente strings/IDs imutáveis seguem ao SQLite. Validação assíncrona de config agenda codec no servidor e cancela esperas no shutdown. Não foram adicionadas dependências nem APIs de mod presumidas. [Testes reais](validation/phase3-saved-items.json).

## POKEMON — implementação local auditada

Auditoria da fonte 1.8.3 confirma que PokemonProperties é permissivo e que FileBackedPokemonStoreFactory serializa no servidor e grava em executor próprio, sem confirmação pública síncrona. Implementação usa Pokemon/registries reais e codec NBT para congelar UUID/valores; rejeita atributos inválidos em vez de aceitar fallback. PlayerPartyStore.add tenta equipe/PC e emite POKEMON_GAINED, não POKEMON_CAPTURED. Capacidade pública size/occupied/getBoxes/unoccupiedSlots; não usa backupStore ilimitado. Leitura dos caminhos nativos NBT foi conferida em OneToOneFileStoreAdapter e stores; nenhum acesso refletivo a factories nem escrita do Dex nesses arquivos. [Prova](validation/phase3-pokemon.json), [limites](POKEMON_REWARDS.md).

## Central/layout — implementação local

Menus vanilla ChestMenu 3..6 linhas, Component/TextColor por caractere para gradiente e lore curta. A central e grade usam o mesmo guard de cliques/owner; sem renderizador cliente próprio. Retorno externo usa Commands.performPrefixedCommand com player.createCommandSourceStack, sem console/elevação. Probe isolado confirmou jogador não OP; removido o probe/com LuckPerms ausente, interface e wiki continuaram funcionais. [Prova](validation/layout-home.json).

## Empacotamento alpha.5

EntryPoint `br.com.shirokaidex.core.ShirokaiDex` e implementação `net.fabricmc.api.ModInitializer` corretos; o erro reportado foi reproduzido com sources.jar alpha.4 e confirmado pelo usuário como arquivo instalado. O fonte declarava um entrypoint sem bytecode. Alpha.5 remove o descritor das fontes e valida o JAR final; nenhuma API/versão de dependência alterada. Evidência em `validation/entrypoint-alpha5.json`.

## Navegação JSON alpha.6 local

Sem nova integração externa: usa os mesmos SimpleContainer, ChestMenu e callbacks protegidos. Só os slots de navegação mudam, com conteúdos/ações reposicionados juntos antes de abrir o menu; confirmação e preflight continuam nas APIs existentes. Servidor empacotado com Cobblemon1.8.3 e teste de reload com LuckPerms5.4.140; primeira passagem de navegação/resgate também executada sem LuckPerms/API. Cliente gráfico/carga pendentes.

## Aparência alpha.7 local

Somente APIs Minecraft já usadas: SimpleContainer, ItemStack, componentes CUSTOM_NAME/LORE e registry snapshot imutável de itens capturado na thread do servidor. O parser não consulta registries fora da thread. Alterações de ícone afetam apenas controles da tela, sem copiar metadados de prêmio ou mudar entrega. Novos tamanhos usam os tipos de ChestMenu já suportados (3–6 linhas). Teste em servidor empacotado detalhado em `validation/menu-appearance.json`. Nenhuma dependência externa nova.

## Impactor 5.3.5 — CURRENCY, alpha.8 local

API Maven `net.impactdev.impactor.api:economy:5.3.5`, repositório oficial development. Dependência somente compileOnly; event-api snapshot não usado excluído da resolução, conforme artefato consumido. Repo permite também `net.impactdev:json:1.0.0`, dependência real da API. As duas primeiras tentativas de resolução falharam por esses detalhes e foram corrigidas; build final completo aprovado.

JAR oficial `Impactor-Fabric-5.3.5+1.21.1.jar`, versão interna 5.3.5+1.21.1, obtido pela URL/hashes do manifesto público auditado do Shirokai Crates; SHA-512 conferido. APIs reais `EconomyService.instance`, currencies.currency(Key), account, Account.owner/currency/virtual/balance/deposit, EconomyTransaction.successful/type/amount/account/currency e EconomyService.save compiladas/exercitadas. Sem IDs de transação externos inventados, fallback de moeda ou comandos administrativos.

Auditoria adicional nas fontes oficiais da tag 5.3.5 (`da9a2fd55cf6f0acb6e55fe157745f3bdc17ab84`): ImpactorAccount.deposit inicia save sem aguardá-lo; ConfigurateProvider.save(Path, Account) captura exceções e usa doubleValue, enquanto account() restaura via BigDecimal.valueOf(node.getDouble()). O future público, isoladamente, não prova gravação. Dex valida roundtrip antes do crédito e verifica saldo nativo por leitura assíncrona depois do save. Erro real de escrita ocultado pelo Impactor foi reproduzido e ficou em NEEDS_REVIEW após reinício.

Checagem reflexiva isolada/somente leitura para identificar serviço/backend reais, pois API pública não expõe essa capacidade: public storage() do serviço, fields implementation/loader/root conferidos nas fontes e por javap no JAR oficial. Classe JsonLoader também confirmada. Suporta só serviço nativo com JSON, versões 5.3.5/5.3.5+metadata; SQL/HOCON/YAML/custom providers não são certificados e são bloqueados antes de efeitos. Não escreve nem altera configurações do Impactor em servidores de usuários.

[Guia](CURRENCY_REWARDS.md), [evidência](validation/phase3-currency.json): ausência opcional, depósitos reais, isolamento de moedas, rejeição de referência/precisão, plano misto, falha e reinício. Não é certificação de transação distribuída, concorrência com todos os plugins ou carga.

## Decisão posterior — emissão dedicada CRATE_KEY0.2.1

[Auditoria do contrato público](CRATE_KEY_CONTRACT.md): `give` retorna void e checkpoint é privado ao fluxo; instância Tokens é privada, apesar de seus métodos serem públicos. UNSUPPORTED para emissão automática dedicada sob as restrições atuais; não altera o suporte de SAVED_ITEM/checkpoint validado. Conclusão estática de fonte/javap, sem teste novo de CRATE_KEY ou mudanças no provedor.

## COMMAND — alpha.10 local, dispatch com revisão obrigatória

Minecraft1.21.1 mapeado conferido por javap: `Commands.performPrefixedCommand(CommandSourceStack,String): void`, `getDispatcher()`, `CommandSourceStack.withSuppressedOutput()`. Parsing Brigadier usa fonte PLAYER original ou console explicitamente autorizada. Pré-condições/consumo completo/command executável conferidos antes de intenção e novamente antes do dispatch. Nenhuma API de recibo universal foi inventada: performPrefixedCommand retornar não comprova efeito persistido; operação vai a NEEDS_REVIEW, sem retry. Plano congela template/contexto e comando renderizado antes da intenção, permanecendo independente de reload.

[Política/schema/limites](COMMAND_REWARDS.md), [evidência](validation/phase3-commands.json). Não usa shell, não eleva permissão do jogador nem muda outros mods. Testes nativos de scoreboard/PLAYER não certificam comandos externos; contextos privilegiados são escolha explícita de operador no arquivo.

## Recuperação sem efeitos — alpha.11

Sem APIs externas novas: journal SQLite verifica transacionalmente identidade/plano/state e ausência total de reward_steps antes de autorizar retomada. Guard ativo por destinatário e os mesmos adaptadores/preconditions preservados. Fonte PLAYER/CONSOLE em COMMAND continua sendo do destinatário/configuração, não do operador que abriu a prévia. [Teste/limites](validation/phase3-recovery.json).

## Auditoria inicial da captura — ainda PENDING_TEST

Binário oficial Cobblemon1.8.3: `PokemonCapturedEvent(Pokemon,ServerPlayer,EmptyPokeBallEntity)`, getters `getPokemon/getPlayer/getPokeBallEntity`, `EmptyPokeBallEntity.getCapturingPokemon`, `PokemonEntity.getSpawnCause` e SpawnCause.getSpawner reais. Referências ao setter setSpawnCause no JAR ocorrem em PokemonSpawnAction e na própria entidade; bytecode de PokemonSpawnAction obtém causa de SpawnablePosition e atribui à entidade criada. EmptyPokeBallEntity emite POKEMON_CAPTURED na conclusão do fluxo nativo. Isso não comprova origem/persistência/ordenação em servidor: confirmar metadados no instante do evento e após unload/reinício antes de contabilizar capturas legítimas; não criar estatística a partir de eventos artificiais ou simples obtenção.

## Captura/estatísticas — 0.4.0-alpha.1 VERIFIED no escopo descrito

THROWN_POKEBALL_HIT com ThrownPokeballHitEvent.getPokeBall/getPokemon, POKEMON_CAPTURED com PokemonCapturedEvent e EmptyPokeBallEntity.getCapturingPokemon cruzados na thread do servidor. Pokemon.isWild no pré-hit; causa/spawner PlayerSpawner exato e getSpawnPool==CobblemonSpawnPools.WORLD_SPAWN_POOL; IDs/dono/alvo conferidos no pós-evento. getShiny, forma/labels/tipos reais e regras correntes congelados antes de enviar somente dados Java imutáveis ao worker SQLite. Dimensão/posição são da Pokébola, não inferidas da posição posterior do jogador.

[Validação nativa](validation/phase4-captures.json): spawner real + projétil/animação real, replay, origem administrativa desconhecida e reload. Fonte SpawnCause é perdida no NBT/reinício observado; não promover a captura recarregada a legítima. Batalha/pesca/extensões não certificados. IDs compostos de bola/Pokémon são chave derivada, não evento oficial inventado. Primeira falha do teste foi cenário de colisão; a sonda passou a liberar pequena área no mundo descartável. Parser da consulta por espécie inicialmente usava word (rejeitava namespace:ID); corrigido para greedyString com validação de identificador, comprovado no servidor.

## Proveniência persistente — 0.4.0-alpha.2

Fabric ServerEntityEvents.ENTITY_LOAD observa a entidade realmente carregada; não usa SpawnEvent cancelável como confirmação de spawn. PokemonEntity.getSpawnCause/getUUID e Pokemon.getUuid/isWild são lidos na thread do servidor; somente snapshots imutáveis seguem ao SQLite. PlayerSpawner exato e WORLD_SPAWN_POOL permanecem exigidos para criar prova. Causa ausente após reload aceita prova persistida apenas para o mesmo par de identidades e correlação real pré-hit/captura. Nenhum NBT/código de outro mod é alterado. Cobblemon1.8.3/Fabric0.17.2/MC1.21.1; [teste](validation/phase4-provenance.json). Outros modos continuam não certificados.

## Batalha e auditoria de pesca — alpha.3

BattleBuilder.INSTANCE.pve(ServerPlayer,PokemonEntity) retorna BattleStartResult; SuccessfulBattleStart confirma criação real. Probe exige BattleRegistry.getBattleByParticipatingPlayer antes de lançar a bola na mesma entidade. EmptyPokeBallEntity verifica batalha/participante/turno, usa forceChoose(ForcePassActionResponse) e emite THROWN_POKEBALL_HIT antes de concluir POKEMON_CAPTURED; captura PvE positiva validada no servidor, sem emitir evento artificial inicial. Reutiliza prova PlayerSpawner existente. [Evidência](validation/phase4-battle.json).

Pesca: FishingSpawnerFactory.createSharedSpawner cria BasicSpawner com nome fishing, pool configurável (default WORLD_SPAWN_POOL) e fishingBuckets. FishingSpawnCause.affectSpawn delega à causa base e adiciona aspecto fished. Portanto, classe exata PlayerSpawner não abrange essa origem. Nome/aspecto isolados não comprovam origem legítima; API encontrada não autoriza ampliar whitelist sem teste de vara/anzol nativo. FishingSpawnCause/FishingSpawnerFactory inspecionados com javap no binário oficial1.8.3, SHA2564bc3b09d25b93b74a5822dc35b06bd6f5fa8bae36ed43a8d2b3904156a61269c.

## Pesca — alpha.4

FishingSpawnCause exata + BestSpawner.INSTANCE.getFishingSpawner por identidade + WORLD_SPAWN_POOL, SpawnCause.getEntity ServerPlayer no servidor e getRodStack PokerodItem observados. Teste usa PokerodItem.use e bobber.getPlannedSpawnAction apenas para saber quando recolher normalmente; não usa setter nem ação fabricada. NativeSpawnProof.Kind conserva FISHING/WORLD_PLAYER no SQLite. [Guia/teste](FISHING.md). Demais varas/iscas/pools não certificados.

## Coleção Shiny — alpha.5

Sem nova API externa: projeção SQLite de capturas verificadas e IDs/formas imutáveis, registries atuais via CobblemonAccess na thread do servidor. Cache inicial por round robin de um jogador por tick; publicação após commit/consulta e proteção contra sessão desconectada. Filtros/cálculo/ACL e consumidores existentes preservados. [Guia](SHINY_CAPTURES.md).
