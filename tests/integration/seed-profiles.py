#!/usr/bin/env python3
"""Explicit synthetic permanent counters only; never writes official Cobblemon Pokedex data."""
import sys,json,sqlite3,uuid,hashlib,time,datetime,socket
from pathlib import Path
root=Path(sys.argv[1]).resolve();assert str(root).startswith('/tmp/');props=dict(line.split('=',1) for line in (root/'server.properties').read_text().splitlines() if '=' in line and not line.startswith('#'));assert props['level-name']=='profile-medal-validation' and props['server-ip']=='127.0.0.1'
try:
 with socket.create_connection(('127.0.0.1',int(props['server-port'])),timeout=1):raise RuntimeError('Stop the disposable server before seeding')
except (ConnectionRefusedError,TimeoutError):pass
marker=root/'.profiles-fixture.json';assert not marker.exists();player=str(uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:DexMedal').digest(),version=3));now=int(time.time()*1000);at=datetime.datetime.now(datetime.timezone.utc).isoformat()
with sqlite3.connect(root/'config/shirokaidex/data/shirokaidex.sqlite') as c:
 assert c.execute('pragma user_version').fetchone()[0]==14;assert c.execute('select count(*) from capture_totals').fetchone()[0]==0
 c.execute('insert into players values(?,?,?,?)',(player,'DexMedal',at,at))
 for i,(species,shiny,legendary,category) in enumerate([('cobblemon:pikachu',1,0,'past_electric'),('cobblemon:mewtwo',0,1,'past_legendary')]):
  ball=str(uuid.uuid4());pokemon=str(uuid.uuid4());key=ball+':'+pokemon;captured=now-1000*(2-i)
  c.execute('insert into capture_receipts values(?,?,?,1)',(key,player,captured))
  c.execute("insert into capture_events(event_key,ball_uuid,pokemon_uuid,player_uuid,species,form,shiny,labels_json,types_json,categories_json,category_rules,captured_at,dimension,x,y,z,origin) values(?,?,?,?,?,'normal',?,?,?, ?,? ,?,'minecraft:overworld',0,64,0,'NATIVE_PLAYER_SPAWN')",(key,ball,pokemon,player,species,shiny,json.dumps(['legendary'] if legendary else []),json.dumps(['psychic'] if legendary else ['electric']),json.dumps([category]),'a'*64,captured))
  c.execute('insert into captured_forms values(?,?,?, ?,?)',(player,species,'normal',captured,shiny))
  if shiny:c.execute('insert into captured_shiny_forms values(?,?,?,?)',(player,species,'normal',captured))
  c.execute("insert into capture_counts values(?,'SPECIES',?,1)",(player,species));c.execute("insert into capture_counts values(?,'CATEGORY',?,1)",(player,category))
  c.execute("insert into capture_ranking_scores values(?,'SPECIES',?,1,?)",(player,species,captured));c.execute("insert into capture_ranking_scores values(?,'CATEGORY',?,1,?)",(player,category,captured))
 for metric,count in [('TOTAL',2),('SHINY',1),('LEGENDARY',1)]:c.execute('insert into capture_ranking_scores values(?,?,?, ?,?)',(player,metric,'',count,now-1000))
 c.execute('insert into capture_totals values(?,2,1,1,0)',(player,));c.execute('insert into player_preferences values(?,NULL,0)',(player,))
marker.write_text(json.dumps(dict(player=player,source='synthetic isolated permanent counters; official Pokedex untouched')))
print('Seeded synthetic permanent statistics without official Pokedex writes')
