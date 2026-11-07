# Validação da fundação

## Testes automatizados Java

```sh
./gradlew --no-daemon build
```

Na nuvem, use JDK 21 e `python3 scripts/gradle_cloud.py`, conforme [instalação](INSTALLATION.md). Última execução local: 69 casos, zero falhas/erros/skips. `ConfigManagerTest`: 16 casos de parsing estrito, campos duplicados/tipos, reload inválido, backup e recuperação após reinício. `DatabaseTest`: 4 casos de migração/reabertura, 100 atualizações para um UUID, versão futura, rollback e operações após fechamento.

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

```sh
cd tests/integration
npm ci --ignore-scripts --no-audit --no-fund
npm test
```

Se o diretório pessoal for somente de leitura, acrescente `--cache /workspace/.cache/npm` aos comandos npm. Para outra porta loopback: `MC_TEST_PORT=25572 npm test`. O smoke conecta `DexFoundation`, verifica consulta oficial, menu, bloqueio de pickup/clique direito/Shift/hotbar, fechamento, ausência de itens indevidos, progresso inalterado e rejeição de reload sem LuckPerms. Total atual: 11 verificações na execução aprovada, incluindo navegação do seletor para a página Nacional. O relatório é sobrescrito por execução em `build/integration-foundation.json`, com timestamp e target.

O cliente de protocolo ignora metadados de entidades, receitas, advancements e árvore de comandos customizados que a biblioteca vanilla não entende. Inventário, comandos executados e respostas testadas são reais, não mocks. Isto não valida renderização em cliente Minecraft/Cobblemon, sugestões de comandos, receitas nem mecânicas de batalha/captura.

Também foi realizado reinício do servidor de desenvolvimento com JSON inválido: backup válido recuperado, original inválido preservado, SQLite íntegro e sem duplicação da migração, UUIDs/primeiro acesso preservados e smoke aprovado novamente. O arquivo válido do teste foi restaurado depois. Encerre os servidores com `stop` e confirme salvamento e fechamento.

O mesmo smoke passou com o JAR final em um servidor isolado fora do Loom, usando o perfil oficial Maven do Fabric Loader e dependências originais de release. Relatório persistido: [validação](validation/phase1-foundation.json). A primeira conexão ao servidor empacotado foi tentada cedo demais e falhou com ECONNREFUSED; a execução aprovada ocorreu após `Done`, sem alterar o mod para esconder a falha.

## Pendências explícitas

- LuckPerms instalado: permissão positiva, contextos e reload administrativo de ponta a ponta.
- Cliente Minecraft/Cobblemon real: aparência, arraste, double-click, teclas de descarte e demais interações não cobertas.
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

Wiki, visualização em cliente real, contextos de dimensão, desempenho e captura shiny histórica seguem pendentes. A documentação histórica das alphas anteriores mantém seus próprios relatórios imutáveis.
