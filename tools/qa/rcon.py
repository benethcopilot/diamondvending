"""Sends commands to the release-QA server over RCON and prints the replies (docs/qa-checklist.md).

Usage: python tools/qa/rcon.py "time set noon" "give Dev minecraft:diamond 10"
"""
import socket
import struct
import sys

HOST, PORT, PASSWORD = "localhost", 25575, "qa"  # as tools/qa/prepare_server.py sets them


def _packet(request_id, kind, body):
    payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def _read_exactly(sock, size):
    data = b""
    while len(data) < size:
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise ConnectionError("the server closed the RCON connection")
        data += chunk
    return data


def _reply(sock):
    size = struct.unpack("<i", _read_exactly(sock, 4))[0]
    payload = _read_exactly(sock, size)
    request_id = struct.unpack("<i", payload[:4])[0]
    return request_id, payload[8:-2].decode("utf-8", "replace")


def send(*commands):
    """Runs each command as the server console, in order, and returns the replies."""
    replies = []
    with socket.create_connection((HOST, PORT), timeout=10) as sock:
        sock.sendall(_packet(1, 3, PASSWORD))
        if _reply(sock)[0] == -1:
            raise SystemExit("RCON login failed - is the server from prepare_server.py running?")
        for number, command in enumerate(commands, start=2):
            sock.sendall(_packet(number, 2, command))
            replies.append(_reply(sock)[1])
    return replies


if __name__ == "__main__":
    for reply in send(*sys.argv[1:]):
        print(reply)
