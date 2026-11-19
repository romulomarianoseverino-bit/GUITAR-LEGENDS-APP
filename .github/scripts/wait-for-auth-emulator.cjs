const http = require("node:http");
const crypto = require("node:crypto");

const host = "127.0.0.1";
const port = 9099;
const maxAttempts = 30;
let attempts = 0;

function retry(error) {
  if (attempts >= maxAttempts) {
    console.error(`Auth Emulator HTTP readiness failed after ${maxAttempts} attempts: ${error.message}`);
    process.exitCode = 1;
    return;
  }
  setTimeout(probe, 1000);
}

function probe() {
  attempts += 1;
  const payload = JSON.stringify({
    email: `auth-emulator-probe-${crypto.randomUUID()}@guitar-legends.test`,
    password: "password123",
    returnSecureToken: true
  });

  const request = http.request({
    hostname: host,
    port,
    path: "/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key-for-emulator",
    method: "POST",
    timeout: 3000,
    headers: {
      "content-type": "application/json",
      "content-length": Buffer.byteLength(payload)
    }
  }, response => {
    let body = "";
    response.setEncoding("utf8");
    response.on("data", chunk => { body += chunk; });
    response.on("end", () => {
      let parsed = {};
      try { parsed = JSON.parse(body); } catch (_) {}

      if (response.statusCode === 200 && typeof parsed.localId === "string") {
        console.log(`Auth Emulator HTTP sign-up OK on http://${host}:${port}; test UID received`);
        return;
      }

      const error = new Error(`HTTP ${response.statusCode}: ${body.slice(0, 500)}`);
      if (response.statusCode >= 500 || response.statusCode === 429) {
        retry(error);
      } else {
        console.error(`Auth Emulator responded but sign-up API validation failed: ${error.message}`);
        process.exitCode = 1;
      }
    });
  });

  request.on("timeout", () => request.destroy(new Error("request timed out")));
  request.on("error", retry);
  request.end(payload);
}

probe();
