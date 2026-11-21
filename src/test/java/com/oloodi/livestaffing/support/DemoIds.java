package com.oloodi.livestaffing.support;

import java.util.UUID;

/** Stable UUIDs from V2__seed_demo_data.sql */
public final class DemoIds {

    public static final UUID SHIFT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public static final UUID WORKER_SARAH =
            UUID.fromString("22222222-2222-2222-2222-222222222201");
    public static final UUID WORKER_DANIEL =
            UUID.fromString("22222222-2222-2222-2222-222222222202");
    public static final UUID WORKER_ALEX =
            UUID.fromString("22222222-2222-2222-2222-222222222203");

    public static final UUID ASSIGNMENT_SARAH =
            UUID.fromString("33333333-3333-3333-3333-333333333301");
    public static final UUID ASSIGNMENT_DANIEL =
            UUID.fromString("33333333-3333-3333-3333-333333333302");
    public static final UUID ASSIGNMENT_ALEX =
            UUID.fromString("33333333-3333-3333-3333-333333333303");

    private DemoIds() {
    }
}
