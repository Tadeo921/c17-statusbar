package dev.puitheme;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Slot edits preserve inheritance, other regions, global placements and retention priority. */
public final class HintIconOrderDraftCheck {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    public static void main(String[] args){
        HintIconOrderDraft empty=HintIconOrderDraft.from(null);
        check("{}".equals(empty.serialize())&&!empty.hasChanges(),"absent layout inherits the whole existing group");
        check(empty.slots(HintIconOrderDraft.INHERIT).equals(HintIconOrderDraft.SLOTS),"every supported hint is available without claiming native visibility");
        check(empty.resetAll()==empty,"reset of untouched layout is a no-op");
        check("{}".equals(HintIconOrderDraft.from("damaged").serialize()),"damaged preferences retain whole-group fallback");
        try{empty.slots("unexpected");throw new AssertionError("invalid region");}catch(IllegalArgumentException expected){checks++;}
        try{empty.place("wifi","left",0);throw new AssertionError("network glyph entered hints");}catch(IllegalArgumentException expected){checks++;}
        HintIconOrderDraft nativeSide=empty.place("bluetooth","native",0);
        check(nativeSide.hasChanges()&&NativeStatusIconLayout.parse(nativeSide.serialize()).specified("bluetooth"),"explicit native side is different from inheritance");
        check(nativeSide.place("bluetooth","inherit",0).equals(empty),"inherit removes the slot instead of forcing native side");
        check(nativeSide.moveWithin("bluetooth",1)==nativeSide,"native-side entries do not create an artificial mixed-side order");
        HintIconOrderDraft left=empty.place("bluetooth","left",0).place("location","left",0).place("alarm_clock","right",0);
        check(left.slots("left").equals(Arrays.asList("location","bluetooth")),"new placement has an exact same-side sequence");
        HintIconOrderDraft moved=left.moveWithin("bluetooth",-1);
        check(moved.slots("left").equals(Arrays.asList("bluetooth","location")),"arrows move within the chosen side");
        check(moved.order("alarm_clock")==left.order("alarm_clock")&&moved.side("alarm_clock").equals(left.side("alarm_clock")),"other side stays untouched");
        check(moved.moveWithin("bluetooth",-1)==moved,"boundary drag and unchanged drag do not rewrite ranks");
        check("{}".equals(empty.serialize())&&!empty.hasChanges(),"cancel can retain the original immutable draft");
        HintIconOrderDraft imported=HintIconOrderDraft.from("{\"location\":{\"side\":\"right\",\"order\":0},\"bluetooth\":{\"side\":\"native\",\"order\":7}}");
        check(imported.order("bluetooth")==7&&!imported.hasChanges(),"opening an editor preserves existing ranks and ties");
        check(imported.place("location","right",0)==imported,"unchanged placement preserves zero instead of inventing a priority");
        check(imported.place("bluetooth","native",0)==imported,"unchanged native-side drop preserves a stored rank");
        check(HintIconOrderDraft.from(imported.serialize()).equals(imported),"explicit native order survives an unchanged round trip");
        Map<String,Object> saved=new LinkedHashMap<>();saved.put(NativeStatusIcons.PRIORITY,"alarm_clock,location");
        saved.put(StatusBarLayoutSettings.key("system","side"),"right");saved.put(StatusBarLayoutSettings.key("clock","order"),9f);
        saved.put(NativeStatusIcons.LAYOUT,empty.serialize());Map<String,Object> before=new LinkedHashMap<>(saved);
        saved.put(NativeStatusIcons.LAYOUT,moved.serialize());
        for(String key:before.keySet())if(!NativeStatusIcons.LAYOUT.equals(key))check(before.get(key).equals(saved.get(key)),"one slot draft cannot overwrite global or retention setting "+key);
        check(NativeStatusIconLayout.validate(moved.serialize()).equals(moved.serialize()),"draft publishes the strict canonical wire protocol");
        StatusBarOrderDraft outer=StatusBarOrderDraft.from(before);Map<String,Object> baseline=new LinkedHashMap<>();
        baseline.putAll(outer.sideValues());baseline.putAll(outer.orderValues());
        baseline.put(StatusBarLayoutSettings.key("label","order"),0f);baseline.put(NativeStatusIcons.LAYOUT,empty.serialize());
        check(baseline.size()==18,"combined transaction adds exactly one field to the original seventeen-field editor");
        Map<String,Object> combined=new LinkedHashMap<>(baseline);combined.put(NativeStatusIcons.LAYOUT,moved.serialize());
        StatusBarOrderDraft reordered=StatusBarOrderDraft.from(combined).place("battery","left",0);
        combined.putAll(reordered.sideValues());combined.putAll(reordered.orderValues());
        check(combined.keySet().equals(baseline.keySet()),"global and per-hint operations keep one exact transaction keyset");
        check(combined.get(NativeStatusIcons.LAYOUT).equals(moved.serialize()),"outer ordering does not discard an unapplied slot draft");
        Map<String,Object> hintOnly=new LinkedHashMap<>(combined);hintOnly.put(NativeStatusIcons.LAYOUT,nativeSide.serialize());
        check(StatusBarOrderDraft.from(hintOnly).equals(reordered),"slot ordering cannot discard an unapplied outer draft");
        check(baseline.get(NativeStatusIcons.LAYOUT).equals("{}")&&StatusBarOrderDraft.from(baseline).equals(outer),"cancel leaves the entire source baseline untouched");
        Map<String,Object> externallyChanged=new LinkedHashMap<>(baseline);externallyChanged.put(NativeStatusIcons.LAYOUT,nativeSide.serialize());
        check(!baseline.equals(externallyChanged),"same combined baseline detects a concurrent slot editor change");
        externallyChanged=new LinkedHashMap<>(baseline);externallyChanged.put(StatusBarLayoutSettings.key("system","side"),"left");
        check(!baseline.equals(externallyChanged),"same combined baseline detects a concurrent global editor change");
        Random random=new Random(166);HintIconOrderDraft state=imported;
        List<String> regions=Arrays.asList("inherit","left","right","native");
        for(int n=0;n<1600;n++){
            HintIconOrderDraft prior=state;String priorJson=prior.serialize();String slot=HintIconOrderDraft.SLOTS.get(random.nextInt(HintIconOrderDraft.SLOTS.size()));
            String side=regions.get(random.nextInt(regions.size()));state=state.place(slot,side,random.nextInt(23)-2);
            for(String other:HintIconOrderDraft.SLOTS)if(!other.equals(slot)){
                check(prior.side(other).equals(state.side(other)),"moving a hint never changes another membership");
                if(!side.equals(prior.side(other))||side.equals("native")||side.equals("inherit"))check(prior.order(other)==state.order(other),"unrelated region ranks survive");
            }
            check(prior.serialize().equals(priorJson),"source snapshot stays exact");
            check(HintIconOrderDraft.from(state.serialize()).equals(state),"every drag result round trips");
            check(NativeStatusIconLayout.validate(state.serialize()).equals(state.serialize()),"every drag result is canonical");
        }
        check(state.resetAll().serialize().equals("{}"),"all hints can return to inheritance in the unapplied draft");
        System.out.println("HintIconOrderDraftCheck: "+checks+" checks passed");
    }
}
