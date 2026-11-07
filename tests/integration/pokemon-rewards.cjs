// Read-only wiki browsing against Fabric/Cobblemon with collections/datapack fixtures and LuckPerms.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');
const PLAYER = process.env.MC_POKEMON_PLAYER || 'DexPokemon';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER));
const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
const configFile = path.join(root, 'config/shirokaidex/config.json');
const original = fs.readFileSync(configFile, 'utf8');
assert.deepEqual(JSON.parse(original), JSON.parse(fs.readFileSync(process.env.MC_REWARD_FIXTURE || path.join(__dirname, '../fixtures/pokemon-rewards/config.json'), 'utf8')));
const results = [];
let bot, rcon, error, lastSend = 0;
function pass(name) { console.log('PASS', name); results.push({ name, status: 'passed' }); }
async function until(predicate, timeout = 15000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) { if (error) throw error; if (await predicate()) return; await delay(50); }
  throw new Error('Timed out waiting for wiki state');
}
async function send(text) {
  await delay(Math.max(0, 1100 - (Date.now() - lastSend))); lastSend = Date.now(); bot.chat(text);
}
function response(text, matches) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { bot.removeListener('messagestr', listener); reject(new Error('Missing response: ' + text)); }, 15000);
    function listener(value) { if (matches(value)) { clearTimeout(timer); bot.removeListener('messagestr', listener); resolve(value); } }
    bot.on('messagestr', listener); send(text).catch(reject);
  });
}
async function close() { if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await until(() => !bot.currentWindow); } }
async function open(text) {
  await close(); await send(text); await until(() => bot.currentWindow); return bot.currentWindow;
}
async function click(slot) {
  const before = bot.currentWindow; await bot.clickWindow(slot, 0, 0);
  await until(() => bot.currentWindow && bot.currentWindow !== before); return bot.currentWindow;
}
function data(item) { return JSON.stringify(item?.components || item?.nbt || []); }
function ids(menu) { return menu.slots.slice(0, 18).filter(item => item?.name === 'book').map(item => data(item).match(/(?:cobblemon|dex_test):[a-z_]+/)[0]); }
function title(menu) { return JSON.stringify(menu.title); }
async function lp(text) {
  const value = await rcon.command(`lp user ${bot.player.uuid} permission ${text}`);
  assert(!value.includes('Another command is being executed'), value); await delay(1200);
}
function quantity(name){return bot.inventory.items().filter(i=>i.name===name).reduce((n,i)=>n+i.count,0);}
function messageAfter(action,predicate){return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{bot.removeListener('messagestr',listener);reject(new Error('Missing claim response'));},60000);function listener(text){if(predicate(text)){clearTimeout(timer);bot.removeListener('messagestr',listener);resolve(text);}}bot.on('messagestr',listener);Promise.resolve(action()).catch(reject);});}
async function claim(id,expected){let menu=await open('/dex rewards '+id);assert(menu.slots[24]);menu=await click(24);assert(title(menu).includes('Confirmar:'));return messageAfter(()=>bot.clickWindow(13,0,0),text=>text.includes(expected));}
async function stats(action='stats'){const text=await rcon.command(`/dexpokemonprobe ${PLAYER} ${action}`);return JSON.parse(text);}
async function ready(id){await until(async()=>{const menu=await open('/dex rewards '+id);const value=Boolean(menu.slots[24]);await close();return value;},20000);}
async function main(){
  rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT||25573),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
  bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT||25571),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
  const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
  let spawned=false;bot.once('spawn',()=>{spawned=true;});bot.on('error',v=>{error=v;});bot.on('kicked',v=>{error=new Error(JSON.stringify(v));});await until(()=>spawned,45000);await rcon.command(`deop ${PLAYER}`);
  if(process.env.MC_POKEMON_MODE==='after-restart'){
    const data=await stats();assert.equal(data.party_count,6);assert.equal(data.pc_count,30);
    assert.equal(data.party.filter(p=>p.species==='cobblemon:bulbasaur').length,1);assert.equal(data.pc.filter(p=>p.species==='cobblemon:vulpix').length,1);assert.equal(data.pc.filter(p=>p.species==='cobblemon:eevee').length,1);
    await ready('partner');await claim('partner','já registrado ou bloqueado');await claim('overflow','já registrado ou bloqueado');await claim('uncertain','já registrado ou bloqueado');
    const after=await stats();assert.deepEqual(after,data);assert.equal(quantity('paper'),1);
    pass('Saved party/PC and prepared UUIDs survive restart; completed and uncertain operations never redeliver');return;
  }
  assert.equal((await stats()).party_count,0);await rcon.command(`givepokemonother ${PLAYER} pikachu level=5`);await ready('partner');
  let menu=await open('/dex rewards partner');assert.equal(menu.slots[0].name,'paper');assert.equal(menu.slots[1].name,'dragon_egg');assert(data(menu.slots[1]).includes('bulbasaur'));assert(data(menu.slots[1]).includes('31'));await close();assert.equal((await stats()).party_count,1);
  pass('Preview shows mixed item/Pokemon attributes without granting a Pokemon');
  await claim('partner','entregue e registrada');let state=await stats();const p=state.party.find(p=>p.species==='cobblemon:bulbasaur');assert(p);assert.equal(state.party_count,2);assert.equal(quantity('paper'),1);
  for(const [key,value] of Object.entries({level:12,shiny:true,nature:'cobblemon:modest',ability:'overgrow',gender:'MALE',ball:'cobblemon:great_ball',held:'minecraft:apple'}))assert.equal(p[key],value,key);
  assert.deepEqual(p.moves,['tackle','growl','vinewhip']);assert.equal(p.ivs.hp,31);assert.equal(p.ivs.atk,20);assert.equal(p.evs.hp,6);assert.equal(p.evs.spa,252);assert.equal(p.evs.spe,252);
  await claim('partner','já registrado ou bloqueado');assert.deepEqual(await stats(),state);assert.equal(quantity('paper'),1);
  assert((await response('/dex shiny status',t=>t.startsWith('Shiny:'))).includes('N/D'));
  pass('Mixed journal delivers exact validated properties, waits for NBT readback and blocks duplicate claims without inventing shiny captures');
  await lp('set shirokaidex.admin.reload true');const backup=fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8');
  for(const patch of [{species:'cobblemon:missing'},{form:'missing'},{nature:'cobblemon:missing'},{ability:'static'},{gender:'GENDERLESS'},{pokeball:'cobblemon:missing'},{moves:['splash']},{held_item:'minecraft:missing'}]){
    const invalid=JSON.parse(original);Object.assign(invalid.rewards[0].pokemon[0],patch);fs.writeFileSync(configFile,JSON.stringify(invalid));await response('/dex admin reload',t=>t.includes('última configuração válida'));assert.equal(fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8'),backup);
  }
  fs.writeFileSync(configFile,original);await response('/dex admin reload',t=>t.includes('recarregada'));
  pass('Eight invalid registry/form/property candidates are rejected atomically instead of silently falling back');
  const changed=JSON.parse(original);changed.collections.find(c=>c.id==='national').permission='shirokaidex.pokemon.claim';fs.writeFileSync(configFile,JSON.stringify(changed));await response('/dex admin reload',t=>t.includes('recarregada'));await lp('set shirokaidex.pokemon.claim true');
  menu=await open('/dex rewards overflow');menu=await click(24);await lp('set shirokaidex.pokemon.claim false');await messageAfter(()=>bot.clickWindow(13,0,0),t=>t.includes('sem permissão'));await close();assert.equal((await stats()).pc_count,0);
  fs.writeFileSync(configFile,original);await response('/dex admin reload',t=>t.includes('recarregada'));
  pass('Old Pokemon confirmation rechecks the current permission and cannot deliver after revocation');
  await stats('partyfill');await ready('overflow');await claim('overflow','entregue e registrada');state=await stats();const alola=state.pc.find(p=>p.species==='cobblemon:vulpix');assert(alola);assert.equal(alola.form.toLowerCase(),'alola');assert.equal(alola.ability,'snowcloak');assert.equal(alola.level,20);assert.equal(state.party_count,6);assert.equal(state.pc_count,1);
  pass('A full party overflows into real PC capacity while preserving a valid alternate form');
  await stats('fillall');await claim('uncertain','sem espaço');state=await stats();assert.equal(state.free,0);assert.equal(state.pc_count,30);assert(!state.pc.some(p=>p.species==='cobblemon:eevee'));
  pass('Full party plus full PC refuses the entire Pokemon reward without reserving an effect');
  await stats('freepc');const folder=path.join(root,'pokemon-reward-validation/pokemon/pcstore');const preserved=folder+'.reward-test-'+process.pid;
  assert(/^level-name=pokemon-reward-validation$/m.test(fs.readFileSync(path.join(root,'server.properties'),'utf8')));assert(fs.lstatSync(folder).isDirectory());assert(!fs.existsSync(preserved));fs.renameSync(folder,preserved);fs.writeFileSync(folder,'Disposable native PC save failure fixture');
  try{await claim('uncertain','bloqueada para revisão');}
  finally{fs.unlinkSync(folder);fs.renameSync(preserved,folder);}
  await stats('touch');await delay(3500);state=await stats();const eevee=state.pc.find(p=>p.species==='cobblemon:eevee');assert(eevee);assert.equal(eevee.level,5);assert.equal(eevee.shiny,false);assert.equal(eevee.ball,'cobblemon:poke_ball');assert.equal(eevee.held,'minecraft:air');assert(Object.values(eevee.ivs).every(n=>n>=0&&n<=31));assert(Object.values(eevee.evs).every(n=>n===0));
  await claim('uncertain','já registrado ou bloqueado');assert.deepEqual(await stats(),state);
  pass('Failed asynchronous PC write leaves one frozen Pokemon in review, never retries delivery, and native omitted defaults remain valid');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'pokemon rewards integration',status:'failed',error:failure.message});}finally{fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();const report=path.resolve(__dirname,process.env.MC_POKEMON_MODE==='after-restart'?'../../build/integration-pokemon-restart.json':'../../build/integration-pokemon.json');fs.mkdirSync(path.dirname(report),{recursive:true});fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140',test_player:PLAYER,results},null,2));process.exitCode=exit;}})();
