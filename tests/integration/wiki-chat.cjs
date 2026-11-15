// Real Shirokai Core chat + packaged Dex, on a disposable loopback server only.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs'),{setTimeout:delay}=require('node:timers/promises');
const root=path.resolve(process.env.MC_TEST_RUN||'');assert(root.startsWith('/tmp/'));
let rcon,error;const bots=[],messages=new Map(),results=[];
async function until(fn,timeout=30000){const end=Date.now()+timeout;while(Date.now()<end){if(error)throw error;if(await fn())return;await delay(100);}throw Error('Wiki/chat condition timeout');}
async function send(bot,text){await delay(1300);bot.chat(text);}
async function open(bot,command){if(bot.currentWindow)bot.closeWindow(bot.currentWindow);await send(bot,command);await until(()=>bot.currentWindow);await delay(300);return bot.currentWindow;}
async function search(bot){await open(bot,'/wiki nacional');await bot.clickWindow(49,0,0);await until(()=>!bot.currentWindow);}
function pass(name){results.push({name,status:'passed'});console.log('PASS',name);}
(async()=>{try{
 rcon=await new Rcon().connect(25573,fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
 const info=await rcon.command('dexcaptureprobe modelid'),componentId=Number(info.match(/component id: ([0-9]+)/)[1]);
 const protocol=JSON.parse(JSON.stringify(require('minecraft-data')('1.21.1').protocol));const componentType=protocol.types.SlotComponentType;componentType[1].mappings[componentId]='cobblemon_pokemon_item';const slotComponent=protocol.types.SlotComponent;slotComponent[1][1].type[1].fields.cobblemon_pokemon_item='anonymousNbt';
 for(const name of ['DexSearchA','DexSearchB']){
  const bot=mineflayer.createBot({host:'127.0.0.1',port:25571,username:name,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{types:{SlotComponentType:componentType,SlotComponent:slotComponent},play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});bots.push(bot);messages.set(bot,[]);
  const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};let spawned=false;bot.once('spawn',()=>spawned=true);bot.on('messagestr',x=>messages.get(bot).push(x));bot.on('error',e=>error=e);await until(()=>spawned,45000);
 }
 const [actor,observer]=bots;
 await send(actor,'wiki_normal_before');await until(()=>messages.get(observer).some(x=>x.includes('wiki_normal_before')));pass('Shirokai Core normal chat reaches another player');
 await search(actor);const seen=messages.get(observer).length;await send(actor,'zorua');await delay(3000);assert(actor.currentWindow,'Pending search was consumed by chat instead of reopening wiki');assert(JSON.stringify(actor.currentWindow.slots[49].components).includes('zorua'));assert(!messages.get(observer).slice(seen).some(x=>x.includes('zorua')),'Search leaked into Core chat');pass('Wiki consumes zorua before Shirokai Core and reopens filtered menu without broadcast');
 await search(actor);const cancelled=messages.get(observer).length;await send(actor,'cancelar');await until(()=>messages.get(actor).some(x=>x.includes('Pesquisa cancelada')));assert(!actor.currentWindow);assert(!messages.get(observer).slice(cancelled).some(x=>x.includes('cancelar')));pass('Cancel is private and does not reopen wiki');
 await send(actor,'wiki_normal_after');await until(()=>messages.get(observer).some(x=>x.includes('wiki_normal_after')));pass('Core chat works after search/cancel');
 await search(actor);await send(observer,'other_player_normal');await until(()=>messages.get(actor).some(x=>x.includes('other_player_normal')));await send(actor,'pikachu');await until(()=>actor.currentWindow);assert(JSON.stringify(actor.currentWindow.slots[49].components).includes('pikachu'));pass('Pending search belongs only to its player; another player still chats normally');
 await search(actor);await open(actor,'/dex shiny');actor.closeWindow(actor.currentWindow);await send(actor,'wiki_abandoned');await until(()=>messages.get(observer).some(x=>x.includes('wiki_abandoned')));pass('Opening another menu abandons search and restores normal chat');
 }catch(e){process.exitCode=1;console.error(e);results.push({name:'wiki chat integration',status:'failed',error:e.message});}finally{for(const bot of bots)bot.quit();if(rcon)rcon.close();fs.writeFileSync(path.join(__dirname,'../../build/integration-wiki-chat.json'),JSON.stringify({timestamp:new Date().toISOString(),results},null,2)+'\n');}})();
