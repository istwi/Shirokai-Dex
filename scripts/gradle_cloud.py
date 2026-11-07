#!/usr/bin/env python3
"""Run the Wrapper using inherited proxy settings, without changing global config."""
import os
import subprocess
import sys
from pathlib import Path
from urllib.parse import urlsplit

root = Path(__file__).resolve().parents[1]
environment = os.environ.copy()
environment.setdefault("GRADLE_USER_HOME", str(root.parent / ".cache" / "shirokai-gradle"))
command = [str(root / "gradlew")]
for scheme in ("http", "https"):
    value = environment.get(scheme + "_proxy") or environment.get(scheme.upper() + "_PROXY")
    if not value:
        continue
    proxy = urlsplit(value)
    if proxy.scheme != "http" or not proxy.hostname or proxy.username or proxy.password:
        sys.exit("Proxy format requires separate secure configuration; credentials will not be printed.")
    command += [f"-D{scheme}.proxyHost={proxy.hostname}", f"-D{scheme}.proxyPort={proxy.port or 80}"]
command += ["-Dhttp.nonProxyHosts=localhost|127.*|[::1]"]
command += sys.argv[1:] or ["--no-daemon", "build"]
sys.exit(subprocess.call(command, cwd=root, env=environment))
