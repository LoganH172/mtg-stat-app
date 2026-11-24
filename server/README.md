# Commander Ledger Java server

This is an independent Java 17 project. It doesn't load or change
the Android Gradle project. Versions are pinned for a reproducible starter,
not claimed to be the newest releases.

## Open and build on Windows

Open this `server` directory as a Gradle project in Android
Studio. Choose JDK 17 as the project's Gradle JVM. And for terminal commands, open
a terminal in Andriod Studio and run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat clean build
```

The first build downloads Gradle and Maven dependencies. The wrapper means you
do not need a separate system Gradle installation. `generateProto` creates
Java message classes and service stubs under `build/generated`; do not edit or
commit those generated files. The authoritative contract is `../proto/greeting.proto`.


## Demonstrate the RPC

In terminal 1, from `server`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat run
```

Leave it running. In terminal 2, also from `server`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat greet -Pname=Android
```

Expected response: `Hello Android! Connection successful.`
Stop the server with Ctrl+C. To use another port, run the server with
`run --args="50052"` and the client with `greet -Pport=50052`.

## Tests

```powershell
.\gradlew.bat test
```

The greeting tests start an actual gRPC server on a dynamically assigned local
port and check named, blank, and whitespace-padded requests. Results are in
`build/reports/tests/test/index.html`.

## Run registration and login locally

Use a MySQL database with the `commander` schema. The `players` and `sessions`
tables need the `salt` columns and their existing insert triggers. Those
triggers generate salts and store hashes. Use a MySQL host and port reachable
from the Java process.

```powershell
$env:COMMANDER_DB_URL = 'jdbc:mysql://127.0.0.1:3306/commander?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:COMMANDER_DB_USER = 'server'
$env:COMMANDER_DB_PASSWORD = Read-Host 'Local MySQL password'
.\gradlew.bat run
```

With those variables set, the server exposes authentication, profile stats,
and match-room RPCs on the same port as the greeting service. Registration
immediately signs the player in.
Login creates a fresh session each time. Both return the player profile, a
session token, and an expiration in Unix seconds. Sessions last 24 hours.
Android should treat the token as an opaque string. The server port is
plaintext for local development. An unset `COMMANDER_DB_URL` starts the
greeting demo by itself.

## Test an authenticated call

After registering or logging in, send `authorization: Bearer <token>` as gRPC
metadata with an empty `GetProfileStatsRequest`. The server checks the stored
session hash and expiration and loads the signed-in player's stats. A missing,
invalid, or expired token returns `UNAUTHENTICATED`.

A fresh account returns zero wins, losses, and games played. The current schema
identifies recorded results by a winning game member. A game with a missing or
unreadable commander color identity still counts toward the overall record.
An empty JSON color list represents a colorless commander.

## Test creating and joining a match

Apply the match-room migration after the base `commander` schema is installed.
It creates room and participant tables and can be rerun on an existing
database. Run this from the `server` directory, adjusting the host and port
to match your MySQL setup.

```powershell
Get-Content -Raw .\sql\001-match-rooms.sql |
    mysql --host=127.0.0.1 --port=3306 --user=server --password commander
```

1. Sign in on two clients and attach each client's own bearer token.
2. On the first client, call `CreateMatch` with a fresh request UUID. The
   reply includes the creator, a match UUID, and an eight-character room code.
   Reuse the request UUID if the same create attempt times out.
3. On the second client, call `JoinMatch` with that room code. The reply
   includes the same match UUID and both players. Retrying the join keeps one
   participant entry for that player.
4. On the first client, call `GetMatch` with the match UUID to refresh the
   participant list.

Leave the playgroup ID unset for an unaffiliated room. If a playgroup is
supplied, both clients must belong to it. These three calls support the
open-room phase. The server handles commander selection and results in
separate RPCs planned for the match flow.

## Handoff to Logan

- Contract: `proto/greeting.proto`, service `commander.v1.Greeter`, RPC `SayHello`.
- Input: `HelloRequest.name`; output: `HelloReply.message`.
- Server port: 50051 for the local demonstration.
- The Android project should generate its own Android-compatible messages and
  client stubs from the same shared proto. Don't copy server generated Java
  files or add the server Netty transport to Android.
- Android emulator on my computer: connect to `10.0.2.2:50051`.
- Physical Android phone: use this computer's LAN IPv4 address on the same
  network. Allow the server through Windows Firewall on a trusted private
  network if needed. `localhost` on the phone means the phone itself.
- Logan owns the Android permission, client generation, background RPC execution,
  and UI wiring.

A passing Java client proves the server works; the milestone's Android-to-server
demo still requires Logan's client to display the response.
