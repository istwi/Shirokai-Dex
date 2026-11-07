// Read-only wiki browsing against Fabric/Cobblemon with collections/datapack fixtures and LuckPerms.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');
const PLAYER = process.env.MC_SAVED_PLAYER || 'DexSaved';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER));
const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
const configFile = path.join(root, 'config/shirokaidex/config.json');
const original = fs.readFileSync(configFile, 'utf8');
const afterRestart=process.env.MC_SAVED_MODE==='after-restart';
const listMode=process.env.MC_SAVED_MODE==='review-list';
if(!afterRestart&&!listMode)assert.deepEqual(JSON.parse(original),JSON.parse(fs.readFileSync(path.join(__dirname,'../fixtures/saved-items/config.json'),'utf8')));
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
async function claim(id,expected){let menu=await open('/dex rewards '+id);if(expected==='já registrado ou bloqueado'){assert(!menu.slots[24],'Closed operation must hide repeat claim');assert(['Resgate concluído','Revisão necessária','Resgate cancelado','Recompensa alterada'].some(label=>data(menu.slots[19]).includes(label)),'Journal state must explain the block');return;}assert(menu.slots[24]);menu=await click(24);assert(title(menu).includes('Confirmar:'));return messageAfter(()=>bot.clickWindow(13,0,0),text=>text.includes(expected));}
let keepChanged=false;
function sword(name){return `minecraft:diamond_sword[minecraft:custom_name='${JSON.stringify({text:name,color:'aqua',italic:false})}',minecraft:lore=['{"text":"Lunar lore","color":"gold"}'],minecraft:enchantments={levels:{"minecraft:sharpness":4}},minecraft:damage=5,minecraft:custom_data={dex:{source:"saved_item_test",tier:7}}]`;}
function components(item){return JSON.parse(JSON.stringify(item.components||item.nbt||{}),(_,v)=>v);}
function componentKey(item){const value=components(item);return JSON.stringify(Array.isArray(value)?value.slice().sort((a,b)=>JSON.stringify(a.type).localeCompare(JSON.stringify(b.type))):value);}
async function readyReward(id){await until(async()=>{const menu=await open('/dex rewards '+id);const ready=Boolean(menu.slots[24]);await close();return ready;},20000);}
async function main(){
  rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT||25573),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
  bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT||25571),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
  const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
  let spawned=false;bot.once('spawn',()=>{spawned=true;});bot.on('error',v=>{error=v;});bot.on('kicked',v=>{error=new Error(JSON.stringify(v));});await until(()=>spawned,45000);await rcon.command(`deop ${PLAYER}`);
  if(listMode){
    await lp('set shirokaidex.admin.reload false');await lp('set shirokaidex.admin.items false');await lp('set shirokaidex.admin.rewards true');
    const page=await response('/dex admin reward list',t=>t.startsWith('Operação:'));assert(page.includes('NEEDS_REVIEW'));assert(page.includes('save_failure'));assert(!page.includes(' | saved | '));
    assert((await response('/dex admin reward list 2',t=>t.startsWith('Operação:'))).includes('—'));
    await lp('set shirokaidex.admin.rewards false');await response('/dex admin reward list',t=>!t.startsWith('Operação:')&&(t.includes('command')||t.includes('comando')));
    pass('Review queue exposes pending operations by page with its own live permission and excludes completed claims');
    await lp('set shirokaidex.admin.items true');await lp('set shirokaidex.admin.reload true');bot.setQuickBarSlot(0);await delay(200);assert(data(bot.heldItem).includes('Moon'));
    await response('/dex admin item save typed',t=>t==='Itens salvos: typed');const typed=JSON.parse(original);
    typed.rewards.push({id:'typed_saved',collection:'national',minimum_registered:1,items:[{type:'SAVED_ITEM',saved_item:'typed',count:1}]});
    typed.rewards.push({id:'typed_mix',collection:'national',minimum_registered:1,items:[{type:'CUSTOM_ITEM',id:'minecraft:paper',count:1,payload_snbt:`{id:"minecraft:paper",count:1,components:{"minecraft:custom_name":'"TypedInline"'}}`},{type:'ITEM',id:'minecraft:diamond',count:1}]});
    fs.writeFileSync(configFile,JSON.stringify(typed));await response('/dex admin reload',t=>t.includes('recarregada'));
    const compiled=fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8');assert(!compiled.includes('saved_item"'));assert(compiled.includes('CUSTOM_ITEM'));
    await response('/dex admin item delete typed confirm',t=>t==='Itens salvos: typed');
    await readyReward('typed_saved');await claim('typed_saved','entregue e registrada');assert.equal(quantity('diamond_sword'),5);
    await readyReward('typed_mix');await claim('typed_mix','entregue e registrada');assert.equal(quantity('diamond'),3);assert.equal(quantity('paper'),66);assert(bot.inventory.items().some(i=>i.name==='paper'&&data(i).includes('TypedInline')));keepChanged=true;
    pass('Explicit ITEM/CUSTOM_ITEM/SAVED_ITEM schemas validate and deliver preserved payloads after template deletion');return;
  }
  if(afterRestart){
    const before=quantity('diamond_sword');assert.equal(before,3);
    await readyReward('after');await claim('after','entregue e registrada');await until(()=>quantity('diamond_sword')===4);
    for(const item of bot.inventory.items().filter(i=>i.name==='diamond_sword')){assert(data(item).includes('Moon'));assert(!data(item).includes('Sun'));assert(data(item).includes('saved_item_test'));}
    pass('Restart recovers compiled component snapshots even though the saved template was deleted');
    await claim('saved','já registrado ou bloqueado');assert.equal(quantity('diamond_sword'),4);
    await readyReward('inline');await claim('inline','entregue e registrada');assert.equal(quantity('paper'),1);assert(data(bot.inventory.items().find(i=>i.name==='paper')).includes('Inline'));
    pass('Claimed saved reward does not repeat; inline custom item remains valid after restart');
    const plain=JSON.parse(fs.readFileSync(path.join(__dirname,'../fixtures/rewards/config.json'),'utf8'));fs.writeFileSync(configFile,JSON.stringify(plain));
    await response('/dex admin reload',t=>t.includes('recarregada'));await readyReward('first');await claim('first','entregue e registrada');
    assert.equal(quantity('diamond'),2);assert.equal(quantity('paper'),65);
    const papers=bot.inventory.items().filter(i=>i.name==='paper');assert.equal(papers.length,2);assert.equal(papers.find(i=>data(i).includes('Inline')).count,1);assert(papers.some(i=>i.count===64));keepChanged=true;
    pass('Plain item rewards still deliver and never merge with an existing custom-component stack');return;
  }
  assert((await response('/dex status',t=>t.includes('Pokédex oficial:'))).includes('0 espécies obtidas'));
  await lp('set shirokaidex.admin.items true');
  await response('/dex admin item save empty',t=>t.includes('inválidos/indisponíveis'));
  await rcon.command(`give ${PLAYER} ${sword('Moon')} 1`);await until(()=>quantity('diamond_sword')===1);bot.setQuickBarSlot(0);await delay(200);
  const originalComponents=componentKey(bot.inventory.items().find(i=>i.name==='diamond_sword'));
  await response('/dex admin item save moon',t=>t==='Itens salvos: moon');
  assert((await response('/dex admin item list',t=>t.startsWith('Itens salvos:'))).includes('moon'));assert.equal(quantity('diamond_sword'),1);
  pass('Dedicated item permission saves full components and lists templates without consuming the held item; empty hand is rejected');
  await rcon.command(`clear ${PLAYER}`);await until(()=>bot.inventory.items().length===0);
  let menu=await open('/dex admin item give moon '+PLAYER);assert(data(menu.slots[11]).includes('Moon'));
  await lp('set shirokaidex.admin.items false');await messageAfter(()=>bot.clickWindow(13,0,0),t=>t.includes('não tem permissão'));await close();assert.equal(quantity('diamond_sword'),0);
  await lp('set shirokaidex.admin.items true');menu=await open('/dex admin item give moon '+PLAYER);await messageAfter(()=>bot.clickWindow(13,0,0),t=>t.includes('entregue e registrada'));await until(()=>quantity('diamond_sword')===1);
  assert.equal(componentKey(bot.inventory.items().find(i=>i.name==='diamond_sword')),originalComponents);
  pass('Confirmed manual give preserves name/lore/enchantment/damage/custom data and rechecks permission on an old confirmation');
  await lp('set shirokaidex.admin.reload true');
  const changed=JSON.parse(original);changed.rewards=['saved','deleted','after'].map(id=>({id,collection:'national',minimum_registered:1,items:[{saved_item:'moon',count:id==='saved'?2:1}]}));
  changed.rewards.push({id:'inline',collection:'national',minimum_registered:1,items:[{id:'minecraft:paper',count:1,payload_snbt:`{id:"minecraft:paper",count:1,components:{"minecraft:custom_name":'"Inline"'}}`}]});
  fs.writeFileSync(configFile,JSON.stringify(changed));await response('/dex admin reload',t=>t.includes('recarregada'));
  const recovery=fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8');assert(!recovery.includes('saved_item"'));assert(recovery.includes('payload_snbt'));
  const invalid=structuredClone(changed);invalid.rewards[3].items[0].payload_snbt='{id:"minecraft:paper",components:{"test:not_a_component":1}}';fs.writeFileSync(configFile,JSON.stringify(invalid));
  await response('/dex admin reload',t=>t.includes('última configuração válida'));assert.equal(fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8'),recovery);
  fs.writeFileSync(configFile,JSON.stringify(changed));
  pass('Registry-aware component validation rejects unknown components atomically and recovery backup embeds resolved templates');
  await rcon.command(`item replace entity ${PLAYER} weapon.mainhand with ${sword('Sun')} 1`);await until(()=>data(bot.heldItem).includes('Sun'));
  await response('/dex admin item save moon',t=>t==='Itens salvos: moon');await rcon.command(`clear ${PLAYER}`);await until(()=>bot.inventory.items().length===0);
  await rcon.command(`givepokemonother ${PLAYER} pikachu level=5`);await readyReward('saved');
  menu=await open('/dex rewards saved');assert(data(menu.slots[0]).includes('Moon'));assert(!data(menu.slots[0]).includes('Sun'));assert(data(menu.slots[0]).includes('Lunar lore'));await close();
  await claim('saved','entregue e registrada');await until(()=>quantity('diamond_sword')===2);
  for(const item of bot.inventory.items().filter(i=>i.name==='diamond_sword'))assert.equal(componentKey(item),originalComponents);
  pass('Changing the saved template cannot alter an already compiled reward; quantity override and components are preserved');
  await response('/dex admin item delete moon confirm',t=>t==='Itens salvos: moon');
  assert(!(await response('/dex admin item list',t=>t.startsWith('Itens salvos:'))).includes('moon'));
  await claim('deleted','entregue e registrada');await until(()=>quantity('diamond_sword')===3);
  await response('/dex admin reload',t=>t.includes('última configuração válida'));
  assert.equal(fs.readFileSync(path.join(root,'config/shirokaidex/config.last-good.json'),'utf8'),recovery);keepChanged=true;
  pass('Deletion keeps compiled rewards usable; missing template rejects reload and leaves an independent recovery snapshot');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'saved items integration',status:'failed',error:failure.message});}finally{if(!keepChanged&&!afterRestart)fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();const report=path.resolve(__dirname,listMode?'../../build/integration-reward-review-list.json':afterRestart?'../../build/integration-saved-items-restart.json':'../../build/integration-saved-items.json');fs.mkdirSync(path.dirname(report),{recursive:true});fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140',test_player:PLAYER,results},null,2));process.exitCode=exit;}})();
