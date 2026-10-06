"""Prepares run/1.20.1-forge-floor/ for the floor check (docs/qa-checklist.md): a real Forge server of the oldest Forge the
jar asks for (deps.forge_loader_min), with the release jar in mods/ and the same settings as prepare_server.py. Downloads
Forge's installer from maven.minecraftforge.net the first time. Writing eula=true means agreeing to the Minecraft EULA
(https://aka.ms/MinecraftEULA).

Usage: python tools/qa/prepare_floor_server.py <versions/1.20.1-forge/build/libs/diamondvending-forge-*.jar> <java 17>
"""
import os
import pathlib
import re
import shutil
import subprocess
import sys
import urllib.request

from prepare_server import RUN, prepare_client, prepare_server

ROOT = pathlib.Path(__file__).resolve().parents[2]
MINECRAFT = "1.20.1"


def floor():
    """The oldest Forge the jar asks for, from stonecutter.properties.toml."""
    table = (ROOT / "stonecutter.properties.toml").read_text(encoding="utf-8").split(f'[forge."{MINECRAFT}"]', 1)[1]
    return re.search(r'deps\.forge_loader_min\s*=\s*"([^"]+)"', table).group(1)


def main(jar, java):
    forge = f"{MINECRAFT}-{floor()}"
    server = RUN / "1.20.1-forge-floor"
    server.mkdir(parents=True, exist_ok=True)
    installer = server / f"forge-{forge}-installer.jar"
    if not installer.exists():
        url = f"https://maven.minecraftforge.net/net/minecraftforge/forge/{forge}/forge-{forge}-installer.jar"
        # The maven refuses Python's default user agent.
        request = urllib.request.Request(url, headers={"User-Agent": "diamondvending-qa"})
        with urllib.request.urlopen(request) as response:
            installer.write_bytes(response.read())
    if not (server / "libraries" / "net" / "minecraftforge" / "forge" / forge).exists():
        with open(server / "installer.log", "w", encoding="utf-8") as log:  # its output is long; the log keeps it
            subprocess.run([java, "-jar", str(installer), "--installServer", str(server)], cwd=server, check=True,
                           stdout=log, stderr=subprocess.STDOUT)
    mods = server / "mods"
    shutil.rmtree(mods, ignore_errors=True)  # only the jar under test
    mods.mkdir()
    shutil.copy2(jar, mods / jar.name)
    prepare_server(server)
    prepare_client("1.20.1-forge")
    args = f"libraries/net/minecraftforge/forge/{forge}/{'win' if os.name == 'nt' else 'unix'}_args.txt"
    print(f"prepared Forge {forge} with {jar.name} in {server}\nstart it there with:\n  \"{java}\" @user_jvm_args.txt @{args} nogui")


if __name__ == "__main__":
    if len(sys.argv) != 3 or not pathlib.Path(sys.argv[1]).is_file():
        sys.exit("usage: python tools/qa/prepare_floor_server.py <forge jar> <java 17 executable>")
    main(pathlib.Path(sys.argv[1]), sys.argv[2])
