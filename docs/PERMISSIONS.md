# Permissões

| Nó LuckPerms | Ação | Padrão |
|---|---|---|
| `shirokaidex.admin.reload` | `/dex admin reload` | Negado se não houver permissão positiva |

Exemplo de concessão por um administrador LuckPerms:

```text
/lp group admin permission set shirokaidex.admin.reload true
```

O Dex usa usuário carregado e dados de permissão em cache/contextos do LuckPerms. Não usa nível OP como substituto quando LuckPerms está ausente. API opcional isolada em classe própria; a ausência do mod foi testada em servidor real e não impede a inicialização.

Grant/revoke global, reload positivo/negativo e botão antigo após revogação foram testados com LuckPerms Fabric 5.4.140. A validação complementar em servidor de desenvolvimento confirmou contexto por dimensão: `world=overworld` permite a coleção/reload, a ida ao Nether nega e oculta a coleção, e o retorno restaura o acesso. Revogação contextual também bloqueia botão já aberto. [Evidência](validation/phase2-contexts.json). `/dex` e `/dex status` são públicos; coleções JSON já têm permissões opcionais. Condições avançadas de botões continuam pendentes.

Exemplo de permissão válida somente no Overworld nessa distribuição Fabric:

```text
/lp user <uuid> permission set shirokaidex.collection.dimension true world=overworld
```

O valor de contexto observado foi `overworld`, não `minecraft:overworld`. Confira o contexto ativo com `lp user <uuid> info` no console antes de configurar outros mundos. A configuração do nó deve corresponder ao campo `permission` da coleção.

Cada coleção JSON pode definir `permission`; vazio é público, demais nós exigem LuckPerms positivo e são revalidados a cada acesso. Sem provedor, coleções restritas são negadas e ocultadas. [Detalhes](COLLECTIONS.md).

Placeholders básicos e suas prévias usam a permissão atual da coleção para o jogador do contexto; acesso negado não revela nem o total. A API retorna `N/D` para essa consulta; a prévia de comando informa negação. Não existe fallback OP. [Sintaxe e segurança](PLACEHOLDERS.md).

## Recompensas locais

Prévia/resgate herdam a permissão atual da coleção. `shirokaidex.admin.rewards` autoriza revisão/cancelamento/reconhecimento manual de evidência, sem exigir também reload. Complete não reenvia itens. Os comandos são exclusivos de jogador autorizado via LuckPerms. Desbloqueio persistente não concede permissão.

## Itens salvos

`shirokaidex.admin.items`: salvar/listar/excluir e confirmar concessões de itens salvos. Independente de reload e revisão de recompensas; permissão revalidada nos callbacks. Resgates comuns de SAVED_ITEM continuam herdando a permissão da coleção. [Guia](SAVED_ITEMS.md).

`shirokaidex.admin.captures` — consultar histórico detalhado de capturas de qualquer UUID; somente jogador autorizado, sem fallback OP/console.
