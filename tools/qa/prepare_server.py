"""Prepares run/<node>-server for release QA (docs/qa-checklist.md): a fresh flat, peaceful, offline-mode world with
RCON on for tools/qa/rcon.py. Writing eula=true means agreeing to the Minecraft EULA (https://aka.ms/MinecraftEULA).
Also sets a few options of the dev client in run/<node>, so it joins straight away with a clear screen (CLIENT_OPTIONS).

Usage: python tools/qa/prepare_server.py 26.1-neoforge
"""
import pathlib
import shutil
import sys

PROPERTIES = {
    "online-mode": "false",  # the dev clients have no Microsoft account
    "server-ip": "127.0.0.1",  # this machine only: the game port and RCON (weak password) stay off the network
    "enable-rcon": "true",
    "rcon.port": "25575",
    "rcon.password": "qa",
    "broadcast-rcon-to-ops": "false",  # keep the scene commands out of the chat, so they don't cover the screenshots
    "level-type": "minecraft\\:flat",
    "generate-structures": "false",
    "difficulty": "peaceful",
    "spawn-protection": "0",
    "view-distance": "6",
    "simulation-distance": "6",
    "motd": "Diamond Vending QA",
    "level-name": "qa-world",  # its own world, so a world someone plays on this dev server is never touched
}
NODES = {"26.1-neoforge", "26.1-fabric", "1.21.1-neoforge", "1.21.1-fabric"}

# A client's first start shows a welcome screen that holds up the auto-join (and a stray key there turns the narrator
# on), a first multiplayer join asks about third-party servers, and the tutorial's toasts cover the screenshots.
CLIENT_OPTIONS = {"onboardAccessibility": "false", "narrator": "0", "skipMultiplayerWarning": "true", "tutorialStep": "none"}

if len(sys.argv) != 2 or sys.argv[1] not in NODES:
    sys.exit(f"usage: python tools/qa/prepare_server.py <node>, where <node> is one of {', '.join(sorted(NODES))}")
node = sys.argv[1]
run = pathlib.Path(__file__).resolve().parents[2] / "run"
server = run / f"{node}-server"
server.mkdir(parents=True, exist_ok=True)
shutil.rmtree(server / PROPERTIES["level-name"], ignore_errors=True)  # every QA run starts from a new QA world
(server / "eula.txt").write_text("eula=true\n", encoding="utf-8")
(server / "server.properties").write_text("".join(f"{key}={value}\n" for key, value in PROPERTIES.items()), encoding="utf-8")
options = run / node / "options.txt"
lines = options.read_text(encoding="utf-8").splitlines() if options.exists() else []
lines = [line for line in lines if line.split(":", 1)[0] not in CLIENT_OPTIONS]
lines += [f"{key}:{value}" for key, value in CLIENT_OPTIONS.items()]
options.parent.mkdir(parents=True, exist_ok=True)
options.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"prepared {server} and the client options in {options}")
