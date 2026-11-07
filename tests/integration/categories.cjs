// Named category integration: use categories config and the wiki-facets datapack in an isolated world.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');
const PLAYER = process.env.MC_CATEGORY_PLAYER || 'DexCategory';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER));
const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
const configFile = path.join(root, 'config/shirokaidex/config.json');
const original = fs.readFileSync(configFile, 'utf8');
const afterRestart = process.env.MC_CATEGORY_MODE === 'after-restart';
if (afterRestart) assert(original.startsWith('{ invalid'));
else assert.deepEqual(JSON.parse(original), JSON.parse(fs.readFileSync(path.join(__dirname, '../fixtures/categories/config.json'), 'utf8')));
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
  bot = mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT || 25571),username:PLAYER,
    auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{
      packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'
    }}}}}});
  const emit=bot._client.emit;
  bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
  let spawned=false;bot.once('spawn',()=>{spawned=true;});bot.on('error',v=>{error=v;});bot.on('kicked',v=>{error=new Error(JSON.stringify(v));});await until(()=>spawned,45000);
  await rcon.command(`deop ${PLAYER}`);
  const baseline=await response('/dex status',text=>text.includes('Pokédex oficial:'));
  assert(baseline.includes('0 espécies obtidas'),'Use a fresh disposable player');
  if(afterRestart) {
    assert((await response('/dex variants status',t=>t.startsWith('Variantes:'))).includes('0/1'));
    assert((await response('/dex split status',t=>t.startsWith('Mesma forma:'))).includes('0/0'));
    pass('Restart recovers validated category backup without overwriting invalid config.json');
    const recovered=await open('/dex variants wiki');assert.deepEqual(ids(recovered),['dex_test:variants']);await close();
    assert.equal(await response('/dex status',t=>t.includes('Pokédex oficial:')),baseline);
    pass('Recovered rules drive read-only wiki and progress after migration 2 restart');
    return;
  }

  assert((await response('/dex split status',t=>t.startsWith('Mesma forma:'))).includes('0/0'));
  assert((await response('/dex variants status',t=>t.startsWith('Variantes:'))).includes('0/1'));
  assert((await response('/dex union status',t=>t.startsWith('Uniao:'))).includes('0/1'));
  pass('Named all/any/none conditions use the same form and count a species once across 41 forms');
  let menu=await open('/dex variants wiki');assert.deepEqual(ids(menu),['dex_test:variants']);
  menu=await click(0);assert(data(menu.slots[13]).includes('Obtida: Não'));
  await close();menu=await open('/dex split wiki');assert.equal(ids(menu).length,0);
  pass('Wiki and progress use identical category scope; incompatible forms do not cross-match');
  await close();await response('/dex locked wiki',t=>t.includes('não tem permissão'));
  await lp('set shirokaidex.collection.locked true');menu=await open('/dex locked wiki');assert.equal(ids(menu).length,1);
  await lp('set shirokaidex.collection.locked false');
  const denied=new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(new Error('Missing stale permission denial')),10000);bot.once('messagestr',function check(t){if(t.includes('não tem permissão')){clearTimeout(timer);resolve();}else bot.once('messagestr',check);});});
  await bot.clickWindow(0,0,0);await denied;await close();
  pass('Collection permissions protect named categories and stale wiki buttons');
  await lp('set shirokaidex.admin.reload true');menu=await open('/dex variants wiki');
  const changed=JSON.parse(original);changed.categories.find(c=>c.id==='water').criteria.types=['fire'];
  fs.writeFileSync(configFile,JSON.stringify(changed));await response('/dex admin reload',t=>t.includes('recarregada'));
  menu=await click(0);assert.equal(ids(menu).length,0);await close();
  assert((await response('/dex variants status',t=>t.startsWith('Variantes:'))).includes('0/0'));
  pass('Category reload invalidates progress caches and stale wiki navigation');
  const validBackup=fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8');
  for(const replacement of [{id:'water',all_of:['water']},{id:'water',criteria:{types:['made_up_type']}}]){
    const invalid=structuredClone(changed);invalid.categories[1]=replacement;fs.writeFileSync(configFile,JSON.stringify(invalid));
    await response('/dex admin reload',t=>t.includes('última configuração válida foi mantida'));
    assert((await response('/dex variants status',t=>t.startsWith('Variantes:'))).includes('0/0'));
    assert.equal(fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8'),validBackup);
  }
  pass('Cycles and unknown registry references reject the whole reload and preserve the last valid backup');
  fs.writeFileSync(configFile,original);await response('/dex admin reload',t=>t.includes('recarregada'));
  assert((await response('/dex variants status',t=>t.startsWith('Variantes:'))).includes('0/1'));
  assert.equal(await response('/dex status',t=>t.includes('Pokédex oficial:')),baseline);
  assert(!bot.inventory.items().some(item=>item.name==='book'));
  pass('Valid restoration reapplies categories without modifying official progress or granting menu icons');
}
(async()=>{
  let exit=0;
  try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'categories integration',status:'failed',error:failure.message});}
  finally{
    fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();
    const report=path.resolve(__dirname,afterRestart ? '../../build/integration-category-restart.json' : '../../build/integration-categories.json');fs.mkdirSync(path.dirname(report),{recursive:true});
    fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140',test_player:PLAYER,results},null,2));process.exitCode=exit;
  }
})();
