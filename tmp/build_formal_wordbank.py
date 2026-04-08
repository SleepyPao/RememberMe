import json
import re
from pathlib import Path
from pypdf import PdfReader

PDF = Path(r"C:\Users\21658\Downloads\听力拼写词汇.pdf")
OUT = Path(r"e:\INT4074\app\src\main\assets\words.json")
REPORT = Path(r"e:\INT4074\tmp\final_wordbank_report.txt")

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

PHRASE_TAGS = {
    "employee first": ["听写", "短语"],
    "energy saving": ["听写", "形容词"],
    "switch off": ["听写", "动词", "短语"],
}

POS_TOKEN = r"(?:n|adj|adv|v|prep|conj|pron|num|int|det)"
POS_RE = re.compile(
    rf"^(?P<word>.+?)\s+(?P<pos>{POS_TOKEN}(?:\.?/{POS_TOKEN}\.?)*\.?)\s+(?P<rest>.+)$",
    re.IGNORECASE,
)
PHRASE_RE = re.compile(r"^(?P<word>[A-Za-z][A-Za-z'\- ]+[A-Za-z])\s+(?P<rest>.+)$")
CLEAN_LINE_RE = re.compile(r"^\d+/43$|^雅思听力拼写词汇$|^词汇\s+词性\s+语义\s+备注$|^2022雅思听力拼写词汇--增补$")
MEANING_TOKEN_RE = re.compile(r"^[\u3400-\u9fff（）()、，；：/·\-\s]+$")


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
        if not note_mode and MEANING_TOKEN_RE.match(token):
            meaning_parts.append(token)
        else:
            note_mode = True
            note_parts.append(token)
    if not meaning_parts and tokens:
        meaning_parts = [tokens[0]]
        note_parts = tokens[1:]
    return " ".join(meaning_parts).strip(), " ".join(note_parts).strip()


def pos_to_tags(pos: str):
    tags = ["听写"]
    cleaned = pos.replace(" ", "").replace(".", "")
    for chunk in cleaned.split("/"):
        chunk = chunk.lower().strip()
        if chunk in POS_MAP:
            label = POS_MAP[chunk]
            if label not in tags:
                tags.append(label)
    return tags


def merge_entry(dst, src):
    if src["meaning"] and src["meaning"] not in dst["meaning"]:
        dst["meaning"] = dst["meaning"] + "；" + src["meaning"]
    if src["example"] and not dst["example"]:
        dst["example"] = src["example"]
    for tag in src["tags"]:
        if tag not in dst["tags"]:
            dst["tags"].append(tag)


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
    parsed = None
    m = POS_RE.match(line)
    if m:
        word = m.group("word").strip()
        pos = m.group("pos").strip()
        meaning, note = split_meaning_note(m.group("rest"))
        if meaning:
            parsed = {
                "id": normalize_id(word),
                "word": word,
                "phonetic": "",
                "meaning": meaning,
                "example": f"备注：{note}" if note else "",
                "level": "IELTS",
                "tags": pos_to_tags(pos),
            }
    if parsed is None:
        m = PHRASE_RE.match(line)
        if m:
            word = m.group("word").strip()
            meaning, note = split_meaning_note(m.group("rest"))
            if meaning:
                tags = PHRASE_TAGS.get(word.lower(), ["听写", "名词"])
                parsed = {
                    "id": normalize_id(word),
                    "word": word,
                    "phonetic": "",
                    "meaning": meaning,
                    "example": f"备注：{note}" if note else "",
                    "level": "IELTS",
                    "tags": tags,
                }
    if parsed is None:
        unparsed.append(line)
        continue
    key = parsed["word"].lower()
    if key in entries:
        merge_entry(entries[key], parsed)
    else:
        entries[key] = parsed

result = sorted(entries.values(), key=lambda x: x["word"].lower())
OUT.write_text(json.dumps(result, ensure_ascii=True, indent=2), encoding="utf-8")
REPORT.write_text(
    "\n".join([
        f"raw_lines={len(raw_lines)}",
        f"entries={len(result)}",
        f"unparsed={len(unparsed)}",
        "--- sample unparsed ---",
        *unparsed[:100],
    ]),
    encoding="utf-8",
)
print(f"entries={len(result)}")
print(f"unparsed={len(unparsed)}")
for item in result[:5]:
    print(item["word"], item["meaning"], item["tags"])
if unparsed:
    print("UNPARSED_SAMPLE")
    for item in unparsed[:20]:
        print(item)
