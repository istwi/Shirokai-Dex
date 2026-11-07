// Isolated production-server validation; use tests/fixtures/submenus/config.json and synthetic journal states.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs');
const {setTimeout:delay}=require('node:timers/promises');
const PLAYER=process.env.MC_SUBMENU_PLAYER||'DexCurrency';
const root=path.resolve(process.env.MC_TEST_RUN);assert(root.startsWith('/tmp/'),'Disposable /tmp server required');
const configFile=path.join(root,'config/shirokaidex/config.json'),original=fs.readFileSync(configFile,'utf8');
let bot,rcon,error,lastSend=0;const results=[];
function pass(name){console.log('PASS',name);results.push({name,status:'passed'});}
async function until(predicate,timeout=15000){const end=Date.now()+timeout;while(Date.now()<end){if(error)throw error;if(await predicate())return;await delay(50);}throw new Error('Timed out waiting for submenu');}
async function send(text){await delay(Math.max(0,1100-(Date.now()-lastSend)));lastSend=Date.now();bot.chat(text);}
async function close(){if(bot.currentWindow){bot.closeWindow(bot.currentWindow);await until(()=>!bot.currentWindow);}}
async function open(text){await close();await send(text);await until(()=>bot.currentWindow);return bot.currentWindow;}
async function click(slot){const before=bot.currentWindow;await bot.clickWindow(slot,0,0);await until(()=>bot.currentWindow&&bot.currentWindow!==before);return bot.currentWindow;}
function data(item){return JSON.stringify(item?.components||item?.nbt||[]);}
function title(menu){return JSON.stringify(menu.title);}
function quantity(name){return bot.inventory.items().filter(i=>i.name===name).reduce((n,i)=>n+i.count,0);}
function message(action,match){return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{bot.removeListener('messagestr',listener);reject(new Error('Missing message'));},15000);function listener(text){if(match(text)){clearTimeout(timer);bot.removeListener('messagestr',listener);resolve(text);}}bot.on('messagestr',listener);Promise.resolve(action()).catch(reject);});}
async function reload(json,invalid=false){fs.writeFileSync(configFile,JSON.stringify(json));await close();await message(()=>send('/dex admin reload'),text=>text.includes(invalid?'Configuração inválida':'recarregada'));}
async function ready(id){await until(async()=>{const menu=await open('/dex rewards '+id);const ok=menu.slots[24]?.name==='arrow';await close();return ok;},20000);}
async function claim(id,expected){let menu=await open('/dex rewards '+id);assert.equal(menu.slots[24].name,'arrow');menu=await click(24);assert.equal(menu.slots[13].name,'lime_dye');return message(()=>bot.clickWindow(13,0,0),text=>text.includes(expected));}
function accountFile(){const owner=bot.player.uuid;return path.join(root,'config/impactor/economy/accounts/users',owner.slice(0,2),owner+'.conf');}
function balances(){const file=accountFile();return fs.existsSync(file)?JSON.parse(fs.readFileSync(file,'utf8')):{};}
async function main(){
 rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
 bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
 const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
 let spawned=false;bot.once('spawn',()=>spawned=true);bot.on('error',v=>error=v);bot.on('kicked',v=>error=new Error(JSON.stringify(v)));await until(()=>spawned,45000);await rcon.command(`deop ${PLAYER}`);
 if(process.env.MC_CURRENCY_MODE==='after-restart'){
  assert.equal(balances()['impactor:dollars'],15);assert.equal(balances()['impactor:tokens'],4);assert.equal(quantity('diamond'),1);
  for(const [id,label] of [['cash','Resgate concluído'],['tokens','Resgate concluído'],['mixed','Resgate concluído'],['uncertain','Revisão necessária']]){const menu=await open('/dex rewards '+id);assert(data(menu.slots[19]).includes(label));assert(!menu.slots[24]);await bot.clickWindow(24,0,0);await delay(100);assert.equal(bot.currentWindow,menu);}
  assert.equal(balances()['impactor:dollars'],15);assert.equal(balances()['impactor:tokens'],4);assert.equal(quantity('diamond'),1);
  pass('Native balances/items and completed/review states survive restart without redelivery');return;
 }
 await rcon.command(`givepokemonother ${PLAYER} pikachu level=5`);await ready('cash');
 let menu=await open('/dex rewards cash');assert.equal(menu.slots[0].name,'gold_ingot');assert(data(menu.slots[0]).includes('12.5'));menu=await click(24);assert.equal(menu.slots[11].name,'gold_ingot');assert(data(menu.slots[15]).includes('impactor:dollars'));
 pass('Currency-only preview and confirmation show exact currency/amount without item or Pokemon assumptions');
 if(process.env.MC_CURRENCY_MODE==='without-impactor'){
  await message(()=>bot.clickWindow(13,0,0),text=>text.includes('Impactor 5.3.5 indisponível'));assert.equal(quantity('diamond'),0);assert(!fs.existsSync(accountFile()));
  pass('Missing optional Impactor does not prevent startup or create an account/delivery');return;
 }
 await close();assert.equal(balances()['impactor:dollars']||0,0);assert.equal(balances()['impactor:tokens']||0,0);
 await claim('cash','entregue e registrada');assert.equal(balances()['impactor:dollars'],12.5);assert.equal(balances()['impactor:tokens']||0,0);
 await claim('tokens','entregue e registrada');assert.equal(balances()['impactor:tokens'],3);assert.equal(balances()['impactor:dollars'],12.5);
 pass('Real Impactor dollars/tokens deposits are independently saved with exact amounts and no conversion');
 const before=balances();for(const id of ['missing','precision']){await claim(id,'precisão incompatível');assert.deepEqual(balances(),before);assert.equal(quantity('netherite_ingot'),0);}
 pass('Missing currency and incompatible token precision abort mixed plans before any item or monetary effect');
 await claim('mixed','entregue e registrada');assert.equal(quantity('diamond'),1);assert.equal(balances()['impactor:dollars'],15);assert.equal(balances()['impactor:tokens'],4);
 for(const id of ['cash','tokens','mixed']){menu=await open('/dex rewards '+id);assert(data(menu.slots[19]).includes('Resgate concluído'));assert(!menu.slots[24]);}
 assert.equal(quantity('diamond'),1);assert.equal(balances()['impactor:dollars'],15);assert.equal(balances()['impactor:tokens'],4);
 pass('Mixed item+two-currency plan completes once with journal-backed closed previews');
 await close();const file=accountFile(),backup=file+'.currency-test-backup';assert(fs.statSync(file).isFile());fs.renameSync(file,backup);fs.mkdirSync(file);
 try{await claim('uncertain','bloqueada para revisão');}finally{fs.rmdirSync(file);fs.renameSync(backup,file);}
 menu=await open('/dex rewards uncertain');assert(data(menu.slots[19]).includes('Revisão necessária'));assert(!menu.slots[24]);assert.equal(balances()['impactor:dollars'],15);assert.equal(balances()['impactor:tokens'],4);assert.equal(quantity('diamond'),1);
 pass('Real suppressed native JSON write failure is detected by readback and blocks uncertain credit for manual review');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'currency integration',status:'failed',error:failure.message});}finally{fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();fs.mkdirSync(path.join(__dirname,'../../build'),{recursive:true});fs.writeFileSync(path.join(__dirname,'../../build/integration-currency-'+(process.env.MC_CURRENCY_MODE||'main')+'.json'),JSON.stringify({target:'Packaged Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / Impactor 5.3.5 native JSON',player:PLAYER,results},null,2));process.exitCode=exit;}})();
