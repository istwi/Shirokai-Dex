// Packaged disposable server with the separate capture probe (model component ID and optional public skin fixtures).
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs'),{setTimeout:delay}=require('node:timers/promises');
const root=path.resolve(process.env.MC_TEST_RUN||'');assert(root.startsWith('/tmp/'));
let bot,rcon,error;const results=[];
async function until(fn){const end=Date.now()+45000;while(Date.now()<end){if(error)throw error;if(await fn())return;await delay(100);}throw Error('Menu readiness timeout');}
async function open(text){if(bot.currentWindow)bot.closeWindow(bot.currentWindow);await delay(1200);bot.chat(text);await until(()=>bot.currentWindow);await delay(300);return bot.currentWindow;}
function pass(name){results.push({name,status:'passed'});console.log('PASS',name);}
(async()=>{try{
 rcon=await new Rcon().connect(25573,fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
 const info=await rcon.command('dexcaptureprobe modelid'),componentId=Number(info.match(/component id: ([0-9]+)/)[1]);
 const protocol=JSON.parse(JSON.stringify(require('minecraft-data')('1.21.1').protocol));const componentType=protocol.types.SlotComponentType;componentType[1].mappings[componentId]='cobblemon_pokemon_item';const slotComponent=protocol.types.SlotComponent;slotComponent[1][1].type[1].fields.cobblemon_pokemon_item='anonymousNbt';
 const skinFixture=process.env.MC_HEAD_SKIN_FIXTURE?JSON.parse(fs.readFileSync(process.env.MC_HEAD_SKIN_FIXTURE,'utf8')):null;
 if(skinFixture)assert.notEqual(skinFixture.DexHeadA.value,skinFixture.DexHeadB.value);
 for(const name of ['DexHeadA','DexHeadB']){
  error=null;bot=mineflayer.createBot({host:'127.0.0.1',port:25571,username:name,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{types:{SlotComponentType:componentType,SlotComponent:slotComponent},play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
  const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};let spawned=false;bot.once('spawn',()=>spawned=true);bot.on('error',e=>error=e);await until(()=>spawned);
  if(skinFixture)assert.match(await rcon.command('dexcaptureprobe headskin'),/updated: 1/);
  for(const dex of ['nacional','special','shiny']){
   const expected=await open('/wiki '+dex),title=JSON.stringify(expected.title);const menu=await open('/dex '+dex);assert.equal(menu.slots[10].name,'player_head');const profile=menu.slots[10].components.find(x=>x.type==='profile');assert(profile,'Missing head profile');assert(JSON.stringify(profile).includes(name),'Head must carry this viewer profile');if(skinFixture){const data=JSON.stringify(profile);assert(data.includes(skinFixture[name].value),'Missing exact skin texture');assert(data.includes(skinFixture[name].signature),'Missing skin signature');}assert.equal(menu.slots[19].name,'book');assert.equal(menu.slots[28].name,'barrier');
   await bot.clickWindow(19,0,0);await until(()=>bot.currentWindow&&bot.currentWindow!==menu);assert.equal(JSON.stringify(bot.currentWindow.title),title);assert.equal(bot.currentWindow.type,'minecraft:generic_9x6');pass(name+': own head'+(skinFixture?' with exact signed texture':'')+' and '+dex+' wiki at slot19; close preserved');
  }
  assert(!bot.inventory.items().some(x=>['player_head','book','barrier'].includes(x.name)));bot.quit();bot=null;await delay(500);
 }
 }catch(e){process.exitCode=1;console.error(e);if(bot?.currentWindow)console.error(JSON.stringify(bot.currentWindow.slots[10]));results.push({name:'head/wiki integration',status:'failed',error:e.message});}finally{if(bot)bot.quit();if(rcon)rcon.close();fs.writeFileSync(path.join(__dirname,'../../build/integration-head-wiki.json'),JSON.stringify({timestamp:new Date().toISOString(),results},null,2)+'\n');}})();
