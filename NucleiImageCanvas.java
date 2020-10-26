/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.PolygonRoi;
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
import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author vivek.trivedi
 */
public class NucleiImageCanvas extends ImageCanvas
{
    
    private Vector typeVector;
 //   private CellCntrMarkerVector currentMarkerVector;
    private IEG_Nuclei_Counter cc;
    private ImagePlus img;
    private boolean delmode = false;
    private boolean showNumbers = true;
    private boolean IEG_Analysis = false;
    private boolean Hide_Markers = false;
    private boolean ROI_Mode = false;
    private boolean bShowAllMarkers = true;
    private ImagePlus m_mapImage = null;
    public int m_roiId = -1;
    
    public boolean bReset = false;
    public boolean bShow_only_active = false;
    public boolean bShowAll_only_active = false;
    public boolean bShowAll = false;
    
    public ArrayList<IEGPolygonRoi> m_vRoi = new ArrayList<IEGPolygonRoi>();
    
    Hashtable m_ncRois = null;
    java.awt.List m_ncList = null;
      
    
    private Font font = new Font("SansSerif", Font.PLAIN, 10);
    
    /** Creates a new instance of CellCntrImageCanvas */
    public NucleiImageCanvas(ImagePlus img, ImagePlus mapImg, IEG_Nuclei_Counter cc, Vector displayList) {
        super(img);
        this.img=img;
        this.typeVector = typeVector;
        this.cc = cc;
        if (displayList!=null)
            this.setDisplayList(displayList);
        this.m_mapImage = mapImg;
        m_vRoi.clear();
        
         m_ncRois = cc.getROIs();
         m_ncList = cc.getList();
        
    }
    
    void setMapImage(ImagePlus mapImage)
    {
        m_mapImage = mapImage;
    }
    
    void setImage(ImagePlus img)
    {
        this.img = img;
    }
    
    public void mouseReleased(MouseEvent e)
    {
        super.mouseReleased(e);
        
    }
    
    int getPixelFromMap(int x, int y)
    {
        int[] cVal = m_mapImage.getPixel(x, y);
        if (cVal[0]==0)
        {
            int cSlice = m_mapImage.getCurrentSlice();
            for ( int i = cSlice-1; i >= cSlice-2 && i > 0; i-- )
            {
                m_mapImage.setSlice(i);
                cVal = m_mapImage.getPixel(x, y);
                if (cVal[0] > 0)
                {
                    m_mapImage.setSlice(cSlice);
                    return cVal[0];
                }
            }
            
            for ( int i = cSlice+1; i <= cSlice+2 && i < m_mapImage.getStackSize(); i++ )
            {
                m_mapImage.setSlice(i);
                cVal = m_mapImage.getPixel(x, y);
                if (cVal[0] > 0)
                {
                    m_mapImage.setSlice(cSlice);
                    return cVal[0];
                }
            }
            
        }
        return cVal[0];
    }
    
    public void mousePressed(MouseEvent e) {
        if (IJ.spaceBarDown() || Toolbar.getToolId()==Toolbar.MAGNIFIER || 
                Toolbar.getToolId()==Toolbar.HAND || ROI_Mode) {
            super.mousePressed(e);
            return;
        }
        int x = super.offScreenX(e.getX());
        int y = super.offScreenY(e.getY());
        
        //getPixelFromMap(x,y);
        
        m_mapImage.setSlice(img.getCurrentSlice());
        
     /*   int[] cVal = m_mapImage.getPixel(x, y);
        
        m_roiId = cVal[0];*/
        
        m_roiId = getPixelFromMap(x,y);
        
        IEGPolygonRoi pRoi =  cc.getPolyRoiById( m_roiId );
        
        if (pRoi!=null)
        {
            if (!pRoi.bDraw)
            {
                pRoi.bDraw = true;
                m_vRoi.add(pRoi);
            }
            else
            {
                pRoi.bDraw = false;
            }
        }
        
        
        repaint();
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
        
        int sliceId = this.img.getCurrentSlice();        
        if (!bReset)
        {
            if ( bShowAll_only_active || bShowAll )
            {
                int n = m_ncList.getItemCount();
		for (int i=0; i<n; i++) 
                {
                    String label = m_ncList.getItem(i);
                    IEGPolygonRoi pr = (IEGPolygonRoi) m_ncRois.get(label);
                
                    if (bShowAll_only_active)
                    {
                       if (pr.m_pParent.IsActive(sliceId))
                          pr.draw(g);
                    }
                    else
                          pr.draw(g);
                }
            }            
            else
            {
                for (int i = 0; i < m_vRoi.size(); i++ )
                {
                    IEGPolygonRoi pr = (IEGPolygonRoi)m_vRoi.get(i);
                    if (pr!=null)
                    {
                        if (pr.bDraw)
                        {
                            if (!bShow_only_active)
                                pr.draw(g);
                            else
                            {
                                if (pr.m_pParent.IsActive(sliceId))
                                    pr.draw(g);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            bReset=false;
            for (int i = 0; i < m_vRoi.size(); i++ )
            {
                IEGPolygonRoi pr = (IEGPolygonRoi)m_vRoi.get(i);
                if (pr!=null)
                {
                    pr.bDraw = false;
                }                    
            }            
            m_vRoi.clear();
        }
    }
}
