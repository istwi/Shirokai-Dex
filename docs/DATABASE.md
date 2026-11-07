# SQLite — migração 1

Caminho atual: `config/shirokaidex/data/shirokaidex.sqlite`, relativo ao servidor. O diretório de configuração deve permanecer persistente. Não coloque este arquivo no Git.

Uma conexão é confinada a um executor próprio. Acesso SQL usa statements parametrizados; nenhum objeto Minecraft é enviado ao worker. WAL, foreign keys, busy timeout de 5 segundos e synchronous FULL são habilitados.

A migração `V001__foundation.sql` cria:

- `schema_migrations(version, applied_at)`;
- `players(uuid, last_known_name, first_join_at, last_seen_at)`;
- `player_settings(player_uuid, language)` (estrutura preparada; preferências individuais ainda não expostas).

O `PRAGMA user_version` é `1`. Migração e histórico são gravados na mesma transação; falha reverte alterações. Versões futuras são recusadas sem downgrade. Login atualiza nome/último acesso preservando UUID e primeiro acesso. A Pokédex oficial não é copiada para esse banco.

Testes verificaram 100 atualizações enfileiradas para o mesmo UUID, reabertura, rollback em conflito de esquema, recusa de versão futura e rejeição após fechamento. No servidor real, reinício preservou os registros e uma única migração, com `integrity_check=ok`.

Para backup manual seguro nesta fase, desligue o servidor normalmente e copie todo o diretório `data` (incluindo arquivos WAL/SHM se existirem). Não copie somente o `.sqlite` de um servidor ativo. Comandos de backup/restauração e demais tabelas do prompt ainda não foram implementados.
