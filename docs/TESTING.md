# Validação da fundação

## Testes automatizados Java

```sh
./gradlew --no-daemon build
```

Na nuvem, use JDK 21 e `python3 scripts/gradle_cloud.py`, conforme [instalação](INSTALLATION.md). Última execução local: 20 casos, zero falhas/erros/skips. `ConfigManagerTest`: 16 casos de parsing estrito, campos duplicados/tipos, reload inválido, backup e recuperação após reinício. `DatabaseTest`: 4 casos de migração/reabertura, 100 atualizações para um UUID, versão futura, rollback e operações após fechamento.

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

Se o diretório pessoal for somente de leitura, acrescente `--cache /workspace/.cache/npm` aos comandos npm. Para outra porta loopback: `MC_TEST_PORT=25572 npm test`. O smoke conecta `DexFoundation`, verifica consulta oficial, menu, bloqueio de pickup/clique direito/Shift/hotbar, fechamento, ausência de itens indevidos, progresso inalterado e rejeição de reload sem LuckPerms. Total: 10 verificações na execução aprovada. O relatório é sobrescrito por execução em `build/integration-foundation.json`, com timestamp e target.

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
