#!/usr/bin/env python3
"""Run a complete packaged-server suite/restart with cached official assets; no environment publication."""
import argparse,hashlib,json,os,secrets,shutil,subprocess,tempfile,time,zipfile
from pathlib import Path
from urllib.parse import urlsplit
REPO=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base',type=Path,required=True,help='Prepared disposable server containing official server.jar, classpath.txt and dependency mods')
parser.add_argument('--suite',choices=['commands','recovery','captures'],required=True)
parser.add_argument('--root',type=Path,help='New /tmp directory; never overwrite an existing world')
args=parser.parse_args();base=args.base.resolve();root=args.root.resolve() if args.root else Path(tempfile.mkdtemp(prefix='shirokai-'+args.suite+'-'))
assert str(root).startswith('/tmp/') and str(base).startswith('/tmp/')
if args.root:root.mkdir(exist_ok=False)
assert hashlib.sha1((base/'server.jar').read_bytes()).hexdigest()=='59353fb40c36d304f2035d51e7d6e6baa98dc05c','Use original MC1.21.1 server.jar'
classpath=(base/'classpath.txt').read_text();assert 'build/classes' not in classpath and 'loom-cache' not in classpath
(root/'mods').mkdir()
for name in ['server.jar','classpath.txt']:shutil.copy2(base/name,root/name)
for jar in (base/'mods').glob('*.jar'):
 with zipfile.ZipFile(jar) as z:
  if 'fabric.mod.json' not in z.namelist():continue
  meta=json.loads(z.read('fabric.mod.json'))
  if meta['id']=='shirokaidex' or 'probe' in meta['id']:continue
 shutil.copy2(jar,root/'mods'/jar.name)
binaries=list((REPO/'build/server-mod').glob('*.jar'));assert len(binaries)==1
shutil.copy2(binaries[0],root/'mods'/binaries[0].name)
config=root/'config/shirokaidex';config.mkdir(parents=True);shutil.copy2(REPO/'tests/fixtures'/args.suite/'config.json',config/'config.json')
world={'commands':'command-reward-validation','recovery':'reward-recovery-validation','captures':'native-capture-validation'}[args.suite]
secret=root/'.rcon-test-secret';secret.write_text(secrets.token_hex(24));secret.chmod(0o600)
(root/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25571\nlevel-name='+world+'\nonline-mode=false\nenforce-secure-profile=false\nview-distance=2\nsimulation-distance=3\nmax-players=4\nspawn-protection=0\nenable-rcon=true\nrcon.port=25573\nrcon.password='+secret.read_text()+'\n')
# The responsible user accepted the EULA for disposable loopback tests in this session.
(root/'eula.txt').write_text('eula=true\n')
if args.suite=='captures':
    probe=REPO/'build/test-mods'/(binaries[0].stem+'-capture-probe.jar');assert probe.is_file()
    shutil.copy2(probe,root/'mods'/probe.name)
    shutil.copytree(REPO/'tests/fixtures/captures/datapacks',root/world/'datapacks')

java=Path(os.environ['JAVA_HOME'])/'bin/java';command=[str(java),'-Xms512M','-Xmx2300M','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
for scheme in ['http','https']:
 value=os.environ.get(scheme+'_proxy') or os.environ.get(scheme.upper()+'_PROXY')
 if value:
  proxy=urlsplit(value);assert proxy.scheme=='http' and proxy.hostname and not proxy.username and not proxy.password
  command += [f'-D{scheme}.proxyHost={proxy.hostname}',f'-D{scheme}.proxyPort={proxy.port or 80}']
command+=['-Dhttp.nonProxyHosts=localhost|127.*|[::1]','-Dfabric.gameJarPath=server.jar','-cp',classpath,'net.fabricmc.loader.impl.launch.knot.KnotServer','nogui']
process=None;output=None;starts=[]
def stop():
 global process,output
 if process and process.poll() is None:
  process.stdin.write('stop\n');process.stdin.flush()
  try:process.wait(timeout=45)
  except subprocess.TimeoutExpired:process.terminate();process.wait(timeout=10);raise RuntimeError('Server failed normal shutdown; inspect log')
 if output:output.close();output=None
 if process:
  assert process.returncode==0,'Test server failed'
  assert 'All dimensions are saved' in starts[-1].read_text(),'World save not confirmed'
  process=None

def start():
 global process,output
 path=root/f'console-{len(starts)+1}.log';starts.append(path);output=path.open('w');process=subprocess.Popen(command,cwd=root,stdin=subprocess.PIPE,stdout=output,stderr=subprocess.STDOUT,text=True)
 deadline=time.monotonic()+180
 while time.monotonic()<deadline:
  if process.poll() is not None:raise RuntimeError('Server exited before readiness: '+str(path))
  text=path.read_text()
  if 'Done (' in text and 'Shirokai Dex ready:' in text:print('READY',args.suite,'start',len(starts),flush=True);return
  time.sleep(.2)
 raise RuntimeError('Server readiness timed out: '+str(path))
def run(restart=False):
 env=os.environ.copy();env.update(MC_TEST_RUN=str(root),MC_TEST_PORT='25571',MC_TEST_RCON_PORT='25573',MC_TEST_RCON_PASSWORD_FILE=str(secret))
 if restart:env[{'commands':'MC_COMMAND_MODE','recovery':'MC_RECOVERY_MODE','captures':'MC_CAPTURE_MODE'}[args.suite]]='after-restart'
 subprocess.run(['npm','--prefix',str(REPO/'tests/integration'),'run','test:'+args.suite],env=env,check=True,cwd=REPO)
try:
 print('SERVER_ROOT',root,flush=True)
 if args.suite=='recovery':
  start();stop();subprocess.run(['python3',str(REPO/'tests/integration/seed-recovery.py'),str(root)],check=True)
 start();run();stop();start();run(True);stop()
 (root/'suite-result.json').write_text(json.dumps(dict(suite=args.suite,starts=len(starts),jar=binaries[0].name,sha256=hashlib.sha256(binaries[0].read_bytes()).hexdigest(),result='passed'),indent=2)+'\n')
 print('SUITE PASSED',args.suite,flush=True)
finally:
 try:stop()
 finally:
  secret.unlink(missing_ok=True)
  p=root/'server.properties';lines=[('enable-rcon=false' if line.startswith('enable-rcon=') else 'rcon.password=' if line.startswith('rcon.password=') else line) for line in p.read_text().splitlines()];p.write_text('\n'.join(lines)+'\n')
