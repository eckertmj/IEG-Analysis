

/**
 *
 * @author Vivek Trivedi ref from Kurt De Vos (http://www.gnu.org/licenses/gpl.txt 
 */
public class IEGCellCntrMarker {
    private int x;
    private int y;
    private int z;
    private ZRange zR;
    private int mId;
    
    public class ZRange
    {
        public  int z1;
        public  int z2;
        public ZRange(int z1, int z2)
        {
            this.z1 = z1;
            this.z2 = z2;
        }
    }    
    
    /** Creates a new instance of Marker */
    public IEGCellCntrMarker() {
    }
    
    public IEGCellCntrMarker(int x, int y, int z) {
        this.x=x;
        this.y=y;
        this.z=z;
    }

    public IEGCellCntrMarker(int id, int x, int y, int z) {
        this.x=x;
        this.y=y;
        this.z=z;
        this.mId = id;
    }

    public IEGCellCntrMarker(int x, int y, int z, ZRange zR) {
        this.x=x;
        this.y=y;
        this.z=z;
        this.zR = zR;
    }

    public IEGCellCntrMarker(int id, int x, int y, int z, ZRange zR) {
        this.mId = id;
        this.x=x;
        this.y=y;
        this.z=z;
        this.zR = zR;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }
    
    public void setZRange(ZRange zR)
    {
        this.zR = zR;
    }
    
    public ZRange getZRange()
    {
        return zR;
    }
    
    public void setId(int id)
    {
        mId = id;
    }

    public int getId()
    {
        return mId;
    }
}


