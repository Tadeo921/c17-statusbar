package dev.puitheme;

import java.util.*;

/** Persisted appearance upgrades must preserve the independent source, color, alpha and size axes. */
public final class IconAppearanceUpgradeCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void rejected(RunnableChecked action,String why)throws Exception{try{action.run();throw new AssertionError(why);}catch(java.io.IOException expected){checks++;}}
    private interface RunnableChecked{void run()throws Exception;}
    private static Map<String,Object> map(Object... pairs){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
    private static IconManagementManifest manifest(Object presets)throws Exception{
        Map<String,Object> root=map("format","c17-statusbar-icons","version",3,"id","palette-test","name","Palette test","license","GPL-3.0-only","fallback","native","icons",map("hint.location.default",map("source","assets/location.png")));
        if(presets!=null)root.put("colorPresets",presets);return IconManagementManifest.parse(IconManagementJson.encode(root).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static void palettes()throws Exception{
        check(manifest(null).colorPresets.isEmpty(),"old v3 packages need no palettes");
        check(IconColorPresets.options(manifest(null)).size()==2,"old libraries offer system and original-artwork choices");
        check(!new String(manifest(null).bytes(),java.nio.charset.StandardCharsets.UTF_8).contains("colorPresets"),"old package canonical bytes gain no new empty fields");
        Map<String,Object> colors=map("scope:hint",map("mode","custom","lightColor",0xff102030,"darkColor",0xfff0e0d0),
                "role:tile.wifi.on",map("mode","custom","lightColor",0xff0080ff,"darkColor",0xff3090ff,"opacity",75),
                "role:tile.wifi.off",map("mode","system"),"scope:wifi",map("mode","artwork"));
        Map<String,Object> palettes=map("ios-blue",map("name","蓝色与系统色","appearances",colors));IconManagementManifest pack=manifest(palettes);
        check(pack.colorPresets.size()==1&&pack.colorPresets.get("ios-blue").name.equals("蓝色与系统色"),"palette names survive strict manifest parsing");
        check(IconManagementManifest.parse(pack.bytes()).colorPresets.get("ios-blue").appearances.size()==4,"palette entries round-trip");
        check(pack.withHashes(Collections.singletonMap("assets/location.png",String.join("",Collections.nCopies(64,"a")))).colorPresets.size()==1,"import normalization preserves palettes");
        String id=String.join("",Collections.nCopies(64,"b"));String records=IconManagementSettings.applyPack(IconManagementSettings.DEFAULT,id,Arrays.asList("hint","wifi"),false);
        records=IconManagementSettings.applyAppearance(records,"scope:hint",new IconManagementSettings.Appearance("system",0xff000000,0xffffffff,30f,125f));
        records=IconManagementSettings.applyAppearance(records,"scope:wifi",new IconManagementSettings.Appearance("custom",0xff222222,0xffeeeeee,50f,75f));
        records=IconManagementSettings.applyAppearance(records,"role:tile.wifi.on",new IconManagementSettings.Appearance("inherit",0xff000000,0xffffffff,null,150f));
        records=IconManagementSettings.applyTarget(records,"hint.bluetooth",Collections.singletonMap("hint.bluetooth.on",new IconManagementSettings.Override("pack",id,"hint.location.default")),new IconManagementSettings.Appearance("custom",0xff900000,0xff900000,44f,60f),Collections.emptyMap(),true);
        records=IconManagementSettings.applyAppearance(records,"role:hint.location.default",new IconManagementSettings.Appearance("custom",0xffcc0000,0xffcc0000,40f,175f));
        IconManagementSettings.State before=IconManagementSettings.parse(records);
        IconManagementSettings.State after=IconManagementSettings.parse(IconManagementSettings.applyColorPreset(records,pack.colorPresets.get("ios-blue"),Arrays.asList("hint","tile")));
        check(after.scopePacks.equals(before.scopePacks)&&after.overrides.size()==1&&after.overrides.get("hint.bluetooth.on").packId.equals(id)&&after.overrides.get("hint.bluetooth.on").sourceRole.equals("hint.location.default"),"applying a palette never changes suite or individual image sources");
        check(after.appearances.get("scope:wifi").lightColor==before.appearances.get("scope:wifi").lightColor,"unselected scope color retained");
        check(after.appearances.get("scope:wifi").sizePercent.equals(75f),"unselected scope size retained");
        check(after.appearances.get("scope:hint").sizePercent.equals(125f)&&after.appearances.get("scope:hint").opacity.equals(30f),"palette preserves size and unspecified alpha");
        check(after.appearances.get("role:tile.wifi.on").sizePercent.equals(150f),"palette preserves individual size");
        check(IconManagementSelection.appearance(after,"tile.wifi.on",false,"mask").color==0xff0080ff,"active control receives active preset color");
        check(IconManagementSelection.appearance(after,"tile.wifi.on",false,"mask").alpha==.75f,"declared palette alpha is applied once");
        check(IconManagementSelection.appearance(after,"tile.wifi.off",false,"mask").mode.equals("system"),"inactive control keeps a separate system-color choice");
        check(IconManagementSelection.appearance(after,"hint.location.default",false,"mask").color==0xff102030&&IconManagementSelection.appearance(after,"hint.location.default",true,"mask").color==0xfff0e0d0,"system hints use separate shallow/deep background colors");
        check(after.appearances.get("role:hint.location.default").sizePercent.equals(175f)&&after.appearances.get("role:hint.location.default").opacity.equals(40f),"scope palette clears shadowing role color while preserving role size and alpha");
        check(IconManagementSelection.appearance(after,"hint.bluetooth.on",false,"mask").color==0xff102030,"scope palette also clears shadowing target color");
        check(after.appearances.get("target:hint.bluetooth").sizePercent.equals(60f)&&after.appearances.get("target:hint.bluetooth").opacity.equals(44f),"cleared target keeps independent size and alpha");
        IconManagementSettings.State system=IconManagementSettings.parse(IconManagementSettings.applyColorPreset(IconManagementSettings.encode(after),IconColorPresets.options(pack).get("@system"),Arrays.asList("hint")));
        check(IconManagementSelection.appearance(system,"hint.location.default",false,"mask").mode.equals("system"),"one-tap system palette overrides old role colors");
        check(IconManagementSelection.appearance(system,"tile.wifi.on",false,"mask").color==0xff0080ff,"system palette leaves unselected scope colors unchanged");
        check(system.appearances.get("role:hint.location.default").sizePercent.equals(175f)&&system.appearances.get("role:hint.location.default").opacity.equals(40f),"system palette retains individual geometry and opacity");
        Map<String,Object> oneRole=map("one-role",map("name","One role","appearances",map("role:hint.bluetooth.on",map("mode","system"))));
        IconManagementSettings.State individual=IconManagementSettings.parse(IconManagementSettings.applyColorPreset(records,manifest(oneRole).colorPresets.get("one-role"),Arrays.asList("hint")));
        check(IconManagementSelection.appearance(individual,"hint.location.default",false,"mask").color==0xffcc0000,"single-role palette does not clear unrelated colors in the same selected scope");
        rejected(()->manifest(map("missing-name",map("appearances",colors))),"missing preset name is an IOException");
        rejected(()->manifest(map("bad id",map("name","bad","appearances",colors))),"unsafe preset identity rejected");
        rejected(()->manifest(map("bad",map("name","bad","appearances",map("role:tile.future.on",map("mode","system"))))),"unknown native role in palette rejected");
        rejected(()->manifest(map("bad",map("name","bad","appearances",map("scope:hint",map("mode","custom","lightColor",0,"darkColor",-1))))),"preset RGB cannot carry transparency");
        rejected(()->manifest(map("bad",map("name","bad","appearances",map("scope:hint",map("sizePercent",150))))),"palette cannot smuggle geometry");
        rejected(()->manifest(map("bad",map("name","bad","appearances",map("scope:hint",map("source","assets/a.png"))))),"palette cannot smuggle a source");
        Map<String,Object> many=new LinkedHashMap<>();for(int i=0;i<17;i++)many.put("palette-"+i,map("name","p"+i,"appearances",colors));rejected(()->manifest(many),"preset count budget enforced");
    }
    public static void main(String[] args)throws Exception{
        String records="{\"version\":3,\"appearances\":{\"scope:hint\":{\"sizePercent\":125}}}";
        IconManagementSettings.State state=IconManagementSettings.parse(records);
        check(state.appearances.containsKey("scope:hint"),"size-only appearance remains explicit");
        check(IconManagementSettings.encode(state).contains("sizePercent"),"size survives settings serialization");
        check(IconManagementSelection.appearance(state,"hint.location.default",true,"mask").sizePercent==125f,"size-only scope affects native and image plans");
        check(IconManagementSelection.appearance(state,"hint.location.default",true,"mask").mode.equals("system"),"size does not force a custom color");
        records=IconManagementSettings.applyAppearance(records,"target:hint.bluetooth",new IconManagementSettings.Appearance("custom",0xff123456,0xffabcdef,50f,75f));
        records=IconManagementSettings.applyAppearance(records,"role:hint.bluetooth.on",new IconManagementSettings.Appearance("inherit",0,0,null,25f));state=IconManagementSettings.parse(records);
        check(IconManagementSelection.appearance(state,"hint.bluetooth.on",false,"mask").sizePercent==25f,"state size overrides target and scope");
        check(IconManagementSelection.appearance(state,"hint.bluetooth.off",false,"mask").sizePercent==75f,"other state retains target size");
        check(IconManagementSelection.appearance(state,"hint.bluetooth.on",false,"mask").alpha==.5f,"size inherits alpha independently");
        String invalidSize="{\"version\":3,\"appearances\":{\"scope:hint\":{\"sizePercent\":125}}}";
        for(String value:new String[]{"24.9","200.1","-1","0","nullx"})rejected(()->IconManagementSettings.parse(invalidSize.replace("125",value)),"invalid size rejected");
        palettes();
        System.out.println("IconAppearanceUpgradeCheck passed: "+checks);
    }
}
