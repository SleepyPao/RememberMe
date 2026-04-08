import json
import re
from pathlib import Path
from pypdf import PdfReader

PDF = Path(r"C:\Users\21658\Downloads\听力拼写词汇.pdf")
OUT = Path(r"e:\INT4074\tmp\parsed_words.json")
REPORT = Path(r"e:\INT4074\tmp\parsed_report.txt")

POS_MAP = {
    "n": "名词",
    "adj": "形容词",
    "adv": "副词",
    "v": "动词",
    "prep": "介词",
    "conj": "连词",
    "pron": "代词",
    "num": "数词",
    "int": "感叹词",
    "det": "限定词",
}

POS_RE = re.compile(
    r"^(?P<word>.+?)\s+(?P<pos>(?:n|adj|adv|v|prep|conj|pron|num|int|det)(?:\./(?:n|adj|adv|v|prep|conj|pron|num|int|det)\.?)*\.?)\s+(?P<rest>.+)$",
    re.IGNORECASE,
)

CLEAN_LINE_RE = re.compile(r"^\d+/43$|^雅思听力拼写词汇$|^词汇\s+词性\s+语义\s+备注$")
CHINESE_START_RE = re.compile(r"^[\u3400-\u9fff（(]")
LATIN_RE = re.compile(r"[A-Za-z]")
MEANING_RE = re.compile(r"^[\u3400-\u9fff（）()、，；：/·\-\s]+$")


def normalize_id(word: str) -> str:
    value = word.lower().strip()
    value = re.sub(r"[^a-z0-9]+", "-", value)
    value = re.sub(r"-+", "-", value).strip("-")
    return value or "entry"


def split_meaning_note(rest: str):
    rest = rest.strip()
    if not rest:
        return "", ""
    tokens = rest.split()
    meaning_parts = []
    note_parts = []
    note_mode = False
    for token in tokens:
        if not note_mode:
            if MEANING_RE.match(token):
                meaning_parts.append(token)
            else:
                note_mode = True
                note_parts.append(token)
        else:
            note_parts.append(token)
    if not meaning_parts and tokens:
        meaning_parts.append(tokens[0])
        note_parts = tokens[1:]
    return " ".join(meaning_parts).strip(), " ".join(note_parts).strip()


def pos_to_tags(pos: str):
    tags = ["听写"]
    for chunk in pos.replace(".", "").split("/"):
        chunk = chunk.strip().lower()
        if chunk in POS_MAP:
            label = POS_MAP[chunk]
            if label not in tags:
                tags.append(label)
    return tags

raw_lines = []
reader = PdfReader(str(PDF))
for page in reader.pages:
    text = page.extract_text() or ""
    for line in text.splitlines():
        line = line.strip()
        if not line or CLEAN_LINE_RE.match(line):
            continue
        raw_lines.append(line)

entries = {}
unparsed = []
for line in raw_lines:
    m = POS_RE.match(line)
    if not m:
        unparsed.append(line)
        continue
    word = m.group("word").strip()
    pos = m.group("pos").strip()
    rest = m.group("rest").strip()
    meaning, note = split_meaning_note(rest)
    if not meaning:
        unparsed.append(line)
        continue
    key = word.lower()
    entry = {
        "id": normalize_id(word),
        "word": word,
        "phonetic": "",
        "meaning": meaning,
        "example": f"备注：{note}" if note else "",
        "level": "IELTS",
        "tags": pos_to_tags(pos),
    }
    if key in entries:
        old = entries[key]
        if note and not old["example"]:
            old["example"] = f"备注：{note}"
        for tag in entry["tags"]:
            if tag not in old["tags"]:
                old["tags"].append(tag)
        if meaning not in old["meaning"]:
            old["meaning"] = old["meaning"] + "；" + meaning
    else:
        entries[key] = entry

result = list(entries.values())
result.sort(key=lambda x: x["word"].lower())
OUT.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
REPORT.write_text(
    "\n".join([
        f"raw_lines={len(raw_lines)}",
        f"entries={len(result)}",
        f"unparsed={len(unparsed)}",
        "--- sample unparsed ---",
        *unparsed[:200],
    ]),
    encoding="utf-8",
)
print(f"entries={len(result)} unparsed={len(unparsed)}")
print("sample unparsed:")
for item in unparsed[:30]:
    print(item)
