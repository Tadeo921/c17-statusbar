// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable editor draft; native membership never implies a resolved physical side. */
public final class StatusBarOrderDraft {
    public static final String NATIVE = "native", LEFT = "left", RIGHT = "right";
    // Keep the persisted protocol's token order without depending on Android runtime classes.
    public static final List<String> TOKENS = Collections.unmodifiableList(Arrays.asList(
            "clock", "notification", "carrier", "speed", "wifi", "mobile", "battery", "system"));
    private final String[] sides, sourceSides;
    private final int[] orders, sourceOrders;

    private StatusBarOrderDraft(String[] sides, int[] orders, String[] sourceSides, int[] sourceOrders) {
        this.sides = sides;
        this.orders = orders;
        this.sourceSides = sourceSides;
        this.sourceOrders = sourceOrders;
    }

    /** Missing or invalid persisted fields follow the existing protocol defaults. */
    public static StatusBarOrderDraft from(Map<String, ?> values) {
        String[] sides = new String[TOKENS.size()];
        int[] orders = new int[TOKENS.size()];
        for (int index = 0; index < TOKENS.size(); index++) {
            String token = TOKENS.get(index);
            Object side = values == null ? null : values.get(key(token, "side"));
            sides[index] = normalizedSide(side);
            Object rawOrder = values == null ? null : values.get(key(token, "order"));
            double order = rawOrder instanceof Number ? ((Number) rawOrder).doubleValue() : 0d;
            orders[index] = Double.isFinite(order) && order >= 0d && order <= Integer.MAX_VALUE
                    && order == Math.floor(order) ? (int) order : 0;
        }
        return new StatusBarOrderDraft(sides, orders, sides, orders);
    }

    /** Editor order only: positive priorities first; ties/defaults use stable protocol order.
     * The actual side and tie order are resolved by SystemUI, never inferred from this list. */
    public List<String> tokens() {
        List<String> result = new ArrayList<>(TOKENS);
        Collections.sort(result, new Comparator<String>() {
            @Override public int compare(String first, String second) {
                int firstOrder = order(first), secondOrder = order(second);
                if (firstOrder > 0 && secondOrder == 0) return -1;
                if (firstOrder == 0 && secondOrder > 0) return 1;
                int orderResult = Integer.compare(firstOrder, secondOrder);
                return orderResult != 0 ? orderResult : Integer.compare(index(first), index(second));
            }
        });
        return Collections.unmodifiableList(result);
    }

    /** Filtering does not create an independent ordering domain. */
    public List<String> tokens(String side) {
        String region = normalizedSide(side);
        List<String> result = new ArrayList<>();
        for (String token : tokens()) if (region.equals(side(token))) result.add(token);
        return Collections.unmodifiableList(result);
    }

    public String side(String token) { return sides[index(token)]; }
    public int order(String token) { return orders[index(token)]; }

    /** Only a deliberate change of list position establishes explicit global priorities. */
    public StatusBarOrderDraft move(String token, int delta) {
        index(token);
        List<String> sequence = new ArrayList<>(tokens());
        int start = sequence.indexOf(token);
        int end = (int) Math.max(0L, Math.min((long) sequence.size() - 1, (long) start + delta));
        if (start == end) return this;
        sequence.remove(start);
        sequence.add(end, token);
        return reorder(sequence);
    }

    /** Complete drag result. A cancelled/unchanged drag must not turn defaults/ties into ranks. */
    public StatusBarOrderDraft reorder(List<String> sequence) {
        if (sequence == null || sequence.size() != TOKENS.size())
            throw new IllegalArgumentException("A status-bar list must contain every element once");
        boolean[] seen = new boolean[TOKENS.size()];
        for (String token : sequence) {
            int at = index(token);
            if (seen[at]) throw new IllegalArgumentException("Duplicate status-bar element: " + token);
            seen[at] = true;
        }
        if (tokens().equals(sequence)) return this;
        int[] nextOrders = new int[TOKENS.size()];
        number(sequence, nextOrders);
        return changed(sides, nextOrders);
    }

    /** Region selection changes only this element's side. Priority is an independent field. */
    public StatusBarOrderDraft moveTo(String token, String side) {
        int tokenIndex = index(token);
        String destination = normalizedSide(side), origin = sides[tokenIndex];
        if (origin.equals(destination)) return this;
        String[] nextSides = sides.clone();
        nextSides[tokenIndex] = destination;
        return changed(nextSides, orders);
    }

    /** An explicit placement edits only the destination sequence. Source/native neighbours keep
     * their saved priorities, including zero and ties. Position excludes the moving element. */
    public StatusBarOrderDraft place(String token, String side, int position) {
        int tokenIndex = index(token);
        String destination = normalizedSide(side);
        if (NATIVE.equals(destination)) {
            String[] nextSides = sides.clone();
            int[] nextOrders = orders.clone();
            nextSides[tokenIndex] = NATIVE;
            nextOrders[tokenIndex] = 0;
            return changed(nextSides, nextOrders);
        }
        List<String> sequence = new ArrayList<>(tokens(destination));
        int start = sequence.indexOf(token);
        sequence.remove(token);
        int end = Math.max(0, Math.min(sequence.size(), position));
        if (destination.equals(sides[tokenIndex]) && start == end) return this;
        sequence.add(end, token);
        String[] nextSides = sides.clone();
        int[] nextOrders = orders.clone();
        nextSides[tokenIndex] = destination;
        number(sequence, nextOrders);
        return changed(nextSides, nextOrders);
    }

    /** Up/down operates within the chosen physical side, never across unrelated sides. */
    public StatusBarOrderDraft moveWithin(String token, int delta) {
        String region = side(token);
        if (NATIVE.equals(region)) return this;
        List<String> sequence = tokens(region);
        int start = sequence.indexOf(token);
        int end = (int) Math.max(0L, Math.min((long) sequence.size() - 1, (long) start + delta));
        return start == end ? this : place(token, region, end);
    }

    /** Keep region membership, removing only the selected region's explicit ordering. */
    public StatusBarOrderDraft resetRegionOrder(String side) {
        int[] nextOrders = orders.clone();
        for (String token : tokens(side)) nextOrders[index(token)] = 0;
        return changed(sides, nextOrders);
    }

    /** Keep every region while returning the whole list's priorities to SystemUI. */
    public StatusBarOrderDraft resetOrder() { return changed(sides, new int[TOKENS.size()]); }

    /** Restore the eight outer elements to native regions and native ordering. */
    public StatusBarOrderDraft resetAll() {
        String[] nextSides = new String[TOKENS.size()];
        Arrays.fill(nextSides, NATIVE);
        return changed(nextSides, new int[TOKENS.size()]);
    }

    /** Compare to the normalized source snapshot, unaffected by later caller map mutations. */
    public boolean hasChanges() {
        return !Arrays.equals(sides, sourceSides) || !Arrays.equals(orders, sourceOrders);
    }

    /** Full persisted side keys, suitable for the existing single settings transaction. */
    public Map<String, String> sideValues() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String token : TOKENS) result.put(key(token, "side"), side(token));
        return Collections.unmodifiableMap(result);
    }

    /** The existing preference protocol stores these integral order values as floats. */
    public Map<String, Float> orderValues() {
        Map<String, Float> result = new LinkedHashMap<>();
        for (String token : TOKENS) result.put(key(token, "order"), (float) order(token));
        return Collections.unmodifiableMap(result);
    }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof StatusBarOrderDraft)) return false;
        StatusBarOrderDraft other = (StatusBarOrderDraft) value;
        return Arrays.equals(sides, other.sides) && Arrays.equals(orders, other.orders);
    }

    @Override public int hashCode() { return 31 * Arrays.hashCode(sides) + Arrays.hashCode(orders); }

    private StatusBarOrderDraft changed(String[] nextSides, int[] nextOrders) {
        if (Arrays.equals(sides, nextSides) && Arrays.equals(orders, nextOrders)) return this;
        return new StatusBarOrderDraft(nextSides, nextOrders, sourceSides, sourceOrders);
    }

    private static void number(List<String> tokens, int[] orders) {
        for (int index = 0; index < tokens.size(); index++) orders[index(tokens.get(index))] = index + 1;
    }

    private static int index(String token) {
        int index = TOKENS.indexOf(token);
        if (index < 0) throw new IllegalArgumentException("Unknown status-bar element: " + token);
        return index;
    }

    private static String normalizedSide(Object side) {
        return LEFT.equals(side) ? LEFT : RIGHT.equals(side) ? RIGHT : NATIVE;
    }

    private static String key(String token, String suffix) { return "statusbar_layout_" + token + "_" + suffix; }
}
