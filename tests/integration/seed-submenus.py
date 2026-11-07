#!/usr/bin/env python3
"""Seed synthetic journal states in a STOPPED, disposable /tmp layout server only."""
import argparse
import hashlib
import json
from pathlib import Path
import socket
import sqlite3
import uuid

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('server_directory', type=Path)
parser.add_argument('--player', default='DexNavLayout')
parser.add_argument('--fixture', choices=['submenus','menu-appearance'], default='submenus')
args = parser.parse_args()
root = args.server_directory.resolve()
if not str(root).startswith('/tmp/') or not args.player.startswith('DexNav') or len(args.player) > 16:
    parser.error('Only /tmp servers and reserved DexNav test players are allowed')
config_file = root / 'config/shirokaidex/config.json'
fixture = Path(__file__).resolve().parents[1] / ('fixtures/' + args.fixture + '/config.json')
if json.loads(config_file.read_text()) != json.loads(fixture.read_text()):
    parser.error('Install the exact submenu fixture before seeding')
properties = dict(line.split('=', 1) for line in (root / 'server.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
if properties.get('server-ip') != '127.0.0.1':
    parser.error('Test server must bind loopback only')
try:
    with socket.create_connection(('127.0.0.1', int(properties['server-port'])), timeout=1):
        parser.error('Stop the test server before seeding')
except (ConnectionRefusedError, TimeoutError):
    pass
player = str(uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:' + args.player).encode()).digest(), version=3))
db_file = root / 'config/shirokaidex/data/shirokaidex.sqlite'
with sqlite3.connect('file:' + str(db_file) + '?mode=rw', uri=True) as db:
    if db.execute('SELECT count(*) FROM reward_operations WHERE player_uuid=?', (player,)).fetchone()[0]:
        parser.error('Use a fresh reserved test player; existing operations will not be overwritten')
    for reward, state in [('completed','COMPLETED'),('review','NEEDS_REVIEW'),('cancelled','CANCELLED'),('pending','PENDING'),('changed','PENDING'),('recovered','PROCESSING')]:
        plan = dict(format_version=1, id=reward, collection='national', minimum_registered=1,
                    items=[dict(type='ITEM', id='minecraft:paper', count=2 if reward == 'changed' else 1)])
        operation = str(uuid.uuid4())
        now = '2026-11-07T00:00:00Z'
        db.execute('INSERT INTO reward_operations VALUES(?,?,?,?,?,?,?)',
                   (operation,player,reward,json.dumps(plan,separators=(',',':')),state,now,now))
        if state == 'COMPLETED':
            db.execute('INSERT INTO reward_steps VALUES(?,?,?,?)', (operation,0,'COMPLETED','Synthetic isolated layout fixture, no real delivery'))
        db.execute('INSERT INTO reward_audit(operation_id,action,actor,reason,recorded_at) VALUES(?,?,?,?,?)',
                   (operation,'LAYOUT_FIXTURE','test','Synthetic isolated layout state, not production history',now))
print('Seeded six synthetic operations for', args.player)
