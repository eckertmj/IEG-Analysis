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
import ij.*;
import ij.process.*;
import ij.measure.*;
//import ij.plugin.Geometry_Helper;
import ij.plugin.frame.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Collections;
import ij.plugin.frame.RoiManager;
import ij.plugin.*;


/**
 *
 * @author vivek.trivedi
 */
public class IEGPolygonRoi extends PolygonRoi{
    public IEG_Nuclei_Counter.RoiWithStat m_pParent = null;
    public boolean bDraw = false;
    
    public Color[] _3DColor = null;
    public boolean m_bIsSubRoi = false;

    public int m_cIdx = -1;
    
    public void Init3DColor()
    {
        _3DColor = new Color[20];

        _3DColor[0] = Color.BLUE;
        _3DColor[1] = Color.GREEN;
        _3DColor[2] = new Color(255, 10, 20);
        _3DColor[3] = Color.CYAN;
        _3DColor[4] = Color.GRAY;
        _3DColor[5] = new Color(10, 255, 10 );
        _3DColor[6] = Color.MAGENTA;
        _3DColor[7] = Color.ORANGE;
        _3DColor[8] = Color.pink;
        _3DColor[9] = Color.RED;
        _3DColor[10] = Color.YELLOW;
        _3DColor[11] = Color.WHITE;
        _3DColor[12] = new Color(100, 255, 20);
        _3DColor[13] = new Color(100, 20, 255 );
        _3DColor[14] = new Color(20, 255, 100);
        _3DColor[15] = new Color(150, 100, 255);
        _3DColor[16] = new Color(100, 150, 255);
        _3DColor[17] = new Color(255, 150, 100);
        _3DColor[18] = new Color(50, 255, 255);
        _3DColor[19] = new Color(255, 255, 10);
    }
    
    public IEGPolygonRoi(int[] xPoints, int[] yPoints, int nPoints, int type)
    {
        super(xPoints, yPoints, nPoints, type);
        Init3DColor();
    }
    
    public IEGPolygonRoi(float[] xPoints, float[] yPoints, int nPoints, int type)
    {
        super(xPoints, yPoints, nPoints, type);
        Init3DColor();
    }
    
    public IEGPolygonRoi(Polygon p, int type)
    {
        super(p.xpoints, p.ypoints, p.npoints, type);
        Init3DColor();
    }
    
    public IEGPolygonRoi(int[] xPoints, int[] yPoints, int nPoints, ImagePlus imp, int type)
    {
	super(xPoints, yPoints, nPoints, imp ,type);
        Init3DColor();
    }
    
    public IEGPolygonRoi(int sx, int sy, ImagePlus imp) 
    {
        super(sx, sy, imp);
        Init3DColor();
    }
    
    
    public void setParent( IEG_Nuclei_Counter.RoiWithStat pParent)
    {
        m_pParent = pParent;
    }
    
    
    public void getParent( IEG_Nuclei_Counter.RoiWithStat pParent)
    {
        m_pParent = pParent;
    }
    
    public void draw(Graphics g)
    {
        super.draw(g);
      //  g.drawPolygon(xp2, yp2, nPoints);
        Draw3DLayers(m_pParent, g, 0, imp);
        
    }

    void Draw3DLayers(IEG_Nuclei_Counter.RoiWithStat rs, Graphics g, int cIdx, ImagePlus imp)
    {
        boolean b = false;
        if (b)
            return;
        IJ.log("\\Clear");
        if (!m_bIsSubRoi)
            return;
        if (m_cIdx==-1)
            m_cIdx = 0;

        IJ.log( rs.m_Name +  
                       "(" + roundTwoDecimals(rs.cx) + "," + roundTwoDecimals(rs.cy) + "," + roundTwoDecimals(rs.cz) + ")" + 
                       " <area> " + roundTwoDecimals(rs.m_RoiStat.area) +  
                       " <cr> " + roundTwoDecimals(rs.m_Circularity) +  
                       " <AR> " + roundTwoDecimals(rs.m_AxisAspectRatio) +
                       " <so> " + roundTwoDecimals(rs.m_Solidity) + 
                       " <ro> " + roundTwoDecimals(rs.m_Roundness));


        for ( int i = 0; i < rs.m_Over_ROIs_f__a_b.size(); i++ )
        {
                    IEG_Nuclei_Counter.RoiWithStat roiS = (IEG_Nuclei_Counter.RoiWithStat)rs.m_Over_ROIs_f__a_b.get(i);
                    if (cIdx>=20)
                        cIdx = 0;

                    //if (((PolygonRoi)ors.m_OverlapRoi.m_Roi).getImage() == null)
                    IEGPolygonRoi pr = (IEGPolygonRoi)roiS.m_Roi;

                    pr.setImage(imp);

                    if (_3DColor == null)
                        Init3DColor();
                    
                    g.setColor(_3DColor[m_cIdx]);
                    cIdx++;
                    pr.updatePolygon3D();
//                        g.drawPolygon(ors.m_OverlapRoi.m_Roi.getPolygon());
                    g.drawPolygon(pr.xp2, pr.yp2, pr.nPoints);
                    IJ.log( roiS.m_Name + Integer.toString(i) + 
                       "(" + roundTwoDecimals(roiS.cx) + "," + roundTwoDecimals(roiS.cy) + "," + roundTwoDecimals(roiS.cz) + ")" + 
                       " <area> " + roundTwoDecimals(roiS.m_RoiStat.area) +  
                       " <cr> " + roundTwoDecimals(roiS.m_Circularity) +  
                       " <AR> " + roundTwoDecimals(roiS.m_AxisAspectRatio) +
                       " <so> " + roundTwoDecimals(roiS.m_Solidity) + 
                       " <ro> " + roundTwoDecimals(roiS.m_Roundness));

        }
    }
    
    public void updatePolygon3D()
    {
        updatePolygon();
    }

    
    void DumpROIPoints()
    {
        IJ.log("\\Clear");
        for ( int i = 0; i < nPoints; i+=2)
        {
            IJ.log(roundTwoDecimals(xp[i]) + "," + roundTwoDecimals(yp[i]));
        }
        IJ.log(roundTwoDecimals(xp[0]) + "," + roundTwoDecimals(yp[0]));
    }

    
    String roundTwoDecimals(double d) {
            DecimalFormat twoDForm = new DecimalFormat("#.##");
            return twoDForm.format(d);
    }
    
    void drawAllLayers(IEG_Nuclei_Counter.RoiWithStat rs, Graphics g, int cIdx, ImagePlus imp)
    {
        if (rs.m_Over_ROIs_f__a_b != null)
        {
            if (rs.m_Over_ROIs_f__a_b.size() > 0)
            {
                IEG_Nuclei_Counter.OverlapROI ors = (IEG_Nuclei_Counter.OverlapROI)rs.m_Over_ROIs_f__a_b.get(0);
                if ( ors.m_OverlapRoi.m_Circularity >= 0)//0.65 )
                {

                    IJ.log(">>"+ ors.m_OverlapRoi.m_Name +  "<Area1> " + roundTwoDecimals(rs.m_RoiStat.area) +  "<Area2> " + roundTwoDecimals(ors.m_OverlapRoi.m_RoiStat.area) + 
                        "<O_Area> " + roundTwoDecimals(ors.m_OverlapArea) + " <O_AP> " + roundTwoDecimals(ors.m_OverlapAreaPer) + 
                        " <cr> " + roundTwoDecimals(ors.m_OverlapRoi.m_Circularity) + "<AR>" + roundTwoDecimals(ors.m_OverlapRoi.m_AxisAspectRatio)
                            + "<so>" + roundTwoDecimals(ors.m_OverlapRoi.m_Solidity) + "<ro>" + roundTwoDecimals(ors.m_OverlapRoi.m_Roundness) );
                    ors.m_OverlapRoi.m_Moments.printMoments();
                }
               /* else
                {
                    g.setColor(Color.green);
                    ((PolygonRoi)ors.m_OverlapRoi.m_Roi).updatePolygon3D();
                    PolygonRoi pr = (PolygonRoi)ors.m_OverlapRoi.m_Roi;
                    g.drawPolygon(pr.xp2, pr.yp2, pr.nPoints);

                    g.drawPolygon(ors.m_OverlapRoi.m_Roi.getPolygon());
                    IJ.log("**"+ ors.m_OverlapRoi.m_Name +  "<Area1> " + roundTwoDecimals(rs.m_RoiStat.area) +  "<Area2> " + roundTwoDecimals(ors.m_OverlapRoi.m_RoiStat.area) + 
                        "<O_Area> " + roundTwoDecimals(ors.m_OverlapArea) + " <O_AP> " + roundTwoDecimals(ors.m_OverlapAreaPer) + 
                        " <cr> " + roundTwoDecimals(ors.m_OverlapRoi.m_Circularity) + "<AR>" + roundTwoDecimals(ors.m_OverlapRoi.m_AxisAspectRatio) 
                            + "<so>" + roundTwoDecimals(ors.m_OverlapRoi.m_Solidity) + "<ro>" + roundTwoDecimals(ors.m_OverlapRoi.m_Roundness) );
                    ors.m_OverlapRoi.m_Moments.printMoments();
                }*/
                drawAllLayers(ors.m_OverlapRoi,g, cIdx,  m_pParent.m_Image);
            }
        }
    }
    
    
    
        public class _1DPoint
        {
            public int p1Idx = -1;
            public Point2D p1;
            public double p1_p2_angle = Double.MAX_VALUE;
            public double p3_p1_p2_angle = Double.MAX_VALUE;
            
            public int p2Idx = -1;
            public Point2D p2;
            
            public int p3Idx = -1;
            public Point2D p3;
            public double p3_p2_angle = Double.MAX_VALUE;
            public double p1_p3_p2_angle = Double.MAX_VALUE;
            
            public int m_dir;
            
            public double p1_p3_angle = Double.MAX_VALUE;

            public ArrayList m_vBreakable = new ArrayList();

            
            public double slope = 0.0;
            
            public int p1Idx_i1 = -1;
            public int p2Idx_i1 = -1;
            
            public int p1Idx_i2 = -1;
            public int p2Idx_i2 = -1;
            
            InterSecLineSeg iLineSeg = null;
            
            
        }
        
        class InterSecLineSeg implements Comparable
        {
            public int p1x = -1;
            public int p1y = -1;

            public int ipx = -1;
            public int ipy = -1;
            
            public double dist = 0.0d;
            
            public _1DPoint m_parent = null;
            
            public void calcDist()
            {
                dist = Point2D.distance(p1x , p1y, ipx, ipy);
            }
            
            @ Override
            public int compareTo(Object o)
            {
                InterSecLineSeg t = (InterSecLineSeg)o;
                if( this.dist < t.dist)
                    return -1;
                else if( this.dist > t.dist)
                    return 1;
                else
                    return 0;
            }
        }
        
        public java.util.ArrayList _1D = new java.util.ArrayList();
        public java.util.ArrayList _1D_Change = new java.util.ArrayList();
        public java.util.ArrayList _1D_In_Point = new java.util.ArrayList();
        
        //
        public java.util.ArrayList _1D_Int_Point1 = new java.util.ArrayList();
        public java.util.ArrayList _1D_Int_Point2 = new java.util.ArrayList();
        
        //direction
        int[] m_Direction = {0,1,2,3,4,5,6,7};
        
        void calc1Deri(int stepSize)
        {
            _1D.clear();
             _1D_Change.clear();
             
             _1D_Int_Point1.clear();
             _1D_Int_Point2.clear();

             _1D_In_Point.clear();
            
            for ( int i = 0; i < nPoints; i+=stepSize)
            {
               // IJ.log(roundTwoDecimals(xp[i]) + "," + roundTwoDecimals(yp[i]));
                int p1 = i;
                int p2 = i + stepSize;
                int p3 = p2 + stepSize;
                
                if (p2>=nPoints)
                {
                    p2 = p2-nPoints;
                }
                
                if (p3>=nPoints)
                {
                    p3 = p3 - nPoints;
                }
                Geometry_Helper gh = new Geometry_Helper();
                
                
                _1DPoint dp = new _1DPoint();
                
                dp.slope = (double)(yp[p2] - yp[p1]) / (double)(xp[p2] - xp[p1]);
                dp.p1Idx = p1;
                dp.p1 = new Point(xp[p1], yp[p1]);
                
                dp.p2Idx = p2;
                dp.p2 = new Point(xp[p2], yp[p2]);
                dp.p1_p2_angle = gh.getAngleWithXAxisInDegree((float)dp.p1.getX(), (float)dp.p1.getY(), (float)dp.p2.getX(), (float)dp.p2.getY());
                
                dp.p3Idx = p3;
                dp.p3 = new Point(xp[p3], yp[p3]);
                dp.p3_p2_angle = gh.getAngleWithXAxisInDegree((float)dp.p3.getX(), (float)dp.p3.getY(), (float)dp.p2.getX(), (float)dp.p2.getY());

                dp.p3_p1_p2_angle = gh.getAngleInDegree((float)dp.p3.getX(), (float)dp.p3.getY(), (float)dp.p1.getX(), (float)dp.p1.getY(),
                       (float)dp.p1.getX(), (float)dp.p1.getY(),(float)dp.p2.getX(), (float)dp.p2.getY() );
                dp.p1_p3_p2_angle = gh.getAngleInDegree((float)dp.p1.getX(), (float)dp.p1.getY(), (float)dp.p3.getX(), (float)dp.p3.getY(),
                       (float)dp.p3.getX(), (float)dp.p3.getY(),(float)dp.p2.getX(), (float)dp.p2.getY() );
                
                dp.p1_p3_angle = gh.getAngleWithXAxisInDegree((float)dp.p1.getX(), (float)dp.p1.getY(), (float)dp.p3.getX(), (float)dp.p3.getY());
                
                _1D.add(dp);
            }
        }
        void calcSlopeChange()
        {
            IJ.log("-----");
            boolean bPositive = true;
            _1DPoint dp = (_1DPoint)_1D.get(0);
            _1DPoint dp1;
            
            if (_1D.size()>0)
            {
                if (dp.slope < 0)
                    bPositive=false;
            }

            for (int i = 1; i < _1D.size(); i++)
            {
                dp = (_1DPoint)_1D.get(i);
                if ( dp.slope > 0  && !bPositive)
                {
                    dp1 =  (_1DPoint)_1D.get(i-1);
                    EstimateSlopeDirection(dp1);
                    _1D_Change.add(dp1);
                    IJ.log( roundTwoDecimals(xp[dp1.p1Idx]) + "," + roundTwoDecimals(yp[dp1.p1Idx]) + " - " +
                            roundTwoDecimals(xp[dp1.p2Idx]) + "," + roundTwoDecimals(yp[dp1.p2Idx]) + " - " + 
                            roundTwoDecimals(xp[dp1.p3Idx]) + "," + roundTwoDecimals(yp[dp1.p3Idx]));
                    bPositive = true;
                }
                else if (dp.slope < 0 && bPositive)
                {
                    dp1 =  (_1DPoint)_1D.get(i-1);
                    EstimateSlopeDirection(dp1);
                    _1D_Change.add(dp1);
                    IJ.log( roundTwoDecimals(xp[dp1.p1Idx]) + "," + roundTwoDecimals(yp[dp1.p1Idx]) + " - "+ 
                            roundTwoDecimals(xp[dp1.p2Idx]) + "," + roundTwoDecimals(yp[dp1.p2Idx]) + " - " + 
                            roundTwoDecimals(xp[dp1.p3Idx]) + "," + roundTwoDecimals(yp[dp1.p3Idx]));
                    bPositive = false;
                }
                
            }
        }
        
        public void EstimateSlopeDirection(_1DPoint dp)
        {
            Geometry_Helper gh = new Geometry_Helper();
            
            int x1 = (int)dp.p1.getX();
            int y1 = (int)dp.p1.getY();
            
            int x2 = (int)dp.p2.getX();
            int y2 = (int)dp.p2.getY();

            int x3 = (int)dp.p3.getX();
            int y3 = (int)dp.p3.getY();
            
            
            
        /*    double angle1 = gh.getAngleWithXAxisInDegree(x1, y1, x3, y3);
            
            double angle2 = 0.0d;
            if ( y1 > y2 )
            {
                angle2 = gh.getAngleWithXAxisInDegree(x2, y2, x1, y1);
            }
            else
            {
                angle2 = gh.getAngleWithXAxisInDegree(x3, y3, x1, y1);
            }
            
            if (angle2 > angle1) //west
            {
                dp.m_dir = 0;
            }
            else //e
            {
                dp.m_dir = 4;
            }*/
            boolean q1 = false;
            boolean q2 = false;
            boolean q3 = false;
            boolean q4 = false;
            boolean east = false;
            boolean west = false;
            boolean south = false;
            boolean north = false;
            
            if (x2 > x1 && x2 > x3)
            {
                if ( y2 > y1 && y2 > y3)
                {
                    q1 = true;
                    dp.m_dir = 1;
                }
                else if ( y2 < y1 && y2 < y3)
                {
                    q4 = true;
                    dp.m_dir = 7;
                }
                else if (y2 == Math.min(y1,y3) || y2 == Math.max(y1,y3))
                {
                    east = true;
                    dp.m_dir = 0;
                }
                else if (y2 > Math.min(y1,y3) && y2 < Math.max(y1,y3))
                {
                    if (x1==x3)
                    {
                        east = true;
                        dp.m_dir = 0;

                    }
                    else if (x1 > x3 && y1 > y3)
                    {
                        q4 = true;
                        dp.m_dir = 7;
                    }
                    else if (x1 > x3 && y1 < y3)
                    {
                        q1 = true;
                        dp.m_dir = 1;
                        
                    }                    
                    else if (x1 < x3 && y1 < y3)
                    {
                        q4 = true;
                        dp.m_dir = 7;
                    
                    }
                    else if (x1 < x3 && y1 > y3)
                    {
                        q1 = true;
                        dp.m_dir = 1;
                    }
                    else
                        dp.m_dir = -1;
                    //lots of option
                }
                else
                    dp.m_dir = -1;
            }
            else if (x2 < x1 && x2 < x3)
            {
                if ( y2 >= Math.max(y1,y3))
                {
                    q2 = true;
                    dp.m_dir = 3;
                    
                }
                else if ( y2 < Math.min(y1,y3))
                {
                    q3 = true;
                    dp.m_dir = 5;
                    
                }
                else if (y2 == Math.min(y1,y3) || y2 == Math.max(y1,y3))
                {
                    west = true;
                    dp.m_dir = 4;
                    
                }
                else if (y2 > Math.min(y1,y3) && y2 < Math.max(y1,y3))
                {
                    if (x1==x3)
                    {
                        west = true;
                        dp.m_dir = 4;
                    }
                    else if (x1 > x3 && y1 > y3)
                    {
                        q2 = true;
                        dp.m_dir = 3;
                    }
                    else if (x1 > x3 && y1 < y3)
                    {
                        q3 = true;
                        dp.m_dir = 5;
                    }
                    else if (x1 < x3 && y1 < y3)
                    {
                        q2 = true;
                        dp.m_dir = 3;
                    }
                    else if (x1 < x3 && y1 > y3)
                    {
                        q3 = true;
                        dp.m_dir = 5;
                    }
                    else
                        dp.m_dir=-1;//lots of option
                }
                else
                    dp.m_dir=-1;
            }
            else if ( x2 >= Math.min(x1,x3) && x2 <= Math.max(x1,x3) ) 
            {
                if (y1 == y3 && y2 > y3)
                {
                    north = true;
                    dp.m_dir = 2;
                }
                else if (y1 == y3 && y2 < y3)
                {
                    south = true;
                    dp.m_dir = 6;
                }
                else if  ( y1 > y3 && x1 > x3 && y2 > Math.min(y1,y3) ) // /
                {
                    double a1 = gh.getAngleWithXAxisInDegree(x3, y3, x1, y1);
                    double a2 = gh.getAngleWithXAxisInDegree(x3, y3, x2, y2);
                    
                    if ( a1 > a2 )
                    {
                        q4 = true;
                        dp.m_dir = 7;
                    }
                    else if ( a1 < a2 )
                    {
                        q2 = true;
                        dp.m_dir = 3;
                    }
                    else
                        dp.m_dir=-1;
                }
                else if (y3 > y1 && x1 > x3 && y2 > Math.min(y1,y3)) // \
                {
                    double a1 = gh.getAngleWithXAxisInDegree(x1, y1, x3,y3);
                    double a2 = gh.getAngleWithXAxisInDegree(x1, y1, x2, y2);
                    
                    if ( a1 > a2 )
                    {
                        q1 = true;
                        dp.m_dir = 1;
                    }
                    else if ( a1 < a2 )
                    {
                        q3 = true;
                        dp.m_dir = 5;
                    }
                    else
                        dp.m_dir=-1;
                }
                else if  ( y1 < y3 && x1 < x3 && y2 > Math.min(y1,y3) ) // /
                {
                    double a1 = gh.getAngleWithXAxisInDegree(x1, y1, x3, y3);
                    double a2 = gh.getAngleWithXAxisInDegree(x1, y1, x2, y2);
                    
                    if ( a1 > a2 )
                    {
                        q4 = true;
                        dp.m_dir = 7;
                    }
                    else if ( a1 < a2 )
                    {
                        q2 = true;
                        dp.m_dir = 3;
                    }
                    else
                        dp.m_dir=-1;
                    
                }
                else if  (y1 > y3 && x1 < x3 && y2 > Math.min(y1,y3)) // \
                {
                    double a1 = gh.getAngleWithXAxisInDegree(x3, y3, x1,y1);
                    double a2 = gh.getAngleWithXAxisInDegree(x3, y3, x2, y2);
                    
                    if ( a1 > a2 )
                    {
                        q1 = true;
                        dp.m_dir = 1;
                    }
                    else if ( a1 < a2 )
                    {
                        q3 = true;
                        dp.m_dir = 5;
                    }
                    else
                        dp.m_dir=-1;
                }
                else
                    dp.m_dir = -1;
                
            }
            /*else if ( x2 == Math.min(x1,x3) && x1 > x3 )
            {
                if ( y2 > y1 && y2 > y3)
                {
                    north = true;
                    dp.m_dir = 2;
                }
                else if ( y2 < y1 && y2 < y3)
                {
                    south = true;
                    dp.m_dir = 6;
                }
                else if (y2 == Math.min(y1,y3))
                {
                    
                }
                else
                    dp.m_dir =-1;
            }
            else if (y2 == Math.min(y1,y3))
            {
                if ( x2 > x1 && x2 > x3)
                {
                    east = true;
                    dp.m_dir = 0;
                }
                else if ( y2 < y1 && y2 < y3)
                {
                    west = true;
                    dp.m_dir = 4;
                }
                else
                    dp.m_dir=-1;
            }*/
            else
                dp.m_dir=-1;

        }
        
        
        void filterSlopePoints(int stepSize)
        {
            boolean bL1Intersect = false, bL2Intersect = false;
        //     _1D_Int_Point1.clear();
        //     _1D_Int_Point2.clear();
             
            filterSlopePoints_1:
            for ( int i = 0; i < _1D_Change.size(); i++ )
            {
                 _1D_Int_Point1.clear();
                 _1D_Int_Point2.clear();
                 
                bL1Intersect =  bL2Intersect = false;
                 _1DPoint dp = (_1DPoint)_1D_Change.get(i); 
                 
                 float px1 = xp[dp.p1Idx];
                 float py1 = yp[dp.p1Idx];

                 float px2 = xp[dp.p2Idx];
                 float py2 = yp[dp.p2Idx];
                 float px3 = -1, py3 = -1, px4 = -1, py4 = -1;

                 filterSlopePoints_2:
                 for (int j = 0; j < nPoints; j+=stepSize)
                 {
                     int k = j + stepSize;
                     if ( j == dp.p1Idx && k == dp.p2Idx )
                         continue filterSlopePoints_2;
                         
                     if ( j == dp.p2Idx && k == dp.p3Idx )
                         continue filterSlopePoints_2;
                     
                     if (k >= nPoints)
                         k = k -nPoints;

                     px3 = xp[j];
                     py3 = yp[j];
                     

                     px4 = xp[k];
                     py4 = yp[k];
                     
                     InterSecLineSeg t = new InterSecLineSeg();
                    
                     if (doInterSect(dp.p1Idx, dp.p2Idx, j, k, t))
                     {
                          dp.p1Idx_i1 = j;
                          dp.p2Idx_i1 = k;
                          t.m_parent = dp;
                          dp.iLineSeg = t;
                          _1D_Int_Point1.add(t);
                          
                         // break filterSlopePoints_2;
                     }
                 }
                 
                 if ( _1D_Int_Point1.size() > 0)
                 {
                     Collections.sort(_1D_Int_Point1);
                     InterSecLineSeg t = (InterSecLineSeg)_1D_Int_Point1.get(0);
                     
                     Rectangle r = getBounds();
                     int _px = r.x + t.ipx;
                     int _py = r.y + t.ipy;
           
                     if ( this.contains( _px, _py ) )
                     {
                         bL1Intersect = true;
                         
                         IJ.log("----");
                         px3 = xp[t.m_parent.p1Idx_i1];
                         py3 = yp[t.m_parent.p1Idx_i1];
                         px4 = xp[t.m_parent.p2Idx_i1];
                         py4 = yp[t.m_parent.p2Idx_i1];

                         IJ.log( px1 + "," + py1 + " - "+ px2 + "," + py2 + " - " + px3 + "," + py3 + "-" + px4 + "," + py4 );
                         IJ.log("iPoint: "+ roundTwoDecimals(t.ipx) + "," + roundTwoDecimals(t.ipy));
                         IJ.log("----");                     
                     }
                 }
                 
                 px1 = xp[dp.p3Idx];
                 py1 = yp[dp.p3Idx];
                 px2 = xp[dp.p2Idx];
                 py2 = yp[dp.p2Idx];
                 
                 filterSlopePoints_3:
                 for (int j = 0; j < nPoints; j+=stepSize)
                 {
                     int k = j + stepSize;
                     if ( j == dp.p1Idx && k == dp.p2Idx )
                         continue filterSlopePoints_3;
                         
                     if ( j == dp.p2Idx && k == dp.p3Idx )
                         continue filterSlopePoints_3;
                     
                     if (k >= nPoints)
                         k = k -nPoints;

                     px3 = xp[j];
                     py3 = yp[j];
                     

                     px4 = xp[k];
                     py4 = yp[k];
                     
                     InterSecLineSeg t = new InterSecLineSeg();
                     if ( doInterSect( dp.p3Idx, dp.p2Idx, j, k, t ) )
                     {
                          dp.p1Idx_i2 = j;
                          dp.p2Idx_i2 = k;
                          t.m_parent = dp;
                          dp.iLineSeg = t;
                          _1D_Int_Point2.add(t);
                     //     bL2Intersect = true;
                     //     break filterSlopePoints_3;
                     }
                 }
                 if ( _1D_Int_Point2.size() > 0)
                 {
                     Collections.sort(_1D_Int_Point2);
                     InterSecLineSeg t = (InterSecLineSeg)_1D_Int_Point2.get(0);
                     
                     Rectangle r = getBounds();
                     int _px = r.x + t.ipx;
                     int _py = r.y + t.ipy;
           
                     if ( this.contains( _px, _py ) )
                     {
                         bL2Intersect = true;
                         
                         IJ.log("----");
                         px3 = xp[t.m_parent.p1Idx_i2];
                         py3 = yp[t.m_parent.p1Idx_i2];
                         px4 = xp[t.m_parent.p2Idx_i2];
                         py4 = yp[t.m_parent.p2Idx_i2];

                         IJ.log( px1 + "," + py1 + " - "+ px2 + "," + py2 + " - " + px3 + "," + py3 + "-" + px4 + "," + py4 );
                         IJ.log("iPoint: "+ roundTwoDecimals(t.ipx) + "," + roundTwoDecimals(t.ipy));
                         IJ.log("----");                     
                     }
                     
                 }

                 
                 if ( bL1Intersect && bL2Intersect )
                     _1D_In_Point.add(dp);
                 
            }
        }
        
        boolean doInterSect( int a, int b, int j, int k, InterSecLineSeg tl)
        {
             float px1 = xp[a];
             float py1 = yp[a];

             float px2 = xp[b];
             float py2 = yp[b];


             float px3 = xp[j];
             float py3 = yp[j];


             float px4 = xp[k];
             float py4 = yp[k];

             Geometry_Helper gh = new Geometry_Helper();
             gh.RayToLineSegmentIntersection(px1,py1,px2,py2,  px3, py3, px4, py4);
             if (gh.m_result.NumberOfSolutions() > 0 )
             {
                 float ix = gh.m_result.points.get(0).x;
                 float iy = gh.m_result.points.get(0).y;

/*                         if ( ix != px1 && ix != px2 && ix != px3 && ix != px4 &&
                      iy != py1 && iy != py2 && iy != py3 && iy != py4   )*/
                 if ( ix != px1 && ix != px2 && iy != py1 && iy != py2 )
                 {
                   //  IJ.log( px1 + "," + py1 + " - "+ px2 + "," + py2 + " - " + px3 + "," + py3 + "-" + px4 + "," + py4 );
                   //  IJ.log("iPoint: "+ roundTwoDecimals(gh.m_result.points.get(0).x) + "," + roundTwoDecimals(gh.m_result.points.get(0).y));
                   //  IJ.log("----");


                     gh.getMidPoint(px2, py2, ix, iy );

                     if (gh.m_result.NumberOfSolutions() > 0 )
                     {
//                                 int _px = this.x + Math.round(gh.m_result.points.get(0).x);
//                               int _py = this.y + Math.round(gh.m_result.points.get(0).y);
                         int _px =  Math.round(gh.m_result.points.get(0).x);
                         int _py =  Math.round(gh.m_result.points.get(0).y);

                         tl.p1x = (int)px2;
                         tl.p1y = (int)py2;

                         tl.ipx = _px;
                         tl.ipy = _py;

                         tl.calcDist();
                         return true;
                         

/*                                 if ( this.contains( _px, _py ) )
                         {
                             IJ.log("----");
                             IJ.log( px1 + "," + py1 + " - "+ px2 + "," + py2 + " - " + px3 + "," + py3 + "-" + px4 + "," + py4 );
                             IJ.log("iPoint: "+ roundTwoDecimals(ix) + "," + roundTwoDecimals(iy));
                             IJ.log("----");

                             intVec.add(tl);

                             return true;
                         }*/
                     }
                 }
             }
             return false;
        }
        
        public ArrayList BreakAtCleavage()
        {
            
            calc1Deri(2);
            calcSlopeChange();
            filterSlopePoints(2);
            
            if (_1D_In_Point == null)
                return null;
            
            if (_1D_In_Point.size() <= 0)
                return null;

            ArrayList subRois = null;
            
            if ( _1D_In_Point.size() == 1 ) //we have just one cleavage
            {
                return null;
            }
            else
            {
                double minDist = Double.MAX_VALUE;
                int minp1Idx = -1;
                int minp2Idx = -1;
                
                getBestBreakingPoints_1:
                for ( int i = 0; i < _1D_In_Point.size(); i++ )
                {
                    _1DPoint dp = (_1DPoint)_1D_In_Point.get(i); 
                    dp.m_vBreakable.clear();
                    
                    float x1 = xp[dp.p2Idx];
                    float y1 = yp[dp.p2Idx];
                    
                    getBestBreakingPoints_2:
                    for ( int j = i+1; j < _1D_In_Point.size(); j++ )
                    {
                        if ( i == j )
                            continue getBestBreakingPoints_2;
                        
                        _1DPoint dp1 = (_1DPoint)_1D_In_Point.get(j); 
                        float x2 = xp[dp1.p2Idx];
                        float y2 = yp[dp1.p2Idx];
                        
                        if (IsBreakable(dp, dp1))
                        {
                            if (dp.m_vBreakable==null)
                                dp.m_vBreakable = new ArrayList();
                            
                            dp.m_vBreakable.add(dp1);
                            
                            double dist = Math.abs(Point2D.distance(x1,y1, x2, y2));

                            if (dist < minDist)
                            {
                                minDist = dist;
                                minp1Idx = dp.p2Idx;
                                minp2Idx = dp1.p2Idx;
                            }
                        }
                    }
                }
                
                if ( minp1Idx >=0 && minp2Idx >=0 )
                    subRois = GetSubRois(minp1Idx, minp2Idx);
            }
            return subRois;
        }
        
        public boolean IsBreakable(_1DPoint dp, _1DPoint dp1)
        {
            int _x = (int)dp.p2.getX();
            int _y = (int)dp.p2.getY();
            int x1 = (int)dp1.p2.getX();
            int y1 = (int)dp1.p2.getY();
            
            if ( dp.m_dir == 0 )
            {
                if (dp1.m_dir == 2)
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if (dp1.m_dir == 3)
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if (dp1.m_dir == 3)
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if (dp1.m_dir == 4)
                {
                    if ( x1 >= _x )
                        return true;
                }
                else if (dp1.m_dir == 5)
                {
                    if ( x1 >= _x && y1 >= _y)
                        return true;
                }
                else if (dp1.m_dir == 6)
                {
                    if ( x1 > _x && y1 >= _y)
                        return true;
                }
            }
            else if ( dp.m_dir == 1 )
            {
                if (dp1.m_dir == 3)
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if (dp1.m_dir == 4)
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if (dp1.m_dir == 5)
                {
                    if ( x1 >= _x && y1 > _y)
                        return true;
                }
                else if (dp1.m_dir == 6)
                {
                    if ( x1 >= _x && y1 >= _y)
                        return true;
                }
                else if (dp1.m_dir == 7)
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
            }
            else if ( dp.m_dir == 2 )
            {
                if  (dp1.m_dir == 0)
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if  (dp1.m_dir == 1)
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if  (dp1.m_dir == 3)
                {
                    if ( x1 >= _x && y1 >= _y)
                        return true;
                }
                else if (dp1.m_dir == 4)
                {
                    if ( x1 >= _x && y1 >= _y)
                        return true;
                }
                else if (dp1.m_dir == 5)
                {
                    if ( x1 >= _x && y1 >= _y)
                        return true;
                }
                else if (dp1.m_dir == 6)
                {
                    if ( y1 >= _y)
                        return true;
                }
                else if (dp1.m_dir == 7)
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
            }
            else if ( dp.m_dir == 3 )
            {
                if  ( dp1.m_dir == 0 )
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 1 )
                {
                    if ( x1 <= _x && y1 > _y)
                        return true;
                }
                else if  ( dp1.m_dir == 2 )
                {
                    if ( x1 >= _x && y1 >= _y)
                        return true;
                }
                else if ( dp1.m_dir == 4 )
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if ( dp1.m_dir == 5 )
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if ( dp1.m_dir == 6 )
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if ( dp1.m_dir == 7 )
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
            }
            else if ( dp.m_dir == 4 )
            {
                if  ( dp1.m_dir == 0 )
                {
                    if ( x1 <= _x )
                        return true;
                }
                else if  ( dp1.m_dir == 1 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 2 )
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if ( dp1.m_dir == 3 )
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if ( dp1.m_dir == 6 )
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
                else if ( dp1.m_dir == 7 )
                {
                    if ( x1 <= _x && y1 >= _y)
                        return true;
                }
            }
            else if ( dp.m_dir == 5 )
            {
                if  ( dp1.m_dir == 0 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 1 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 2 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 3 )
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 6 )
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 7 )
                {
                    if ( x1 <= _x )
                        return true;
                }
            }
            else if ( dp.m_dir == 5 )
            {
                if  ( dp1.m_dir == 0 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 1 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 2 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 3 )
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 6 )
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 7 )
                {
                    if ( x1 <= _x )
                        return true;
                }
            }
            else if ( dp.m_dir == 6 )
            {
                if  ( dp1.m_dir == 0 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 1 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 2 )
                {
                    if ( y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 3 )
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 4 )
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
            }
            else if ( dp.m_dir == 7 )
            {
                if  ( dp1.m_dir == 0 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                if  ( dp1.m_dir == 1 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 1 )
                {
                    if ( x1 <= _x && y1 <= _y)
                        return true;
                }
                else if  ( dp1.m_dir == 2 )
                {
                    if ( x1 >= _x &&y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 3 )
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 4 )
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
                else if ( dp1.m_dir == 5 )
                {
                    if ( x1 >= _x && y1 <= _y)
                        return true;
                }
            }         
            return false;
        }
        
        public ArrayList getCleBasedSubRois()
        {
             return BreakAtCleavage();
        }
        
        public ArrayList GetSubRois( int p1Idx, int p2Idx )
        {
            IEG_Nuclei_Counter rm = IEG_Nuclei_Counter.getInstance();
            ArrayList wRois = new ArrayList();
            IEGPolygonRoi[] subRois = new IEGPolygonRoi[2];
            
            subRois[0] = (IEGPolygonRoi)super.clone();
            subRois[1] = (IEGPolygonRoi)super.clone();
            
            int maxPoint1, maxPoint2;
            
            maxPoint1 = maxPoint2 = -1;
            
            if ( p1Idx > p2Idx )
            {
              
                maxPoint2 = p1Idx - p2Idx + 1;
                maxPoint1 = maxPoints - p1Idx + 1;
                maxPoint1 += p2Idx;
                
                if (xpf!=null) 
                {
                    subRois[0].xpf = new float[maxPoint1];
                    subRois[0].ypf = new float[maxPoint1];

                    subRois[1].xpf = new float[maxPoint2];
                    subRois[1].ypf = new float[maxPoint2];
                } 
                else 
                {
                    subRois[0].xp = new int[maxPoint1];
                    subRois[0].yp = new int[maxPoint1];

                    subRois[1].xp = new int[maxPoint2];
                    subRois[1].yp = new int[maxPoint2];
                }

                subRois[0].xp2 = new int[maxPoint1];
                subRois[0].yp2 = new int[maxPoint1];
                
                subRois[1].xp2 = new int[maxPoint2];
                subRois[1].yp2 = new int[maxPoint2];
                
                
                //for r1
                for (int i=0; i<=p2Idx; i++) 
                {
                        if (xpf!=null) 
                        {
                            subRois[0].xpf[i] = xpf[i];
                            subRois[0].ypf[i] = ypf[i];
                        } else 
                        {
                            subRois[0].xp[i] = xp[i];
                            subRois[0].yp[i] = yp[i];
                        }
                        subRois[0].xp2[i] = xp2[i];
                        subRois[0].yp2[i] = yp2[i];
                }
                
                for (int i = p1Idx, j = p2Idx + 1; i<nPoints && j < maxPoint1; i++, j++) 
                {
                        if (xpf!=null) 
                        {
                            subRois[0].xpf[j] = xpf[i];
                            subRois[0].ypf[j] = ypf[i];
                        } else 
                        {
                            subRois[0].xp[j] = xp[i];
                            subRois[0].yp[j] = yp[i];
                        }
                        subRois[0].xp2[j] = xp2[i];
                        subRois[0].yp2[j] = yp2[i];
                }
                
                //for r2
                for (int i = p2Idx, j = 0; i < p1Idx && j < maxPoint2; i++, j++) 
                {
                        if (xpf!=null) 
                        {
                            subRois[1].xpf[j] = xpf[i];
                            subRois[1].ypf[j] = ypf[i];
                        } else 
                        {
                            subRois[1].xp[j] = xp[i];
                            subRois[1].yp[j] = yp[i];
                        }
                        subRois[1].xp2[j] = xp2[i];
                        subRois[1].yp2[j] = yp2[i];
                }
                
            }
            else //p2Idx > p1Idx
            {
                maxPoint2 = p2Idx - p1Idx + 1;
                maxPoint1 = maxPoints - p2Idx + 1;
                maxPoint1 += p1Idx;
                
                if (xpf!=null) 
                {
                    subRois[0].xpf = new float[maxPoint1];
                    subRois[0].ypf = new float[maxPoint1];

                    subRois[1].xpf = new float[maxPoint2];
                    subRois[1].ypf = new float[maxPoint2];
                } 
                else 
                {
                    subRois[0].xp = new int[maxPoint1];
                    subRois[0].yp = new int[maxPoint1];

                    subRois[1].xp = new int[maxPoint2];
                    subRois[1].yp = new int[maxPoint2];
                }

                subRois[0].xp2 = new int[maxPoint1];
                subRois[0].yp2 = new int[maxPoint1];
                
                subRois[1].xp2 = new int[maxPoint2];
                subRois[1].yp2 = new int[maxPoint2];
                
                //for r1
                for (int i=0; i<=p1Idx; i++) 
                {
                        if (xpf!=null) 
                        {
                            subRois[0].xpf[i] = xpf[i];
                            subRois[0].ypf[i] = ypf[i];
                        } else 
                        {
                            subRois[0].xp[i] = xp[i];
                            subRois[0].yp[i] = yp[i];
                        }
                        subRois[0].xp2[i] = xp2[i];
                        subRois[0].yp2[i] = yp2[i];
                }
                
                for (int i=p2Idx, j = p1Idx + 1; i<nPoints && j < maxPoint1; i++, j++) 
                {
                        if (xpf!=null) 
                        {
                            subRois[0].xpf[j] = xpf[i];
                            subRois[0].ypf[j] = ypf[i];
                        } else 
                        {
                            subRois[0].xp[j] = xp[i];
                            subRois[0].yp[j] = yp[i];
                        }
                        subRois[0].xp2[j] = xp2[i];
                        subRois[0].yp2[j] = yp2[i];
                }
                
                //for r2
                for (int i = p1Idx, j = 0; i <= p2Idx && j < maxPoint2; i++, j++) 
                {
                        if (xpf!=null) 
                        {
                            subRois[1].xpf[j] = xpf[i];
                            subRois[1].ypf[j] = ypf[i];
                        } else 
                        {
                            subRois[1].xp[j] = xp[i];
                            subRois[1].yp[j] = yp[i];
                        }
                        subRois[1].xp2[j] = xp2[i];
                        subRois[1].yp2[j] = yp2[i];
                }
                
            }
            
            
            
//          subRois[0] = new PolygonRoi(subRois[0].xp, subRois[0].yp, maxPoint1, Roi.FREEROI);
            
            int stX =  IEG_Nuclei_Counter.getMinValue(subRois[0].xp, maxPoint1);
            int stY =  IEG_Nuclei_Counter.getMinValue(subRois[0].yp, maxPoint1);

            subRois[0] = new IEGPolygonRoi(subRois[0].xp, subRois[0].yp, maxPoint1, type);
            subRois[0].setName(this.getName());
            subRois[0].setLocation(stX, stY);
            subRois[0].setPosition(this.getPosition());
            wRois.add(rm.new RoiWithStat(subRois[0], this.getImage()));
            //wRois[0].m_Roi.m_bIsSubRoi = true;
            
            
            stX = IEG_Nuclei_Counter.getMinValue(subRois[1].xp, maxPoint2);
            stY = IEG_Nuclei_Counter.getMinValue(subRois[1].yp, maxPoint2);

            subRois[1] = new IEGPolygonRoi(subRois[1].xp, subRois[1].yp, maxPoint2, type);
            subRois[1].setName(this.getName());
            subRois[1].setLocation(stX, stY);
            subRois[1].setPosition(this.getPosition());
            wRois.add(rm.new RoiWithStat(subRois[1], this.getImage()));
//            wRois[1] = rm.new RoiWithStat(subRois[0], this.getImage());
  //          wRois[1].m_Roi.m_bIsSubRoi = true;
            
            //subRois[1] = new PolygonRoi(subRois[1].xp, subRois[1].yp, maxPoint2, Roi.FREEROI);
            
            return wRois;
        }
        public int[] GetXP2()
        {
            return xp2;
        }
        public int[] GetYP2()
        {
            return yp2;
        }
    

}
