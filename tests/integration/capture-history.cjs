// Disposable loopback server only; does not grant or manufacture capture progress.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { setTimeout: delay } = require('node:timers/promises');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const root = path.resolve(process.env.MC_TEST_RUN || '');
assert(/^level-name=capture-history-validation$/m.test(fs.readFileSync(path.join(root, 'server.properties'), 'utf8')));
let bot, rcon;
const results = [];
function pass(name) { results.push({ name, status: 'passed' }); console.log('PASS', name); }
function response(command, matches) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { bot.removeListener('messagestr', listener); reject(new Error('Missing history response: ' + command)); }, 15000);
    function listener(text) { if (matches(text)) { clearTimeout(timer); bot.removeListener('messagestr', listener); resolve(text); } }
    bot.on('messagestr', listener); bot.chat(command);
  });
}
async function permission(value) {
  await rcon.command(`lp user ${bot.player.uuid} permission set shirokaidex.admin.captures ${value}`);
  await delay(1300);
}
async function main() {
  rcon = await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT || 25573), fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE, 'utf8').trim());
  bot = mineflayer.createBot({ host: '127.0.0.1', port: Number(process.env.MC_TEST_PORT || 25571), username: 'DexHistory', auth: 'offline', version: '1.21.1', hideErrors: true,
    customPackets: { '1.21': { play: { toClient: { types: { packet_entity_metadata: 'restBuffer', packet_declare_recipes: 'restBuffer', packet_advancements: 'restBuffer', packet_declare_commands: 'restBuffer' } } } } } });
  const emit = bot._client.emit;
  bot._client.emit = function(event, ...args) { if (['entity_metadata', 'advancements', 'declare_commands'].includes(event)) return false; return emit.call(this, event, ...args); };
  await new Promise((resolve, reject) => { bot.once('spawn', resolve); bot.once('error', reject); setTimeout(() => reject(new Error('Login timeout')), 30000).unref(); });
  await delay(2000);
  const uuid = bot.player.uuid;
  await permission(false);
  await response(`/dex admin captures ${uuid}`, text => /Unknown|Incorrect|incomplet|desconhecid/i.test(text));
  pass('Unprivileged player cannot query detailed capture history');
  await permission(true);
  const header = await response(`/dex admin captures ${uuid}`, text => text.startsWith('Capturas de '));
  assert(header.includes(uuid)); assert(header.includes('página 1')); assert(header.includes('false'));
  pass('Dedicated permission alone grants administrative access and returns bounded empty history');
  await delay(1100);
  await response('/dex admin captures invalid', text => text.includes('UUID inválido'));
  pass('Invalid UUID is rejected without database lookup');
  await delay(1100);
  await response(`/dex admin captures ${uuid} 100001`, text => text.includes('página 100001'));
  pass('Last supported page is bounded and returns successfully');
  await permission(false);
  await response(`/dex admin captures ${uuid}`, text => /Unknown|Incorrect|incomplet|desconhecid/i.test(text));
  pass('Revoking permission prevents further history access');
  assert(/Unknown|Incorrect|incomplet|desconhecid/i.test(await rcon.command(`dex admin captures ${uuid}`)));
  pass('Console has no bypass of player-only history ACL');
}
(async () => {
  try { await main(); } catch (failure) { results.push({ name: 'history integration', status: 'failed', error: failure.message }); console.error(failure); process.exitCode = 1; }
  finally {
    if (bot && rcon && bot.player) await rcon.command(`lp user ${bot.player.uuid} permission unset shirokaidex.admin.captures`).catch(() => {});
    if (bot) bot.quit(); if (rcon) rcon.close();
    fs.mkdirSync(path.join(__dirname, '../../build'), { recursive: true });
    fs.writeFileSync(path.join(__dirname, '../../build/integration-capture-history.json'), JSON.stringify({ timestamp: new Date().toISOString(), results }, null, 2) + '\n');
  }
})();
