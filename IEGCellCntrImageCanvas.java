

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.process.ImageProcessor;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ListIterator;
import java.util.Vector;
import javax.swing.JOptionPane;
import ij.plugin.frame.RoiManager;
import java.awt.Color;
import java.util.ArrayList;

/**
 *
 * @author Vivek Trivedi ref from Kurt De Vos (http://www.gnu.org/licenses/gpl.txt 
 */
public class IEGCellCntrImageCanvas extends ImageCanvas{
    private Vector typeVector;
    private Vector typeNVector;
    private IEGCellCntrMarkerVector currentMarkerVector;
    private IEGCellCounter cc;
    private ImagePlus img;
    private boolean delmode = false;
    private boolean showNumbers = true;
    private boolean showAll = false;
    private boolean IEG_Analysis = false;
    private boolean Hide_Markers = false;
    private boolean ROI_Mode = false;
    private boolean bShowAllMarkers = true;
    private boolean bShowMapMarkers = false;
    private boolean bNEnabled = false;
    
      
    
    private Font font = new Font("SansSerif", Font.PLAIN, 10);
    
    /** Creates a new instance of CellCntrImageCanvas */
    public IEGCellCntrImageCanvas(ImagePlus img, Vector typeVector, Vector typeNVector, IEGCellCounter cc, Vector displayList) 
    {
        super(img);
        this.img=img;
        this.typeVector = typeVector;
        this.typeNVector = typeNVector;
        this.cc = cc;
        if (displayList!=null)
            this.setDisplayList(displayList);
    }
    
    public void mouseReleased(MouseEvent e)
    {
        Roi m_roi = null;
        if ( e != null)
            super.mouseReleased(e);
        if (ROI_Mode && !bShowMapMarkers)
        {
                RoiManager rm=RoiManager.getInstance();
                
                if (rm!=null && e != null)
                    return;
                int mROIs = 1;
                Roi[] rois = null;
                if (rm!=null)
                {
                    rois = rm.getRoisAsArray();
                    mROIs = rm.getCount();
                    if (mROIs == 0)
                    {
                        mROIs = 1;
                        rm=null;
                    }
                    
                   // m_vImp = new ArrayList<ImagePlus>();
                }

                ArrayList<Integer> vId = new ArrayList<Integer>();
                Roi roi;
                for ( int i = 0; i < mROIs; i++ )
                {
                    ImagePlus imp2;
                    if (rm==null)
                        m_roi = imp.getRoi();
                    else
                        m_roi = rois[i];
                    
                    if (m_roi==null)
                    {
                        cc.populateTxtFields(currentMarkerVector.getTotalCells());
                        return;
                    }
                    
                    ListIterator it = currentMarkerVector.listIterator();
                    while(it.hasNext()){
                        IEGCellCntrMarker m1 = (IEGCellCntrMarker)it.next();
                        if (m_roi.contains(m1.getX(), m1.getY()))
                        {
                            if (!vId.contains(m1.getId()))
                                vId.add(m1.getId());
                        }
                    }
                }
                if (vId.size()>0)
                {
                    cc.populateTxtFields(vId.size());
                    cc.ShowROIResultTable(vId, currentMarkerVector.getType());
                    
                }
                
                if (m_roi==null)
                {
                    cc.populateTxtFields(currentMarkerVector.getTotalCells());
                }
        }
    }
    public void mp(MouseEvent e)
    {
        int x = super.offScreenX(e.getX());
        int y = super.offScreenY(e.getY());

        if (!delmode)
        {
            if (IEG_Analysis)
            {
                String zRange = JOptionPane.showInputDialog(null,"Enter Blos's Z Range (eg. 3-7) :", "Blob's Z Range", JOptionPane.QUESTION_MESSAGE );
                if (zRange == null)
                    return;
                String[] zRanges = zRange.split("-");
                if (zRanges.length>1)
                {
                    int zSt = Integer.parseInt(zRanges[0]);
                    int zEnd = Integer.parseInt(zRanges[1]);
                    
                    int stackSize = img.getStackSize();
                    if ((zSt > zEnd) || (zSt<0 ||zEnd<0) || (zSt > stackSize || zEnd> stackSize))
                    {
                        JOptionPane.showMessageDialog(null,"Invalid Z-Range !!", "Error - Clicked Blob is Discarded", JOptionPane.ERROR_MESSAGE);
                        return;
                            
                    }
                    else
                    {
                        IEGCellCntrMarker m1;
                        int newId;
                        
                        if (currentMarkerVector.size()> 0 )
                        {
                            m1 = (IEGCellCntrMarker)currentMarkerVector.get(currentMarkerVector.size()-1);
                            newId = m1.getId()+1;
                        }
                        else
                            newId = 1;
                        for ( int i = zSt; i <= zEnd; i++  )
                        {
                            IEGCellCntrMarker m = new IEGCellCntrMarker(newId, x, y, i);
                            m.setZRange(m.new ZRange(zSt, zEnd));
                            currentMarkerVector.addMarker(m);
                        }
                        currentMarkerVector.setTotalCells(currentMarkerVector.getTotalCells()+1);
                        if (currentMarkerVector.bIsFoci)
                            cc.AddToResultTable(currentMarkerVector.getType(), newId, x, y, img.getCurrentSlice());
                        else
                            cc.AddToNuResultTable(currentMarkerVector.m_tag, newId, x, y, img.getCurrentSlice());
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(null,"Invalid Z-Range !!", "Error - Clicked Blob is Discarded", JOptionPane.ERROR_MESSAGE);
                }
                
            }
            else
            {
                IEGCellCntrMarker m = new IEGCellCntrMarker(x, y, img.getCurrentSlice());
                currentMarkerVector.addMarker(m);
            }
        }
        else
        {
            IEGCellCntrMarker m = currentMarkerVector.getMarkerFromPosition(new Point(x,y) ,img.getCurrentSlice());
            if (IEG_Analysis)
            {
                
                int retValue = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this blob??", 
                        "Blob Deletion", JOptionPane.YES_NO_OPTION );  
                
                if (retValue!=0)
                    return;
                
                int id = m.getId();
                ListIterator it = currentMarkerVector.listIterator();
                while(it.hasNext()){
                    IEGCellCntrMarker m1 = (IEGCellCntrMarker)it.next();
                    if (id == m1.getId())
                    {
                        currentMarkerVector.remove(m1);
                        it = currentMarkerVector.listIterator();
                    }
                
                }
                currentMarkerVector.removeMarker(m);
                if (currentMarkerVector.bIsFoci)
                    cc.DeleteFromResultTable(currentMarkerVector.getType(), id);
                else
                    cc.DeleteFromNuResultTable(id);
                        
                        /*CntrMarker.ZRange z = m.getZRange();
                if (z != null)
                {
                    for ( int i = z.z1; i <= z.z2; i++  )
                    {
                        CellCntrMarker m1 = currentMarkerVector.getMarkerFromPosition(new Point(x,y) , i);
                        currentMarkerVector.remove(m1);
                    }
                }*/
                
            }
            else
            {
                currentMarkerVector.remove(m);
            }
        }
        
    }
    
    public void mousePressed(MouseEvent e) {
        if (IJ.spaceBarDown() || Toolbar.getToolId()==Toolbar.MAGNIFIER || Toolbar.getToolId()==Toolbar.HAND || ROI_Mode) {
            super.mousePressed(e);
            return;
        }
        
        if (currentMarkerVector==null){
            IJ.error("Select a counter type first!");
            return;
        }
        
        mp(e);
        
        repaint();
        cc.populateTxtFields();
    }
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
    }
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
    }
    public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        if (!IJ.spaceBarDown() | Toolbar.getToolId()!=Toolbar.MAGNIFIER | Toolbar.getToolId()!=Toolbar.HAND)
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
    }
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
    }
    
    private Point point;
    private Rectangle srcRect = new Rectangle(0, 0, 0, 0);
    public void paint(Graphics g)
    {
        super.paint(g);
        srcRect = getSrcRect();
        Roi roi = img.getRoi();
        double xM=0;
        double yM=0;

        
        Graphics2D g2 = (Graphics2D)g;
        g2.setStroke(new BasicStroke(1f));
        g2.setFont(font);
        
        ShowFoci(g2);
        ShowNu(g2);
        
        
        /*
        double magnification = super.getMagnification();
        
        try {
            if (imageUpdated) {
                imageUpdated = false;
                img.updateImage();
            }
            Image image = img.getImage();
            if (image!=null)
                g.drawImage(image, 0, 0, (int)(srcRect.width*magnification),
                        (int)(srcRect.height*magnification),
                        srcRect.x, srcRect.y, srcRect.x+srcRect.width,
                        srcRect.y+srcRect.height, null);
            if (roi != null)
                roi.draw(g);
        } catch(OutOfMemoryError e) {
            IJ.outOfMemory("Paint "+e.getMessage());
        }
        */
        
        
        
    }
    
    public void ShowNu( Graphics2D g2 )
    {
        srcRect = getSrcRect();
        Roi roi = img.getRoi();
        double xM=0;
        double yM=0;
        
        if (bShowAllMarkers)
        {
            ListIterator it = typeNVector.listIterator();
            while(it.hasNext() && !Hide_Markers)
            {
                IEGCellCntrMarkerVector mv = (IEGCellCntrMarkerVector)it.next();
                int typeID = mv.getType();
                g2.setColor(mv.getColor());
                ListIterator mit = mv.listIterator();
                while(mit.hasNext())
                {
                    IEGCellCntrMarker m = (IEGCellCntrMarker)mit.next();
                    boolean sameSlice = m.getZ()==img.getCurrentSlice();
                    if (sameSlice || showAll){
                        xM = ((m.getX()-srcRect.x)*magnification);
                        yM = ((m.getY()-srcRect.y)*magnification);
                        if (sameSlice)
                        {
                         //   g2.drawRect((int)xM-2, (int)yM-2,10,10);
                            Geometry_Helper.drawDiamond(g2, (int)xM-2, (int)yM-2, (int)xM+4, (int)yM+4);
                            //g2.drawOval((int)xM-2, (int)yM-2,10,10);
                        }
                        else
                        {
                            g2.drawOval((int)xM-2, (int)yM-2,10,10);
                        }
                        if (showNumbers)
                        {
                         //   g2.drawString(Integer.toString(typeID), (int)xM+3, (int)yM-3);
                            g2.drawString(Integer.toString(m.getId()), (int)xM+3, (int)yM-3);
                        }
                    }
                }
            }
        }
    /*    else if (bShowMapMarkers)
        {
                RoiManager rm=RoiManager.getInstance();

                Roi mRoi;
                
                int mROIs = 1;
                Roi[] rois = null;
                if (rm!=null)
                {
                    rois = rm.getRoisAsArray();
                    mROIs = rm.getCount();
                    if (mROIs == 0)
                    {
                        mROIs = 1;
                        rm=null;
                    }
                    for ( int i = 0; i < mROIs; i++ )
                    {
                        mRoi = rois[i];
                        DrawOval(mRoi, g2);
                    }
                    
                }
                else
                {
                    mRoi = imp.getRoi();
                    DrawOval(mRoi, g2);
                }
                
        }*/
        else
        {
                int typeID = currentMarkerVector.getType();
                g2.setColor(currentMarkerVector.getColor());
                ListIterator mit = currentMarkerVector.listIterator();
                while(mit.hasNext())
                {
                    IEGCellCntrMarker m = (IEGCellCntrMarker)mit.next();
                    boolean sameSlice = m.getZ()==img.getCurrentSlice();
                    if (sameSlice || showAll){
                        xM = ((m.getX()-srcRect.x)*magnification);
                        yM = ((m.getY()-srcRect.y)*magnification);
                        if (sameSlice)
                        {
                            g2.drawRect((int)xM-2, (int)yM-2,10,10);
                            //g2.drawOval((int)xM-2, (int)yM-2,10,10);
                        }
                        else
                        {
                            g2.drawOval((int)xM-2, (int)yM-2,10,10);
                        }
                        if (showNumbers)
                        {
                         //   g2.drawString(Integer.toString(typeID), (int)xM+3, (int)yM-3);
                            g2.drawString(Integer.toString(m.getId()), (int)xM+3, (int)yM-3);
                        }
                    }
                }
            
        }
        
    }
    
    public void ShowFoci( Graphics2D g2 )
    {
        srcRect = getSrcRect();
        Roi roi = img.getRoi();
        double xM=0;
        double yM=0;
        
        if (bShowAllMarkers)
        {
            ListIterator it = typeVector.listIterator();
            while(it.hasNext() && !Hide_Markers)
            {
                IEGCellCntrMarkerVector mv = (IEGCellCntrMarkerVector)it.next();
                int typeID = mv.getType();
                g2.setColor(mv.getColor());
                ListIterator mit = mv.listIterator();
                while(mit.hasNext())
                {
                    IEGCellCntrMarker m = (IEGCellCntrMarker)mit.next();
                    boolean sameSlice = m.getZ()==img.getCurrentSlice();
                    if (sameSlice || showAll){
                        xM = ((m.getX()-srcRect.x)*magnification);
                        yM = ((m.getY()-srcRect.y)*magnification);
                        if (sameSlice)
                        {
                            g2.drawRect((int)xM-2, (int)yM-2,10,10);
                            //g2.drawOval((int)xM-2, (int)yM-2,10,10);
                        }
                        else
                        {
                            g2.drawOval((int)xM-2, (int)yM-2,10,10);
                        }
                        if (showNumbers)
                        {
                         //   g2.drawString(Integer.toString(typeID), (int)xM+3, (int)yM-3);
                            g2.drawString(Integer.toString(m.getId()), (int)xM+3, (int)yM-3);
                        }
                    }
                }
            }
        }
        else if (bShowMapMarkers)
        {
                RoiManager rm=RoiManager.getInstance();

                Roi mRoi;
                
                int mROIs = 1;
                Roi[] rois = null;
                if (rm!=null)
                {
                    rois = rm.getRoisAsArray();
                    mROIs = rm.getCount();
                    if (mROIs == 0)
                    {
                        mROIs = 1;
                        rm=null;
                    }
                    for ( int i = 0; i < mROIs; i++ )
                    {
                        mRoi = rois[i];
                        DrawOval(mRoi, g2);
                    }
                    
                }
                else
                {
                    mRoi = imp.getRoi();
                    DrawOval(mRoi, g2);
                }
                
        }
        else
        {
                int typeID = currentMarkerVector.getType();
                g2.setColor(currentMarkerVector.getColor());
                ListIterator mit = currentMarkerVector.listIterator();
                while(mit.hasNext())
                {
                    IEGCellCntrMarker m = (IEGCellCntrMarker)mit.next();
                    boolean sameSlice = m.getZ()==img.getCurrentSlice();
                    if (sameSlice || showAll){
                        xM = ((m.getX()-srcRect.x)*magnification);
                        yM = ((m.getY()-srcRect.y)*magnification);
                        if (sameSlice)
                        {
                            g2.drawRect((int)xM-2, (int)yM-2,10,10);
                            //g2.drawOval((int)xM-2, (int)yM-2,10,10);
                        }
                        else
                        {
                            g2.drawOval((int)xM-2, (int)yM-2,10,10);
                        }
                        if (showNumbers)
                        {
                         //   g2.drawString(Integer.toString(typeID), (int)xM+3, (int)yM-3);
                            g2.drawString(Integer.toString(m.getId()), (int)xM+3, (int)yM-3);
                        }
                    }
                }
            
        }
        
    }
    
    int m_OvalSize = 3;
    public void DrawOval(Roi mRoi,  Graphics2D g2)
    {
        long totH = 0, totA = 0;
        if ( ( cc.m_ShowMarkerMapOption & cc.HOMER_COLOR_ID ) != 0)
        {
            if (cc.markerMap.m_HomerMap !=null)
            {
                g2.setColor(cc.markerMap.m_HColor);

                for ( int i = 0; i < cc.markerMap.m_HomerMap.size(); i++ )
                {
                    Point p = (Point)cc.markerMap.m_HomerMap.get(i);

                    if ( mRoi ==null )
                    {
                        double xM = ((p.x-srcRect.x)*magnification);
                        double yM = ((p.y-srcRect.y)*magnification);

                        g2.fillOval((int)xM, (int)yM, m_OvalSize, m_OvalSize);
                        totH++;

                    }
                    else
                    {
                        if (mRoi.contains(p.x,p.y))
                        {
                            double xM = ((p.x-srcRect.x)*magnification);
                            double yM = ((p.y-srcRect.y)*magnification);

                            g2.fillOval((int)xM, (int)yM, m_OvalSize, m_OvalSize);
                            totH++;
                        }
                    }
                }
            }
        }

        if ( ( cc.m_ShowMarkerMapOption & cc.ARC_COLOR_ID ) != 0)
        {
            if (cc.markerMap.m_ArcMap !=null)
            {
                g2.setColor(cc.markerMap.m_AColor);
                for ( int i = 0; i < cc.markerMap.m_ArcMap.size(); i++ )
                {
                    Point p = (Point)cc.markerMap.m_ArcMap.get(i);

                    if ( mRoi == null )
                    {
                        double xM = ((p.x-srcRect.x)*magnification);
                        double yM = ((p.y-srcRect.y)*magnification);

                        g2.fillOval( (int)xM, (int)yM, m_OvalSize, m_OvalSize);
                        totA++;
                    }
                    else
                    {
                        if (mRoi.contains(p.x,p.y))
                        {
                            double xM = ((p.x-srcRect.x)*magnification);
                            double yM = ((p.y-srcRect.y)*magnification);

                            g2.fillOval( (int)xM, (int)yM, m_OvalSize, m_OvalSize);
                            totA++;
                        }
                    }
                }
            }
        }
        
        if ( cc.markerMap.m_txtH != null )
            cc.markerMap.m_txtH.setText("Total Homer: " +  Long.toString(totH));
        
        if ( cc.markerMap.m_txtA != null )
            cc.markerMap.m_txtA.setText("Total Arc: " +  Long.toString(totA));
    }
    
    public void removeLastMarker(){
        currentMarkerVector.removeLastMarker();
        repaint();
        cc.populateTxtFields();
    }
    public ImagePlus imageWithMarkers(){
        Image image = this.createImage(img.getWidth(),img.getHeight());
        Graphics gr = image.getGraphics();
        
        double xM=0;
        double yM=0;
        
        try {
            if (imageUpdated) {
                imageUpdated = false;
                img.updateImage();
            }
            Image image2 = img.getImage();
            if (image!=null)
                gr.drawImage(image2, 0, 0, img.getWidth(),img.getHeight(),null);
        } catch(OutOfMemoryError e) {
            IJ.outOfMemory("Paint "+e.getMessage());
        }
        
        Graphics2D g2r = (Graphics2D)gr;
        g2r.setStroke(new BasicStroke(1f));
        
        ListIterator it = typeVector.listIterator();
        while(it.hasNext()){
            IEGCellCntrMarkerVector mv = (IEGCellCntrMarkerVector)it.next();
            int typeID = mv.getType();
            g2r.setColor(mv.getColor());
            ListIterator mit = mv.listIterator();
            while(mit.hasNext()){
                IEGCellCntrMarker m = (IEGCellCntrMarker)mit.next();
                if (m.getZ()==img.getCurrentSlice()){
                    xM = m.getX();
                    yM = m.getY();
                    g2r.fillOval((int)xM-2, (int)yM-2,4,4);
                    if (showNumbers)
                        g2r.drawString(Integer.toString(typeID), (int)xM+3, (int)yM-3);
                }
            }
        }

        Vector displayList = getDisplayList();
         if (displayList!=null && displayList.size()==1) {
             Roi roi = (Roi)displayList.elementAt(0);
             if (roi.getType()==Roi.COMPOSITE)
                 roi.draw(gr);
         }
        
        return new ImagePlus("Markers_"+img.getTitle(),image);
    }
    
    public void measure(){
        IJ.setColumnHeadings("Type\tSlice\tX\tY\tValue");
        for (int i=1; i<=img.getStackSize(); i++){
            img.setSlice(i);
            ImageProcessor ip = img.getProcessor();
        
            ListIterator it = typeVector.listIterator();
            while(it.hasNext()){
                IEGCellCntrMarkerVector mv = (IEGCellCntrMarkerVector)it.next();
                int typeID = mv.getType();
                ListIterator mit = mv.listIterator();
                while(mit.hasNext()){
                    IEGCellCntrMarker m = (IEGCellCntrMarker)mit.next();
                    if (m.getZ()==i){
                        int xM = m.getX();
                        int yM = m.getY();
                        int zM = m.getZ();
                        double value = ip.getPixelValue(xM,yM);
                        IJ.write(typeID+"\t"+zM+"\t"+xM+"\t"+yM+"\t"+value);
                    }
                }
            }
        }
    }
    
    public Vector getTypeVector() {
        return typeVector;
    }
    
    public void setTypeVector(Vector typeVector) {
        this.typeVector = typeVector;
    }
    
    public Vector getTypeNVector() {
        return typeNVector;
    }
    
    public void setTypeNVector(Vector typeVector) {
        this.typeNVector = typeVector;
    }

    public IEGCellCntrMarkerVector getCurrentMarkerVector() {
        return currentMarkerVector;
    }
    
    public void setCurrentMarkerVector(IEGCellCntrMarkerVector currentMarkerVector) {
        this.currentMarkerVector = currentMarkerVector;
    }
    
    public boolean isDelmode() {
        return delmode;
    }
    
    public void setDelmode(boolean delmode) {
        this.delmode = delmode;
    }
    
    public boolean isShowNumbers() {
        return showNumbers;
    }

    public void setShowNumbers(boolean showNumbers) {
        this.showNumbers = showNumbers;
    }
    
    public void setShowAll(boolean showAll) {
        this.showAll = showAll;
    }
    
    public void setIEGAnalysis(boolean IEGAnalysis)
    {
        this.IEG_Analysis = IEGAnalysis;
    }

    public void setHideMarkers(boolean HideMarkers)
    {
        this.Hide_Markers = HideMarkers;
    }

    public void setROIMode(boolean ROIMode)
    {
        this.ROI_Mode = ROIMode;
    }
    public void setShowAllMarkers(boolean ShowAllMarkers)
    {
        this.bShowAllMarkers = ShowAllMarkers;
    }
    public void setShowBlobMaps(boolean ShowBlobMaps)
    {
        this.bShowMapMarkers = ShowBlobMaps;
    }
    public void setNu(boolean bN)
    {
        this.bNEnabled = bN;
    }
    
    
    
}
