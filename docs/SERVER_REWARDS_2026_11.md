# Recompensas Dex — atualização do servidor

Requer o novo JAR dev.6: suporte a pokemon/random_pool nos menus diretos, além das mensagens de chat dev.5. Não apague o banco nem direct.last-good.json. O pacote contém apenas os três JSONs alterados. IDs de recompensa e nós de tags foram preservados; resgates anteriores não são liberados novamente. Pendências antigas/plano alterado precisam da revisão existente.

## Permissões do administrador

Se ainda faltarem, conceda somente ao administrador:

```text
/lp user SEUNICK permission set shirokaicrates.give true
/lp user SEUNICK permission set shirokaidex.admin.items true
/lp user SEUNICK permission set shirokaidex.admin.reload true
```

## Preparar as chaves autênticas (uma vez, no mesmo servidor/segredo do Crates)

Antes de copiar os novos menus, mantenha a configuração atual ativa. Como administrador com shirokaicrates.give e shirokaidex.admin.items, execute cada givekey, coloque só essa chave na mão principal e salve o modelo correspondente. Troque SEUNICK pelo seu nome; não conceda permissão administrativa aos jogadores premiados.

```text
/crates givekey SEUNICK caixa_epica 1
/dex admin item save dex_key_caixa_epica
```

```text
/crates givekey SEUNICK caixa_inicial_rara 1
/dex admin item save dex_key_caixa_inicial_rara
```

```text
/crates givekey SEUNICK caixa_lendaria 1
/dex admin item save dex_key_caixa_lendaria
```

```text
/crates givekey SEUNICK caixa_mitica 1
/dex admin item save dex_key_caixa_mitica
```

```text
/crates givekey SEUNICK caixa_paradoxos 1
/dex admin item save dex_key_caixa_paradoxos
```

```text
/crates givekey SEUNICK caixa_ultra_beasts 1
/dex admin item save dex_key_caixa_ultra_beasts
```

```text
/crates givekey SEUNICK shiny 1
/dex admin item save dex_key_shiny
```

Os nomes Caixa Básica/Epic/Paradoxo/Ultra Beast/Mítica/Lendária/Shiny permanecem no visual do TXT; Básica usa caixa_inicial_rara. A assinatura da chave é criada pelo próprio Crates. Nenhuma chave de outro servidor ou segredo acompanha este ZIP. Recompensas usam SAVED_ITEM e checkpoint nativo já suportados pela Dex; não executam givekey como console.

## Aplicar

1. Faça backup dos três JSONs e mantenha SQLite/dados existentes intactos.
2. Confira /dex admin item list para os sete modelos.
3. Com o novo JAR instalado, copie shirokaidex/menus deste ZIP para config/.
4. Use /dex admin reload (ou reinicie) e confira os três menus. Se faltar um modelo, o reload rejeita o conjunto em vez de conceder um pacote incompleto.

## Pedidos aplicados

- Todas as Dex ativas voltam por pokedex com cobblemon:eject_button e mantêm cabeça/wiki nas posições pedidas.
- 18 pacotes do TXT; percentuais 5/15/30/50/75/100 para Nacional/Special/Shiny.
- Permissões das tags preservadas, inclusive os antigos sufixos .001/.003/.010/.025 da Shiny.
- Vale Shiny: somente nome/quantidade no visual, sem item, comando, vale funcional ou entrega adicional.
- VIP Top removido.
- Shiny100%: um lendário implementado do pool nativo legendary, aleatório, shiny e seis IVs31. Sorteio congelado no plano antes da entrega; retomada não sorteia outro Pokémon. Nível não foi especificado: usa o padrão atual da Dex (5, limitado ao máximo do servidor).


## Validação

427 JUnit sem falhas/erros/skips; build, JAR e7 checks nativos aprovados, incluindo pacote final real, chaves autenticadas e preservação do mesmo Pokémon/plano após reinício.24 freezes/restores de lendários adicionais conferidos. Demais17 pacotes validados por parsing/quantidades, sem resgate nativo individual. Sem cliente gráfico/carga. O arquivo do servidor e seu banco original não foram alterados/importados.
