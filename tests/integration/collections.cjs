// Isolated real-server test: requires the collections fixture, LuckPerms and authenticated loopback RCON.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');
const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
const configFile = path.join(root, 'config/shirokaidex/config.json');
const results = [];
let bot, rcon, connectionError;
const PLAYER = process.env.MC_COLLECTIONS_PLAYER || 'DexCollections';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER), 'Invalid test player name');
let lastSend = 0;
const recentMessages = [];
async function send(text) {
  if (connectionError) throw connectionError;
  await delay(Math.max(0, 1100 - (Date.now() - lastSend)));
  if (connectionError) throw connectionError;
  lastSend = Date.now(); bot.chat(text);
}
function pass(name) { console.log('PASS', name); results.push({ name, status: 'passed' }); }
async function until(predicate, timeout = 15000) {
  const end = Date.now() + timeout;
  while (Date.now() < end) { if (await predicate()) return; await delay(50); }
  throw new Error('Timed out waiting for server state');
}
async function command(text, predicate) {
  if (connectionError) throw connectionError;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { bot.removeListener('messagestr', listener); reject(connectionError || new Error('Missing expected response: ' + text + '; recent messages: ' + recentMessages.slice(-3).join(' | '))); }, 15000);
    function listener(message) { if (predicate(message)) { clearTimeout(timer); bot.removeListener('messagestr', listener); resolve(message); } }
    bot.on('messagestr', listener); send(text).catch(error => { clearTimeout(timer); bot.removeListener('messagestr', listener); reject(error); });
  });
}
async function status(id, name) {
  return command('/dex ' + id + ' status', message => message.startsWith(name + ':'));
}
async function counter(id, name) {
  const text = await status(id, name);
  const match = text.match(/: (\d+)\/(\d+) — ([\d,.]+%|N\/D)/);
  assert(match, text);
  return { registered: Number(match[1]), total: Number(match[2]), percent: match[3] };
}
async function waitCount(id, name, expected) {
  await until(async () => (await counter(id, name)).registered === expected, 4000);
}
async function openSelector() {
  const old = bot.currentWindow; await send('/dex');
  await until(() => bot.currentWindow && bot.currentWindow !== old); const hub=bot.currentWindow; await bot.clickWindow(20,0,0); await until(()=>bot.currentWindow&&bot.currentWindow!==hub); return bot.currentWindow;
}
function itemName(item) { return item ? JSON.stringify(item.customName || item.nbt || '') : ''; }
async function closeMenu() {
  if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await until(() => !bot.currentWindow); }
}
async function main() {
  const passwordFile = process.env.MC_TEST_RCON_PASSWORD_FILE;
  assert(passwordFile, 'Set MC_TEST_RCON_PASSWORD_FILE to the local test password file');
  rcon = await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT || 25573), fs.readFileSync(passwordFile, 'utf8').trim());
  bot = mineflayer.createBot({ host: '127.0.0.1', port: Number(process.env.MC_TEST_PORT || 25572), username: PLAYER, auth: 'offline', version: '1.21.1', hideErrors: true,
    customPackets: { '1.21': { play: { toClient: { types: {
      packet_entity_metadata: 'restBuffer', packet_declare_recipes: 'restBuffer', packet_advancements: 'restBuffer', packet_declare_commands: 'restBuffer'
    } } } } }
  });
  const emit = bot._client.emit;
  bot._client.emit = function (event, ...args) { if (['entity_metadata', 'advancements', 'declare_commands'].includes(event)) return false; return emit.call(this, event, ...args); };
  let spawned = false, error;
  bot.once('spawn', () => { spawned = true; }); bot.on('error', failure => { error = connectionError = failure; }); bot.on('kicked', reason => { error = connectionError = new Error('Server disconnect: ' + JSON.stringify(reason)); });
  bot.on('messagestr', message => { recentMessages.push(message); if (recentMessages.length > 10) recentMessages.shift(); });
  await until(() => { if (error) throw error; return spawned; }, 45000);
  if (process.env.MC_COLLECTIONS_MODE === 'after-restart') {
    assert(fs.readFileSync(configFile, 'utf8').startsWith('{ invalid'));
    const value = await counter('duo', 'Duo recarregada');
    assert.equal(value.total, 1); assert.equal(value.registered, 1); assert.equal(value.percent, '100,00%');
    pass('Restart recovers semantically valid collection backup and preserves official player progress');
    let first = await openSelector();
    assert.equal(first.slots[26].name, 'arrow');
    await bot.clickWindow(26, 0, 0); await until(() => bot.currentWindow && bot.currentWindow !== first);
    const second = bot.currentWindow;
    assert.equal(second.slots[18].name, 'arrow');
    await bot.clickWindow(18, 0, 0); await until(() => bot.currentWindow && bot.currentWindow !== second);
    assert.equal(bot.currentWindow.slots[13].name, 'book');
    pass('Pagination forward and backward survives configuration recovery');
    await closeMenu();
    return;
  }
  const original = JSON.parse(fs.readFileSync(configFile, 'utf8'));
  assert.equal((await counter('duo', 'Duo')).registered, 0, 'Use a fresh disposable DexCollections player');
  assert.equal((await counter('duo', 'Duo')).total, 2);
  const future = await counter('future_species', 'Espécie não implementada');
  assert.equal(future.total, 0); assert.equal(future.registered, 0); assert.equal(future.percent, 'N/D');
  pass('JSON species whitelist resolves namespaced species; unimplemented species remains valid but has no eligible progress');
  await command('/dex locked status', text => text.includes('não tem permissão'));
  let menu = await openSelector();
  assert(!menu.slots.some(item => itemName(item).includes('Restrita')));
  assert.equal(menu.slots[26].name, 'arrow');
  await bot.clickWindow(26, 0, 0); await until(() => bot.currentWindow && bot.currentWindow !== menu);
  menu = bot.currentWindow;
  assert(!menu.slots.some(item => itemName(item).includes('Restrita')));
  assert.equal(menu.slots[18].name, 'arrow');
  pass('Selector paginates more than 18 collections and hides restricted collection on every page');
  await closeMenu();

  await rcon.command(`givepokemonother ${PLAYER} dex_test:ordinary level=5`);
  await waitCount('duo', 'Duo', 1);
  assert.equal((await counter('duo', 'Duo')).percent, '50,00%');
  assert.equal((await counter('seen_duo', 'Vistas')).registered, 1);
  assert.equal((await counter('normal_legends', 'Lendárias normais')).registered, 0);
  await rcon.command(`givepokemonother ${PLAYER} dex_test:legend level=5`);
  await waitCount('duo', 'Duo', 2); await waitCount('normal_legends', 'Lendárias normais', 1);
  assert.equal((await counter('without_legend', 'Sem lendária')).total, 1);
  assert.equal((await counter('without_legend', 'Sem lendária')).registered, 1);
  pass('Custom owned/seen collections combine species, labels, types, forms and exclude precedence correctly');
  await rcon.command(`givepokemonother ${PLAYER} dex_test:ordinary shiny=true level=5`);
  await waitCount('shiny_seen', 'Shiny vistas', 1);
  assert.equal((await counter('shiny_seen', 'Shiny vistas')).percent, '100,00%');
  const shinyCaptured = await status('shiny', 'Shiny'); assert(shinyCaptured.includes('N/D'));
  pass('Explicit SHINY_SEEN uses official seen state while SHINY_CAPTURED remains unavailable');
  await rcon.command(`givepokemonother ${PLAYER} pikachu level=5`);
  await waitCount('electric', 'Elétricas', 1);
  pass('Type-filtered collection resolves real Cobblemon elemental types');

  await rcon.command(`lp user ${PLAYER} permission set shirokaidex.collection.locked true`);
  await until(async () => {
    const response = await command('/dex locked status', text => text.startsWith('Restrita:') || text.includes('não tem permissão'));
    return response.startsWith('Restrita:');
  });
  menu = await openSelector();
  const privateSlot = menu.slots.findIndex(item => itemName(item).includes('Restrita')); assert(privateSlot >= 0 && privateSlot < 18);
  await rcon.command(`lp user ${PLAYER} permission set shirokaidex.collection.locked false`);
  await until(async () => (await command('/dex locked status', text => text.startsWith('Restrita:') || text.includes('não tem permissão'))).includes('não tem permissão'));
  const denied = command('/dex locked status', text => text.includes('não tem permissão')); await denied;
  const clickDenied = new Promise((resolve, reject) => {
    const timer = setTimeout(() => { bot.removeListener('messagestr', listener); reject(new Error('Stale button did not revalidate permission')); }, 10000);
    function listener(message) { if (message.includes('não tem permissão')) { clearTimeout(timer); bot.removeListener('messagestr', listener); resolve(); } }
    bot.on('messagestr', listener);
  });
  await bot.clickWindow(privateSlot, 0, 0); await clickDenied;
  await until(() => bot.currentWindow === menu && !menu.selectedItem && menu.slots[privateSlot]?.name === 'diamond');
  pass('LuckPerms positive permission works and stale GUI button rechecks revocation without leaking its icon');
  await closeMenu();

  await command('/dex admin reload', text => /Incorrect argument|Unknown|incomplete/i.test(text));
  await rcon.command(`lp user ${PLAYER} permission set shirokaidex.admin.reload true`);
  await until(async () => (await command('/dex admin reload', text => /recarregada|Incorrect argument|Unknown|incomplete/i.test(text))).includes('recarregada'));
  pass('Administrative reload succeeds with an explicit LuckPerms grant and was denied before the grant');
  const backupFile = path.join(root, 'config/shirokaidex/config.last-good.json');
  const backup = fs.readFileSync(backupFile, 'utf8');
  const bad = structuredClone(original); bad.menu_title = 'Must not apply'; bad.language = 'en_us'; bad.collections.find(value => value.id === 'duo').icon = 'test:missing';
  fs.writeFileSync(configFile, JSON.stringify(bad));
  await command('/dex admin reload', text => text.includes('última configuração válida foi mantida'));
  assert.equal(fs.readFileSync(backupFile, 'utf8'), backup);
  assert.equal((await counter('duo', 'Duo')).total, 2);
  menu = await openSelector(); assert(JSON.stringify(menu.title).includes(original.menu_title)); await closeMenu();
  pass('Invalid registry reference rejects the whole reload including title/language and preserves last-good backup');
  const good = structuredClone(original); good.menu_title = 'Coleções recarregadas';
  const duo = good.collections.find(value => value.id === 'duo'); duo.name = 'Duo recarregada'; duo.criteria.include_species = ['dex_test:ordinary'];
  fs.writeFileSync(configFile, JSON.stringify(good));
  await command('/dex admin reload', text => text.includes('recarregada'));
  const updated = await counter('duo', 'Duo recarregada');
  assert.equal(updated.total, 1); assert.equal(updated.registered, 1); assert.equal(updated.percent, '100,00%');
  menu = await openSelector(); assert(JSON.stringify(menu.title).includes(good.menu_title)); await closeMenu();
  pass('Valid reload atomically applies metadata/filters and invalidates cached collection percentage');
}
(async () => {
  let exit = 0;
  try { await main(); }
  catch (error) { exit = 1; console.error(error); results.push({ name: 'collections integration', status: 'failed', error: error.message }); }
  finally {
    if (bot) bot.quit(); if (rcon) rcon.close();
    const suffix = process.env.MC_COLLECTIONS_MODE === 'after-restart' ? '-restart' : '';
    const target = path.resolve(__dirname, '../../build/integration-collections' + suffix + '.json');
    fs.mkdirSync(path.dirname(target), { recursive: true }); fs.writeFileSync(target, JSON.stringify({ timestamp: new Date().toISOString(), test_player: PLAYER, results }, null, 2));
    process.exitCode = exit;
  }
})();
