#!/usr/bin/env python3
"""Package the reviewed dev.6 server menus beside the built JAR, without player data."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import zipfile

REPO = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output', type=Path, default=REPO / 'build/server-mod')
args = parser.parse_args()
out = args.output.resolve()
out.mkdir(parents=True, exist_ok=True)
menus = sorted((REPO / 'tests/fixtures/server-rewards/menus').glob('dex_*.json'))
assert {p.name for p in menus} == {'dex_nacional.json', 'dex_special.json', 'dex_shiny.json'}
entries = {}
summary = []
for path in menus:
    data = json.loads(path.read_text())
    assert len(data['reward_groups']) == 6
    assert data['menu']['buttons']['close']['command'] == 'pokedex'
    assert 'VIP Top' not in path.read_text()
    entries['shirokaidex/menus/' + path.name] = path.read_bytes()
    summary.append('=== ' + data['collection']['name'] + ' ===')
    for reward in data['reward_groups']:
        summary.append(f"{reward['percent']}% — {reward['name']}")
        summary.extend(line for line in reward['lore'] if '•' in line or 'Tag SPECIAL' in line)
summary = '\n'.join(re.sub(r'<[^>]+>', '', line) for line in summary) + '\n'
guide = (REPO / 'docs/SERVER_REWARDS_2026_11.md').read_bytes()
entries['INSTALACAO-RECOMPENSAS.md'] = guide
entries['RESUMO-RECOMPENSAS.txt'] = summary.encode('utf-8')
with zipfile.ZipFile(out / 'shirokaidex-recompensas-atualizadas.zip', 'w') as archive:
    for name, data in sorted(entries.items()):
        info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o100644 << 16
        archive.writestr(info, data)
(out / 'INSTALACAO-RECOMPENSAS.md').write_bytes(guide)
jar, = out.glob('*.jar')
validation = json.loads((REPO / 'docs/validation/server-rewards-dev6.json').read_text())
provenance = {
    'source_commit': os.environ.get('GITHUB_SHA'),
    'build_run': os.environ.get('GITHUB_RUN_ID'),
    'jar': jar.name,
    'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
    'native_tested_sha256': validation['sha256'],
    'native_binary_match': hashlib.sha256(jar.read_bytes()).hexdigest() == validation['sha256'],
    'validation': '427 JUnit; native tests/limits recorded in repository docs/validation/server-rewards-dev6.json',
    'installation': 'Save seven authentic Crates key templates on the target server before applying the menus.',
}
(out / 'BUILD_PROVENANCE.json').write_text(json.dumps(provenance, indent=2) + '\n')
files = [jar, out / 'shirokaidex-recompensas-atualizadas.zip', out / 'INSTALACAO-RECOMPENSAS.md', out / 'BUILD_PROVENANCE.json']
(out / 'SHA256SUMS').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.name + '\n' for p in sorted(files)))
print('Packaged JAR, server config ZIP, installation guide, provenance and checksums.')
