# Auditoria de compatibilidade — Fase 0

Data: 2026-11-07. Esta é uma auditoria estática inicial; não uma certificação de funcionamento em servidor. O repositório estava vazio, sem HEAD, fontes, build ou testes. Java local confirmado: OpenJDK 21.0.13.

## Estados e evidências

- `VERIFIED`: apenas o fato explicitamente acompanhado de evidência foi confirmado.
- `PENDING_TEST`: API localizada, mas integração, compilação ou comportamento em servidor ainda não testados.
- `UNSUPPORTED`: contrato solicitado não demonstrado pela API examinada; não implica impossibilidade de todas as alternativas.

A auditoria estática inicial não executou testes funcionais. As validações posteriores da fundação estão separadas abaixo; captura, resgate e economia continuam sem testes funcionais.

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

Wiki inicial (`VERIFIED` no escopo da alpha.3 acima); spawn (`PENDING_TEST`): não foi validada uma consulta universal de biomas/horários, sobretudo para spawners de outros mods/datapacks. Mostrar “Informação de aparecimento indisponível.”; ausência de informação não prova ausência de spawn natural.

## Acesso e testes executados

- Histórico do bootstrap: propriedades JVM de proxy permitiram baixar a distribuição Gradle. O checksum inicialmente salvo tinha 66 caracteres e foi corrigido após consulta ao SHA-256 oficial de 64 caracteres (`https://services.gradle.org/distributions/gradle-8.14.3-bin.zip.sha256`). A execução corrigida já passou nas entregas posteriores, inclusive no build de 69 testes desta retomada; não permanece bloqueada. TLS/checksum continuam ativos.

- Java 21 executado; checkout vazio e ausência de instruções AGENTS confirmados.
- Tags Cobblemon, Impactor e submódulo, LuckPerms, Placeholder API e SGUI obtidas e inspecionadas.
- POM Cobblemon e catálogo Maven oficial consultados com TLS ativo; commit do POM corresponde à tag.
- `api.modrinth.com` e `maven.cobblemon.com` retornaram bloqueio HTTP 403 no proxy. O Maven oficial alternativo acima respondeu; não desabilitar TLS nem ampliar rede automaticamente.
- Na auditoria inicial, compilação e testes eram pendentes. O escopo básico foi posteriormente validado conforme a seção de atualização e o relatório; demais integrações continuam pendentes. Inspeção de fonte não é teste funcional.

## Critérios para avançar

Fundação pode começar com as APIs identificadas, mantendo integrações opcionais isoladas. Antes de concluir a Fase 1: resolver artefatos fixados, compilar, executar testes reais de configuração/migração e inicializar servidor Fabric com Cobblemon 1.8.3. Antes da Fase 2: resolver a limitação shiny sem falsificar dados; validar datapacks e eventos no servidor. Recompensas, competições e chaves permanecem fora da fundação.
