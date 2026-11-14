#!/usr/bin/env python3
"""Convert only DexRewards configuration to typed rewards/menu JSON. Never reads or imports players/."""
import argparse,json,re,zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('zip',type=Path);p.add_argument('output',type=Path);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
def save(name,data):(a.output/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def button(slot,item,name,lore=None):return dict(slot=slot,item=item,name=name,lore=lore or [])
def menu(title,rows,buttons,content=None,rewards=None):return dict(title=title,rows=rows,background=dict(item='minecraft:gray_stained_glass_pane',name=' ',lore=[]),buttons=buttons,content_slots=content or [],reward_slots=rewards or [])
with zipfile.ZipFile(a.zip) as z:
 gui=json.loads(z.read('DexRewards/reward_gui.json'))
 for old,key,command,label in [('national','national','nacional','Nacional'),('legendary','special','special','Special'),('shiny','shiny','shiny','Shiny')]:
  data=json.loads(z.read('DexRewards/dex_types/'+old+'.json'));groups=[]
  for name,g in data['reward_groups'].items():
   out=dict(id=f'dex_{key}_{name}',percent=g['required_percent'],name=g['display_name'],icon=g['icon_item']['id'],lore=g['icon_lore'],items=[],currencies=[],permissions=[])
   for reward in g['rewards'].values():
    if reward['type']=='item':
     assert reward['min_count']==reward['max_count'];out['items'].append(dict(id=reward['item']['id'],count=reward['min_count']))
    elif reward['type']=='command':
     for cmd in reward['commands']:
      money=re.fullmatch(r'eco deposit ([0-9]+) (impactor:(?:dollars|tokens)) %player%',cmd)
      tag=re.fullmatch(r'lp user %player% permission set (shirokai\.dex\.[a-z0-9_.-]+) true',cmd)
      if money:out['currencies'].append(dict(currency=money[2],amount=int(money[1])))
      elif tag:out['permissions'].append(tag[1])
      else:raise ValueError('Unsupported command configuration; no command executed')
    else:raise ValueError('Unsupported reward type')
   groups.append(out)
  layout=gui['gui_layout'];slots=lambda symbol:[r*9+c for r,row in enumerate(layout) for c,ch in enumerate(row) if ch==symbol]
  buttons={}
  for role,field,symbol,itemid in [('info','player_info_item','I','minecraft:player_head'),('close','close_item','C','minecraft:barrier'),('previous','previous_item','P','minecraft:arrow'),('next','next_item','N','minecraft:arrow')]:
   view=gui[field];buttons[role]=button(slots(symbol)[0],view.get('item',{}).get('id',itemid),view['item_name'],view.get('item_lore',[]))
  view=menu(gui['title'],gui['rows'],buttons,rewards=slots('G'));view['reward_states']={}
  for source,target in [('claimed','claimed'),('claimable','available'),('locked','locked')]:
   v=gui['group_item'][source];view['reward_states'][target]=dict(item=v.get('item',{}).get('id',''),name=v['item_name'],lore=v['item_lore'],glint=v['show_enchantment_glint'])
  collection=dict(id=key,name=label+' Dex',icon='minecraft:book' if key=='national' else 'minecraft:nether_star' if key=='special' else 'minecraft:amethyst_shard',progress_mode='SHINY_CAPTURED' if key=='shiny' else 'OWNED')
  if key!='shiny':collection['category']='ordinary_species' if key=='national' else 'special_species'
  save('dex_'+command+'.json',dict(collection=collection,menu=view,reward_groups=groups))
 for cmd,key in [('nacional','national'),('special','special'),('shiny','shiny')]:
  buttons={'filter':button(48,'minecraft:clock','<yellow>Filtro: %filter%',['<gray>Esquerdo: avançar filtro.','<gray>Direito: voltar filtro.','<aqua>Selecionado: %filter%']), 'search':button(49,'minecraft:writable_book','<aqua>Pesquisar Pokémon',['<gray>Pesquisa: %query%','<yellow>Clique e escreva o nome no chat.']), 'clear':button(50,'minecraft:hopper','<red>Limpar filtros',['<gray>Remove a pesquisa e os filtros.']), 'previous':button(45,'minecraft:arrow','<aqua>← Página anterior'), 'next':button(53,'minecraft:arrow','<aqua>Próxima página →')}
  view=menu('<gradient:#00C6FF:#0072FF>✦ Wiki '+cmd+'</gradient>',6,buttons,content=[r*9+c for r in range(1,5) for c in range(1,8)])
  view['entry']=dict(item='',name='<aqua>%pokemon_name%',lore=['<dark_gray>%pokemon_id%','<gray>Visto: <white>%seen%','<gray>Obtido: <white>%owned%','<gray>Capturado: <white>%captured%','','<yellow>Onde encontrar','%spawns%'])
  view['filter_names']=dict(ALL='Todos',SEEN='Vistos',OWNED='Obtidos',UNSEEN='Não vistos',SHINY_SEEN='Shiny vistos')
  save('wiki_'+cmd+'.json',view)
 future=menu('<gradient:#00C6FF:#0072FF>✦ Forms Dex</gradient>',3,{'future':button(13,'minecraft:dragon_egg','<yellow>Forms Dex',['<gray>Disponível em uma atualização futura.'])})
 save('dex_forms.json',future);save('wiki_forms.json',future)
 for name,roles in [('rank_captura',[('total',11,'minecraft:poke_ball','Capturas gerais'),('shiny',13,'minecraft:amethyst_shard','Capturas shiny'),('legendary',15,'minecraft:nether_star','Capturas lendárias')]),('rank_dex',[('national',10,'minecraft:book','Nacional Dex'),('special',12,'minecraft:nether_star','Special Dex'),('shiny',14,'minecraft:amethyst_shard','Shiny Dex'),('forms',16,'minecraft:dragon_egg','Forms Dex')]),('rank_derrotados',[('defeats',13,'minecraft:iron_sword','Pokémon derrotados')])]:
  buttons={}
  for role,slot,icon,title in roles:
   if icon=='minecraft:poke_ball':icon='cobblemon:poke_ball'
   buttons[role]=button(slot,icon,'<gold>✦ '+title,['<gray>Disponível em uma atualização futura.'] if role=='forms' else ['<yellow>Top 10','%ranking%'])
  view=menu('<gradient:#00C6FF:#0072FF>✦ Rankings</gradient>',3,buttons)
  view['ranking_line']='<gray>%position%. <white>%player_name% <gray>— <aqua>%score%'
  save(name+'.json',view)
 save('messages.json',dict(search_prompt='<yellow>Escreva no chat o Pokémon que procura. Digite cancelar para sair.',search_cancelled='<gray>Pesquisa cancelada.',search_expired='<gray>O prazo da pesquisa terminou.',loading='<gray>Carregando dados…',no_data='<gray>Nenhum jogador classificado.',claimed='<green>Recompensa resgatada.',unavailable='<red>Não foi possível abrir este menu. Consulte os logs.'))
