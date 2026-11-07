// Read-only wiki browsing against Fabric/Cobblemon with collections/datapack fixtures and LuckPerms.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');
const PLAYER = process.env.MC_UNLOCK_PLAYER || 'DexUnlock';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER));
const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
const configFile = path.join(root, 'config/shirokaidex/config.json');
const original = fs.readFileSync(configFile, 'utf8');
const afterRestart=process.env.MC_UNLOCK_MODE==='after-restart';
if(!afterRestart)assert.deepEqual(JSON.parse(original),JSON.parse(fs.readFileSync(path.join(__dirname,'../fixtures/reward-unlocks/config.json'),'utf8')));
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
let keepChanged=false;
async function unlocked(id){await until(async()=>{const menu=await open('/dex rewards '+id);const value=Boolean(menu.slots[24]);await close();return value;},20000);}
async function main(){
  rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT||25573),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
  bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT||25571),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
  const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
  let spawned=false;bot.once('spawn',()=>{spawned=true;});bot.on('error',v=>{error=v;});bot.on('kicked',v=>{error=new Error(JSON.stringify(v));});await until(()=>spawned,45000);await rcon.command(`deop ${PLAYER}`);
  if(afterRestart){
    assert.equal(JSON.parse(original).rewards.find(p=>p.id==='permanent').minimum_registered,2);
    assert((await response('/dex pair status',t=>t.startsWith('Par:'))).includes('0/1'));
    await unlocked('permanent');assert.equal(quantity('emerald'),0);
    pass('Unclaimed durable unlock survives restart despite current zero progress and increased threshold');
    await claim('permanent','entregue e registrada');await until(()=>quantity('emerald')===1);
    await claim('permanent','já registrado ou bloqueado');assert.equal(quantity('emerald'),1);
    pass('Persisted unlock allows one later claim and blocks a repeat without requalifying current progress');return;
  }
  assert((await response('/dex status',t=>t.includes('Pokédex oficial:'))).includes('0 espécies obtidas'));
  let menu=await open('/dex rewards permanent');assert(!menu.slots[24]);await close();
  await rcon.command(`givepokemonother ${PLAYER} pikachu level=5`);await unlocked('permanent');assert.equal(quantity('emerald'),0);
  pass('Observed official progress automatically persists an unlock without claiming or delivering');
  await lp('set shirokaidex.admin.reload true');const changed=JSON.parse(original);
  changed.collections.find(c=>c.id==='pair').criteria.include_species=['cobblemon:bulbasaur'];changed.rewards.find(p=>p.id==='permanent').minimum_registered=2;
  fs.writeFileSync(configFile,JSON.stringify(changed));await response('/dex admin reload',t=>t.includes('recarregada'));
  assert((await response('/dex pair status',t=>t.startsWith('Par:'))).includes('0/1'));await unlocked('permanent');
  pass('Successful category/threshold changes preserve the previously observed unlock');
  changed.collections.find(c=>c.id==='pair').permission='shirokaidex.unlock.claim';fs.writeFileSync(configFile,JSON.stringify(changed));await response('/dex admin reload',t=>t.includes('recarregada'));
  await response('/dex rewards permanent',t=>t.includes('não tem permissão'));await lp('set shirokaidex.unlock.claim true');await unlocked('permanent');
  menu=await open('/dex rewards permanent');await lp('set shirokaidex.unlock.claim false');await messageAfter(()=>bot.clickWindow(24,0,0),t=>t.includes('sem permissão'));await close();assert.equal(quantity('emerald'),0);
  await lp('set shirokaidex.unlock.claim true');keepChanged=true;
  pass('Permanent unlock never bypasses the current permission or delivers from a stale revoked button');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'unlock integration',status:'failed',error:failure.message});}finally{if(!keepChanged&&!afterRestart)fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();const report=path.resolve(__dirname,afterRestart?'../../build/integration-unlocks-restart.json':'../../build/integration-unlocks.json');fs.mkdirSync(path.dirname(report),{recursive:true});fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140',test_player:PLAYER,results},null,2));process.exitCode=exit;}})();
