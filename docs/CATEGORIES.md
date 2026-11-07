# Categorias nomeadas

Alpha.7: `categories` opcional no mesmo `config.json` das coleções. Configurações antigas continuam válidas. Regras descrevem espécies/formas do registro atual; não são histórico de capturas.

```json
{
  "schema_version": 1,
  "language": "pt_br",
  "menu_title": "Shirokai Dex",
  "categories": [
    {"id": "rare", "criteria": {"labels": ["legendary", "mythical"]}},
    {"id": "water", "criteria": {"types": ["water"]}},
    {"id": "rare_water", "all_of": ["rare", "water"]}
  ],
  "collections": [
    {"id": "aquatic", "name": "Raras aquáticas", "category": "rare_water"}
  ]
}
```

Cada categoria aceita `id`, `criteria`, `all_of`, `any_of`, `none_of`. IDs: `[a-z][a-z0-9_]{0,47}`. Até 64 categorias e 64 referências por operador; permanece o limite global de configuração de 64 KiB. Categorias vazias selecionam todas as formas conhecidas; lista `categories` vazia é válida quando nenhuma coleção a referencia.

`criteria` reutiliza `include_species`, `exclude_species`, `labels`, `types`, `forms`. Exclusão prevalece sobre inclusão. Valores de um campo combinam por OU; campos combinam por E. `all_of` exige todas as referências, `any_of` exige pelo menos uma quando preenchido, `none_of` exclui qualquer referência. Critério local e operadores combinam por E. Todas as condições precisam valer **na mesma forma**, inclusive em referências encadeadas. Referências futuras são permitidas; ciclos, ausentes, duplicados e campos desconhecidos são rejeitados antes de aplicar.

A coleção pode combinar `category` com seus próprios `criteria`; ambos devem valer na mesma forma. Progresso e wiki usam essa seleção, contando cada espécie uma única vez. Conhecimento é lido das formas elegíveis, sem transferir obtenção da forma padrão às variantes. Sem categoria, a semântica anterior permanece. Categorias não inferem shiny capturado: esse modo continua N/D sem fonte verificada.

Labels são classificações reais do Cobblemon; IDs administrativos de categorias pertencem a outro espaço de nomes. O seletor de categorias já existente na wiki continua mostrando **labels**. Para navegar uma categoria administrativa, abra uma coleção que a referencie. Paradox e outras classificações sem dado verificável exigem espécies/configuração explícitas; não existe lista presumida embutida.

Acesso é definido por `permission` **na coleção**, não na categoria: categorias são regras reutilizáveis, sem ACL própria. Duas coleções podem compartilhar regras e ter permissões diferentes. Cada comando/clique continua verificando a permissão atual. Não use categorias para guardar informação secreta.

O reload valida grafo, referências Cobblemon e coleções como um único candidato; falha preserva snapshot ativo e `config.last-good.json`. Sucesso invalida caches e menus reavaliam regras atuais. Backup de recuperação também exige validação semântica. Nenhuma classificação histórica, estatística ou resultado competitivo é recalculado. Versionamento persistido foi acrescentado no trabalho local abaixo; recálculo auditado e SNAPSHOT/LIVE de competições continuam pendentes.

## Versões persistidas das regras (trabalho local)

`CategoryRules` produz JSON canônico formato 1 e SHA-256, ordenando IDs/conjuntos; ordem dos campos/elementos e omissão de listas vazias não alteram a identidade. SQLite migração 2 preserva cada conteúdo em `category_rule_versions`, sem sobrescrever versões anteriores. Startup e reload validam referências reais e persistem as regras antes de publicar a configuração; falha de persistência mantém configuração ativa e backup. A gravação e o arquivo de backup não são uma transação distribuída: uma regra validada pode ficar registrada mesmo se a publicação posterior falhar. `first_observed_at` significa primeira observação validada, não ativação ou captura.

A identidade cobre definições explícitas de categorias; não é uma versão dos datapacks, da configuração inteira ou dos resultados de classificação. Mudanças no Cobblemon podem alterar classificações sem mudar esse hash. Competições futuras precisarão congelar também o contexto aplicável. Nenhuma captura passada, ativação histórica ou resultado é inferido desta tabela.
