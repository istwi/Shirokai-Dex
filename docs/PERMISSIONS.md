# Permissões

| Nó LuckPerms | Ação | Padrão |
|---|---|---|
| `shirokaidex.admin.reload` | `/dex admin reload` | Negado se não houver permissão positiva |

Exemplo de concessão por um administrador LuckPerms:

```text
/lp group admin permission set shirokaidex.admin.reload true
```

O Dex usa usuário carregado e dados de permissão em cache/contextos do LuckPerms. Não usa nível OP como substituto quando LuckPerms está ausente. API opcional isolada em classe própria; a ausência do mod foi testada em servidor real e não impede a inicialização.

Grant/revoke global, reload positivo/negativo e botão antigo após revogação foram testados com LuckPerms Fabric 5.4.140. Contextos específicos ainda requerem testes. `/dex` e `/dex status` são públicos nesta fundação; permissões de coleções e botões avançados serão adicionadas na fase correspondente.

Cada coleção JSON pode definir `permission`; vazio é público, demais nós exigem LuckPerms positivo e são revalidados a cada acesso. Sem provedor, coleções restritas são negadas e ocultadas. [Detalhes](COLLECTIONS.md).
