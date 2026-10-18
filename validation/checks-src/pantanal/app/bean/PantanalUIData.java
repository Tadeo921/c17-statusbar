package pantanal.app.bean;
public final class PantanalUIData {
    private final byte[] data;
    public PantanalUIData(byte[] data){this.data=data;}
    public byte[] getData(){throw new AssertionError("read verified cache fields only");}
}
