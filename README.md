# Shirokai Dex

Mod server-side experimental para Minecraft 1.21.1, Fabric, Java21 e Cobblemon1.8.3.

## Snapshot atual: 0.7.0-dev.6

Branch `fix/latest-menu-command`, derivada da tag `v0.7.0-dev.1`. Mensagens da Dex no chat usam **✦ SHIROKAI** branco/negrito e texto cinza. Cabeça pessoal, wiki da coleção, botão de voltar por comando e pesquisa compatível com Shirokai Core/Tab permanecem disponíveis.

Menus diretos: `/dex nacional|special|shiny|forms`, `/dexrank captura|derrotados|dex` e `/wiki nacional|special|shiny|forms`. `/dex` sozinho não abre tela; Forms é futura. [Guia dos menus](docs/DIRECT_MENUS.md).

O pacote de configuração do servidor contém18 recompensas revisadas conforme o TXT enviado: itens, dólares/tokens e permissões de tags, chaves autênticas via SAVED_ITEM, Vale Shiny apenas visual e VIP Top removido. O marco final da Shiny inclui um lendário aleatório implementado, shiny e seis IVs31, com espécie/UUID congelados antes da entrega.

**Antes de aplicar os três menus, cadastre os sete modelos de chaves no próprio servidor.** O ZIP não inclui banco, jogadores, segredo ou chaves assinadas de outro servidor. [Instalação e comandos de preparação](docs/SERVER_REWARDS_2026_11.md). Arquivos revisados: [menus do pacote](tests/fixtures/server-rewards/menus).

## Downloads e validação

[Builds e downloads no GitHub Actions](https://github.com/istwi/Shirokai-Dex/actions/workflows/build.yml): selecione o build desta branch/commit. O artifact contém JAR, `shirokaidex-recompensas-atualizadas.zip`, guia, `BUILD_PROVENANCE.json` e `SHA256SUMS`. Este snapshot não implica merge na main, release estável ou publicação do ambiente.

427 JUnit/build/JAR aprovados; sete checks de recompensa em servidor empacotado e24 freezes/restores aleatórios adicionais, incluindo chaves Crates0.2.1, Impactor5.3.5/JSON nativo, LuckPerms5.4.140 e reinício. [Evidência e limites](docs/validation/server-rewards-dev6.json). Os17 pacotes restantes foram validados por parsing/quantidades; não foram resgatados individualmente no servidor. Renderização gráfica e carga não certificadas. [Estilo de chat](docs/validation/chat-style-dev5.json).

## Desenvolvimento

Com JDK21 completo:

```sh
./gradlew --no-daemon build serverDistribution
python3 scripts/package_server_update.py
```

Na nuvem com proxy herdado, use `python3 scripts/gradle_cloud.py -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts --no-daemon build serverDistribution`. Não desabilite TLS/checksums. [Testes e servidores descartáveis](docs/TESTING.md).

Leia [AGENTS.md](AGENTS.md), [estado](docs/PROJECT_STATUS.md), [próximos passos](docs/NEXT_STEPS.md), [checklist](docs/MASTER_CHECKLIST.md) e [auditoria](docs/API_COMPATIBILITY.md). Apenas um Codex por vez; use o checkout existente. [Recompensas Pokémon](docs/POKEMON_REWARDS.md), [itens salvos](docs/SAVED_ITEMS.md) e [contrato de chaves](docs/CRATE_KEY_CONTRACT.md).
