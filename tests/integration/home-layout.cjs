// Read-only wiki browsing against Fabric/Cobblemon with collections/datapack fixtures and LuckPerms.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');
const PLAYER = process.env.MC_LAYOUT_PLAYER || 'DexLayout';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER));
const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
const configFile = path.join(root, 'config/shirokaidex/config.json');
const original = fs.readFileSync(configFile, 'utf8');
assert.deepEqual(JSON.parse(original), JSON.parse(fs.readFileSync(process.env.MC_REWARD_FIXTURE || path.join(__dirname, '../fixtures/layout/config.json'), 'utf8')));
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
function messageAfter(action,predicate){return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{bot.removeListener('messagestr',listener);reject(new Error('Missing claim response'));},15000);function listener(text){if(predicate(text)){clearTimeout(timer);bot.removeListener('messagestr',listener);resolve(text);}}bot.on('messagestr',listener);Promise.resolve(action()).catch(reject);});}
async function claim(id,expected){let menu=await open('/dex rewards '+id);assert(menu.slots[24]);menu=await click(24);assert(title(menu).includes('Confirmar:'));return messageAfter(()=>bot.clickWindow(13,0,0),text=>text.includes(expected));}
async function main(){
  rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT||25573),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
  bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT||25571),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
  const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
  let spawned=false;bot.once('spawn',()=>{spawned=true;});bot.on('error',v=>{error=v;});bot.on('kicked',v=>{error=new Error(JSON.stringify(v));});await until(()=>spawned,45000);await rcon.command(`deop ${PLAYER}`);
  const baseline=await response('/dex status',t=>t.includes('Pokédex oficial:'));let menu=await open('/dex');
  assert.equal(menu.type,'minecraft:generic_9x6');assert.equal(menu.slots[4].name,'player_head');assert(data(menu.slots[4]).includes(PLAYER));assert(title(menu).includes('✦'));assert(new Set(title(menu).match(/#[0-9a-f]{6}/ig)).size>1);assert(data(menu.slots[22]).includes('20'));
  if(process.env.MC_LAYOUT_MODE==='without-external'){
    assert(data(menu.slots[48]).includes('indisponível'));await bot.clickWindow(48,0,0);await until(()=>bot.currentWindow===menu&&!menu.selectedItem&&menu.slots[48]?.name==='arrow');
    await close();assert.equal(await response('/dex status',t=>t.includes('Pokédex oficial:')),baseline);
    pass('Missing external /menu stays visibly unavailable, with no execution or progress mutation');return;
  }
  pass('Six-row Shirokai hub exposes real trainer progress, gradient title and current reward count');
  for(const [slot,icon] of [[29,'golden_sword'],[31,'nether_star'],[33,'writable_book'],[40,'comparator']]){
    assert(data(menu.slots[slot]).includes('Em desenvolvimento'));await bot.clickWindow(slot,0,0);await until(()=>bot.currentWindow===menu&&!menu.selectedItem&&menu.slots[slot]?.name===icon);
  }
  for(const [button,mode] of [[0,0],[1,0],[0,1],[0,2],[0,4]]){await bot.clickWindow(4,button,mode);await until(()=>bot.currentWindow===menu&&!menu.selectedItem&&menu.slots[4]?.name==='player_head');}
  assert.equal(bot.inventory.items().length,0);
  pass('Future systems are honestly disabled; profile, decoration and six-row icons cannot be extracted');
  menu=await click(20);assert.equal(menu.type,'minecraft:generic_9x3');assert.equal(menu.slots[13].name,'book');menu=await click(13);assert.equal(menu.slots[13].name,'book');menu=await click(18);menu=await click(23);assert.equal(menu.type,'minecraft:generic_9x6');
  pass('Hub, collection selector and collection details have reversible navigation to the central menu');
  menu=await click(22);assert.equal(menu.slots.slice(0,18).filter(item=>item?.name==='chest').length,18);menu=await click(26);assert.equal(menu.slots.slice(0,18).filter(item=>item?.name==='chest').length,2);menu=await click(18);menu=await click(0);assert.equal(menu.slots[0].name,'paper');assert(!menu.slots[24]);menu=await click(18);menu=await click(23);assert.equal(menu.type,'minecraft:generic_9x6');
  pass('Reward grid paginates twenty visible milestones, hides restricted definitions and returns from preview');
  menu=await click(24);assert.equal(menu.type,'minecraft:generic_9x4');assert(title(menu).includes('Wiki'));await close();
  await lp('set shirokaidex.admin.reload true');const changed=JSON.parse(original);changed.collections[0].permission='shirokaidex.layout.national';fs.writeFileSync(configFile,JSON.stringify(changed));await response('/dex admin reload',t=>t.includes('recarregada'));
  menu=await open('/dex');assert(data(menu.slots[24]).includes('Sem acesso'));await bot.clickWindow(24,0,0);await until(()=>bot.currentWindow===menu&&!menu.selectedItem);await close();
  fs.writeFileSync(configFile,original);await response('/dex admin reload',t=>t.includes('recarregada'));
  pass('Wiki shortcut uses the actual current collection permission and disables itself after revocation');
  const backup=fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8');const invalid=JSON.parse(original);invalid.home={slots:{dex:22}};fs.writeFileSync(configFile,JSON.stringify(invalid));await response('/dex admin reload',t=>t.includes('última configuração válida'));assert.equal(fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8'),backup);
  const themed=JSON.parse(original);themed.language='en_us';themed.home={rows:4,palette:'OCEAN',slots:{dex:10,rewards:12,wiki:14,competitions:19,achievements:20,history:21,settings:28,back:30,close:32}};fs.writeFileSync(configFile,JSON.stringify(themed));await response('/dex admin reload',t=>t.includes('reloaded'));
  menu=await open('/dex');assert.equal(menu.type,'minecraft:generic_9x4');assert.equal(menu.slots[10].name,'book');assert.equal(menu.slots[12].name,'chest');assert(data(menu.slots[12]).includes('Rewards'));assert(/#90f5e8/i.test(title(menu)));await close();
  fs.writeFileSync(configFile,original);await response('/dex admin reload',t=>t.includes('recarregada'));
  pass('Conflicting layout is rejected atomically; valid four-row Ocean palette and English labels apply together');
  menu=await open('/dex');await messageAfter(()=>bot.clickWindow(48,0,0),t=>t==='DEX_LAYOUT_EXTERNAL_OK '+PLAYER);await until(()=>!bot.currentWindow);
  pass('Back-to-menu action executes the registered external command as the non-op player, without privilege elevation');
  menu=await open('/dex');await bot.clickWindow(50,0,0);await until(()=>!bot.currentWindow);assert.equal(bot.inventory.items().length,0);assert.equal(await response('/dex status',t=>t.includes('Pokédex oficial:')),baseline);
  pass('Closing the redesigned hub preserves inventory and official progress');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'home layout integration',status:'failed',error:failure.message});}finally{fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();const report=path.resolve(__dirname,process.env.MC_LAYOUT_MODE==='without-external'?'../../build/integration-home-without-external.json':'../../build/integration-home.json');fs.mkdirSync(path.dirname(report),{recursive:true});fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140',test_player:PLAYER,results},null,2));process.exitCode=exit;}})();
