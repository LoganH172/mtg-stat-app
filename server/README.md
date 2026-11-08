# Milestone 1: Java greeting server

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

Two tests start an actual gRPC server on a dynamically assigned local port,
send requests through a generated client, and check named and blank-name
responses. Results: `build/reports/tests/test/index.html`.

## Handoff to Logan

- Contract: `proto/greeting.proto`, service `commander.v1.Greeter`, RPC `SayHello`.
- Input: `HelloRequest.name`; output: `HelloReply.message`.
- Server port: 50051, plaintext just for the local demonstration.
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
