"""Sends console commands to the local test server over RCON (settings come from server/server.properties).

Usage: python tools/rcon.py "cloudewars info" "plugins"
"""
import pathlib
import re
import socket
import struct
import sys

PROPERTIES = pathlib.Path(__file__).resolve().parent.parent / "server" / "server.properties"
COLOR_CODES = re.compile("§.")


def read_properties():
    props = {}
    for line in PROPERTIES.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def packet(request_id, kind, body):
    payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def receive(sock):
    def exactly(n):
        data = b""
        while len(data) < n:
            chunk = sock.recv(n - len(data))
            if not chunk:
                raise ConnectionError("server closed the RCON connection")
            data += chunk
        return data

    (length,) = struct.unpack("<i", exactly(4))
    request_id, _kind = struct.unpack("<ii", exactly(8))
    return request_id, exactly(length - 8)[:-2].decode("utf-8", "replace")


def main(commands):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    props = read_properties()
    with socket.create_connection(("127.0.0.1", int(props.get("rcon.port", "25575"))), timeout=10) as sock:
        sock.sendall(packet(1, 3, props["rcon.password"]))
        if receive(sock)[0] == -1:
            sys.exit("RCON login failed (wrong rcon.password?)")
        for command in commands:
            sock.sendall(packet(2, 2, command))
            print(f"> {command}\n{COLOR_CODES.sub('', receive(sock)[1])}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1:])
