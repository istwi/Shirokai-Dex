#!/usr/bin/env python3
"""Synthetic operations only, in a stopped disposable world whose schema was created by the real mod."""
import argparse,hashlib,json,socket,sqlite3,uuid,datetime
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('server_directory',type=Path);a=p.parse_args();root=a.server_directory.resolve();repo=Path(__file__).resolve().parents[2]
assert str(root).startswith('/tmp/')
props=dict(line.split('=',1) for line in (root/'server.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
assert props['server-ip']=='127.0.0.1' and props['level-name']=='reward-recovery-validation'
try:
 with socket.create_connection(('127.0.0.1',int(props['server-port'])),timeout=1):raise RuntimeError('Stop the test server before seeding')
except (ConnectionRefusedError,TimeoutError):pass
fixture=json.loads((repo/'tests/fixtures/recovery/config.json').read_text());assert json.loads((root/'config/shirokaidex/config.json').read_text())==fixture
player=str(uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:DexRecovery').digest(),version=3));now=datetime.datetime.now(datetime.timezone.utc).isoformat()
with sqlite3.connect('file:'+str(root/'config/shirokaidex/data/shirokaidex.sqlite')+'?mode=rw',uri=True) as db:
 assert db.execute('pragma user_version').fetchone()[0]==7
 assert db.execute('select count(*) from reward_operations where player_uuid=?',(player,)).fetchone()[0]==0
 for reward in fixture['rewards']:
  items=[dict(type='CUSTOM_ITEM' if 'payload_snbt' in item else 'ITEM',id=item['id'],count=item['count'],**({'payload_snbt':item['payload_snbt']} if 'payload_snbt' in item else {})) for item in reward['items']]
  if reward['id']=='changed':items[0]['count']=2
  plan=dict(format_version=1,id=reward['id'],collection=reward['collection'],minimum_registered=1,items=items);operation=str(uuid.uuid4());state='NEEDS_REVIEW' if reward['id'] in ['review_empty','uncertain','prefix'] else 'PENDING'
  db.execute('insert into reward_operations values(?,?,?,?,?,?,?)',(operation,player,reward['id'],json.dumps(plan,ensure_ascii=False,separators=(',',':')),state,now,now))
  if reward['id'] in ['uncertain','prefix']:db.execute('insert into reward_steps values(?,?,?,?)',(operation,0,'INTENT' if reward['id']=='uncertain' else 'COMPLETED','Synthetic isolated journal fixture, no real effect'))
  db.execute('insert into reward_audit(operation_id,action,actor,reason,recorded_at) values(?,?,?,?,?)',(operation,'TEST_FIXTURE','fixture','Synthetic states only; no actual delivery',now))
print('Recovery synthetic fixtures seeded without overwriting operations.')
