// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Finite, physical-pixel status-bar layout. Native renderers retain ownership of their Views and animations. */
public final class StatusBarFlowPlanner {
    private static final float LIMIT = 65535f;
    private StatusBarFlowPlanner() { }

    /** A real measured outer element. Absent/GONE elements are omitted by the scene adapter. */
    public static final class Node {
        public final String token, nativeSide, sideOverride;
        public final int nativeOrdinal;
        public final float width, leftNativeMargin, rightNativeMargin;
        public Node(String token, String nativeSide, int nativeOrdinal, float width,
                float leftNativeMargin, float rightNativeMargin) {
            this(token,nativeSide,nativeOrdinal,width,leftNativeMargin,rightNativeMargin,null);
        }
        /** A split SYSTEM sub-group keeps the existing SYSTEM anchor/margins, on its resolved physical side. */
        public Node(String token,String nativeSide,int nativeOrdinal,float width,float leftNativeMargin,float rightNativeMargin,String sideOverride) {
            this.token = token; this.nativeSide = nativeSide; this.nativeOrdinal = nativeOrdinal;
            this.sideOverride=StatusBarLayoutSettings.LEFT.equals(sideOverride)||StatusBarLayoutSettings.RIGHT.equals(sideOverride)?sideOverride:null;
            this.width = width; this.leftNativeMargin = leftNativeMargin; this.rightNativeMargin = rightNativeMargin;
        }
    }

    /** Two physical safe intervals. Touching/overlapping intervals share one budget; a positive gap is never crossed. */
    public static final class Bounds {
        public final float leftStart, leftEnd, rightStart, rightEnd;
        public final boolean valid, shared;
        public Bounds(float leftStart, float leftEnd, float rightStart, float rightEnd) {
            valid = Float.isFinite(leftStart) && Float.isFinite(leftEnd) && Float.isFinite(rightStart)
                    && Float.isFinite(rightEnd) && leftStart >= 0f && leftEnd >= leftStart
                    && rightStart >= leftStart && rightEnd >= rightStart && rightEnd >= leftEnd;
            this.leftStart = coordinate(leftStart); this.leftEnd = coordinate(leftEnd);
            this.rightStart = coordinate(rightStart); this.rightEnd = coordinate(rightEnd);
            shared = valid && this.leftEnd >= this.rightStart;
        }
    }

    public static final class Placement {
        public final Node node;
        public final String side, reason;
        public final float x, width, leftMargin, rightMargin, reservedWidth;
        /** Paint movement of the innermost element by its centre-facing user margin; 0 otherwise. */
        public final float shift;
        public final boolean visible;
        private Placement(Item item, float x, boolean visible, String reason) {
            node = item.node; side = item.side; width = item.width;
            leftMargin = item.left; rightMargin = item.right; shift = visible ? item.shift : 0f;
            reservedWidth = item.reserved; this.x = x; this.visible = visible; this.reason = reason;
        }
    }

    public static final class Plan {
        public final Bounds bounds;
        /** Input order and object identity are retained, including several SYSTEM or MOBILE elements. */
        public final List<Placement> placements;
        public final float leftUsed, rightUsed;
        public final int overflowCount;
        private Plan(Bounds bounds, List<Placement> placements, float leftUsed, float rightUsed, int overflowCount) {
            this.bounds = bounds; this.placements = Collections.unmodifiableList(placements);
            this.leftUsed = leftUsed; this.rightUsed = rightUsed; this.overflowCount = overflowCount;
        }
        public Placement find(Node node) {
            for (Placement placement : placements) if (placement.node == node) return placement;
            return null;
        }
    }

    private static final class Item {
        final Node node;
        final int inputIndex, order;
        final String side;
        final float width, left, right, reserved, userLeft, userRight;
        final boolean validWidth;
        boolean accepted;
        float x, shift;
        Item(Node node, int index, StatusBarLayoutSettings.Snapshot settings, float density) {
            this.node = node; inputIndex = index;
            boolean customized = settings != null && settings.enabled;
            String chosen = customized ? settings.orderingEnabled&&node.sideOverride!=null?node.sideOverride:settings.side(node.token) : StatusBarLayoutSettings.NATIVE;
            side = StatusBarLayoutSettings.LEFT.equals(chosen) || StatusBarLayoutSettings.RIGHT.equals(chosen)
                    ? chosen : StatusBarLayoutSettings.LEFT.equals(node.nativeSide)
                    ? StatusBarLayoutSettings.LEFT : StatusBarLayoutSettings.RIGHT;
            order = customized ? settings.order(node.token) : 0;
            validWidth = Float.isFinite(node.width) && node.width >= 0f;
            width = dimension(node.width);
            left = sumMargin(node.leftNativeMargin, customized ? settings.left(node.token) : 0f, density);
            right = sumMargin(node.rightNativeMargin, customized ? settings.right(node.token) : 0f, density);
            userLeft = left - dimension(node.leftNativeMargin); userRight = right - dimension(node.rightNativeMargin);
            // Each term is bounded. Do not saturate their sum to one term's limit and accidentally make a huge gap fit.
            reserved = (float) Math.max(0d, (double) left + width + right);
        }
    }

    /** Custom orders precede order 0. Equal custom values and all native entries retain native physical order. */
    private static final Comparator<Item> ORDER = (first, second) -> {
        int custom = Boolean.compare(first.order == 0, second.order == 0);
        if (custom != 0) return custom;
        int rank = Integer.compare(first.order, second.order);
        return rank != 0 ? rank : nativeCompare(first, second);
    };
    private static final Comparator<Item> RETENTION = (first, second) -> {
        int priority = Integer.compare(priority(first.node.token), priority(second.node.token));
        return priority != 0 ? priority : nativeCompare(first, second);
    };
    private static int nativeCompare(Item first, Item second) {
        int ordinal = Integer.compare(first.node.nativeOrdinal, second.node.nativeOrdinal);
        return ordinal != 0 ? ordinal : Integer.compare(first.inputIndex, second.inputIndex);
    }
    private static int priority(String token) {
        if (StatusBarLayoutSettings.CLOCK.equals(token) || StatusBarLayoutSettings.BATTERY.equals(token)) return 0;
        if (StatusBarLayoutSettings.WIFI.equals(token) || StatusBarLayoutSettings.MOBILE.equals(token)
                || StatusBarLayoutSettings.SPEED.equals(token)) return 1;
        if (StatusBarLayoutSettings.SYSTEM.equals(token)) return 2;
        if (StatusBarLayoutSettings.NOTIFICATION.equals(token)) return 3;
        if (StatusBarLayoutSettings.CARRIER.equals(token)) return 4;
        return 5;
    }

    public static Plan plan(Bounds bounds, List<Node> nodes, StatusBarLayoutSettings.Snapshot settings, float density) {
        if (bounds == null) bounds = new Bounds(0f, 0f, 0f, 0f);
        float validDensity = Float.isFinite(density) && density > 0f ? density : 1f;
        ArrayList<Item> input = new ArrayList<>();
        if (nodes != null) for (Node node : nodes) if (node != null) input.add(new Item(node, input.size(), settings, validDensity));
        ArrayList<Item> retained = new ArrayList<>(input); Collections.sort(retained, RETENTION);
        double leftUsed = 0d, rightUsed = 0d;
        double leftCapacity = bounds.valid ? (bounds.shared ? bounds.rightEnd : bounds.leftEnd) - bounds.leftStart : 0d;
        double rightCapacity = bounds.valid ? bounds.rightEnd - (bounds.shared ? bounds.leftStart : bounds.rightStart) : 0d;
        for (Item item : retained) {
            if (!bounds.valid || !item.validWidth || item.width <= 0f) continue;
            boolean left = StatusBarLayoutSettings.LEFT.equals(item.side);
            double budget = bounds.shared ? leftUsed + rightUsed : left ? leftUsed : rightUsed;
            double capacity = left ? leftCapacity : rightCapacity;
            if (budget + item.reserved <= capacity) {
                item.accepted = true;
                if (left) leftUsed += item.reserved; else rightUsed += item.reserved;
            }
        }
        ArrayList<Item> left = new ArrayList<>(), right = new ArrayList<>();
        for (Item item : input) if (item.accepted) {
            if (StatusBarLayoutSettings.LEFT.equals(item.side)) left.add(item); else right.add(item);
        }
        Collections.sort(left, ORDER); Collections.sort(right, ORDER);
        place(left, bounds.leftStart, false, bounds.leftStart, bounds.shared ? bounds.rightEnd : bounds.leftEnd);
        // The entire right group is right anchored, but its order is still physical left-to-right.
        place(right, (double) bounds.rightEnd - rightUsed, true, bounds.shared ? bounds.leftStart : bounds.rightStart, bounds.rightEnd);
        ArrayList<Placement> output = new ArrayList<>(); int overflow = 0;
        for (Item item : input) {
            String reason = !bounds.valid ? "invalid_bounds" : !item.validWidth ? "invalid_width"
                    : item.width <= 0f ? "empty" : item.accepted ? "placed" : "overflow";
            if ("overflow".equals(reason)) overflow++;
            output.add(new Placement(item, item.accepted ? item.x : bounds.leftStart, item.accepted, reason));
        }
        return new Plan(bounds, output, (float) leftUsed, (float) rightUsed, overflow);
    }
    private static void place(List<Item> items, double origin, boolean rightAnchored, float start, float end) {
        double cursor = origin;
        for (Item item : items) { item.x = (float) (cursor + item.left); cursor += item.reserved; }
        if (items.isEmpty()) return;
        // The side facing the screen centre has no neighbour, so its gap alone is invisible
        // (device code160: the network speed's left margin "did nothing"). There the user's
        // margin moves the element itself: more space on that side pushes it towards the edge.
        // The paint never leaves the side's safe interval.
        Item inner = rightAnchored ? items.get(0) : items.get(items.size() - 1);
        float wanted = rightAnchored ? inner.userLeft : -inner.userRight;
        if (wanted == 0f || !Float.isFinite(wanted)) return;
        // Negative neighbouring gaps may already overlap paint; the shift never makes that worse.
        float low = Math.min(inner.x, start), high = Math.max(inner.x, end - inner.width);
        // Device code162: a 20dp speed margin pushed the speed onto the signal bars. Towards
        // its neighbour the element stops when it touches it; it never paints over it.
        if (items.size() > 1) {
            Item neighbour = rightAnchored ? items.get(1) : items.get(items.size() - 2);
            if (rightAnchored) high = Math.min(high, Math.max(inner.x, neighbour.x - inner.width));
            else low = Math.max(low, Math.min(inner.x, neighbour.x + neighbour.width));
        }
        float moved = Math.max(low, Math.min(high, inner.x + wanted));
        if (!Float.isFinite(moved)) return;
        inner.shift = moved - inner.x; inner.x = moved;
    }
    private static float coordinate(float value) { return Float.isNaN(value) ? 0f : Math.max(0f, Math.min(LIMIT, value)); }
    private static float dimension(float value) { return Float.isFinite(value) ? Math.max(0f, Math.min(LIMIT, value)) : 0f; }
    private static float sumMargin(float nativePixels, float addedDp, float density) {
        double added = Float.isFinite(addedDp) ? (double) addedDp * density : 0d;
        return (float) Math.max(-LIMIT, Math.min(LIMIT, (double) dimension(nativePixels) + added));
    }
}
