package dev.puitheme;

/** The clear-all button pushes the shade's small bottom text down instead of covering it. */
public final class ShadeTipsPushCheck {
    private static int checks;
    private static void same(float expected, float actual, String why) {
        checks++;
        if (Math.abs(expected - actual) > 1e-4f) throw new AssertionError("Tips push " + checks + " (" + why + "): " + expected + " != " + actual);
    }

    public static void main(String[] args) {
        // a 1440-wide screen: the text sits at 3080..3144, 516 wide in the middle; the button is 384 wide in the middle
        float textTop = 3080f, textLeft = 462f, textRight = 978f, gap = 22f, margin = 24f;
        same(0f, ShadeTipsPush.push(2976f, 528f, 912f, textTop, textLeft, textRight, gap, margin), "the system's own place: they never meet");
        same(0f, ShadeTipsPush.push(3058f, 528f, 912f, textTop, textLeft, textRight, gap, margin), "the gap is just kept: nothing to push");
        same(2f, ShadeTipsPush.push(3060f, 528f, 912f, textTop, textLeft, textRight, gap, margin), "the gap is cut by two pixels: pushed by two");
        same(70f, ShadeTipsPush.push(3128f, 528f, 912f, textTop, textLeft, textRight, gap, margin), "the button on the text: the text goes below the button and the gap");
        same(110f, ShadeTipsPush.push(3168f, 528f, 912f, textTop, textLeft, textRight, gap, margin), "the button at the very bottom: the text goes off the screen");
        same(0f, ShadeTipsPush.push(3128f, 20f, 300f, textTop, textLeft, textRight, gap, margin), "the button at the left edge, apart from the text: nothing");
        same(0f, ShadeTipsPush.push(3128f, 1100f, 1400f, textTop, textLeft, textRight, gap, margin), "at the right edge: nothing");
        same(70f, ShadeTipsPush.push(3128f, 100f, 450f, textTop, textLeft, textRight, gap, margin), "within the margin of the text's edge: pushed");
        same(0f, ShadeTipsPush.push(3128f, 100f, 430f, textTop, textLeft, textRight, gap, margin), "outside the margin: not");
        same(0f, ShadeTipsPush.push(Float.NaN, 528f, 912f, textTop, textLeft, textRight, gap, margin), "a missing number is nothing");
        same(0f, ShadeTipsPush.push(3128f, 528f, 912f, Float.NaN, textLeft, textRight, gap, margin), "a missing text is nothing");
        same(26f, ShadeTipsPush.push(3106f, 528f, 912f, textTop, textLeft, textRight, -5f, margin), "a negative gap is none");
        System.out.println("Shade tips push checks passed: " + checks);
    }
}
