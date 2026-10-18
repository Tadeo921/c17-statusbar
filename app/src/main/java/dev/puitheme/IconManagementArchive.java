// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import me.zhanghai.android.libarchive.Archive;
import me.zhanghai.android.libarchive.ArchiveEntry;

/** Rejects unsafe entries before any extraction; ZIP Unix links are checked in the central directory too. */
final class IconManagementArchive {
    static final int MAX_ARCHIVE=24*1024*1024,MAX_ENTRY=1024*1024,MAX_EXPANDED=48*1024*1024,MAX_ENTRIES=1024;
    static Map<String,byte[]> read(File archive)throws IOException{
        if(!archive.isFile()||archive.length()<=0||archive.length()>MAX_ARCHIVE)throw IconManagementJson.error("图标包最多 24 MiB");
        byte[] head=new byte[8];int count;try(InputStream input=new FileInputStream(archive)){count=input.read(head);}
        if(count>=4&&head[0]=='P'&&head[1]=='K'){
            byte[] bytes;try(InputStream input=new FileInputStream(archive)){bytes=readEntry(input,MAX_ARCHIVE);}checkZipDirectory(bytes);
            Collector collected=new Collector();try(ZipInputStream input=new ZipInputStream(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8)){ZipEntry entry;while((entry=input.getNextEntry())!=null){collected.header(entry.getName(),entry.isDirectory(),entry.getSize());if(!entry.isDirectory())collected.put(entry.getName(),readEntry(input,MAX_ENTRY));input.closeEntry();}}
            return collected.result();
        }
        if(count>=7&&head[0]=='R'&&head[1]=='a'&&head[2]=='r'&&head[3]=='!'&&head[4]==0x1a&&head[5]==7)return rar(archive);
        throw IconManagementJson.error("请选择未加密的 ZIP 或单卷 RAR4/RAR5 图标包");
    }
    private static int u16(byte[] bytes,int at)throws IOException{if(at<0||at+2>bytes.length)throw IconManagementJson.error("ZIP 结构不完整");return (bytes[at]&255)|((bytes[at+1]&255)<<8);}
    private static long u32(byte[] bytes,int at)throws IOException{return u16(bytes,at)|((long)u16(bytes,at+2)<<16);}
    private static void checkZipDirectory(byte[] bytes)throws IOException{
        int end=-1;for(int at=bytes.length-22;at>=Math.max(0,bytes.length-65557);at--)if(u32(bytes,at)==0x06054b50L&&at+22+u16(bytes,at+20)==bytes.length){end=at;break;}
        if(end<0||u16(bytes,end+4)!=0||u16(bytes,end+6)!=0||u16(bytes,end+8)!=u16(bytes,end+10))throw IconManagementJson.error("不支持分卷或损坏 ZIP");
        int entries=u16(bytes,end+10);long size=u32(bytes,end+12),offset=u32(bytes,end+16);if(entries>MAX_ENTRIES||entries==65535||offset+size!=end||offset>bytes.length)throw IconManagementJson.error("ZIP 数量或目录范围无效");
        int at=(int)offset;for(int i=0;i<entries;i++){
            if(u32(bytes,at)!=0x02014b50L)throw IconManagementJson.error("ZIP 目录无效");int flags=u16(bytes,at+8),method=u16(bytes,at+10),name=u16(bytes,at+28),extra=u16(bytes,at+30),comment=u16(bytes,at+32);long attributes=u32(bytes,at+38);int type=(int)(attributes>>>16)&0170000;
            if((flags&1)!=0||method!=0&&method!=8||type!=0&&type!=0100000&&type!=0040000||u32(bytes,at+24)>MAX_ENTRY||u16(bytes,at+34)!=0)throw IconManagementJson.error("ZIP 不允许链接、特殊文件、加密或超限资源");
            long next=(long)at+46+name+extra+comment;if(next>end)throw IconManagementJson.error("ZIP 目录截断");at=(int)next;
        }if(at!=end)throw IconManagementJson.error("ZIP 目录大小不匹配");
    }
    private static Map<String,byte[]> rar(File source)throws IOException{
        long archive=0;try{
            archive=Archive.readNew();Archive.readSupportFilterNone(archive);Archive.readSupportFormatRar(archive);Archive.readSupportFormatRar5(archive);Archive.readOpenFileName(archive,source.getAbsolutePath().getBytes(StandardCharsets.UTF_8),8192);
            Collector collected=new Collector();long entry;while((entry=Archive.readNextHeader(archive))!=0){String name=ArchiveEntry.pathnameUtf8(entry);int type=ArchiveEntry.filetype(entry);boolean directory=type==ArchiveEntry.AE_IFDIR;
                if(name==null||type!=ArchiveEntry.AE_IFREG&&!directory||ArchiveEntry.hardlink(entry)!=null||ArchiveEntry.symlink(entry)!=null||ArchiveEntry.isEncrypted(entry))throw IconManagementJson.error("RAR 不允许链接、特殊文件或加密");
                collected.header(name,directory,ArchiveEntry.size(entry));if(directory){Archive.readDataSkip(archive);continue;}
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();ByteBuffer buffer=ByteBuffer.allocateDirect(8192);for(;;){buffer.clear();Archive.readData(archive,buffer);int count=buffer.position();if(count==0)break;if(bytes.size()+count>MAX_ENTRY)throw IconManagementJson.error("RAR 单项超过 1 MiB");byte[] block=new byte[count];buffer.flip();buffer.get(block);bytes.write(block);}collected.put(name,bytes.toByteArray());
            }return collected.result();
        }catch(IOException failure){throw failure;}catch(Exception|LinkageError failure){throw new IOException("图标管理：RAR 读取失败，请使用未加密单卷归档或 ZIP",failure);}finally{if(archive!=0)try{Archive.free(archive);}catch(Exception ignored){}}
    }
    static byte[] readEntry(InputStream input,int limit)throws IOException{ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(count==0){int next=input.read();if(next<0)break;if(output.size()==limit)throw IconManagementJson.error("文件超限");output.write(next);continue;}if(count>limit-output.size())throw IconManagementJson.error("文件超限");output.write(buffer,0,count);}return output.toByteArray();}
    private static boolean safe(String name,boolean directory){if(name==null||name.length()>192||name.startsWith("/")||name.indexOf('\\')>=0||name.indexOf(':')>=0||name.matches("(?s).*\\p{Cntrl}.*"))return false;String path=directory&&name.endsWith("/")?name.substring(0,name.length()-1):name;if(path.isEmpty())return false;for(String part:path.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))return false;return true;}
    static final class Collector{
        final Map<String,byte[]> entries=new LinkedHashMap<>();final Set<String> names=new HashSet<>();int count,total;
        void header(String name,boolean directory,long size)throws IOException{String normalized=directory&&name.endsWith("/")?name.substring(0,name.length()-1):name;if(++count>MAX_ENTRIES||!safe(name,directory)||!names.add(normalized.toLowerCase(Locale.ROOT))||size>MAX_ENTRY||size< -1)throw IconManagementJson.error("归档路径、重复项或数量超限");}
        void put(String name,byte[] bytes)throws IOException{if(bytes.length>MAX_EXPANDED-total)throw IconManagementJson.error("图标包展开超过 48 MiB");total+=bytes.length;entries.put(name,bytes);}
        Map<String,byte[]> result()throws IOException{
            String prefix=null;for(String name:entries.keySet())if(name.equals("manifest.json")||name.endsWith("/manifest.json")){if(prefix!=null)throw IconManagementJson.error("图标包必须恰好包含一个 manifest.json");prefix=name.substring(0,name.length()-13);}
            if(prefix==null)throw IconManagementJson.error("图标包缺少 manifest.json");if(!prefix.isEmpty()&&prefix.indexOf('/')!=prefix.length()-1)throw IconManagementJson.error("图标包最多允许一层外部目录");Map<String,byte[]> result=new LinkedHashMap<>();
            for(Map.Entry<String,byte[]> row:entries.entrySet()){
                if(!row.getKey().startsWith(prefix))throw IconManagementJson.error("归档包含图标包之外的文件");String name=row.getKey().substring(prefix.length());
                if(!name.equals("manifest.json")&&!IconManagementManifest.validAsset(name)&&!Arrays.asList("LICENSE","LICENSE.txt","LICENSE.md","README.md").contains(name))throw IconManagementJson.error("图标包含不支持的资源："+name);
                int limit=name.equals("manifest.json")?IconManagementManifest.MAX_BYTES:name.endsWith(".xml")?128*1024:name.endsWith(".json")?256*1024:name.startsWith("assets/")?MAX_ENTRY:64*1024;if(row.getValue().length>limit)throw IconManagementJson.error("图标包文件大小超限："+name);result.put(name,row.getValue());
            }return Collections.unmodifiableMap(result);
        }
    }
    private IconManagementArchive(){ }
}
