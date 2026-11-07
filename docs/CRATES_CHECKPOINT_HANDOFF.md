# Passagem — checkpoint do Shirokai Crates

## Retomada atual — validação local da alpha.9

Branch `handoff/crates-checkpoint`, base `6b97093`; main preservada em `c89cc1b` (funcional `191a65e`). O responsável confirmou a continuação após a pergunta de commit/push somente nesta branch; envio deste bloco validado autorizado. Não houve release nem publicação de ambiente.

O runner `tests/integration/crates-checkpoint.cjs` executa quatro modos em servidor **empacotado fora do Loom**, mundo descartável `crate-checkpoint-validation`, loopback, perfil reservado `DexCheckpoint`:

1. `main`: chave real emitida por `/crates givekey`, capturada por `/dex admin item save native_key`, usada via SAVED_ITEM (duas unidades). Componentes preservados; `Tokens.matches` reconhece três chaves autênticas no total. Writer atrasado em 2000ms: inventário já atualizado e journal ainda PROCESSING; ACK somente depois do checkpoint/readback. Repete atraso para itens simples, bloqueia repetição, injeta falha real no `.dat` do perfil, restaura em finally e comprova NEEDS_REVIEW. Provedor unhealthy impede novo efeito, sem fallback silencioso.
2. `after-restart`: Crates e probe presentes; chaves continuam autênticas, quantidades/estados persistem, efeito incerto não é reentregue.
3. `vanilla`: retirar somente Crates e probe de teste, reiniciar preservando mundo/config; resgate emerald conclui pelo fallback vanilla e revisão anterior permanece bloqueada.
4. `vanilla-restart`: novo reinício sem Crates/probe; inventário e journal continuam corretos, sem reentrega.

Build: `build serverDistribution remapCheckpointProbeJar remapPokemonProbeJar`, 274 JUnit sem falhas/erros/skips; 26 checks de servidor: 8 checkpoint + 10 regressões de itens/reinício + 8 de Pokémon/reinício. Servidores encerrados, SQLite integrity_check=ok, configuração temporária do provedor restaurada e segredo RCON removido. Evidência e resultados de regressão: [phase3-crates-checkpoint.json](validation/phase3-crates-checkpoint.json). O probe separado nunca integra o JAR distribuído. Para regressão Pokémon empacotada, `remapPokemonProbeJar` gera o probe intermediary; seu input usa classifier `pokemon-probe-dev` para não sobrescrever a saída.

Comando para cada modo (somente servidor isolado com EULA já aceita):

```sh
MC_CHECKPOINT_MODE=main MC_TEST_RUN=/tmp/servidor-descartavel MC_TEST_RCON_PASSWORD_FILE=/arquivo/temporario npm --prefix tests/integration run test:crates-checkpoint
```

Os modos não são intercambiáveis com uma reconexão: encerrar/salvar o servidor e reiniciar nas transições indicadas. O modo main exige perfil/mundo novos e fixture `tests/fixtures/crates-checkpoint/config.json`; ele substitui o papel provisório por referência à chave emitida pelo próprio provedor. Não salvar payloads assinados ou segredos no Git. O servidor de testes precisa das versões/checksums dos manifestos abaixo.

## Próximo bloco

- Envio deste bloco validado autorizado somente nesta branch. Manter a main funcional preservada; build sozinho nunca autoriza promoção.
- CRATE_KEY dedicado continua **não implementado**. A compatibilidade validada é SAVED_ITEM + checkpoint com Crates0.2.1/mesmo segredo. Auditar contrato público de criação/acesso antes de implementar um novo tipo; reflexão privada está restrita ao probe de teste, não é API de produção.
- Versionamento desconhecido/indisponibilidade/timeout/carga/concorrência ampla, formatos e modpacks adicionais não certificados. Sem transação distribuída, retry automático ou promessa de recuperação após perda de energia.

## Estado de origem (antes desta retomada)

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

## Procedimento original (agora automatizado pelo runner)

1. Preservar main validada. Checkout desta branch e ler diff, AGENTS/status/next. Conferir origem antes de continuar; não reaplicar alpha.6/7/8.
2. Criar runner `tests/integration/crates-checkpoint.cjs` em servidor Fabric empacotado/loopback/descartável. Native mundo deve chamar-se `crate-checkpoint-validation`, pois o probe rejeita qualquer outro e comandos de jogadores.
3. Instalar dependências exatas de testes: Cobblemon/Fabric API/Kotlin existentes, LuckPerms5.4.140, Impactor5.3.5 (manifesto em fixtures/currency/runtime-mods.json), Crates0.2.1 e probe remapeado. Não alterar mods de usuários. Gerar JAR/probe com Java21: `./gradlew build serverDistribution remapCheckpointProbeJar` (helper cloud/TLS em TESTING.md).
4. Usar perfil novo reservado `DexCheckpoint` (até16 caracteres). Dar permissões LP shirokaicrates.give, shirokaidex.admin.items/reload. Emitir chave real como jogador autorizado: `/crates givekey DexCheckpoint shiny1 1`. Console não satisfaz esse comando. Esperar checkpoint antes de próximos passos.
5. Segurar chave e `/dex admin item save native_key`. Trocar `keys_saved.items` por `[{"type":"SAVED_ITEM","saved_item":"native_key","count":2}]` e recarregar como jogador autorizado. Conceder Pokémon via comando Cobblemon real para observar marco. Não ler segredo/HMAC, não recriar assinatura: a chave vem do provedor.
6. Abrir confirmação de keys_saved; antes do clique final, RCON `dexcheckpointprobe` atrasa a fila nativa em 2000ms. Clicar confirmar e verificar: inventário recebe chaves, mas resgate só conclui depois do checkpoint, não em revisão por leitura prematura. Conferir chave com RCON `dexcheckpointprobe keys DexCheckpoint shiny1` (deve reportar 3 chaves válidas pela rotina Tokens.matches real). Probe só permite prefixo reservado e não expõe segredo.
7. Repetir atraso com itens simples da fixture e validar leitura/ACK. Injetar falha real no arquivo playerdata SOMENTE do teste, restaurando em finally, e confirmar NEEDS_REVIEW/sem repetição. Checkpoint nativo fica unhealthy até reiniciar; isso é comportamento do provedor. Nunca testar num mundo de usuários.
8. Validar fallback sem Crates (remover também probe), resgate vanilla e comportamento após reinício. Salvar evidências reais, conferir JAR sem probe e documentar limites antes de integrar na main.
9. Depois dessa base comprovada, implementar tipo CRATE_KEY dedicado. Até lá chaves via SAVED_ITEM são apenas cenário de compatibilidade a testar; não marcar integração pronta. Tokens.create/Matches são públicos, mas tokens é campo privado do serviço. Não chamar givekey e confiar apenas no retorno: entrega/save são assíncronos.

## Limites registrados na passagem original

- Nada desse WIP foi publicado como release. Main contém apenas blocos anteriormente validados e documentação de passagem.
- Fonte/manifestos/fixtures são fonte de continuidade, não /tmp ou caches. Servidor temporário foi mandado parar; propriedades/segredo/probes/dependências temporárias serão removidos antes de encerrar.
- A passagem original só tinha build baseline 274 JUnit + verifyModJar/serverDistribution. A retomada descrita no topo agora exerceu os comandos e o fluxo nativo; não confundir o estado original com o atual.
- Sem transação distribuída SQLite/inventário; futuro falho/timeout deve manter revisão e nunca reentregar automaticamente.
