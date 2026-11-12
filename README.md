Commander Ledger - MTG Stat Tracking App Android UI and Connectivity

All UI elements are subject to change as development progresses. 

# Screens
- Log-In Screen: This is where the user will login with a username and password
- Home Screen: This is where player stats, and recently played games will be displayed.
- Game Screen: This is a more granular view of the games the user has played and their associated win data.
- Playgroup Screen: This will host a playgroup leaderboard, among other meta statistics (hopefully)


# Known issues with client gRPC:

My home network caused issues with the gRPC calls, leading to a DEADLINE_EXCEEDED error. Therefore I had to change the host address to 10.0.0.193. After doing this I got a successful connection. 

I alternatively got the connection to work with reverse port forwarding using the following commands in a PowerShell terminal:

& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:50051 tcp:50051

*The above command sets up the reverse port forwarding*

& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse --list

*The above command lists all connections using reverse port forwarding *

& $env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe reverse --remove tcp:50051

*The above command removes the reverse port forwading operation to reinstate normal traffic*
