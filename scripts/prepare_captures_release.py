#!/usr/bin/env python3
"""Package the exact authorized source; preserve native and CI binary identities separately."""
from pathlib import Path
import argparse,hashlib,json,shutil,subprocess,xml.etree.ElementTree as E,zipfile,os
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();source=a.source.resolve();output=a.output.resolve();output.mkdir(parents=True,exist_ok=False)
expected='78dfe72f8cef55f20c3645dc132ea6c3d97ad6f0';actual=subprocess.check_output(['git','rev-parse','HEAD'],cwd=source,text=True).strip();assert actual==expected
version='0.4.0-alpha.1+mc1.21.1-cobblemon1.8.3';jar=source/'build/server-mod'/('shirokai-dex-'+version+'.jar');sha=lambda f:hashlib.sha256(f.read_bytes()).hexdigest()
units=[E.parse(f).getroot() for f in (source/'build/test-results/test').glob('TEST-*.xml')];counts={k:sum(int(x.attrib.get(k,0)) for x in units) for k in ['tests','failures','errors','skipped']};assert counts==dict(tests=304,failures=0,errors=0,skipped=0)
with zipfile.ZipFile(jar) as z:
 metadata=json.loads(z.read('fabric.mod.json'));assert metadata['version']==version;assert not any('/probe/' in n for n in z.namelist())
 for entry in metadata['entrypoints']['main']:assert entry.replace('.','/')+'.class' in z.namelist()
report=source/'docs/validation/phase4-captures.json';native=json.loads(report.read_text());assert native['build']['junit']==counts and native['protocol_checks']==18
shutil.copy2(jar,output/jar.name);shutil.copy2(report,output/report.name)
provenance=dict(tag='v0.4.0-alpha.1',source_commit=actual,version=version,builder='GitHub Actions' if os.environ.get('GITHUB_ACTIONS') else 'Local preparation',workflow_run=os.environ.get('GITHUB_RUN_ID'),jar=jar.name,sha256=sha(jar),junit=counts,native_validation_report=report.name,native_tested_binary_sha256=native['build']['sha256'],native_binary_equals_release_binary=(sha(jar)==native['build']['sha256']),native_scope='Native capture/recovery tests were run locally against the documented implementation; CI repeats Java/build/package checks, not the game-server suite.',source_tree_clean=not bool(subprocess.check_output(['git','status','--porcelain'],cwd=source,text=True).strip()))
(output/'BUILD_PROVENANCE.json').write_text(json.dumps(provenance,ensure_ascii=False,indent=2)+'\n')
files=sorted(f for f in output.iterdir() if f.is_file());(output/'SHA256SUMS.txt').write_text(''.join(sha(f)+'  '+f.name+'\n' for f in files))
print('Prepared four release assets; source and 304 JUnit verified; native hash retained separately.')
