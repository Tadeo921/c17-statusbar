// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Public package/import and metadata compatibility contracts. The drawings
 * below are original rectangles. No PUI artwork/provenance result is implied. */
public final class PublicIconPackageCheck {
    private static int checks;
    private static void require(boolean value,String why){checks++;if(!value)throw new AssertionError("Public icons: "+why);}
    private static Map<String,Object> map(Object...pairs){Map<String,Object> value=new LinkedHashMap<>();for(int n=0;n<pairs.length;n+=2)value.put((String)pairs[n],pairs[n+1]);return value;}
    private static byte[] bytes(String value){return value.getBytes(StandardCharsets.UTF_8);}
    private static IconManagementManifest manifest(Map<String,Object> value)throws Exception{return IconManagementManifest.parse(bytes(IconManagementJson.encode(value)));}
    private static Map<String,Object> copy(IconManagementManifest value)throws Exception{return IconManagementJson.parse(value.bytes(),IconManagementManifest.MAX_BYTES);}
    private static Map<String,Object> envelope(String id,String author,Map<String,Object> icons){return map("format",IconManagementManifest.FORMAT,"version",3,"id",id,"name","Original public fixture","author",author,"license","GPL-3.0-only","fallback","native","icons",icons);}
    private static final class FilesContext extends Context {
        final Path path;FilesContext(Path path){this.path=path;}
        public File getFilesDir(){return path.toFile();}
    }
    private static void metadataAliases()throws Exception{
        // Independently pinned compatibility metadata only: there are no image
        // bytes corresponding to these hashes in this fixture or public APK.
        String[][] rows={
            {"cellular.stacked_primary.4","hint.pui_signal.stat_signal_lte_signal_stacked_primary_4","assets/pui-0deec0fc22fc616426736dae.xml","6fe3f9b8cf0f36e2de6e5aff9bbe5823aaeeb82fee057e423fddf9e25b081bf9"},
            {"cellular.stacked_secondary.2","hint.pui_signal.stat_signal_lte_signal_stacked_secondary_2","assets/pui-04b792177299bfe576022afc.xml","a4ef262f9d7874ed35e9b793c4d1f8080c89539585a49cd6ec68cd872751428d"},
            {"tile.camera.on","tile.pui_resource.status_bar_qs_camera_allowed","assets/pui-43e01e5763a991de549b6671.xml","bb8140edff0dff1f7c944871c217cfaa65f845c661f36b92b6f761a584d3465a"},
            {"tile.camera.off","tile.pui_resource.status_bar_qs_camera_blocked","assets/pui-25032e25133147381383f950.xml","5dc72a1d611383b0eec0d24a08ec0a3712129a28b4cb53dfb1b93187245fb80b"},
            {"footer.settings.default","tile.pui_resource.status_bar_qs_footer_settings_icon_in_bg","assets/pui-4d5f33291d8b7105e879ba6a.xml","831950f39ba032f22afbcb6a3cdc7bec0b335ce2e69969f4dbd4c143be63bf47"}
        };
        Map<String,Object> icons=map();for(String[] row:rows)icons.put(row[1],map("source",row[2],"sha256",row[3],"render","mask"));
        IconManagementManifest old=manifest(envelope("pui-coloros17","天伞桜&PanL",icons));byte[] before=old.bytes();
        String id=String.join("",Collections.nCopies(64,"a"));
        String records=IconManagementSettings.applyPack(IconManagementSettings.DEFAULT,id,Arrays.asList(IconRoleRegistry.SCOPES),false);
        for(String[] row:rows){
            require(row[1].equals(PuiIconAliases.sourceRole(old,row[0])),"pinned old-library alias survives public build: "+row[0]);
            IconManagementSelection.Plan plan=IconManagementSelection.plan(IconManagementSettings.parse(records),row[0],Collections.singletonMap(id,old));
            require(!plan.candidates.isEmpty()&&plan.candidates.get(0).role.equals(row[1])&&plan.candidates.get(0).packId.equals(id),"production selector preserves old library identity: "+row[0]);
            for(String mutation:new String[]{"id","author","source","sha256","render","missing"}){
                Map<String,Object> bad=copy(old);Map<String,Object> altered=IconManagementJson.object(IconManagementJson.object(bad.get("icons")).get(row[1]));
                if("id".equals(mutation))bad.put("id","other-library");
                else if("author".equals(mutation))bad.put("author","other author");
                else if("missing".equals(mutation))IconManagementJson.object(bad.get("icons")).remove(row[1]);
                else altered.put(mutation,"source".equals(mutation)?"assets/other.xml":"render".equals(mutation)?"color":String.join("",Collections.nCopies(64,"0")));
                require(PuiIconAliases.sourceRole(manifest(bad),row[0])==null,"unverified "+mutation+" cannot acquire alias "+row[0]);
            }
        }
        require(PuiIconAliases.sourceRole(old,"cellular.stacked_secondary.none")==null,"missing no-service state stays native");
        require(PuiIconAliases.sourceRole(null,"tile.camera.on")==null&&PuiIconAliases.sourceRole(old,null)==null,"empty metadata is not a source");
        require(Arrays.equals(before,old.bytes()),"compatibility lookup never rewrites the immutable manifest");
        Map<String,Object> direct=envelope("unrelated-library","original fixture",map("tile.camera.on",map("source","assets/direct.xml","render","color")));
        require("tile.camera.on".equals(PuiIconAliases.sourceRole(manifest(direct),"tile.camera.on")),"explicit formal role wins without PUI identity");
    }
    private static Path originalArchive(Path directory)throws Exception{
        Path archive=directory.resolve("original-public-fixture.zip");
        byte[] shape=bytes("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"12dp\" android:height=\"12dp\" android:viewportWidth=\"12\" android:viewportHeight=\"12\"><path android:fillColor=\"#ffffffff\" android:pathData=\"M1,1 H11 V11 H1 Z\"/></vector>");
        Map<String,Object> icons=map("tile.camera.on",map("source","assets/square.xml","render","mask"),"tile.camera.off",map("source","assets/square.xml","render","mask"));
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(archive))){
            out.putNextEntry(new ZipEntry("manifest.json"));out.write(bytes(IconManagementJson.encode(envelope("original-public-fixture","original fixture",icons))));out.closeEntry();
            out.putNextEntry(new ZipEntry("assets/square.xml"));out.write(shape);out.closeEntry();
        }
        return archive;
    }
    private static void importedPackage(Path directory,Path archive)throws Exception{
        FilesContext context=new FilesContext(Files.createDirectory(directory.resolve("repository")));
        try{
            IconManagementRepository.Pack pack;
            try(IconManagementRepository.PreparedPack pending=IconManagementRepository.prepare(context,archive.toFile(),"")){
                require(pending.pack.iconCount==2,"real synthetic ZIP passes production XML admission");
                require(IconManagementRepository.list(context).isEmpty(),"preparing a user library does not install or select it");
                pack=pending.commit(()->true);
            }
            require(IconManagementRepository.get(context,pack.id).id.equals(pack.id),"installed imported resources retain verified content identity");
            String selected=IconManagementSettings.applyPack(IconManagementSettings.DEFAULT,pack.id,Collections.singletonList("tile"),false);
            IconManagementSettings.State state=IconManagementSettings.parse(selected);
            for(String role:new String[]{"tile.camera.on","tile.camera.off"}){
                IconManagementSelection.Plan plan=IconManagementSelection.plan(state,role,Collections.singletonMap(pack.id,pack.manifest));
                require(!plan.candidates.isEmpty()&&role.equals(plan.candidates.get(0).role),"user-imported toggle state is actually selected: "+role);
                android.graphics.Bitmap preview=IconManagementRepository.preview(context,pack.id,role,48);require(preview.getWidth()>0&&preview.getHeight()>0,"production parser draws imported original XML: "+role);preview.recycle();
            }
            require(IconManagementSelection.plan(state,"tile.camera.unavailable",Collections.singletonMap(pack.id,pack.manifest)).candidates.isEmpty(),"missing imported state never borrows an available image");
        }finally{IconManagementRepository.clear(context);}
    }
    private static final class Sources implements IconManagementRepository.BundledSource {
        final Map<String,Path> paths=new HashMap<>();int attempts,closed,reads;
        public InputStream open(String path)throws IOException{
            attempts++;Path file=paths.get(path);if(file==null)throw new FileNotFoundException(path);
            return new FilterInputStream(Files.newInputStream(file)){
                public int read()throws IOException{reads++;return super.read();}
                public int read(byte[] data,int start,int length)throws IOException{reads++;return super.read(data,start,length);}
                public void close()throws IOException{closed++;super.close();}
            };
        }
    }
    private static void bundledPresence(Path archive)throws Exception{
        String original="icon-management-v3/c17-rounded-27.zip",pui="icon-management-v3/pui-coloros17.zip";
        Sources source=new Sources();source.paths.put(original,Paths.get("app/src/main/assets/"+original));
        Set<String> kinds=IconManagementRepository.bundledKinds(source);
        require(kinds.equals(Collections.singleton("original"))&&source.closed==1&&source.attempts==2&&source.reads==0,"actual public package has original only; probe opens/closes without reading ZIP payload");
        boolean immutable=false;try{kinds.add("pui");}catch(UnsupportedOperationException expected){immutable=true;}require(immutable,"caller cannot mutate available builtins");
        source.paths.put(pui,archive);source.paths.put("unknown.zip",archive);source.attempts=source.closed=0;
        require(IconManagementRepository.bundledKinds(source).equals(new LinkedHashSet<>(Arrays.asList("original","pui")))&&source.closed==2&&source.attempts==2&&source.reads==0,"only two named readable files can expose builtins; synthetic PUI-named fixture contains no PUI artwork");
        source.paths.clear();require(IconManagementRepository.bundledKinds(source).isEmpty(),"missing files expose no false builtin");
        require(IconManagementRepository.bundledKinds((Context)null).isEmpty()&&IconManagementRepository.bundledKinds((IconManagementRepository.BundledSource)null).isEmpty(),"null context and loader fail closed");
        require(IconManagementRepository.bundledKinds(path->{throw new IOException("unavailable");}).isEmpty(),"IO failure does not expose a package");
        require(IconManagementRepository.bundledKinds(path->{throw new IllegalStateException("unavailable");}).isEmpty(),"runtime resource failure does not expose a package");
        require(IconManagementRepository.bundledKinds(path->new ByteArrayInputStream(new byte[]{1}){public void close()throws IOException{throw new IOException("close failed");}}).isEmpty(),"a failed close cannot advertise readable resources");
        Sources glue=new Sources();glue.paths.put(original,Paths.get("app/src/main/assets/"+original));
        Context context=new Context(){public AssetManager getAssets(){return new AssetManager(){public InputStream open(String path)throws IOException{return glue.open(path);}};}};
        require(IconManagementRepository.bundledKinds(context).equals(Collections.singleton("original"))&&glue.closed==1&&glue.reads==0,"Context glue uses actual AssetManager open/close contract");
        Context publicTree=new Context(){public AssetManager getAssets(){return new AssetManager(){public InputStream open(String path)throws IOException{return Files.newInputStream(Paths.get("app/src/main/assets",path));}};}};
        require(IconManagementRepository.bundledKinds(publicTree).equals(Collections.singleton("original")),"actual public source assets expose original and contain no bundled PUI ZIP");
        boolean explicit=false;try{PuiBuiltinIconCheck.main(new String[0]);}catch(IllegalArgumentException expected){explicit=expected.getMessage().contains("Explicit private fixtures required");}
        require(explicit,"exact PUI artwork provenance cannot run or be reported passed without explicit fixtures");
    }
    public static void main(String[] args)throws Exception{
        Path directory=Files.createTempDirectory("c17-public-icons-");
        try{Path archive=originalArchive(directory);metadataAliases();importedPackage(directory,archive);bundledPresence(archive);}
        finally{try(java.util.stream.Stream<Path> files=Files.walk(directory)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}}
        System.out.println("PublicIconPackageCheck passed: "+checks+" (actual original import, builtin availability and pinned alias metadata; PUI artwork provenance NOT RUN)");
    }
}
