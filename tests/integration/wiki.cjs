// Read-only wiki browsing against Fabric/Cobblemon with collections/datapack fixtures and LuckPerms.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');
const PLAYER = process.env.MC_WIKI_PLAYER || 'DexWiki';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER));
const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
const configFile = path.join(root, 'config/shirokaidex/config.json');
const original = fs.readFileSync(configFile, 'utf8');
assert.deepEqual(JSON.parse(original), JSON.parse(fs.readFileSync(path.join(__dirname, '../fixtures/collections/config.json'), 'utf8')));
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
async function main() {
  assert(process.env.MC_TEST_RCON_PASSWORD_FILE);
  rcon = await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT || 25573), fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE, 'utf8').trim());
  bot = mineflayer.createBot({ host:'127.0.0.1', port:Number(process.env.MC_TEST_PORT || 25571), username:PLAYER,
    auth:'offline', version:'1.21.1', hideErrors:true, customPackets:{'1.21':{play:{toClient:{types:{
      packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'
    }}}}} });
  const emit = bot._client.emit;
  bot._client.emit = function(event,...args) { if (['entity_metadata','advancements','declare_commands'].includes(event)) return false; return emit.call(this,event,...args); };
  let spawned=false; bot.once('spawn',()=>{spawned=true;}); bot.on('error',value=>{error=value;});
  bot.on('kicked',value=>{error=new Error(JSON.stringify(value));}); await until(()=>spawned,45000);
  await rcon.command(`deop ${PLAYER}`);
  const baseline = await response('/dex status',text=>text.includes('Pokédex oficial:'));
  assert(baseline.includes('0 espécies obtidas'), 'Use a fresh disposable wiki player');
  let menu=await open('/dex national'); assert.equal(menu.slots[15].name,'arrow');
  menu=await click(15); assert(title(menu).includes('Wiki'));
  const first=ids(menu); assert.equal(first.length,18); assert.equal(menu.slots[26].name,'arrow');
  menu=await click(26); assert(!ids(menu).some(id=>first.includes(id)));
  menu=await click(18); assert.deepEqual(ids(menu),first);
  pass('Collection button opens a dynamic wiki; pagination is stable and reversible');
  menu=await click(20); const ordered=ids(menu); assert.deepEqual(ordered,[...ordered].sort());
  assert(data(menu.slots[20]).includes('Identificador'));
  pass('Wiki supports deterministic ordering by identifier');
  menu=await open('/dex duo wiki DEX_TEST:ORDINARY'); assert.deepEqual(ids(menu),['dex_test:ordinary']);
  menu=await click(0); const details=data(menu.slots[13]);
  for (const text of ['normal','Vista: Não','Obtida: Não','Shiny vista: Não','Informação de aparecimento indisponível','Capturas shiny verificadas: indisponível']) assert(details.includes(text),text);
  menu=await click(18); assert.deepEqual(ids(menu),['dex_test:ordinary']);
  menu=await open('/dex duo wiki Dex Test Ordinary'); assert.deepEqual(ids(menu),['dex_test:ordinary']);
  pass('Search accepts canonical names and case-insensitive namespaced IDs; details show real forms/types and explicit unknown spawn/capture data');
  menu=await open('/dex duo wiki no_such_species'); assert.equal(menu.slots[13].name,'paper'); assert.equal(ids(menu).length,0);
  menu=await click(24); assert.equal(ids(menu).length,2);
  menu=await open('/dex future_species wiki'); assert.equal(ids(menu).length,0); assert(title(menu).includes('(0)'));
  await close(); await response('/dex duo wiki '+'x'.repeat(129),text=>text.includes('até 128 caracteres'));
  pass('Empty results, reset, valid unimplemented species and oversized search have explicit bounded behavior');
  menu=await open('/dex without_legend wiki'); assert.deepEqual(ids(menu),['dex_test:ordinary']);
  menu=await open('/dex normal_legends wiki dex_test:'); assert.deepEqual(ids(menu),['dex_test:legend']);
  menu=await open('/dex shiny wiki dex_test:ordinary'); assert.deepEqual(ids(menu),['dex_test:ordinary']);
  assert(data(menu.slots[0]).includes('Capturas shiny verificadas: indisponível'));
  await bot.clickWindow(0,0,4); await until(()=>menu.slots[0]?.name==='book'&&!menu.selectedItem);
  await close(); assert(!bot.inventory.items().some(item=>item.name==='book'));
  assert.equal(await response('/dex status',text=>text.includes('Pokédex oficial:')),baseline);
  pass('Collection filters and shiny-unavailable semantics are preserved; browsing and discard do not mutate official progress or give icons');
  await rcon.command(`givepokemonother ${PLAYER} dex_test:ordinary level=5`);
  await until(async()=> (await response('/dex duo status',text=>text.startsWith('Duo:'))).includes('1/2'));
  menu=await open('/dex duo wiki'); menu=await click(19); assert.deepEqual(ids(menu),['dex_test:ordinary']);
  menu=await click(19); assert.deepEqual(ids(menu),['dex_test:ordinary']);
  menu=await click(19); assert.deepEqual(ids(menu),['dex_test:legend']);
  menu=await click(19); assert.equal(ids(menu).length,0);
  await close(); await rcon.command(`givepokemonother ${PLAYER} dex_test:ordinary shiny=true level=5`);
  await until(async()=> (await response('/dex shiny_seen status',text=>text.startsWith('Shiny vistas:'))).includes('1/1'));
  menu=await open('/dex duo wiki'); for(let i=0;i<4;i++) menu=await click(19);
  assert.deepEqual(ids(menu),['dex_test:ordinary']); assert(data(menu.slots[0]).includes('Shiny vista: Sim'));
  pass('Seen/obtained/unseen/shiny-seen filters track official Cobblemon events without treating granted Pokémon as verified shiny captures');
  await close(); await response('/dex locked wiki',text=>text.includes('não tem permissão'));
  await lp('set shirokaidex.collection.locked true');
  menu=await open('/dex locked wiki'); assert.deepEqual(ids(menu),['dex_test:ordinary']);
  await lp('set shirokaidex.collection.locked false');
  const denied=new Promise((resolve,reject)=>{
    const timer=setTimeout(()=>{bot.removeListener('messagestr',listener);reject(new Error('Stale wiki action did not deny access'));},10000);
    function listener(text){if(text.includes('não tem permissão')){clearTimeout(timer);bot.removeListener('messagestr',listener);resolve();}}
    bot.on('messagestr',listener);
  });
  await bot.clickWindow(0,0,0); await denied;
  await until(()=>bot.currentWindow===menu&&!menu.selectedItem&&menu.slots[0]?.name==='book');
  pass('Restricted wiki commands and stale species buttons enforce current LuckPerms permissions');
  await close(); await lp('set shirokaidex.collection.locked true'); await lp('set shirokaidex.admin.reload true');
  menu=await open('/dex locked wiki');
  const changed=JSON.parse(original);changed.collections.find(c=>c.id==='locked').criteria.include_species=['dex_test:legend'];
  fs.writeFileSync(configFile,JSON.stringify(changed));
  await response('/dex admin reload',text=>text.includes('recarregada'));
  menu=await click(0); assert.deepEqual(ids(menu),['dex_test:legend']);
  await close();fs.writeFileSync(configFile,original);await response('/dex admin reload',text=>text.includes('recarregada'));
  menu=await open('/dex locked wiki'); assert.deepEqual(ids(menu),['dex_test:ordinary']);
  menu=await click(23); assert(!title(menu).includes('Wiki')); assert.equal(menu.slots[13].name,'diamond');
  await close();
  pass('Stale wiki navigation revalidates reloaded collection criteria and returns safely to the collection');
}
(async()=>{
  let exit=0;
  try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'wiki integration',status:'failed',error:failure.message});}
  finally{
    fs.writeFileSync(configFile,original);
    if(bot)bot.quit();if(rcon)rcon.close();
    const report=path.resolve(__dirname,'../../build/integration-wiki.json');fs.mkdirSync(path.dirname(report),{recursive:true});
    fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140',test_player:PLAYER,results},null,2));
    process.exitCode=exit;
  }
})();
