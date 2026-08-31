#!/usr/bin/env python3
"""Send NotQuests E2E commands through Minecraft RCON."""
import argparse
import json
import pathlib
import re
import socket
import struct
import sys
import time

REQUEST_COMMAND = 2
AUTH = 3


def packet(request_id, packet_type, payload):
    data = payload.encode("utf-8") + b"\x00\x00"
    return struct.pack("<iii", len(data) + 8, request_id, packet_type) + data


def recv_exact(sock, size):
    chunks = []
    remaining = size
    while remaining > 0:
        chunk = sock.recv(remaining)
        if not chunk:
            raise ConnectionError("RCON connection closed")
        chunks.append(chunk)
        remaining -= len(chunk)
    return b"".join(chunks)


def recv_packet(sock):
    size = struct.unpack("<i", recv_exact(sock, 4))[0]
    payload = recv_exact(sock, size)
    request_id, packet_type = struct.unpack("<ii", payload[:8])
    body = payload[8:-2].decode("utf-8", errors="replace")
    return request_id, packet_type, body


def connect(host, port, password, timeout):
    deadline = time.monotonic() + timeout
    last_error = None
    while time.monotonic() < deadline:
        try:
            sock = socket.create_connection((host, port), timeout=5)
            sock.sendall(packet(1, AUTH, password))
            request_id, _, body = recv_packet(sock)
            if request_id == -1:
                sock.close()
                raise RuntimeError("RCON authentication failed")
            # Some servers send an empty auth response before the actual auth response.
            if request_id != 1:
                request_id, _, body = recv_packet(sock)
                if request_id == -1:
                    sock.close()
                    raise RuntimeError("RCON authentication failed")
            return sock
        except OSError as error:
            last_error = error
            time.sleep(0.5)
    raise RuntimeError(f"RCON did not become available: {last_error}")


def load_commands(path):
    commands = []
    for raw in pathlib.Path(path).read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        command = re.sub(r"\s+#.*$", "", line).strip()
        if command.startswith("/"):
            command = command[1:]
        if command:
            commands.append(command)
    return commands


def send(sock, request_id, command):
    sock.sendall(packet(request_id, REQUEST_COMMAND, command))
    while True:
        response_id, _, body = recv_packet(sock)
        if response_id == request_id:
            return body
        # Paper can send additional chunks for a previous command after the first response packet.
        # Those packets keep the previous request id; discard them and wait for this command's id.
        if response_id < request_id:
            continue
        raise RuntimeError(f"Unexpected RCON response id {response_id}; expected {request_id}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--commands")
    parser.add_argument("--command", action="append", default=[])
    parser.add_argument("--responses")
    parser.add_argument("--timeout", type=float, default=60)
    args = parser.parse_args()

    commands = list(args.command)
    if args.commands:
        commands.extend(load_commands(args.commands))
    if not commands:
        return 0

    responses = []
    output = pathlib.Path(args.responses) if args.responses else None
    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text("", encoding="utf-8")
    with connect(args.host, args.port, args.password, args.timeout) as sock:
        for index, command in enumerate(commands, start=2):
            body = send(sock, index, command)
            entry = {"command": command, "response": body}
            responses.append(entry)
            if output:
                with output.open("a", encoding="utf-8") as file:
                    file.write(json.dumps(entry, ensure_ascii=False) + "\n")
            if body:
                print(body)
    return 0


if __name__ == "__main__":
    sys.exit(main())
