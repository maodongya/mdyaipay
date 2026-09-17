#!/usr/bin/env python3
"""扫描 src/main/java：缺类级 Javadoc 的 public 类型、缺 package-info 的包目录。"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    java_roots = [
        p
        for p in ROOT.glob("**/src/main/java/**/*.java")
        if "/target/" not in str(p)
    ]
    missing_class: list[str] = []
    for path in sorted(java_roots):
        text = path.read_text(encoding="utf-8", errors="replace")
        m = re.search(
            r"(?m)^(?:(?:@\w+(?:\([^)]*\))?\s*)*)*(public\s+(?:final\s+|abstract\s+|sealed\s+)?(?:class|interface|enum|record)\s+(\w+))",
            text,
        )
        if not m:
            continue
        before = text[: m.start()].rstrip()
        if not before.endswith("*/"):
            missing_class.append(str(path.relative_to(ROOT)))

    dirs_with_java = {p.parent for p in java_roots}
    missing_pkg: list[str] = []
    for d in sorted(dirs_with_java):
        if (d / "package-info.java").exists():
            continue
        if any(f.name != "package-info.java" for f in d.glob("*.java")):
            missing_pkg.append(str(d.relative_to(ROOT)))

    print(f"main Java files: {len(java_roots)}")
    print(f"missing class Javadoc: {len(missing_class)}")
    for p in missing_class:
        print(f"  {p}")
    print(f"missing package-info.java: {len(missing_pkg)}")
    for d in missing_pkg:
        print(f"  {d}")
    return 1 if missing_class or missing_pkg else 0


if __name__ == "__main__":
    sys.exit(main())
