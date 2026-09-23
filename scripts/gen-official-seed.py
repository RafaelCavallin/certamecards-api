#!/usr/bin/env python3
import json
import sys
import unicodedata
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "certamecards-prototipo" / "dados" / "baralhos-iniciais.json"
TARGET = Path(__file__).resolve().parents[1] / "src/main/resources/db/migration/V6__seed_official_decks.sql"
NAMESPACE = uuid.UUID("6f1c2a52-8d0b-4f7e-9a53-2c0d7e1b4a10")
SUBJECT_ALIASES = {"Língua Portuguesa": "Português"}
INITIAL_STATUS = "draft"


def normalize(text):
    decomposed = unicodedata.normalize("NFD", text.strip())
    stripped = "".join(ch for ch in decomposed if not unicodedata.combining(ch))
    return " ".join(stripped.lower().split())


def literal(text):
    return "'" + text.replace("'", "''") + "'"


def deck_sql(deck):
    subject = normalize(SUBJECT_ALIASES.get(deck["subject"], deck["subject"]))
    deck_id = uuid.uuid5(NAMESPACE, deck["id"])
    return (
        "INSERT INTO decks (id, owner_id, subject_id, name, origin, official_status, card_count, search_text)\n"
        f"SELECT '{deck_id}', NULL, s.id, {literal(deck['title'])}, 'official_subscription', "
        f"'{INITIAL_STATUS}', {len(deck['cards'])}, {literal(normalize(deck['title']))}\n"
        f"FROM subjects s WHERE s.normalized_name = {literal(subject)}\n"
        "ON CONFLICT (id) DO NOTHING;\n"
    )


def card_sql(deck, card):
    deck_id = uuid.uuid5(NAMESPACE, deck["id"])
    card_id = uuid.uuid5(NAMESPACE, f"{deck['id']}/{card['id']}")
    return (
        "INSERT INTO cards (id, deck_id, front, back, source) VALUES "
        f"('{card_id}', '{deck_id}', {literal(card['front'])}, {literal(card['back'])}, "
        f"{literal(card['source'])}) ON CONFLICT (id) DO NOTHING;\n"
    )


def main():
    decks = sorted(json.loads(SOURCE.read_text(encoding="utf-8")), key=lambda d: d["order"])
    parts = [deck_sql(d) + "".join(card_sql(d, c) for c in d["cards"]) for d in decks]
    TARGET.write_text("\n".join(parts), encoding="utf-8")
    total = sum(len(d["cards"]) for d in decks)
    print(f"{len(decks)} decks, {total} cards -> {TARGET}", file=sys.stderr)


if __name__ == "__main__":
    main()
