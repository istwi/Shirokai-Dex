// Read-only wiki browsing against Fabric/Cobblemon with collections/datapack fixtures and LuckPerms.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');
const PLAYER = process.env.MC_REWARD_PLAYER || 'DexReward';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER));
const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
const configFile = path.join(root, 'config/shirokaidex/config.json');
const original = fs.readFileSync(configFile, 'utf8');
assert.deepEqual(JSON.parse(original), JSON.parse(fs.readFileSync(process.env.MC_REWARD_FIXTURE || path.join(__dirname, '../fixtures/rewards/config.json'), 'utf8')));
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
  assert(process.env.MC_TEST_RCON_PASSWORD_FILE);
  rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT||25573),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
  bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT||25571),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
  const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
  let spawned=false;bot.once('spawn',()=>{spawned=true;});bot.on('error',v=>{error=v;});bot.on('kicked',v=>{error=new Error(JSON.stringify(v));});await until(()=>spawned,45000);await rcon.command(`deop ${PLAYER}`);
  if(process.env.MC_REWARD_MODE==='after-restart'){
    const expectedDiamonds=Number(process.env.MC_REWARD_EXPECT_DIAMONDS||2);
    await until(()=>quantity('diamond')===expectedDiamonds&&quantity('paper')===64);
    await until(async()=>{const menu=await open('/dex rewards first');const ready=Boolean(menu.slots[24]);await close();return ready;});
    await claim('first','já registrado ou bloqueado');assert.equal(quantity('diamond'),expectedDiamonds);assert.equal(quantity('paper'),64);
    if(JSON.parse(original).rewards.some(p=>p.id==='save_failure')){await claim('save_failure','já registrado ou bloqueado');assert.equal(quantity('diamond'),expectedDiamonds);pass('Uncertain save failure stays blocked after restart without redelivery');}
    pass('Completed claim and saved inventory survive restart without redelivery');
    if(process.env.MC_REWARD_REVIEW_FIXTURE){
      const fixture=JSON.parse(fs.readFileSync(process.env.MC_REWARD_REVIEW_FIXTURE,'utf8'));
      await lp('set shirokaidex.admin.rewards true');
      const state=await response('/dex admin reward status '+fixture.uncertain_complete,t=>t.startsWith('Operação:'));
      assert(state.includes('NEEDS_REVIEW'));
      await response('/dex admin reward complete '+fixture.uncertain_complete+' confirm Simulated external evidence fixture',t=>t==='Operação: OK');
      assert((await response('/dex admin reward status '+fixture.uncertain_complete,t=>t.startsWith('Operação:'))).includes('COMPLETED'));
      assert.equal(quantity('diamond'),2);assert.equal(quantity('paper'),64);
      pass('Interrupted processing recovers to review; explicit administrative completion records evidence without delivering items');
      await lp('set shirokaidex.admin.rewards false');
      await send('/dex admin reward cancel '+fixture.pending_cancel+' confirm Unauthorized fixture attempt');await delay(500);
      await lp('set shirokaidex.admin.rewards true');
      assert((await response('/dex admin reward status '+fixture.pending_cancel,t=>t.startsWith('Operação:'))).includes('PENDING'));
      await response('/dex admin reward cancel '+fixture.pending_cancel+' confirm Operator cancelled disposable fixture',t=>t==='Operação: OK');
      assert((await response('/dex admin reward status '+fixture.pending_cancel,t=>t.startsWith('Operação:'))).includes('CANCELLED'));
      pass('Reward administration uses its own live permission; revocation prevents cancellation and authorized cancellation is final');
    }
    return;
  }
  const baseline=await response('/dex status',t=>t.includes('Pokédex oficial:'));assert(baseline.includes('0 espécies obtidas'),'Use fresh reward player');
  let menu=await open('/dex rewards first');assert(!menu.slots[24]);assert(data(menu.slots[19]).includes('Bloqueada'));
  await bot.clickWindow(0,0,4);await until(()=>menu.slots[0]?.name==='diamond'&&!menu.selectedItem);await close();assert.equal(quantity('diamond'),0);
  pass('Locked milestone preview cannot claim or extract displayed reward icons');
  await rcon.command(`givepokemonother ${PLAYER} pikachu level=5`);
  await until(async()=>(await response('/dex pair status',t=>t.startsWith('Par:'))).includes('1/2'));
  const afterGrant=await response('/dex status',t=>t.includes('Pokédex oficial:'));
  menu=await open('/dex rewards complete');assert(!menu.slots[24]);assert(data(menu.slots[19]).includes('100%'));
  menu=await open('/dex rewards pages');assert(menu.slots[26]);assert.equal(menu.slots.slice(0,18).filter(Boolean).length,18);menu=await click(26);assert.equal(menu.slots.slice(0,18).filter(Boolean).length,14);await close();
  pass('Official progress unlocks count threshold, keeps 100 percent locked and paginates all 32 item entries');
  await rcon.command(`give ${PLAYER} minecraft:cobblestone 2304`);await until(()=>quantity('cobblestone')===2304);
  await claim('first','inventário sem espaço');assert.equal(quantity('diamond'),0);assert.equal(quantity('paper'),0);assert.equal(quantity('cobblestone'),2304);
  pass('Full inventory rejects entire plan without partial delivery or item drops');
  await rcon.command(`clear ${PLAYER}`);await until(()=>bot.inventory.items().length===0);
  await claim('first','entregue e registrada');await until(()=>quantity('diamond')===2&&quantity('paper')===64);
  pass('Separate explicit confirmation delivers every item and acknowledges persistent journal steps');
  await claim('first','já registrado ou bloqueado');assert.equal(quantity('diamond'),2);assert.equal(quantity('paper'),64);
  pass('Repeated claim preserves exactly the acknowledged quantities without another delivery');
  await close();await response('/dex rewards restricted',t=>t.includes('não tem permissão'));
  await lp('set shirokaidex.reward.private true');menu=await open('/dex rewards restricted');assert(menu.slots[24]);await lp('set shirokaidex.reward.private false');
  await messageAfter(()=>bot.clickWindow(24,0,0),t=>t.includes('sem permissão'));await close();assert.equal(quantity('emerald'),0);
  pass('Current collection permission is required for preview and stale claim buttons');
  await lp('set shirokaidex.admin.reload true');menu=await open('/dex rewards pages');const changed=JSON.parse(original);changed.rewards.find(p=>p.id==='pages').items[0].count=99;fs.writeFileSync(configFile,JSON.stringify(changed));
  await response('/dex admin reload',t=>t.includes('recarregada'));await messageAfter(()=>bot.clickWindow(24,0,0),t=>t.includes('bloqueada, removida'));await close();
  changed.rewards[0].items[0].id='test:missing';fs.writeFileSync(configFile,JSON.stringify(changed));await response('/dex admin reload',t=>t.includes('última configuração válida'));
  fs.writeFileSync(configFile,original);await response('/dex admin reload',t=>t.includes('recarregada'));assert.equal(await response('/dex status',t=>t.includes('Pokédex oficial:')),afterGrant);
  assert.equal(quantity('diamond'),2);assert.equal(quantity('paper'),64);assert.equal(quantity('emerald'),0);
  pass('Reload revalidates stale plans, rejects unknown items atomically and leaves official progress unchanged');
  if(JSON.parse(original).rewards.some(p=>p.id==='save_failure')){
    const properties=fs.readFileSync(path.join(root,'server.properties'),'utf8');assert(/^level-name=reward-validation$/m.test(properties),'I/O fault test only accepts the disposable reward-validation world');
    const folder=path.join(root,'reward-validation/playerdata');const preserved=folder+'.reward-test-'+process.pid;
    assert(fs.lstatSync(folder).isDirectory());assert(!fs.existsSync(preserved));fs.renameSync(folder,preserved);fs.writeFileSync(folder,'Disposable player save failure fixture');
    try{await claim('save_failure','bloqueada para revisão');assert.equal(quantity('diamond'),3);}
    finally{fs.unlinkSync(folder);fs.renameSync(preserved,folder);}
    await claim('save_failure','já registrado ou bloqueado');assert.equal(quantity('diamond'),3);await rcon.command('save-all');
    pass('Silent vanilla player save failure is detected by readback; uncertain effect is blocked for review without a second delivery');
  }

}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'reward integration',status:'failed',error:failure.message});}finally{fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();const report=path.resolve(__dirname,process.env.MC_REWARD_MODE==='after-restart'?'../../build/integration-rewards-restart.json':'../../build/integration-rewards.json');fs.mkdirSync(path.dirname(report),{recursive:true});fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140',test_player:PLAYER,results},null,2));process.exitCode=exit;}})();
