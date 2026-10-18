package dev.puitheme;
import java.util.Objects;

/** Parsed placement data never changes visibility, priority selection or the global ordering gate. */
public final class NativeStatusIconLayoutCheck {
    private static int checks;
    private static void equal(Object expected,Object actual,String reason){checks++;if(!Objects.equals(expected,actual))throw new AssertionError(reason+": "+expected+" != "+actual);}
    public static void main(String[] args){
        equal("{}",NativeStatusIconLayout.DEFAULT,"neutral default");
        equal(true,NativeStatusIconLayout.parse("{}").empty(),"empty inherits group");
        String source=" { \"location\":{\"order\":2,\"side\":\"right\"}, \"bluetooth\":{\"side\":\"left\",\"order\":1e0} } ";
        NativeStatusIconLayout.Snapshot parsed=NativeStatusIconLayout.parse(source);
        equal(2,parsed.entries().size(),"two independent placements");equal(true,parsed.specified("bluetooth"),"explicit side");
        equal("left",parsed.side("bluetooth"),"physical left");equal(1,parsed.order("bluetooth"),"integer JSON exponent is normalized");
        equal("right",parsed.side("location"),"physical right");equal(2,parsed.order("location"),"independent order");
        equal(false,parsed.specified("alarm_clock"),"omitted means inherit");
        String canonical="{\"bluetooth\":{\"side\":\"left\",\"order\":1},\"location\":{\"side\":\"right\",\"order\":2}}";
        equal(canonical,parsed.serialize(),"stable slot order and canonical fields");equal(canonical,NativeStatusIconLayout.validate(source),"validator canonicalizes");
        equal(canonical,NativeStatusIconLayout.validate(canonical),"idempotent canonical form");equal(null,NativeStatusIconLayout.validationError(source),"valid import has no error");
        parsed=NativeStatusIconLayout.parse("{\"bluetooth\":{\"side\":\"native\",\"order\":0}}");
        equal(true,parsed.specified("bluetooth"),"explicit native differs from omission");equal("native",parsed.side("bluetooth"),"native means native side");
        equal("{\"bluetooth\":{\"side\":\"left\",\"order\":1}}",NativeStatusIconLayout.validate("{\"\\u0062luetooth\":{\"side\":\"left\",\"order\":1.0}}"),"standard JSON string escapes");
        android.os.Bundle values=new android.os.Bundle();values.putString(NativeStatusIcons.LAYOUT,source);
        equal(false,StatusBarLayoutSettings.Snapshot.parse(values).hintLayoutActive(),"saved placements pause with sorting off");
        values.putBoolean(StatusBarLayoutSettings.MASTER,true);
        StatusBarLayoutSettings.Snapshot snapshot=StatusBarLayoutSettings.Snapshot.parse(values);
        equal(true,snapshot.hintLayoutActive(),"one sorting master enables placements without hint appearance master");
        equal("left",snapshot.hintSide("bluetooth"),"per-slot physical side");equal(1,snapshot.hintOrder("bluetooth"),"per-slot order");
        equal(false,snapshot.hintSpecified("alarm_clock"),"absent slot inherits group");
        values.putString(NativeStatusIcons.LAYOUT,"{}");equal(true,snapshot.hintLayoutActive(),"configure-time snapshot is immutable");
        equal(false,snapshot.withoutOrdering().hintLayoutActive(),"ordering mask also pauses individual slots");
        values.putString(NativeStatusIcons.LAYOUT,source);values.putBoolean(StatusBarSettings.SAFE_MODE,true);
        equal(false,StatusBarLayoutSettings.Snapshot.parse(values).hintLayoutActive(),"safe mode pauses placement");
        for(String bad:new String[]{null,"","[]","null","{}x","{ } {}","{'bluetooth':{}}","{bluetooth:{}}",
                "{\"\\u００６２luetooth\":{\"side\":\"left\",\"order\":1}}",
                "{\"bluetooth\":{\"side\":\"left\",\"order\":1,\"order\":2}}",
                "{\"bluetooth\":{\"side\":\"left\",\"order\":1},\"bluetooth\":{\"side\":\"right\",\"order\":2}}",
                "{\"wifi\":{\"side\":\"left\",\"order\":1}}","{\"unknown\":{\"side\":\"left\",\"order\":1}}",
                "{\"bluetooth\":{\"side\":\"left\"}}","{\"bluetooth\":{\"side\":\"left\",\"order\":\"1\"}}",
                "{\"bluetooth\":{\"side\":\"left\",\"order\":1.25}}","{\"bluetooth\":{\"side\":\"left\",\"order\":-1}}",
                "{\"bluetooth\":{\"side\":\"left\",\"order\":2147483648}}","{\"bluetooth\":{\"side\":\"left\",\"order\":01}}",
                "{\"bluetooth\":{\"side\":\"left\",\"order\":true}}","{\"bluetooth\":{\"side\":\"left\",\"order\":null}}",
                "{\"bluetooth\":{\"side\":\"center\",\"order\":0}}","{\"bluetooth\":{\"side\":\"left\",\"order\":0,\"hidden\":true}}",
                "{\"bluetooth\":{\"side\":\"left\",\"order\":0},}","{/*x*/}",new String(new char[8193]).replace('\0',' ')}){
            equal(true,NativeStatusIconLayout.parse(bad).empty(),"malformed runtime data safely inherits");
            equal(true,NativeStatusIconLayout.validationError(bad)!=null,"malformed import rejected");
            checks++;try{NativeStatusIconLayout.validate(bad);throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){ }
        }
        try{parsed.entries().clear();throw new AssertionError("mutable snapshot");}catch(UnsupportedOperationException expected){checks++;}
        System.out.println("NativeStatusIconLayoutCheck: "+checks+" checks passed");
    }
}
