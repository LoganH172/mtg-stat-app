# Milestone 1: Java greeting server

This is an independent Java 17 / Gradle 8.8 project. It does not load or change
the Android Gradle project. Versions are pinned for a reproducible starter,
not claimed to be the newest releases.

## Open and build on Windows

Open this `server` directory as a Gradle project in IntelliJ IDEA or Android
Studio. Choose JDK 17 as the project's Gradle JVM. For terminal commands, open
PowerShell in this directory and run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat clean build
```

The first build downloads Gradle and Maven dependencies. The wrapper means you
do not need a separate system Gradle installation. `generateProto` creates
Java message classes and service stubs under `build/generated`; do not edit or
commit those generated files. The authoritative contract is `../proto/greeting.proto`.

On this computer the initial dependency download needed Windows' certificate
store. If downloads fail with a certificate error, set this in the same
PowerShell terminal and retry (it keeps certificate verification enabled):

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
```

For Git's certificate error on this computer, use
`git -c http.sslBackend=schannel fetch origin` (or the same prefix with `push`).

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

Two tests start an actual gRPC server on a dynamically assigned local port,
send requests through a generated client, and check named and blank-name
responses. Results: `build/reports/tests/test/index.html`.

## Handoff to Logan

- Contract: `proto/greeting.proto`, service `commander.v1.Greeter`, RPC `SayHello`.
- Input: `HelloRequest.name`; output: `HelloReply.message`.
- Server port: 50051, plaintext for the local demonstration.
- The Android project should generate its own Android-compatible messages and
  client stubs from the same shared proto. Do not copy server generated Java
  files or add the server Netty transport to Android.
- Android emulator on this computer: connect to `10.0.2.2:50051`.
- Physical Android phone: use this computer's LAN IPv4 address on the same
  network. Allow the server through Windows Firewall on a trusted private
  network if needed. `localhost` on the phone means the phone itself.
- Logan owns Android permission, client generation, background RPC execution,
  and UI wiring. Agree on the shared contract before extending it. Never reuse
  an existing protobuf field number for a different meaning.

A passing Java client proves the server works; the milestone's Android-to-server
demo still requires Logan's client to display the response.

## Branch and upload workflow

The starting branch is `backend/milestone-1`, based on current `main` so the
Android work remains present. Only `server/` and `proto/` should be in this change.
From the repository root:

```powershell
git status
git diff -- android
git add -- server proto
git diff --cached --stat
git diff --cached
git commit -m "Add Milestone 1 Java gRPC greeting server"
git push -u origin backend/milestone-1
```

Review staged files before committing. Exclude `.gradle`, `build`, IDE settings,
and credentials. GitHub may open a browser sign-in for the push; use your own
collaborator account. After pushing, open a pull request with base `main` and
compare `backend/milestone-1`; let Logan review before merging. Do not force push.

For later work: `git fetch origin`, then `git switch backend/milestone-1`.
Only merge updated `origin/main` after saving your own work; ask for help if a
conflict appears rather than accepting changes blindly.

References:
- https://grpc.io/docs/languages/java/quickstart/
- https://github.com/google/protobuf-gradle-plugin
- https://developer.android.com/studio/run/emulator-networking
