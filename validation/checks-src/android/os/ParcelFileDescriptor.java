package android.os;
public final class ParcelFileDescriptor implements java.io.Closeable {
    public static final int MODE_READ_ONLY=0x10000000;
    private final java.io.FileInputStream input;
    private ParcelFileDescriptor(java.io.File file)throws java.io.FileNotFoundException{input=new java.io.FileInputStream(file);}
    public static ParcelFileDescriptor open(java.io.File file,int mode)throws java.io.FileNotFoundException{return new ParcelFileDescriptor(file);}
    public java.io.FileDescriptor getFileDescriptor(){try{return input.getFD();}catch(java.io.IOException failure){throw new IllegalStateException(failure);}}
    @Override public void close()throws java.io.IOException{input.close();}
}
