// Isolated production-server validation; use tests/fixtures/submenus/config.json and synthetic journal states.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs');
const {setTimeout:delay}=require('node:timers/promises');
const PLAYER=process.env.MC_SUBMENU_PLAYER||'DexNavLayout';
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
async function main(){
 rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
 bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
 const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
 let spawned=false;bot.once('spawn',()=>spawned=true);bot.on('error',v=>error=v);bot.on('kicked',v=>error=new Error(JSON.stringify(v)));await until(()=>spawned,45000);await rcon.command(`deop ${PLAYER}`);
 await rcon.command(`givepokemonother ${PLAYER} pikachu level=5`);await delay(6500);
 let menu=await open('/dex');assert(title(menu).includes('✦'));menu=await click(20);assert.equal(menu.slots[26].name,'barrier');
 await bot.clickWindow(26,0,1);await delay(250);assert.equal(bot.currentWindow,menu);assert(!menu.selectedItem);
 menu=await click(13);assert.equal(menu.slots[26].name,'arrow');assert.equal(menu.slots[18].name,'barrier');assert(data(menu.slots[13]).includes('▱'));
 menu=await click(26);assert.equal(menu.slots[26].name,'barrier');
 pass('Selector/collection navigation remaps callbacks with icons; progress bar and click protection remain intact');
 menu=await open('/dex national wiki');assert.equal(menu.slots[35].name,'barrier');assert.equal(menu.slots[34].name,'arrow');const first=data(menu.slots[0]);menu=await click(34);assert.notEqual(data(menu.slots[0]),first);menu=await click(29);assert.equal(data(menu.slots[0]),first);
 menu=await click(33);assert.equal(menu.slots[33].name,'arrow');menu=await click(33);assert.equal(menu.slots[35].name,'barrier');
 menu=await click(32);assert.equal(menu.slots[33].name,'arrow');menu=await click(33);
 pass('Wiki pagination and type/category pickers use configured navigation slots without losing content');
 menu=await open('/dex national wiki cobblemon:vulpix');menu=await click(0);assert.equal(menu.slots[34].name,'arrow');assert.equal(menu.slots[35].name,'barrier');menu=await click(15);assert.equal(menu.slots[33].name,'arrow');menu=await click(33);menu=await click(16);assert.equal(menu.slots[32].name,'paper');menu=await click(33);assert.equal(menu.slots[35].name,'barrier');
 pass('Details, forms and spawn summaries preserve return paths and fixed payload cards');
 menu=await open('/dex rewards');assert(!menu.slots[21]);assert.equal(menu.slots[26].name,'barrier');assert.equal(menu.slots[22].name,'arrow');const grid=data(menu.slots[0]);menu=await click(22);assert.notEqual(data(menu.slots[0]),grid);menu=await click(18);assert.equal(data(menu.slots[0]),grid);
 pass('Reward grid swaps close/next safely, keeps paging, and honors footer=false');
 for(const [id,label] of [['completed','Resgate concluído'],['review','Revisão necessária'],['cancelled','Resgate cancelado'],['changed','Recompensa alterada'],['locked','Marco bloqueado'],['recovered','Revisão necessária']]){
  menu=await open('/dex rewards '+id);assert(data(menu.slots[26]).includes(label),id);assert.notEqual(menu.slots[25]?.name,'arrow',id+' must not offer claim');const before=quantity('diamond');await bot.clickWindow(25,0,0);await delay(100);assert.equal(bot.currentWindow,menu);assert.equal(quantity('diamond'),before);
 }
 menu=await open('/dex rewards review');const operation=await message(()=>bot.clickWindow(26,0,0),text=>/[a-f0-9]{8}-[a-f0-9-]{27}/.test(text));assert(operation.includes('-'));
 pass('Completed/review/cancelled/changed/locked/recovered states suppress claim; relocated status displays operation UUID');
 menu=await open('/dex rewards pending');assert(data(menu.slots[26]).includes('Resgate reservado'));assert.equal(menu.slots[25].name,'arrow');
 menu=await open('/dex rewards available');assert(data(menu.slots[0]).includes('Prêmio intacto'));assert(data(menu.slots[0]).includes('Lore original'));assert(!data(menu.slots[0]).includes('✦'));
 menu=await click(25);assert.equal(menu.slots[13].name,'lime_dye');assert.equal(menu.slots[15].name,'paper');assert(data(menu.slots[11]).includes('Prêmio intacto'));assert(!data(menu.slots[11]).includes('✦'));assert.equal(menu.slots[26].name,'arrow');assert.equal(menu.slots[18].name,'barrier');
 menu=await click(26);assert(data(menu.slots[26]).includes('Disponível'));menu=await click(25);
 await message(()=>bot.clickWindow(13,0,0),text=>text.includes('entregue e registrada'));await until(()=>quantity('diamond')===1);menu=await open('/dex rewards available');assert(data(menu.slots[26]).includes('Resgate concluído'));assert.notEqual(menu.slots[25]?.name,'arrow');assert.equal(quantity('diamond'),1);
 menu=await open('/dex rewards');assert(data(menu.slots[0]).includes('Resgate concluído'));
 pass('Custom item preview/confirmation preserves metadata; one real claim updates preview/grid and hides repeat claim');
 if(process.env.MC_SUBMENU_MODE==='without-luckperms')return;
 await rcon.command(`lp user ${bot.player.uuid} permission set shirokaidex.admin.reload true`);await delay(1500);
 const invalid=JSON.parse(original);invalid.menus.reward.slots.claim=0;await reload(invalid,true);menu=await open('/dex rewards available');assert(data(menu.slots[26]).includes('Resgate concluído'));assert.equal(menu.slots[23].name,'arrow');
 const defaults=JSON.parse(original);delete defaults.menus;await reload(defaults);menu=await open('/dex rewards available');assert(data(menu.slots[19]).includes('Resgate concluído'));assert.equal(menu.slots[18].name,'arrow');assert.equal(menu.slots[22].name,'barrier');assert(!menu.slots[24]);
 await reload(JSON.parse(original));menu=await open('/dex rewards available');assert(data(menu.slots[26]).includes('Resgate concluído'));assert.equal(quantity('diamond'),1);
 pass('Invalid reload preserves active layout; omission restores backward-compatible defaults; valid reload restores configured layout without redelivery');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'submenu integration',status:'failed',error:failure.message});}finally{fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();fs.mkdirSync(path.join(__dirname,'../../build'),{recursive:true});fs.writeFileSync(path.join(__dirname,'../../build/integration-submenus.json'),JSON.stringify({target:'Packaged Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3; reload mode '+(process.env.MC_SUBMENU_MODE||'with-luckperms'),player:PLAYER,results},null,2));process.exitCode=exit;}})();
