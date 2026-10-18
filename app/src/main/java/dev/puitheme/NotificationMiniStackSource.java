// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;

/** Source declaration of the same-app mini stack and its app-only acknowledgement. The record is independent of the
 * free notice and of the feature switch, and is never part of the hook settings, mirror or JSON export: an imported
 * "on" configuration therefore cannot stand in for the user having seen the declaration. */
public final class NotificationMiniStackSource {
    public static final String PROJECT = "OplusX", AUTHOR = "xxxxawl", FEATURE = "启用小堆叠（同应用）";
    public static final String URL = "https://www.coolapk.com/u/18956155";
    public static final String TITLE = "功能来源声明";
    /** The full declaration: first use, inside the page, and the About credit. */
    public static final String DECLARATION = "功能参考 OplusX「启用小堆叠（同应用）」。感谢 OplusX 作者 xxxxawl 提供实现参考。点击查看作者酷安主页。";
    /** Next to the 通知栏 entry. */
    public static final String SHORT = "同应用通知小堆叠 · 参考 OplusX / xxxxawl";
    /** The page's permanent source line. */
    public static final String INSIDE = "功能参考 OplusX · 作者 xxxxawl";
    public static final String LINK = "查看作者酷安主页";
    /** The reference's own stack code builds on this; credited separately so its origin is not attributed to one author. */
    public static final String TECHNICAL_ORIGIN = "OplusX 的通知堆叠基础来自 VivoGroupDeck test21。";
    public static final String NOTICE = DECLARATION + "\n\n本功能只呈现系统已形成的原生通知组：收起时显示完整的第一条，后面最多露出两层真实通知；"
            + "不重新分组、不排序，也不增加下拉手势。确认只记录你已看过本声明，不会自动重启系统界面。";
    private static final String PREFERENCES = "app_mini_stack_source";
    private static final String ACKNOWLEDGED_VERSION = "acknowledged_version";
    private static final int VERSION = 1;
    private NotificationMiniStackSource() { }
    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
    public static boolean acknowledged(Context context) {
        return Integer.valueOf(VERSION).equals(PreferenceWrites.values(preferences(context)).get(ACKNOWLEDGED_VERSION));
    }
    public static boolean acknowledge(Context context) {
        SharedPreferences raw = preferences(context);
        return PreferenceWrites.commit(raw, raw.edit().putInt(ACKNOWLEDGED_VERSION, VERSION),
                Collections.<String, Object>singletonMap(ACKNOWLEDGED_VERSION, VERSION), false);
    }
    /** Turning the feature on, from a switch or an import, first needs the declaration to have been acknowledged. */
    public static boolean requiresDeclaration(String key, Object value) {
        return NotificationMiniStack.ENABLED.equals(key) && Boolean.TRUE.equals(value);
    }
    /** An import that would switch the feature on cannot count as having seen the declaration. */
    public static boolean importNeedsDeclaration(java.util.Map<String, ?> imported, boolean acknowledged) {
        return !acknowledged && imported != null && requiresDeclaration(NotificationMiniStack.ENABLED, imported.get(NotificationMiniStack.ENABLED));
    }
}
