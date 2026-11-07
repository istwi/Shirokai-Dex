# Passagem — checkpoint do Shirokai Crates

## Estado confirmado

- Main validada: `191a65e` (menus alpha.6/7 e moedas alpha.8), 274 JUnit aprovados e 8 checks monetários reais, além das evidências anteriores dos menus.
- Actions dessa main: https://github.com/istwi/Shirokai-Dex/actions/runs/42406111856 — sucesso, incluindo download e checksum do artefato publicado.
- Artefato instalável alpha.8: https://github.com/istwi/Shirokai-Dex/actions/runs/42406111856/artifacts/13587642421 . Instalar o JAR sem sources após extrair o ZIP.
- Branch `handoff/crates-checkpoint`: trabalho incompleto alpha.9, separado da main. Build/JAR e 274 JUnit passaram; teste funcional do checkpoint em servidor NÃO foi executado. Usuário pediu parar para trocar de Codex. Não declarar chaves integradas nem promover o WIP por apenas compilar.

## Descoberta e alteração em andamento

Repo oficial Crates: https://github.com/istwi/Shirokai-Crates , HEAD auditado `05b3995c4d4466ef17c6171ad07d4f0eab512db9` confirmado no GitHub. Release v0.2.1, SHA256 do JAR `49a82904ef3fa971c535191657a592b9ee5affad5577913f1bb537ca1274adfd`; hash e métodos conferidos por javap no binário real. Metadata exige Loader >=0.17.2, MC1.21.1, Java21, LuckPerms>=5.4.140 e Impactor5.3.5. Não inferir versão instalada do usuário a partir disso.

`ShirokaiCrates.service()` expõe `checkpoints()`. `PlayerCheckpoint.save(ServerPlayer, true)` retorna CompletableFuture e mantém barreira de interação durante escrita; `healthy()`/`blocked(UUID)` são públicos. O mixin PlayerSaveMixin intercepta saves vanilla de jogadores managed e enfileira gravação assíncrona: `saveAll()` retornar NÃO garante que o inventário já foi gravado.

WIP implementado:

- `PlayerSaveCoordinator`: integração opcional por reflexão de métodos públicos, sem dependência obrigatória no JAR Crates. Versão 0.2.1 e API/saúde/barreira conferidas; sem fallback se provedor carregado for desconhecido/indisponível. Sem Crates usa saveAll vanilla.
- `ItemRewardAdapter`: espera o future coordenado no último item e só depois confirma leitura do inventário. Compara snapshot esperado capturado no momento da entrega. Preflight de planos sem itens não é bloqueado por esse coordenador.
- `src/checkpointProbe`: mod somente de teste, separado/remapeado para produção pelo task `remapCheckpointProbeJar`. Nunca incluí-lo no JAR distribuído.
- Fixture `tests/fixtures/crates-checkpoint/config.json` e manifesto do JAR Crates. A recompensa `keys_saved` contém papel provisório: precisa ser substituída no teste por SAVED_ITEM após capturar chave real. NÃO é uma implementação CRATE_KEY pronta.

## Próxima tarefa exata

1. Preservar main validada. Checkout desta branch e ler diff, AGENTS/status/next. Conferir origem antes de continuar; não reaplicar alpha.6/7/8.
2. Criar runner `tests/integration/crates-checkpoint.cjs` em servidor Fabric empacotado/loopback/descartável. Native mundo deve chamar-se `crate-checkpoint-validation`, pois o probe rejeita qualquer outro e comandos de jogadores.
3. Instalar dependências exatas de testes: Cobblemon/Fabric API/Kotlin existentes, LuckPerms5.4.140, Impactor5.3.5 (manifesto em fixtures/currency/runtime-mods.json), Crates0.2.1 e probe remapeado. Não alterar mods de usuários. Gerar JAR/probe com Java21: `./gradlew build serverDistribution remapCheckpointProbeJar` (helper cloud/TLS em TESTING.md).
4. Usar perfil novo reservado `DexCheckpoint` (até16 caracteres). Dar permissões LP shirokaicrates.give, shirokaidex.admin.items/reload. Emitir chave real como jogador autorizado: `/crates givekey DexCheckpoint shiny1 1`. Console não satisfaz esse comando. Esperar checkpoint antes de próximos passos.
5. Segurar chave e `/dex admin item save native_key`. Trocar `keys_saved.items` por `[{"type":"SAVED_ITEM","saved_item":"native_key","count":2}]` e recarregar como jogador autorizado. Conceder Pokémon via comando Cobblemon real para observar marco. Não ler segredo/HMAC, não recriar assinatura: a chave vem do provedor.
6. Abrir confirmação de keys_saved; antes do clique final, RCON `dexcheckpointprobe` atrasa a fila nativa em 2000ms. Clicar confirmar e verificar: inventário recebe chaves, mas resgate só conclui depois do checkpoint, não em revisão por leitura prematura. Conferir chave com RCON `dexcheckpointprobe keys DexCheckpoint shiny1` (deve reportar 3 chaves válidas pela rotina Tokens.matches real). Probe só permite prefixo reservado e não expõe segredo.
7. Repetir atraso com itens simples da fixture e validar leitura/ACK. Injetar falha real no arquivo playerdata SOMENTE do teste, restaurando em finally, e confirmar NEEDS_REVIEW/sem repetição. Checkpoint nativo fica unhealthy até reiniciar; isso é comportamento do provedor. Nunca testar num mundo de usuários.
8. Validar fallback sem Crates (remover também probe), resgate vanilla e comportamento após reinício. Salvar evidências reais, conferir JAR sem probe e documentar limites antes de integrar na main.
9. Depois dessa base comprovada, implementar tipo CRATE_KEY dedicado. Até lá chaves via SAVED_ITEM são apenas cenário de compatibilidade a testar; não marcar integração pronta. Tokens.create/Matches são públicos, mas tokens é campo privado do serviço. Não chamar givekey e confiar apenas no retorno: entrega/save são assíncronos.

## Limites e cuidados

- Nada desse WIP foi publicado como release. Main contém apenas blocos anteriormente validados e documentação de passagem.
- Fonte/manifestos/fixtures são fonte de continuidade, não /tmp ou caches. Servidor temporário encerrou normalmente; propriedades/segredo/probes/dependências temporárias foram removidos antes de encerrar.
- Build baseline 274 JUnit + verifyModJar/serverDistribution; novo fluxo precisa de validação nativa própria. O probe foi compilado/remapeado, mas seus comandos não foram exercitados.
- Sem transação distribuída SQLite/inventário; futuro falho/timeout deve manter revisão e nunca reentregar automaticamente.
