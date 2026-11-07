// Isolated production-server validation; use tests/fixtures/menu-appearance/config.json and synthetic journal states.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs');
const {setTimeout:delay}=require('node:timers/promises');
const PLAYER=process.env.MC_SUBMENU_PLAYER||'DexNavSkin';
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
function plainTitle(menu){
 let value=menu.title;if(value?.type)value=require('prismarine-nbt').simplify(value);
 if(typeof value==='string'){try{value=JSON.parse(value);}catch{return value;}}
 function text(v){if(typeof v==='string')return v;if(!v)return '';return (v.text||'')+(v.extra||[]).map(text).join('');}
 return text(value);
}
async function main(){
 rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
 bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
 const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
 let spawned=false;bot.once('spawn',()=>spawned=true);bot.on('error',v=>error=v);bot.on('kicked',v=>error=new Error(JSON.stringify(v)));await until(()=>spawned,45000);await rcon.command(`deop ${PLAYER}`);
 await rcon.command(`givepokemonother ${PLAYER} pikachu level=5`);await delay(6500);
 let menu=await open('/dex');menu=await click(20);assert.equal(menu.type,'minecraft:generic_9x5');assert(plainTitle(menu).includes(PLAYER));assert.equal(menu.slots[44].name,'barrier');
 menu=await click(13);assert.equal(menu.type,'minecraft:generic_9x4');assert.equal(menu.slots[35].name,'barrier');assert(data(menu.slots[13]).includes('▱'));menu=await click(27);assert.equal(menu.type,'minecraft:generic_9x5');
 pass('Expanded selector/collection windows preserve content, progress, player title and callbacks');
 menu=await open('/dex national wiki');assert.equal(menu.type,'minecraft:generic_9x6');assert(plainTitle(menu).includes(PLAYER));assert.equal(menu.slots[28].name,'cyan_stained_glass_pane');let first=data(menu.slots[0]);menu=await click(52);assert.notEqual(data(menu.slots[0]),first);menu=await click(45);assert.equal(data(menu.slots[0]),first);
 for(const slot of [51,44]){menu=await click(slot);assert.equal(menu.type,'minecraft:generic_9x5');assert(plainTitle(menu).includes(PLAYER));menu=await click(40);assert.equal(menu.type,'minecraft:generic_9x6');}
 menu=await open('/dex national wiki cobblemon:vulpix');menu=await click(0);assert.equal(menu.type,'minecraft:generic_9x6');menu=await click(15);assert.equal(menu.type,'minecraft:generic_9x5');menu=await click(40);menu=await click(16);assert.equal(menu.type,'minecraft:generic_9x6');assert.equal(menu.slots[50].name,'paper');menu=await click(49);assert.equal(menu.slots[53].name,'barrier');
 pass('Expanded wiki/pickers/details/forms/spawns retain paging and scope cards across different sizes');
 menu=await open('/dex rewards');assert.equal(menu.type,'minecraft:generic_9x6');assert(plainTitle(menu).includes(PLAYER));assert.equal(menu.slots[53].name,'oak_door');assert(data(menu.slots[53]).includes(PLAYER));assert(data(menu.slots[53]).includes('Retorno personalizado'));assert.equal(menu.slots[28].name,'cyan_stained_glass_pane');first=data(menu.slots[0]);menu=await click(52);assert.notEqual(data(menu.slots[0]),first);menu=await click(45);assert.equal(data(menu.slots[0]),first);
 await bot.clickWindow(53,0,1);await delay(150);assert.equal(bot.currentWindow,menu);assert.equal(quantity('oak_door'),0);
 pass('Reward grid title, palette, custom close icon/name/lore and safe paging survive expansion without extraction');
 for(const [id,label] of [['completed','Resgate concluído'],['review','Revisão necessária'],['cancelled','Resgate cancelado'],['changed','Recompensa alterada'],['locked','Marco bloqueado'],['recovered','Revisão necessária']]){
  menu=await open('/dex rewards '+id);assert.equal(menu.type,'minecraft:generic_9x6');assert.equal(menu.slots[49].name,'compass');assert(data(menu.slots[49]).includes(label));assert(data(menu.slots[49]).includes('Estado'));assert(data(menu.slots[49]).includes(PLAYER));assert.notEqual(menu.slots[52]?.name,'emerald');await bot.clickWindow(52,0,0);await delay(80);assert.equal(bot.currentWindow,menu);assert.equal(quantity('diamond'),0);
 }
 pass('Custom status labels retain actual journal states; styled absent claim cannot enable locked or terminal operations');
 menu=await open('/dex rewards available');assert.equal(menu.slots[52].name,'emerald');assert.equal(menu.slots[53].name,'red_dye');assert.equal(menu.slots[28].name,'orange_stained_glass_pane');assert(data(menu.slots[0]).includes('Prêmio intacto'));assert(!data(menu.slots[0]).includes(PLAYER));assert(!data(menu.slots[0]).includes('Estado'));
 menu=await click(52);assert.equal(menu.type,'minecraft:generic_9x4');assert(plainTitle(menu).includes(PLAYER));assert.equal(menu.slots[13].name,'lime_dye');assert(data(menu.slots[11]).includes('Prêmio intacto'));assert(data(menu.slots[11]).includes('Lore original'));assert(!data(menu.slots[11]).includes(PLAYER));menu=await click(27);menu=await click(52);
 await message(()=>bot.clickWindow(13,0,0),text=>text.includes('entregue e registrada'));await until(()=>quantity('diamond')===1);menu=await open('/dex rewards available');assert(data(menu.slots[49]).includes('Resgate concluído'));assert.notEqual(menu.slots[52]?.name,'emerald');assert(data(bot.inventory.items().find(i=>i.name==='diamond')).includes('Prêmio intacto'));assert(!data(bot.inventory.items().find(i=>i.name==='diamond')).includes(PLAYER));
 pass('Themed claim keeps separate confirmation and pristine prize metadata; exactly one item delivered');
 await rcon.command(`lp user ${bot.player.uuid} permission set shirokaidex.admin.reload true`);await delay(1400);
 const bad=JSON.parse(original);bad.menus.reward.buttons.status.icon='missing:icon';await reload(bad,true);menu=await open('/dex rewards available');assert.equal(menu.slots[49].name,'compass');assert(data(menu.slots[49]).includes('Resgate concluído'));
 const defaults=JSON.parse(original);delete defaults.menus;await reload(defaults);menu=await open('/dex rewards available');assert.equal(menu.type,'minecraft:generic_9x3');assert.equal(menu.slots[19].name,'emerald');assert(!menu.slots[24]);assert(!plainTitle(menu).includes(PLAYER));
 await reload(JSON.parse(original));menu=await open('/dex rewards available');assert.equal(menu.type,'minecraft:generic_9x6');assert.equal(menu.slots[49].name,'compass');assert.equal(quantity('diamond'),1);
 pass('Unknown native icon rejects whole reload; removing/restoring style settings returns default/expanded UI without redelivery');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'appearance integration',status:'failed',error:failure.message});}finally{fs.writeFileSync(configFile,original);if(bot)bot.quit();if(rcon)rcon.close();fs.mkdirSync(path.join(__dirname,'../../build'),{recursive:true});fs.writeFileSync(path.join(__dirname,'../../build/integration-menu-appearance.json'),JSON.stringify({target:'Packaged Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140',player:PLAYER,results},null,2));process.exitCode=exit;}})();
