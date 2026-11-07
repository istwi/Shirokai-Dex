// Real Crates 0.2.1 checkpoint and signed SAVED_ITEM compatibility; isolated packaged server only.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {execFileSync}=require('node:child_process'),{setTimeout:delay}=require('node:timers/promises');
const mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs');
const PLAYER='DexCheckpoint',MODE=process.env.MC_CHECKPOINT_MODE||'main',root=path.resolve(process.env.MC_TEST_RUN||'');
assert(root.startsWith('/tmp/'));assert(fs.readFileSync(path.join(root,'server.properties'),'utf8').includes('level-name=crate-checkpoint-validation\n'));
const configFile=path.join(root,'config/shirokaidex/config.json'),db=path.join(root,'config/shirokaidex/data/shirokaidex.sqlite');
const results=[],timings=[];let bot,rcon,error,lastSend=0;
function pass(name){console.log('PASS',name);results.push({name,status:'passed'});}
async function until(predicate,timeout=15000){const end=Date.now()+timeout;while(Date.now()<end){if(error)throw error;if(await predicate())return;await delay(40);}throw new Error('Checkpoint condition timed out');}
async function send(text){await delay(Math.max(0,1100-(Date.now()-lastSend)));lastSend=Date.now();bot.chat(text);}
function message(action,matches){return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{bot.removeListener('messagestr',listener);reject(new Error('Missing checkpoint response'));},15000);function listener(text){if(matches(text)){clearTimeout(timer);bot.removeListener('messagestr',listener);resolve(text);}}bot.on('messagestr',listener);Promise.resolve(action()).catch(reject);});}
async function close(){if(bot.currentWindow){bot.closeWindow(bot.currentWindow);await until(()=>!bot.currentWindow);}}
async function open(text){await close();await send(text);await until(()=>bot.currentWindow);return bot.currentWindow;}
async function click(slot){const old=bot.currentWindow;await bot.clickWindow(slot,0,0);await until(()=>bot.currentWindow&&bot.currentWindow!==old);return bot.currentWindow;}
function data(item){return JSON.stringify(item?.components||[]);}
function quantity(name){return bot.inventory.items().filter(i=>i.name===name).reduce((n,i)=>n+i.count,0);}
function fingerprint(item){return crypto.createHash('sha256').update(data(item)).digest('hex');}
function journal(id){const code='import sqlite3,json,sys\nc=sqlite3.connect("file:"+sys.argv[1]+"?mode=ro",uri=True)\nr=c.execute("SELECT state FROM reward_operations WHERE player_uuid=? AND reward_id=?",sys.argv[2:]).fetchone()\nprint(json.dumps(r[0] if r else None))';return JSON.parse(execFileSync('python3',['-c',code,db,bot.player.uuid,id],{encoding:'utf8'}));}
async function lp(node){assert(!(await rcon.command(`lp user ${bot.player.uuid} permission set ${node} true`)).includes('Another command'));await delay(1200);}
async function confirmation(id){const menu=await open('/dex rewards '+id);assert.equal(menu.slots[24]?.name,'arrow');await click(24);assert.equal(bot.currentWindow.slots[13]?.name,'lime_dye');}
async function blocked(id,state){const menu=await open('/dex rewards '+id);assert(data(menu.slots[19]).includes(state));assert(!menu.slots[24]);await bot.clickWindow(24,0,0);await delay(120);assert.equal(bot.currentWindow,menu);}
async function keys(expected){assert.match(await rcon.command(`dexcheckpointprobe keys ${PLAYER} shiny1`),new RegExp('Valid keys: '+expected+'(?:\\D|$)'));}
async function delayed(id,item,expected){await confirmation(id);assert.match(await rcon.command('dexcheckpointprobe'),/delayed 2000ms/);const start=Date.now();const done=message(()=>bot.clickWindow(13,0,0),text=>text.includes('entregue e registrada'));await until(()=>quantity(item)===expected,1200);assert.equal(journal(id),'PROCESSING');await delay(350);assert.equal(journal(id),'PROCESSING');await done;const elapsed=Date.now()-start;assert(elapsed>=1500,`Acknowledged too early: ${elapsed}ms`);assert.equal(journal(id),'COMPLETED');timings.push({reward:id,acknowledgement_ms:elapsed});}
async function main(){
 rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT||25573),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
 bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT||25571),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
 const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
 let spawned=false;bot.once('spawn',()=>spawned=true);bot.on('error',value=>error=value);bot.on('kicked',value=>error=new Error(JSON.stringify(value)));await until(()=>spawned,45000);await rcon.command(`deop ${PLAYER}`);
 if(MODE==='after-restart'||MODE==='vanilla-restart'){
  assert.equal(quantity('diamond'),2);assert.equal(quantity('gold_ingot'),0);
  for(const id of ['keys_saved','plain']){assert.equal(journal(id),'COMPLETED');await blocked(id,'Resgate concluído');}assert.equal(journal('failed'),'NEEDS_REVIEW');await blocked('failed','Revisão necessária');
  if(MODE==='after-restart'){await keys(3);pass('Signed keys/items and completed/review journal survive native restart without redelivery');}
  else{assert.equal(quantity('emerald'),2);assert.equal(journal('vanilla'),'COMPLETED');await blocked('vanilla','Resgate concluído');pass('Vanilla fallback inventory and journal survive restart; uncertain delivery remains blocked');}return;
 }
 if(MODE==='vanilla'){
  assert(!fs.readdirSync(path.join(root,'mods')).some(name=>/crates|checkpoint-probe/i.test(name)));
  await confirmation('vanilla');await message(()=>bot.clickWindow(13,0,0),text=>text.includes('entregue e registrada'));assert.equal(quantity('emerald'),2);assert.equal(journal('vanilla'),'COMPLETED');await blocked('failed','Revisão necessária');pass('Without Crates/probe the vanilla save/readback fallback completes once and preserves review blocking');return;
 }
 assert.equal(journal('keys_saved'),null,'Use a fresh disposable world for main mode');
 await lp('shirokaicrates.give');await lp('shirokaidex.admin.items');await lp('shirokaidex.admin.reload');
 await message(()=>send(`/crates givekey ${PLAYER} shiny1 1`),text=>text.includes('Itens entregues'));
 assert.equal(bot.inventory.items().length,1);const original=bot.inventory.items()[0],keyItem=original.name,signature=fingerprint(original);bot.setQuickBarSlot(original.slot-36);await delay(150);await keys(1);
 await message(()=>send('/dex admin item save native_key'),text=>text==='Itens salvos: native_key');
 const config=JSON.parse(fs.readFileSync(configFile,'utf8'));config.rewards.find(x=>x.id==='keys_saved').items=[{type:'SAVED_ITEM',saved_item:'native_key',count:2}];fs.writeFileSync(configFile,JSON.stringify(config));await message(()=>send('/dex admin reload'),text=>text.includes('recarregada'));
 await rcon.command(`givepokemonother ${PLAYER} pikachu level=5`);await until(()=>journal('keys_saved')===null);
 await delayed('keys_saved',keyItem,3);await keys(3);for(const item of bot.inventory.items().filter(i=>i.name===keyItem))assert.equal(fingerprint(item),signature);
 pass('Native signed key saved as SAVED_ITEM preserves components, waits delayed checkpoint, and yields three provider-authentic keys');
 await delayed('plain','diamond',2);assert.equal(quantity('paper'),5);await keys(3);pass('Multi-item delivery waits native async save before ACK/readback; no premature review');
 for(const id of ['keys_saved','plain'])await blocked(id,'Resgate concluído');assert.equal(quantity(keyItem),3);assert.equal(quantity('diamond'),2);pass('Completed rewards reject repeated confirmation without duplicating physical items or keys');
 await close();await rcon.command('save-all');await delay(500);
 const target=path.join(root,'crate-checkpoint-validation/playerdata',bot.player.uuid+'.dat'),backup=target+'.checkpoint-test-backup';assert(fs.statSync(target).isFile());assert(!fs.existsSync(backup));fs.renameSync(target,backup);fs.mkdirSync(target);fs.writeFileSync(path.join(target,'owned-test-blocker'),'test');
 try{await confirmation('failed');await message(()=>bot.clickWindow(13,0,0),text=>text.includes('bloqueada para revisão'));assert.equal(quantity('gold_ingot'),1);assert.equal(journal('failed'),'NEEDS_REVIEW');}
 finally{fs.unlinkSync(path.join(target,'owned-test-blocker'));fs.rmdirSync(target);fs.renameSync(backup,target);}
 await blocked('failed','Revisão necessária');assert.equal(quantity('gold_ingot'),1);pass('Real playerdata atomic-write failure yields NEEDS_REVIEW and blocks repetition; original file restored in finally');
 await confirmation('vanilla');await message(()=>bot.clickWindow(13,0,0),text=>text.includes('mudou')||text.includes('indisponível')||text.includes('espaço'));assert.equal(quantity('emerald'),0);assert.equal(journal('vanilla'),null);pass('Unhealthy loaded Crates blocks new item effects instead of silently falling back to vanilla');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'checkpoint integration',status:'failed',error:failure.message});}finally{if(bot)bot.quit();if(rcon)rcon.close();const file=path.join(__dirname,'../../build/integration-crates-checkpoint-'+MODE+'.json');fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,JSON.stringify({timestamp:new Date().toISOString(),target:'Packaged Fabric 0.17.2 / Minecraft 1.21.1 / Cobblemon 1.8.3 / Crates 0.2.1 / Impactor 5.3.5',mode:MODE,player:PLAYER,timings,results},null,2));process.exitCode=exit;}})();
