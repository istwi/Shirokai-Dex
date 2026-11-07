// Minimal authenticated RCON client for the isolated loopback test server.
const net = require('node:net');
class Rcon {
  constructor() { this.nextId = 1; this.pending = new Map(); this.buffer = Buffer.alloc(0); }
  async connect(port, password) {
    this.socket = net.createConnection({ host: '127.0.0.1', port });
    this.socket.on('data', data => this.receive(data));
    this.socket.on('error', error => this.fail(error));
    this.socket.on('close', () => this.fail(new Error('RCON connection closed')));
    await new Promise((resolve, reject) => { this.socket.once('connect', resolve); this.socket.once('error', reject); });
    await this.send(3, password, 2); return this;
  }
  fail(error) {
    for (const value of this.pending.values()) { clearTimeout(value.timer); value.reject(error); }
    this.pending.clear();
  }
  receive(data) {
    this.buffer = Buffer.concat([this.buffer, data]);
    while (this.buffer.length >= 4) {
      const length = this.buffer.readInt32LE(0);
      if (length < 10 || length > 1024 * 1024) { this.fail(new Error('Invalid RCON packet length')); this.socket.destroy(); return; }
      if (this.buffer.length < length + 4) return;
      const packet = this.buffer.subarray(4, length + 4); this.buffer = this.buffer.subarray(length + 4);
      const id = packet.readInt32LE(0), type = packet.readInt32LE(4);
      if (id === -1) { this.fail(new Error('RCON authentication rejected')); return; }
      const request = this.pending.get(id);
      if (request && request.expectedType === type) {
        clearTimeout(request.timer); this.pending.delete(id); request.resolve(packet.subarray(8, -2).toString());
      }
    }
  }
  send(type, text, expectedType = 0) {
    const id = this.nextId++, payload = Buffer.from(text, 'utf8');
    const packet = Buffer.alloc(payload.length + 14);
    packet.writeInt32LE(payload.length + 10, 0); packet.writeInt32LE(id, 4); packet.writeInt32LE(type, 8); payload.copy(packet, 12);
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => { this.pending.delete(id); reject(new Error('RCON request timed out')); }, 15000);
      this.pending.set(id, { resolve, reject, timer, expectedType }); this.socket.write(packet);
    });
  }
  command(text) { return this.send(2, text); }
  close() { if (this.socket) this.socket.end(); }
}
module.exports = Rcon;
