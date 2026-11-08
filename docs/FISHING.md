# Captura por pesca — desenvolvimento alpha.4

A suíte usa POKE_ROD.use real, o anzol gerado pelo item e ticks normais até a mordida. O probe recolhe pela mesma API do item quando existe ação planejada pelo anzol. Não cria SpawnCause, não injeta ação de spawn nem emite a primeira captura artificialmente. Datapack próprio favorece Magikarp shiny no contexto fishing. Probe e datapack ficam apenas no servidor descartável.

## Evidência aceita

CaptureAccess exige Pokémon selvagem, classe exata FishingSpawnCause, causa originada pelo singleton BestSpawner.fishingSpawner, pool atual WORLD_SPAWN_POOL, entidade de origem ServerPlayer no mesmo servidor e stack de PokerodItem. Não confia em nome, aspecto fished ou espécie. Uma captura posterior por outro jogador não muda a origem realmente observada; identidade de dono/alvo/Pokébola/pré-hit continua obrigatória para quem captura.

Provas guardam fonte WORLD_PLAYER ou FISHING, separadas do ponto de observação ENTITY_LOAD/PREHIT. O primeiro registro não é substituído. Consumo e captura/agregados são transacionais; replay nunca altera fonte/classificação anterior. Após reload, causa ausente só aceita prova persistida para o mesmo Pokémon/entidade com correlação selvagem pré-hit válida. Sem prova/correlação fica UNKNOWN, consumindo eventual prova correspondente para impedir reuso.

O campo histórico origin=NATIVE_PLAYER_SPAWN foi mantido para compatibilidade dos agregados e indica spawn nativo comprovado causado por jogador; native_source distingue WORLD_PLAYER/FISHING. Eventos sem prova recebem UNVERIFIED. Schema8 deixa eventos antigos com fonte LEGACY, sem inferir pesca; provas anteriores recebem WORLD_PLAYER porque só aquela origem era aceita. Migração preserva prova já consumida e não reabre UNKNOWN antigo. Não fazer downgrade para schema7/6; backup antes de migrar.

## Testes e limites

317 JUnit, incluindo persistência/reload da fonte FISHING, fonte original imutável, UNKNOWN sem metadado legítimo e migração7→8 com prova consumida. [Relatório nativo e regressões](validation/phase4-fishing.json).

```sh
JAVA_HOME=/caminho/jdk21 python3 scripts/gradle_cloud.py --no-daemon build serverDistribution remapCaptureProbeJar
JAVA_HOME=/caminho/jdk21 python3 scripts/integration_server.py --base /tmp/assets-oficiais-preparados --suite fishing
```

Preservar TLS/truststore/proxy conforme TESTING. Apenas loopback, mundos/profiles reservados, save normal e limpeza de RCON/segredo. O teste usa Magikarp shiny/POKE_ROD; outras varas, iscas, pools personalizados e modpacks não estão certificados. Fonte de pesca com jogador ausente ou causa expirada é conservadora e pode ficar UNKNOWN; nenhuma origem é presumida por aspecto. Carga/crescimento de provas, retenção, crash de disco e captura durante falha do banco continuam sem certificação. Este bloco não conclui Fase4 nem publica release.
