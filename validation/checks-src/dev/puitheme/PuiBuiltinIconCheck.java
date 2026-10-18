// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.stream.*;

/** Optional private-fixture provenance check. Never run implicitly by public checks.
 * Arguments: converted PUI ZIP, provenance JSON, optional original user theme ZIP. */
public final class PuiBuiltinIconCheck {
    private static int checks;
    private static void require(boolean value,String why){checks++;if(!value)throw new AssertionError("PUI built-in: "+why);}
    private static final String SOURCE_SHA="0dabda20b8d1d47643aa4c4198c68c6fa2bc4a8bb68deee2d11b3f446c9317a3";
    private static final String ANDROID="http://schemas.android.com/apk/res/android";
    private static final class FilesContext extends Context {
        final File directory;FilesContext(Path directory){this.directory=directory.toFile();}
        @Override public File getFilesDir(){return directory;}
    }
    private static List<String> paths(byte[] bytes)throws Exception{
        XMLInputFactory factory=XMLInputFactory.newFactory();factory.setProperty(XMLInputFactory.SUPPORT_DTD,false);factory.setProperty("javax.xml.stream.isSupportingExternalEntities",false);
        XMLStreamReader reader=factory.createXMLStreamReader(new ByteArrayInputStream(bytes),"UTF-8");List<String> paths=new ArrayList<>();
        try{while(reader.hasNext())if(reader.next()==XMLStreamConstants.START_ELEMENT){String path=reader.getAttributeValue(ANDROID,"pathData");if(path!=null)paths.add(path);}}finally{reader.close();}return paths;
    }
    private static List<Object> styles(byte[] bytes)throws Exception{
        XMLInputFactory factory=XMLInputFactory.newFactory();factory.setProperty(XMLInputFactory.SUPPORT_DTD,false);factory.setProperty("javax.xml.stream.isSupportingExternalEntities",false);
        XMLStreamReader reader=factory.createXMLStreamReader(new ByteArrayInputStream(bytes),"UTF-8");List<Object> styles=new ArrayList<>();
        try{while(reader.hasNext())if(reader.next()==XMLStreamConstants.START_ELEMENT&&Arrays.asList("path","clip-path").contains(reader.getLocalName())){
            Map<String,Object> row=new LinkedHashMap<>(),attributes=new LinkedHashMap<>();row.put("tag",reader.getLocalName());for(int i=0;i<reader.getAttributeCount();i++)attributes.put('{'+reader.getAttributeNamespace(i)+'}'+reader.getAttributeLocalName(i),reader.getAttributeValue(i));row.put("attributes",attributes);styles.add(row);
        }}finally{reader.close();}return styles;
    }
    private static final String NATIVE_SHA="da83ae63b6cfc43a4ce57f21ee7a54444e14dada85b24ad36d2fe74181f0a927";
    /** Independent expected native resource contract: inactive action icons are default, never off. */
    private static Map<String,String> addedAliases(){
        Map<String,String> expected=new LinkedHashMap<>();
        for(String[] target:new String[][]{{"battery_saver","battery"},{"dnd","dnd"},{"no_disturb","no_disturb"},{"otg","otg"},{"envelope","red_packet"},{"super_power_saver","super_power_save"},{"vibration","vibrate"}})
            for(String[] state:new String[][]{{"off","inactive"},{"on","active"}})expected.put("tile."+target[0]+'.'+state[0],"status_bar_qs_"+target[1]+'_'+state[1]);
        expected.put("tile.auto_color_temperature.off","status_bar_screen_temperature");expected.put("tile.auto_color_temperature.on","status_bar_screen_temperature_active");
        expected.put("tile.super_power_saver.unavailable","status_bar_qs_super_power_save_unavailable");
        for(String[] action:new String[][]{{"alipay","alipay"},{"ali_scan","aliscan"},{"wechat_pay","wechat_pay"},{"wechat_scan","wechat_scan"},{"calculator","calculator"},{"clean","clean"},{"lock_screen","lock"},{"multi_system","multi_system"},{"screenshot","screenshot"}})
            expected.put("tile."+action[0]+".default","status_bar_qs_"+action[1]+"_inactive");
        expected.put("tile.shutdown.default","oplus_qs_shutdown_tile_icon");expected.put("tile.ringer_mode.silent","status_bar_qs_mute_active");expected.put("tile.ringer_mode.ring","status_bar_qs_mute_inactive");
        expected.put("tile.screen_record.idle","status_bar_qs_screenrecord_full_record");expected.put("tile.screen_record.recording","status_bar_qs_screenrecord_full_record");expected.put("tile.screen_record.unavailable","status_bar_qs_screenrecord_disable");
        for(String row:new String[]{"primary","secondary"})for(int level=0;level<=4;level++)expected.put("cellular.stacked_"+row+'.'+level,"stat_signal_lte_signal_stacked_"+row+'_'+level);
        expected.put("cellular.stacked_primary.none","stat_signal_lte_signal_stacked_primary_noservice");return expected;
    }
    private static IconManagementManifest manifest(Map<String,Object> data)throws Exception{return IconManagementManifest.parse(IconManagementJson.encode(data).getBytes(StandardCharsets.UTF_8));}
    private static Map<String,Object> copy(IconManagementManifest source)throws Exception{return IconManagementJson.parse(source.bytes(),IconManagementManifest.MAX_BYTES);}
    private static void checkAddedAliases(IconManagementRepository.Pack pack,Map<String,Map<String,Object>> aliases)throws Exception{
        require(aliases.size()==103,"complete 60 existing + 32 audited QS + 11 stacked aliases");require(pack.iconCount==284,"all 181 source candidates remain alongside 103 formal aliases");
        for(Map.Entry<String,String> expected:addedAliases().entrySet()){
            String role=expected.getKey();Map<String,Object> alias=aliases.get(role);require(alias!=null&&expected.getValue().equals(alias.get("resourceName")),"new formal role has exact native/PUI source "+role);
            require(IconRoleRegistry.role(role)!=null,"new alias is actually registered "+role);
            if(role.startsWith("tile.")){
                require(NATIVE_SHA.equals(alias.get("nativeApkSha256")),"native source APK pinned "+role);require(expected.getValue().equals(alias.get("nativeResourceName")),"exact native resource name "+role);
                require(((String)alias.get("nativeResourceId")).matches("0x7f08[0-9a-f]{4}"),"exact native drawable ID recorded "+role);
                require(((String)alias.get("nativeMethod")).endsWith(".handleUpdateState"),"native update method identified "+role);
                require(((String)alias.get("nativeCodeEvidence")).contains(".txt:"),"audited native code location recorded "+role);
                int state=role.endsWith(".unavailable")?0:role.endsWith(".off")||role.endsWith(".default")||role.endsWith(".idle")?1:2;
                require(IconManagementJson.integer(alias.get("nativeState"),0,2)==state,"formal role follows actual raw state "+role);
                require(Boolean.valueOf(!role.equals("tile.super_power_saver.on")).equals(alias.get("resourceTableDeclared")),"only audited original super-power active XML needs exact entry lookup "+role);
            }else require("preserve-original-viewport".equals(alias.get("layout")),"stacked source retains common viewport "+role);
        }
        require(IconManagementJson.integer(aliases.get("tile.ringer_mode.silent").get("nativeThreeStageMode"),0,2)==0,"silent is actual threeStageMode zero");
        require(IconManagementJson.integer(aliases.get("tile.ringer_mode.ring").get("nativeThreeStageMode"),0,2)==2,"ring is actual threeStageMode two");
        require(pack.manifest.icons.get("tile.screen_record.idle").source.equals(pack.manifest.icons.get("tile.screen_record.recording").source),"record raw1/raw2 use the same actual OEM full-record resource");
        for(String missing:new String[]{"tile.ringer_mode.vibrate","tile.auto_brightness.on","tile.auto_brightness.off","tile.vpn.on","tile.vpn.off","tile.input_method.default","cellular.stacked_secondary.none"})
            require(!pack.manifest.icons.containsKey(missing),"unproved/missing source cannot borrow another graphic "+missing);
        for(String action:new String[]{"alipay","ali_scan","wechat_pay","wechat_scan","calculator","clean","lock_screen","shutdown","multi_system","screenshot"})
            for(String state:new String[]{"on","off"})require(!pack.manifest.icons.containsKey("tile."+action+'.'+state),"action default never manufactures toggle states "+action+'.'+state);
    }
    private static void checkOldLibrary(FilesContext context,Path directory,IconManagementRepository.Pack current,Map<String,Object> report,Map<String,byte[]> entries)throws Exception{
        Map<String,Map<String,Object>> aliases=new HashMap<>();for(Object item:IconManagementJson.array(report.get("formalAliases"))){Map<String,Object> row=IconManagementJson.object(item);aliases.put((String)row.get("role"),row);}
        Map<String,Object> previous=copy(current.manifest);Map<String,Object> previousIcons=IconManagementJson.object(previous.get("icons"));for(String role:addedAliases().keySet())previousIcons.remove(role);
        Map<String,byte[]> oldEntries=new LinkedHashMap<>(entries);oldEntries.put("manifest.json",IconManagementJson.encode(previous).getBytes(StandardCharsets.UTF_8));Path archive=directory.resolve("old-installed-pui.zip");
        try(ZipOutputStream output=new ZipOutputStream(Files.newOutputStream(archive))){for(Map.Entry<String,byte[]> row:oldEntries.entrySet()){output.putNextEntry(new ZipEntry(row.getKey()));output.write(row.getValue());output.closeEntry();}}
        IconManagementRepository.Pack old;
        try(IconManagementRepository.PreparedPack source=IconManagementRepository.prepare(context,archive.toFile(),"")){require(source.pack.iconCount==241,"real old library form has original 241 aliases/candidates");source.rename("PUI兼容旧库");old=source.commit(()->true);}
        byte[] original=old.manifest.bytes();String records=IconManagementSettings.applyPack(IconManagementSettings.DEFAULT,old.id,Arrays.asList(IconRoleRegistry.SCOPES),false);
        for(String role:addedAliases().keySet()){
            Map<String,Object> row=aliases.get(role);String raw=(String)row.get("rawRole");require(raw.equals(PuiIconAliases.sourceRole(old.manifest,role)),"existing old PUI obtains pinned raw source without new library ID "+role);
            IconManagementSelection.Plan plan=IconManagementSelection.plan(IconManagementSettings.parse(records),role,Collections.singletonMap(old.id,old.manifest));require(!plan.system&&!plan.candidates.isEmpty(),"real selection uses old PUI source "+role);
            require(raw.equals(plan.candidates.get(0).role)&&old.id.equals(plan.candidates.get(0).packId),"selection keeps immutable old library ID and exact raw state "+role);
            Map<String,Object> bad=copy(old.manifest);IconManagementJson.object(IconManagementJson.object(bad.get("icons")).get(raw)).put("sha256",String.join("",Collections.nCopies(64,"0")));
            require(PuiIconAliases.sourceRole(manifest(bad),role)==null,"changed raw hash cannot reuse compatibility alias "+role);
            bad=copy(old.manifest);IconManagementJson.object(bad.get("icons")).remove(raw);require(PuiIconAliases.sourceRole(manifest(bad),role)==null,"missing raw state is never borrowed "+role);
            bad=copy(old.manifest);IconManagementJson.object(IconManagementJson.object(bad.get("icons")).get(raw)).put("render","color");require(PuiIconAliases.sourceRole(manifest(bad),role)==null,"different source render mode cannot reuse pinned mask alias "+role);
            bad=copy(old.manifest);IconManagementJson.object(IconManagementJson.object(bad.get("icons")).get(raw)).put("source","assets/renamed.xml");require(PuiIconAliases.sourceRole(manifest(bad),role)==null,"unverified source path cannot reuse pinned alias even with the same digest "+role);
        }
        Map<String,Object> foreign=copy(old.manifest);foreign.put("id","unrelated-library");Map<String,Object> renamedAuthor=copy(old.manifest);renamedAuthor.put("author","another author");
        IconManagementManifest foreignManifest=manifest(foreign),foreignAuthor=manifest(renamedAuthor);for(String role:addedAliases().keySet()){
            require(PuiIconAliases.sourceRole(foreignManifest,role)==null,"foreign library ID never borrows PUI role "+role);require(PuiIconAliases.sourceRole(foreignAuthor,role)==null,"foreign source identity never borrows PUI role "+role);
        }
        Map<String,Object> formalForeign=copy(current.manifest);formalForeign.put("id","explicit-third-party");IconManagementManifest direct=manifest(formalForeign);
        for(String role:addedAliases().keySet())require(role.equals(PuiIconAliases.sourceRole(direct,role)),"explicit formal source wins without PUI fallback "+role);
        require(PuiIconAliases.sourceRole(old.manifest,"cellular.stacked_secondary.none")==null,"old library cannot fabricate missing secondary no-service");
        require(Arrays.equals(original,old.manifest.bytes()),"compatibility lookup never rewrites immutable manifest");require(IconManagementRepository.get(context,old.id).id.equals(old.id),"original library content identity remains verifiable");
    }
    public static void main(String[] args)throws Exception{
        if(args.length<2||args.length>3)throw new IllegalArgumentException("Explicit private fixtures required: PuiBuiltinIconCheck <converted-pui.zip> <provenance.json> [original-user-theme.zip]; no PUI provenance checks were run");
        File bundle=new File(args[0]);
        Map<String,Object> report=IconManagementJson.parse(Files.readAllBytes(Paths.get(args[1])),512*1024);
        require(SOURCE_SHA.equals(report.get("sourceSha256")),"exact user-supplied source revision");
        require(IconAssetDecoder.sha256(Files.readAllBytes(bundle.toPath())).equals(report.get("bundleSha256")),"bundled archive matches conversion report");
        Path directory=Files.createTempDirectory("c17-pui-v3-check-");FilesContext context=new FilesContext(directory);
        try{
            Map<String,byte[]> entries=IconManagementArchive.read(bundle);
            IconManagementRepository.Pack pack;
            try(IconManagementRepository.PreparedPack pending=IconManagementRepository.prepare(context,bundle,"")){
                pack=pending.pack;
                require(pack.name.startsWith("PUI ColorOS 17"),"actual PUI name retained");require(pack.author.equals("天伞桜&PanL"),"source authors retained");
                require(pack.animationCount==0,"no invented PUI animation");require(pack.vectorCount==pack.iconCount,"actual source paths retained as vectors");
                List<Object> resources=IconManagementJson.array(report.get("resources"));
                require(resources.size()==IconManagementJson.integer(report.get("sourceDrawables"),1,512),"all source drawable rows present");
                Map<String,Map<String,Object>> byRawRole=new HashMap<>();
                for(Object value:resources){Map<String,Object> row=IconManagementJson.object(value);String asset=(String)row.get("asset"),role=(String)row.get("rawRole");
                    byRawRole.put(role,row);
                    require(pack.manifest.icons.containsKey(role),"original resource candidate retained "+role);
                    require(IconAssetDecoder.sha256(entries.get(asset)).equals(row.get("assetSha256")),"converted source digest "+asset);
                    require(IconAssetDecoder.sha256(IconManagementJson.encode(paths(entries.get(asset))).getBytes(StandardCharsets.UTF_8)).equals(row.get("pathDataSha256")),"original pathData exact "+asset);
                    require(IconAssetDecoder.sha256(IconManagementJson.encode(styles(entries.get(asset))).getBytes(StandardCharsets.UTF_8)).equals(row.get("pathStyleSha256")),"original path Alpha/fill/stroke style exact "+asset);
                }
                for(Object role:IconManagementJson.array(report.get("formalRoles")))require(pack.manifest.icons.containsKey(role),"audited formal alias "+role);
                Map<String,Map<String,Object>> byAlias=new HashMap<>();
                for(Object value:IconManagementJson.array(report.get("formalAliases"))){Map<String,Object> alias=IconManagementJson.object(value);String role=(String)alias.get("role");
                    require(byAlias.put(role,alias)==null,"formal provenance has no duplicate role "+role);Map<String,Object> raw=byRawRole.get(alias.get("rawRole"));require(raw!=null,"formal provenance refers to real raw source "+role);
                    IconManagementManifest.Asset actual=pack.manifest.icons.get(role);require(actual!=null&&actual.source.equals(alias.get("asset")),"formal alias points to exact recorded asset "+role);
                    require(actual.source.equals(raw.get("asset"))&&actual.sha256.equals(raw.get("assetSha256")),"formal alias preserves original asset digest "+role);
                    require(raw.get("apk").equals(alias.get("apk"))&&raw.get("entry").equals(alias.get("entry"))&&IconManagementJson.array(raw.get("resourceNames")).contains(alias.get("resourceName")),"formal alias retains real APK XML identity "+role);
                }
                require(byAlias.keySet().equals(new HashSet<>(IconManagementJson.array(report.get("formalRoles")))),"every audited alias has exactly one source provenance row");
                checkAddedAliases(pack,byAlias);
                for(String[] expected:new String[][]{{"camera","camera"},{"microphone","mic"}}){for(String state:new String[]{"on","off"}){
                    String role="tile."+expected[0]+'.'+state,name="status_bar_qs_"+expected[1]+(state.equals("on")?"_allowed":"_blocked");Map<String,Object> alias=byAlias.get(role);
                    require(alias!=null,"real allowed/blocked PUI alias present "+role);require(name.equals(alias.get("resourceName")),"permission alias never reverses allowed and blocked "+role);
                    require(IconManagementJson.integer(alias.get("nativeState"),1,2)==(state.equals("on")?2:1),"permission alias uses audited raw native state "+role);
                    require((state.equals("on")?"允许访问":"阻止访问").equals(alias.get("meaning")),"permission alias names actual access state "+role);
                    require("com.android.systemui.qs.tiles.SensorPrivacyToggleTile.handleUpdateState".equals(alias.get("nativeMethod")),"permission alias records audited native method "+role);
                    require("value=!isSensorBlocked; blocked ? state=1 : state=2".equals(alias.get("nativeRule")),"permission mapping follows actual sensor blocked branch "+role);
                    IconManagementManifest.Asset actual=pack.manifest.icons.get(role),raw=pack.manifest.icons.get("tile.pui_resource."+name);
                    require(raw!=null&&actual.source.equals(raw.source)&&actual.sha256.equals(raw.sha256),"permission alias directly reuses unchanged original graphic "+role);
                }}
                for(String role:new String[]{"wifi.none","tile.wifi.unavailable","tile.rotation.unavailable","tile.dark.unavailable","battery.0","battery.charging.100","slider.volume.normal.high","media.play_pause.play","device.phone.default"})require(!pack.manifest.icons.containsKey(role),"missing source state stays native "+role);
                String records=IconManagementSettings.applyPack(IconManagementSettings.DEFAULT,pack.id,Arrays.asList(IconRoleRegistry.SCOPES),false);
                require(IconManagementSelection.plan(IconManagementSettings.parse(records),"wifi.none",Collections.singletonMap(pack.id,pack.manifest)).candidates.isEmpty(),"missing exact state never borrows a main glyph");
                require(IconManagementRepository.list(context).isEmpty(),"prepare changes no library or settings");pack=pending.commit(()->true);
            }
            require(IconManagementRepository.get(context,pack.id).id.equals(pack.id),"installed PUI source content verified");
            checkOldLibrary(context,directory,pack,report,entries);
            if(args.length>2)try(ZipFile source=new ZipFile(args[2])){
                require(IconAssetDecoder.sha256(Files.readAllBytes(Paths.get(args[2]))).equals(SOURCE_SHA),"source ZIP exact SHA");Map<String,Map<String,byte[]>> apks=new HashMap<>();
                for(Object value:IconManagementJson.array(report.get("resources"))){Map<String,Object> row=IconManagementJson.object(value);String name=(String)row.get("apk");Map<String,byte[]> files=apks.get(name);
                    if(files==null){byte[] bytes;try(InputStream input=source.getInputStream(source.getEntry(name))){bytes=input.readAllBytes();}require(IconAssetDecoder.sha256(bytes).equals(row.get("apkSha256")),"source resource APK hash "+name);files=new HashMap<>();try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;while((entry=zip.getNextEntry())!=null){files.put(entry.getName(),zip.readAllBytes());zip.closeEntry();}}apks.put(name,files);}
                    require(IconAssetDecoder.sha256(files.get(row.get("entry"))).equals(row.get("entrySha256")),"original binary XML digest "+row.get("entry"));
                }
            }
            System.out.println("PuiBuiltinIconCheck passed: "+checks+" ("+pack.iconCount+" exact-source aliases/candidates; "+pack.id+")");
        }finally{IconManagementRepository.clear(context);try(java.util.stream.Stream<Path> files=Files.walk(directory)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}}
    }
}
