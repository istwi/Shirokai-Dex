# Coleções configuráveis — 0.2.0-alpha.2

Adicione o campo opcional `collections` ao mesmo `config/shirokaidex/config.json`. Arquivos antigos sem esse campo continuam usando Nacional, Shiny e Lendária, sem reescrita automática. Se o campo estiver presente, ele define a lista completa (1–64 coleções, IDs únicos); as três coleções iniciais também podem ser personalizadas ou removidas.

Exemplo completo:

```json
{
  "schema_version": 1,
  "language": "pt_br",
  "menu_title": "✦ Shirokai Dex",
  "collections": [
    { "id": "national" },
    { "id": "shiny" },
    { "id": "legendary" },
    {
      "id": "electric",
      "name": "Pokédex Elétrica",
      "description": "Espécies elétricas obtidas oficialmente.",
      "icon": "minecraft:lightning_rod",
      "progress_mode": "OWNED",
      "permission": "",
      "criteria": { "types": ["electric"] }
    },
    {
      "id": "kanto_pair",
      "name": "Dupla de Kanto",
      "icon": "minecraft:book",
      "progress_mode": "SEEN",
      "criteria": {
        "include_species": ["cobblemon:pikachu", "cobblemon:eevee"],
        "exclude_species": [],
        "labels": [],
        "types": [],
        "forms": []
      }
    }
  ]
}
```

Use somente IDs presentes no registro real do seu servidor. O exemplo usa espécies vanilla Cobblemon, mas IDs de datapacks funcionam com seu namespace próprio.

## Campos

| Campo | Regra |
|---|---|
| `id` | Obrigatório, único, até 48 caracteres; `[a-z][a-z0-9_]*` |
| `name` | Obrigatório para coleções novas, até 64 caracteres; nas três iniciais pode ser omitido para tradução padrão |
| `description` | Opcional, até 512 caracteres; texto literal, sem ações/execução de comandos |
| `icon` | ID de item registrado; padrão `minecraft:book` nas novas; air não é aceito |
| `progress_mode` | Padrão OWNED nas novas; vide modos abaixo |
| `permission` | Vazio/omitido = público; caso contrário, nó LuckPerms minúsculo até 128 caracteres |
| `criteria` | Opcional; filtros de espécie, label, tipo e forma |

IDs de comandos reservados (`status`, `admin`, `rewards`, `top`, `history`, `profile`, `settings`, `achievements`, `language`, `help`) não podem ser usados. Campos desconhecidos, tipos incorretos, IDs/campos/valores duplicados, referências inexistentes, texto de controle e UTF-8 inválido são rejeitados. Arquivo limitado a 64 KiB; até 2048 entradas por lista de critério, sujeito ao limite do arquivo.

## Modos e semântica

| Modo | Fonte |
|---|---|
| `OWNED` | Espécies obtidas segundo a Pokédex oficial; não comprova captura selvagem |
| `SEEN` | Espécies vistas segundo os registros oficiais |
| `SHINY_SEEN` | Estados shiny vistos nos registros oficiais das formas elegíveis |
| `SHINY_CAPTURED` | Mantém N/D até existir fonte verificável de capturas shiny; não inventa histórico |

O modo é mostrado na lore e no status. `SHINY_SEEN` é uma escolha explícita; não muda silenciosamente o padrão da coleção Shiny, que continua SHINY_CAPTURED e agora usa o ledger verificado na alpha.5; N/D somente sem snapshot confirmado. Formas registradas como unidade de contagem ainda não estão implementadas: nesta etapa cada espécie conta uma vez.

## Critérios

- `include_species`: whitelist quando não vazia. Ausente/vazia permite todas as espécies implementadas, sujeitas aos demais filtros.
- `exclude_species`: exclusão tem prioridade, inclusive sobre a whitelist.
- `labels`, `types`, `forms`: OR dentro de cada lista e AND entre listas. Label/tipo/forma precisam coincidir **na mesma forma**, sem misturar propriedades de formas diferentes.
- Filtros de forma restringem também a prova de registro: obter uma forma comum não registra outra forma elegível ainda não obtida. Sem filtro de forma, preserva-se o agregado oficial da espécie, inclusive conhecimento de formas antigas.
- Valores de labels/tipos/formas devem ser minúsculos e conhecidos no catálogo capturado na inicialização. Espécies e itens exigem IDs completos com namespace. Não há fallback silencioso para forma/item padrão quando a referência está errada.

As coleções são filtros de progresso atual, não um histórico de categorias competitivas. Não há alteração de estatísticas permanentes nem de classificações passadas, que ainda pertencem às próximas fases.

Uma espécie conhecida, mas não implementada, pode ser uma referência válida e ainda produzir total zero/N/D. Uma referência que desapareceu do registro deve ser corrigida no JSON: ela é um erro explícito, não é ignorada. Datapacks de espécies exigem reinício conforme o contrato do Cobblemon 1.8.3.

## Menus, comandos e autorização

`/dex <id>` abre a página e `/dex <id> status` mostra quantidade/total e %. Os comandos usam as definições ativas e não precisam ser registrados novamente para cada coleção. `/dex status` continua sendo o resumo oficial geral, independente de filtros aplicados a uma coleção personalizada.

O seletor pagina 18 coleções por página, na ordem do JSON; as primeiras três posições mantêm o layout anterior quando a configuração é padrão. Setas navegam páginas; o retorno de uma coleção abre o seletor interno. Personalização de slots/ações e integração com `/menu` externo ainda pertencem à fase de menus avançados.

Coleções com permissão são ocultadas no seletor e nas sugestões dinâmicas sem autorização. Cada abertura/clique revalida a definição e a permissão atual, inclusive após revogação com GUI antiga aberta. Sem LuckPerms, coleções públicas continuam disponíveis e coleções restritas são negadas, sem fallback OP. O usuário só consulta seus próprios dados.

## Reload e recuperação

`/dex admin reload` exige `shirokaidex.admin.reload`. Configuração global e coleções são lidas, validadas estruturalmente e contra um snapshot imutável do registro, fora da thread principal. Só após validação e gravação atômica do backup a candidata inteira é publicada. Se algo falhar, título, idioma, coleções e backup anteriores são preservados.

Um reload válido invalida os snapshots de progresso e atualiza a árvore de comandos dos jogadores online. Isso não recarrega o registro de espécies do Cobblemon. No startup, o backup também deve passar pela validação semântica antes de ser usado; arquivo inválido original permanece para diagnóstico.

Wiki, busca, ranking, recompensas, categorias históricas, modelos complexos de condições e placeholders de terceiros ainda não estão concluídos.

## Wiki da coleção (alpha.3)

`/dex <id> wiki [pesquisa]` e o botão na página da coleção abrem a wiki usando os mesmos critérios de espécies/formas/tipos/labels e a mesma permissão. Espécies não implementadas permanecem fora da lista. Navegação e detalhes revalidam configuração e autorização inclusive após reload/revogação. [Pesquisa, controles e limites](WIKI.md).

Na alpha.5, seletores de tipo/label são filtros temporários da wiki e precisam combinar na mesma forma já aceita pelos critérios JSON. A navegação de formas usa páginas sem corte em 32 linhas; não altera o progresso/denominador da coleção nem sua configuração.

## Categorias reutilizáveis (alpha.7)

O campo opcional `category` referencia uma regra em `categories` no mesmo config.json. Critérios locais e categoria combinam na mesma forma. Veja [schema e limites](CATEGORIES.md). Permissões continuam definidas por coleção.
