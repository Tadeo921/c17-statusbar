package dev.puitheme;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** The thanks wall: the list in the APK is valid and carries every avatar it names, a download is validated before it is kept, the
 * cache round-trips, and the sources are the project's public repository over HTTPS. */
public final class ThanksWallCheck {
    private static int checks;
    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }

    private static final String HEAD = "{\"schema_version\":1,\"revision\":3,\"updated_at\":\"2026-10-08\","
            + "\"groups\":[{\"id\":\"donation\",\"title\":\"捐赠支持\"},{\"id\":\"support\",\"title\":\"其他支持\"}],\"people\":[";

    private static String person(String id, String fields) {
        return "{\"id\":\"" + id + "\",\"display_name\":\"名字" + id + "\",\"group\":\"donation\"" + fields + "}";
    }

    private static void refused(String json, String why) {
        try { ThanksWall.parse(json); check(false, "refused: " + why); }
        catch (IllegalArgumentException expected) { check(true, why); }
    }

    private static void bundled() throws Exception {
        File asset = new File("app/src/main/assets/thanks/thanks.json");
        check(asset.isFile(), "the APK carries the list");
        ThanksWall.Wall wall = ThanksWall.parse(new String(Files.readAllBytes(asset.toPath()), StandardCharsets.UTF_8));
        check(wall.revision >= 1 && !wall.people.isEmpty(), "the bundled list is valid and not empty");
        check(wall.groups.size() == 2 && "donation".equals(wall.groups.get(0).id) && "support".equals(wall.groups.get(1).id), "two groups");
        Set<String> avatars = new HashSet<>();
        for (ThanksWall.Person person : wall.people) {
            if (person.avatar == null) continue;
            File file = new File("app/src/main/assets/thanks/" + person.avatar);
            check(file.isFile() && file.length() > 0 && file.length() < 64 * 1024, person.id + ": its small avatar copy is in the APK");
            avatars.add(file.getName());
        }
        File[] shipped = new File("app/src/main/assets/thanks/avatars").listFiles();
        check(shipped != null, "the avatar folder exists");
        for (File file : shipped) check(avatars.contains(file.getName()), file.getName() + " is used by the list (nothing unused is shipped)");
        int featured = wall.featured().size(), grouped = 0;
        for (ThanksWall.Group group : wall.groups) grouped += wall.members(group.id).size();
        check(featured + grouped == wall.people.size(), "everyone shows exactly once: in the featured area or in their group");
    }

    private static void validation() {
        ThanksWall.Wall wall = ThanksWall.parse(HEAD
                + person("p1", ",\"order\":5,\"avatar\":\"avatars/p1.jpg\",\"note\":\"第一行\\n第二行\"") + ","
                + person("p2", ",\"visible\":false") + ","
                + person("p3", ",\"order\":5,\"featured\":true,\"avatar\":null") + ","
                + person("p4", ",\"future_field\":{\"a\":1}") + ","
                + "{\"id\":\"p5\",\"display_name\":\" 帮\\u0007忙 \",\"group\":\"support\",\"order\":1}]}");
        check(wall.revision == 3 && "2026-10-08".equals(wall.updatedAt), "revision and date");
        check(wall.people.size() == 4, "hidden people are left out");
        List<ThanksWall.Person> people = wall.people;
        check("p5".equals(people.get(0).id) && "p1".equals(people.get(1).id) && "p3".equals(people.get(2).id) && "p4".equals(people.get(3).id),
                "ordered by order, ties keep the file's order, a missing order is 100");
        check("帮忙".equals(people.get(0).name), "control characters are removed and the name is trimmed");
        check("第一行 第二行".equals(people.get(1).note), "a line break in a note becomes a space");
        check(people.get(2).avatar == null && people.get(2).featured, "a null avatar shows the default one");
        check(wall.featured().size() == 1 && wall.members("donation").size() == 2 && wall.members("support").size() == 1,
                "the featured person is not repeated in their group");
        // The cache keeps exactly what this version reads.
        ThanksWall.Wall again = ThanksWall.parse(ThanksWall.serialize(wall));
        check(again.revision == wall.revision && again.people.size() == wall.people.size(), "the cache round-trips");
        for (int i = 0; i < wall.people.size(); i++) {
            ThanksWall.Person a = wall.people.get(i), b = again.people.get(i);
            check(a.id.equals(b.id) && a.name.equals(b.name) && a.group.equals(b.group) && a.featured == b.featured && a.order == b.order
                    && (a.avatar == null ? b.avatar == null : a.avatar.equals(b.avatar)) && a.note.equals(b.note), "round-trip " + a.id);
        }
        // Anything wrong keeps the shown list.
        refused("{", "malformed JSON");
        refused(HEAD.replace("\"schema_version\":1", "\"schema_version\":2") + "]}", "an unknown schema");
        refused(HEAD.replace("\"schema_version\":1", "\"schema_version\":\"1\"") + "]}", "a schema given as text");
        refused(HEAD.replace("\"revision\":3", "\"revision\":0") + "]}", "revision 0");
        refused(HEAD.replace("2026-10-08", "8.10.2026") + "]}", "a malformed date");
        refused(HEAD.replace("{\"id\":\"support\",\"title\":\"其他支持\"}", "{\"id\":\"other\",\"title\":\"其他\"}") + "]}", "an unknown group");
        refused(HEAD.replace("{\"id\":\"support\",\"title\":\"其他支持\"}", "{\"id\":\"donation\",\"title\":\"又一个\"}") + "]}", "a repeated group");
        refused(HEAD + person("p1", "") + "," + person("p1", "") + "]}", "a repeated id");
        refused(HEAD + person("P1", "") + "]}", "an id with capitals");
        refused(HEAD + person("p1", ",\"avatar\":\"../secret.png\"") + "]}", "an avatar outside avatars/");
        refused(HEAD + person("p1", ",\"avatar\":\"https://example.com/a.png\"") + "]}", "an avatar on another host");
        refused(HEAD + person("p1", ",\"avatar\":\"avatars/a.gif\"") + "]}", "an avatar of another type");
        refused(HEAD + person("p1", ",\"featured\":\"yes\"") + "]}", "a flag given as text");
        refused(HEAD + person("p1", ",\"order\":-1") + "]}", "a negative order");
        StringBuilder note = new StringBuilder();
        for (int i = 0; i < 201; i++) note.append('字');
        refused(HEAD + person("p1", ",\"note\":\"" + note + "\"") + "]}", "a note over 200 characters");
        refused(HEAD + "{\"id\":\"p1\",\"display_name\":\"\",\"group\":\"donation\"}]}", "an empty name");
        refused(HEAD + "{\"id\":\"p1\",\"display_name\":\"名\",\"group\":\"elsewhere\"}]}", "a person in no group");
        refused(null, "nothing");
    }

    private static void sources() {
        check(ThanksWall.REPO_URL.equals("https://github.com/SANWU5/c17-supporters"), "the repository page");
        check(ThanksWall.BASES.length == 2, "two sources");
        for (String base : ThanksWall.BASES)
            check(base.startsWith("https://") && base.contains("SANWU5/c17-supporters") && base.endsWith("/"), "HTTPS source of the same repository: " + base);
        check(ThanksWall.CHECK_INTERVAL_MS == 24L * 60L * 60L * 1000L, "checked at most once a day by itself");
    }

    /** Exercise every possible choice, rather than relying on a random run to reach the ends of the pool. */
    private static final class Choice extends Random {
        final int choice, expectedBound;
        Choice(int choice, int expectedBound) { this.choice = choice; this.expectedBound = expectedBound; }
        @Override public int nextInt(int bound) {
            check(bound == expectedBound, "the picker uses the exact available pool");
            return choice;
        }
    }

    private static void dedications() {
        int count = ThanksMessages.count();
        check(count == 100, "exactly one hundred local dedications");
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < count; index++) {
            String message = ThanksMessages.message(index);
            check(message != null && !message.trim().isEmpty(), "each dedication contains text");
            check(unique.add(message), "each dedication is distinct");
            for (int offset = 0; offset < message.length(); offset++)
                check(!Character.isISOControl(message.charAt(offset)), "a dedication is plain display text");
        }
        for (int previous : new int[]{-1, count, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            for (int choice = 0; choice < count; choice++)
                check(ThanksMessages.choose(new Choice(choice, count), previous) == choice,
                        "a first or invalid previous choice leaves every dedication available");
        }
        for (int previous = 0; previous < count; previous++) {
            Set<Integer> available = new HashSet<>();
            for (int choice = 0; choice < count - 1; choice++) {
                int picked = ThanksMessages.choose(new Choice(choice, count - 1), previous);
                check(picked >= 0 && picked < count && picked != previous, "a new opening never repeats the immediately previous dedication");
                check(available.add(picked), "each possible random value maps to one distinct dedication");
                check(!ThanksMessages.message(picked).isEmpty(), "every choice can be displayed");
            }
            check(available.size() == count - 1, "all other dedications remain reachable");
        }
        int previous = -1;
        Random random = new Random(104L);
        for (int opening = 0; opening < 2000; opening++) {
            int picked = ThanksMessages.choose(random, previous);
            check(picked >= 0 && picked < count && picked != previous, "successive detail openings stay in bounds without adjacent repetition");
            previous = picked;
        }
    }

    private static void laneBudget() {
        check(ThanksLaneBudget.lanes(44, 252, 40, 8, 20) == 5, "five rows fit exactly, including the four gaps and wall padding");
        check(ThanksLaneBudget.lanes(44, 251, 40, 8, 20) == 4, "one pixel less reserves four fully visible rows");
        check(ThanksLaneBudget.lanes(44, 204, 40, 8, 20) == 4, "four rows fit exactly");
        check(ThanksLaneBudget.lanes(44, 203, 40, 8, 20) == 3, "one pixel less reserves three rows");
        check(ThanksLaneBudget.lanes(44, 156, 40, 8, 20) == 3, "three rows fit exactly");
        check(ThanksLaneBudget.lanes(44, 155, 40, 8, 20) == 2, "one pixel less reserves two rows");
        check(ThanksLaneBudget.lanes(44, 108, 40, 8, 20) == 2, "two rows fit exactly");
        check(ThanksLaneBudget.lanes(44, 107, 40, 8, 20) == 1, "a narrow viewport uses one row instead of clipping a second");
        check(ThanksLaneBudget.lanes(44, -100, 40, 8, 20) == 1, "a very small window keeps one row and the outer scroll fallback");
        check(ThanksLaneBudget.lanes(1, 1000, 40, 8, 20) == 1, "one person does not reserve an empty row");
        check(ThanksLaneBudget.lanes(6, 1000, 40, 8, 20) == 2, "the original small list limit remains");
        check(ThanksLaneBudget.lanes(16, 1000, 40, 8, 20) == 3, "the original medium list limit remains");
        check(ThanksLaneBudget.lanes(30, 1000, 40, 8, 20) == 4, "the original large list limit remains");
        check(ThanksLaneBudget.lanes(44, 312, 52, 8, 20) == 5, "enlarged text is budgeted at its actual row height");
        check(ThanksLaneBudget.lanes(44, 311, 52, 8, 20) == 4, "enlarged text loses a row before it clips");
        check(ThanksLaneBudget.lanes(44, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 20) == 1,
                "integer arithmetic does not overflow into an excess row count");
        for (int people = 1; people <= 50; people++) {
            for (int available = 60; available <= 380; available++) {
                int lanes = ThanksLaneBudget.lanes(people, available, 40, 8, 20);
                check(lanes >= 1 && lanes <= 5 && lanes <= people, "row count stays in the visible and populated range");
                check(20 + lanes * 40 + (lanes - 1) * 8 <= available, "every selected row fits the complete wall height");
                Set<Integer> visible = new HashSet<>();
                for (int lane = 0; lane < lanes; lane++) {
                    for (int person = 0; person < people; person++) if (person % lanes == lane) visible.add(person);
                }
                check(visible.size() == people, "redistributing into fewer looping rows keeps every person reachable");
            }
        }
    }

    /** One capsule for every name: equal height and full corner, a concentric avatar and one centre line. */
    private static void chipGeometry() throws Exception {
        check(ThanksChipGeometry.height(18f) == 40f, "the normal text size keeps the wall's 40 dp chips");
        for (float broken : new float[]{0f, -5f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
            check(ThanksChipGeometry.height(broken) == 40f, "an unusable text height never shrinks the chip");
        check(ThanksChipGeometry.height(26f) == 42f && ThanksChipGeometry.height(40f) == 56f, "enlarged text grows every chip together");
        for (float height = 36f; height <= 96f; height += .5f) {
            float avatar = ThanksChipGeometry.avatarSize(height), radius = ThanksChipGeometry.cornerRadius(height);
            check(Math.abs(radius * 2f - height) < 1e-4f, "the corner is half the height: a full capsule at " + height);
            check(Math.abs(ThanksChipGeometry.AVATAR_INSET - (height - avatar) / 2f) < 1e-4f, "the avatar has the same inset above and below at " + height);
            check(Math.abs(ThanksChipGeometry.avatarCenterX(height) - radius) < 1e-4f, "the avatar is concentric with the start cap at " + height);
            check(Math.abs(ThanksChipGeometry.centerY(height) - radius) < 1e-4f, "the avatar and the name share the centre line at " + height);
            check(avatar > 0f && avatar + 2f * ThanksChipGeometry.AVATAR_INSET == height, "nothing overlaps the capsule edge at " + height);
        }
        check(ThanksChipGeometry.avatarSize(2f) == 0f, "a degenerate height gives no negative avatar");
        check(ThanksChipGeometry.MIN_WIDTH > ThanksChipGeometry.MIN_HEIGHT && ThanksChipGeometry.MAX_TEXT_WIDTH > ThanksChipGeometry.MIN_WIDTH,
                "width limits keep every chip in a sensible range");
        // The Compose wall has to follow the shared geometry and the gold scheme has to reach the dialog host's own composition.
        File source = new File("app/src/main/java/dev/puitheme/ModernThanksWall.kt");
        check(source.isFile(), "the Compose wall is in the project");
        String wall = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        check(wall.contains("ThanksChipGeometry.height(") && wall.contains("ThanksChipGeometry.avatarSize(")
                && wall.contains("ThanksChipGeometry.AVATAR_INSET") && wall.contains("ThanksChipGeometry.END_PADDING"),
                "the chips use the shared capsule geometry");
        check(wall.split("MiuixTheme\\(colors = goldScheme\\)", -1).length - 1 == 2,
                "the gold scheme wraps the dialog and is applied again inside the host's own composition");
        check(wall.contains("backgroundColor = palette.surface") && wall.contains("titleColor = palette.ink"),
                "the dialog frame itself is gold-toned too");
        check(wall.contains("ThanksLaneBudget.lanes(") && wall.contains("ThanksMessages.choose(") && wall.contains("showPerson"),
                "lane budget, random dedications and per-person details remain");
        check(wall.contains("LinearEasing") && wall.contains("while (isActive)"), "the lanes keep looping automatically");
        // The line under the title is one short sentence; the longer two-line text pushed the layout out of alignment.
        check(wall.contains("summary = \"感谢每一位伙伴\""), "the wall subtitle is the short one-line sentence");
        check(!wall.contains("让小小的热爱"), "the long two-line subtitle is gone");
    }

    public static void main(String[] args) throws Exception {
        chipGeometry();
        bundled();
        validation();
        sources();
        dedications();
        laneBudget();
        System.out.println("ThanksWallCheck: " + checks + " checks passed");
    }
}
