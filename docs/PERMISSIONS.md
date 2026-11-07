# Permissões

| Nó LuckPerms | Ação | Padrão |
|---|---|---|
| `shirokaidex.admin.reload` | `/dex admin reload` | Negado se não houver permissão positiva |

Exemplo de concessão por um administrador LuckPerms:

```text
/lp group admin permission set shirokaidex.admin.reload true
```

O Dex usa usuário carregado e dados de permissão em cache/contextos do LuckPerms. Não usa nível OP como substituto quando LuckPerms está ausente. API opcional isolada em classe própria; a ausência do mod foi testada em servidor real e não impede a inicialização.

O fluxo positivo com LuckPerms instalado e contextos específicos ainda requer teste de integração. `/dex` e `/dex status` são públicos nesta fundação; permissões de coleções e botões avançados serão adicionadas na fase correspondente.
