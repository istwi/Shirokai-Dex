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

## Categorias nomeadas

Campo opcional `categories` e referência `collections[].category`: [guia](CATEGORIES.md). Aplicação transacional junto das coleções, sem alteração de arquivos antigos.

## Recompensas locais (Fase 3)

`rewards` é opcional e vazio por padrão. [Schema, permissões e limites](REWARDS.md). Somente itens simples estão habilitados; campos/tipos desconhecidos rejeitam o candidato integral.

## Itens com componentes e referências salvas

`items[].type` aceita ITEM/CUSTOM_ITEM/SAVED_ITEM, ou é inferido quando omitido. Referências são resolvidas no candidato; backup expandido independente até 512 KiB, mantendo configuração original em 64 KiB. [Schema](SAVED_ITEMS.md).

## Pokémon e central visual

`rewards[].pokemon`: [schema/limites de atributos e armazenamento](POKEMON_REWARDS.md). `home`: rows/palette/slots validados da central, [exemplos](LAYOUT.md). São campos opcionais; configurações antigas permanecem válidas. Nenhuma configuração de ambiente foi publicada.
