# Menus diretos — desenvolvimento 0.7.0-dev.6

Esta direção substitui o hub e os menus públicos antigos. Na dev.7, `/dex` sozinho executa `skiesguis open pokedex` como o jogador, sem elevar permissões. Requer SkiesGUIs e o menu `pokedex` configurados no servidor; a integração visual externa ainda precisa ser validada nesse servidor. Subcomandos Dex existentes permanecem iguais.

| Comando | Tela |
|---|---|
| `/dex nacional` | Progresso Nacional e seis recompensas percentuais |
| `/dex special` | Progresso Special e seis recompensas percentuais |
| `/dex shiny` | Progresso Shiny e seis recompensas percentuais |
| `/dex forms` | Aviso de atualização futura |
| `/dexrank captura` | Top 10 geral, shiny e lendários na lore |
| `/dexrank derrotados` | Top 10 de derrotas verificadas na lore |
| `/dexrank dex` | Top 10 percentual Nacional, Special e Shiny; Forms futura |
| `/wiki nacional`, `/wiki special`, `/wiki shiny` | Consulta por modelos oficiais de Pokémon |
| `/wiki forms` | Aviso de atualização futura |

A wiki também aceita pesquisa direta: `/wiki nacional pikachu`. Relógio: esquerdo avança e direito volta o filtro; livro: fecha o inventário e recebe a próxima mensagem como pesquisa durante 60 segundos; `cancelar` encerra. A mensagem de pesquisa não é transmitida pelo chat normal. Funil limpa pesquisa e filtro. A borda cinza e os controles inferiores seguem a imagem enviada; 28 posições internas (as não ocupadas ficam vazias) e até duas setas para paginação. Os dados de spawn são um resumo parcial das condições conhecidas, sem garantia de aparição.

## Configurações

Na inicialização, cria somente arquivos ausentes em `config/shirokaidex/menus/`:

- `dex_nacional.json`, `dex_special.json`, `dex_shiny.json`: coleção, apresentação e pacotes de recompensa juntos.
- `dex_forms.json`, `wiki_forms.json`: ícones de atualização futura.
- `wiki_nacional.json`, `wiki_special.json`, `wiki_shiny.json`: título, tamanho, fundo, posições, controles, nomes dos filtros e apresentação dos Pokémon.
- `rank_captura.json`, `rank_derrotados.json`, `rank_dex.json`: itens e Top 10; `ranking_line` aceita `%position%`, `%player_name%`, `%score%`.
- `messages.json`: mensagens da pesquisa, carregamento e falha.

Cada ícone tem `item`, `name`, `lore`, `glint`; botões também têm `slot` (começa em zero). `entry.item` vazio na wiki usa o modelo oficial; preenchido usa o item configurado. `entry.name/lore` aceitam `%pokemon_name%`, `%pokemon_id%`, `%seen%`, `%owned%`, `%captured%` e linha `%spawns%`. Relógio aceita `%filter%`, livro `%query%`. Cor, hex, gradiente de duas cores, negrito, itálico e sublinhado são apresentação; campos não executam comandos.

Dex: `reward_slots` define a posição dos marcos; `reward_states` define aparência de bloqueado/disponível/resgatado. Cartão de jogador aceita `%player%`, `%dex_type%`, `%player_dex_count%`, `%pokedex_total%`, `%player_dex_percent%` (número sem o símbolo), `%progress%` (registrados/total) e `%percent%` (com símbolo). Reward group tem `id`, `percent`, `name`, `icon`, `lore`, `items`, `currencies`, `permissions`. Preserve IDs após uso: identificam o mesmo prêmio no histórico de resgates.

Aplique mudanças com `/dex admin reload`. Slots conflitantes, itens desconhecidos e JSON inválido rejeitam o conjunto inteiro, preservando o estado anterior. `direct.last-good.json` permite recuperar o conjunto validado no reinício; não é uma configuração para edição. `config.json` preserva as configurações gerais; suas listas antigas de coleções/recompensas/menus/conquistas/eventos não comandam os novos painéis. Dados históricos do SQLite são preservados. Eventos não têm entrada pública nem processamento competitivo ativo nesta versão.

## Recompensas do ZIP

Somente configurações foram convertidas. **Nenhum jogador, progresso ou resgate do ZIP foi importado**, conforme escolha explícita do responsável. Não há importador automático de `players/`.

Os 18 pacotes mantêm itens/quantidades, dólares, tokens e nós de permissão originais. Nacional/Special: 5, 15, 30, 50, 75, 100%. Shiny: 1, 3, 10, 25, 50, 100%. A tela tem cinco linhas com recompensas nas posições 12/14/16/30/32/34, cartão 10 e fechar 28.

Moedas exigem Impactor 5.3.5 auditado, backend JSON nativo e moedas registradas `impactor:dollars` e `impactor:tokens`. Permissões exigem LuckPerms e são limitadas a nós `shirokai.dex.*`; não alteram grupos ou VIP. Os nomes históricos `shirokai.dex.legendary.*` foram mantidos para compatibilidade com suas tags externas. Sem um provedor necessário, o pacote inteiro é recusado antes da entrega. A configuração do Dex não instala nem altera outros mods.

O journal registra itens → moedas → permissões, com confirmação por etapa. Falhas externas incertas bloqueiam para revisão; não repetem automaticamente créditos. IDs novos não bloqueiam resgates antigos do mod DexRewards: essa importação foi explicitamente recusada.

## Placeholders para menus externos

Com Text Placeholder API compatível instalada e uma GUI que realmente processe essa API:

| Placeholder | Exemplo |
|---|---|
| `%shirokaidex:progress nacional%` | `123/905` |
| `%shirokaidex:registered nacional%` | `123` |
| `%shirokaidex:total nacional%` | `905` |
| `%shirokaidex:percent nacional%` | `13,59%` |

Troque `nacional` por `special` ou `shiny`. `national` também é aceito pela API. O denominador usa as espécies implementadas e os critérios da coleção; não força 1025 quando isso produziria um total falso. Shiny usa capturas verificadas pelo Dex. Dados indisponíveis retornam `N/D`. Ter um placeholder não garante compatibilidade com qualquer plugin GUI: ele precisa suportar a Text Placeholder API do Fabric. Nenhum PlaceholderAPI Bukkit é embutido.

## Validação local

410 JUnit/build e15 verificações em servidor com APIs reais e reinício aprovados. [Evidência](validation/direct-menus.json). Geometria, controles, modelos no protocolo, pesquisa, reload, ranking e um pacote integral foram testados. Não houve revisão manual da renderização no cliente gráfico ou teste de carga. A falha de captura específica do servidor do responsável ainda não tem causa confirmada; o teste local de PvE atualizou Top Geral/Shiny.

## Botão Fechar ou comando de outro menu (dev.2)

Edite `config/shirokaidex/menus/dex_nacional.json`, `dex_special.json` ou `dex_shiny.json`. Dentro de `menu.buttons.close`, adicione `command`:

```json
"close": {
  "slot": 28,
  "item": "minecraft:barrier",
  "name": "<red>Voltar ao menu",
  "lore": [],
  "command": "/menu"
}
```

Pode usar `/pokedex`, `/dex shiny` ou outro comando registrado, com argumentos. A barra inicial é opcional. Sem `command` ou com `""`, continua apenas fechando. Cada Dex configura seu destino independentemente. Aparência/nome/slot continuam editáveis. Arquivos existentes não são sobrescritos: acrescente o campo manualmente e use `/dex admin reload` (LuckPerms `shirokaidex.admin.reload`) ou reinicie.

O clique fecha a tela e executa como o jogador, respeitando suas permissões; comando ausente/negado recebe o erro normal. Não é console, não concede OP e não expande placeholders no comando. Aceita até 256 caracteres; controles de linha, tipos inválidos e comandos nos outros botões são rejeitados no reload atômico. Wiki/rankings preservados.

Esta versão deriva da tag v0.7.0-dev.1 (`dea5e5e`); a alteração anterior de home.close_command na alpha.8 não é usada pelos menus diretos.

## Cabeça pessoal e wiki da coleção (dev.3)

Nas três Dex ativas, a cabeça de `menu.buttons.info` (slot10) recebe o perfil completo do jogador que abriu a tela, incluindo UUID e propriedades da skin da sessão. Quando o item configurado é diferente de player_head, ele é preservado. Não usa a cabeça de um jogador fixo nem busca skins de forma síncrona. A textura disponível depende do perfil fornecido pelo servidor; login offline sem skin não fornece uma textura autenticada.

O botão `menu.buttons.wiki` (slot19, abaixo da cabeça) abre a wiki da mesma coleção: Nacional → Nacional, Special → Special, Shiny → Shiny. É um callback interno, independente de outro provedor registrar `/wiki`; mantém a validação de permissões da coleção. Fechar/voltar no slot28 continua independente.

```json
"wiki": {
  "slot": 19,
  "item": "minecraft:book",
  "name": "<aqua>Wiki %dex_type%",
  "lore": ["<gray>Clique para consultar esta Pokédex"]
}
```

Item/nome/lore/slot continuam editáveis em cada JSON. Arquivos antigos sem wiki recebem o botão padrão na leitura, sem sobrescrever o arquivo. Se slot19 já estiver ocupado, usa o primeiro slot livre; pode adicionar wiki explicitamente para escolher a posição. Slots em conflito continuam rejeitados. Forms permanece tela futura.

Teste dev.3: duas texturas públicas Mojang distintas preservadas com assinatura exata no item recebido por dois perfis descartáveis, nas três Dex. Fixtures existem somente no servidor de teste; o produto não fixa skins e usa o perfil do jogador. Renderização gráfica não testada.

## Compatibilidade da pesquisa com Shirokai Core/Tab (dev.4)

A captura da próxima mensagem da pesquisa roda na fase shirokaidex:wiki_search, antes dos roteadores na fase padrão do Fabric. Isso impede que o Core encaminhe o nome pesquisado ao chat local antes da Dex. Sem pesquisa pendente, o evento segue normalmente ao Core. Não intercepta pacotes nem pula assinatura/validação do Minecraft. /wiki nacional zorua continua disponível como consulta direta. Correção restrita à pesquisa; layout, skin, atalhos e botão de comando preservados.

## Estilo de mensagens no chat (dev.5)

Padrão: **✦ SHIROKAI** branco/negrito, ` » ` e texto cinza normal. Exemplo: `✦ SHIROKAI » Recompensa desbloqueada: ...`. Aplicado às notificações em CHAT (incluindo recompensa disponível/resgatada), respostas e erros de comandos e prompts/cancelamento da pesquisa. O texto em messages.json permanece editável; tags antigas de cor são normalizadas somente na saída ao chat. Não é necessário apagar/recriar JSONs.

Cores de itens, nomes/lore dos menus, actionbar e títulos continuam com sua apresentação própria. Chat de jogadores no Shirokai Core não passa por essa formatação. Eventos de clique existentes no texto são preservados.

## Atualização das recompensas do servidor (dev.6)

Pacote derivado dos arquivos enviados pelo responsável: ver SERVER_REWARDS_2026_11.md. O ZIP contém somente três menus, sem banco/backup/jogadores. Pokemon é aceito no reward_group, inclusive random_pool legendary. Chaves físicas são SAVED_ITEM autenticados no servidor do responsável; modelos ausentes rejeitam o reload inteiro. O Vale Shiny aparece apenas no visual por pedido explícito; VIP Top removido. As definições específicas não substituem silenciosamente os presets gerais do mod.
