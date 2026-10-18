package android.graphics;
public class Path {
    public enum Direction {CW,CCW}
    public enum FillType {WINDING,EVEN_ODD,INVERSE_WINDING,INVERSE_EVEN_ODD}
    final java.awt.geom.Path2D.Float rasterPath=new java.awt.geom.Path2D.Float();
    public float nativeRadiusX,nativeRadiusY;
    private final RectF recordedBounds=new RectF();
    private boolean empty=true;
    public void addRoundRect(RectF rect,float x,float y,Direction direction){recordedBounds.set(rect.left,rect.top,rect.right,rect.bottom);nativeRadiusX=x;nativeRadiusY=y;empty=false;rasterPath.append(new java.awt.geom.RoundRectangle2D.Float(rect.left,rect.top,rect.width(),rect.height(),x*2,y*2),false);}
    public void reset() {empty=true;rasterPath.reset();}
    public boolean isEmpty(){return empty;}
    public void computeBounds(RectF output,boolean exact){output.set(recordedBounds.left,recordedBounds.top,recordedBounds.right,recordedBounds.bottom);}
    public void set(Path other){if(other==this)return;other.computeBounds(recordedBounds,true);nativeRadiusX=other.nativeRadiusX;nativeRadiusY=other.nativeRadiusY;empty=other.empty;rasterPath.reset();rasterPath.setWindingRule(other.rasterPath.getWindingRule());rasterPath.append(other.rasterPath,false);}
    private void updated(){java.awt.geom.Rectangle2D b=rasterPath.getBounds2D();recordedBounds.set((float)b.getMinX(),(float)b.getMinY(),(float)b.getMaxX(),(float)b.getMaxY());empty=rasterPath.getCurrentPoint()==null;}
    public void setFillType(FillType type){rasterPath.setWindingRule(type==FillType.EVEN_ODD||type==FillType.INVERSE_EVEN_ODD?java.awt.geom.Path2D.WIND_EVEN_ODD:java.awt.geom.Path2D.WIND_NON_ZERO);}
    public void moveTo(float x,float y) {rasterPath.moveTo(x,y);updated();}
    public void lineTo(float x,float y) {rasterPath.lineTo(x,y);updated();}
    public void cubicTo(float x1,float y1,float x2,float y2,float x3,float y3) {rasterPath.curveTo(x1,y1,x2,y2,x3,y3);updated();}
    public void quadTo(float x1,float y1,float x2,float y2){rasterPath.quadTo(x1,y1,x2,y2);updated();}
    private float x(){java.awt.geom.Point2D p=rasterPath.getCurrentPoint();return p==null?0:(float)p.getX();}
    private float y(){java.awt.geom.Point2D p=rasterPath.getCurrentPoint();return p==null?0:(float)p.getY();}
    public void rMoveTo(float x,float y){moveTo(x()+x,y()+y);}
    public void rLineTo(float x,float y){lineTo(x()+x,y()+y);}
    public void rCubicTo(float a,float b,float c,float d,float e,float f){float x=x(),y=y();cubicTo(x+a,y+b,x+c,y+d,x+e,y+f);}
    public void rQuadTo(float a,float b,float c,float d){float x=x(),y=y();quadTo(x+a,y+b,x+c,y+d);}
    public void transform(Matrix matrix) {float[] v=new float[9];matrix.getValues(v);rasterPath.transform(new java.awt.geom.AffineTransform(v[0],v[3],v[1],v[4],v[2],v[5]));updated();}
    public void close() {if(rasterPath.getCurrentPoint()!=null){rasterPath.closePath();updated();}}
}
