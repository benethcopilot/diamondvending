"""Prepares run/<node>-server for release QA (docs/qa-checklist.md): a fresh flat, peaceful, offline-mode world with
RCON on for tools/qa/rcon.py. Writing eula=true means agreeing to the Minecraft EULA (https://aka.ms/MinecraftEULA).

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
}

node = sys.argv[1]
server = pathlib.Path(__file__).resolve().parents[2] / "run" / f"{node}-server"
server.mkdir(parents=True, exist_ok=True)
shutil.rmtree(server / "world", ignore_errors=True)  # every QA run starts from a new world
(server / "eula.txt").write_text("eula=true\n", encoding="utf-8")
(server / "server.properties").write_text("".join(f"{key}={value}\n" for key, value in PROPERTIES.items()), encoding="utf-8")
print(f"prepared {server}")
