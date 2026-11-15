// Disposable loopback server only. Tests real clicks and player command permissions.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs'),{setTimeout:delay}=require('node:timers/promises');
const root=path.resolve(process.env.MC_TEST_RUN||'');assert(root.startsWith('/tmp/'));
let bot,rcon,error;const messages=[],results=[];
async function until(fn){const end=Date.now()+30000;while(Date.now()<end){if(error)throw error;if(await fn())return;await delay(100);}throw Error('Readiness timeout');}
async function send(text){await delay(1200);bot.chat(text);}
async function open(){if(bot.currentWindow)bot.closeWindow(bot.currentWindow);await send('/dex nacional');await until(()=>bot.currentWindow);await delay(300);return bot.currentWindow;}
async function configure(command){const file=path.join(root,'config/shirokaidex/menus/dex_nacional.json');const menu=JSON.parse(fs.readFileSync(file,'utf8'));menu.menu.buttons.close.command=command;fs.writeFileSync(file,JSON.stringify(menu));const index=messages.length;await send('/dex admin reload');await until(()=>messages.slice(index).some(x=>x.includes('recarregada')));}
function pass(name){results.push({name,status:'passed'});console.log('PASS',name);}
(async()=>{try{
 rcon=await new Rcon().connect(25573,fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
 bot=mineflayer.createBot({host:'127.0.0.1',port:25571,username:'DexButton',auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});
 const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};let spawned=false;bot.once('spawn',()=>spawned=true);bot.on('messagestr',x=>messages.push(x));bot.on('error',e=>error=e);bot.on('kicked',()=>error=Error('Disconnected'));await until(()=>spawned);
 await rcon.command('lp user '+bot.player.uuid+' permission set shirokaidex.admin.reload true');await delay(1000);
 await open();await bot.clickWindow(28,0,0);await until(()=>!bot.currentWindow);pass('Default barrier closes without opening a menu');
 await send('/dex shiny');await until(()=>bot.currentWindow);const shinyTitle=JSON.stringify(bot.currentWindow.title);bot.closeWindow(bot.currentWindow);await configure('/dex shiny');const previous=await open();await bot.clickWindow(28,0,0);await until(()=>bot.currentWindow&&bot.currentWindow!==previous);assert.equal(JSON.stringify(bot.currentWindow.title),shinyTitle);pass('Player command opens another direct menu with arguments');
 bot.closeWindow(bot.currentWindow);await configure('/give @s minecraft:diamond');await open();await bot.clickWindow(28,0,0);await until(()=>!bot.currentWindow);await delay(600);assert(!bot.inventory.items().some(x=>x.name==='diamond'));pass('Non-OP player cannot elevate permissions through a configured command');
 await configure('/shirokai_missing_command');await open();const index=messages.length;await bot.clickWindow(28,0,0);await until(()=>!bot.currentWindow);await until(()=>messages.slice(index).some(x=>/Unknown|incomplet|desconhecid/i.test(x)));pass('Missing destination closes menu and reports dispatcher error');
 await configure('');await open();await bot.clickWindow(28,0,0);await until(()=>!bot.currentWindow);pass('Reload restores default close behavior');
 }catch(e){process.exitCode=1;console.error(e);results.push({name:'close command integration',status:'failed',error:e.message});}finally{if(bot)bot.quit();if(rcon)rcon.close();fs.writeFileSync(path.join(__dirname,'../../build/integration-close-command.json'),JSON.stringify({timestamp:new Date().toISOString(),results},null,2)+'\n');}})();
