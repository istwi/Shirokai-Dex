#!/usr/bin/env python3
"""Seed one reserved protocol-test player in a stopped disposable schema10 ranking server."""
import sqlite3,uuid,hashlib,json,time,sys
from pathlib import Path
root=Path(sys.argv[1]).resolve()
assert str(root).startswith(('/tmp/','/workspace/'))
assert 'level-name=capture-ranking-validation' in (root/'server.properties').read_text().splitlines()
marker=root/'.ranking-fixture.json';assert not marker.exists()
player=str(uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:DexRanking').digest(),version=3));now=int(time.time()*1000)
with sqlite3.connect(root/'config/shirokaidex/data/shirokaidex.sqlite') as c:
    assert c.execute('pragma user_version').fetchone()[0]==10
    assert c.execute('select count(*) from capture_totals where player_uuid=?',(player,)).fetchone()[0]==0
    for i in range(3):
        ball=str(uuid.uuid4());pokemon=str(uuid.uuid4());key=ball+':'+pokemon;at=now-1000*(3-i)
        c.execute("INSERT INTO capture_events(event_key,ball_uuid,pokemon_uuid,player_uuid,species,form,shiny,labels_json,types_json,categories_json,category_rules,captured_at,dimension,x,y,z,origin) VALUES(?,?,?,?,?,'normal',1,'[]','[]','[]',?,?,'minecraft:overworld',0,64,0,'NATIVE_PLAYER_SPAWN')",(key,ball,pokemon,player,'cobblemon:pikachu','a'*64,at))
        c.execute('insert into capture_receipts values(?,?,?,1)',(key,player,at))
    c.execute('insert into capture_totals values(?,3,3,0,0)',(player,));c.execute("insert into capture_counts values(?,'SPECIES','cobblemon:pikachu',3)",(player,))
    c.execute("insert into captured_shiny_forms values(?,'cobblemon:pikachu','normal',?)",(player,now-3000))
marker.write_text(json.dumps(dict(player=player,score=3,source='synthetic isolated fixture; not native gameplay')))
p=root/'config/shirokaidex/config.json';values=json.loads(p.read_text());values['rankings']=[dict(id='captures',name='Capturas',metric='TOTAL'),dict(id='shiny',name='Shiny',metric='SHINY'),dict(id='restricted',name='Restrito',metric='TOTAL',permission='shirokaidex.test.restricted')];p.write_text(json.dumps(values,ensure_ascii=False,indent=2)+'\n')
print('Seeded reserved DexRanking fixture; requires migration10 to11')
