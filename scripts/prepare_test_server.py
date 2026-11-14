#!/usr/bin/env python3
"""Prepare a reusable packaged Fabric test base from verified cached artifacts and official Maven metadata."""
import argparse,hashlib,json,shutil,urllib.request,zipfile
from pathlib import Path
REPO=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('root',type=Path);p.add_argument('--cache',type=Path,default=REPO.parent/'.cache/shirokai-gradle');args=p.parse_args()
root=args.root.resolve();assert str(root).startswith('/tmp/');root.mkdir(exist_ok=False);(root/'mods').mkdir();(root/'libraries').mkdir()
cache=args.cache.resolve();modules=cache/'caches/modules-2/files-2.1'
def download(url):
    with urllib.request.urlopen(url,timeout=60) as response:return response.read()
def cached(group,artifact,version):
    files=list((modules/group/artifact/version).glob('*/'+artifact+'-'+version+'.jar'));assert len(files)==1,(group,artifact,version)
    file=files[0];assert hashlib.sha1(file.read_bytes()).hexdigest()==file.parent.name.zfill(40),'Cached artifact checksum mismatch';return file
classpath=[]
def library(group,artifact,version,expected=None):
    path=group.replace('.','/')+'/'+artifact+'/'+version+'/'+artifact+'-'+version+'.jar';url='https://maven.fabricmc.net/'+path
    if expected is None:expected=download(url+'.sha256').decode().strip().split()[0]
    candidates=list((modules/group/artifact/version).glob('*/'+artifact+'-'+version+'.jar'))
    data=candidates[0].read_bytes() if len(candidates)==1 else download(url)
    assert hashlib.sha256(data).hexdigest()==expected,'Official library checksum mismatch: '+artifact
    file=root/'libraries'/path;file.parent.mkdir(parents=True,exist_ok=True);file.write_bytes(data);classpath.append(str(file));return file
profile=json.loads(download('https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.17.2/fabric-loader-0.17.2.json'))
for entry in profile['libraries']['common']+profile['libraries']['server']:
    group,artifact,version=entry['name'].split(':');library(group,artifact,version,entry['sha256'])
library('net.fabricmc','fabric-loader','0.17.2')
# Intermediary mapping JAR is required only in the production server namespace.
url='https://maven.fabricmc.net/net/fabricmc/intermediary/1.21.1/intermediary-1.21.1-v2.jar';data=download(url);expected=download(url+'.sha256').decode().strip().split()[0];assert hashlib.sha256(data).hexdigest()==expected
file=root/'libraries/intermediary-1.21.1.jar';file.write_bytes(data);classpath.append(str(file))
server=cache/'caches/fabric-loom/1.21.1/minecraft-server.jar';assert hashlib.sha1(server.read_bytes()).hexdigest()=='59353fb40c36d304f2035d51e7d6e6baa98dc05c';shutil.copy2(server,root/'server.jar')
with zipfile.ZipFile(server) as archive:
    for line in archive.read('META-INF/libraries.list').decode().splitlines():
        digest,coordinate,path=line.split('\t');data=archive.read('META-INF/libraries/'+path);assert hashlib.sha256(data).hexdigest()==digest
        file=(root/'libraries'/path).resolve();assert file.is_relative_to(root/'libraries');file.parent.mkdir(parents=True,exist_ok=True);file.write_bytes(data);classpath.append(str(file))
for group,artifact,version in [('net.fabricmc.fabric-api','fabric-api','0.116.6+1.21.1'),('com.cobblemon','fabric','1.8.3+1.21.1'),('net.fabricmc','fabric-language-kotlin','1.13.6+kotlin.2.2.20')]:
    file=cached(group,artifact,version);shutil.copy2(file,root/'mods'/file.name)
luck=json.loads((REPO/'tests/fixtures/placeholders/runtime.json').read_text())
# Pinned provider is shared with the existing validated integration fixture.
provider=luck['luckperms'] if 'luckperms' in luck else None
if provider is None:
    for value in luck.values():
        if isinstance(value,dict) and 'LuckPerms' in value.get('source',''):provider=value
assert provider is not None
url=provider['source'];data=download(url);assert hashlib.sha512(data).hexdigest()==provider['sha512'];(root/'mods/LuckPerms-Fabric-5.4.140.jar').write_bytes(data)
classpath.append(str(root/'server.jar'));(root/'classpath.txt').write_text(':'.join(classpath))
print('Prepared verified packaged test base:',root)
