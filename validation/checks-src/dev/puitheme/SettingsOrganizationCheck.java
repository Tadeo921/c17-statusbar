package dev.puitheme;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Navigation may move; published share ownership and every setting destination must remain stable. */
public final class SettingsOrganizationCheck {
    private static int checks;
    private static void require(boolean condition, String why) {
        checks++; if (!condition) throw new AssertionError(why);
    }
    public static void main(String[] args) throws Exception {
        searchDestinations();
        String[] legacy = {"statusbar_layout","speed","data","wifi","label","native_network_badge","clock","carrier","font","battery",
                "notification_icons","status_hint_icons","notification_icon_overrides","status_icon_library","qs_appearance","tile_2x1_fill",
                "tile_corners","tile_icon_size","qs_entrances","shade_clock","notification_big_clock","notification_big_clock_landscape",
                "notification_stack","notification_mini_stack","notification_clear","notification_clear_landscape","qs_media","c17_highlight_removal",
                "classic_text","shade_wallpaper_portrait","shade_wallpaper_landscape","lockscreen_date","lockscreen_lock_icon","lockscreen_status_blur",
                "fluid_cloud_accent","fluid_cloud_style","fluid_cloud_capsule","lock_awake","lock_aod"};
        require(SettingsOrganization.guides().size() == legacy.length, "every original page has an explicit scope guide");
        Set<String> destinations = new HashSet<>();
        for (String category : Arrays.asList(SettingsCatalog.STATUSBAR,SettingsCatalog.NOTIFICATION,SettingsCatalog.CONTROL_CENTER,SettingsCatalog.LOCK_SCREEN,SettingsCatalog.OTHER))
            for (SettingsCatalog.NavigationSection section : SettingsCatalog.navigationSections(category))
                for (SettingsCatalog.Group group : section.groups) require(group != null && destinations.add(category + ":" + group.id), "no missing or repeated destination inside a category");
        Set<String> itemKeys = new LinkedHashSet<>();
        for (int n = 0; n < legacy.length; n++) {
            SettingsCatalog.Group group = SettingsCatalog.groups().get(n);
            require(group.id.equals(legacy[n]), "published group order must not reassign partial configuration: " + legacy[n]);
            require(!SettingsOrganization.guide(group.id).isEmpty(), "page scope guide: " + group.id);
        }
        for (SettingsCatalog.Group group : SettingsCatalog.groups()) {
            require(destinations.stream().anyMatch(it -> it.endsWith(":" + group.id)), "group is reachable: " + group.id);
            Set<String> detail = new HashSet<>();
            for (SettingsCatalog.Item item : SettingsCatalog.commonDetailItems(group)) require(detail.add(item.key), "common item unique: " + item.key);
            for (SettingsCatalog.DetailPage page : SettingsCatalog.detailPages(group)) for (SettingsCatalog.Item item : page.items)
                require(detail.add(item.key), "item has one detail tab: " + item.key);
            for (SettingsCatalog.Item item : group.items) {
                require(itemKeys.add(item.key), "a key has one catalogue owner: " + item.key);
                require(item.master || detail.contains(item.key), "folding may not lose a parameter: " + item.key);
            }
        }
        require(SettingsCatalog.groups().size() == 39, "exactly the 39 original functional pages, with no withdrawn parent controls");
        String[] parents = {"layout_controls","stack_controls","clear_controls","lock_effects"};
        String[] parentKeys = {FeatureGroups.LAYOUT,FeatureGroups.STACKS,FeatureGroups.CLEAR,FeatureGroups.LOCK};
        Set<String> newKeys = new HashSet<>(Arrays.asList(parentKeys));
        for (int n = 0; n < parents.length; n++) {
            require(SettingsCatalog.group(parents[n]) == null, "withdrawn parent destination is absent");
            require(SettingsCatalog.item(parentKeys[n]) == null && !itemKeys.contains(parentKeys[n]), "withdrawn parent has no visible switch");
            require(SettingsCatalog.hiddenKeys().contains(parentKeys[n]), "published compatibility key is deliberately hidden");
            require(ConfigTransfer.types().get(parentKeys[n]) == ConfigTransfer.Type.BOOLEAN, "old files still understand the retired Boolean");
            require(ConfigScope.section(ConfigScope.OTHER_ID).keys().contains(parentKeys[n]), "retired parent naturally belongs to hidden compatibility settings");
            require(ConfigScope.section(parents[n]) == null, "there is no ghost share section for a removed function");
        }
        for (SettingsCatalog.Group group : SettingsCatalog.groups()) for (SettingsCatalog.Item item : SettingsCatalog.searchItems(group))
            require(!newKeys.contains(item.key), "withdrawn parent cannot reappear in search");
        // Freeze the code174 portable key->section protocol, not current presentation or implementation values.
        Set<String> laterKeys = new HashSet<>(newKeys);
        laterKeys.add(BigClockWidgetsSettings.LEFT_WEIGHT);laterKeys.add(BigClockWidgetsSettings.RIGHT_WEIGHT);laterKeys.addAll(Upgrade184.DEFAULTS.keySet());laterKeys.addAll(Upgrade185.DEFAULTS.keySet());laterKeys.addAll(Upgrade186.DEFAULTS.keySet());laterKeys.addAll(Upgrade187.DEFAULTS.keySet());laterKeys.addAll(Upgrade188.DEFAULTS.keySet());
        TreeMap<String,String> owners = new TreeMap<>();
        for (ConfigScope.Section section : ConfigScope.sections()) for (String key : section.keys())
            if (!laterKeys.contains(key)) require(owners.put(key,section.id) == null, "portable key still has one owner");
        // These three fields were portable in code174. Freeze their historical
        // ownership explicitly while proving the current protocol retires them.
        for(String retired:IconManagementSettings.LEGACY_KEYS){
            require(!ConfigTransfer.types().containsKey(retired),"retired icon rules are absent from current portable registry");
            require(!owners.containsKey(retired),"retired icon rules have no current share owner");
            owners.put(retired,"status_icon_library");
        }
        require(owners.size() == 943, "all original portable settings remain, excluding later compatibility and font-weight keys");
        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<String,String> owner : owners.entrySet()) canonical.append(owner.getKey()).append('\t').append(owner.getValue()).append('\n');
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();for (byte value : digest) hash.append(String.format(java.util.Locale.ROOT,"%02x",value & 255));
        require(hash.toString().equals("9e45238e9cfaa56847c367a0e04a44a7aac3663a0c078fc15adadebf03042b0f"), "all code174 share ownership survives navigation and folding changes");
        for (boolean landscape : new boolean[]{false,true}) {
            SettingsCatalog.Group group = SettingsCatalog.group(landscape ? "notification_big_clock_landscape" : "notification_big_clock");
            String date = landscape ? NotificationBigClockSettings.landscapeKey(ClockBackdropSettings.DATE_MASK_OPACITY) : ClockBackdropSettings.DATE_MASK_OPACITY;
            String footer = landscape ? NotificationBigClockSettings.landscapeKey(ClockBackdropSettings.FOOTER_MASK_OPACITY) : ClockBackdropSettings.FOOTER_MASK_OPACITY;
            require(tab(group,date).equals("date"), "date strength belongs beside date settings");
            require(tab(group,footer).equals("footer"), "footer strength belongs beside footer settings");
        }
        require(tab(SettingsCatalog.group("notification_big_clock"),BigClockWidgetsSettings.SWAP_WITH_DATE).equals("widgets"), "date/component exchange remains reachable");
        require(SettingsOrganization.advanced("notification_big_clock","主时间立体边缘"), "large appearance controls fold separately");
        require(!SettingsOrganization.advanced("notification_big_clock","背景透出与遮罩"), "main material control is common, not buried as advanced");
        require(!SettingsOrganization.advanced("clock","大小") && !SettingsOrganization.advanced("font","字体"), "primary size and font selection stay common");
        for (String prefix : Arrays.asList("clock","shade_clock",CarrierPanels.NOTIFICATION,CarrierPanels.CONTROL,CarrierPanels.LOCKSCREEN,ClassicTextSettings.CLOCK,ClassicTextSettings.CARRIER)) {
            SettingsCatalog.Item size = SettingsCatalog.item(prefix + "_scale");
            SettingsCatalog.Item weight = SettingsCatalog.item(prefix + "_weight");
            require(size != null && size.section.endsWith("大小"), "main size has a common block: " + prefix);
            require(weight != null && !weight.section.equals(size.section), "type fine tuning is separate from primary size: " + prefix);
        }
        require(!SettingsCatalog.item(NotificationBigClockSettings.DATE_ENABLED).description.contains("在时间下方"), "date description is valid in both row arrangements");
        for (String key : Arrays.asList(BigClockWidgetsSettings.LEFT_ENABLED,BigClockWidgetsSettings.RIGHT_ENABLED))
            require(!SettingsCatalog.item(key).description.contains("大时钟下方"), "component summary allows its real swapped location");
        for (String retired : Arrays.asList(ClockBackdropSettings.NOTIFICATION_FOLLOW,ClockBackdropSettings.NOTIFICATION_RELIEF))
            require(!itemKeys.contains(retired) && SettingsCatalog.item(retired) == null, "removed notification text mode stays absent");
        System.out.println("Whole-module organisation checks passed: " + checks);
    }
    private static String tab(SettingsCatalog.Group group,String key) {
        for (SettingsCatalog.DetailPage page : SettingsCatalog.detailPages(group)) for (SettingsCatalog.Item item : page.items)
            if (key.equals(item.key)) return page.id;
        return "";
    }
    private static void searchDestinations() {
        SettingsCatalog.Group carrier = SettingsCatalog.group("carrier");
        Set<String> searchable = new HashSet<>();
        for (SettingsCatalog.Item item : SettingsCatalog.searchItems(carrier)) searchable.add(item.key);
        require(!searchable.contains(carrier.masterKey), "compatibility-only carrier master is not a dead search result");
        for (String prefix : CarrierPanels.GROUPS) require(searchable.contains(prefix + "_enabled"), "each real scene switch remains searchable");
        require(searchable.size() == carrier.items.size() - 1, "exclude only the unrendered compatibility carrier switch");
        for (boolean landscape : new boolean[]{false,true}) {
            SettingsCatalog.Group group = SettingsCatalog.group(landscape ? "shade_wallpaper_landscape" : "shade_wallpaper_portrait");
            Set<String> common = new HashSet<>();
            for (SettingsCatalog.Item item : SettingsCatalog.commonDetailItems(group)) common.add(item.key);
            for (String mode : Arrays.asList(PanelMode.CLASSIC,PanelMode.SEPARATE,PanelMode.UNKNOWN)) {
                List<SettingsCatalog.DetailPage> ordinary = SettingsCatalog.editorDetailPages(group,mode,null);
                require(ordinary.size() == (PanelMode.CLASSIC.equals(mode) ? 1 : 2), "ordinary wallpaper tabs retain native mode partition");
                for (SettingsCatalog.Item item : SettingsCatalog.searchItems(group)) {
                    if (item.master || common.contains(item.key)) continue;
                    int found = 0;
                    for (SettingsCatalog.DetailPage page : SettingsCatalog.editorDetailPages(group,mode,item.key)) {
                        for (SettingsCatalog.Item displayed : page.items) if (displayed.key.equals(item.key)) {
                            found++;
                            String reason = SettingsCatalog.detailModeReason(group,page,mode);
                            boolean activeScene = page.id.equals("classic") == mode.equals(PanelMode.CLASSIC);
                            require(PanelMode.UNKNOWN.equals(mode) ? !reason.isEmpty() : activeScene == reason.isEmpty(), "cross-mode search explains paused scene: " + mode + "/" + item.key);
                        }
                    }
                    require(found == 1, "every saved wallpaper field has exactly one search destination in every panel mode: " + item.key);
                }
            }
        }
        for (SettingsCatalog.Group group : SettingsCatalog.groups()) if (!group.id.startsWith("shade_wallpaper") && !group.id.equals("carrier"))
            require(SettingsCatalog.searchItems(group).equals(group.items), "other visible masters and parameters remain searchable");
        require(!SettingsCatalog.group("classic_text").description.contains("右上角开关"), "classic page guide names the visible controls");
        require(!SettingsCatalog.group("fluid_cloud_accent").description.contains("通知栏默认也会生效"), "retired accent scene is not advertised");
    }
}
