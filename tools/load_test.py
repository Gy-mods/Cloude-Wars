"""Starts fresh Paper servers with plugin jars (e.g. New Bedwars + Cloude Wars), one version after another, and reports
how they loaded. Server jars are downloaded from PaperMC the first time and kept in the cache folder.

Usage: python tools/load_test.py <cache dir> <work dir> <plugin jar> [<plugin jar> ...] -- <version> [<version> ...]
The servers stay in <work dir>/<version> afterwards, so their files can be looked at.
"""
import json
import pathlib
import re
import secrets
import shutil
import socket
import struct
import subprocess
import sys
import time
import urllib.request

JAVA = {21: r"C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin\java.exe",
        25: r"C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin\java.exe"}
PORT, RCON_PORT = 25600, 25601


def java_for(version):
    return JAVA[25] if not version.startswith("1.") else JAVA[21]  # Minecraft 26.x needs Java 25


def rcon(password, *commands):
    def packet(request_id, kind, body):
        payload = struct.pack("<ii", request_id, kind) + body.encode() + b"\x00\x00"
        return struct.pack("<i", len(payload)) + payload

    def receive(sock):
        def exactly(n):
            data = b""
            while len(data) < n:
                data += sock.recv(n - len(data))
            return data
        (length,) = struct.unpack("<i", exactly(4))
        exactly(8)
        return exactly(length - 8)[:-2].decode("utf-8", "replace")

    with socket.create_connection(("127.0.0.1", RCON_PORT), timeout=10) as sock:
        sock.sendall(packet(1, 3, password))
        receive(sock)
        out = []
        for command in commands:
            sock.sendall(packet(2, 2, command))
            out.append(re.sub("§.", "", receive(sock)))
        return out


def download(version, folder):
    """The newest Paper build of a version, from PaperMC's fill API (it wants a User-Agent)."""
    headers = {"User-Agent": "CloudeWars-dev-load-test"}
    url = f"https://fill.papermc.io/v3/projects/paper/versions/{version}/builds"
    with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=60) as response:
        build = json.load(response)[0]["downloads"]["server:default"]
    folder.mkdir(parents=True, exist_ok=True)
    target = folder / build["name"]
    with urllib.request.urlopen(urllib.request.Request(build["url"], headers=headers), timeout=300) as response:
        target.write_bytes(response.read())
    return target


def test(version, cache, work, jars):
    source = cache / version
    folder = work / version
    if folder.exists():
        shutil.rmtree(folder)
    (folder / "plugins").mkdir(parents=True)
    server_jar = next(source.glob("paper-*.jar"), None) or download(version, source)
    shutil.copy(server_jar, folder)
    for name in ("cache", "libraries", "versions"):
        if (source / name).exists():
            shutil.copytree(source / name, folder / name)
    for jar in jars:
        shutil.copy(jar, folder / "plugins" / jar.name)
    (folder / "eula.txt").write_text("eula=true\n")
    password = secrets.token_hex(12)
    (folder / "server.properties").write_text(
        f"server-port={PORT}\nserver-ip=127.0.0.1\nonline-mode=false\nlevel-type=minecraft\\:flat\n"
        f"generate-structures=false\nenable-rcon=true\nrcon.port={RCON_PORT}\nrcon.password={password}\n")
    log = folder / "console.log"
    with open(log, "w", encoding="utf-8") as out:
        process = subprocess.Popen([java_for(version), "-Xmx2G", "-jar", server_jar.name, "--nogui"], cwd=folder,
                                   stdout=out, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
    deadline = time.time() + 300
    text = ""
    while time.time() < deadline and process.poll() is None:
        text = log.read_text(encoding="utf-8", errors="replace")
        if "Done (" in text:
            break
        time.sleep(2)
    result = {"version": version, "started": "Done (" in text}
    if result["started"]:
        time.sleep(5)
        result["plugins"] = rcon(password, "plugins")[0].replace("\n", " ")
        rcon(password, "stop")
        process.wait(timeout=120)
    else:
        process.kill()
    lines = log.read_text(encoding="utf-8", errors="replace").splitlines()
    result["problems"] = [line for line in lines if re.search(r"ERROR|Exception|Could not load|NoSuchMethod|NoClassDef", line)
                          and "oshi" not in line and "Win32Exception" not in line and "No key layers" not in line][:15]
    result["warnings"] = [line for line in lines if ("CloudeWars" in line or "NewBedwars" in line) and "WARN" in line][:10]
    result["enabled"] = [name for name in ("NewBedwars", "CloudeWars") if any(f"Enabling {name}" in line for line in lines)]
    return result


def main():
    split = sys.argv.index("--")
    cache, work = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
    jars = [pathlib.Path(jar) for jar in sys.argv[3:split]]
    for version in sys.argv[split + 1:]:
        print(json.dumps(test(version, cache, work, jars), indent=1), flush=True)


if __name__ == "__main__":
    main()
