#!/usr/bin/env python3
"""Seed explicitly synthetic standings in a stopped, fresh disposable prize server."""
import sys,json,sqlite3,uuid,hashlib,time
from pathlib import Path
root=Path(sys.argv[1]).resolve();assert str(root).startswith('/tmp/');assert 'level-name=prize-award-validation' in (root/'server.properties').read_text().splitlines()
marker=root/'.prize-fixture.json';assert not marker.exists();player=str(uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:DexPrize').digest(),version=3));now=int(time.time()*1000)
def compact(value):return json.dumps(value,separators=(',',':'),ensure_ascii=False)
def iso(ms):
 import datetime
 return datetime.datetime.fromtimestamp(ms/1000,datetime.timezone.utc).isoformat().replace('+00:00','Z')
def plan(identifier):return dict(id=identifier,collection='national',minimumRegistered=1,items=[dict(id='minecraft:diamond',count=2,data='')],manual=False,pokemon=[],currencies=[],commands=[])
def snapshot(identifier):return compact(dict(format_version=1,plan=plan(identifier)))
rules=compact(dict(format_version=1,categories=[]));fingerprint=hashlib.sha256(rules.encode()).hexdigest();expired=None
with sqlite3.connect(root/'config/shirokaidex/data/shirokaidex.sqlite') as c:
 assert c.execute('pragma user_version').fetchone()[0]>=13
 assert len(c.execute('pragma table_info(competition_editions)').fetchall())==11
 assert c.execute('select count(*) from competition_editions').fetchone()[0]==0
 assert c.execute('select count(*) from reward_operations').fetchone()[0]==0
 for old in [True,False]:
  name='expired' if old else 'native_prize';start=now-(10*86400000 if old else 600000);end=now-(9*86400000 if old else 1000);edition=name+':'+str(start)
  definition=dict(id=name,name=name,cadence='CUSTOM',timezone='UTC',start=iso(start),end=iso(end),metric='TOTAL',key='',category_update_mode='SNAPSHOT',visibility='PUBLIC',permission='',result_review=dict(enabled=False,duration_hours=0,auto_finalize=True),prizes=dict(enabled=True,minimum_participants=1,minimum_captures=1,redistribute=False,expiry_days=7 if old else 0,bands=[dict(maximum_position=1,reward='winner',minimum_score=1)]))
  templates=compact(dict(winner=snapshot('winner')))
  c.execute("insert into competition_editions values(?,?,?,?,?,?,?,?,?,?,?)",(edition,name,start,end,end,'FINAL' if old else 'ACTIVE',compact(definition),rules,fingerprint,end if old else None,templates))
  c.execute('insert into competition_entries values(?,?,?,NULL,1,?)',(edition,player,start,start+1000))
  ball=str(uuid.uuid4());pokemon=str(uuid.uuid4());key=ball+':'+pokemon
  c.execute('insert into capture_receipts values(?,?,?,1)',(key,player,start+1000))
  c.execute("insert into competition_points values(?,?,?,1,'COUNTED',?,?)",(edition,key,player,fingerprint,start+1000))
  if old:
   c.execute('insert into competition_results values(?,?,1,1,?,1)',(edition,player,start+1000));award='cp_'+hashlib.sha256((edition+':'+player).encode()).hexdigest()[:40];expired=award
   c.execute('insert into competition_awards values(?,?,?,?,?,?,?,?,?)',(award,edition,player,1,1,'winner',snapshot(award),end, end+7*86400000))
  else:current=definition
 c.execute('insert into capture_totals values(?,2,0,0,0)',(player,));c.execute("insert into capture_counts values(?,'SPECIES','cobblemon:pikachu',2)",(player,))
p=root/'config/shirokaidex/config.json';config=json.loads(p.read_text());config['competitions']=[current];p.write_text(json.dumps(config,indent=2)+'\n')
marker.write_text(json.dumps(dict(player=player,expired=expired,source='synthetic isolated fixture, not gameplay capture proof')))
print('Seeded expired award and overdue active synthetic edition for automatic finalization')
