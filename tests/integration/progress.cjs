// Real server integration using the disposable dex-progress datapack and a test operator.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { setTimeout: delay } = require('node:timers/promises');
const results = [];
const mode = process.env.MC_PROGRESS_MODE || 'with-fixture';
let bot;
function pass(name) { console.log('PASS', name); results.push({ name, status: 'passed' }); }
async function until(predicate, timeout = 15000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) { if (predicate()) return; await delay(30); }
  throw new Error('Timed out waiting for progress state');
}
function command(text, predicate) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { bot.removeListener('messagestr', listener); reject(new Error('Missing response: ' + text)); }, 15000);
    function listener(message) { if (predicate(message)) { clearTimeout(timer); bot.removeListener('messagestr', listener); resolve(message); } }
    bot.on('messagestr', listener); bot.chat(text);
  });
}
async function progress(collection) {
  const name = collection === 'national' ? 'Nacional' : 'Lendária';
  const message = await command(`/dex ${collection} status`, text => text.startsWith(name + ':'));
  const match = message.match(/: (\d+)\/(\d+) — (\d+,\d{2})%/);
  assert(match, message);
  return { registered: Number(match[1]), total: Number(match[2]), percent: Number(match[3].replace(',', '.')) };
}
function expectPercentage(value) {
  assert.equal(value.percent, Math.floor(value.registered * 10000 / value.total) / 100);
}
async function waitCount(collection, expected) {
  const end = Date.now() + 4000;
  while (Date.now() < end) {
    const value = await progress(collection);
    if (value.registered === expected) { expectPercentage(value); return value; }
    await delay(50);
  }
  throw new Error('Cache did not reflect official progress event within 4 seconds');
}
async function give(species) {
  await command('/givepokemon ' + species + ' level=5', text => /given|gave|deu|entreg|recebe|givepokemon\.give/i.test(text));
}
async function main() {
  bot = mineflayer.createBot({ host: '127.0.0.1', port: Number(process.env.MC_TEST_PORT || 25571), username: 'DexProgress', auth: 'offline', version: '1.21.1', hideErrors: true,
    customPackets: { '1.21': { play: { toClient: { types: {
      packet_entity_metadata: 'restBuffer', packet_declare_recipes: 'restBuffer', packet_advancements: 'restBuffer', packet_declare_commands: 'restBuffer'
    } } } } }
  });
  const emit = bot._client.emit;
  bot._client.emit = function (event, ...args) {
    if (['entity_metadata', 'advancements', 'declare_commands'].includes(event)) return false;
    return emit.call(this, event, ...args);
  };
  let ready = false, failure;
  bot.once('spawn', () => { ready = true; });
  bot.on('error', error => { failure = error; });
  bot.on('kicked', reason => { failure = new Error(JSON.stringify(reason)); });
  await until(() => { if (failure) throw failure; return ready; }, 45000);
  const national = await progress('national'), legendary = await progress('legendary');
  expectPercentage(national); expectPercentage(legendary);
  if (mode === 'after-removal') {
    const old = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../../build/progress-fixture-baseline.json')));
    assert.equal(national.total, old.national.total - 2);
    assert.equal(legendary.total, old.legendary.total - 1);
    assert.equal(national.registered, old.national.registered);
    assert.equal(legendary.registered, old.legendary.registered);
    pass('Removing test datapack on restart recalculates both denominators and excludes removed species');
    return;
  }
  if (mode === 'resume-with-fixture') {
    const baseline = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../../build/progress-fixture-baseline.json')));
    assert.equal(national.registered, baseline.national.registered + 2);
    assert.equal(legendary.registered, baseline.legendary.registered + 1);
    pass('Official collection progress remains available on reconnect');
  } else {
    assert.equal(mode, 'with-fixture');
    assert.equal(national.registered, 0, 'Use a fresh disposable DexProgress player');
    assert.equal(legendary.registered, 0);
    assert(national.total > 2 && legendary.total > 1);
    pass('National and legendary show real zero percentages with nonempty dynamic totals');
    fs.mkdirSync(path.resolve(__dirname, '../../build'), { recursive: true });
    fs.writeFileSync(path.resolve(__dirname, '../../build/progress-fixture-baseline.json'), JSON.stringify({ national, legendary }));
    await give('dex_test:ordinary');
    await waitCount('national', national.registered + 1);
    assert.equal((await progress('legendary')).registered, legendary.registered);
    pass('Namespaced datapack species updates national count and percent through official data event');
    await give('dex_test:ordinary');
    assert.equal((await progress('national')).registered, national.registered + 1);
    pass('Repeated acquisition of one species does not increase species completion');
    await give('dex_test:legend');
    await waitCount('national', national.registered + 2);
    await waitCount('legendary', legendary.registered + 1);
    pass('Legendary species with no alternate forms counts via standard-form labels and official ownership');
    await give('dex_test:ordinary shiny=true');
    const shiny = await command('/dex shiny status', text => text.startsWith('Shiny:'));
    assert(shiny.includes('N/D') && !/\d+[,\.]\d+%/.test(shiny));
    pass('Shiny acquisition is not misrepresented as verified shiny capture percentage');
  }
  for (const collection of ['national', 'legendary', 'shiny']) {
    bot.chat('/dex ' + collection); await until(() => bot.currentWindow);
    const current = bot.currentWindow;
    assert.equal(current.slots[18].name, 'arrow');
    const lore = JSON.stringify(current.slots[13].customLore);
    assert(lore.includes('Conclusão:'), 'Completion label is present in transmitted item lore');
    if (collection === 'shiny') assert(lore.includes('N/D') && lore.includes('não comprova'));
    else {
      const value = await progress(collection);
      assert(lore.includes(value.percent.toFixed(2).replace('.', ',') + '%'), 'Menu percentage matches the real status response');
    }
    await bot.clickWindow(18, 0, 0);
    await until(() => bot.currentWindow && bot.currentWindow !== current);
    assert.equal(bot.currentWindow.slots[11].name, 'amethyst_shard');
    bot.closeWindow(bot.currentWindow); await until(() => !bot.currentWindow);
  }
  pass('Direct pages transmit correct percentages/N-D in item lore and back navigation works for all three collections');
}
(async () => {
  let exit = 0;
  try { await main(); }
  catch (error) { exit = 1; console.error(error); results.push({ name: 'percentage integration', status: 'failed', error: error.message }); }
  finally {
    if (bot) bot.quit();
    const target = path.resolve(__dirname, `../../build/integration-progress-${mode}.json`);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, JSON.stringify({ timestamp: new Date().toISOString(), mode, results }, null, 2));
    process.exitCode = exit;
  }
})();
