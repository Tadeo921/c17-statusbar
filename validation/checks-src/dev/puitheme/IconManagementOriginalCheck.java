// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import java.io.File;
import java.nio.file.*;
import java.util.*;

/** Real bundled archives use the same manifest, XML, PNG and animation admission as an imported package. */
public final class IconManagementOriginalCheck {
    private static int checks;
    private static void require(boolean value,String why){checks++;if(!value)throw new AssertionError("Original v3 package: "+why);}
    private static final class FilesContext extends Context {
        final File directory;
        FilesContext(Path directory){this.directory=directory.toFile();}
        @Override public File getFilesDir(){return directory;}
    }
    public static void main(String[] args)throws Exception {
        String[] archives=args.length==0?new String[]{"app/src/main/assets/icon-management-v3/c17-rounded-27.zip"}:args;
        for(String path:archives){
            Path directory=Files.createTempDirectory("c17-original-v3-");FilesContext context=new FilesContext(directory);
            try{
                IconManagementRepository.Pack pack;
                try(IconManagementRepository.PreparedPack pending=IconManagementRepository.prepare(context,new File(path),"")){
                    pack=pending.pack;
                    require(pack.iconCount>100,"all static states declared");
                    for(String scope:IconRoleRegistry.SCOPES)for(String role:IconRoleRegistry.roles(scope))require(pack.manifest.icons.containsKey(role),"missing declared role "+role);
                    require(pack.animationCount==3,"three bounded transitions declared");
                    for(Map.Entry<String,IconManagementManifest.Transition> row:pack.manifest.transitions.entrySet()){
                        IconManagementManifest.Transition motion=row.getValue();
                        require(!motion.sha256.isEmpty(),"validated motion digest "+row.getKey());
                        String target=row.getKey().substring(0,row.getKey().lastIndexOf('.'));
                        require(pack.manifest.icons.containsKey(target+'.'+motion.from)&&pack.manifest.icons.containsKey(target+'.'+motion.to),"static endpoints present "+row.getKey());
                    }
                    require(IconManagementRepository.list(context).isEmpty(),"prepare never applies or publishes a package");
                    pack=pending.commit(()->true);
                }
                require(IconManagementRepository.get(context,pack.id).id.equals(pack.id),"installed library hashes reverified");
                require(IconManagementRepository.list(context).size()==1,"one library published");
                System.out.println("Original v3 admitted: "+path+"; "+pack.iconCount+" static / "+pack.animationCount+" dynamic; "+pack.id);
            }finally{
                IconManagementRepository.clear(context);
                try(java.util.stream.Stream<Path> files=Files.walk(directory)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}
            }
        }
        System.out.println("IconManagementOriginalCheck passed: "+checks+" (real XML/PNG admission; Android Skia rendering is validated separately)");
    }
}
