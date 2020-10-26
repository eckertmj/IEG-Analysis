/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author vivek.trivedi
 */

import ij.*;

import ij.ImagePlus.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import java.lang.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.frame.RoiManager;
import ij.util.*;
import java.sql.*;
import java.awt.*;
import java.util.*;
import java.util.ArrayList;
import java.awt.event.*;
import java.io.IOException;
import java.util.Vector;
import javax.swing.JFrame;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;


public class IEG_NucleiSegment_3D implements PlugIn {
    
    public ImagePlus imp;
    public ImagePlus imp_map;

    public boolean m_bInitRoiManager = false;
    public int m_roiCounter = 1;
    
    public int stThres = 30;
    public int thresStep = 1;

    //area 40x
   // public double m_minArea = 200;
   // public double m_maxArea = 1000;
   // public double m_minArea_1 = 400;

    //area 40x
    public double m_minArea = 1000;
    public double m_maxArea = 3000;
    public double m_minArea_1 = 1700;
    
    
    public double m_minCircularity = 0.75;

    public double m_minCircularity_1 = 0.70;
    public double m_minCircularity_2 = 0.85;

    public double m_minCircularity_1_sub = 0.80;
    public double m_minCircularity_2_sub = 0.85;
    
    
    public double m_minAxisAspectRatio = 1.8;
    public int m_min3DLevels = 6;
    
    public ArrayList m_vFrameWiseMinThreshValue = new ArrayList();
    IEG_Nuclei_Counter m_roiManager = new IEG_Nuclei_Counter();
    
    public class FrameParam
    {
        public int frameId = -1;
        public int minT = -1;
        public int totRois = 0;
        public int filterTypeId = -1;
        public int filterRadId = -1;
        public ArrayList m_vRoi = null;
        public ArrayList m_vRoi_big = null;
        
        public FrameParam(int ii, int i,int j, int ith,int sRoiCount, ArrayList vRoi, ArrayList vRoi_big)
        {
            frameId = ii;
            filterTypeId = i;
            filterRadId = j;
            minT = ith;
            totRois = sRoiCount;
            m_vRoi = vRoi;
            m_vRoi_big = vRoi_big;
            
        }
    }
    
    public void NLog( String msg, String lPath)
    {
        try
        {
            FileWriter m_fWriter ;
            BufferedWriter m_writer;
        
            m_fWriter = new FileWriter(lPath + imp.getTitle() +"_log.txt", true);
            m_writer = new BufferedWriter(m_fWriter);
            
            m_writer.write(msg);
            m_writer.newLine();
            
            m_writer.close();
            
        }
        catch(Exception e)
        {
            IJ.error("NLOg:" + e.toString());
        }
    }
    
    public boolean ScaleAndSave(double xscale, double yscale, double bgValue)
    {
        try
        {
            if (this.imp==null)
                return false;
            
            createNewStack(this.imp, this.imp.getProcessor(), xscale, yscale, bgValue);
            
            return true;
            
        }
        catch(Exception e)
        {
            IJ.error(e.toString());
        }
        return false;
    }
            
	void createNewStack(ImagePlus imp1, ImageProcessor ip, double xscale, double yscale, double bgValue) {
		int nSlices = this.imp.getStackSize();
		int w=imp.getWidth(), h=imp.getHeight();
                
                int newHeight = h* (int) yscale;
                int newWidth = w* (int) xscale;
                
                String newTitle = "n3d_" + imp.getTitle();
		ImagePlus imp2 = imp.createImagePlus();
		if (newWidth!=w || newHeight!=h) {
			Rectangle r = ip.getRoi();
			boolean crop = r.width!=imp.getWidth() || r.height!=imp.getHeight();
			ImageStack stack1 = imp.getStack();
			ImageStack stack2 = new ImageStack(newWidth, newHeight);
			ImageProcessor ip1, ip2;
			int method = ImageProcessor.BICUBIC;
			if (w==1 || h==1)
				method = ImageProcessor.NONE;
			for (int i=1; i<=nSlices; i++) {
				IJ.showStatus("Preprocessing: " + i + "/" + nSlices);
				ip1 = stack1.getProcessor(i);
				String label = stack1.getSliceLabel(i);
				if (crop) {
					ip1.setRoi(r);
					ip1 = ip1.crop();
				}
				ip1.setInterpolationMethod(method);
				ip2 = ip1.resize(newWidth, newHeight, false);
				if (ip2!=null)
					stack2.addSlice(label, ip2);
				IJ.showProgress(i, nSlices);
			}
			imp2.setStack(newTitle, stack2);
			Calibration cal = imp2.getCalibration();
			if (cal.scaled()) {
				cal.pixelWidth *= 1.0/xscale;
				cal.pixelHeight *= 1.0/yscale;
			}
			IJ.showProgress(1.0);
                      //  imp.close();
                        FileInfo ofi = this.imp.getOriginalFileInfo();
                        this.imp = imp2;
                        this.imp.updateAndDraw();
                        this.imp.changes = true;
                        
                        
                        String name = this.imp.getTitle();
                        String directory = ofi.directory;
			String path = directory+name;
                        
                        IJ.saveAs(this.imp, "tif", path);
		} 
	}
    
    public void run(String arg)
    {
        if (!IsValid())
             return;
        
        imp.setCalibration(null);
        imp.hide();
        IJ.log(imp.getTitle() + " -- " +Integer.toString(imp.getStackSize()));
        
        Object[] options = {"Yes, please",
                            "No, thanks"};
        
        int n = JOptionPane.showOptionDialog(null,
            "Would you like to preprocess?",
            "A Silly Question",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1]);     
        
        if (n==0)
        {
        
            if (ScaleAndSave(2.0, 2.0, 0.0))
            {
                imp.show();
                imp.hide();
            }
        }
        
        ImagePlus img = imp;
//        img.hide();
        
        IJ.log(img.getTitle() + " -- " +Integer.toString(img.getStackSize()));
        
        int stackSize = img.getStackSize();
        ImageStack stack = img.getStack();
        
        ArrayList v_FrameRoiCount = new ArrayList();
                
        String[] filterTypeStr = {"median", "mean"};
        
        int[] filterType = {4,0};
        int[] filterRad = {2,4};
        boolean bDebug = false;
        
        int imode = Wand.EIGHT_CONNECTED;
        
        
        
        try
        {
        
        ImageProcessor ip;
        for ( int ii = 1; ii <= stackSize; ii++)
        {
                img.setPosition(ii);
                v_FrameRoiCount.clear();
                ip = stack.getProcessor(ii);

                int width = ip.getWidth();
		int height = ip.getHeight();
                
                IEGPolygonRoi wRoi = null;
                
                getSubRois1:
                for ( int i = 0; i < filterType.length; i++)
                {
                    int xxx=0;
                    getSubRois2:
                    for ( int j = 0; j < filterRad.length; j++ )    
                    {
                        ip.snapshot();
                        
          /*              ImagePlus img1 = new ImagePlus( "img1", ip.crop() );
                        ImageProcessor ip1 = img1.getProcessor();
                        
                        RankFilters rFilter = new RankFilters();
                        rFilter.setup(filterTypeStr[i], img);
                        rFilter.rank(ip, filterRad[j], filterType[i]);*/
                        
                        int itr = 0;
                        boolean bMaxArea = false;

                        getSubRois3:
                        for ( int ith = 25; ith < 50; ith ++)
                        {
                            ip.snapshot();
                            
                            String pFix = Integer.toString(ii) + "_" + Integer.toString(i) + "_" +
                                    Integer.toString(j) + "_" + Integer.toString(ith);
                            NLog("Processing " + pFix, img.getOriginalFileInfo().directory );
                            
                            
                            
                            makeBinary(img, ith, 255);
                            
                            if (bDebug)
                            {
                                String iName = "c:\\ij\\vj\\dist\\d\\v_" + pFix + ".tif";
                                saveSubImage(img, iName);
                            }
                            NLog("\t 1" , img.getOriginalFileInfo().directory );
                            
                            

                            RankFilters rFilter1 = new RankFilters();
                            rFilter1.setup(filterTypeStr[i], img);
                            rFilter1.rank(ip, filterRad[j], filterType[i]);

                            if (bDebug)
                            {
                                String iName = "c:\\ij\\vj\\dist\\d\\v_filt_" + pFix + "_fw_.tif";
                                saveSubImage(img, iName);
                            }
                            NLog("\t 2" , img.getOriginalFileInfo().directory );                            
                            
                            Prefs.blackBackground = true;
                            Binary b = new Binary();
                            
                            b.setup("fill", img);
                            b.run(ip);

                            
                            if (bDebug)
                            {
                                String iName = "c:\\ij\\vj\\dist\\d\\v_filt_bin" + pFix + "_fw_.tif";
                                saveSubImage(img, iName);
                            }
                            NLog("\t 3" , img.getOriginalFileInfo().directory );                            
                            
                            
                            EDM ed = new EDM();
                            ed.setup("watershed", img);
                            ed.run(ip);
                            NLog("\t 4" , img.getOriginalFileInfo().directory );                            
                            
                            img.updateAndDraw();
                            
                            if (bDebug)
                            {
                                String iName = "c:\\ij\\vj\\dist\\d\\v_" + pFix + "_fw_.tif";
                                saveSubImage(img, iName);
                            }
                            
                            IJ.showStatus("Processing " + pFix );
                            
                            byte[] pixels = null;
                            pixels = (byte[])ip.getPixels();
                            
                            double value=0;
                            int offset = 0;
                            
                            int sRoiCount=0;
                            
                            ArrayList v_subRois = new ArrayList();
                            ArrayList v_subRois_big = new ArrayList();

                            getSubRois4:
                            for (int y = 0; y < height; y++) 
                            {
                                    offset = y * width;

                                    getSubRois5:
//                                    for (int x=r.x; x<(r.x+r.width); x++) 
                                    for (int x = 0; x < width; x++) 
                                    {
                                            if ( pixels != null )
                                                    value = pixels[ offset + x ] & 255;
                                            
                                            if ( value == 0 )
                                                continue getSubRois5;
                                            
//                                            if (!roi.contains(x, y))
  //                                              continue getSubRois5;
                                            
                                            if ( value >= 255 )
                                            {
                                                Wand w = new Wand( ip );
                                                w.autoOutline( x, y, 1, 255, imode );

                                                if ( w.npoints > 0 ) 
                                                {
                                                    int type = Roi.FREEROI;
                                                    int stX = IEG_Nuclei_Counter.getMinValue(w.xpoints, w.npoints);
                                                    int stY = IEG_Nuclei_Counter.getMinValue(w.ypoints, w.npoints);
                                                    
                                                    wRoi = new IEGPolygonRoi(w.xpoints, w.ypoints, w.npoints, 
                                                            type);
                                                    //wRoi.setName(roiName);
                                                    wRoi.setLocation(stX, stY);
                                                    wRoi.setPosition(ii);
                                                    String rName = getLabel(wRoi, -1);
                                                    wRoi.setName(rName);
                                                    IEG_Nuclei_Counter.RoiWithStat wRs = new IEG_Nuclei_Counter().new 
                                                            RoiWithStat(wRoi, img);
                                                    Rectangle r = wRoi.getBounds();
                                                    
                                                    if (r.x > 0 && r.y > 0 && r.x+r.width<width-1 && r.y+r.height<height-1)
                                                    {
                                                       // v_subRois.add(wRs);

                                                        if (wRs.m_RoiStat.area >= m_maxArea)
                                                        {
                                                            v_subRois_big.add(wRs);

                                                        }
                                                        else if ( wRs.m_RoiStat.area >= m_minArea && 
                                                                wRs.m_RoiStat.area <= m_maxArea  )
                                                        {
                                                            if (wRs.m_RoiStat.area >= m_minArea_1 && 
                                                                    wRs.m_Circularity >= m_minCircularity_1 )
                                                            {
                                                                    sRoiCount++;
                                                                    v_subRois.add(wRs);
                                                            }
                                                            else if (wRs.m_RoiStat.area < m_minArea_1 && 
                                                                    wRs.m_Circularity >= m_minCircularity_2 )
                                                            {
                                                                    sRoiCount++;
                                                                    v_subRois.add(wRs);
                                                            }
                                                            /*if (wRs.m_RoiStat.histogram[0] > 0 )
                                                                IJ.log( "V>> "+ wRoi.getName() + Integer.toString(wRs.m_RoiStat.histogram[0]) + "-Area:"+ Double.toString(wRs.m_RoiStat.area));*/
                                                        }
                                                    }

                                                    setRoiPixToBlack(ip, wRoi);
                                                    img.updateAndDraw();
                                                    
                                                    if (bDebug)
                                                    {
                                                        String iName = "c:\\ij\\vj\\dist\\d\\v" + Integer.toString(sRoiCount) + ".tif";
                                                       // saveSubImage(img, iName);
                                                    }
                                                    
                                                }   
                                            }
                                             
                                    }
                            }
                            NLog("\t 5" , img.getOriginalFileInfo().directory );                            

                            ip.setThreshold(ImageProcessor.NO_THRESHOLD, 255, ImageProcessor.NO_LUT_UPDATE);
                            ip.swapPixelArrays();
                            img.updateAndDraw();
                            System.gc();
                            v_FrameRoiCount.add(new FrameParam( ii, i, j, ith, sRoiCount, v_subRois, v_subRois_big));
                            NLog("\t 6" , img.getOriginalFileInfo().directory );                            
                            
                        }
                       /* ip.setThreshold(ImageProcessor.NO_THRESHOLD, 255, ImageProcessor.NO_LUT_UPDATE);
                        ip.swapPixelArrays();
                        img.updateAndDraw();
                        System.gc();*/
                        System.gc();

                    }
                    System.gc();
                }
                FrameParam fp = getBestCount(v_FrameRoiCount);
                if ( fp != null)
                {
                    BreakBigRoi(fp);
                    m_vFrameWiseMinThreshValue.add(fp);
                }
                
                System.gc();
        }
        
        PopulateRoiManager();
        
        
        int totItems = m_roiManager.getList().getItemCount();
        int indexes[] = new int[totItems];

        for ( int i = 0; i < totItems; i++)
        {
            indexes[i] = i;
        }

        String savePath = img.getOriginalFileInfo().directory + "nRois.zip";
        IJ.log(savePath);
        m_roiManager.saveMultiple(  indexes, savePath );
        
        }
        catch(Exception e)
            {
                IJ.error(e.toString());
            }
        
 /*       RoiManager roiManager = null;
        if (roiManager==null) {
                Frame frame = WindowManager.getFrame("ROI Manager");
                if (frame==null)
                        IJ.run("ROI Manager...");
                frame = WindowManager.getFrame("ROI Manager");
                if (frame==null || !(frame instanceof RoiManager))
                {
                    IJ.log("Unable to get handle of Roi Manager...");
                        return;
                }
                roiManager = (RoiManager)frame;
                int totItems = roiManager.getList().getItemCount();
                int indexes[] = new int[totItems];
                
                for ( int i = 0; i < totItems; i++)
                {
                    indexes[i] = i;
                }
                
                String savePath = img.getOriginalFileInfo().directory + "\\nRois.zip";
                IJ.log(savePath);
                roiManager.saveMultiple(  indexes, savePath);
                
        }*/
        
        
    }

    public void BreakBigRoi(FrameParam fp)
    {
        for ( int i = 0; i < fp.m_vRoi_big.size(); i++ )
        {
            IEG_Nuclei_Counter.RoiWithStat roiBig = (IEG_Nuclei_Counter.RoiWithStat) fp.m_vRoi_big.get(i);
            
            
            Rectangle r = roiBig.m_Roi.getBounds();
            int stX = r.x;
            int stY = r.y;
            
            ArrayList subRois = getSubRois(imp,imp.getProcessor(), (IEGPolygonRoi) roiBig.m_Roi, fp.minT+1);
            if (subRois!=null)
            {
                if (subRois.size() > 0 )
                    fp.m_vRoi.add(roiBig);
                
                for ( int j = 0; j < subRois.size(); j++ )
                {
                    IEG_Nuclei_Counter.RoiWithStat tRs = (IEG_Nuclei_Counter.RoiWithStat)subRois.get(j); 
                    tRs.m_Roi.setName(roiBig.m_Roi.getName() + "_sub_" + Integer.toString(j));
                    Rectangle r1 = tRs.m_Roi.getBounds();
                    tRs.m_Roi.setLocation(stX + r1.x, stY + r1.y);
                    
                    fp.m_vRoi.add(tRs);
                }
            }
        }
    }
    
    public void PopulateRoiManager()
    {
        for ( int i = 0; i < m_vFrameWiseMinThreshValue.size(); i++  )
        {
            FrameParam fp = (FrameParam)m_vFrameWiseMinThreshValue.get(i);
            
            for ( int j = 0; j < fp.m_vRoi.size(); j++ )
            {
                IEG_Nuclei_Counter.RoiWithStat wRs = (IEG_Nuclei_Counter.RoiWithStat)fp.m_vRoi.get(j);
                
                AddToRoiManager(wRs.m_Roi);
            }
            
        }
    }
    
    public boolean AddToRoiManager(PolygonRoi roi)
    {
        
        /*if (roiManager==null) {
                if (roiManager==null) {
                        Frame frame = WindowManager.getFrame("ROI Manager");
                        if (frame==null)
                                IJ.run("ROI Manager...");
                        frame = WindowManager.getFrame("ROI Manager");
                        if (frame==null || !(frame instanceof RoiManager))
                                return false;
                        
                        roiManager = (RoiManager)frame;
                }
                if (m_bInitRoiManager)
                {
                        roiManager.runCommand("reset");
                        m_bInitRoiManager=false;
                }
        }
      /*  if (imp.getStackSize()>1)
                roi.setPosition(imp.getCurrentSlice());*/
        roi.setStrokeWidth(1);
        m_roiManager.add(imp, roi, m_roiCounter++);
        
        return true;
    }

    
    
    FrameParam getBestCount(ArrayList vRois)
    {
        FrameParam retFp= null;
        int maxC = 0;
        for ( int i = 0; i < vRois.size();i++)
        {
            FrameParam fp = (FrameParam)vRois.get(i);
            if (fp.totRois > maxC)
            {
                maxC = fp.totRois;
                retFp = fp;
            }
        }
        
        return retFp;
    }

    void setRoiPixToBlack(ImageProcessor ip,  Roi roi)
    {
        int width = ip.getWidth();
        int height = ip.getHeight();

         byte[] pixels = null;
         pixels = (byte[])ip.getPixels();

         if (pixels==null)
             return;

         Rectangle r = roi.getBounds();
         int offset=0;

        makeBinary1:
        for (int y=r.y; y<(r.y+r.height); y++) 
        {
                offset = y*width;

                makeBinary2:
                for (int x=r.x; x<(r.x+r.width); x++) 
                {
                        if ( roi.contains(x, y) )
                            pixels[offset+x]=0;
                }
        }
    }
    
    int getTotalWhitePix(ImageProcessor ip,  Roi roi)
    {
        int TotalWhitePix = 0;
        int width = ip.getWidth();
        int height = ip.getHeight();

         byte[] pixels = null;
         pixels = (byte[])ip.getPixels();

         if (pixels==null)
             return 0;

         Rectangle r = roi.getBounds();
         int offset=0;

        makeBinary1:
        for (int y=0; y<height; y++) 
        {
                offset = y*width;

                makeBinary2:
                for (int x=0; x<width; x++) 
                {
                   // if (roi.contains(x, y))
                    {
                        if ( pixels[offset+x] == -1 )
                            TotalWhitePix++;
                    }
                }
        }
        return TotalWhitePix;
    }
    
    void saveSubImage(ImagePlus sImg, String name)
    {
        ImagePlus imgT = new ImagePlus("TempImg", sImg.getProcessor() );
        WindowManager.setTempCurrentImage(imgT); 
        IJ.saveAs("Tiff", name);
        imgT.close();
    }
        
    boolean makeBinary(ImagePlus img1,  int minTh, int maxTh)
    {

        ImageProcessor ip = img1.getProcessor();
        int width = ip.getWidth();
        int height = ip.getHeight();

        if (minTh < 0)
            minTh=0;

        if (maxTh > 255)
            maxTh = 255;

        ip.resetBinaryThreshold();

        int[] lut = new int[256];
        for (int i=0; i<256; i++) 
        {
            if (i>=minTh && i<=maxTh)
                lut[i] = 255;
            else 
            {
                lut[i] = 0;
            }
        }

        ip.applyTable(lut);

        return true;
    }        
    
    public boolean IsValid()
    {
        imp = WindowManager.getCurrentImage();
        
        if (imp==null)
        {
            IJ.noImage();
            return false;
        } else if (imp.getStackSize() == 1) 
        {
            IJ.error("Stack required");
            return false;
        } 
        else if (imp.getType() != ImagePlus.GRAY8 ) 
        {
            IJ.error("Gray scale image required....");
            return false;
        }
        
        m_bInitRoiManager = false;
        m_roiCounter = 1;
        return true;
    }

        public void keepOnlyRoiData(ImageProcessor ip1, IEGPolygonRoi pr)
        {
          //  Roi roiSub = ip1.getRoi();
                int width = ip1.getWidth();
		int height = ip1.getHeight();
            
                byte[] pixels = null;
                pixels = (byte[])ip1.getPixels();

                double value=0;
                int offset = 0;
                
                keepOnlyRoiData1:
//                            for (int y=r.y; y<(r.y+r.height); y++) 
                for (int y=0; y<height; y++) 
                {
                        offset = y*width;

                        keepOnlyRoiData:
//                                    for (int x=r.x; x<(r.x+r.width); x++) 
                        for (int x=0; x<width; x++) 
                        {
                            if (!pr.contains(x, y))
                                pixels[offset+x]=0;
                        }
                }
    
        }
    
    
        public java.util.ArrayList getSubRois(ImagePlus img1, ImageProcessor ip, IEGPolygonRoi roi, int stTh )
        {
            
            try
            {
                
                int SliceNo = roi.getPosition();
                img1.setSlice(roi.getPosition());

                    Rectangle r = roi.getBounds();
                    Point roiStLocation = new Point(r.x,r.y);

                    double totRoiArea = 0;


                    ip.resetRoi();
                    ip.setRoi(roi);

                    ImageProcessor ipSub = ip.crop();
                    ImagePlus imgSub = new ImagePlus("SubImage", ipSub);

                    IEGPolygonRoi pr = new IEGPolygonRoi(((IEGPolygonRoi)roi).getPolygon(), ((IEGPolygonRoi)roi).getType());
                    pr.setLocation(0,0);
                    keepOnlyRoiData(ipSub, pr);
                  //  imgSub.show();

                    java.util.ArrayList vSubRois = new java.util.ArrayList();
                    java.util.ArrayList vFinalSubRois = new java.util.ArrayList();

                    java.util.ArrayList v_small_SubRois = new java.util.ArrayList();


                    IEG_Nuclei_Counter.RoiWithStat parentRoiS = new IEG_Nuclei_Counter().new RoiWithStat(roi, imgSub);
                    String[] filterTypeStr = {"median", "mean"};
                    int[] filterType = {4,0};
                    int[] filterRad = {2,4,6};

                    int width = ipSub.getWidth();
                    int height = ipSub.getHeight();

                    int imode = Wand.EIGHT_CONNECTED;

                    IEGPolygonRoi wRoi = null;   

                    boolean bDebug = false;

                    getSubRois1:
                    for ( int i = 0; i < filterType.length; i++)
                    {
                        getSubRois2:
                        for ( int j = 0; j < filterRad.length; j++ )    
                        {
                            ipSub.snapshot();
                            RankFilters rFilter = new RankFilters();
                            rFilter.setup(filterTypeStr[i], imgSub);
                            rFilter.rank(ipSub, filterRad[j], filterType[i]);
                            int itr = 0;
                            boolean bMaxArea = false;
                            getSubRois3:
                            for ( int ith = stTh; ith < 200; ith += thresStep)
                            {
                                
                                String pFix = Integer.toString(SliceNo) + "_" + Integer.toString(i) + "_"+ Integer.toString(j)+"_"+Integer.toString(ith);

                                IJ.showStatus("Processing Sub " + pFix );
                                NLog("Processing Sub " + pFix, imp.getOriginalFileInfo().directory );
                                
                                

                                makeBinary(imgSub, ith, 255);
                                if (bDebug)
                                {
                                    String iName = "c:\\ij\\vj\\dist\\d1\\v.tif";
                                    saveSubImage(imgSub, iName);
                                }
                                NLog("\t\t 1", imp.getOriginalFileInfo().directory );

                                RankFilters rFilter1 = new RankFilters();
                                rFilter1.setup(filterTypeStr[i], imgSub);
                                NLog("\t\t\t 2-1", imp.getOriginalFileInfo().directory );
                                rFilter1.rank(ipSub, filterRad[j], filterType[i]);
                                NLog("\t\t 2", imp.getOriginalFileInfo().directory );


    //                             imgSub.updateAndDraw();
    //                            if (true)
    //                                return null;
                            //    ipSub.setThreshold(ith, 255, ImageProcessor.BLACK_AND_WHITE_LUT);

                                Binary b = new Binary();

                                b.setup("fill", imgSub);
                                b.run(ipSub);
                                NLog("\t\t 3", imp.getOriginalFileInfo().directory );

                                EDM ed = new EDM();
                                ed.setup("watershed", imgSub);
                                ed.run(ipSub);
                                NLog("\t\t 4", imp.getOriginalFileInfo().directory );



                                imgSub.updateAndDraw();


                                if ( vFinalSubRois.size()>0 || v_small_SubRois.size() > 0)
                                {
                                    for ( int fIdx=0; fIdx < vFinalSubRois.size(); fIdx++ )
                                    {
                                        setRoiPixToBlack( ipSub, ((IEG_Nuclei_Counter.RoiWithStat)vFinalSubRois.get(fIdx)).m_Roi );
                                    }

                                    for ( int fIdx=0; fIdx < v_small_SubRois.size(); fIdx++ )
                                    {
                                        setRoiPixToBlack( ipSub, ((IEG_Nuclei_Counter.RoiWithStat)v_small_SubRois.get(fIdx)).m_Roi );
                                    }


                                        if (bDebug)
                                        {
                                            String iName = "c:\\ij\\vj\\dist\\d1\\v_after_final.tif";
                                            saveSubImage(imgSub, iName);
                                        }


                                    int totWPix = getTotalWhitePix(ipSub, pr);
                                    if ( totWPix < m_minArea )
                                    {
                                      /*  ipSub.setThreshold(ImageProcessor.NO_THRESHOLD, 255, ImageProcessor.NO_LUT_UPDATE);
                                        ipSub.swapPixelArrays();
                                        imgSub.updateAndDraw();
                                        if (bDebug)
                                        {
                                            String iName = "c:\\ij\\vj\\dist\\d1\\vDone.tif";
                                            saveSubImage(imgSub, iName);
                                        }

                                        ipSub.snapshot();
                                        System.gc();

                                        continue getSubRois3;*/
                                        imgSub.changes = false;    
                                        imgSub.close();
                                        return vFinalSubRois;

                                    }
                                }

                                imgSub.updateAndDraw();

                                if (bDebug)
                                {
                                    String iName = "c:\\ij\\vj\\dist\\d1\\vFilt.tif";
                                    saveSubImage(imgSub, iName);
                                }



                              //  WindowManager.setTempCurrentImage(imgSub); 
                             //   IJ.saveAs("Tiff", ); 
                //                if (true)
                 //                   return null;

    //                            if ()



                                byte[] pixels = null;
                                pixels = (byte[])ipSub.getPixels();

                                double value=0;
                                int offset = 0;
                                getSubRois4:
    //                            for (int y=r.y; y<(r.y+r.height); y++) 
                                for (int y=0; y<height; y++) 
                                {
                                        offset = y*width;

                                        getSubRois5:
    //                                    for (int x=r.x; x<(r.x+r.width); x++) 
                                        for (int x=0; x<width; x++) 
                                        {
                                                if (pixels!=null)
                                                        value = pixels[offset+x]&255;

                                                if ( value == 0 )
                                                    continue getSubRois5;

    //                                            if (!roi.contains(x, y))
      //                                              continue getSubRois5;

                                                if ( vSubRois.size() > 0 )
                                                {
                                                    getSubRois6:
                                                    for ( int idx=0 ; idx< vSubRois.size();idx++ )
                                                    {
                                                        IEG_Nuclei_Counter.RoiWithStat tRs = (IEG_Nuclei_Counter.RoiWithStat)vSubRois.get(idx);
                                                        if (tRs.m_Roi.contains(x, y))
                                                        {
                                                            value = 0;
                                                            break getSubRois6;
                                                        }
                                                    }
                                                }


                                                if ( value >= 255 )
                                                {
                                                    Wand w = new Wand(ipSub);
                                                    w.autoOutline(x, y, 1, 255, imode);

                                                    if ( w.npoints > 0 ) 
                                                    {
                                                        int type = Roi.FREEROI;
                                                        int stX = IEG_Nuclei_Counter.getMinValue(w.xpoints, w.npoints);
                                                        int stY = IEG_Nuclei_Counter.getMinValue(w.ypoints, w.npoints);

                                                        wRoi = new IEGPolygonRoi(w.xpoints, w.ypoints, w.npoints, type);
                                                     //   wRoi.setName(roiName);
                                                        wRoi.setLocation(stX, stY);
                                                        wRoi.setPosition(roi.getPosition());
                                                        IEG_Nuclei_Counter.RoiWithStat wRs = new IEG_Nuclei_Counter().new RoiWithStat(wRoi, imgSub);

                                                        if (wRs.m_RoiStat.area >= m_minArea)
                                                            bMaxArea = true;


                                                        if ( wRs.m_RoiStat.area >= m_minArea && wRs.m_RoiStat.area <= m_maxArea  )
                                                        {

                                                            if (wRs.m_RoiStat.area >= m_minArea_1 && wRs.m_Circularity >= m_minCircularity_1_sub )
                                                            {
                                                                if (wRs.m_RoiStat.histogram[0] <= 0 )
                                                                    vSubRois.add(wRs);
                                                            }
                                                            else if (wRs.m_RoiStat.area < m_minArea_1 && wRs.m_Circularity >= m_minCircularity_2_sub )
                                                            {
                                                                if (wRs.m_RoiStat.histogram[0] <= 0 )
                                                                    vSubRois.add(wRs);
                                                            }
                                                            /*if (wRs.m_RoiStat.histogram[0] > 0 )
                                                                IJ.log( "V>> "+ wRoi.getName() + Integer.toString(wRs.m_RoiStat.histogram[0]) + "-Area:"+ Double.toString(wRs.m_RoiStat.area));*/
                                                        }
                                                        else if(wRs.m_RoiStat.area < m_minArea)
                                                        {
                                                            v_small_SubRois.add(wRs);
                                                        }

                                                        setRoiPixToBlack(ipSub, wRoi);
                                                        imgSub.updateAndDraw();

                                                        if (bDebug)
                                                        {
                                                            String iName = "c:\\ij\\vj\\dist\\d1\\v" + Integer.toString(vSubRois.size()) + ".tif";
                                                            saveSubImage(imgSub, iName);
                                                        }
                                                    }   
                                                }
                                        }
                                }

                                NLog("\t\t 5", imp.getOriginalFileInfo().directory );


                                ipSub.setThreshold(ImageProcessor.NO_THRESHOLD, 255, ImageProcessor.NO_LUT_UPDATE);
                                ipSub.swapPixelArrays();
                                imgSub.updateAndDraw();
                                if (bDebug)
                                {
                                    String iName = "c:\\ij\\vj\\dist\\d1\\vDone.tif";
                                    saveSubImage(imgSub, iName);
                                }

                                ipSub.snapshot();
                                System.gc();


                                if (vSubRois.isEmpty())
                                {
                                    if (!bMaxArea)
                                        continue getSubRois2;
                                    else
                                    {
                                        bMaxArea = false;
                                        continue getSubRois3;
                                    }
                                }
                                else
                                {
                                    getSubRois6:
                                    for ( int idx=0 ; idx< vSubRois.size();idx++ )
                                    {
                                        IEG_Nuclei_Counter.RoiWithStat tRs = (IEG_Nuclei_Counter.RoiWithStat)vSubRois.get(idx);
                                     //   if (tRs.m_Circularity>=m_minCircularity)
                                        {
                                            totRoiArea += tRs.m_RoiStat.area;
                                            vFinalSubRois.add(tRs);
                                        }
                                    }
                                }
                                vSubRois.clear();
                            }            
                        }
                    }

                imgSub.changes = false;    
                imgSub.close();
                 NLog("\t\t 6", imp.getOriginalFileInfo().directory );
                
                return vFinalSubRois;
            }
            catch(Exception e)
            {
                IJ.error("Break Sub Rois >> " + e.toString());
            }
            
            return null;
        }

	String getLabel( Roi roi, int n) {
		Rectangle r = roi.getBounds();
		int xc = r.x + r.width/2;
		int yc = r.y + r.height/2;
		if (n>=0)
			{xc = yc; yc=n;}
		if (xc<0) xc = 0;
		if (yc<0) yc = 0;
		int digits = 4;
		String xs = "" + xc;
		if (xs.length()>digits) digits = xs.length();
		String ys = "" + yc;
		if (ys.length()>digits) digits = ys.length();
		if (digits==4 && imp!=null && imp.getStackSize()>=10000) digits = 5;
		xs = "000000" + xc;
		ys = "000000" + yc;
		String label = ys.substring(ys.length()-digits) + "-" + xs.substring(xs.length()-digits);
		if (imp!=null && imp.getStackSize()>1) {
			int slice = roi.getPosition();
			if (slice==0)
				slice = imp.getCurrentSlice();
			String zs = "000000" + slice;
			label = zs.substring(zs.length()-digits) + "-" + label;
			roi.setPosition(slice);
		}
		return label;
	}

        void CreateImageMap()
        {
            IJ.newImage( "3DNuclieMap", "16-bit black", imp.getWidth(), imp.getHeight(), imp.getStackSize() );
            imp_map=WindowManager.getCurrentImage();
        }

}
