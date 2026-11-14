# Validação da fundação

## Testes automatizados Java

```sh
./gradlew --no-daemon build
```

Na nuvem, use JDK 21 e `python3 scripts/gradle_cloud.py`, conforme [instalação](INSTALLATION.md). Última execução local: 102 casos, zero falhas/erros/skips. `ConfigManagerTest`: 16 casos de parsing estrito, campos duplicados/tipos, reload inválido, backup e recuperação após reinício. `DatabaseTest`: 4 casos de migração/reabertura, 100 atualizações para um UUID, versão futura, rollback e operações após fechamento.

`DexProgressTest` adiciona 12 casos de cálculo, truncamento, vazio/indisponível, IDs namespaced, denominador dinâmico, formas e duplicação.

Resultados completos locais: `build/test-results/test/TEST-*.xml`. Esses testes não cobrem capturas, recompensas, competições ou desempenho multiplayer.

## Smoke de protocolo em servidor real

Servidor dedicado Fabric 0.17.2, Minecraft 1.21.1 e Cobblemon 1.8.3. Rodar em mundo descartável, apenas loopback, com EULA lida/aceita pelo responsável. Nunca use o modo offline de teste em servidor público.

Exemplo de `run/server.properties` para o teste de desenvolvimento:

```properties
server-ip=127.0.0.1
server-port=25571
online-mode=false
enforce-secure-profile=false
view-distance=2
simulation-distance=3
max-players=4
```

Inicie `./gradlew --no-daemon runServer` em terminal interativo. Aguarde **ambas** as mensagens `Shirokai Dex ready` e `Done (...)!`, sem falha fatal. O Gradle pode terminar com código zero mesmo que Minecraft não tenha iniciado; não usar somente o resultado da tarefa como readiness.

Em cloud, o proxy da JVM do Gradle não é automaticamente herdado pela JVM do Minecraft. Use o init script opt-in para repassar proxy e truststore ao `JavaExec` do Loom:

```sh
JAVA_HOME=/workspace/.tools/jdk-21.0.13+9 python3 scripts/gradle_cloud.py \
  -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
  --init-script scripts/server_proxy.init.gradle --no-daemon runServer
```

Execute na raiz do checkout, com o JDK 21 completo existente na máquina. O init script não modifica configurações globais nem desabilita TLS. LuckPerms baixa dependências adicionais na primeira inicialização; sem proxy no processo filho, esse download pode falhar com `UnknownHostException`, mesmo com o build aprovado.

```sh
cd tests/integration
npm ci --ignore-scripts --no-audit --no-fund
npm test
```

Se o diretório pessoal for somente de leitura, acrescente `--cache /workspace/.cache/npm` aos comandos npm. Para outra porta loopback: `MC_TEST_PORT=25572 npm test`. O smoke conecta `DexFoundation`, verifica consulta oficial, menu, bloqueio de pickup/clique direito/Shift/hotbar, fechamento, ausência de itens indevidos, progresso inalterado e rejeição de reload sem LuckPerms. Total atual: 13 verificações, incluindo navegação, wiki/detalhes e ausência das APIs opcionais. O relatório é sobrescrito por execução em `build/integration-foundation.json`, com timestamp e target.

O cliente de protocolo ignora metadados de entidades, receitas, advancements e árvore de comandos customizados que a biblioteca vanilla não entende. Inventário, comandos executados e respostas testadas são reais, não mocks. Isto não valida renderização em cliente Minecraft/Cobblemon, sugestões de comandos, receitas nem mecânicas de batalha/captura.

Também foi realizado reinício do servidor de desenvolvimento com JSON inválido: backup válido recuperado, original inválido preservado, SQLite íntegro e sem duplicação da migração, UUIDs/primeiro acesso preservados e smoke aprovado novamente. O arquivo válido do teste foi restaurado depois. Encerre os servidores com `stop` e confirme salvamento e fechamento.

O mesmo smoke passou com o JAR final em um servidor isolado fora do Loom, usando o perfil oficial Maven do Fabric Loader e dependências originais de release. Relatório persistido: [validação](validation/phase1-foundation.json). A primeira conexão ao servidor empacotado foi tentada cedo demais e falhou com ECONNREFUSED; a execução aprovada ocorreu após `Done`, sem alterar o mod para esconder a falha.

## Pendências explícitas

- Cliente Minecraft/Cobblemon real: aparência e gestos físicos na interface. Arraste, double-click e descarte têm cobertura complementar por pacotes, descrita abaixo; isso não comprova renderização/experiência visual.
- Contextos LuckPerms adicionais, herança complexa e regras personalizadas além do cenário de dimensão verificado.
- Datapacks adicionais, mudança de espécies entre versões e política de progresso shiny.
- Capturas, recompensas e falhas externas: ainda não implementadas.
- Testes de carga: não realizados; não há alegação de capacidade de jogadores.
- GitHub Actions: confira execução/artifact do commit correspondente. Resultado local não garante CI.

## Testes de porcentagem e datapacks (0.2.0-alpha.1)

Fixture própria: `tests/fixtures/datapacks/dex-progress`, formato 48 para Minecraft 1.21.1. Define uma espécie comum e uma lendária implementadas, com namespace `dex_test`, sem formas alternativas. Não contém modelos nem spawns e serve apenas para testes de dados/protocolo.

Use mundo descartável e perfil DexProgress sem registros prévios. Com servidor parado, copie a pasta da fixture para `run/world/datapacks/dex-progress`. Inicie o servidor, aguarde readiness e conceda OP **somente ao perfil de teste DexProgress**, pelo console vanilla, para que o runner possa executar o comando oficial de obtenção Cobblemon. Isso não substitui permissões administrativas do Dex: seu reload continua exigindo LuckPerms. Não apagar dados de jogadores reais para adaptar um teste.

```sh
cd tests/integration
npm run test:progress
```

O runner exige Nacional/Lendária zeradas no início. Obtém Pokémon pela API real do comando oficial `givepokemon`, sem comandos de concessão direta de Pokédex; confere atualização pelo evento, espécie repetida, forma padrão lendária, Shiny indisponível mesmo após shiny obtido e porcentagem real nos componentes de lore. Executa 6 verificações agrupadas. Para reconferir apenas reconexão/lore sem repetir concessões: `MC_PROGRESS_MODE=resume-with-fixture npm run test:progress` (2 verificações).

Depois pare o servidor, mova a fixture para fora de `world/datapacks` preservando os arquivos e reinicie. Execute `MC_PROGRESS_MODE=after-removal npm run test:progress`: uma verificação agrupada compara com a baseline gravada no primeiro teste, exigindo total Nacional menor em 2, Lendária menor em 1 e exclusão das espécies removidas da contagem atual. Não faz hot reload nem altera registros oficiais para fabricar o resultado.

Relatórios atuais: `build/integration-progress-*.json`; evidência persistida em [phase2-percentages.json](validation/phase2-percentages.json). Mesmo código/JAR foi validado também fora do Loom em servidor isolado, usando `MC_TEST_PORT=25572`. A imagem de um cliente Minecraft real continua pendente; apenas os componentes efetivamente transmitidos e suas respostas foram assertados.

## Coleções JSON e permissões (alpha.2)

31 casos CollectionsConfigTest e 6 ConfiguredProgressTest somam-se aos 32 anteriores: 69 JUnit sem falhas/erros/skips. Integração atual: 11 checks de regressão sem LuckPerms, 9 com configuração de 31 coleções/LuckPerms 5.4.140/JAR instalado e 2 após reinício com JSON inválido. [Evidência](validation/phase2-collections.json).

Fixtures em tests/fixtures/collections e datapacks/dex-progress (agora inclui espécie future não implementada). Runner `npm run test:collections`, no diretório tests/integration, exige servidor descartável loopback, LuckPerms e RCON autenticado com segredo temporário externo ao Git. Defina MC_TEST_RUN, MC_TEST_PORT, MC_TEST_RCON_PORT e MC_TEST_RCON_PASSWORD_FILE; MC_COLLECTIONS_PLAYER escolhe perfil descartável novo (máximo 16 caracteres). Não usar registros reais nem copiar o segredo para relatórios. O runner espaça comandos de um jogador não OP; rajadas/carga não foram certificadas.

Depois dos 9 checks, um JSON inválido foi colocado no arquivo ativo, servidor encerrado e reiniciado; `MC_COLLECTIONS_MODE=after-restart` com o mesmo perfil validou backup semântico, progresso persistido e navegação nos dois sentidos. O runner usa comandos oficiais de obtenção de Pokémon e grants LuckPerms pelo RCON; não concede registros artificiais de Pokédex. Encerrar o servidor e restaurar configurações originais; senha temporária não deve sobreviver ao teste.

Na alpha.2 a wiki ainda era pendente; a alpha.3 adiciona a versão inicial descrita abaixo. Visualização em cliente real, desempenho e captura shiny histórica seguem pendentes. A documentação histórica das alphas anteriores mantém seus próprios relatórios imutáveis; contextos de dimensão foram verificados no complemento abaixo.

## Contextos de dimensão e pacotes de inventário

Runner: `npm run test:contexts`, com a fixture `tests/fixtures/contexts/config.json` em `run/config/shirokaidex/config.json`. Prepare um servidor **descartável**, offline e limitado a loopback, com LuckPerms Fabric 5.4.140 e RCON autenticado também em loopback. A EULA precisa estar aceita pelo responsável. Não use dados reais: o runner troca dimensão/modo de jogo, limpa o inventário do perfil de teste e concede 7 pedras e 2 livros para conferir contagens. O perfil não recebe OP nem permissão administrativa global.

Com o servidor parado, preserve qualquer configuração existente antes de instalar a fixture. Configure RCON com uma senha temporária fora do Git e forneça somente o caminho do arquivo ao runner. Depois inicie pelo comando cloud acima e aguarde `Shirokai Dex ready`, `Done` e readiness RCON.

```sh
cd tests/integration
MC_TEST_RCON_PASSWORD_FILE=/caminho/seguro/temporario \
  MC_CONTEXT_PLAYER=DexContext npm run test:contexts
```

`MC_TEST_RUN` aponta para o diretório do servidor (padrão `run` na raiz do checkout); `MC_TEST_PORT` e `MC_TEST_RCON_PORT` usam 25571 e 25573 por padrão. `MC_CONTEXT_PLAYER` deve identificar um perfil descartável, com até 16 caracteres. A fixture é conferida antes de executar operações. Comandos LuckPerms usam o UUID real do perfil conectado e são espaçados, pois o executor assíncrono pode recusar outro comando como ocupado.

Oito checks agrupados verificam: permissão/reload por `world=overworld`; negação e ocultação no Nether; restauração no retorno/revogação com GUI aberta; descarte individual/pilha; double-click com cursor forjado; fases de arraste esquerdo/direito; clone criativo/clique externo; contagens autoritativas do inventário, ausência de itens largados, reabertura e progresso oficial intacto. Os testes aguardam atualizações completas do servidor, sem confundir predição local do cliente com estado confirmado. Os pacotes são reais; o runner não modifica o mod para ignorar assertions.

Relatório por execução: `build/integration-contexts.json`. [Evidência preservada](validation/phase2-contexts.json). Esta execução complementar usa o servidor de desenvolvimento Loom; não é uma nova certificação do JAR instalado fora do Loom, autenticação online, cliente visual ou carga. Ao terminar, envie `stop`, confira salvamento/fechamento e remova a senha RCON temporária e sua propriedade; restaure a configuração local anterior.

## Wiki inicial (alpha.3)

`WikiTest` acrescenta nove casos de seleção pura: nomes/IDs, desempate/ordem, paginação limitada, filtros de estados, critérios e exclusões, conhecimento por forma elegível, Shiny sem histórico, revalidação de detalhes e consulta vazia/inválida. Total atual: **78 JUnit**, zero falhas/erros/skips.

Runner `npm run test:wiki`, no diretório `tests/integration`, exige servidor descartável loopback com LuckPerms 5.4.140, RCON e as fixtures `tests/fixtures/collections/config.json` e `tests/fixtures/datapacks/dex-progress`. Instale o datapack em `<mundo>/datapacks/dex-progress` com o servidor parado, preservando configurações existentes, e aguarde `Shirokai Dex ready`, `Done` e RCON. O responsável já precisa ter aceitado a EULA. Não utilizar perfis ou dados de produção.

```sh
MC_TEST_RCON_PASSWORD_FILE=/caminho/seguro/temporario MC_WIKI_PLAYER=DexWiki npm run test:wiki
```

O nome descartável deve ter até 16 caracteres e Pokédex inicial vazia. `MC_TEST_PORT`, `MC_TEST_RCON_PORT` e `MC_TEST_RUN` usam 25571, 25573 e `run` por padrão. O runner confere a fixture antes de operar; usa comandos oficiais Cobblemon para obter Pokémon de teste, grants LuckPerms pelo UUID e reload com critérios alterados. Isso não classifica os Pokémon concedidos como capturas legítimas. A configuração original é restaurada no disco ao terminar; na execução aprovada também foi recarregada e conferida no servidor.

Oito checks agrupados verificam paginação/retorno, ordem, pesquisa/detalhes, consulta vazia/limites, critérios e leitura sem mutação, eventos/filtros, permissões com botão antigo e revalidação após reload. Relatório atual em `build/integration-wiki.json`. O smoke de fundação agora tem 12 checks, incluindo wiki e detalhes sem LuckPerms. Regressão complementar: os nove checks existentes de coleções passaram com um perfil novo.

[Evidência da alpha.3](validation/phase2-wiki.json). Todas essas execuções desta entrega usaram o servidor de desenvolvimento Loom, não uma nova instalação do JAR fora dele. UI em cliente Minecraft real, spawn e carga permanecem pendentes. Encerrar com `stop`, preservar salvamento e remover senha RCON temporária, restaurando configuração e mods locais anteriores. As evidências antigas das alphas anteriores não são substituídas por esta execução.

## Placeholders básicos (alpha.4)

`PlaceholderTest` acrescenta nove casos para formatação/indisponibilidade, argumentos, revisão/TTL, fila coalescida, imutabilidade, invalidação/logout, backoff de falha e preservação de registros conflitantes/substituídos. Total: **87 JUnit**, sem falhas/erros/skips.

O runner `tests/integration/placeholders.cjs` exige servidor de desenvolvimento descartável em loopback, RCON temporário fora do Git, LuckPerms 5.4.140, Text Placeholder API 2.4.2+1.21 e as mesmas fixtures de configuração/espécies dos testes de coleções. Artefatos/URLs/hashes em `tests/fixtures/placeholders/runtime.json`; API obtida da release oficial, sem desativar TLS. O responsável precisa ter aceitado a EULA. Preserve configuração e mods locais antes de instalar as fixtures.

```sh
cd tests/integration
MC_TEST_RCON_PASSWORD_FILE=/caminho/seguro/temporario MC_PLACEHOLDER_PLAYER=DexPapi npm run test:placeholders
```

`MC_TEST_RUN`, `MC_TEST_PORT` e `MC_TEST_RCON_PORT` têm os padrões `run`, 25571 e 25573. Use perfil novo, sem obtenções prévias, com até 15 caracteres; o teste também conecta `<nome>B` para verificar isolamento entre jogadores. Oito checks agrupados: cinco métricas pelo parser real via prévia; shiny/coleção vazia; leitura sem mutação e métrica inválida; evento oficial antes do fallback de cinco segundos; grant/revoke; reload de revisão/idioma; restauração; logout/outro jogador. O runner usa comandos oficiais de obtenção para dados descartáveis, sem classificar essas concessões como capturas legítimas. Restaura a configuração original e, na execução aprovada, também a recarrega no servidor.

Para contextos e conflitos reais, pare o servidor e construa o **probe exclusivo de desenvolvimento**, separado do JAR do mod:

```sh
# Na raiz, com JDK 21 e truststore/proxy usuais:
JAVA_HOME=/workspace/.tools/jdk-21.0.13+9 python3 scripts/gradle_cloud.py \
  -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts --no-daemon placeholderProbeJar
cp build/test-mods/*-placeholder-probe.jar run/mods/dex-placeholder-probe.jar
```

Reinicie o servidor Loom com as APIs e a configuração/fixture originais; aguarde readiness. O probe registra um handler externo para `shirokaidex:remaining` e um alias legado externo antes do startup. Expõe somente um comando de teste com nível administrativo 4, usado via RCON pelo runner:

```sh
cd tests/integration
MC_TEST_RCON_PASSWORD_FILE=/caminho/seguro/temporario \
  MC_PLACEHOLDER_MODE=probe MC_PLACEHOLDER_PLAYER=DexProbe npm run test:placeholders
```

Cinco checks: parser/argumentos; provedores de namespace/alias preservados; contexto de servidor sem jogador e expansão assíncrona; permissões diretamente pelo parser; substituição posterior de handler. Depois envie `stop` e **confira `DEX_PROBE cleanup: PASS` no log desta execução**, sem `FAIL`: verifica que o Dex removeu somente seus registros e preservou os handlers externos/substituídos. O probe usa espera limitada de cinco segundos apenas no teste assíncrono para detectar bloqueio; o mod de produção não bloqueia aguardando trabalho do servidor.

Remova o probe e ambas as dependências opcionais, reinicie e execute `npm test`: 13 checks da fundação passaram, incluindo consulta/wiki e prévia explicitamente indisponível, sem falha de class loading. Ao terminar, `stop`, verificar salvamento/SQLite e restaurar os mods/configuração anteriores; remover a credencial temporária RCON. O probe nunca deve ser instalado em produção ou incluído nos assets de distribuição.

Relatórios: `build/integration-placeholders.json`, `build/integration-placeholders-probe.json`, `build/integration-foundation.json`; [evidência persistida](validation/phase2-placeholders.json). Execuções em servidor de desenvolvimento real, não nova certificação fora do Loom. TAB, alias legado positivo, cliente visual e carga continuam pendentes.

## Tipos, labels e todas as formas da wiki (alpha.5)

`WikiFacetsTest` acrescenta oito testes: cruzamento na mesma forma, estados das formas selecionadas, restrição dos critérios JSON, opções estáveis/imutáveis, paginação de 100 formas sem perdas, opções com várias páginas, preservação do estado de navegação e detalhe invalidado após mudança de dados. Total: **95 JUnit**, zero falhas/erros/skips.

Fixture própria em `tests/fixtures/wiki-facets`: configuração com coleção de variantes/restrita e datapack `datapacks/wiki-facets` contendo `dex_test:variants`. Tem 40 formas de água com label mythical e uma forma padrão normal/legendary. A obtenção da padrão não comprova obtenção das variantes. O schema foi conferido nos dados reais de Unown do artefato fixado; não utiliza modelos/spawns nem define histórico de capturas.

Com servidor **parado e descartável**, preserve a configuração existente e instale o config.json dessa fixture em `run/config/shirokaidex/config.json` e o datapack em `<mundo>/datapacks/wiki-facets`. Use mundo separado da fixture antiga de progresso. LuckPerms Fabric 5.4.140 e RCON temporário autenticado em loopback são necessários; o responsável precisa ter aceitado a EULA. Inicie pelo comando cloud documentado e aguarde ambas as mensagens de readiness e o RCON.

```sh
cd tests/integration
MC_TEST_RCON_PASSWORD_FILE=/caminho/seguro/temporario \
  MC_WIKI_FACETS_PLAYER=DexFacets npm run test:wiki-facets
```

`MC_TEST_RUN`, `MC_TEST_PORT` e `MC_TEST_RCON_PORT` têm os padrões `run`, 25571 e 25573. Use perfil novo e descartável, com até 16 caracteres. A configuração é conferida antes de operar. Oito checks agrupados verificam opções dinâmicas/paginadas, interseção de tipo/label, todas as 41 formas, estados e critérios por forma, bloqueio de inventário de quatro linhas (inclusive item real do jogador), revogação com seletor aberto, revogação com página de formas aberta e reload/idioma/ajuste de páginas. A concessão oficial de um Pokémon e de dois papéis é setup do teste; não é classificada como captura legítima.

Após esse teste, o servidor foi encerrado e a fixture antiga de coleções/progresso foi validada em outro mundo com perfil novo: **os oito checks de `npm run test:wiki` passaram**. Não fazer hot reload de espécies para trocar fixtures; reiniciar a instância é obrigatório. A configuração válida original foi restaurada/recarregada pelo teste, e os servidores foram encerrados com mundos salvos. Propriedades locais anteriores restauradas, senha temporária removida e SQLite com integrity_check=ok.

Resultados atuais: `build/integration-wiki-facets.json` e `build/integration-wiki.json`; [evidência](validation/phase2-wiki-facets.json), incluindo fingerprints da fixture conferidos contra os bytes instalados no mundo de teste. As evidências antigas não são substituídas. Não houve teste visual/carga ou instalação desta alpha fora do Loom, nem nova certificação de TAB/alias legado.

## Resumos de spawn — alpha.6

[Relatório](validation/phase2-spawns.json): 102 JUnit (sete em `SpawnCatalogTest`) e 17 checks de protocolo, com LuckPerms e sem Placeholder API opcional. Build via helper cloud/JDK 21, TLS/checksums mantidos.

Em servidor descartável loopback com RCON, use a configuração de `tests/fixtures/collections/config.json`, o datapack de espécies `tests/fixtures/datapacks/dex-progress` e o pack `tests/fixtures/spawning/datapacks/dex-spawn` no diretório datapacks do mundo. Inicie até `Done` e execute:

```sh
MC_TEST_RCON_PASSWORD_FILE=/caminho/temporario npm --prefix tests/integration run test:spawns
```

São sete checks: regras nativas e namespaced, 19 cards sem perda, biomas/tags, dimensões, faixas reais de ticks, exclusões, forma declarada, conjunto desabilitado, leitura sem mutação, permissões e reload de configuração. O runner exige a fixture exata; use `MC_SPAWN_PLAYER` para um perfil descartável novo. Não concede progresso oficial.

Encerre normalmente, retire somente o pack de spawn e reinicie preservando as espécies. Execute `MC_SPAWN_MODE=after-removal` com o mesmo comando: dois checks confirmam remoção de regras antigas e permanência das nativas. Execute também `test:wiki` (oito regressões), com `MC_WIKI_PLAYER` novo. Encerre, confira integridade SQLite e restaure a configuração anterior; remova o segredo RCON. Não inclua dados runtime no Git.

O primeiro ensaio revelou resolução incorreta de namespace via `getByName`, corrigida por `getByIdentifier`; os sete checks finais passaram. A expectativa do runner após reload foi ajustada para a consulta preservada, sem alterar filtros do produto. Testes validam declarações carregadas, não spawn efetivo, cliente visual, carga ou a distribuição alpha.6 fora do Loom. Composições e extensões não interpretadas continuam explicitamente parciais.

## Categorias nomeadas — alpha.7

Build integral: **124 JUnit, zero falhas/erros/skips**. Testes novos cobrem grafo, referências futuras/ciclos/ausentes, schema, dados reais, mesma forma, união sem duplicatas, escopo wiki/progresso, reload/backup e recuperação semântica. Igualdade estrutural do catálogo corrigiu uma regressão inicial; suíte final integral aprovada.

Runner: `npm --prefix tests/integration run test:categories`, com `MC_TEST_RCON_PASSWORD_FILE` apontando para segredo temporário. Use servidor descartável loopback, LuckPerms 5.4.140, perfil novo (`MC_CATEGORY_PLAYER`, padrão DexCategory), configuração `tests/fixtures/categories/config.json` e datapack `tests/fixtures/wiki-facets/datapacks/wiki-facets`; portas padrão 25571/25573. Pare o servidor antes de instalar/trocar espécies. Exige a aceitação prévia da EULA pelo responsável. Sem APIs opcionais de placeholder nesta execução.

**6 checks reais aprovados:** composição nas 41 formas sem cruzamento/duplicação; wiki/progresso no mesmo escopo; permissão atual e botão antigo; invalidação após reload de categoria; rejeição integral de ciclo/tipo desconhecido preservando backup; restauração sem escrever progresso ou entregar ícones. [Evidência](validation/phase2-categories.json). Servidor encerrado normalmente, SQLite íntegro, configuração/propriedades restauradas e segredo removido. Recuperação/reinício das categorias foi testada por JUnit, não novamente no servidor; cliente visual/carga/JAR alpha.7 instalado fora do Loom pendentes.

## Versões de categorias — trabalho local sem commit

Build integral: **130 JUnit sem falhas/erros/skips**. Testes novos verificam identidade canônica, mudança de regras, migração 1→2/reabertura, dados preservados, idempotência, rollback da migração 2, falha de persistência antes de aplicar configuração e hash/conteúdo corrompido recusado sem sobrescrever evidência.

Servidor dev: repetir `test:categories` com perfil novo (6 checks); parar normalmente, manter backup válido, escrever JSON inválido descartável e reiniciar. Aguardar `Done`/RCON, além de `Shirokai Dex ready`. Executar `MC_CATEGORY_MODE=after-restart MC_CATEGORY_PLAYER=DexHistoryB` com o runner e segredo temporário usuais (2 checks). Conferir snapshots e first_observed_at no SQLite antes/depois; nesta execução ambos permaneceram idênticos. Uma tentativa antes do RCON pronto foi recusada; após readiness, execução final passou. [Evidência](validation/phase2-category-versions.json). Sem nova certificação de cliente visual/carga/JAR fora do Loom.

## Recompensas de itens e desbloqueios — local

`test:rewards` usa fixture `tests/fixtures/rewards/config.json`, servidor isolado com LuckPerms/RCON e perfil novo. Sete checks: bloqueio/prévia somente de leitura, marcos/32 entradas, inventário cheio, confirmação/entrega, repetição, permissão revogada e reload. Modo `MC_REWARD_MODE=after-restart` confirma inventário/journal; fixture administrativa descartável indicada por MC_REWARD_REVIEW_FIXTURE testa revisão/conclusão sem efeitos e cancelamento protegido. [Evidência](validation/phase3-items.json). Não inserir fixtures em bancos reais.

`test:reward-unlocks` usa `tests/fixtures/reward-unlocks/config.json` e perfil novo. Mantém configuração válida alterada para testar reinício; executar `MC_UNLOCK_MODE=after-restart` após parar/iniciar normalmente, preservando o mesmo perfil/mundo. Confere desbloqueio observado sem entrega, persistência após progresso/requisito mudarem, permissão revogada e resgate posterior único. [Evidência](validation/phase3-unlocks.json). Não fazer hot reload de espécies. Aguardar RCON/Done e readiness do Dex.

Validação final local: **159 JUnit, zero falhas/erros/skips** e **25 checks em servidor** no conjunto dos blocos de recompensas. Regressão final acrescenta falha real de salvamento: somente no mundo descartável `reward-validation`, runner troca temporariamente o diretório playerdata por um arquivo para provocar erro vanilla, restaura em finally e verifica NEEDS_REVIEW/ausência de repetição após reinício. `MC_REWARD_FIXTURE` aponta para `tests/fixtures/reward-failures/config.json`; perfil novo; no reinício, `MC_REWARD_MODE=after-restart` e `MC_REWARD_EXPECT_DIAMONDS=3` preservam o mesmo perfil. [Prova](validation/phase3-items.json). Não executar essa fixture em mundo/banco de produção. Todos os processos encerrados e configuração local restaurada.

## Itens salvos e componentes — local alpha.2

Build integral: **179 JUnit, zero falhas/erros/skips**. Casos novos cobrem checksum/limites, quantidade, histórico/imutabilidade, rollback de save/delete, reparo auditado, quota, tipos explícitos, resolução, backup independente e falha de lookup; fila de revisão paginada exclui estados finais. Uma colisão de nome de variável foi corrigida antes do build final.

Runner `npm --prefix tests/integration run test:saved-items`: servidor descartável loopback, LuckPerms 5.4.140/RCON, perfil novo e config `tests/fixtures/saved-items/config.json`. Responsável já aceitou EULA. `MC_SAVED_PLAYER` muda o perfil; segredo em MC_TEST_RCON_PASSWORD_FILE. O teste cria itens vanilla por comando apenas no perfil descartável. Cinco checks verificam captura/round-trip de nome, lore, encantamento, dano e custom_data; permissão no confirm; componentes inválidos rejeitados; edição/exclusão não alteram snapshots; backup resolve referências.

Parar/reiniciar preservando o mundo e o mesmo perfil; configuração válida semanticamente dependente de modelo apagado deve recuperar o backup. `MC_SAVED_MODE=after-restart` executa três checks: recuperação/entrega preservada, não repetição/inline customizado e regressão de itens comuns sem mistura de componentes. Mantém configuração alterada para testar fluxo. Após novo reinício, `MC_SAVED_MODE=review-list` verifica duas vezes agrupadas: permissão/páginas da fila real e tipos explícitos ITEM/CUSTOM_ITEM/SAVED_ITEM. Total desta etapa: **10 checks de servidor aprovados**, com SQLite e planos/auditoria conferidos. [Evidência](validation/phase3-saved-items.json).

Ao terminar: stop normal, integrity_check, restaurar propriedades/config/backup anteriores e remover segredo/mod opcional temporários. Cliente visual/carga/JAR fora do Loom não certificados. Não rodar fixtures em dados reais.

## Pokémon e primeira passagem do layout — local alpha.4

**214 JUnit sem falhas/erros/skips**: schema/limites e reuso de snapshots Pokémon, contagem de etapas/journal misto, layout imutável e slots/paletas/reload. [Pokémon](validation/phase3-pokemon.json), [layout](validation/layout-home.json).

POKEMON: `pokemonProbeJar` é um JAR separado de desenvolvimento, nunca do mod. Fixture `tests/fixtures/pokemon-rewards/config.json`, mundo descartável pokemon-reward-validation e perfil DexPokemon; probe inspeciona atributos e preenche equipe/PC apenas nesse mundo/perfil. Intervalo Cobblemon foi reduzido temporariamente a dois segundos e depois restaurado. `npm --prefix tests/integration run test:pokemon`: sete checks, incluindo oito candidatos inválidos, confirmação/atributos/shiny sem captura, Alola/overflow e lotação. Falha real troca temporariamente a pasta pcstore por arquivo e restaura em finally; touch da fixture permite o salvamento nativo posterior. Após reinício, MC_POKEMON_MODE=after-restart: um check confirma UUIDs/contagens e bloqueio, incluindo a operação incerta. Total oito; journal/intenções/evidências conferidos no SQLite.

Layout: `layoutProbeJar` expõe /menu exclusivamente em mundo layout-validation e jogador DexLayout não OP, para verificar a fonte real do comando. Fixture `tests/fixtures/layout/config.json`; `npm --prefix tests/integration run test:layout`: oito checks de gradiente/54 slots/perfil, bloqueios e inventário, navegação, 20 marcos paginados, permissão, conflito/tema Ocean/36 slots/idioma e /menu sem elevação. Remover probe e LuckPerms, reiniciar: MC_LAYOUT_MODE=without-external confirma botão indisponível; npm test repete 13 checks da fundação/wiki e ausência das APIs opcionais. Total 22 checks de layout/regressão, 30 somando este bloco e Pokémon. Helpers de coleções/contextos agora clicam no hub para chegar ao seletor; não certificam nova execução completa desses dois runners.

Todos os servidores foram encerrados; SQLite integrity_check=ok; configuração/propriedades e arquivo Cobblemon anteriores restaurados, segredos/probes removidos. Aparência em cliente gráfico e carga, instalação do JAR fora do Loom, formatos/provedores Pokémon adicionais e layouts completos dos submenus continuam pendentes. Não usar fixtures em dados reais.

## Regressão de empacotamento alpha.5

`./gradlew --no-daemon clean build serverDistribution` executa JUnit e `verifyModJar`. O segundo lê o ZIP final, o descritor e o bytecode ASM (nome interno, Java 21, interface ModInitializer, método público onInitialize), além do namespace intermediary/dependências embutidas; rejeita descritor nas fontes e probes no binário. `build/server-mod` contém somente o binário validado e SHA256SUMS e é a única origem do artefato de instalação do Actions.

A investigação reproduziu o erro de entrypoint com fontes alpha.4 em servidor Fabric 0.17.2 / MC1.21.1 fora do Loom, isolado em /tmp, sem outros mods de usuário. Ver `validation/entrypoint-alpha5.json` para o resultado da inicialização corrigida. A presença de entradas no ZIP não substitui esse teste real.

## Navegação personalizada e estados de resgate

`MenuNavigationTest` verifica defaults/imutabilidade/trocas, limites/duplicatas/ações desconhecidas e preservação de backup na rejeição. O runner `tests/integration/submenus.cjs` usa servidor empacotado, loopback e a fixture `tests/fixtures/submenus/config.json`, com posições alteradas nas 11 telas. Não execute em servidor real de usuários.

Para preparar estados: inicialize um servidor descartável em `/tmp` uma vez para criar schema 5 e pare-o; copie a fixture para config/shirokaidex/config.json; com o servidor parado execute `python3 tests/integration/seed-submenus.py /tmp/SEU_SERVIDOR --player DexNavLayout`. O script recusa perfis com operações existentes e cria estados sintéticos identificados no audit. Após iniciar, rode com MC_TEST_RUN, MC_TEST_PORT, MC_TEST_RCON_PORT e MC_TEST_RCON_PASSWORD_FILE, usando `MC_SUBMENU_PLAYER` igual ao nome semeado. Segredo somente em arquivo externo ao repo. O comando givepokemonother usado pelo runner registra uma obtenção real no perfil de teste para liberar marcos.

Modo padrão exige LuckPerms 5.4.140 para autorizar reload ao jogador; console sem jogador não tem essa permissão. `MC_SUBMENU_MODE=without-luckperms` encerra após os seis casos de navegação/resgate, sem testar reload. Para repetir o teste inteiro, semeie outro perfil reservado DexNav (até 16 caracteres). O runner não deve ser repetido como se o perfil já resgatado fosse novo.

Cobertura: paginação/troca close-next, filtros/formas/spawn, shift-click bloqueado, rodapé desativado, estados concluído/revisão/cancelado/plano alterado/bloqueado/recuperado, código de operação, confirmação/recuo, item personalizado intacto e não repetição. No modo LuckPerms, testa rejeição de reload e retorno aos defaults. Runner restaura o arquivo de configuração ao sair; encerre o servidor e remova o segredo/propriedade RCON ao concluir. Aparência em cliente e carga permanecem pendentes.

## Aparência dos submenus alpha.7

`MenuAppearanceTest`: limites de linhas/textos/lore, templates e expansão sem reinterpretação de valores, retenção de estado nos nomes, registro de ícones e preservação de configuração/backup após falha. `tests/integration/menu-appearance.cjs` usa a fixture `menu-appearance` e o mesmo seeder, com `--fixture menu-appearance` e perfil reservado novo (`--player DexNavSkin`, MC_SUBMENU_PLAYER igual). Exige LuckPerms no servidor descartável para os testes finais de reload. Use as variáveis MC_TEST_* documentadas acima.

O runner valida 11 telas com linhas 4/5/6, slots fora do tamanho original, títulos reais, paletas, ícones/nomes/lore de navegação, ausência de ação em marcos bloqueados e uma entrega real preservando metadados do prêmio. Rejeita ícone inexistente e retorna aos defaults e depois à aparência configurada. A conferência é de protocolo/servidor, não inspeção gráfica.

Os runners antigos de itens/desbloqueios/itens salvos/Pokémon agora verificam estado persistido e ausência de botão em tentativas repetidas, preservando os asserts de inventário/stores. Apenas sua sintaxe foi conferida neste bloco; as suítes completas históricas não foram repetidas. Não confundir isso com o runner novo exercitado em servidor.

## Impactor e moedas alpha.8

Unitários `CurrencyRewardsTest`/`CurrencyPersistenceTest` cobrem normalização exata/compatibilidade/limites, ordem e intenção de passos mistos, ausência de moeda antes de qualquer efeito, espera por persistência, falha/reinício/anti-repetição, leitura numérica não ambígua e perda de precisão do double nativo.

Runner `tests/integration/currency.cjs`, fixture `tests/fixtures/currency/config.json` e manifesto runtime-mods.json. Servidor somente em /tmp/loopback, perfil reservado DexCurrency novo, MC_TEST_RUN/PORT/RCON_PORT/RCON_PASSWORD_FILE como nos demais runners. Três modos: `MC_CURRENCY_MODE=without-impactor`, padrão e `after-restart`, mantendo o mesmo perfil/banco/mundo. Entre etapas, parar e aguardar o servidor. O teste de ausência deve preceder instalação do Impactor no servidor descartável.

Para o modo padrão: instalar somente nesse servidor o Impactor oficial com hash verificado; na configuração nativa de testes definir default-balance 0 e moedas `impactor:dollars` com duas casas, `impactor:tokens` com zero casas. Token usa symbol ✦, primary false, singular/plural e formatting como um registro nativo normal. Não aplicar essa configuração no servidor de usuários. O runner concede Pikachu via comando Cobblemon real para observar o marco.

O modo padrão deposita 12.5 dólares, 3 tokens, depois um item +2.5 dólares/+1 token, e verifica saldo nativo durável 15/4. Moeda inexistente/precisão inválida devem bloquear antes de itens. O último caso renomeia temporariamente o arquivo nativo desse perfil, substitui por diretório para induzir falha real e restaura em finally; a operação precisa ficar em revisão sem novo crédito. Não repetir o modo padrão em perfil já resgatado. Após reiniciar, modo after-restart confirma saldos/inventário/estados sem repetir entregas. Restaura JSON do Dex ao terminar; encerrar servidor, remover segredo RCON/propriedade e JAR Impactor temporário ao finalizar.

Backends fora do JSON nativo, cliente gráfico e carga não certificados. Erros de arquivo neste cenário são injetados intencionalmente. Registro completo em `validation/phase3-currency.json`.

## Checkpoint Crates — alpha.9 na branch de validação

[Procedimento e limites](CRATES_CHECKPOINT_HANDOFF.md), [evidência](validation/phase3-crates-checkpoint.json). Servidor **empacotado** Fabric0.17.2/MC1.21.1/Cobblemon1.8.3, LuckPerms5.4.140 e Impactor5.3.5, mundo descartável loopback `crate-checkpoint-validation`, perfil reservado `DexCheckpoint`. Crates0.2.1 original e hash no manifesto `tests/fixtures/crates-checkpoint/runtime-mods.json`; Impactor no manifesto currency. Nunca usar mundo/mods de usuários.

Build `build serverDistribution remapCheckpointProbeJar` com helper cloud/JDK21/TLS. Instalar somente JAR intermediary e probe remapeado separado; fixture crates-checkpoint. EULA exige aceite do responsável (já concedido nesta sessão). `MC_TEST_RUN` aponta para `/tmp/...`; RCON usa arquivo temporário fora do Git. `npm --prefix tests/integration run test:crates-checkpoint`, com `MC_CHECKPOINT_MODE=main`, depois `after-restart`, `vanilla`, `vanilla-restart`. Encerrar normalmente entre modos; retirar Crates/probe antes de vanilla, preservando mundo/config. O runner emite a chave como jogador autorizado, salva seus componentes e substitui o papel provisório da fixture; não fabrica assinatura.

Journal deve continuar PROCESSING durante atraso de 2000ms apesar da alteração já visível, e confirmar após future/readback. Falha real usa somente o `.dat` do perfil de teste, com backup/restauração em finally; deve bloquear revisão/repetição e recusar novo efeito com provedor unhealthy. Reinícios verificam arquivos reais e estados persistidos. Sem Crates/probe, fallback vanilla deve continuar funcional.

Regressões de itens/Pokémon usam seus runners e mundos descartáveis próprios; `remapPokemonProbeJar` produz probe separado intermediary (input `pokemon-probe-dev`, evitando colisão com saída). A primeira tentativa de remapeamento colidiu com o nome de input e foi corrigida. Runner de itens tinha matcher antigo “inventário sem espaço”; agora espera “sem espaço”, compatível com a mensagem atual de inventário/equipe/PC, sem alterar validação do produto.

Não certifica outros mods/versões/segredos, concorrência ampla, timeout real, perda de energia ou carga. CRATE_KEY dedicado não está implementado. Após os testes, encerrar servidores, verificar SQLite, remover segredo temporário e conferir que produção não contém probes. Não publicar release/ambiente automaticamente.

## COMMAND — alpha.10 local

[Relatório](validation/phase3-commands.json), [política e escopo](COMMAND_REWARDS.md). Build/JDK21/TLS e serverDistribution; 282 JUnit (oito novos). Servidor Fabric0.17.2/MC1.21.1/Cobblemon1.8.3 empacotado fora do Loom, sem probe, em `/tmp`, mundo `command-reward-validation`, loopback/EULA aceita, perfil reservado DexCommand e fixture `tests/fixtures/commands/config.json`.

`MC_TEST_RUN=/tmp/servidor MC_TEST_RCON_PASSWORD_FILE=/arquivo/temporario npm --prefix tests/integration run test:commands`: sete checks. Encerre/salve normalmente e reinicie o mesmo mundo/config; `MC_COMMAND_MODE=after-restart` com o mesmo comando: dois checks. Verificam prévia/segunda confirmação, permissões PLAYER, comando ausente, efeito scoreboard CONSOLE, mensagem PLAYER, erro de execução, intenção/snapshot/revisão/anti-repetição, revogação por política e troca de template, reload inválido, reinício e resolução supervisionada sem replay. Usar mundo novo no modo inicial.

Primeiro ensaio de um teste novo ficou bloqueado por join no worker SQLite do próprio teste; o worker foi encerrado e o teste passou a encadear future sem join. Produto não foi alterado para contornar o teste. Execução final da suíte passou; uma consulta SQL independente comprova INTENT antes do efeito simulado.

Comandos externos, recibos automáticos, planos mistos, crash durante dispatch, carga e cliente gráfico continuam pendentes. A suíte Java inclui regressões anteriores; os runners completos de itens/Pokémon/moedas/checkpoint não foram repetidos nesta alpha.10. A evidência alpha.9 permanece separada. Nenhuma release ou configuração de ambiente publicada.

## Executor reutilizável e recuperação — alpha.11

[Comandos e requisitos](RECOVERY.md#automatização-dos-testes), [evidência](validation/phase3-recovery.json): 290 JUnit, nove checks recovery e nove regressões COMMAND. O executor cria mundo novo, preserva base, espera Done + Dex ready, para/salva antes de seeding/restart, testa e limpa RCON em finally. Concurrency de operadores é testada em Java; callback obsoleto por permissão/desconexão/reload e identidade/payload são testados nativamente. Não reexecuta INTENT nem presume durabilidade de prefixo.

## Captura nativa — 0.4.0-alpha.1

[Procedimento](CAPTURES.md#validação-e-repetição), [evidência](validation/phase4-captures.json): 304 JUnit, nove checks de captura/reinício e nove de recuperação/schema6. `remapCaptureProbeJar` gera teste separado; executor --suite captures prepara datapack que favorece spawn nativo de Pikachu shiny. A sonda seleciona a entidade realmente gerada, libera pequena área de colisão e lança Master Ball física; não atribui SpawnCause e não emite o primeiro evento positivo artificialmente. Replay usa evento nativo já observado para provar dedup.

Concessão por comando não cria evento; entidade administrativa realmente capturada é UNKNOWN. Duas capturas naturais distintas da mesma espécie incrementam total e preservam classificação por versão; reload de categorias não reescreve anteriores. Park de outra criatura natural permite demonstrar perda da causa no reload e classificação conservadora. Não certifica todos os caminhos de batalha/pesca/spawners nem cliente visual/carga.

Ensaios corrigidos: cenário inicial de projétil não concluiu captura, ajustado apenas no probe; stats species com namespace falhou por argumento word, corrigido no produto para string completa validada e repetido. Expectativa antiga de cinco migrações foi atualizada para seis; teste explícito de upgrade preserva plano legado. Dimensão registrada é a da Pokébola. Probes não integram distribuição.

## Proveniência — desenvolvimento 0.4.0-alpha.2

313 JUnit aprovados, incluindo nove testes de prova/consumo/migração6→7. Repetir build/serverDistribution/remapCaptureProbeJar e integration_server.py --suite captures; o executor escolhe o probe correspondente à versão exata. Suíte captura/reinício: três capturas legítimas (duas LIVE_NATIVE, uma PERSISTED_NATIVE após reload), uma UNKNOWN administrativa, replay sem duplicação e snapshots preservados. Regressão recovery usa schema7. Evidência e hashes: [phase4-provenance.json](validation/phase4-provenance.json). Release alpha.1 mantém seu relatório histórico separado.

## Batalha PvE — alpha.3

Mesmo executor --suite captures, build/serverDistribution/remapCaptureProbeJar. Dez checks nativos: oito antes e dois depois do reinício. Fixture continua com Pikachu shiny natural; probe inicia PvE real, exige batalha ativa e lança bola na mesma entidade. Final: quatro capturas legítimas (três LIVE_NATIVE/uma PERSISTED_NATIVE) e uma UNKNOWN administrativa. Replay de batalha e reload não duplicam. 313 JUnit, sem novos testes de produção porque a política/ledger não mudaram. Recuperação não repetida nesta alpha.3; relatório alpha.2 separado. [Evidência](validation/phase4-battle.json).

## Pesca nativa — alpha.4

Executor --suite fishing instala fixture/probe próprios e repete após reinício. Vara POKE_ROD, bobber real em água, ticks até mordida, recolhimento pela API do item, captura física e replay/reload. Primeira fixture gerou peixe nativo, mas bola iniciou sobreposta à caixa maior do Magikarp e não produziu pré-hit; posição corrigida antes da validação final. 317 JUnit/quatro novos, testes/regressões/hashes no [relatório](validation/phase4-fishing.json). [Repetição e limites](FISHING.md).

## Coleção Shiny — alpha.5

326 JUnit (nove novos): cálculo atual por espécie/forma, ausência de fonte/zero, visto/obtido separados, migração8→9, isolamento e projeção permanente. Captures/fishing agora verificam status Shiny após aquisição administrativa/captura nativa/repetição/reinício. Runner antigo progress adaptado/sintaxe conferida; suíte antiga inteira não repetida. [Relatório](validation/phase4-shiny.json).

## Histórico administrativo — retomada 2026-11-14

Build local com JDK21 completo: `JAVA_HOME=/workspace/.tools/jdk-21.0.13+9 python3 scripts/gradle_cloud.py -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts --no-daemon build serverDistribution`. Não presumir o caminho em outra máquina. Dois casos novos em CaptureHistoryTest cobrem 21 registros empatados/páginas/isolamento, origem desconhecida, limites, imutabilidade, contadores intactos e reinício.

`node tests/integration/capture-history.cjs`, com MC_TEST_RUN e MC_TEST_RCON_PASSWORD_FILE como nos demais runners, exige mundo descartável capture-history-validation e LuckPerms5.4.140, servidor loopback/RCON nas portas usuais. O runner verifica ACL negativa/positiva/revogação, UUID inválido, página limite e ausência de bypass por console. Não executar em mundo de usuários. Cenário nativo usa histórico vazio; conteúdo/persistência são exercitados pelos testes SQLite, sem fabricar capturas legítimas no servidor.

## Recibos e retenção — schema10 local

`CaptureReceiptsTest` e `CaptureRetentionTest` acrescentam nove casos de migração, rollback, replay após remoção/reinício, períodos/fusos, lotes/cutoff, auditoria preservada e configuração estrita. Build/serverDistribution com 337 JUnit aprovado; nenhum caso ignorado. Cenário nativo e instruções de seed/verify documentados em [RETENTION.md](RETENTION.md). Fixtures sintéticas não comprovam captura no jogo.
