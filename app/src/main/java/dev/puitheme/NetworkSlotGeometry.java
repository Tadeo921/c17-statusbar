// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

/** Post-1.8.2 saved configuration keys only; the public network renderer has no shared geometry. */
final class NetworkSlotGeometry {
    static final String POSITION = "network_position_enabled", OFFSET_X = "network_offset_x", OFFSET_Y = "network_offset_y";
    static final String MARGIN_LEFT = "network_margin_left", MARGIN_RIGHT = "network_margin_right";
    private NetworkSlotGeometry() { }
}
