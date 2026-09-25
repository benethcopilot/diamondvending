# Release QA checklist

Before a release, run this on **each of the four jars**: the dev client against a dedicated server of the same jar, so
every packet really crosses the network (singleplayer skips encoding). It takes about ten minutes per jar. Record each
result (✅ or what was wrong) in the release PR. Needs Windows (the window scripts), Python 3 and a display.

## Run one jar

Replace `<node>` with `26.1-neoforge`, `26.1-fabric`, `1.21.1-neoforge` or `1.21.1-fabric`. One jar at a time.

1. `python tools/qa/prepare_server.py <node>` — a fresh flat world in `run/<node>-server/`, RCON on (this accepts the
   [Minecraft EULA](https://aka.ms/MinecraftEULA) for that local test server).
2. Start the server and wait for `Done (` in `run/<node>-server/logs/latest.log`: `./gradlew :<node>:runServer`
3. Start the client, wait for `Dev joined the game` in the same log, then a few seconds more (a teleport straight after
   joining can leave the camera behind):
   `./gradlew :<node>:runClient -Pdiamondvending.join=127.0.0.1`
4. For each check below: `python tools/qa/scene.py <scene> <node>`, then the input, then
   `powershell -File tools/qa/capture.ps1 build/qa/<node>/<name>.png`, and compare with "Should show".
5. `python tools/qa/scene.py stop <node>`, close the game window, and look for `ERROR` or `Exception` lines that
   mention `diamondvending` in `run/<node>-server/logs/latest.log` and `run/<node>/logs/latest.log`.

| # | Scene(s) | Input (`tools/qa/input.ps1 …`) | Capture | Should show |
|---|---|---|---|---|
| 1 | `start`, `shop` | — | `front` | The machine with items on its shelves and white tags `1 · 3`, `2 · 1`, `3 · 12`, `5 · FREE`, `8 · 64`, a red `SOLD OUT` tag for the cake (12), cookies in the tray, the display lit green (`SELECT ITEM`). |
| 2 | `buy` | `-Click right` | `bought` | The hover tooltip for button 1 (apple × 2, 3 diamonds), the display flashing `THANK YOU` or the apples falling or in the tray. |
| 3 | `setup` | `-Sneak -Click right` | `setup` | The setup screen: tabs Items (current) / Stock / Cash Box / Admin, "No problems. Happy selling!", the 3 × 4 grid with the six items, "Button 1: Apple", Amount and Price, the player inventory. Then `-Keys "{ESC}"`. |
| 4 | `hint` | `-Slot 1 -Sneak -Click right` | `hint` (right away) | "Empty both hands, then sneak + right-click to open setup." above the hotbar, and no stone block against the machine. |
| 5 | `kept`, then `kept_view` | `-Slot 1 -Click right` before `kept_view` | `kept` | A second machine at x 9–10 with a cake on button 1's shelf and a red `SOLD OUT` tag (its setup came back; its stock didn't). |
| 6 | `colors` | — | `colors` | 16 machines in a row in white, orange, magenta, light blue, yellow, lime, pink, gray, light gray, cyan, purple, blue, brown, green, red, black — each clearly its own color, fronts drawn right. |
| 7 | `icons` | `-Keys "e"` | `icons` | The inventory with red, blue, lime and black machine icons in the hotbar. Then `-Keys "{ESC}"`. |
| 8 | `manual`, wait 2 s, `read` | `-Slot 1 -Click right` | `manual_1`; `-PageDown 10`, then `manual_11` | The crafter made the manual (it's in slot 1). The book opens on "Welcome, New Franchise Owner!" (bold) with its text; page 11 is "More Admin Tools". Nothing is cut off. Then `-Keys "{ESC}"`. |

JEI, EMI and REI aren't in the dev runs. If you play a pack with one of them, check that the machine and the manual
recipes show up there too.
