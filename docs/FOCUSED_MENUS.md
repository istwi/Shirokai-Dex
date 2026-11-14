# Menus públicos reduzidos

> Direção atual 0.7.0-dev.1: consulte [Menus diretos](DIRECT_MENUS.md). As seções abaixo são históricas; o hub e os antigos comandos de navegação/prévia foram retirados. `/dex` sozinho não abre tela.

Pedido vigente do responsável. Instalações novas recebem o preset `src/main/resources/config/focused.json`; configuração existente não é substituída. Um exemplo completo para instalação/revisão está em [focused-server.json](examples/focused-server.json). Faça backup de config.json e use o exemplo adaptado às suas recompensas; `/dex admin reload` exige a permissão administrativa existente. Dados do banco não são apagados ao mudar de modo.

`home.focused: true` ativa o hub com Dex, Rankings de captura e Eventos. Profile/settings/achievements/history/prizes/records ficam sem acesso público nesse modo; ferramentas administrativas continuam protegidas. `false` recupera a navegação clássica. Não remove os sistemas ou históricos existentes.

Nacional exclui formas classificadas como legendary/mythical/ultra_beast; Special inclui esses grupos. As categorias são JSON, portanto espécies adicionais com essas labels também são avaliadas. Shiny usa captura shiny verificada, sem promover trocas/comandos a captura. Forms é apenas ícone futuro e não executa ação.

Cada Dex apresenta Wiki e seis marcos. Os percentuais são `home.milestones`, seis inteiros distintos em ordem crescente de1 a100. Padrão10/25/50/75/90/100. Uma recompensa normal com collection correspondente e minimum_percent exatamente igual ao marco aparece nesse espaço; sem configuração fica bloqueada. O preset contém apenas o exemplo1 papel aos10% da Nacional. Configure livremente itens/Pokémon/moedas/comandos dentro dos contratos já suportados; não há prêmios ocultos nos outros espaços. Clique elegível aciona o journal existente diretamente, com revalidação e sem segundo resgate.

Rankings mostram Top10 na lore de cada item: três coleções ativas + Forms futuro; capturas geral/shiny/lendárias; derrotas verificadas. Respeitam privacidade/ocultação dos rankings, invalidando telas públicas na mudança. Evento fechado tem ícone sem ação. Evento abertoTOTAL inscreve ao clicar e apresenta período no fuso configurado, score próprio e Top10; só capturas verificadas após a inscrição pontuam.

Wiki usa `PokemonItem.from` da API pública Cobblemon1.8.3 e aspectos oficiais das formas, em vez de livros. Cliente precisa do Cobblemon e seus recursos para renderizar modelos. A lore diferencia obtenção oficial e captura registrada; em Shiny considera apenas capturas shiny. Resumos de spawn são parciais e não garantem aparição. Clique esquerdo no filtro avança, direito volta; a lore e a consulta são atualizadas. Roda do mouse sobre lore não chega ao servidor no cliente padrão.

Derrotas: schema16 guarda recibo battle_uuid+pokemon_uuid e total por jogador. Apenas combate PvE com um jogador e inimigo selvagem de origem nativa comprovada ao início. PvP, NPCs, atribuição ambígua e origem desconhecida não pontuam. Faint repetido do mesmo Pokémon na mesma batalha não duplica; não há backfill de combates anteriores à instalação. Ranking Top10 e exclusão global persistem após reinício. Validação:402 JUnit e9+1 checks nativos aprovados, com reinício e SQLite íntegro; [evidência](validation/focused-menus.json). Não certificar capacidade/carga nem renderização gráfica manual a partir de testes de protocolo.


Exemplo opcional de evento recorrente (não é ativado pelo preset):

```json
{"competitions":[{"id":"weekly_captures","name":"Evento de capturas","cadence":"WEEKLY","timezone":"America/Sao_Paulo","metric":"TOTAL"}]}
```

Para duração específica, use CUSTOM com start/end ISO8601. Regras de edições já criadas são preservadas: não é um botão de pausa ou cancelamento retroativo. Top10 de conclusão usa observações atuais; jogadores com snapshots antigos incompatíveis voltam a aparecer após nova observação ao entrar no servidor.
