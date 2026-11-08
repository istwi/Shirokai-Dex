// First positive events come from real native projectile collisions with naturally spawned Pokemon.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{execFileSync}=require('node:child_process');
const {setTimeout:delay}=require('node:timers/promises'),mineflayer=require('mineflayer'),Rcon=require('./rcon.cjs');
const root=path.resolve(process.env.MC_TEST_RUN||''),PLAYER='DexCapture',RESTART=process.env.MC_CAPTURE_MODE==='after-restart';assert(root.startsWith('/tmp/'));assert(/^level-name=native-capture-validation$/m.test(fs.readFileSync(path.join(root,'server.properties'),'utf8')));
const results=[];let bot,rcon,error;
function pass(name){console.log('PASS',name);results.push({name,status:'passed'});}
async function until(predicate,timeout=15000){const end=Date.now()+timeout;while(Date.now()<end){if(error)throw error;if(await predicate())return;await delay(100);}throw new Error('Capture condition timed out');}
function query(sql,args=[]){const code='import sqlite3,json,sys\nc=sqlite3.connect("file:"+sys.argv[1]+"?mode=ro",uri=True)\nprint(json.dumps(c.execute(sys.argv[2],json.loads(sys.argv[3])).fetchall()))';return JSON.parse(execFileSync('python3',['-c',code,path.join(root,'config/shirokaidex/data/shirokaidex.sqlite'),sql,JSON.stringify(args)],{encoding:'utf8'}));}
function total(){return query('select coalesce((select total from capture_totals where player_uuid=?),0)',[bot.player.uuid])[0][0];}
function rows(){return query('select pokemon_uuid,origin,shiny,categories_json,category_rules from capture_events where player_uuid=? order by captured_at,event_key',[bot.player.uuid]);}
async function launch(mode,known,unknown){assert.match(await rcon.command('dexcaptureprobe '+mode),/Native ball launched:/);try{await until(()=>total()===known&&rows().filter(r=>r[1]==='UNKNOWN').length===unknown,45000);}catch(failure){console.error(await rcon.command('dexcaptureprobe status'));console.error('observations',rows().map(r=>r[1]));throw failure;}}
async function fish(){
 assert.match(await rcon.command('dexcaptureprobe fishcast'),/Native rod cast/);
 try{await until(async()=>/Fishing candidates=[1-9]/.test(await rcon.command('dexcaptureprobe fishstatus')),180000);}catch(f){console.error(await rcon.command('dexcaptureprobe fishstatus'));throw f;}
}
async function main(){
 rcon=await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT||25573),fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE,'utf8').trim());
 bot=mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.MC_TEST_PORT||25571),username:PLAYER,auth:'offline',version:'1.21.1',hideErrors:true,customPackets:{'1.21':{play:{toClient:{types:{packet_entity_metadata:'restBuffer',packet_declare_recipes:'restBuffer',packet_advancements:'restBuffer',packet_declare_commands:'restBuffer'}}}}}});const emit=bot._client.emit;bot._client.emit=function(event,...args){if(['entity_metadata','advancements','declare_commands'].includes(event))return false;return emit.call(this,event,...args);};let ready=false;bot.once('spawn',()=>ready=true);bot.on('error',v=>error=v);bot.on('kicked',v=>error=new Error(JSON.stringify(v)));await until(()=>ready,45000);await rcon.command(`deop ${PLAYER}`);

 if(RESTART){
  assert.equal(total(),1);assert.equal(rows().length,1);assert.equal(query("select native_source from capture_events")[0][0],'FISHING');pass('Fishing source and permanent shiny totals survive restart');
  const launched=await rcon.command('dexcaptureprobe fishparked');assert.match(launched,/spawn cause: unknown/);await until(()=>total()===2,45000);assert.equal(rows().length,2);assert.deepEqual(query("select native_source,verification from capture_events order by captured_at desc limit 1")[0],['FISHING','PERSISTED_NATIVE']);await rcon.command('dexcaptureprobe replay');await delay(400);assert.equal(total(),2);pass('Same fished entity reloads without cause and consumes persisted fishing proof once');return;
 }
 assert.equal(rows().length,0);await fish();pass('Native rod cast, bobber ticks and native reel produce a wild fishing entity without assigning SpawnCause');
 await launch('fishthrow',1,0);assert.equal(rows().length,1);assert.deepEqual(query("select native_source,verification from capture_events")[0],['FISHING','LIVE_NATIVE']);pass('Physical ball captures native fished Pokemon with observed fishing source');
 await rcon.command('dexcaptureprobe replay');await delay(400);assert.equal(total(),1);assert.equal(rows().length,1);pass('Fishing event replay cannot duplicate permanent statistics');
 await fish();const parked=await rcon.command('dexcaptureprobe fishpark');assert.match(parked,/Native candidate parked:/);const id=parked.match(/[0-9a-f]{8}-[0-9a-f-]{27}/)[0];await until(()=>query("select count(*) from native_spawn_proofs where pokemon_uuid=? and native_source='FISHING' and consumed_event is null",[id])[0][0]===1);pass('Untouched fished entity proof exists before restart with immutable FISHING source');
}
(async()=>{let exit=0;try{await main();}catch(failure){exit=1;console.error(failure);results.push({name:'native fishing integration',status:'failed',error:failure.message});}finally{if(bot)bot.quit();if(rcon)rcon.close();const report=path.join(__dirname,'../../build/integration-fishing'+(RESTART?'-restart':'')+'.json');fs.mkdirSync(path.dirname(report),{recursive:true});fs.writeFileSync(report,JSON.stringify({timestamp:new Date().toISOString(),target:'Packaged native rod/bobber/reel/capture',results},null,2));process.exitCode=exit;}})();
