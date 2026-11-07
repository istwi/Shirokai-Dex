// Real-server wiki facet and >32-form pagination checks using a disposable species fixture.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const mineflayer=require('mineflayer');
const Rcon=require('./rcon.cjs');
const {setTimeout:delay}=require('node:timers/promises');
const PLAYER=process.env.MC_WIKI_FACETS_PLAYER||'DexFacets';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER));
const root=path.resolve(process.env.MC_TEST_RUN||path.join(__dirname,'../../run'));
const configFile=path.join(root,'config/shirokaidex/config.json');
const original=fs.readFileSync(configFile,'utf8');
assert.deepEqual(JSON.parse(original),JSON.parse(fs.readFileSync(path.join(__dirname,'../fixtures/wiki-facets/config.json'),'utf8')));
const results=[];
let bot,rcon,error,lastSend=0;
function pass(name){console.log('PASS',name);results.push({name,status:'passed'});}
async function until(predicate,timeout=15000){const end=Date.now()+timeout;while(Date.now()<end){if(error)throw error;if(await predicate())return;await delay(50);}throw new Error('Timed out waiting for facet/form state');}
async function send(text){await delay(Math.max(0,1100-(Date.now()-lastSend)));lastSend=Date.now();bot.chat(text);}
function response(text,matches){return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{bot.removeListener('messagestr',listener);reject(new Error('Missing response: '+text));},15000);function listener(value){if(matches(value)){clearTimeout(timer);bot.removeListener('messagestr',listener);resolve(value);}}bot.on('messagestr',listener);send(text).catch(reject);});}
async function close(){if(bot.currentWindow){bot.closeWindow(bot.currentWindow);await until(()=>!bot.currentWindow);}}
async function open(text){await close();await send(text);await until(()=>bot.currentWindow);return bot.currentWindow;}
async function click(slot){const before=bot.currentWindow;await bot.clickWindow(slot,0,0);await until(()=>bot.currentWindow&&bot.currentWindow!==before);return bot.currentWindow;}
function name(item){return item?.components?.find(component=>component.type==='custom_name')?.data?.value||'';}
function data(item){return JSON.stringify(item?.components||[]);}
function title(menu){return JSON.stringify(menu.title);}
function species(menu){return menu.slots.slice(0,18).filter(item=>item?.name==='book').map(item=>name(item));}
function forms(menu){return menu.slots.slice(0,18).filter(item=>item?.name==='paper').map(item=>name(item));}
function signature(menu){return menu.slots.map(item=>item?[item.type,item.count,data(item)]:null);}
async function choose(slot,value){let menu=await click(slot);for(let i=0;i<20;i++){const index=menu.slots.slice(0,18).findIndex(item=>name(item)===value);if(index>=0)return click(index);assert.equal(menu.slots[26]?.name,'arrow','Missing facet option '+value);menu=await click(26);}throw new Error('Facet picker exceeded bounded test pages');}
async function lp(text){const result=await rcon.command(`lp user ${bot.player.uuid} permission ${text}`);assert(!result.includes('Another command is being executed'),result);await delay(1200);}
async function denyClick(slot){const menu=bot.currentWindow;const denied=new Promise((resolve,reject)=>{const timer=setTimeout(()=>{bot.removeListener('messagestr',listener);reject(new Error('Stale facet/form action bypassed permission'));},10000);function listener(value){if(value.includes('não tem permissão')){clearTimeout(timer);bot.removeListener('messagestr',listener);resolve();}}bot.on('messagestr',listener);});await bot.clickWindow(slot,0,0);await denied;await until(()=>bot.currentWindow===menu&&!menu.selectedItem);}
async function main(){
  assert(process.env.MC_TEST_RCON_PASSWORD_FILE);
  rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT||25573),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
  bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT||25571),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,
    customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
  const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
  let spawned=false;bot.once('spawn',()=>{spawned=true;});bot.on('error',value=>{error=value;});bot.on('kicked',value=>{error=new Error(JSON.stringify(value));});await until(()=>spawned,45000);
  await rcon.command(`deop ${PLAYER}`);
  const baseline=await response('/dex variants status',text=>text.startsWith('Variantes:'));assert(baseline.includes('0/1'),'Use a fresh disposable player');
  let menu=await open('/dex variants wiki dex_test:variants');assert.equal(menu.type,'minecraft:generic_9x4');
  menu=await click(25);assert.deepEqual(forms(menu),['Sem filtro','normal','water']);menu=await click(23);
  menu=await click(27);assert.deepEqual(forms(menu),['Sem filtro','dex_fixture','legendary','mythical']);menu=await click(23);
  assert(data(menu.slots[21]).includes('dex_test:variants'));
  menu=await open('/dex national wiki dex_test:variants');menu=await click(25);assert.equal(menu.slots[26].name,'arrow');
  const first=forms(menu);menu=await click(26);assert(!forms(menu).some(value=>first.includes(value)));menu=await click(18);assert.deepEqual(forms(menu),first);
  pass('Dynamic type/category pickers are collection-scoped, preserve query and paginate all type choices');
  menu=await open('/dex variants wiki dex_test:variants');menu=await choose(25,'water');menu=await choose(27,'legendary');
  assert.equal(species(menu).length,0);assert.equal(menu.slots[13].name,'paper');
  menu=await choose(27,'mythical');assert.equal(species(menu).length,1);
  menu=await click(20);assert(data(menu.slots[25]).includes('water'));assert(data(menu.slots[27]).includes('mythical'));
  menu=await click(0);assert(data(menu.slots[13]).includes('Formas elegíveis: 40'));
  menu=await click(15);assert(title(menu).includes('(40)'));assert(!forms(menu).includes('normal'));menu=await click(23);menu=await click(18);
  assert(data(menu.slots[21]).includes('dex_test:variants'));assert(data(menu.slots[25]).includes('water'));assert(data(menu.slots[27]).includes('mythical'));
  pass('Type and label intersect on the same form and survive ordering/details/back navigation');
  menu=await open('/dex variants wiki dex_test:variants');menu=await click(0);menu=await click(15);
  const visited=[];while(true){visited.push(...forms(menu));if(!menu.slots[26])break;menu=await click(26);}
  assert.equal(visited.length,41);assert.equal(new Set(visited).size,41);assert(visited.includes('normal'));assert(visited.includes('variant_39'));assert.deepEqual(visited,[...visited].sort());
  menu=await click(23);menu=await click(18);assert(data(menu.slots[21]).includes('dex_test:variants'));
  await close();assert.equal(await response('/dex variants status',text=>text.startsWith('Variantes:')),baseline);
  pass('All 41 real registered forms are reachable beyond the old 32-line limit without changing official progress');
  await rcon.command(`givepokemonother ${PLAYER} dex_test:variants level=5`);
  await until(async()=>(await response('/dex variants status',text=>text.startsWith('Variantes:'))).includes('1/1'));
  menu=await open('/dex variants wiki');menu=await choose(25,'water');assert(data(menu.slots[0]).includes('Obtida: Não'));
  menu=await click(19);assert.equal(species(menu).length,0);menu=await click(19);assert.equal(species(menu).length,0);
  menu=await click(19);assert.equal(species(menu).length,1);
  menu=await open('/dex water_forms wiki');menu=await click(25);assert.deepEqual(forms(menu),['Sem filtro','water']);menu=await click(23);
  menu=await click(0);menu=await click(15);assert(!forms(menu).includes('normal'));
  pass('Owned normal form does not mark water forms seen/owned and collection criteria cannot be bypassed');
  await close();await rcon.command(`give ${PLAYER} minecraft:paper 2`);
  menu=await open('/dex variants wiki');menu=await click(0);menu=await click(15);const before=signature(menu);
  for(const [button,mode]of[[0,0],[1,0],[0,1],[0,2],[0,4],[1,4]]){await bot.clickWindow(0,button,mode);await until(()=>!menu.selectedItem&&JSON.stringify(signature(menu))===JSON.stringify(before));}
  const ownSlot=menu.slots.findIndex((item,index)=>index>=36&&item?.name==='paper');assert(ownSlot>=36);
  await bot.clickWindow(ownSlot,0,1);await until(()=>!menu.selectedItem&&JSON.stringify(signature(menu))===JSON.stringify(before));
  await close();assert.match(await rcon.command(`clear ${PLAYER} minecraft:paper 0`),/Found 2 matching item/);
  pass('Four-row form menus block pickup/right/shift/hotbar/discard and preserve real player inventory');
  await response('/dex locked wiki',text=>text.includes('não tem permissão'));await lp('set shirokaidex.collection.locked true');
  menu=await open('/dex locked wiki');menu=await click(25);const water=menu.slots.findIndex(item=>name(item)==='water');assert(water>=0&&water<18);
  await lp('set shirokaidex.collection.locked false');await denyClick(water);
  pass('An already open facet picker rechecks permission before applying an option');
  await close();await lp('set shirokaidex.collection.locked true');menu=await open('/dex locked wiki');menu=await click(0);menu=await click(15);
  await lp('set shirokaidex.collection.locked false');await denyClick(26);
  pass('An already open form-page button rechecks revocation without handing out paper icons');
  await close();await lp('set shirokaidex.admin.reload true');menu=await open('/dex variants wiki');menu=await click(0);menu=await click(15);menu=await click(26);menu=await click(26);
  const config=JSON.parse(original);config.language='en_us';config.collections.find(value=>value.id==='variants').criteria.forms=['normal'];fs.writeFileSync(configFile,JSON.stringify(config));
  await response('/dex admin reload',text=>text.includes('reloaded'));menu=await click(18);assert.deepEqual(forms(menu),['normal']);assert(title(menu).includes('[1/1]'));assert(title(menu).includes('(1)'));
  menu=await click(23);assert(data(menu.slots[13]).includes('Eligible forms: 1'));await close();
  fs.writeFileSync(configFile,original);await response('/dex admin reload',text=>text.includes('recarregada'));
  menu=await open('/dex variants wiki');menu=await click(0);assert(data(menu.slots[13]).includes('Formas elegíveis: 41'));await close();
  pass('Reload revalidates stale form callbacks, clamps the page, changes language and restores all forms safely');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'wiki facets integration',status:'failed',error:failure.message});}finally{fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();const report=path.resolve(__dirname,'../../build/integration-wiki-facets.json');fs.mkdirSync(path.dirname(report),{recursive:true});fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140',test_player:PLAYER,results},null,2));process.exitCode=exit;}})();
