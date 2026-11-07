# Placeholders básicos — alpha.4

A integração opcional usa **Text Placeholder API 2.4.2+1.21**, de Patbox. Instale essa API separadamente no servidor; ela não é incluída no JAR do Dex. Sem a API, coleções, wiki, SQLite e demais funções existentes continuam disponíveis. Não é necessário configurar credenciais.

## Sintaxe e valores

A sintaxe validada pelo parser real desta versão é **`%namespace:placeholder argumento%`**, com espaço antes do ID da coleção:

| Placeholder | Resultado |
|---|---|
| `%shirokaidex:percent national%` | Percentual com duas casas e `%`, truncado; por exemplo `33,33%` |
| `%shirokaidex:registered national%` | Quantidade registrada conforme o modo da coleção |
| `%shirokaidex:seen national%` | Quantidade vista conforme a coleção/modo |
| `%shirokaidex:total national%` | Total dinâmico de espécies elegíveis implementadas |
| `%shirokaidex:remaining national%` | Total menos quantidade registrada, quando esta é conhecida |

Substitua `national` por `legendary`, `shiny` ou pelo ID de uma coleção JSON. Omitir o argumento usa `national`, por exemplo `%shirokaidex:total%`. IDs usam as mesmas letras minúsculas, números e `_` das coleções; argumentos adicionais ou inválidos retornam `N/D`. A sintaxe com `/national` dentro do nome não foi registrada como alias.

A apresentação segue o idioma global do Dex: `33,33%` em pt_br e `33.33%` em en_us. `percent` é texto formatado, não uma promessa de valor numérico sem símbolo.

**Registrada/obtida não significa captura selvagem legítima.** Em OWNED, `registered` usa obtenção oficial; em SEEN, espécies vistas; em SHINY_SEEN, shiny visto. `seen` segue o contador da coleção: nos modos shiny, usa shiny visto. Os filtros de espécies/formas/tipos/labels são os mesmos do progresso e da wiki.

## Dados indisponíveis e permissões

`N/D` nunca é convertido silenciosamente em zero:

- Em SHINY_CAPTURED, `percent`, `registered` e `remaining` são `N/D`, pois não existe histórico verificado de capturas shiny. `total` e `seen` podem ser conhecidos sem comprovar captura.
- Uma coleção vazia conhecida tem `total=0`, `registered=0`, `remaining=0` e percentual `N/D`; isso não é 100% de conclusão.
- Coleção inexistente, acesso negado, ausência de jogador, consulta fora da thread do servidor, inicialização/parada ou snapshot ainda não preparado retornam `N/D`.
- Um placeholder restrito consulta a permissão atual do **jogador do contexto**, mesmo que já exista snapshot. Não existe consulta por nome/UUID de outro jogador nem fallback OP. O consumidor externo ainda precisa aplicar sua própria política de visibilidade ao exibir os dados desse contexto.

As expansões leem somente snapshots imutáveis. Login, mudança oficial de Pokédex e reload agendam atualização no servidor; logout/parada removem dados. No primeiro acesso após invalidação pode haver um intervalo curto de `N/D` até o preparo. A expansão não executa comandos, não escreve na Pokédex e não consulta SQLite.

## Conferir no jogo

```text
/dex national placeholder percent
/dex national placeholder total
/dex <id> placeholder registered
```

Esses comandos mostram uma expansão pelo parser real, com o contexto do próprio jogador e as mesmas permissões. Não abrem um interpretador de comandos nem aceitam templates arbitrários. Sem a API, mostram que a prévia está indisponível; métrica inválida é recusada.

## Conflitos e limites

O Dex registra somente as cinco chaves listadas. Se uma já existir, preserva o provedor e registra aviso nos logs; a prévia do Dex retorna `N/D` para uma chave que ele não controla. Se outro mod substituir um handler depois, a limpeza do Dex não remove esse novo provedor.

O alias `dexrewards:player_dex_percent` **não é registrado nesta entrega**. Foi testado que um provedor legado existente permanece intacto. Compatibilidade positiva com esse alias, sintaxes alternativas e TAB/bridge Fabric continuam pendentes. Instalar a Text Placeholder API não comprova integração com qualquer scoreboard específico.

Capturas permanentes, recompensas, rankings, competições, medalhas e dados ainda não implementados não recebem placeholders fictícios nem zeros inventados. A expansão segura em consumidores assíncronos depende de um contexto na thread do servidor; chamadas fora dela retornam `N/D`, sem bloquear aguardando o servidor.

## Desenvolvimento e verificação

Artefato oficial: [release 2.4.2+1.21](https://github.com/Patbox/TextPlaceholderAPI/releases/tag/2.4.2%2B1.21), licença LGPLv3, revisão auditada `2427627df3e73ec00a2d3623c7e60e1d076bdb67`. O build usa um repositório Ivy restrito a esse módulo e aponta diretamente ao artefato oficial do GitHub, pois o Maven Nucleoid não está acessível neste ambiente. Versão e SHA-256 do runtime testado estão em `tests/fixtures/placeholders/runtime.json`.

O preparo consome apenas a fila de jogadores solicitados/invalidados, até quatro por fim de tick; não faz varredura global de jogadores. Uma leitura após cinco segundos agenda atualização e pode retornar o snapshot anterior da mesma revisão até o preparo. Mudança de configuração nunca reutiliza dados da revisão antiga. Falhas de preparo mantêm `N/D` e aguardam cinco segundos antes de nova tentativa causada por leitura.

[Reprodução e testes](TESTING.md), [evidência](validation/phase2-placeholders.json). Esta entrega não certifica carga, TAB, cliente visual ou o JAR instalado fora do Loom. A fixture de probe é exclusiva do servidor de desenvolvimento e não deve ser instalada em produção.
