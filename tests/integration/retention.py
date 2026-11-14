#!/usr/bin/env python3
"""Seed a stopped disposable server, or verify its automatic retention through read-only SQLite."""
import argparse,json,sqlite3,time,uuid
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('action',choices=['seed','verify'])
parser.add_argument('root',type=Path)
args=parser.parse_args(); root=args.root.resolve()
assert str(root).startswith(('/tmp/','/workspace/'))
assert 'level-name=capture-retention-validation' in (root/'server.properties').read_text().splitlines()
file=root/'config/shirokaidex/data/shirokaidex.sqlite'
marker=root/'.retention-fixture.json'
if args.action=='seed':
    assert not marker.exists(),'Use a new disposable fixture; do not overwrite prior evidence'
    # Caller must stop the disposable server before seeding.
    with sqlite3.connect(file) as c:
        version=c.execute('pragma user_version').fetchone()[0]; assert version in (9,10)
        assert c.execute('select count(*) from capture_events').fetchone()[0]==0
        assert c.execute('select count(*) from capture_totals').fetchone()[0]==0
        player=str(uuid.uuid4()); now=int(time.time()*1000); old=now-91*86400000
        for i in range(5):
            ball=str(uuid.uuid4()); pokemon=str(uuid.uuid4()); key=ball+':'+pokemon
            verified=i!=3; at=old if i<4 else now
            c.execute("INSERT INTO capture_events(event_key,ball_uuid,pokemon_uuid,player_uuid,species,form,shiny,labels_json,types_json,categories_json,category_rules,captured_at,dimension,x,y,z,origin) VALUES(?,?,?,?,?,'normal',0,'[]','[]','[]',?,?,'minecraft:overworld',0,64,0,?)",(key,ball,pokemon,player,'cobblemon:pikachu','a'*64,at,'NATIVE_PLAYER_SPAWN' if verified else 'UNKNOWN'))
            if version==10:c.execute('insert into capture_receipts values(?,?,?,?)',(key,player,at,int(verified)))
        c.execute('insert into capture_totals values(?,4,0,0,0)',(player,))
        c.execute("insert into capture_counts values(?,'SPECIES','cobblemon:pikachu',4)",(player,))
    marker.write_text(json.dumps(dict(player=player,now=now,schema_before=version)))
    config=root/'config/shirokaidex/config.json'; values=json.loads(config.read_text()); values['capture_history']=dict(enabled=True,retention_days=90,cleanup=dict(enabled=True,batch_size=1000));config.write_text(json.dumps(values,ensure_ascii=False,indent=2)+'\n')
    print('Seeded five explicitly synthetic events in disposable world; three eligible for cleanup')
else:
    fixture=json.loads(marker.read_text()); player=fixture['player']
    with sqlite3.connect('file:'+str(file)+'?mode=ro',uri=True) as c:
        assert c.execute('pragma user_version').fetchone()[0]==10
        assert c.execute('select count(*) from capture_receipts where player_uuid=?',(player,)).fetchone()[0]==5
        assert c.execute('select count(*) from capture_events where player_uuid=?',(player,)).fetchone()[0]==2
        assert c.execute("select count(*) from capture_events where origin='UNKNOWN' and player_uuid=?",(player,)).fetchone()[0]==1
        assert c.execute('select total from capture_totals where player_uuid=?',(player,)).fetchone()[0]==4
        assert c.execute('select total from capture_counts where player_uuid=?',(player,)).fetchone()[0]==4
        assert c.execute('select count(*) from capture_receipts where player_uuid=? and verified=1',(player,)).fetchone()[0]==4
        assert c.execute('pragma integrity_check').fetchone()[0]=='ok'
    print('PASS: schema10, automatic expiration, unknown audit, five permanent receipts, four permanent captures, species counter, integrity')
