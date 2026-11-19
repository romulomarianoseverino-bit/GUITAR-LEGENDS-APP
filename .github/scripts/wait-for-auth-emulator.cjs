const net = require("node:net");

const host = "127.0.0.1";
const port = 9099;
const maxAttempts = 30;
let attempts = 0;

function probe() {
  attempts += 1;
  const socket = net.createConnection({ host, port });
  socket.setTimeout(1000);

  socket.once("connect", () => {
    console.log(`Auth Emulator TCP OK on ${host}:${port}`);
    socket.end();
  });

  socket.once("error", retry);
  socket.once("timeout", () => {
    socket.destroy();
    retry();
  });
}

function retry() {
  if (attempts >= maxAttempts) {
    console.error(`Auth Emulator did not accept connections on ${host}:${port} after ${maxAttempts} attempts`);
    process.exitCode = 1;
    return;
  }
  setTimeout(probe, 1000);
}

probe();
