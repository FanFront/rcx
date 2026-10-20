#!/usr/bin/env python3
"""Validate Simplified Chinese Android resources without needing an Android SDK.

Usage: python3 scripts/check_chinese_resources.py
"""
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent / "app/src/main/res"
DEFAULT = ROOT / "values"
CHINESE = ROOT / "values-zh-rCN"
PLACEHOLDER = re.compile(r"%\d+\$(?:\.\d+)?[a-z]", flags=re.IGNORECASE)


def document(path):
    try:
        return ET.parse(path).getroot()
    except ET.ParseError as exc:
        raise AssertionError(f"Invalid Android XML in {path}: {exc}") from exc


def item_map(root, element):
    values = [item for item in root.findall(element) if item.get("name")]
    keys = [item.get("name") for item in values]
    duplicates = [key for key, count in Counter(keys).items() if count > 1]
    assert not duplicates, f"Duplicate {element} resources: {duplicates}"
    return {item.get("name"): item for item in values}


def indexed_placeholders(value):
    return sorted(PLACEHOLDER.findall(value or ""))


def list_values(root, name):
    element = item_map(root, "array").get(name)
    if element is None:
        element = item_map(root, "integer-array").get(name)
    assert element is not None, f"Missing array: {name}"
    return ["".join(node.itertext()) for node in element.findall("item")]


def check():
    base = document(DEFAULT / "strings.xml")
    localized = document(CHINESE / "strings.xml")
    strings = item_map(base, "string")
    translations = item_map(localized, "string")

    for name, string in strings.items():
        if string.get("translatable") == "false":
            continue
        assert name in translations, f"Missing Simplified Chinese string: {name}"
        expected = indexed_placeholders("".join(string.itertext()))
        actual = indexed_placeholders("".join(translations[name].itertext()))
        assert expected == actual, (f"Placeholder mismatch in {name}: "
                                    f"expected {expected}, got {actual}")

    for name, entries in item_map(base, "plurals").items():
        if entries.get("translatable") == "false":
            continue
        assert name in item_map(localized, "plurals"), f"Missing Chinese plural: {name}"

    default_arrays = document(DEFAULT / "arrays.xml")
    base_providers = document(DEFAULT / "arrays_translatable.xml")
    translated_providers = document(CHINESE / "arrays_translatable.xml")
    expected = len(list_values(default_arrays, "provider_ids"))
    for root, name in [
        (default_arrays, "provider_icons"),
        (base_providers, "provider_names"),
        (base_providers, "provider_summaries"),
        (translated_providers, "provider_names"),
        (translated_providers, "provider_summaries"),
    ]:
        assert len(list_values(root, name)) == expected, (
            f"{name} count must match {expected} provider IDs"
        )
    locales = list_values(default_arrays, "locales")
    labels = list_values(default_arrays, "locale_display_names")
    assert len(locales) == len(labels), "Locale list and native labels differ"
    assert "zh-CN" in locales, "Chinese must be selectable"
    assert "简体中文" in labels, "Chinese must be visibly named in native language"

    print(f"OK: {len(translations)} Simplified Chinese strings, "
          f"{expected} provider names/summaries, {len(locales)} locale options")


if __name__ == "__main__":
    try:
        check()
    except AssertionError as exc:
        print(f"Resource validation FAILED: {exc}", file=sys.stderr)
        sys.exit(1)
