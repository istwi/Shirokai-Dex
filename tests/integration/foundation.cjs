// Real protocol client against a dedicated loopback Fabric server; no gameplay API is mocked.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { setTimeout: delay } = require('node:timers/promises');

const results = [];
const report = path.resolve(__dirname, '../../build/integration-foundation.json');
let bot;
function pass(name) { results.push({ name, status: 'passed' }); console.log('PASS', name); }
async function until(predicate, timeout = 15000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    if (predicate()) return;
    await delay(50);
  }
  throw new Error('Timed out waiting for server state');
}
function command(text, matches) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => { bot.removeListener('messagestr', listener); reject(new Error(`No expected response to ${text}`)); }, 15000);
    function listener(message) {
      if (matches(message)) { clearTimeout(timeout); bot.removeListener('messagestr', listener); resolve(message); }
    }
    bot.on('messagestr', listener);
    bot.chat(text);
  });
}
async function main() {
  bot = mineflayer.createBot({
    host: '127.0.0.1', port: Number(process.env.MC_TEST_PORT || 25571),
    username: 'DexFoundation', auth: 'offline', version: '1.21.1', hideErrors: true,
    // Cobblemon adds metadata and recipes unknown to the vanilla protocol library.
    // These packets are unrelated to the commands/inventory behavior being asserted.
    customPackets: { '1.21': { play: { toClient: { types: {
      packet_entity_metadata: 'restBuffer', packet_declare_recipes: 'restBuffer',
      packet_advancements: 'restBuffer', packet_declare_commands: 'restBuffer'
    } } } } }
  });
  const originalEmit = bot._client.emit;
  bot._client.emit = function (event, ...args) {
    if (event === 'entity_metadata' || event === 'advancements' || event === 'declare_commands') return false;
    return originalEmit.call(this, event, ...args);
  };
  let error;
  bot.on('error', failure => { error = failure; });
  bot.on('kicked', reason => { error = new Error(`Server disconnected client: ${JSON.stringify(reason)}`); });
  let spawned = false;
  bot.once('spawn', () => { spawned = true; });
  await until(() => { if (error) throw error; return spawned; }, 45000);
  pass('Fabric login with Cobblemon and without LuckPerms/other optional integrations');
  const before = await command('/dex status', message => message.includes('Pokédex oficial:'));
  const values = before.match(/(\d+) espécies obtidas, (\d+) vistas, de (\d+) implementadas/);
  assert(values, before);
  const [owned, seen, total] = values.slice(1).map(Number);
  assert(total > 0 && owned <= seen && seen <= total);
  pass(`Official Pokédex query against dynamic registry (${total} implemented species)`);

  bot.chat('/dex');
  await until(() => bot.currentWindow);
  let window = bot.currentWindow;
  assert.equal(window.type, 'minecraft:generic_9x3');
  assert.equal(window.slots[13].name, 'book');
  assert.equal(window.slots[22].name, 'barrier');
  pass('Initial server-side inventory menu opens');
  assert.equal(window.slots[11].name, 'amethyst_shard');
  assert.equal(window.slots[15].name, 'nether_star');
  const selector = window;
  await bot.clickWindow(13, 0, 0);
  await until(() => bot.currentWindow && bot.currentWindow !== selector);
  window = bot.currentWindow;
  assert.equal(window.slots[18].name, 'arrow');
  pass('Collection selector navigates to national progress page');
  for (const [button, mode, label] of [[0, 0, 'pickup'], [1, 0, 'right click'], [0, 1, 'shift click'], [0, 2, 'hotbar swap']]) {
    await bot.clickWindow(13, button, mode);
    await until(() => !window.selectedItem && window.slots[13]?.name === 'book' &&
      !window.slots.slice(27).some(item => item?.name === 'book'));
    pass(`Menu blocks ${label} without transferring the display item`);
  }
  await bot.clickWindow(22, 0, 0);
  await until(() => !bot.currentWindow);
  pass('Close button closes the menu');
  bot.chat('/dex national wiki');
  await until(() => bot.currentWindow);
  const wiki = bot.currentWindow;
  assert(JSON.stringify(wiki.title).includes('Wiki'));
  assert.equal(wiki.slots[0].name, 'book');
  await bot.clickWindow(0, 0, 0);
  await until(() => bot.currentWindow && bot.currentWindow !== wiki);
  assert(JSON.stringify(bot.currentWindow.slots[13].components).includes('Informação de aparecimento indisponível'));
  await bot.clickWindow(22, 0, 0);
  await until(() => !bot.currentWindow);
  pass('Public wiki and species details work without LuckPerms or other optional integrations');
  const after = await command('/dex status', message => message.includes('Pokédex oficial:'));
  assert.equal(after, before);
  assert(!bot.inventory.items().some(item => ['book', 'barrier', 'amethyst_shard', 'nether_star', 'arrow'].includes(item.name)));
  pass('Official progress unchanged and no display items left in player inventory');
  await command('/dex admin reload', message => /Unknown|incomplete|desconhecido|incompleto|Incorrect argument|Argumento incorreto/i.test(message));
  pass('Administrative reload rejected without LuckPerms permission');
}

(async () => {
  let exit = 0;
  try { await main(); }
  catch (failure) { exit = 1; console.error(failure); results.push({ name: 'foundation smoke', status: 'failed', error: failure.message }); }
  finally {
    if (bot) bot.quit();
    fs.mkdirSync(path.dirname(report), { recursive: true });
    fs.writeFileSync(report, JSON.stringify({ timestamp: new Date().toISOString(), target: 'Minecraft 1.21.1 / Fabric 0.17.2 / Cobblemon 1.8.3', results }, null, 2));
    process.exitCode = exit;
  }
})();
