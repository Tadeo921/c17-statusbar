// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Independent slot placements inside the status-bar editor's single atomic draft.
 * Missing entries inherit the existing SYSTEM group; explicit native entries return to
 * the original physical side. Display-retention priority is a different preference. */
public final class HintIconOrderDraft {
    public static final String INHERIT="inherit",NATIVE="native",LEFT="left",RIGHT="right";
    public static final List<String> SLOTS=Collections.unmodifiableList(Arrays.asList(NativeStatusIcons.PRIORITY_SLOTS.clone()));
    private final String[] sides,sourceSides;
    private final int[] orders,sourceOrders;
    private HintIconOrderDraft(String[] sides,int[] orders,String[] sourceSides,int[] sourceOrders){
        this.sides=sides;this.orders=orders;this.sourceSides=sourceSides;this.sourceOrders=sourceOrders;
    }
    public static HintIconOrderDraft from(String json){
        NativeStatusIconLayout.Snapshot snapshot=NativeStatusIconLayout.parse(json);
        String[] sides=new String[SLOTS.size()];int[] orders=new int[SLOTS.size()];
        for(int i=0;i<SLOTS.size();i++){String slot=SLOTS.get(i);sides[i]=snapshot.specified(slot)?snapshot.side(slot):INHERIT;orders[i]=snapshot.order(slot);}
        return new HintIconOrderDraft(sides,orders,sides,orders);
    }
    public String side(String slot){return sides[index(slot)];}
    public int order(String slot){return orders[index(slot)];}
    public List<String> slots(String side){
        validSide(side);List<String> result=new ArrayList<>();
        for(String slot:SLOTS)if(side.equals(side(slot)))result.add(slot);
        Collections.sort(result,new Comparator<String>(){
            @Override public int compare(String a,String b){int x=order(a),y=order(b);
                if(x>0&&y==0)return -1;if(x==0&&y>0)return 1;
                int value=Integer.compare(x,y);return value!=0?value:Integer.compare(index(a),index(b));}
        });return Collections.unmodifiableList(result);
    }
    /** Only the destination's hint sequence receives new ranks; other regions stay exact. */
    public HintIconOrderDraft place(String slot,String side,int position){
        int at=index(slot);validSide(side);
        String[] nextSides=sides.clone();int[] nextOrders=orders.clone();
        if(INHERIT.equals(side)||NATIVE.equals(side)){
            if(side.equals(sides[at]))return this;
            nextSides[at]=side;nextOrders[at]=0;return changed(nextSides,nextOrders);
        }
        List<String> sequence=new ArrayList<>(slots(side));int previous=sequence.indexOf(slot);sequence.remove(slot);
        int target=Math.max(0,Math.min(sequence.size(),position));
        if(side.equals(sides[at])&&previous==target)return this;
        sequence.add(target,slot);nextSides[at]=side;
        for(int i=0;i<sequence.size();i++)nextOrders[index(sequence.get(i))]=i+1;
        return changed(nextSides,nextOrders);
    }
    public HintIconOrderDraft moveWithin(String slot,int delta){
        String side=side(slot);if(INHERIT.equals(side)||NATIVE.equals(side))return this;
        List<String> sequence=slots(side);int at=sequence.indexOf(slot);
        int target=(int)Math.max(0L,Math.min(sequence.size()-1L,(long)at+delta));
        return at==target?this:place(slot,side,target);
    }
    public HintIconOrderDraft resetAll(){String[] next=new String[SLOTS.size()];Arrays.fill(next,INHERIT);return changed(next,new int[SLOTS.size()]);}
    public boolean hasChanges(){return !Arrays.equals(sides,sourceSides)||!Arrays.equals(orders,sourceOrders);}
    /** Stable JSON contains only explicit choices, so unchanged defaults never override SYSTEM. */
    public String serialize(){
        StringBuilder value=new StringBuilder("{");boolean first=true;
        for(String slot:SLOTS){if(INHERIT.equals(side(slot)))continue;if(!first)value.append(',');first=false;
            value.append('"').append(slot).append("\":{\"side\":\"").append(side(slot)).append("\",\"order\":").append(order(slot)).append('}');}
        return value.append('}').toString();
    }
    private HintIconOrderDraft changed(String[] nextSides,int[] nextOrders){
        if(Arrays.equals(sides,nextSides)&&Arrays.equals(orders,nextOrders))return this;
        return new HintIconOrderDraft(nextSides,nextOrders,sourceSides,sourceOrders);
    }
    @Override public boolean equals(Object value){if(this==value)return true;if(!(value instanceof HintIconOrderDraft))return false;
        HintIconOrderDraft other=(HintIconOrderDraft)value;return Arrays.equals(sides,other.sides)&&Arrays.equals(orders,other.orders);}
    @Override public int hashCode(){return 31*Arrays.hashCode(sides)+Arrays.hashCode(orders);}
    private static int index(String slot){int at=SLOTS.indexOf(slot);if(at<0)throw new IllegalArgumentException("Unknown hint slot");return at;}
    private static void validSide(String side){if(!INHERIT.equals(side)&&!NATIVE.equals(side)&&!LEFT.equals(side)&&!RIGHT.equals(side))throw new IllegalArgumentException("Unknown hint region");}
}
