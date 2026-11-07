// Real LuckPerms dimension contexts and hostile inventory packets on a disposable server.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const Item = require('prismarine-item')('1.21.1');
const Rcon = require('./rcon.cjs');
const { setTimeout: delay } = require('node:timers/promises');

const PLAYER = process.env.MC_CONTEXT_PLAYER || 'DexContext';
assert(/^[A-Za-z0-9_]{1,16}$/.test(PLAYER), 'Invalid disposable player name');
const results = [];
let bot, rcon, connectionError, lastSend = 0, stateId = 0, fullUpdates = 0;
let userId;
function pass(name) { results.push({ name, status: 'passed' }); console.log('PASS', name); }
async function until(predicate, timeout = 15000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    if (connectionError) throw connectionError;
    if (await predicate()) return;
    await delay(50);
  }
  throw new Error('Timed out waiting for server state');
}
async function command(text, matches) {
  await delay(Math.max(0, 1100 - (Date.now() - lastSend)));
  if (connectionError) throw connectionError;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { bot.removeListener('messagestr', listener); reject(new Error('Missing response to ' + text)); }, 15000);
    function listener(message) {
      if (matches(message)) { clearTimeout(timer); bot.removeListener('messagestr', listener); resolve(message); }
    }
    bot.on('messagestr', listener); lastSend = Date.now(); bot.chat(text);
  });
}
async function closeMenu() {
  if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await until(() => !bot.currentWindow); }
}
async function selector() {
  await closeMenu();
  await delay(Math.max(0, 1100 - (Date.now() - lastSend)));
  lastSend = Date.now(); bot.chat('/dex');
  await until(() => bot.currentWindow); const hub=bot.currentWindow; await bot.clickWindow(20,0,0); await until(()=>bot.currentWindow&&bot.currentWindow!==hub);
  return bot.currentWindow;
}
async function allowed(expected) {
  await until(async () => {
    const text = await command('/dex dimension_locked status', value => value.startsWith('Dimensional:') || value.includes('não tem permissão'));
    return text.startsWith('Dimensional:') === expected;
  });
}
async function updatePermission(argumentsText) {
  // LuckPerms executes commands asynchronously; a rapid next RCON command can be rejected as busy.
  const response = await rcon.command(`lp user ${userId} permission ${argumentsText}`);
  assert(!response.includes('Another command is being executed'), response);
  await delay(1200);
}
function signature(window) {
  return window.slots.map(item => item ? [item.type, item.count, JSON.stringify(item.components || item.nbt || null)] : null);
}
async function packetClick(slot, mouseButton, mode, cursor = null) {
  // Send the actual protocol packet: prismarine-windows cannot model every drag phase.
  const update = fullUpdates;
  bot._client.write('window_click', { windowId: bot.currentWindow.id, stateId, slot, mouseButton, mode,
    changedSlots: [], cursorItem: Item.toNotch(cursor) });
  await until(() => fullUpdates > update);
}
async function main() {
  const root = path.resolve(process.env.MC_TEST_RUN || path.join(__dirname, '../../run'));
  const fixture = JSON.parse(fs.readFileSync(path.join(__dirname, '../fixtures/contexts/config.json'), 'utf8'));
  assert.deepEqual(JSON.parse(fs.readFileSync(path.join(root, 'config/shirokaidex/config.json'), 'utf8')), fixture,
    'Install the contexts fixture on a disposable server before running this test');
  assert(process.env.MC_TEST_RCON_PASSWORD_FILE, 'Set MC_TEST_RCON_PASSWORD_FILE');
  rcon = await new Rcon().connect(Number(process.env.MC_TEST_RCON_PORT || 25573), fs.readFileSync(process.env.MC_TEST_RCON_PASSWORD_FILE, 'utf8').trim());
  bot = mineflayer.createBot({ host: '127.0.0.1', port: Number(process.env.MC_TEST_PORT || 25571), username: PLAYER,
    auth: 'offline', version: '1.21.1', hideErrors: true,
    customPackets: { '1.21': { play: { toClient: { types: {
      packet_entity_metadata: 'restBuffer', packet_declare_recipes: 'restBuffer',
      packet_advancements: 'restBuffer', packet_declare_commands: 'restBuffer'
    } } } } }
  });
  const emit = bot._client.emit;
  bot._client.emit = function (event, ...args) {
    if (['entity_metadata', 'advancements', 'declare_commands'].includes(event)) return false;
    return emit.call(this, event, ...args);
  };
  let spawned = false;
  bot.once('spawn', () => { spawned = true; });
  bot.on('error', error => { connectionError = error; });
  bot.on('kicked', reason => { connectionError = new Error('Disconnected: ' + JSON.stringify(reason)); });
  bot._client.on('window_items', packet => { stateId = packet.stateId; fullUpdates++; });
  bot._client.on('set_slot', packet => { if (packet.windowId === bot.currentWindow?.id) stateId = packet.stateId; });
  await until(() => spawned, 45000);
  userId = bot.player.uuid;
  assert.match(userId, /^[0-9a-f-]{36}$/i);
  // No OP: RCON performs setup, while all protected commands come from the player.
  await rcon.command(`deop ${PLAYER}`);
  await rcon.command(`gamemode creative ${PLAYER}`);
  await rcon.command(`execute in minecraft:overworld run tp ${PLAYER} 0 200 0`);
  const permission = 'shirokaidex.collection.dimension';
  await updatePermission(`unset ${permission}`);
  await updatePermission(`set ${permission} true world=overworld`);
  await updatePermission('set shirokaidex.admin.reload true world=overworld');
  await allowed(true);
  assert.equal(bot.game.dimension, 'overworld');
  let menu = await selector();
  const privateSlot = menu.slots.findIndex(item => item?.name === 'diamond');
  assert(privateSlot >= 0 && privateSlot < 27);
  await closeMenu();
  await command('/dex admin reload', text => text.includes('recarregada'));
  pass('Overworld-scoped LuckPerms grants expose collection and allow administrative reload without OP');

  await rcon.command(`execute in minecraft:the_nether run tp ${PLAYER} 0 200 0`);
  await allowed(false);
  assert.equal(bot.game.dimension, 'the_nether');
  menu = await selector(); assert(!menu.slots.some(item => item?.name === 'diamond'));
  await closeMenu();
  await command('/dex admin reload', text => /Incorrect argument|Unknown|incomplete/i.test(text));
  pass('Dimension change denies collection and reload and hides the restricted button in the Nether');

  await rcon.command(`execute in minecraft:overworld run tp ${PLAYER} 0 200 0`);
  await allowed(true);
  menu = await selector(); assert(menu.slots[privateSlot]?.name === 'diamond');
  await updatePermission(`set ${permission} false world=overworld`);
  await allowed(false);
  const denied = new Promise((resolve, reject) => {
    const timer = setTimeout(() => { bot.removeListener('messagestr', listener); reject(new Error('Stale contextual button did not deny access')); }, 10000);
    function listener(text) { if (text.includes('não tem permissão')) { clearTimeout(timer); bot.removeListener('messagestr', listener); resolve(); } }
    bot.on('messagestr', listener);
  });
  await bot.clickWindow(privateSlot, 0, 0); await denied;
  await until(() => bot.currentWindow === menu && !menu.selectedItem && menu.slots[privateSlot]?.name === 'diamond');
  pass('Return to Overworld restores access; contextual revocation also rejects an already open GUI button');
  await closeMenu();

  await rcon.command(`clear ${PLAYER}`);
  await rcon.command(`give ${PLAYER} minecraft:stone 7`);
  await rcon.command(`give ${PLAYER} minecraft:book 2`);
  const before = await command('/dex status', text => text.includes('Pokédex oficial:'));
  menu = await selector();
  const baseline = signature(menu), icon = menu.slots[13];
  assert.equal(icon.name, 'book');
  for (const button of [0, 1]) {
    await packetClick(13, button, 4);
    assert.deepEqual(signature(menu), baseline); assert.equal(menu.selectedItem, null);
  }
  pass('Single-item and whole-stack discard packets cannot remove display icons');
  await packetClick(13, 0, 6, icon);
  assert.deepEqual(signature(menu), baseline); assert.equal(menu.selectedItem, null);
  pass('Double-click with a forged carried icon cannot collect menu or inventory books');
  for (const buttons of [[0, 1, 2], [4, 5, 6]]) {
    for (const [slot, button] of [[-999, buttons[0]], [13, buttons[1]], [0, buttons[1]], [-999, buttons[2]]]) {
      await packetClick(slot, button, 5, icon);
      assert.deepEqual(signature(menu), baseline); assert.equal(menu.selectedItem, null);
    }
  }
  pass('Left/right drag start, add-slot and finish packets cannot distribute forged icons');
  await packetClick(13, 2, 3);
  await packetClick(-999, 0, 0, icon);
  assert.deepEqual(signature(menu), baseline); assert.equal(menu.selectedItem, null);
  pass('Creative clone and outside-click packets cannot create or drop display items');
  await closeMenu();
  assert.match(await rcon.command(`clear ${PLAYER} minecraft:stone 0`), /Found 7 matching item/);
  assert.match(await rcon.command(`clear ${PLAYER} minecraft:book 0`), /Found 2 matching item/);
  assert.match(await rcon.command(`clear ${PLAYER} minecraft:diamond 0`), /No items were found/);
  const dropped = await rcon.command('execute in minecraft:overworld if entity @e[type=minecraft:item] run say DEX_UNEXPECTED_DROP');
  assert(!dropped.includes('DEX_UNEXPECTED_DROP'), dropped);
  assert.equal(await command('/dex status', text => text.includes('Pokédex oficial:')), before);
  menu = await selector();
  assert.equal(menu.slots[13]?.name, 'book'); assert.equal(menu.selectedItem, null);
  await closeMenu();
  pass('Server inventory counts, absence of dropped items, reopened menu and official progress remain intact');
}
(async () => {
  let exit = 0;
  try { await main(); }
  catch (error) { exit = 1; console.error(error); results.push({ name: 'contexts and inventory integration', status: 'failed', error: error.message }); }
  finally {
    if (bot) bot.quit();
    if (rcon) rcon.close();
    const report = path.resolve(__dirname, '../../build/integration-contexts.json');
    fs.mkdirSync(path.dirname(report), { recursive: true });
    fs.writeFileSync(report, JSON.stringify({ timestamp: new Date().toISOString(), target: 'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3 / LuckPerms 5.4.140', test_player: PLAYER, results }, null, 2));
    process.exitCode = exit;
  }
})();
