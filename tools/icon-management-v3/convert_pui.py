#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
# Copyright (C) 2026 aiingjie
"""Convert resource data from the user-supplied PUI 17.0.0.137 ZIP; never load APK code.

Only aapt2's resource/table/XML dump commands are executed. Original pathData, source alpha,
fill rules, groups and intrinsic inset geometry are preserved in restricted VectorDrawable.
This tool does not install a theme, run its shell scripts, or synthesize missing states.
"""
from __future__ import annotations
import argparse
import copy
import hashlib
import json
import math
from pathlib import Path
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE_SHA = "0dabda20b8d1d47643aa4c4198c68c6fa2bc4a8bb68deee2d11b3f446c9317a3"
NATIVE_APK_SHA = "da83ae63b6cfc43a4ce57f21ee7a54444e14dada85b24ad36d2fe74181f0a927"
ANDROID = "http://schemas.android.com/apk/res/android"
A = "{" + ANDROID + "}"
ET.register_namespace("android", ANDROID)
PACKAGES = {
    "PuiThemeSignalIcon": "cellular",
    "PuiThemeStatusIcon": "hint",
    "PuiThemeQuickPanelIcon": "tile",
    "PuiThemeBrightQSIcon": "slider",
    "PuiThemeVolumePanelInfo": "slider",
}


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical(value) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def num(value: str) -> float:
    result = float(re.sub(r"(?:dp|px)$", "", value))
    if not math.isfinite(result):
        raise ValueError("Nonfinite geometry")
    return result


def fmt(value: float) -> str:
    return format(value, ".12g")


def run(aapt: Path, *args: str) -> str:
    return subprocess.run([str(aapt), "dump", *args], check=True, capture_output=True,
                          encoding="utf-8", errors="strict").stdout


class Resources:
    def __init__(self, aapt: Path, apk: Path, archive: zipfile.ZipFile):
        self.aapt, self.apk, self.archive = aapt, apk, archive
        self.by_id, self.names, self.colors, self.cache = {}, {}, {}, {}
        self.dump = run(aapt, "resources", str(apk))
        current = None
        for line in self.dump.splitlines():
            found = re.search(r"resource (0x[0-9a-f]+) (drawable|color)/([^ ]+)", line)
            if found:
                current = found.groups()
                continue
            if current and (found := re.search(r"\(\) \(file\) (res/[^ ]+) type=XML", line)):
                self.by_id[current[0]] = found[1]
                self.names[current[2]] = found[1]
            if current and current[1] == "color" and (found := re.search(r"\(\) (#[0-9a-fA-F]{8})", line)):
                self.colors[current[0]] = found[1]
        # This is a dynamic framework RGB, converted only as an opaque mask. RGB always follows
        # the native host; source fill/stroke Alpha and shape opacity are retained independently.
        self.colors["0x0106003d"] = "#ffffffff"  # framework system_accent1_400
        self.colors["0x0106003e"] = "#ffffffff"  # framework system_accent1_500, also opaque

    def tree(self, path: str, visiting=()) -> ET.Element:
        if path in visiting or len(visiting) > 8:
            raise ValueError("Recursive drawable reference")
        if path in self.cache:
            return copy.deepcopy(self.cache[path])
        text = run(self.aapt, "xmltree", "--file", path, str(self.apk))
        stack, root = [], None
        for line in text.splitlines():
            if found := re.match(r"^( *)E: ([a-z-]+) \(line=\d+\)$", line):
                depth, tag = len(found[1]), found[2]
                if tag not in {"vector", "inset", "group", "path", "clip-path"}:
                    raise ValueError("Unsupported PUI XML node: " + tag)
                while stack and stack[-1][0] >= depth:
                    stack.pop()
                node = ET.Element(tag)
                if stack:
                    stack[-1][1].append(node)
                elif root is not None:
                    raise ValueError("Multiple XML roots")
                else:
                    root = node
                stack.append((depth, node))
            elif found := re.match(r"^ *A: http://schemas.android.com/apk/res/android:([A-Za-z]+)\(0x[0-9a-f]+\)=(.*)$", line):
                key, value = found.groups()
                value = value.split(" (Raw: ", 1)[0]
                if value.startswith('"'):
                    value = json.loads(value)
                if key in {"fillColor", "strokeColor"} and value.startswith("@"):
                    value = self.colors[value[1:]]
                if key == "fillType":
                    value = {"0": "nonZero", "1": "evenOdd"}[value]
                if key == "strokeLineCap":
                    value = {"0": "butt", "1": "round", "2": "square"}[value]
                if key == "strokeLineJoin":
                    value = {"0": "miter", "1": "round", "2": "bevel"}[value]
                if not stack:
                    raise ValueError("Attribute outside XML element")
                stack[-1][1].set(A + key, value)
        if root is None:
            raise ValueError("Empty resource XML")
        if root.tag == "inset" and A + "drawable" in root.attrib:
            reference = root.attrib.pop(A + "drawable")
            root.append(self.tree(self.by_id[reference[1:]], (*visiting, path)))
        self.cache[path] = copy.deepcopy(root)
        return root


def flatten(root: ET.Element) -> ET.Element:
    """Represent intrinsic VectorDrawable bounds and InsetDrawable padding as an exact group matrix."""
    if root.tag == "inset":
        allowed = {A + key for key in ("insetLeft", "insetTop", "insetRight", "insetBottom")}
        if set(root.attrib) - allowed or len(root) != 1:
            raise ValueError("Unsupported inset geometry")
        child = flatten(root[0])
        left, top, right, bottom = (num(root.get(A + key, "0")) for key in
                                    ("insetLeft", "insetTop", "insetRight", "insetBottom"))
        if min(left, top, right, bottom) < 0:
            raise ValueError("Negative inset")
        width, height = num(child.get(A + "width")), num(child.get(A + "height"))
        outer = ET.Element("vector", {A + "width": fmt(width + left + right) + "dp",
                                     A + "height": fmt(height + top + bottom) + "dp",
                                     A + "viewportWidth": fmt(width + left + right),
                                     A + "viewportHeight": fmt(height + top + bottom)})
        if A + "alpha" in child.attrib:
            outer.set(A + "alpha", child.get(A + "alpha"))
        group = ET.SubElement(outer, "group", {A + "translateX": fmt(left), A + "translateY": fmt(top)})
        group.extend(list(child))
        return outer
    if root.tag != "vector":
        raise ValueError("Expected vector root")
    vector = copy.deepcopy(root)
    width, height = num(vector.get(A + "width")), num(vector.get(A + "height"))
    vx, vy = num(vector.get(A + "viewportWidth")), num(vector.get(A + "viewportHeight"))
    if min(width, height, vx, vy) <= 0 or max(width, height) > 1024:
        raise ValueError("Invalid intrinsic vector bounds")
    children = list(vector)
    for node in children:
        vector.remove(node)
    vector.set(A + "viewportWidth", fmt(width))
    vector.set(A + "viewportHeight", fmt(height))
    group = ET.SubElement(vector, "group", {A + "scaleX": fmt(width / vx), A + "scaleY": fmt(height / vy)})
    group.extend(children)
    return vector


def paths(root: ET.Element) -> list[str]:
    return [node.get(A + "pathData") for node in root.iter() if A + "pathData" in node.attrib]


def path_styles(root: ET.Element):
    return [{"tag": node.tag, "attributes": dict(node.attrib)} for node in root.iter()
            if node.tag in {"path", "clip-path"}]


def audited_qs_aliases() -> dict[str, dict]:
    """Pinned OEM handleUpdateState resource branches, not guesses from active/inactive names.

    Exact classes and state contracts are recorded in ICON-V3-QS-STATE-CONTRACT.
    Missing PUI resources (auto-brightness, VPN, keyboard and ringer vibrate) stay raw candidates.
    """
    out = {}

    def add(role, resource, cls, resource_id, lines, native_state, meaning, rule, mode=None):
        row = {"resourceName": resource, "nativeResourceName": resource, "nativeResourceId": resource_id,
               "nativeMethod": cls + ".handleUpdateState", "nativeState": native_state,
               "meaning": meaning, "nativeRule": rule, "nativeApkSha256": NATIVE_APK_SHA,
               "stateEvidence": "docs/ICON-V3-QS-STATE-CONTRACT.zh-CN.md#3",
               "nativeCodeEvidence": cls + ".txt:" + lines}
        if mode is not None:
            row["nativeThreeStageMode"] = mode
        out[role] = row

    package = "com.oplus.systemui.qs.tiles."
    for target, off, on, cls, off_id, on_id, lines in (
        ("auto_color_temperature", "status_bar_screen_temperature", "status_bar_screen_temperature_active", "AutoScreenTemperatureTile", "0x7f08199f", "0x7f0819a0", "308-351"),
        ("battery_saver", "status_bar_qs_battery_inactive", "status_bar_qs_battery_active", "OplusBatterySaverTile", "0x7f0818e4", "0x7f0818e3", "170-195"),
        ("dnd", "status_bar_qs_dnd_inactive", "status_bar_qs_dnd_active", "OplusDndTile", "0x7f08191b", "0x7f08191a", "515-650"),
        ("no_disturb", "status_bar_qs_no_disturb_inactive", "status_bar_qs_no_disturb_active", "NoDisturbTile", "0x7f08194c", "0x7f08194b", "247-279"),
        ("otg", "status_bar_qs_otg_inactive", "status_bar_qs_otg_active", "FlavorOneOtgTile", "0x7f08194e", "0x7f08194d", "400-420"),
        ("envelope", "status_bar_qs_red_packet_inactive", "status_bar_qs_red_packet_active", "EnvelopeTile", "0x7f081953", "0x7f081952", "319-368"),
        ("super_power_saver", "status_bar_qs_super_power_save_inactive", "status_bar_qs_super_power_save_active", "SuperPowerSaveModeTile", "0x7f081966", "0x7f081965", "374-419"),
        ("vibration", "status_bar_qs_vibrate_inactive", "status_bar_qs_vibrate_active", "VibrationModeTile", "0x7f081970", "0x7f08196f", "369-413"),
    ):
        rule = "audited BooleanState: raw1=inactive/off, raw2=active/on; preserve native availability"
        add(f"tile.{target}.off", off, package + cls, off_id, lines, 1, "功能关闭", rule)
        add(f"tile.{target}.on", on, package + cls, on_id, lines, 2, "功能开启", rule)
    add("tile.super_power_saver.unavailable", "status_bar_qs_super_power_save_unavailable",
        package + "SuperPowerSaveModeTile", "0x7f081967", "374-419", 0, "不可用",
        "native relatedTileAvailable/thermal override -> raw0; do not substitute off")

    for target, resource, cls, resource_id, lines in (
        ("alipay", "status_bar_qs_alipay_inactive", package + "AliPayTile", "0x7f0818df", "208-222"),
        ("ali_scan", "status_bar_qs_aliscan_inactive", package + "AliScanTile", "0x7f0818e0", "189-203"),
        ("wechat_pay", "status_bar_qs_wechat_pay_inactive", "com.oplusos.systemui.qs.tiles.WeChatPayTile", "0x7f081975", "181-195"),
        ("wechat_scan", "status_bar_qs_wechat_scan_inactive", package + "WeChatScanTile", "0x7f081976", "206-220"),
        ("calculator", "status_bar_qs_calculator_inactive", package + "CalculatorTile", "0x7f081908", "266-280"),
        ("clean", "status_bar_qs_clean_inactive", package + "CleanTile", "0x7f08190e", "190-204"),
        ("lock_screen", "status_bar_qs_lock_inactive", package + "LockTile", "0x7f08193c", "189-195"),
        ("shutdown", "oplus_qs_shutdown_tile_icon", package + "OplusShutDownTile", "0x7f0814b8", "228-242"),
        ("multi_system", "status_bar_qs_multi_system_inactive", package + "MultiSystemTile", "0x7f081944", "217-231"),
        ("screenshot", "status_bar_qs_screenshot_inactive", package + "ScreenshotTile", "0x7f08195e", "488-520"),
    ):
        add(f"tile.{target}.default", resource, cls, resource_id, lines, 1, "操作入口",
            "raw1 is an action entry, not an off state; unknown/disabled states remain native")

    for state, resource, resource_id, mode in (
        ("silent", "status_bar_qs_mute_active", "0x7f081946", 0),
        ("ring", "status_bar_qs_mute_inactive", "0x7f081947", 2),
    ):
        add("tile.ringer_mode." + state, resource, package + "ThreeStageRingerModeTile", resource_id,
            "309-407", 2, "静音" if mode == 0 else "响铃",
            "raw state stays 2; use actual threeStageMode 0=silent, 1=vibrate, 2=ring", mode)
    for state, native_state in (("idle", 1), ("recording", 2), ("unavailable", 0)):
        unavailable = state == "unavailable"
        add("tile.screen_record." + state,
            "status_bar_qs_screenrecord_disable" if unavailable else "status_bar_qs_screenrecord_full_record",
            package + "ScreenRecorderTile", "0x7f081958" if unavailable else "0x7f081959",
            "848-1000", native_state, {"idle": "可开始录屏", "recording": "录屏活动", "unavailable": "不可用"}[state],
            "raw1/raw2 both assign full_record; disabled raw0 assigns screenrecord_disable")
    return out


def known_roles() -> dict[str, tuple[str, str]]:
    out = {}
    for level in range(5):
        out["wifi." + str(level)] = ("PuiThemeSignalIcon", f"stat_signal_wifi_signal_{level}_os17")
        out["cellular." + str(level)] = ("PuiThemeSignalIcon", f"stat_signal_signal_lte_single_{level}_os17")
    out["cellular.none"] = ("PuiThemeSignalIcon", "stat_signal_signal_null_lte")
    for level in range(5):
        for row in ("primary", "secondary"):
            out[f"cellular.stacked_{row}.{level}"] = ("PuiThemeSignalIcon", f"stat_signal_lte_signal_stacked_{row}_{level}")
    out["cellular.stacked_primary.none"] = ("PuiThemeSignalIcon", "stat_signal_lte_signal_stacked_primary_noservice")
    for role, name in {
        "bluetooth.off": "stat_sys_data_bluetooth", "bluetooth.on": "stat_sys_data_bluetooth_connected",
        "location.default": "stat_sys_location", "alarm_clock.default": "stat_sys_alarm",
        "zen.default": "stat_sys_dnd", "headset.default": "stat_sys_headset", "vpn.default": "stat_sys_vpn_ic",
        "nfc.default": "stat_sys_nfc", "airplane.default": "stat_sys_airplane_mode",
    }.items():
        out["hint." + role] = ("PuiThemeStatusIcon", name)
    for role, name in {"wifi": "wifi", "bluetooth": "bluetooth", "mobile_data": "data", "hotspot": "hotspot",
                       "airplane": "airplane", "nfc": "nfc", "cast": "cast", "flashlight": "flashlight",
                       "rotation": "rotation", "location": "location"}.items():
        for state, suffix in {"on": "active", "off": "inactive", "unavailable": "unavailable"}.items():
            if role in {"wifi", "rotation"} and state == "unavailable":
                continue
            out[f"tile.{role}.{state}"] = ("PuiThemeQuickPanelIcon", f"status_bar_qs_{name}_{suffix}")
    out["tile.dark.on"] = ("PuiThemeQuickPanelIcon", "darkmode_on_status_bar")
    out["tile.dark.off"] = ("PuiThemeQuickPanelIcon", "darkmode_off_status_bar")
    # SensorPrivacyToggleTile.handleUpdateState: !isSensorBlocked -> raw2 (allowed/on),
    # blocked -> raw1 (blocked/off). Both OEM overrides call super without reversing it.
    # See docs/ICON-V3-QS-STATE-CONTRACT.zh-CN.md section 2; do not interpret on as privacy blocking.
    for name, target in (("camera", "camera"), ("mic", "microphone")):
        for state, suffix in (("on", "allowed"), ("off", "blocked"), ("unavailable", "unavailable")):
            out[f"tile.{target}.{state}"] = ("PuiThemeQuickPanelIcon", f"status_bar_qs_{name}_{suffix}")
    out["footer.settings.default"] = ("PuiThemeQuickPanelIcon", "status_bar_qs_footer_settings_icon_in_bg")
    out["footer.more.default"] = ("PuiThemeQuickPanelIcon", "status_bar_qs_more")
    out["slider.brightness.default"] = ("PuiThemeBrightQSIcon", "status_bar_brightness_auto_mode_icon_sun")
    out["slider.brightness_auto.default"] = ("PuiThemeBrightQSIcon", "status_bar_brightness_auto_mode_icon_alphabet")
    for role, evidence in audited_qs_aliases().items():
        out[role] = ("PuiThemeQuickPanelIcon", evidence["resourceName"])
    return out


def write_java_aliases(path: Path, aliases: list[dict]):
    """Compatibility for already-installed immutable PUI libraries, pinned to exact original bytes."""
    quote = lambda value: json.dumps(value, ensure_ascii=False)
    rows = "\n".join("        add(rows," + ",".join(quote(row[key]) for key in ("role", "rawRole", "asset", "assetSha256")) + ");"
                     for row in sorted(aliases, key=lambda row: row["role"]))
    source = '''// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.*;

/** Generated by tools/icon-management-v3/convert_pui.py from audited original resource aliases.
 * Existing immutable libraries are not rewritten. Unknown identities or changed source bytes never match. */
public final class PuiIconAliases {
    private static final class Alias {
        final String role,source,sha256;
        Alias(String role,String source,String sha256){this.role=role;this.source=source;this.sha256=sha256;}
    }
    private static final Map<String,Alias> ALIASES;
    static {
        Map<String,Alias> rows=new HashMap<>();
''' + rows + '''
        ALIASES=Collections.unmodifiableMap(rows);
    }
    private static void add(Map<String,Alias> rows,String role,String raw,String source,String hash){rows.put(role,new Alias(raw,source,hash));}
    /** Direct formal roles always win. Fallback is only the pinned original built-in PUI source. */
    public static String sourceRole(IconManagementManifest manifest,String role){
        if(manifest==null||role==null)return null;
        if(manifest.icons.containsKey(role))return role;
        if(!"pui-coloros17".equals(manifest.id)||!"天伞桜&PanL".equals(manifest.author))return null;
        Alias alias=ALIASES.get(role);if(alias==null)return null;IconManagementManifest.Asset source=manifest.icons.get(alias.role);
        return source!=null&&"mask".equals(source.render)&&alias.source.equals(source.source)&&alias.sha256.equals(source.sha256)?alias.role:null;
    }
    private PuiIconAliases(){ }
}
'''
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(source, encoding="utf-8", newline="\n")


def write_zip(path: Path, entries: dict[str, bytes]):
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (2026, 10, 18, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, data, compresslevel=9)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("--aapt2", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=ROOT / "app/src/main/assets/icon-management-v3/pui-coloros17.zip")
    parser.add_argument("--report", type=Path, default=ROOT / "tools/icon-management-v3/pui-provenance.json")
    parser.add_argument("--aliases-java", type=Path, default=ROOT / "app/src/main/java/dev/puitheme/PuiIconAliases.java")
    args = parser.parse_args()
    source = args.source.read_bytes()
    if digest(source) != SOURCE_SHA:
        raise ValueError("Expected the exact user-provided PUI 17.0.0.137 source ZIP")
    from io import BytesIO
    origin = zipfile.ZipFile(BytesIO(source))
    entries, lookup, provenance = {}, {}, []
    with tempfile.TemporaryDirectory(prefix="c17-pui-v3-") as temporary:
        for apk_name, scope in PACKAGES.items():
            member = "system/product/overlay/" + apk_name + ".apk"
            apk_bytes = origin.read(member)
            apk = Path(temporary) / (apk_name + ".apk")
            apk.write_bytes(apk_bytes)
            archive = zipfile.ZipFile(BytesIO(apk_bytes))
            if len(archive.infolist()) > 512 or any(re.search(r"(?:^|/)classes\d*\.dex$|\.so$", item.filename) for item in archive.infolist()):
                raise ValueError("Resource APK contains executable data")
            resources = Resources(args.aapt2, apk, archive)
            file_names = {}
            for name, path in resources.names.items():
                file_names.setdefault(path, []).append(name)
            for item in archive.infolist():
                if not re.fullmatch(r"res/drawable(?:-night)?/[^/]+\.xml", item.filename):
                    continue
                original = resources.tree(item.filename)
                converted = flatten(original)
                if paths(original) != paths(converted) or path_styles(original) != path_styles(converted):
                    raise AssertionError("Path geometry, source alpha or fill/stroke style changed")
                asset = "assets/pui-" + digest((apk_name + "/" + item.filename).encode())[:24] + ".xml"
                data = ET.tostring(converted, encoding="utf-8", xml_declaration=True)
                entries[asset] = data
                lookup[apk_name, item.filename] = asset
                for name in file_names.get(item.filename, []):
                    lookup[apk_name, name] = asset
                names = file_names.get(item.filename, [Path(item.filename).stem])
                raw_name = names[0] + ("_night" if item.filename.startswith("res/drawable-night/") else "")
                raw_name = re.sub("[^a-z0-9_]", "_", raw_name.lower()).strip("_")
                if not raw_name or len(raw_name) > 47:
                    raw_name = raw_name[:36] + "_" + digest(item.filename.encode())[:8]
                # cellular.<state> is a closed known target; source-only variants must not masquerade as extra states.
                raw_role = ("hint.pui_signal." if apk_name == "PuiThemeSignalIcon" else scope + ".pui_resource.") + raw_name
                provenance.append({"apk": member, "apkSha256": digest(apk_bytes), "entry": item.filename,
                                   "entrySha256": digest(archive.read(item)), "resourceNames": names,
                                   "resourceTableDeclared": bool(file_names.get(item.filename)),
                                   "rawRole": raw_role, "asset": asset, "assetSha256": digest(data),
                                   "pathDataSha256": digest(canonical(paths(original))),
                                   "pathStyleSha256": digest(canonical(path_styles(original))), "geometryPreserved": True})
    icons = {}
    for item in provenance:
        if item["rawRole"] in icons:
            raise ValueError("Duplicate raw resource role: " + item["rawRole"])
        icons[item["rawRole"]] = {"source": item["asset"], "render": "mask", "sha256": item["assetSha256"]}
    aliases, qs_evidence = [], audited_qs_aliases()
    for role, source_ref in known_roles().items():
        # This actual XML is present in the source APK but absent from its resources table.
        # The OEM raw2 branch uses exactly this resource name. Do not generalize to basename guesses.
        explicit_entry = "res/drawable/status_bar_qs_super_power_save_active.xml" if role == "tile.super_power_saver.on" else None
        asset = lookup.get(source_ref)
        if asset is None and explicit_entry is not None:
            asset = lookup[source_ref[0], explicit_entry]
        if asset is None:
            raise ValueError("Audited source resource is missing: " + str(source_ref))
        icons[role] = {"source": asset, "render": "mask", "sha256": digest(entries[asset])}
        source_row = next(item for item in provenance if item["asset"] == asset)
        alias = {"role": role, "resourceName": source_ref[1], "apk": source_row["apk"],
                 "entry": source_row["entry"], "rawRole": source_row["rawRole"],
                 "asset": asset, "assetSha256": source_row["assetSha256"],
                 "resourceTableDeclared": source_row["resourceTableDeclared"]}
        if role in qs_evidence:
            alias.update(qs_evidence[role])
        if role.startswith("cellular.stacked_"):
            alias.update(layout="preserve-original-viewport", nativeResourceName=source_ref[1],
                         stateEvidence="docs/ICON-V3-NATIVE-CONTRACT.zh-CN.md",
                         meaning="双排主信号" if role.startswith("cellular.stacked_primary.") else "双排副信号")
        if role in {"tile.camera.on", "tile.camera.off", "tile.microphone.on", "tile.microphone.off"}:
            allowed = role.endswith(".on")
            alias.update(nativeState=2 if allowed else 1,
                         meaning="允许访问" if allowed else "阻止访问",
                         stateEvidence="docs/ICON-V3-QS-STATE-CONTRACT.zh-CN.md#2",
                         nativeMethod="com.android.systemui.qs.tiles.SensorPrivacyToggleTile.handleUpdateState",
                         nativeRule="value=!isSensorBlocked; blocked ? state=1 : state=2")
        aliases.append(alias)
    manifest = {"format": "c17-statusbar-icons", "version": 3, "fallback": "native", "id": "pui-coloros17",
                "name": "PUI ColorOS 17 · 17.0.0.137", "author": "天伞桜&PanL",
                "license": "用户提供的 PUI 原主题素材；未声明公开再分发许可，本轮仅本地内置。",
                "icons": icons, "transitions": {}}
    entries["manifest.json"] = canonical(manifest)
    readme = ("# PUI ColorOS 17 真实资源库\n\n来源：用户提供的 PUI Theme For ColorOS 17_v17.0.0.137.zip\n"
              "作者：天伞桜&PanL；原素材非 C17 原创。\n源 ZIP SHA-256: " + SOURCE_SHA + "\n\n"
              "仅提取 Signal/Status/QuickPanel/BrightQS/VolumePanelInfo 五个资源 APK 的静态 Drawable 数据；"
              "不安装 APK、不加载 DEX、不执行主题脚本。原 pathData、Alpha、fill rule、group 与 inset 几何保留。"
              "动态框架 RGB 只转为不透明蒙版颜色，实际前景色跟随宿主；原路径透明度保留。\n\n"
              "正式 role 仅关联可证明的状态；pui_resource.* 是完整原始候选，不表示本版原生适配。"
              "新增控制中心映射逐项核对原生 handleUpdateState 的资源分支；动作只对应 default，录屏 raw1/raw2 原生共用 full_record。"
              "双排 primary/secondary 单独选源并保留同一原始 viewport，上下排不能分别裁白放大；源包缺 secondary none，继续原生。"
              "Wi-Fi/旋转/深色缺少的 unavailable、Wi-Fi none、离散电池各档、音量四档、媒体及设备状态保持原生。"
              "相机/麦克风按已核准的原生状态：on=允许访问(allowed/raw2)，off=阻止访问(blocked/raw1)，原图不改。"
              "亮度主图只用无 A 的 sun，A 只用实际自动模式 overlay。"
              "本库没有伪造 Lottie 动画，缺态不借用主图案。\n")
    entries["README.md"] = readme.encode("utf-8")
    entries["LICENSE.txt"] = ("素材作者：天伞桜&PanL。源主题未附公开再分发许可。用户已明确要求整理其所提供素材并内置 App；"
                              "仅供本项目私有开发使用，不进入公开图标规范仓库。转换工具代码的 GPL-3.0-only 不改变原素材权利。\n").encode("utf-8")
    write_zip(args.output, entries)
    report = {"sourceFile": args.source.name, "sourceSha256": SOURCE_SHA, "sourceBytes": len(source),
              "tool": "aapt2 dump resources/xmltree; no APK execution", "sourceApks": list(PACKAGES),
              "nativeApkSha256": NATIVE_APK_SHA,
              "formalRoles": sorted(known_roles()), "formalAliases": aliases,
              "sourceDrawables": len(provenance), "roleCount": len(icons),
              "animationCount": 0, "bundleSha256": digest(args.output.read_bytes()), "resources": provenance}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_bytes(json.dumps(report, ensure_ascii=False, indent=2).encode("utf-8") + b"\n")
    write_java_aliases(args.aliases_java, aliases)
    print(f"PUI v3: {len(known_roles())} formal roles + {len(provenance)} original resource candidates; "
          f"{args.output.stat().st_size} bytes; SHA-256 {report['bundleSha256']}")


if __name__ == "__main__":
    main()
