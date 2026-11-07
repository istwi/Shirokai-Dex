# Wiki de espécies — alpha.3

A wiki consulta o registro real de espécies **implementadas** e a Pokédex oficial do próprio jogador. Está disponível em cada coleção pública ou autorizada, incluindo coleções JSON e a coleção Shiny cujo progresso de captura continua N/D.

## Abrir e pesquisar

```text
/dex national wiki
/dex legendary wiki
/dex shiny wiki
/dex <id_da_colecao> wiki
/dex national wiki pikachu
/dex <id_da_colecao> wiki namespace:especie
```

A página de uma coleção também possui o botão **Abrir wiki de espécies**. A pesquisa aceita até 128 caracteres e compara, sem diferenciar maiúsculas/minúsculas, trechos do nome canônico fornecido pelo Cobblemon e do identificador completo. Não consulta comandos externos nem interpreta formatação como ação. O botão de pesquisa mostra o comando no chat; esta primeira versão não possui campo de texto dentro da GUI.

## Navegar

A lista exibe até 18 espécies por página. Clique numa espécie para abrir seus detalhes. Os controles permitem:

| Controle | Ação |
|---|---|
| Página anterior/próxima | Navegar mantendo pesquisa, filtro e ordenação |
| Filtro | Alternar Todas → Vistas → Obtidas → Não vistas → Shiny vistas |
| Ordem | Alternar nome canônico e identificador; IDs desempatam nomes iguais |
| Pesquisar | Fechar a GUI e mostrar o comando de pesquisa |
| Limpar pesquisa e filtros | Voltar à lista completa da coleção |
| Voltar à coleção | Abrir a página de progresso da mesma coleção |
| Fechar | Fechar o inventário |

O título informa página/total de páginas e quantidade de resultados. Uma lista vazia mostra uma mensagem explícita. Alterar filtro/ordem reinicia a paginação; voltar dos detalhes preserva a consulta.

## Informações e critérios

Lista e detalhes mostram nome canônico, ID, estado **vista**, **obtida** e **shiny vista**. Os detalhes acrescentam formas elegíveis e seus tipos/estados oficiais. A forma padrão é incluída mesmo quando o registro não fornece formas alternativas. As formas são ordenadas e limitadas a 32 linhas por espécie, com aviso se houver mais.

Os critérios JSON da coleção são aplicados antes da pesquisa e dos filtros. Em coleções filtradas por forma/tipo/label, somente as formas elegíveis entram na exibição e nos estados agregados: obter uma forma não elegível não registra outra forma. Nacional sem filtro preserva o conhecimento agregado oficial. Espécies válidas mas não implementadas não entram na lista, seguindo o denominador do progresso.

**Obtida não significa captura selvagem legítima. Shiny vista não comprova captura shiny.** O estado de captura shiny verificada permanece indisponível em todas as páginas; nenhum histórico é reconstruído. A wiki apenas lê os dados oficiais, sem conceder entradas de Pokédex.

## Aparecimento e limites

Os detalhes exibem **Informação de aparecimento indisponível.** Não foi integrada uma fonte confiável para biomas, dimensões, horários ou condições de spawners. Isso significa informação não consultada, e não prova de que a espécie não tem spawn natural. Não há biomas ou horários inventados.

As mensagens da interface seguem o idioma global pt_br/en_us. Nomes de espécies, formas e tipos são os valores canônicos do registro nesta versão, não traduções personalizadas por cliente. Ícones são livros vanilla, sem modelo 3D nem novo mod cliente do Dex.

Toda navegação com dados reconsulta a definição/permissão atual. Revogação de acesso ou reload não permite usar um botão antigo para abrir espécie que deixou de ser elegível. O snapshot compartilhado com o progresso tem cache de até cinco segundos e invalidação pelos eventos oficiais já utilizados. Não há varredura global por tick, escrita de progresso ou consulta SQLite por página.

[Validação](validation/phase2-wiki.json): testes Java e protocolo em servidor de desenvolvimento. Aparência em cliente Minecraft real, carga, dados de spawn, consulta das formas além do limite e personalização completa dos layouts continuam pendentes. Esta é uma wiki inicial; não conclui toda a Fase 2.
