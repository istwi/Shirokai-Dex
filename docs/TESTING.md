# Validação da fundação

## Testes automatizados Java

```sh
./gradlew --no-daemon build
```

Na nuvem, use JDK 21 e `python3 scripts/gradle_cloud.py`, conforme [instalação](INSTALLATION.md). Última execução local: 87 casos, zero falhas/erros/skips. `ConfigManagerTest`: 16 casos de parsing estrito, campos duplicados/tipos, reload inválido, backup e recuperação após reinício. `DatabaseTest`: 4 casos de migração/reabertura, 100 atualizações para um UUID, versão futura, rollback e operações após fechamento.

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
