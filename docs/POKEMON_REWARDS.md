# Recompensas de Pokémon — escopo validado

Configuração usa `pokemon` ao lado de `items`, podendo combinar os dois. Até seis Pokémon e 32 etapas totais por marco. Exemplo:

```json
{
  "id": "partner",
  "collection": "national",
  "minimum_registered": 1,
  "pokemon": [{
    "type": "POKEMON",
    "species": "cobblemon:bulbasaur",
    "level": 12,
    "shiny": true,
    "nature": "cobblemon:modest",
    "ability": "overgrow",
    "gender": "MALE",
    "pokeball": "cobblemon:great_ball",
    "moves": ["tackle", "growl", "vinewhip"],
    "held_item": "minecraft:apple",
    "ivs": {"hp": 31, "attack": 20},
    "evs": {"hp": 6, "special_attack": 252, "speed": 252}
  }]
}
```

Somente species é obrigatório; type pode ser omitido. IDs namespaced para espécie/nature/bola/item; ability e moves usam nomes Showdown do registro real. Form usa o nome real, em minúsculas (ex.: alola). Gênero: MALE/FEMALE/GENDERLESS. Stats: hp, attack, defence, special_attack, special_defence, speed.

Nível explícito 1..100 e não acima do máximo do servidor; padrão min(5, máximo). Shiny padrão false. Nature, gênero, habilidade, IVs e golpes omitidos usam padrões válidos do Cobblemon; EVs omitidos são zero. IVs 0..31; EVs 0..252 por stat, soma até 510; até quatro golpes distintos. Espécie precisa estar implementada; forma/aspectos, gênero, pool de habilidade e movimentos legais são conferidos no registro. Strings inválidas nunca são passadas ao parser permissivo de propriedades para cair em valores aleatórios.

## Plano e entrega

Prévia não cria nem concede Pokémon. Na confirmação, o mod prepara cada Pokémon fora de qualquer store, define o treinador original destinatário e congela o NBT completo (UUID e valores sorteados) no plano antes da intenção/efeito. PENDING pode reutilizar exatamente o mesmo payload, sem novo sorteio. Operações PROCESSING/NEEDS_REVIEW/COMPLETED não disparam outra entrega.

Capacidade é a soma real de slots livres da equipe e caixas normais do PC; não usa o backup/overflow ilimitado. Revalida ao executar cada etapa. PlayerPartyStore.add é chamado na thread do servidor, exige retorno positivo e presença do UUID no destino. Isso emite obtenção, não captura legítima: não incrementa estatísticas de captura nem transforma shiny recebido em captura shiny verificada.

## Persistência e limites atuais

Suporte validado: stores nativos PlayerPartyStore/PCStore, UUID do jogador, backend NBT e intervalo automático de salvamento 1..60 segundos. JSON/MongoDB/subclasses e provedores alternativos não estão certificados; configurações reconhecidamente incompatíveis são recusadas antes de efeitos. Não altera configuração do Cobblemon para habilitar uma entrega.

Salvamento é do próprio Cobblemon. Leitura assíncrona dos arquivos NBT nativos identifica o UUID e verifica os atributos congelados na thread do servidor antes de reconhecer a etapa. Confirmação aguarda salvamento automático, com limite de 3×intervalo+15 segundos, máximo 180; por isso o jogador deve aguardar antes de mover/usar o Pokémon. Erro/parcial de arquivo é tentado novamente apenas na **leitura**, nunca na entrega. Payload até 64 KiB por Pokémon e plano até 256 KiB; leitura NBT até 32 MiB.

Se a gravação não puder ser confirmada, a operação fica em NEEDS_REVIEW, preservando payload/etapas. Shutdown interrompe verificações e impede reenvio automático; recuperação também bloqueia PROCESSING. SQLite e arquivos Cobblemon não são transação atômica; não há garantia de fsync/perda de energia. A administração deve conferir UUID, lojas e evidências antes de resolver a operação.

[Validação](validation/phase3-pokemon.json): atributos completos, forma Alola, equipe cheia/PC, ambos lotados, falha real de gravação assíncrona e reinício. Backend/formas/datapacks adicionais, cliente visual/carga e JAR instalado fora do Loom continuam pendentes.
