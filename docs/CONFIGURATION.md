# Configuração da fundação

O servidor cria `config/shirokaidex/config.json`:

```json
{
  "schema_version": 1,
  "language": "pt_br",
  "menu_title": "✦ Shirokai Dex"
}
```

São exigidos os três campos básicos; o campo opcional `collections` adiciona [coleções JSON](COLLECTIONS.md). Versão suportada: `1`; idiomas: `pt_br` e `en_us`; título não vazio, até 128 caracteres e sem controles. Tipos incorretos, campos desconhecidos, campos duplicados, JSON malformado e arquivos acima de 64 KiB são rejeitados.

Reload cria e valida uma candidata fora da thread do servidor, grava atomicamente `config.last-good.json` e só então publica a configuração imutável. Se falhar, mantém a configuração em uso e o backup anterior. No início, um arquivo inválido pode ser recuperado pelo backup validado; o arquivo inválido original permanece no lugar e a recuperação é registrada em WARN. Sem configuração válida nem backup válido, o Dex fica indisponível, sem encerrar o servidor.

Execute `/dex admin reload` com a permissão LuckPerms correspondente. Sem LuckPerms, a ação administrativa é negada inclusive para operadores. Reload positivo/negativo com LuckPerms Fabric 5.4.140 foi testado no JAR instalado, incluindo rollback de coleções e recuperação após reinício.

Esta fase implementa filtros iniciais de coleções JSON; layouts completos de menus, recompensas e competições ainda estão pendentes. Os arquivos de tradução atuais são recursos separados `localization/pt_br.json` e `localization/en_us.json`; tradução por cliente e substituições externas pertencem às próximas fases.
