// Real Shirokai Core chat + packaged Dex, on a disposable loopback server only.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs'),{setTimeout:delay}=require('node:timers/promises');
const root=path.resolve(process.env.MC_TEST_RUN||'');assert(root.startsWith('/tmp/'));
let rcon,error;const bots=[],messages=new Map(),results=[],components=[];
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
  const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};let spawned=false;bot.once('spawn',()=>spawned=true);bot.on('messagestr',x=>messages.get(bot).push(x));bot.on('message',message=>components.push({bot,text:message.toString(),json:message.json}));bot.on('error',e=>error=e);await until(()=>spawned,45000);
 }
 const [actor,observer]=bots;
 function styled(entry){
  assert(entry.text.startsWith('✦ SHIROKAI » '),entry.text);
  let branded=false,body=false;
  function visit(node,inherited={}){
   if(typeof node==='string')node={text:node};const style={...inherited,...node};
   if(node.text){const isBrand=node.text==='✦ SHIROKAI';if(isBrand)branded=true;else if(node.text.trim()!=='»')body=true;
    const colors={white:'#ffffff',gray:'#aaaaaa'};assert.equal(colors[style.color]||String(style.color).toLowerCase(),isBrand?'#ffffff':'#aaaaaa',JSON.stringify(node));assert.equal(Boolean(style.bold),isBrand);assert(!style.italic);
   }
   for(const child of node.extra||[])visit(child,style);
  }
  visit(entry.json);assert(branded&&body);
 }
 async function response(command,needle){const start=components.length;await send(actor,command);await until(()=>components.slice(start).some(x=>x.bot===actor&&x.text.includes(needle)));const entry=components.slice(start).find(x=>x.bot===actor&&x.text.includes(needle));styled(entry);return entry;}
 await search(actor);await until(()=>components.some(x=>x.bot===actor&&x.text.includes('Escreva no chat')));styled(components.find(x=>x.bot===actor&&x.text.includes('Escreva no chat')));await response('cancelar','Pesquisa cancelada');pass('Wiki prompt/cancellation have white bold brand and regular gray text despite legacy message colors');
 await rcon.command('lp user '+actor.player.uuid+' permission set shirokaidex.admin.reload true');await delay(1000);await response('/dex admin reload','recarregada');pass('Command success uses the same chat style');
 const file=path.join(root,'config/shirokaidex/menus/wiki_nacional.json'),original=fs.readFileSync(file,'utf8');try{fs.writeFileSync(file,'{broken');await response('/dex admin reload','Configuração inválida');}finally{fs.writeFileSync(file,original);}await response('/dex admin reload','recarregada');pass('Command failure stays gray and keeps white branding despite vanilla error wrapping');
 await rcon.command('givepokemonother DexSearchA pikachu level=5');await until(()=>components.some(x=>x.bot===actor&&x.text.includes('Recompensa desbloqueada')),30000);styled(components.find(x=>x.bot===actor&&x.text.includes('Recompensa desbloqueada')));pass('Actual Pokedex unlock notification reaches chat with requested styling');
 const menu=await open(actor,'/dex nacional');assert(!JSON.stringify(menu.title).includes('SHIROKAI'));assert(!JSON.stringify(menu.slots.slice(0,menu.inventoryStart)).includes('✦ SHIROKAI'));actor.closeWindow(menu);pass('Menu title/items keep their presentation without chat prefix');
 await send(actor,'style_normal_chat');await until(()=>components.some(x=>x.bot===observer&&x.text.includes('style_normal_chat')));assert(!components.find(x=>x.bot===observer&&x.text.includes('style_normal_chat')).text.startsWith('✦ SHIROKAI'));pass('Shirokai Core normal player chat retains its own formatting');
 }catch(e){process.exitCode=1;console.error(e);results.push({name:'chat style integration',status:'failed',error:e.message});}finally{for(const bot of bots)bot.quit();if(rcon)rcon.close();fs.writeFileSync(path.join(__dirname,'../../build/integration-chat-style.json'),JSON.stringify({timestamp:new Date().toISOString(),results},null,2)+'\n');}})();
