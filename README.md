Commander Ledger - MTG Stat Tracking App Android UI and Connectivity

All UI elements are subject to change as development progresses. In its current state as an MVP, UI design is minimal, focusing more on data persistence and showcasing correct implementation of calls. Time will be spent moving towards the final submission on cleaning the UI and overall layout.

The local cache currently only caches a local player. This is included to showcase the local SQLite database is operational, if limited in functionality, as the offline mode will be expanded upon moving towards the final submission. 

# Screens
- TEST_SCREEN: This screen holds general UI elements with minimal design, to showcase basic gRPC calls. Information like who is currently logged in, player statistics, and the ability to create, join and submit matches is present. These are all core functionality of the application, and are displayed for demonstrating the MVP.
- SCRYFALL_TEST_SCREEN: This screen holds minimal UI, to solely showcase the live reading of data from the Scryfall API. Any field typed into the textbox will perform a Scryfall search, which defaults to searching for cards with that name, or similar.  Scryfall does also have special syntax, which you can find a guide for at: https://scryfall.com/docs/syntax. The checkbox acts as a physical indicator of the *is:commander* syntax search. Common searches include *card names*, or using the *o:"query"* to find cards that have text matching the exact phrasing of the passed query.  Currently the search is limited to 10 items, this value will be more mutable by the user in the final deployment. 


# Known issues with client gRPC:

My home network caused issues with the gRPC calls, leading to a DEADLINE_EXCEEDED error. Therefore I had to change the host address to 10.0.0.193. After doing this I got a successful connection. 

I alternatively got the connection to work with reverse port forwarding using the following commands in a PowerShell terminal:

& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:50051 tcp:50051

*The above command sets up the reverse port forwarding*

& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse --list

*The above command lists all connections using reverse port forwarding *

& $env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe reverse --remove tcp:50051

*The above command removes the reverse port forwading operation to reinstate normal traffic*

If using a physical device, please change the **host** parameter of the client variable in ViewModel.kt to **"localhost**. 

If you do not have issues connecting on **10.0.2.2.**, please change the **host** parameter to that value. 
