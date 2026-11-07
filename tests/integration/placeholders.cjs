// Actual Text Placeholder API expansion via public preview; probe mode exercises raw contexts/conflicts.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');
const PLAYER = process.env.MC_PLACEHOLDER_PLAYER || 'DexPapi';
assert(/^[A-Za-z0-9_]{1,15}$/.test(PLAYER), 'Use a disposable name of up to 15 characters');
const PROBE = process.env.MC_PLACEHOLDER_MODE === 'probe';
const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
const configFile = path.join(root,'config/shirokaidex/config.json');
const original = fs.readFileSync(configFile,'utf8');
assert.deepEqual(JSON.parse(original),JSON.parse(fs.readFileSync(path.join(__dirname,'../fixtures/collections/config.json'),'utf8')));
const results=[];
let bot,rcon,connectionError,lastSend=0;
function pass(name){console.log('PASS',name);results.push({name,status:'passed'});}
async function until(predicate,timeout=15000){
  const end=Date.now()+timeout;
  while(Date.now()<end){if(connectionError)throw connectionError;if(await predicate())return;await delay(50);}
  throw new Error('Timed out waiting for placeholder state');
}
async function login(name){
  connectionError=null;
  bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT||25571),username:name,auth:'offline',version:'1.21.1',hideErrors:true,
    customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
  const emit=bot._client.emit;
  bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
  let spawned=false;bot.once('spawn',()=>{spawned=true;});bot.on('error',e=>{connectionError=e;});bot.on('kicked',e=>{connectionError=new Error(JSON.stringify(e));});
  await until(()=>spawned,45000);await rcon.command(`deop ${name}`);
}
async function response(text,matches){
  await delay(Math.max(0,1100-(Date.now()-lastSend)));
  return new Promise((resolve,reject)=>{
    const timer=setTimeout(()=>{bot.removeListener('messagestr',listener);reject(new Error('Missing response: '+text));},15000);
    function listener(value){if(matches(value)){clearTimeout(timer);bot.removeListener('messagestr',listener);resolve(value);}}
    bot.on('messagestr',listener);lastSend=Date.now();bot.chat(text);
  });
}
async function preview(metric,id='national'){
  const prefix=`Placeholder ${metric} ${id}: `;
  return (await response(`/dex ${id} placeholder ${metric}`,text=>text.startsWith(prefix))).slice(prefix.length);
}
async function lp(text){
  const result=await rcon.command(`lp user ${bot.player.uuid} permission ${text}`);
  assert(!result.includes('Another command is being executed'),result);await delay(1200);
}
async function parse(text,target=PLAYER,mode='parse'){
  const value=await rcon.command(`dexprobe ${mode} ${target} ${text}`);
  assert(value.startsWith('PROBE '),value);return value.slice(6);
}
async function probe(){
  await until(async()=>(await parse('%shirokaidex:total%'))!=='N/D');
  const total=await parse('%shirokaidex:total national%');assert(Number(total)>0);
  assert.equal(await parse('%shirokaidex:total%'),total);
  assert.equal(await parse('%shirokaidex:total missing%'),'N/D');
  assert.equal(await parse('%shirokaidex:total national /op nobody%'),'N/D');
  pass('Real API parser accepts default/explicit collection syntax and rejects invalid or injected arguments');
  assert.equal(await parse('%shirokaidex:remaining national%'),'EXTERNAL');
  assert.equal(await parse('%dexrewards:player_dex_percent%'),'EXTERNAL');
  assert.equal(await preview('remaining'),'N/D');
  pass('Existing namespaced and legacy providers remain untouched; preview refuses an unowned handler');
  assert.equal(await parse('%shirokaidex:total national%','server'),'N/D');
  assert.equal(await parse('%shirokaidex:total national%',PLAYER,'async'),'N/D');
  pass('Missing-player and off-thread contexts return N/D without blocking or reading game state');
  assert.equal(await parse('%shirokaidex:total locked%'),'N/D');
  await lp('set shirokaidex.collection.locked true');assert.equal(await parse('%shirokaidex:total locked%'),'1');
  await lp('set shirokaidex.collection.locked false');assert.equal(await parse('%shirokaidex:total locked%'),'N/D');
  pass('Raw API expansions recheck LuckPerms on every request and do not expose restricted counts');
  assert.equal(await rcon.command('dexprobe replace'),'PROBE replaced');
  assert.equal(await parse('%shirokaidex:percent national%'),'REPLACED');
  assert.equal(await preview('percent'),'N/D');
  pass('A later provider replacement is respected; shutdown cleanup is verified separately from server logs');
}
async function normal(){
  const baseline=await response('/dex status',text=>text.includes('Pokédex oficial:'));
  assert(baseline.includes('0 espécies obtidas'),'Use a fresh disposable profile');
  const total=Number(baseline.match(/de (\d+) implementadas/)[1]);
  for(const [metric,expected] of Object.entries({percent:'0,00%',registered:'0',seen:'0',total:'2',remaining:'2'}))assert.equal(await preview(metric,'duo'),expected);
  assert.equal(Number(await preview('total')),total);
  pass('All five preview metrics expand through the real API using collection-aware counts and locale percent');
  for(const metric of ['percent','registered','remaining'])assert.equal(await preview(metric,'shiny'),'N/D');
  assert.equal(Number(await preview('total','shiny')),total);
  assert.equal(await preview('percent','future_species'),'N/D');assert.equal(await preview('registered','future_species'),'0');
  pass('Unknown shiny capture evidence and empty collections remain distinct from known zero');
  assert.equal(await response('/dex status',text=>text.includes('Pokédex oficial:')),baseline);
  await response('/dex national placeholder invalid',text=>text.includes('Métrica inválida'));
  pass('Expansions do not modify official progress and unknown metrics are rejected');
  assert.equal(await preview('registered','duo'),'0');
  const changed=Date.now();await rcon.command(`givepokemonother ${PLAYER} dex_test:ordinary level=5`);
  await until(async()=>(await preview('registered','duo'))==='1',4000);assert(Date.now()-changed<5000);
  assert.equal(await preview('percent','duo'),'50,00%');assert.equal(await preview('remaining','duo'),'1');assert.equal(await preview('seen','duo'),'1');
  pass('Official Pokédex event refreshes the queued immutable snapshot before the five-second fallback');
  await response('/dex locked placeholder total',text=>text.includes('não tem permissão'));
  await lp('set shirokaidex.collection.locked true');assert.equal(await preview('total','locked'),'1');
  await lp('set shirokaidex.collection.locked false');await response('/dex locked placeholder total',text=>text.includes('não tem permissão'));
  pass('Preview command permissions follow live grants and revocations without OP fallback');
  await lp('set shirokaidex.admin.reload true');
  const config=JSON.parse(original);config.language='en_us';config.collections.find(c=>c.id==='duo').criteria.include_species=['dex_test:ordinary'];
  fs.writeFileSync(configFile,JSON.stringify(config));
  await response('/dex admin reload',text=>text.includes('reloaded'));
  await until(async()=>(await preview('total','duo'))==='1');assert.equal(await preview('percent','duo'),'100.00%');
  pass('Configuration reload replaces snapshot revision, criteria and language without serving the previous definition');
  fs.writeFileSync(configFile,original);await response('/dex admin reload',text=>text.includes('recarregada'));
  await until(async()=>(await preview('total','duo'))==='2');assert.equal(await preview('percent','duo'),'50,00%');
  pass('Restoring valid configuration refreshes counts and pt_br formatting');
  bot.quit();await delay(500);await login(PLAYER+'B');
  await until(async()=>(await preview('registered','duo'))==='0');
  assert.equal(await preview('percent','duo'),'0,00%');
  pass('Logout/new-player lifecycle does not leak another player snapshot');
}
(async()=>{
  let exit=0;
  try{
    assert(process.env.MC_TEST_RCON_PASSWORD_FILE);
    rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT||25573),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
    await login(PLAYER);if(PROBE)await probe();else await normal();
  }catch(error){exit=1;console.error(error);results.push({name:'placeholder integration',status:'failed',error:error.message});}
  finally{
    fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();
    const report=path.resolve(__dirname,'../../build/integration-placeholders'+(PROBE?'-probe':'')+'.json');fs.mkdirSync(path.dirname(report),{recursive:true});
    fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / Text Placeholder API 2.4.2+1.21 / LuckPerms 5.4.140',test_player:PLAYER,results},null,2));process.exitCode=exit;
  }
})();
