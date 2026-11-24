"use strict";

function canReleaseNicknameReservation(snapshot, uid) {
  return Boolean(
    snapshot &&
    snapshot.exists &&
    typeof snapshot.get === "function" &&
    snapshot.get("userId") === uid
  );
}

module.exports = { canReleaseNicknameReservation };
