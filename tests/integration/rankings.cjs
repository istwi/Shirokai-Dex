const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {setTimeout:delay}=require('node:timers/promises'),mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs');
const root=path.resolve(process.env.MC_TEST_RUN||'');assert(/^level-name=capture-ranking-validation$/m.test(fs.readFileSync(path.join(root,'server.properties'),'utf8')));
const fixture=JSON.parse(fs.readFileSync(path.join(root,'.ranking-fixture.json'),'utf8'));let bot,rcon;const results=[];
function pass(name){results.push({name,status:'passed'});console.log('PASS',name);}
async function until(predicate){for(let i=0;i<300;i++){if(predicate())return;await delay(50);}throw new Error('Ranking condition timeout');}
function response(command,predicate){return new Promise((resolve,reject)=>{const timeout=setTimeout(()=>{bot.removeListener('messagestr',listener);reject(new Error('Missing response: '+command));},15000);function listener(text){if(predicate(text)){clearTimeout(timeout);bot.removeListener('messagestr',listener);resolve(text);}}bot.on('messagestr',listener);bot.chat(command);});}
async function open(command){if(bot.currentWindow){bot.closeWindow(bot.currentWindow);await until(()=>!bot.currentWindow);}await delay(1100);bot.chat(command);await until(()=>bot.currentWindow);return bot.currentWindow;}
function data(window){return JSON.stringify(window.slots.slice(0,window.inventoryStart).map(item=>item?.components));}
async function main(){
 rcon=await new Rcon().connect(25573,fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
 bot=mineflayer.createBot({host:'127.0.0.1',port:25571,username:'DexRanking',auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
 const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};
 await new Promise((resolve,reject)=>{bot.once('spawn',resolve);bot.once('error',reject);setTimeout(()=>reject(new Error('Login timeout')),30000).unref();});await delay(1800);assert.equal(bot.player.uuid,fixture.player);
 let window=await open('/dex top');assert(data(window).includes('Capturas'));assert(!data(window).includes('Restrito'));pass('Ranking selector shows configured accessible rankings and hides restricted definition without LuckPerms');
 window=await open('/dex top captures');assert(data(window).includes('DexRanking'));assert(data(window).includes('Pontuação: 3'));assert(!data(window).includes('Atingida em: N/D'));pass('Schema10 migration exposes persisted synthetic score, name and evidence-backed achievement timestamp');
 await bot.clickWindow(1,0,1);await delay(300);assert.equal(bot.currentWindow,window);assert.equal(bot.inventory.items().filter(x=>x.name==='player_head').length,0);pass('Ranking inventory icons cannot be extracted with shift click');
 bot.closeWindow(bot.currentWindow);await delay(1100);await response('/dex settings rankings hidden',text=>text.includes('Ocultar dos rankings: true'));
 window=await open('/dex top captures');assert(!data(window).includes('DexRanking'));pass('Hiding a player excludes their score immediately after cached ranking query');
 bot.closeWindow(bot.currentWindow);await delay(1100);await response('/dex settings rankings visible',text=>text.includes('Ocultar dos rankings: false'));
 window=await open('/dex top captures');assert(data(window).includes('DexRanking'));pass('Restoring visibility preserves permanent score');
 bot.closeWindow(bot.currentWindow);await delay(1100);await response('/dex records',text=>text.includes('Recorde DAY: 3'));pass('Personal period records derive from retained receipts in actual server command');
 await delay(1100);await response('/dex top restricted',text=>/permiss|acesso/i.test(text));pass('Direct command rechecks ranking permission');
}
(async()=>{try{await main();}catch(failure){process.exitCode=1;console.error(failure);results.push({name:'ranking integration',status:'failed',error:failure.message});}finally{if(bot)bot.quit();if(rcon)rcon.close();fs.writeFileSync(path.join(__dirname,'../../build/integration-rankings.json'),JSON.stringify({timestamp:new Date().toISOString(),results},null,2)+'\n');}})();
