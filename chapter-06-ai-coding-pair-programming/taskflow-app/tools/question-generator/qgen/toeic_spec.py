#!/usr/bin/env python3
"""TOEIC Listening & Reading format spec: constants, blueprint builder, structure checker.

This file is the single place where the test structure lives. If ETS changes the
format, edit PARTS below and regenerate assets/toeic_lr_spec.json with:

    python toeic_spec.py spec --out ../assets/toeic_lr_spec.json

Commands
    spec                     print the machine-readable spec as JSON
    blueprint                build a slot list for a full test, a section, or chosen parts
    check FILE               check a question set against the spec (exit 1 on errors)

Examples
    python toeic_spec.py blueprint --mode full --out blueprint.json
    python toeic_spec.py blueprint --parts 5,6
    python toeic_spec.py check set.json --parts 5
    python toeic_spec.py check mock.json --mode full
    python toeic_spec.py check some_items.json --partial
"""
import argparse
import json
import sys
from collections import Counter, defaultdict

LETTERS = "ABCD"
TOTAL_QUESTIONS = 200

# Typical single-passage question counts in Part 7 (10 passages, 29 questions).
# Official structure fixes the total (29) and the 2-4 range; this ordering is a
# common pattern, not a guarantee.
SINGLE_PATTERN = [2, 2, 2, 2, 3, 3, 3, 4, 4, 4]

# groups: list of (passage_type, number_of_questions_in_group)
PARTS = {
    1: dict(name="Photographs", section="listening", options=4, options_printed=False,
            audio=True, image=True, graphics_possible=False,
            groups=[(None, 1)] * 6),
    2: dict(name="Question-Response", section="listening", options=3, options_printed=False,
            audio=True, image=False, graphics_possible=False,
            groups=[(None, 1)] * 25),
    3: dict(name="Conversations", section="listening", options=4, options_printed=True,
            audio=True, image=False, graphics_possible=True,
            groups=[(None, 3)] * 13),
    4: dict(name="Talks", section="listening", options=4, options_printed=True,
            audio=True, image=False, graphics_possible=True,
            groups=[(None, 3)] * 10),
    5: dict(name="Incomplete Sentences", section="reading", options=4, options_printed=True,
            audio=False, image=False, graphics_possible=False,
            groups=[(None, 1)] * 30),
    6: dict(name="Text Completion", section="reading", options=4, options_printed=True,
            audio=False, image=False, graphics_possible=False,
            groups=[(None, 4)] * 4),
    7: dict(name="Reading Comprehension", section="reading", options=4, options_printed=True,
            audio=False, image=False, graphics_possible=False,
            groups=[("single", n) for n in SINGLE_PATTERN]
                   + [("double", 5)] * 2 + [("triple", 5)] * 3),
}

_next = 1
for _p in sorted(PARTS):
    PARTS[_p]["questions"] = sum(size for _, size in PARTS[_p]["groups"])
    PARTS[_p]["first_number"] = _next
    PARTS[_p]["last_number"] = _next + PARTS[_p]["questions"] - 1
    _next = PARTS[_p]["last_number"] + 1
assert _next - 1 == TOTAL_QUESTIONS, "part counts must sum to 200"

SECTIONS = {
    "listening": dict(questions=100, minutes_approx=45),
    "reading": dict(questions=100, minutes=75),
}


# ---------------------------------------------------------------- spec / blueprint

def spec_dict():
    parts = {}
    for p, s in PARTS.items():
        sizes = [size for _, size in s["groups"]]
        entry = {
            "name": s["name"],
            "section": s["section"],
            "questions": s["questions"],
            "number_range": [s["first_number"], s["last_number"]],
            "options_per_question": s["options"],
            "options_printed": s["options_printed"],
            "needs_audio": s["audio"],
            "needs_image": s["image"],
            "graphics_possible": s["graphics_possible"],
            "group_count": len(s["groups"]),
            "group_sizes": sorted(set(sizes)),
        }
        if p == 7:
            kinds = defaultdict(lambda: {"groups": 0, "questions": 0})
            for kind, size in s["groups"]:
                kinds[kind]["groups"] += 1
                kinds[kind]["questions"] += size
            entry["passage_sets"] = dict(kinds)
            entry["single_passage_pattern_guidance"] = SINGLE_PATTERN
        parts[str(p)] = entry
    return {
        "test": "TOEIC Listening & Reading",
        "basis": "format in use since the 2018 redesign; verified Nov 2026",
        "total_questions": TOTAL_QUESTIONS,
        "sections": SECTIONS,
        "score_scale": {"total": [10, 990], "per_section": [5, 495], "step": 5},
        "parts": parts,
    }


def build_blueprint(parts):
    slots = []
    for p in parts:
        s = PARTS[p]
        number = s["first_number"]
        for gi, (ptype, size) in enumerate(s["groups"], start=1):
            gid = f"p{p}-g{gi:02d}"
            for pos in range(1, size + 1):
                slot = {
                    "number": number,
                    "part": p,
                    "section": s["section"],
                    "group_id": gid,
                    "position_in_group": pos,
                    "group_size": size,
                    "num_options": s["options"],
                    "needs_audio": s["audio"],
                    "needs_image": s["image"],
                }
                if ptype is not None:
                    slot["passage_type"] = ptype
                slots.append(slot)
                number += 1
    return slots


# ---------------------------------------------------------------- checking

def check(items, expected, full_mode, partial):
    errors, warnings = [], []
    by_part = defaultdict(list)
    numbers = Counter()

    for idx, it in enumerate(items, start=1):
        if not isinstance(it, dict):
            errors.append(f"item #{idx}: not a JSON object")
            continue
        p = it.get("part")
        if p not in PARTS:
            errors.append(f"item #{idx}: invalid part {p!r} (expected 1-7)")
            continue
        by_part[p].append((idx, it))
        n = it.get("number")
        if n is not None:
            if not isinstance(n, int) or isinstance(n, bool):
                errors.append(f"item #{idx}: number must be an integer, got {n!r}")
            else:
                numbers[n] += 1

    # counts per part
    if not partial:
        for p in expected:
            want = PARTS[p]["questions"]
            have = len(by_part.get(p, []))
            if have != want:
                errors.append(f"Part {p} ({PARTS[p]['name']}): {have} question(s), expected {want}")
        for p in by_part:
            if p not in expected:
                warnings.append(f"Part {p} items present but Part {p} was not in the expected set")

    # per-item checks
    for p, lst in sorted(by_part.items()):
        spec = PARTS[p]
        keys_expected = list(LETTERS[: spec["options"]])
        for idx, it in lst:
            label = it.get("id", f"#{idx}")
            opts = it.get("options")
            if opts is not None:
                if isinstance(opts, dict):
                    if sorted(opts.keys()) != keys_expected:
                        errors.append(f"{label} (Part {p}): option keys {sorted(opts.keys())}, expected {keys_expected}")
                elif isinstance(opts, list):
                    if len(opts) != spec["options"]:
                        errors.append(f"{label} (Part {p}): {len(opts)} options, expected {spec['options']}")
                else:
                    errors.append(f"{label} (Part {p}): options must be an object or list")
            ans = it.get("answer")
            if ans is not None and ans not in keys_expected:
                errors.append(f"{label} (Part {p}): answer {ans!r} not in {keys_expected}")
            n = it.get("number")
            if full_mode and isinstance(n, int) and not isinstance(n, bool):
                if not spec["first_number"] <= n <= spec["last_number"]:
                    errors.append(
                        f"{label} (Part {p}): number {n} outside {spec['first_number']}-{spec['last_number']}")

    for n, c in numbers.items():
        if c > 1:
            errors.append(f"number {n} used {c} times")

    # group checks
    for p in sorted(by_part):
        spec = PARTS[p]
        sizes = [size for _, size in spec["groups"]]
        if max(sizes) == 1:
            continue
        groups = defaultdict(list)
        missing = 0
        for idx, it in by_part[p]:
            g = it.get("group_id")
            if g is None:
                missing += 1
            else:
                groups[g].append(it)
        if missing:
            errors.append(f"Part {p}: {missing} item(s) have no group_id (questions in this part come in groups)")
            continue

        if p in (3, 4, 6):
            want = sizes[0]
            for g, its in groups.items():
                if len(its) != want:
                    errors.append(f"Part {p}: group {g!r} has {len(its)} question(s), expected {want}")
            if p in expected and not partial and len(groups) != len(sizes):
                errors.append(f"Part {p}: {len(groups)} group(s), expected {len(sizes)}")

        if p == 7:
            info = {}
            for g, its in groups.items():
                kinds = {it.get("passage_type") for it in its}
                if len(kinds) > 1:
                    errors.append(f"Part 7: group {g!r} mixes passage_type values {sorted(map(str, kinds))}")
                info[g] = (next(iter(kinds)), len(its))
            if any(k is None for k, _ in info.values()):
                warnings.append("Part 7: passage_type missing on some groups; cannot verify single/double/triple structure")
                continue
            for g, (kind, n) in info.items():
                if kind not in ("single", "double", "triple"):
                    errors.append(f"Part 7: group {g!r} has invalid passage_type {kind!r}")
                elif kind == "single" and not 2 <= n <= 4:
                    errors.append(f"Part 7: single passage {g!r} has {n} question(s), expected 2-4")
                elif kind in ("double", "triple") and n != 5:
                    errors.append(f"Part 7: {kind} set {g!r} has {n} question(s), expected 5")
            if p in expected and not partial:
                tot = defaultdict(lambda: [0, 0])
                for kind, n in info.values():
                    tot[kind][0] += 1
                    tot[kind][1] += n
                want = {"single": (10, 29), "double": (2, 10), "triple": (3, 15)}
                for kind, (wg, wq) in want.items():
                    hg, hq = tot.get(kind, [0, 0])
                    if (hg, hq) != (wg, wq):
                        errors.append(
                            f"Part 7 {kind}: {hg} set(s) / {hq} question(s), expected {wg} / {wq}")

    return errors, warnings, by_part


# ---------------------------------------------------------------- CLI

def parse_parts(text):
    try:
        parts = sorted({int(x) for x in text.split(",") if x.strip()})
    except ValueError:
        raise SystemExit(f"--parts must be comma-separated integers, got {text!r}")
    bad = [p for p in parts if p not in PARTS]
    if bad:
        raise SystemExit(f"unknown part(s): {bad} (valid: 1-7)")
    return parts


def parts_from_mode(mode):
    return {
        "full": [1, 2, 3, 4, 5, 6, 7],
        "listening": [1, 2, 3, 4],
        "reading": [5, 6, 7],
    }[mode]


def emit(text, out):
    if out:
        with open(out, "w", encoding="utf-8") as f:
            f.write(text + "\n")
        print(f"wrote {out}", file=sys.stderr)
    else:
        print(text)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("spec", help="print the machine-readable spec")
    s.add_argument("--out")

    b = sub.add_parser("blueprint", help="build a slot list")
    b.add_argument("--mode", choices=["full", "listening", "reading"], default=None)
    b.add_argument("--parts", help="comma-separated part numbers, e.g. 5,6")
    b.add_argument("--out")

    c = sub.add_parser("check", help="check a question set against the spec")
    c.add_argument("file")
    c.add_argument("--mode", choices=["full", "listening", "reading"], default=None)
    c.add_argument("--parts", help="comma-separated part numbers expected to be complete")
    c.add_argument("--partial", action="store_true",
                   help="skip completeness counts; only check structure of what is present")

    args = ap.parse_args(argv)

    if args.cmd == "spec":
        emit(json.dumps(spec_dict(), indent=2, ensure_ascii=False), args.out)
        return 0

    if args.cmd == "blueprint":
        if args.parts:
            parts = parse_parts(args.parts)
        else:
            parts = parts_from_mode(args.mode or "full")
        slots = build_blueprint(parts)
        doc = {"parts": parts, "total_questions": len(slots), "slots": slots}
        emit(json.dumps(doc, indent=2, ensure_ascii=False), args.out)
        counts = Counter(sl["part"] for sl in slots)
        print("Blueprint: " + ", ".join(f"Part {p}={counts[p]}" for p in parts)
              + f" (total {len(slots)})", file=sys.stderr)
        return 0

    # check
    try:
        with open(args.file, encoding="utf-8") as f:
            data = json.load(f)
    except (OSError, json.JSONDecodeError) as exc:
        print(f"ERROR: cannot read {args.file}: {exc}")
        return 1
    if isinstance(data, dict):
        data = data.get("questions")
    if not isinstance(data, list):
        print('ERROR: expected a list of items or {"questions": [...]}')
        return 1

    full_mode = args.mode == "full"
    if args.parts:
        expected = parse_parts(args.parts)
    elif args.mode:
        expected = parts_from_mode(args.mode)
    else:
        expected = sorted({it.get("part") for it in data
                           if isinstance(it, dict) and it.get("part") in PARTS})

    errors, warnings, by_part = check(data, expected, full_mode, args.partial)
    for e in errors:
        print(f"ERROR   {e}")
    for w in warnings:
        print(f"WARNING {w}")
    print()
    print(f"Checked {len(data)} item(s): {len(errors)} error(s), {len(warnings)} warning(s)")
    print("Per part: " + ", ".join(f"Part {p}={len(by_part[p])}" for p in sorted(by_part)))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
