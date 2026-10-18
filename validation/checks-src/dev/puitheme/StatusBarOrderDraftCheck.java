package dev.puitheme;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Standalone javac check for editor operations; no Android or runtime layout dependencies. */
public final class StatusBarOrderDraftCheck {
    private static final String NATIVE = "native", LEFT = "left", RIGHT = "right";
    private static final String[] SIDES = {NATIVE, LEFT, RIGHT};
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static String key(String token, String suffix) { return "statusbar_layout_" + token + "_" + suffix; }

    private static void put(Map<String, Object> values, String token, String side, Object order) {
        values.put(key(token, "side"), side);
        values.put(key(token, "order"), order);
    }

    private static Map<String, Object> values(StatusBarOrderDraft draft) {
        Map<String, Object> values = new HashMap<>();
        values.putAll(draft.sideValues());
        values.putAll(draft.orderValues());
        return values;
    }

    private static void defaultsAndNormalization() {
        StatusBarOrderDraft defaults = StatusBarOrderDraft.from(null);
        check(!defaults.hasChanges(), "null input is a clean default snapshot");
        check(defaults.tokens(NATIVE).equals(StatusBarOrderDraft.TOKENS), "native fallback follows protocol token order");
        check(defaults.tokens(LEFT).isEmpty() && defaults.tokens(RIGHT).isEmpty(), "default physical regions are empty");
        check(defaults.resetAll() == defaults, "no-op reset keeps the snapshot");
        Object[] invalid = {-1, 1.5d, Double.NaN, Double.POSITIVE_INFINITY, 2147483648L, "1", true, null};
        for (Object order : invalid) {
            Map<String, Object> input = new HashMap<>();
            put(input, "clock", "unexpected", order);
            StatusBarOrderDraft draft = StatusBarOrderDraft.from(input);
            check(draft.order("clock") == 0 && NATIVE.equals(draft.side("clock")), "invalid fields normalize");
            check(!draft.hasChanges(), "normalization alone does not mark the editor dirty");
        }
        Map<String, Object> input = new HashMap<>();
        put(input, "clock", LEFT, Integer.MAX_VALUE);
        check(StatusBarOrderDraft.from(input).order("clock") == Integer.MAX_VALUE, "largest protocol integer parses");
        // SharedPreferences stores Float, whose largest valid integral order is below MAX_VALUE.
        put(input, "clock", LEFT, Math.nextDown((float) Integer.MAX_VALUE));
        StatusBarOrderDraft large = StatusBarOrderDraft.from(input);
        check(large.equals(StatusBarOrderDraft.from(values(large))), "largest persisted float order round trips unchanged");
        put(input, "clock", RIGHT, 16f);
        StatusBarOrderDraft snapshot = StatusBarOrderDraft.from(input);
        input.clear();
        check(snapshot.order("clock") == 16 && RIGHT.equals(snapshot.side("clock")), "caller map cannot mutate snapshot");
        check(!snapshot.hasChanges(), "source snapshot is also isolated");
    }
    private static void regionChangesNeverReorder() {
        for(String destination:SIDES){
            StatusBarOrderDraft source=StatusBarOrderDraft.from(null);
            StatusBarOrderDraft moved=source.moveTo("wifi",destination);
            for(String token:StatusBarOrderDraft.TOKENS){
                check(moved.order(token)==source.order(token),"changing one region preserves every default order");
                check(moved.side(token).equals(token.equals("wifi")?destination:source.side(token)),"only selected region changes");
            }
            Map<String,Object> input=new HashMap<>();
            for(String token:StatusBarOrderDraft.TOKENS)put(input,token,LEFT,token.equals("clock")?0f:7f);
            source=StatusBarOrderDraft.from(input);moved=source.moveTo("wifi",destination);
            for(String token:StatusBarOrderDraft.TOKENS)check(moved.order(token)==source.order(token),"zero and tied positive priorities survive region edits");
        }
    }
    private static void movesOrderTheWholeList() {
        Map<String,Object> input=new HashMap<>();
        put(input,"clock",LEFT,0f);put(input,"notification",RIGHT,7f);put(input,"carrier",NATIVE,7f);
        put(input,"speed",LEFT,2f);put(input,"wifi",RIGHT,0f);put(input,"mobile",NATIVE,9f);
        put(input,"battery",RIGHT,9f);put(input,"system",NATIVE,0f);
        StatusBarOrderDraft source=StatusBarOrderDraft.from(input), moved=source.move("clock",-1);
        List<String> expected=Arrays.asList("speed","notification","carrier","mobile","clock","battery","wifi","system");
        for(int position=0;position<expected.size();position++){
            String token=expected.get(position);
            check(moved.order(token)==position+1,"a deliberate move numbers the global list across native and explicit regions");
            check(moved.side(token).equals(source.side(token)),"reordering never chooses a region");
        }
    }

    private static void stableOrderAndMoves() {
        Map<String, Object> input = new HashMap<>();
        put(input, "clock", LEFT, 0f);
        put(input, "notification", LEFT, 7f);
        put(input, "carrier", LEFT, 7f);
        put(input, "speed", LEFT, 2f);
        put(input, "wifi", RIGHT, 0f);
        put(input, "mobile", RIGHT, 9f);
        put(input, "battery", RIGHT, 9f);
        put(input, "system", NATIVE, 0f);
        StatusBarOrderDraft source = StatusBarOrderDraft.from(input);
        check(source.tokens(LEFT).equals(Arrays.asList("speed", "notification", "carrier", "clock")),
                "positive orders precede zero with protocol ties");
        check(source.tokens().equals(Arrays.asList("speed","notification","carrier","mobile","battery","clock","wifi","system")),
                "one editor list combines all regions without claiming physical tie order");
        check(source.move("speed", -1) == source && source.move("system", 1) == source,
                "boundary operations preserve original duplicate and zero orders");
        check(source.move("clock", 0) == source && source.moveTo("clock", LEFT) == source, "same-position moves are no-ops");
        StatusBarOrderDraft moved = source.move("clock", -1);
        check(moved.tokens().equals(Arrays.asList("speed","notification","carrier","mobile","clock","battery","wifi","system")), "up moves one item in the complete editor list");
        numbered(moved);
        check(moved.sideValues().equals(source.sideValues()),"global reordering preserves all regions");
        check(source.order("clock") == 0 && source.order("carrier") == 7 && !source.hasChanges(), "move keeps source immutable");
        check(moved.hasChanges() && !moved.equals(source), "move marks the draft dirty");
        StatusBarOrderDraft across = source.moveTo("notification", RIGHT);
        check(across.tokens(RIGHT).equals(Arrays.asList("notification","mobile", "battery", "wifi")), "region edit preserves priorities rather than appending");
        check(across.tokens(LEFT).equals(Arrays.asList("speed", "carrier", "clock")), "cross-side move preserves remaining order");
        check(across.orderValues().equals(source.orderValues()),"all priorities survive region selection");
        check(across.tokens().equals(source.tokens()),"region selection does not move a row");
        unchangedRegion(source, across, NATIVE);
        StatusBarOrderDraft empty = source.moveTo("system", RIGHT);
        check(empty.tokens(NATIVE).isEmpty() && empty.order("system") == 0, "moving a default item keeps system priority");
        StatusBarOrderDraft fromEmpty = empty.moveTo("system", NATIVE);
        check(fromEmpty.equals(source),"moving an item back restores the exact snapshot");
        check(source.move("clock", Integer.MIN_VALUE).tokens(LEFT).get(0).equals("clock"), "negative delta cannot overflow");
        List<String> down = source.move("speed", Integer.MAX_VALUE).tokens();
        check(down.get(down.size() - 1).equals("speed"), "positive delta cannot overflow");
        StatusBarOrderDraft reset = across.resetRegionOrder(LEFT);
        for (String token : reset.tokens(LEFT)) check(reset.order(token) == 0, "region reset follows native order");
        unchangedRegion(across, reset, RIGHT);
        unchangedRegion(across, reset, NATIVE);
        StatusBarOrderDraft noOrder=source.resetOrder();
        check(noOrder.sideValues().equals(source.sideValues()),"reset ordering leaves every chosen region intact");
        for(String token:StatusBarOrderDraft.TOKENS)check(noOrder.order(token)==0,"reset ordering restores system priority");
        check(source.reorder(source.tokens())==source,"dropping at the original position never numbers zero or tied values");
        List<String> reversed=new java.util.ArrayList<>(source.tokens());java.util.Collections.reverse(reversed);
        StatusBarOrderDraft reordered=source.reorder(reversed);numbered(reordered);
        check(reordered.tokens().equals(reversed)&&reordered.sideValues().equals(source.sideValues()),"drag result numbers the full sequence without changing regions");
        for(List<String> invalid:Arrays.asList(java.util.Collections.<String>emptyList(),
                Arrays.asList("clock","clock","carrier","speed","wifi","mobile","battery","system"),
                Arrays.asList("clock","notification","carrier","speed","wifi","mobile","label","system"))){
            try{source.reorder(invalid);throw new AssertionError("invalid drag result accepted");}
            catch(IllegalArgumentException expected){checks++;}
        }
        StatusBarOrderDraft resetAll = reset.resetAll();
        check(resetAll.equals(StatusBarOrderDraft.from(null)), "reset all restores only outer layout defaults");
        check(resetAll.hasChanges(), "reset compares to original input rather than the previous step");
        check(!StatusBarOrderDraft.from(null).move("clock", 1).resetAll().hasChanges(), "returning to source clears dirty state");
    }

    private static void numbered(StatusBarOrderDraft draft) {
        int position = 0;
        for (String token : draft.tokens()) check(draft.order(token) == ++position, "complete list has contiguous priorities");
    }

    private static void unchangedRegion(StatusBarOrderDraft before, StatusBarOrderDraft after, String side) {
        check(before.tokens(side).equals(after.tokens(side)), "unaffected region retains displayed order");
        for (String token : before.tokens(side)) {
            check(before.side(token).equals(after.side(token)) && before.order(token) == after.order(token),
                    "unaffected region retains exact persisted values");
        }
    }

    private static void immutableExportsAndRoundTrip() {
        StatusBarOrderDraft draft = StatusBarOrderDraft.from(null).moveTo("wifi", LEFT).moveTo("battery", LEFT);
        check(draft.sideValues().size() == 8 && draft.orderValues().size() == 8, "all eight full keys are exported");
        check(draft.sideValues().get(key("wifi", "side")).equals(LEFT), "side export uses persisted key");
        check(draft.orderValues().get(key("battery", "order")).equals(0f), "side-only export keeps default priority as a float");
        StatusBarOrderDraft reread = StatusBarOrderDraft.from(values(draft));
        check(reread.equals(draft) && reread.hashCode() == draft.hashCode() && !reread.hasChanges(), "export round trip creates a clean equivalent source");
        try { draft.tokens(LEFT).clear(); throw new AssertionError("mutable token list"); }
        catch (UnsupportedOperationException expected) { checks++; }
        try { draft.tokens().clear(); throw new AssertionError("mutable complete token list"); }
        catch (UnsupportedOperationException expected) { checks++; }
        try { draft.sideValues().clear(); throw new AssertionError("mutable side map"); }
        catch (UnsupportedOperationException expected) { checks++; }
        try { draft.orderValues().clear(); throw new AssertionError("mutable order map"); }
        catch (UnsupportedOperationException expected) { checks++; }
        try { StatusBarOrderDraft.TOKENS.clear(); throw new AssertionError("mutable protocol list"); }
        catch (UnsupportedOperationException expected) { checks++; }
        try { draft.move("label", 1); throw new AssertionError("inner label accepted as outer token"); }
        catch (IllegalArgumentException expected) { checks++; }
    }

    private static void randomizedOperations() {
        Random random = new Random(152);
        StatusBarOrderDraft draft = StatusBarOrderDraft.from(null);
        for (int step = 0; step < 2000; step++) {
            String token = StatusBarOrderDraft.TOKENS.get(random.nextInt(8));
            String destination = SIDES[random.nextInt(3)];
            StatusBarOrderDraft before = draft;
            Map<String, Object> originalValues = values(before);
            int operation = random.nextInt(5);
            if (operation == 0) draft = draft.move(token, random.nextBoolean() ? 1 : -1);
            else if (operation == 1) draft = draft.moveTo(token, destination);
            else if (operation == 2) draft = draft.resetOrder();
            else if (operation == 4) {
                List<String> sequence=new java.util.ArrayList<>(draft.tokens());java.util.Collections.shuffle(sequence,random);
                draft=draft.reorder(sequence);
            }
            else draft = draft.resetAll();
            check(values(before).equals(originalValues), "every operation keeps previous draft immutable");
            HashSet<String> membership = new HashSet<>();
            for (String side : SIDES) {
                for (String member : draft.tokens(side)) {
                    check(side.equals(draft.side(member)), "membership agrees with side lookup");
                    check(membership.add(member), "no element is duplicated");
                }
            }
            if(operation==1){
                check(draft.orderValues().equals(before.orderValues()),"random region edits preserve all zero/tied/prioritized values");
                for(String member:StatusBarOrderDraft.TOKENS)if(!member.equals(token))
                    check(draft.side(member).equals(before.side(member)),"random region edit changes only one element");
            }else if(operation!=3)check(draft.sideValues().equals(before.sideValues()),"random order edits do not change regions");
            if((operation==0||operation==4)&&draft!=before)numbered(draft);
            check(membership.size() == 8, "no element is lost");
            check(draft.equals(StatusBarOrderDraft.from(values(draft))), "every valid edited state round trips");
        }
    }

    private static void directPlacement() {
        Map<String,Object> input = new HashMap<>();
        put(input, "clock", LEFT, 0f); put(input, "notification", LEFT, 7f);
        put(input, "carrier", LEFT, 7f); put(input, "wifi", RIGHT, 0f);
        put(input, "mobile", RIGHT, 9f); put(input, "battery", RIGHT, 9f);
        put(input, "speed", NATIVE, 5f); put(input, "system", NATIVE, 0f);
        StatusBarOrderDraft source = StatusBarOrderDraft.from(input);
        check(source.place("notification", LEFT, 0) == source, "same slot preserves ties and zero");
        check(source.moveWithin("speed", 1) == source, "native area cannot be falsely reordered");
        StatusBarOrderDraft placed = source.place("clock", RIGHT, 1);
        check(placed.tokens(RIGHT).equals(Arrays.asList("mobile", "clock", "battery", "wifi")), "placement inserts at the exact side slot");
        for (String token : StatusBarOrderDraft.TOKENS) {
            if (!RIGHT.equals(placed.side(token))) check(placed.order(token) == source.order(token), "source and native neighbours retain zero and ties");
            if (!token.equals("clock")) check(placed.side(token).equals(source.side(token)), "placing only changes chosen membership");
        }
        for (int n = 0; n < placed.tokens(RIGHT).size(); n++) check(placed.order(placed.tokens(RIGHT).get(n)) == n + 1, "destination sequence is explicit");
        StatusBarOrderDraft restored = placed.place("clock", NATIVE, 0);
        check(restored.order("clock") == 0 && NATIVE.equals(restored.side("clock")), "give back restores both native fields");
        for (String token : StatusBarOrderDraft.TOKENS) if (!token.equals("clock"))
            check(restored.order(token) == placed.order(token), "give back leaves every neighbour unchanged");
        StatusBarOrderDraft empty = StatusBarOrderDraft.from(null).place("wifi", LEFT, 99);
        check(empty.tokens(LEFT).equals(Arrays.asList("wifi")) && empty.order("wifi") == 1, "empty side is a real destination");
        Random random = new Random(158);
        StatusBarOrderDraft state = source;
        for (int n = 0; n < 1500; n++) {
            StatusBarOrderDraft before = state;
            Map<String,Object> beforeValues = values(before);
            String token = StatusBarOrderDraft.TOKENS.get(random.nextInt(8));
            String destination = SIDES[random.nextInt(SIDES.length)];
            state = before.place(token, destination, random.nextInt(12) - 2);
            for (String other : StatusBarOrderDraft.TOKENS) if (!other.equals(token)) {
                check(state.side(other).equals(before.side(other)), "placement never changes neighbour membership");
                if (!destination.equals(before.side(other)) || NATIVE.equals(destination))
                    check(state.order(other) == before.order(other), "placement never edits unrelated order");
            }
            check(state.equals(StatusBarOrderDraft.from(values(state))), "placed state round trips");
            check(values(before).equals(beforeValues), "placement keeps previous snapshot immutable");
        }
    }

    public static void main(String[] arguments) {
        if(arguments.length>0){if("global".equals(arguments[0]))movesOrderTheWholeList();else regionChangesNeverReorder();System.out.println(checks+" targeted checks passed");return;}
        defaultsAndNormalization();
        regionChangesNeverReorder();
        movesOrderTheWholeList();
        stableOrderAndMoves();
        immutableExportsAndRoundTrip();
        randomizedOperations();
        directPlacement();
        System.out.println("StatusBarOrderDraftCheck: " + checks + " checks passed");
    }
}
