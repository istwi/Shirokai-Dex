# Capturas e estatísticas — 0.4.0-alpha.1

Primeiro bloco da Fase 4. Registra eventos novos do servidor, sem reconstruir capturas a partir da Pokédex, inventário ou Pokémon antigos. Captura é uma ação; dez capturas da mesma espécie contam dez, com uma espécie distinta. Não há pontuação de competição implementada neste bloco.

## Origem comprovada e desconhecida

A integração cruza THROWN_POKEBALL_HIT com POKEMON_CAPTURED do Cobblemon1.8.3. Antes do acerto, o alvo precisa ser selvagem e ter SpawnCause de PlayerSpawner exato usando o pool mundial atual. Depois, UUIDs da Pokébola, Pokémon e jogador precisam corresponder, com a mesma referência de alvo e dono da Pokébola. A evidência prévia é limitada a 512 tentativas e dez minutos; é limpa no ciclo do servidor. Ausência/expiração/incompatibilidade nunca vira origem legítima por suposição.

No escopo testado, duas capturas foram concluídas por colisão/animação nativa de Master Ball com Pikachu shiny gerados pelo spawner real, usando um datapack de condições controladas. Não foi emitido evento artificial para criar a primeira captura positiva. O probe separado só reemite um evento real já observado para testar duplicação. Ele também cria um Pokémon administrativo sem SpawnCause e captura com o motor nativo: a observação fica UNKNOWN e não aumenta estatísticas legítimas.

**Limite comprovado:** um Pokémon naturalmente gerado, preservado e recarregado após reinício perdeu SpawnCause. A captura posterior foi UNKNOWN e ficou fora dos totais legítimos. Portanto, esta versão não cobre universalmente criaturas anteriores ao mod, recarregadas, outros spawners, pesca, batalhas ou extensões. A continuidade de proveniência e esses outros fluxos precisam de auditoria/testes próprios. Comandos de concessão/recompensas/trocas/ovos não são fontes de captura; somente obter/ver um shiny nunca incrementa estes contadores.

## Dados e deduplicação

A chave derivada da Pokébola física + UUID do Pokémon é única no SQLite, inclusive após reinício; não é um identificador oficial de evento inventado. Observação e incrementos são uma única transação. Replay não altera o registro original nem incrementa novamente, mesmo se a segunda entrega perder evidência prévia e parecer UNKNOWN.

Schema 6 guarda jogador, Pokémon/Pokébola, espécie, forma, shiny, labels/tipos reais, categorias observadas, fingerprint das regras, instante UTC em milissegundos, dimensão/posição da Pokébola e origem. Os totais permanentes e contagens por espécie/categoria são agregados independentes dos detalhes. Regras posteriores não reclassificam registros anteriores automaticamente. Dados UNKNOWN ficam auditáveis, separados dos totais legítimos.

Não há importação retroativa ou limpeza automática de histórico nesta entrega. Contagens por período usam registros detalhados; antes de adicionar retenção será necessário preservar agregados de calendário. Falha de banco/snapshot é registrada em log e pode deixar lacuna; não se fabricam eventos para preencher essa lacuna. Não há transação distribuída com o Cobblemon, garantia contra perda de energia ou certificação de carga.

## Consultar os próprios dados

```text
/dex profile captures
/dex profile captures species cobblemon:pikachu
/dex profile captures category electric_catches
```

Resumo: total, shiny, lendárias/míticas por labels observadas, espécies distintas, hoje/semana/mês e quantidade de observações sem origem comprovada. Semana ISO começa segunda-feira. Nenhuma comparação de taxa, isoladamente, acusa abuso. Não há acesso público aos dados de outro jogador por esses comandos.

`statistics_timezone` aceita um ZoneId Java real, por exemplo `America/Sao_Paulo`; padrão UTC. Dia/mês usam calendário local e semana usa início ISO, inclusive transições de horário de verão. Trocar fuso muda consultas de período, preservando instantes UTC e totais permanentes. Categorias usam os filtros já implementados e suas versões; shiny tem campo nativo próprio, não uma label fabricada.

A coleção Shiny/percentual anterior ainda não foi conectada ao novo ledger: continua N/D, separada do contador de capturas shiny verificadas. Perfil gráfico, recordes, histórico administrativo detalhado, revisão/limites por período e rankings/competições seguem pendentes.

## Validação e repetição

[Relatório](validation/phase4-captures.json): 304 JUnit (14 novos), nove checks nativos de captura/reinício e nove de regressão de recuperação no schema 6. Migração 5→6 preserva o plano de recompensa; servidor real e unitários não inventam capturas anteriores.

```sh
JAVA_HOME=/caminho/jdk21 python3 scripts/gradle_cloud.py --no-daemon build serverDistribution remapCaptureProbeJar
JAVA_HOME=/caminho/jdk21 python3 scripts/integration_server.py --base /tmp/servidor-preparado --suite captures
```

Usar TLS/truststore/proxy herdados conforme TESTING. O executor instala probe remapeado somente no mundo descartável `native-capture-validation`, com comandos restritos ao console e perfil DexCapture, e datapack de teste. O probe seleciona entidades realmente geradas pelo pool, prepara uma pequena área livre para colisão e lança projéteis nativos; não atribui SpawnCause. RCON/segredo são removidos após testes. Nunca incluir probe ou datapack de fixture na distribuição do usuário. Nenhuma nova release, merge na main ou configuração de ambiente publicada.
