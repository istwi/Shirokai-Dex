# Perfis, privacidade e conquistas — desenvolvimento 0.6.0-dev.1

Schema14 ativo. `/dex profile [nome|UUID]` apresenta identidade persistida e dados disponíveis; nomes ambíguos exigem UUID. Perfis offline mostram a última observação oficial compatível com a versão atual do catálogo; indisponível não significa zero. O mod não altera a Pokédex oficial.

`/dex settings` abre preferências; `/dex settings privacy <chave> <true|false>` controla total_captures, shiny_captures, legendary_captures, species_captures, category_captures e personal_records. Espécies ficam privadas por padrão. O próprio jogador mantém acesso aos seus dados. Mudanças invalidam telas públicas abertas. `/dex settings rankings hidden|visible` controla presença pública nos rankings; ocultação não remove prêmios já atribuídos.

`/dex achievements [id]` lista conquistas declarativas, com requisitos de contagem, progresso, forma, outras conquistas e combinações AND/OR. Políticas permanentes ou recalculáveis preservam estado quando a fonte é desconhecida. Configuração rejeita ciclos e recompensas materiais/executáveis nas conquistas. Até três favoritos persistem. Medalhas secretas/privadas e estatísticas protegidas são filtradas em perfis públicos.

Rankings de conclusão usam comparação racional exata de registrado/total. Snapshots de catálogo incompatíveis são excluídos; não são criadas datas históricas fictícias. Preferências de notificação já existem, mas idioma individual e notificações gerais continuam pendentes.

Validação: 382 JUnit/build aprovados no conjunto. Consulte validation/development-0.6.0-dev.1.json para o estado nativo; não inferir certificação gráfica, de carga ou de integrações opcionais.
