
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ListIterator;
import java.util.Vector;

/**
 *
 * @author Vivek Trivedi ref from Kurt De Vos (http://www.gnu.org/licenses/gpl.txt 
 */

public class IEGCellCntrMarkerVector extends Vector{
    private int type;
    private Color color;
    private int totalCells;
    private String IEG_Ver;
    public boolean bIsFoci = true;
    public String m_tag;
    /** Creates a new instance of MarkerVector */
    public IEGCellCntrMarkerVector(int type) {
        super();
        this.type=type;
        color = createColor(type);
        this.totalCells = 0;
    }

    public IEGCellCntrMarkerVector(int type, String Tag) {
        super();
        this.type=type;
        color = createColor(type);
        this.totalCells = 0;
        this.m_tag = Tag;
    }
    
    public void setTotalCells(int totCells)
    {
        this.totalCells = totCells;
    }
    
    public int getTotalCells()
    {
        return this.totalCells;
    }
    
    public String getIEGVersion() {
        return this.IEG_Ver;
    }

    
    public void addMarker(IEGCellCntrMarker marker){
        add(marker);
    }
    
    public IEGCellCntrMarker getMarker(int n){
        return (IEGCellCntrMarker)get(n);
    }
    public int getVectorIndex(IEGCellCntrMarker marker){
        return indexOf(marker);
    }
    
    public void removeMarker(IEGCellCntrMarker marker)
    {
        remove(marker);
        
        this.totalCells--;
        if (this.totalCells<0)
            this.totalCells=0;
    }
    
    public void removeMarker(int n){
        remove(n);
    }
    public void removeLastMarker(){
        super.removeElementAt(size()-1);
    }
    
     private Color createColor(int typeID){
        switch(typeID){
            case(1):
                return Color.red;
            case(2):
                return Color.green;
            case(3):
                return Color.yellow;
            case(4):
                return Color.white;
            case(5):
                return Color.orange;
            case(6):
                return Color.pink;
            case(7):
                return Color.LIGHT_GRAY;
            case(8):
                return Color.yellow;
            default:
                Color c = new Color((int)(255*Math.random()),(int)(255*Math.random()),(int)(255*Math.random()));
                while(c.equals(Color.blue) | 
                        c.equals(Color.cyan) | 
                        c.equals(Color.green) | 
                        c.equals(Color.magenta) | 
                        c.equals(Color.orange) | 
                        c.equals(Color.pink) |
                        c.equals(Color.red) |
                        c.equals(Color.yellow)){
                    c = new Color((int)(255*Math.random()),(int)(255*Math.random()),(int)(255*Math.random()));
                }
                return c;
        }
    }
    
    private boolean isCloser(IEGCellCntrMarker m1,IEGCellCntrMarker m2, Point p){
        Point2D p1 = new Point2D.Double(m1.getX(), m1.getY());
        Point2D p2 = new Point2D.Double(m1.getX(), m2.getY());
        System.out.println("px = "+p.x+ " py = "+p.y);
        System.out.println(Math.abs(p1.distance(p)) + " < "+ Math.abs(p2.distance(p)));
        return (Math.abs(p1.distance(p)) < Math.abs(p2.distance(p)));
    }

    public IEGCellCntrMarker getMarkerFromPosition(Point p, int sliceIndex){
        Vector v = new Vector();
        ListIterator it = this.listIterator();
        while(it.hasNext()){
            IEGCellCntrMarker m = (IEGCellCntrMarker)it.next();
            if (m.getZ()==sliceIndex){
                v.add(m);
            }
        }
/*        CellCntrMarker currentsmallest = (CellCntrMarker)v.get(0);
        for (int i=1; i<v.size(); i++){
            CellCntrMarker m2 = (CellCntrMarker)v.get(i);
            Point p1 = new Point(currentsmallest.getX(),currentsmallest.getY());
            Point p2 = new Point(m2.getX(),m2.getY());
            boolean closer = Math.abs(p1.distance(p)) > Math.abs(p2.distance(p));
            if (closer){
                currentsmallest=m2;
            }
        }**/
        
        //new logic as par Val's req
        IEGCellCntrMarker currentsmallest = null;//(CellCntrMarker)v.get(0);
        for (int i=0; i<v.size(); i++){
            IEGCellCntrMarker m2 = (IEGCellCntrMarker)v.get(i);
//            Point p1 = new Point(currentsmallest.getX(),currentsmallest.getY());
            Point p2 = new Point(m2.getX(),m2.getY());
            double dist = Math.abs(p2.distance(p));
         //   boolean closer = ;
            if (dist < 10.0d)
            {
                currentsmallest=m2;
            }
        }
        
                
        return currentsmallest;
    }
    
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    public void setIEGVersion(String sVer) {
        this.IEG_Ver = sVer;
    }
    
    public boolean remove(Object o) 
    {
        int id = ((IEGCellCntrMarker)o).getId();
        boolean bDeleted = false;
        ListIterator it = this.listIterator();
        while(it.hasNext())
        {
            IEGCellCntrMarker m = (IEGCellCntrMarker)it.next();
            if (m.getId()==id)
            {
                bDeleted = super.remove(m);
                it = this.listIterator();
            }
            else
            {
                if (bDeleted)
                    return bDeleted;
            }
        }
        return bDeleted;
    }

}
