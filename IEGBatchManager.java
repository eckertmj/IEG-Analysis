
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.awt.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import java.util.zip.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.plugin.Colors;
import ij.util.*;
import ij.macro.*;
import ij.measure.*;
import java.io.*;
import ij.plugin.*;
import java.util.Scanner;
import org.omg.PortableServer.THREAD_POLICY_ID;

import jxl.write.*;
import jxl.*;



/**
 *
 * @author vivek.trivedi
 */
public class IEGBatchManager extends JFrame implements ActionListener, ItemListener {
    
    
    public static final String LOC_KEY = "manager.loc";
    private Panel panel, panel1;
    static Frame instance;
    public DefaultListModel m_FolderList = new DefaultListModel();
    public JList m_lstFolder = new JList();
    public JList m_lstFolderCompleter = new JList();
    public CheckBoxJList list;
    public JScrollPane scroller;
    public GenericDialog gd; 
    public Button m_btnAddFolder;
    public Button m_btnRemoveFolder;
    public Button m_btnExpFolderList;
    public Button m_btnImpFolderList;
    public Button m_btnIEG_Config;
    public Button m_btnStBatch;
    public Button m_btnStopBatch;
    public Button m_btnCombineImages;
    public JLabel m_str_IEG_Config;
    public boolean m_BTU = false;
    public IEG_ColorStat m_PixelStatCalculator;

    public boolean m_PixelStat = false;
    public boolean m_IEG_BT = false;
    public Checkbox m_chkPixelStat;
    public boolean m_IEG_BlobAnalysis = true;
    public Checkbox m_chkIEG_BlobAnalysis;
    public Checkbox m_chkIEG_Scale;
    public Checkbox m_chkIEG_BT;
    public boolean m_IEG_Scale = false;
    
    public int m_bck=0;
    public boolean m_bNanoZoomer = false;
    public boolean m_bOffset = false;
    public double m_bckPer = 0.0;
    public int m_minBlueToGreen = 0;
    
    public String m_chChoice = "";
    public float m_minG = 0.0f;
    public int m_minGPix=0;
    public int m_maxGPix=0;

    public float m_minR = 0.0f;
    public int m_minRPix=0;
    public int m_maxRPix=0;
    public float m_minZLayers = 0.0f;
    public float m_vX = 1.0f;
    public float m_vY = 1.0f;
    public float m_vZ = 1.0f;
    public IEG_Analysis m_IEG_Analysis = null;//new IEG_Analysis();
//    public int[] m_imgArray = null;
    public int[][] m_imgArray = null;
    public int m_ImageWidth = 0;
    public int m_ImageHeight = 0;
    public int m_ImageDepth = 0;
    String m_FilePath = null;
    FileInfo m_FileInfo = null;
    public String m_ImageTitle = null;
    int m_PeakGreen=0, m_PeakRed=0;

    
    
    ImagePlus imp;
    boolean m_is2D = false;

    
    // void setGUIInput(int bck, String chchoice, float minG, int minGPix, float minR, int minRPix, float minZLayers)
    
    
    
    public String[] ChannelOption={"Both","Green", "Red"};
    
    static void openDirectory( String path) {
            if (path==null) return;
            
            File f = new File(path);
            if (!(path.endsWith(File.separator)||path.endsWith("/")))
                    path += File.separator;
            
            String[] names = f.list();
            FolderOpener fo = new FolderOpener();
            names = fo.trimFileList(names);
            if (names==null)
                    return;
            String options  = " sort";
            IJ.run("Image Sequence...", "open=[" + path + "]"+options);
            DirectoryChooser.setDefaultDirectory(path);
            IJ.register(IEGBatchManager.class);
            
    }
    
	/** Removes names that start with "." or end with ".db", ".txt", ".lut", "roi", ".pty", ".hdr", ".py", etc. */
	public String[] trimFileList(String[] rawlist) {
		int count = 0;
		for (int i=0; i< rawlist.length; i++) {
			String name = rawlist[i];
			if (name.startsWith(".")||name.equals("Thumbs.db")||!IsValidFileType(name))
				rawlist[i] = null;
			else
				count++;
		}
		if (count==0) return null;
		String[] list = rawlist;
		if (count<rawlist.length) {
			list = new String[count];
			int index = 0;
			for (int i=0; i< rawlist.length; i++) {
				if (rawlist[i]!=null)
					list[index++] = rawlist[i];
			}
		}
		return list;
	}
	
	/* Returns true if 'name' ends with ".txt", ".lut", ".roi", ".pty", ".hdr", ".java", ".ijm", ".py", ".js" or ".bsh. */
	public static boolean IsValidFileType(String name) {
		if (name==null) return true;

                if (name.endsWith(".tif") || name.endsWith(".bmp"))
                        return true;
		
                return false;
	}
        
    static public void deleteArtifact(ImagePlus impA)
    {
        int w = impA.getWidth();
        int h = impA.getHeight();
        ColorProcessor cp = (ColorProcessor)impA.getProcessor();
        IJ.log("Deleting Artifact with 0,255,255 for Ms.Xie's images");
        
        for ( int i =0; i < h ;i++)
        {
            for ( int j = 0; j < w ; j++)
            {
                int[] cl = impA.getPixel(j, i);
                
                if (cl!=null)
                {
                    if (cl[1]==255 && cl[2]==255)
                    {
                        cp.set(j, i, 0);
                    }
                }
            }            
        }
    }
    
    public boolean readImageRoi(String Path, String ImageName)
    {
        File fDir = new File(Path);
        
        try
        {
            if (fDir.isDirectory())
            {
                RoiExt roiE = new RoiExt(null, "roi");
                File[] roiFiles = fDir.listFiles(roiE);

                for (File file : roiFiles) 
                {
                    String fName = file.getName();
                    
                    String fName1 = fName.substring(0,fName.length()-4);
                    
                    if (ImageName.contains(fName1))
                    {
						IJ.log("Loading ROI from " + fName + " for the image File " + ImageName);
                        RoiReader roiRead = new RoiReader();
                        roiRead.openRoi(Path, fName);
                        return true;
                    }
                }
            }
        }
        catch(Exception e)
        {
            IJ.error("ROI reading Error");
        }
        
        return false;
    }
    
    
    public boolean populateImageArray(String path)
    {
        IJ.log("Init Memory for Nano Zoomer Images...");
        
        if (path==null) return false;
        
        m_ImageHeight=m_ImageWidth=m_ImageDepth = 0;
        m_FileInfo=null;
        m_FilePath=null;
        m_ImageTitle=null;
        m_imgArray=null;
        System.gc();
        System.gc();
        IEGBatchManager.ThreadSleep(100);
        IJ.showStatus(IJ.freeMemory());


        File f = new File(path);
        if (!(path.endsWith(File.separator)||path.endsWith("/")))
                path += File.separator;

        String[] names = f.list();
        FolderOpener fo = new FolderOpener();

        names = trimFileList(names);

        if (names==null)
        {
            IJ.log("Critical Error: Images Stack Error.");
            return false;
        }
        names = fo.sortFileList(names);

        int offset = 0;
        for ( int i = 0; i < names.length;i++ )
        {
            
            IEGBatchManager.OpenIEGFile(path + names[i]);
            ImagePlus imp = IJ.getImage();
            IEGBatchManager.deleteArtifact(imp);
            IJ.log(path + names[i]);

            int imgsize = imp.getWidth()*imp.getHeight();
            int[] pix = (int[])imp.getProcessor().getPixels();
            if (m_imgArray==null)
            {
                m_ImageWidth = imp.getWidth();
                m_ImageHeight = imp.getHeight();
//                    m_imgArray = new int[(long)((long)imgsize*names.length)];
                m_imgArray = new int[names.length][imgsize];//((long)((long)imgsize*names.length));
                m_FileInfo = imp.getOriginalFileInfo();
                m_FilePath = imp.getOriginalFileInfo().directory;
                m_ImageTitle = imp.getTitle();

            }
            else
            {
                if (imgsize!= m_ImageHeight*m_ImageWidth)
                {
                    IJ.log("Critical Error: Stack Images are not of same size...");
                    return false;
                }
            }
//                System.arraycopy(pix,0,m_imgArray,offset,imgsize);
            System.arraycopy(pix,0,m_imgArray[i],0,imgsize);
            /*for ( int idx = offset, idx1=0; idx < (offset+imgsize); idx++, idx1++)
            {
                m_imgArray.set(idx, pix[idx1]);
            }*/
            m_ImageDepth++;
            offset = offset + imgsize;

            double imgScale = 0.25;
            String sStr = "x=" +Double.toString(imgScale)+ " y=" + Double.toString(imgScale) + " width=1488 height=68 interpolation=Bicubic average create title=";
                
            if (m_IEG_Scale)
            {
//                IJ.run("Scale...", "x=0.25 y=0.25 width=1488 height=68 interpolation=Bicubic average create title=" + m_ImageTitle + "-1.bmp");
                IJ.run("Scale...", sStr + m_ImageTitle + "-1.bmp");
                IJ.saveAs("Tiff", m_FilePath + m_ImageTitle + "_scale_" + Double.toString(imgScale) + "_.tif");
  //              IJ.saveAs("Tiff", "C:\\Users\\vivek.trivedi\\Desktop\\a\\"+ m_ImageTitle +"_scale_" + Double.toString(imgScale) + "_.tif");
                WindowManager.getCurrentImage().close();
            }
            
                imp.close();
                System.gc();
                IJ.showStatus(IJ.freeMemory());
                IEGBatchManager.ThreadSleep(100);
        }
            
        return true;
            
    }
    
    int getArrayLength(Object[] imgArray)
    {
        int size=0;
        
         for ( int i = 0; i < imgArray.length;i++ )
         {
             if (imgArray[i]!=null)
                 size++;
             else
                 break;
         }
        
        return size;
    }
    
    public boolean populateImageArray()
    {
        if(imp==null)
            return false;
        
        if (m_IEG_BT)
        {
            Bleed_Th bt = new Bleed_Th();
            bt.setup(null, imp);
            bt.run(imp.getProcessor());
            imp.updateAndDraw();
        }
        
        m_ImageDepth=0;
        int offset = 0;
        int imgsize = imp.getWidth()*imp.getHeight();
        int imgStackSize = imp.getStackSize();
        m_imgArray=null;
        Object[] imgArray = imp.getStack().getImageArray();
            
        for ( int i = 0; i < imgStackSize;i++ )
        {

            int[] pix = (int[])imgArray[i];
            
            if (m_imgArray==null)
            {
                m_ImageWidth = imp.getWidth();
                m_ImageHeight = imp.getHeight();
//                    m_imgArray = new int[(long)((long)imgsize*names.length)];
                m_imgArray = new int[imgStackSize][imgsize];//((long)((long)imgsize*names.length));
                m_FileInfo = imp.getOriginalFileInfo();
                m_FilePath = imp.getOriginalFileInfo().directory;
                m_ImageTitle = imp.getTitle();

            }
            else
            {
                if ( imgsize != m_ImageHeight * m_ImageWidth )
                {
                    IJ.log("Critical Error: Stack Images are not of same size...");
                    return false;
                }
            }
//                System.arraycopy(pix,0,m_imgArray,offset,imgsize);
            System.arraycopy(pix,0,m_imgArray[i],0,imgsize);
            /*for ( int idx = offset, idx1=0; idx < (offset+imgsize); idx++, idx1++)
            {
                m_imgArray.set(idx, pix[idx1]);
            }*/
            m_ImageDepth++;
            offset = offset + imgsize;
            
            double imgScale = 0.25;
            String sStr = "x=" +Double.toString(imgScale)+ " y=" + Double.toString(imgScale) + " width=1488 height=68 interpolation=Bicubic average create title=";
            
            if (m_IEG_Scale)
            {
//                IJ.run("Scale...", "x=0.25 y=0.25 width=1488 height=68 interpolation=Bicubic average create title=" + m_ImageTitle + "-1.bmp");
                IJ.run("Scale...", sStr + m_ImageTitle + "-1.bmp");
                IJ.saveAs("Tiff", m_FilePath + m_ImageTitle + "_scale_" + Double.toString(imgScale) + "_.tif");
  //              IJ.saveAs("Tiff", "C:\\Users\\vivek.trivedi\\Desktop\\a\\"+ m_ImageTitle +"_scale_" + Double.toString(imgScale) + "_.tif");
                WindowManager.getCurrentImage().close();
            }
            
            if (m_IEG_BT || m_IEG_Scale)
                IJ.saveAs("Tiff", m_FilePath + m_ImageTitle + "_BT_.tif");            
            
            IEGBatchManager.ThreadSleep(1000);
            imp.close();
            imp=null;
            System.gc();
            IJ.showStatus(IJ.freeMemory());
            IEGBatchManager.ThreadSleep(100);
        }
        
        return true;
    }    
    
    public class IEGImageCombiner extends Thread
    {
        public IEGBatchManager m_Parent;

        public IEGImageCombiner(IEGBatchManager parent) 
        {
            m_Parent = parent;
        }

        public void run()
        {
            int[] idx = new int[m_FolderList.getSize()];
            String[] strFileName = new String[m_FolderList.getSize()];
            boolean[] filetype = new boolean[m_FolderList.getSize()];
            
            int imgSize = 0;
            int imgWidth=0;
           int imgHeight = 0;
            ImagePlus impNew=null;
            ImageProcessor cp = null;;
            
            String strPath = JOptionPane.showInputDialog(null,"File path :", "Path", JOptionPane.QUESTION_MESSAGE );
            if (strPath == null)
                    return;

            
            String preFixF = JOptionPane.showInputDialog(null,"File Prefix :", "Path", JOptionPane.QUESTION_MESSAGE );
            if (preFixF == null)
                    return;

            String postFixF = JOptionPane.showInputDialog(null,"File Postfix :", "Path", JOptionPane.QUESTION_MESSAGE );
            if (postFixF == null)
                    return;
            
            
            String EndNum = JOptionPane.showInputDialog(null,"Enter the Last Range :", "Last Range No", JOptionPane.QUESTION_MESSAGE );
            if (EndNum == null)
                    return;
            
            int endNumSlide = Integer.parseInt(EndNum);
            
            String Incr = JOptionPane.showInputDialog(null,"Enter Incr :", "Incr", JOptionPane.QUESTION_MESSAGE );
            if (Incr == null)
                    return;
            
            int IncrSlide = Integer.parseInt(Incr);
            
            String[] fileNames = new String[(endNumSlide/IncrSlide)+1];
            
            for ( int idx1 = 0, fx=0; fx < (endNumSlide/IncrSlide)+1; idx1 += IncrSlide,fx++ )
            {
                fileNames[fx] = strPath + preFixF + Integer.toString(idx1) + postFixF;
            }
            

            for ( int ii = 0; ii < fileNames.length;ii++)
            {
//                if (!f.isDirectory() )
                {
                    IEGBatchManager.OpenIEGFile(fileNames[ii]);
                    imp = WindowManager.getCurrentImage();
                    
                    if (imgSize == 0)
                    {
                        imgWidth = imp.getWidth();
                        imgHeight = imp.getHeight();
                        imgSize = imp.getWidth() * imp.getHeight();
                        
                        impNew = NewImage.createRGBImage("CombinedImage", imgWidth, imgHeight*fileNames.length, 1, 7);
                        cp = impNew.getProcessor();
                        
                    }
                    ImageProcessor ip1 = imp.getProcessor();
                    
                    cp.insert(ip1, 0, ii*imgHeight);
                    
                    imp.close();
                    
                    
                }
                
            }
            
            if(impNew!=null)
            {
                impNew.changes=true;
                IJ.saveAs(impNew, "Tiff", strPath + preFixF + "combined.tif");
            }
            
        }
    }
    
     public class IEGBatchExecuter extends Thread
     {

        public IEGBatchManager m_Parent;

        public IEGBatchExecuter(IEGBatchManager parent) 
        {
            m_Parent = parent;
        }
         
         public void run ()
         {
                    
             try
             {
                  /* File[] FolderAndFilesPath = m_Parent.getSelectedFilesAndFolders( true, " " );
                   if (FolderAndFilesPath !=null && FolderAndFilesPath.length > 0)
                   {
                       
                   }*/


                   
                    WritableWorkbook xlWb = null;
                    
                    WritableSheet xlWs_h = null;
                    WritableSheet xlWs_a = null;
                    int iRowH=0;
                    int iRowA=0;
                    
                    int iRowPStat=0;
                    
                    WritableWorkbook xlWb_PStat = null;
                    WritableSheet xlWs_PStat = null;
                    
                    
                    
             
                    //Win32Thread tt= new Win32Thread();
                    int yOffset=0;
                    
                    if (m_IEG_BlobAnalysis)
                    {
                        if (gd==null)
                        {
                            if (!createGUI())
                            {
                                return;
                            }
                        }
                        /*String strPath = JOptionPane.showInputDialog(null,
                                "Summary File Name and path(eg. c:\\vivek\\batch1.xls :", 
                                "Batch Summary File", JOptionPane.QUESTION_MESSAGE );*/
                        String strPath = JOptionPane.showInputDialog(null,"Summary File Name","c:\\bin\\foo.xls");
                        if (strPath == null)
                            return;
                        
                        xlWb = Workbook.createWorkbook(new File(strPath));

                        xlWs_h = xlWb.createSheet("Batch_Avrage_Homer", 0);
                        xlWs_a = xlWb.createSheet("Batch_Avrage_Arc", 0);
	                	m_Parent.AddWBTitle(xlWs_h);
    	                m_Parent.AddWBTitle(xlWs_a);

                        iRowH=0;
                        iRowA=0;
                        
                    }
                    if (m_PixelStat)
                    {
                        String strPath = JOptionPane.showInputDialog(null,
                                "Summary File Name and path(eg. c:\\vivek\\batch1.xls :", 
                                "Batch Pixel Stat Summary File", JOptionPane.QUESTION_MESSAGE );
                        if (strPath == null)
                            return;
                        
                        xlWb_PStat = Workbook.createWorkbook(new File(strPath));

                        xlWs_PStat = xlWb_PStat.createSheet("Pixel Stat", 0);

                        m_Parent.AddWBTitlePixStat(xlWs_PStat);
    	                m_Parent.AddWBTitle(xlWs_a);
                        
                    }
                    
                    
                    if (m_PixelStat)
                    {
                        m_PixelStatCalculator = new IEG_ColorStat();
                        m_PixelStatCalculator.SetColorRange();
                    }
                    

                    int[] idx = new int[m_FolderList.getSize()];
                    String[] strFileName = new String[m_FolderList.getSize()];
                    boolean[] filetype = new boolean[m_FolderList.getSize()];
                    
                    for ( int ii = 0; ii < m_FolderList.getSize();ii++)
                    {
                        idx[ii]=-1;
                        strFileName[ii] = (String)m_FolderList.get(ii);
                        String folderPath = (String)m_FolderList.get(ii);
                        File f = new File(folderPath);
                        if (!f.isDirectory() )
                        {
                            filetype[ii] = false;
                        }
                        else
                        {
                            filetype[ii] = true;
                        }
                    }
                    
               //     m_FolderList.removeAllElements();
                    //System.gc();
                    //ThreadSleep(1000);    
                    

                    IJ.log("IEG_Batch Processing:");
                    int fLength = strFileName.length;
                    String fnameim = "";
                    ImagePlus imp = null;
                    
                    String fMem1 = "";
                    String fMem2 = "";
                    m_is2D=true;
                    
                    
                    for ( int i = 0; i < fLength; i++)
                    {

                       String folderPath = strFileName[i];
                        if (filetype[i])
                        {
                            if (!m_bNanoZoomer)
                            {
                                // Mike 2006, change to open image without displaying

                                //IEGBatchManager.openDirectory(folderPath);
                                File pathtmp = new File(folderPath);
                                FilenameFilter filt = new myFileFilter();
                                String flist[] = pathtmp.list(filt);
                                if(flist.length == 1) {
                                    fnameim = folderPath + pathtmp.separator + flist[0];
                                    imp = IJ.openImage(fnameim);
                                }
                            }
                            else
                            {
                                IEGBatchManager.openDirectory(folderPath);
                                fMem1 = IJ.freeMemory();

                                populateImageArray(folderPath);
                                
                                System.gc();
                                
                                fMem2 = IJ.freeMemory();
                          //      IEGBatchManager.openDirectory(folderPath);

                            }
                            m_is2D = false;
                            if (m_IEG_BT)
                                m_BTU = true;
                            
                        }
                        else
                        {
                            IEGBatchManager.OpenIEGFile(folderPath);
                            imp = WindowManager.getCurrentImage();
                            
                            if( imp.getStackSize() > 1 )
                            {
                                m_is2D = false;
                            }
                            else
                            {
                                m_is2D = true;
                            }
                            
                            if (m_bNanoZoomer || m_IEG_Scale || m_IEG_BT)
                            {
                                populateImageArray();
                                m_BTU = false;
                            }
                            
                            if (m_is2D && m_bOffset)
                            {
                                int lIdxDot = folderPath.lastIndexOf(".");
                                String fName = folderPath.substring(0, lIdxDot);
                                int lIdxDash = fName.lastIndexOf("_");
                                String fName1 = fName.substring(lIdxDash+1, fName.length());
                                yOffset = Integer.parseInt(fName1);
                            }
                        }
                        


                        IJ.log("Processing batch :" + folderPath);
                        if(imp==null) {
                            IJ.log("\n\nNo valid image in: " + folderPath);
                            return;
                        }
                        if (m_BTU)
                        {
                            Bleed_Th bt = new Bleed_Th(false);
                            IJ.log("Bleed Through...");
                            if (!m_bNanoZoomer)
                            {
                                //imp = IJ.getImage();

                                bt.setup(null, imp);
                                bt.run(imp.getProcessor());
                            }
                            else
                            {
                                bt.m_bSaveMemory = m_bNanoZoomer;
                                bt.m_imgPix = m_imgArray;
                                bt.m_imgHeight = m_ImageHeight;
                                bt.m_imgWidth = m_ImageWidth;
                                bt.m_imgStackSize = m_ImageDepth;
                                
                                bt.setup(null, null);
                                bt.run(null);
                            }
                            bt = null;
                        }
                        else
                            IJ.log("Without Bleed Through...");

                        if (m_PixelStat)
                        {
                            String fPath = "";
                            if (m_is2D)
                               fPath =  folderPath + "_ColorStat.csv";
                            else
                                fPath =  folderPath + "\\ColorStat.csv";
                            
                            
                            String absFolderPath = imp.getOriginalFileInfo().directory;
                            WritableSheet xlWs_PStat_stack = xlWb_PStat.createSheet(imp.getTitle(), 0);
                            m_PixelStatCalculator.ws = xlWs_PStat_stack;
                            
                            if (readImageRoi(absFolderPath, imp.getTitle()))
                                m_PixelStatCalculator.m_bRoi = true;
                            else
                                m_PixelStatCalculator.m_bRoi = false;
                            
                            try
                            {
                                if (m_bNanoZoomer)
                                {
                                    m_PixelStatCalculator.m_bSaveMemory = m_bNanoZoomer;
                                    m_PixelStatCalculator.m_imgHeight = m_ImageHeight;
                                    m_PixelStatCalculator.m_imgWidth = m_ImageWidth;
                                    m_PixelStatCalculator.m_imgStackSize = m_ImageDepth;
                                    m_PixelStatCalculator.m_imgPix = m_imgArray;
                                    m_PixelStatCalculator.setup( fPath, null);
                                    m_PixelStatCalculator.run(null);
                                    
                                }
                                else
                                {
                                    //imp = IJ.getImage();
                                    m_PixelStatCalculator.setup( fPath, imp);
                                    m_PixelStatCalculator.run(imp.getProcessor());
                                }
                                
                                
                                iRowPStat++;
                                AddToXLSheetPixStat(xlWs_PStat, fPath, iRowPStat, m_PixelStatCalculator);     
                                    
                            }
                            catch(Exception ee)
                            {
                                IJ.error("Pixel Stat Error");    
                            }
                        }
                        
                        
                        boolean bUnableToProcess = false;
                        if (m_IEG_BlobAnalysis)
                        {
                            IJ.log("\nStarting IEG_Analysis");
                            try
                            {
                                m_IEG_Analysis = new IEG_Analysis();
                                m_IEG_Analysis.setGUIInput(m_bck, m_bckPer, m_minBlueToGreen, m_chChoice, m_minG, m_minGPix, m_maxGPix, m_minR, 
                                    m_minRPix, m_maxRPix, m_minZLayers, m_vX, m_vY, m_vZ, m_is2D, m_PeakGreen, m_PeakRed, xlWb);

                                m_IEG_Analysis.m_bSaveMemory=m_bNanoZoomer;
                                m_IEG_Analysis.initialize();


                                if (m_IEG_Analysis.m_bSaveMemory)
                                {
                                    m_IEG_Analysis.m_YOffset = yOffset;
                                    m_IEG_Analysis.setImageParameters(m_ImageWidth, m_ImageHeight, m_ImageDepth,  
                                            m_ImageTitle, m_FileInfo,m_FilePath);
                                    m_IEG_Analysis.MaskPixel1=new short [m_IEG_Analysis.NbSlices][m_IEG_Analysis.Width*m_IEG_Analysis.Height];	
                                    m_IEG_Analysis.imgPixel1 = m_imgArray;
                                    m_IEG_Analysis.m_offset = m_ImageHeight*m_ImageWidth;

                                }
                                else
                                {
                                    m_imgArray=null;
                                    if (m_imgArray==null)
                                    {
                                        //imp = IJ.getImage();
                                        if (imp==null)
                                        {
                                            IJ.error("Internal Error Contact Vivek...");
                                            return;
                                        }
                                        m_ImageWidth = imp.getWidth();
                                        m_ImageHeight = imp.getHeight();
                                        m_ImageDepth = imp.getStackSize();
                                        m_ImageTitle = imp.getTitle();
                                        m_FileInfo = imp.getOriginalFileInfo();
                                        m_FilePath = imp.getOriginalFileInfo().directory;
                                        m_IEG_Analysis.setImageParameters(m_ImageWidth, m_ImageHeight, m_ImageDepth,  
                                            m_ImageTitle, m_FileInfo, m_FilePath);

                                        m_IEG_Analysis.m_offset = m_ImageHeight * m_ImageWidth;

                                        m_imgArray=new int[m_IEG_Analysis.NbSlices][m_IEG_Analysis.Width*m_IEG_Analysis.Height];
                                        m_IEG_Analysis.MaskPixel1=new short [m_IEG_Analysis.NbSlices][m_IEG_Analysis.Width*m_IEG_Analysis.Height];

                                        ImageStack st = imp.getStack();
                                        Object[] sPix = st.getImageArray();

                                        for ( int stIdx = 0; stIdx < st.getSize(); stIdx++ )
                                        {
                                            if (sPix[stIdx]!=null)
                                            {
                                                System.arraycopy( (int[])sPix[stIdx], 0, m_imgArray[stIdx], 0, m_IEG_Analysis.m_offset );
                                            }
                                        }
                                        m_IEG_Analysis.imgPixel1 = m_imgArray;
                                        m_IEG_Analysis.m_bSaveMemory = true;
                                    }
                                    else
                                        m_IEG_Analysis.setImageParameters(imp);                                
                                }
                                //imp = WindowManager.getCurrentImage();
                                
                                if (imp!=null)
                                {
                                    imp.changes = false;
                                    //imp = IJ.getImage();
                                    imp.close();
                                }
                                
                                imp=null;

                                m_IEG_Analysis.analyzeOfParentClass();  
                                
                                if (m_IEG_Analysis.m_avgHRes != null )
                                {
                                    iRowH++;
                                    m_Parent.AddToXLSheet(xlWs_h, iRowH, m_IEG_Analysis.m_avgHRes, false);
                                }
                                if (m_IEG_Analysis.m_avgARes != null )
                                {
                                    iRowA++;
                                    m_Parent.AddToXLSheet(xlWs_a, iRowA, m_IEG_Analysis.m_avgARes, false);
                                }
                            }
                            catch(OutOfMemoryError e)
                            {
                                IJ.log("Out of Memory");
                                IJ.log("Unable to process " + m_ImageTitle);
                                bUnableToProcess=true;
                            }
                            
                            if (m_bNanoZoomer)
                            {
                                for ( int mIdx = 0; mIdx < m_ImageDepth; mIdx++ )
                                {
                                    m_imgArray[mIdx] = null;
                                    System.gc(); 
                                    ThreadSleep(500);
                                 //   IJ.log(IJ.freeMemory());
                                }
                            }
                            
                            m_imgArray=null;
                            m_IEG_Analysis = null;
                            m_PixelStatCalculator=null;
                            
                            
                            System.gc();
                            ThreadSleep(1000);
                            
                        }
                        
                        if (!bUnableToProcess)
                        {
                            idx[i]=i;
                            list.setSelectedIndices(idx);
                            list.updateUI();
                        }
                        
                        
                        if (imp!=null)
                        {
                            imp.changes = false;
                            imp = IJ.getImage();
                            imp.close();
                        }
                        
                        
                        System.gc();
                        ThreadSleep(1000);
                        IJ.log("\nBatch processing finished");
                    }
                    
                   if (xlWb != null)
                   {
                       xlWb.write();
                       xlWb.close();
                   }
                   
                   if (xlWb_PStat != null)
                   {
                       xlWb_PStat.write();
                       xlWb_PStat.close();
                       
                   }
                    
                   m_Parent.m_btnStopBatch.setEnabled(false);
                   m_Parent.m_btnStBatch.setEnabled(true);
                   m_Parent.list.setEnabled(true);

                   m_Parent.m_btnAddFolder.setEnabled(true);
                   m_Parent.m_btnRemoveFolder.setEnabled(true);
                   m_Parent.m_btnExpFolderList.setEnabled(true);
                   m_Parent.m_btnImpFolderList.setEnabled(true);
                   m_Parent.m_btnIEG_Config.setEnabled(true);
                   m_Parent.m_chkIEG_BlobAnalysis.setEnabled(true);
                   m_Parent.m_chkIEG_Scale.setEnabled(false);
                   m_Parent.m_chkPixelStat.setEnabled(true);
                    
             
                }
             catch(Exception e)
             {
                 IJ.log("IEG Batch Exception:" + e.toString());
             }
         }
        }    
    
     public class IEGBatchExecuter1 extends Thread
     {

        public IEGBatchManager m_Parent;

        public IEGBatchExecuter1(IEGBatchManager parent) 
        {
            m_Parent = parent;
        }
         
         public void run ()
         {
                if (m_Parent.m_IEG_BlobAnalysis)
                {
                    if (m_Parent.gd==null)
                    {
                        if (!m_Parent.createGUI())
                        {
                           return;
                        }
                    }
                }
               
               if (m_Parent.m_PixelStat)
               {
                   m_Parent.m_PixelStatCalculator = new IEG_ColorStat();
                   m_Parent.m_PixelStatCalculator.SetColorRange();
               }
               
               int[] idx = new int[m_Parent.m_FolderList.getSize()];
               for ( int ii = 0; ii < m_Parent.m_FolderList.getSize();ii++)
                   idx[ii]=-1;
               
               Bleed_Th bt = new Bleed_Th();

               IJ.log("IEG_Batch Processing:");
                
               boolean is2D = false;

               for ( int i = 0; i < m_Parent.m_FolderList.getSize() && m_Parent.m_btnStopBatch.isEnabled();i++ )
               {                  
                   String folderPath = (String)m_Parent.m_FolderList.get(i);
                    File f = new File(folderPath);
                    if (!f.isDirectory() )
                    {
                        
                        try
                        {
                            IEG_FileOpener fOpener = new IEG_FileOpener();
                            fOpener.strFileName = f.getCanonicalPath();
                            fOpener.start();
                            fOpener.join();
                            
                            while (!fOpener.bStatus)
                                Thread.sleep(100);
                            
                            is2D = true;
                           /* if (OpenIEGFile(f.getCanonicalPath()))
                            {
                                is2D=true;
                            }
                            else
                            {
                                continue;
                            }*/
                        }
                        catch(Exception e)
                        {
                            continue;
                        }
                       /* Opener fop = new Opener();
                        try
                        {
                            fop.open(f.getCanonicalPath());
                            fop=null;
                            System.gc();
                            Thread.sleep(100);
                            System.gc();
                            Thread.sleep(100);
                            is2D = true;
                        }
                        catch(Exception ie)
                        {
                            continue;
                        }*/
                     //   continue;
                    }
                    else
                    {
                        try
                        {
                            IEGBatchManager.openDirectory(f.getCanonicalPath());
                            is2D=false;
                        }
                        catch(Exception ie)
                        {
                            continue;
                        }
                    }

                 /*  m_IEG_Analysis.setGUIInput(m_bck, m_bckPer, m_minBlueToGreen, m_chChoice, m_minG, m_minGPix, m_maxGPix, m_minR, 
                                    m_minRPix, m_maxRPix, m_minZLayers, m_vX, m_vY, m_vZ, m_is2D, m_PeakGreen, m_PeakRed, xlWb);*/
                   
                   ImagePlus imp = WindowManager.getCurrentImage();


                   IJ.log("Processing batch :" + folderPath);
                   if (m_Parent.m_BTU)
                   {
                       IJ.log("Bleed Through...");
                       bt.setup(null, imp);
                       bt.run(imp.getProcessor());
                   }
                   else
                       IJ.log("Without Bleed Through...");

                   if (m_PixelStat)
                   {
                       try
                       {
                           if (is2D)
                                m_PixelStatCalculator.setup(f.getCanonicalPath()+"_ColorStat.csv", imp);
                           else
                               m_PixelStatCalculator.setup(f.getCanonicalPath()+"\\ColorStat.csv", imp);
                           
                            m_PixelStatCalculator.run(imp.getProcessor());
                       }
                       catch(Exception e)
                       {
                           
                       }
                   }
                   
                   if (m_Parent.m_IEG_BlobAnalysis)
                   {
                       if (m_Parent.m_IEG_Analysis.setImageParameters(imp))
                       {
                            m_Parent.m_IEG_Analysis.initialize();
                            m_Parent.m_IEG_Analysis.analyzeOfParentClass();  
                            System.gc();
                            System.gc();
                            idx[i]=i;
                            m_Parent.list.setSelectedIndices(idx);
                       }
                   }
                   imp.changes=false;
                   imp.close();
                   
                   IJ.log("----------------------------------------");
         
                }
               m_Parent.m_btnStopBatch.setEnabled(false);
               m_Parent.m_btnStBatch.setEnabled(true);
               m_Parent.list.setEnabled(true);

               m_Parent.m_btnAddFolder.setEnabled(true);
               m_Parent.m_btnRemoveFolder.setEnabled(true);
               m_Parent.m_btnExpFolderList.setEnabled(true);
               m_Parent.m_btnImpFolderList.setEnabled(true);
               m_Parent.m_btnIEG_Config.setEnabled(true);
               m_Parent.m_chkIEG_BlobAnalysis.setEnabled(true);
               m_Parent.m_chkIEG_Scale.setEnabled(true);
               m_Parent.m_chkPixelStat.setEnabled(true);
         }
     }
     
     public static boolean OpenIEGFile(String path)
     {
         
         IJ.open(path);
         return true;
       /* Opener fop = new Opener();
        try
        {
            fop.open(path);
            fop=null;
            System.gc();
            Thread.sleep(100);
            System.gc();
            Thread.sleep(100);
            return true;
        }
        catch(Exception ie)
        {
            return false;
        }*/
         
     }

    

    
    
    
    public IEGBatchManager() {
        
            super("IEG Batch Manager V"+IEG_Analysis.IEG_VER); //LM, 8Jan2013);
            if (instance!=null) {
                    WindowManager.toFront(instance);
                    return;
            }
            instance = this;
//            list = new CheckBoxJList(10, true);

            showWindow();
    }
    
/*    public void paint(Graphics g)
    {
        scroller.revalidate();
        scroller.repaint();
        list.revalidate();

        super.paint(g);
        
    }*/

    
    public void showWindow() {
		ImageJ ij = IJ.getInstance();
 		addKeyListener(ij);
                setSize(800,1100);
                setLocation(800, 100);
                
                m_FolderList = new DefaultListModel();
                list = new CheckBoxJList();
                list.setSize(500, 500);
                list.setModel (m_FolderList);
                
 		//addMouseListener(this);
		//addMouseWheelListener(this);
		WindowManager.addWindow(this);
		//setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
		setLayout(new BorderLayout());
		//list.add("012345678901234");
		//list.addItemListener(this);
 		//list.addKeyListener(ij);
 		//list.addMouseListener(this);
 		//list.addMouseWheelListener(this);
                scroller = new JScrollPane (list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);                
                
                
		add("Center", scroller);
		panel = new Panel();
                

		int nButtons = 4;
		//panel.setLayout(new GridLayout(nButtons, 1, 5, 0));
                GridBagLayout gridbag = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(gridbag);
                c.fill = GridBagConstraints.BOTH;

		c.weightx = 0.0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                m_btnAddFolder =addButton( "Add Folder", gridbag, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
		m_btnRemoveFolder =addButton("Remove Folder",gridbag, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
		m_btnExpFolderList = addButton("Export Folder List",gridbag, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
		m_btnImpFolderList = addButton("Import Folder List",gridbag, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
                JLabel l = new JLabel("   ------------------------------------------------------");
                gridbag.setConstraints(l, c);
                panel.add(l);                
                c.gridwidth = GridBagConstraints.REMAINDER;
		m_btnIEG_Config = addButton("IEG_Analysis Configuration...",gridbag, c);

                c.gridwidth = GridBagConstraints.REMAINDER;
                l = new JLabel("   -----------------------------------------------------");
                gridbag.setConstraints(l, c);
                panel.add(l);                

                c.gridwidth = GridBagConstraints.REMAINDER;
		m_btnCombineImages = addButton("Combine Images",gridbag, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
		m_chkIEG_Scale = addCheckBox("IEG Scale",gridbag, c);
                m_chkIEG_Scale.setState(false);
                m_chkIEG_Scale.setEnabled(false);
                m_btnCombineImages.setEnabled(false);


		m_chkIEG_BT = addCheckBox("Bleed Through",gridbag, c);
                m_chkIEG_BT.setState(false);
                m_chkIEG_BT.setEnabled(true);
                
                
                
                c.gridwidth = GridBagConstraints.REMAINDER;
		m_btnStBatch = addButton("Start Batch",gridbag, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
		m_btnStopBatch = addButton("Stop Batch",gridbag, c);
                m_btnStopBatch.setEnabled(false);
                c.gridwidth = GridBagConstraints.REMAINDER;
		m_chkPixelStat = addCheckBox("Pixel Stats & Noise Offset...",gridbag, c);
                m_chkPixelStat.setState(false);
                c.gridwidth = GridBagConstraints.REMAINDER;
		m_chkIEG_BlobAnalysis = addCheckBox("IEG Blob Analysis",gridbag, c);

                c.gridwidth = GridBagConstraints.REMAINDER;
                l = new JLabel("   ------------------------------------------------------");
                gridbag.setConstraints(l, c);
                panel.add(l);                
                


                c.gridwidth = GridBagConstraints.REMAINDER;
                l = new JLabel(convertToMultiline("\n\n\n"));
                gridbag.setConstraints(l, c);
                panel.add(l);                

                c.gridwidth = GridBagConstraints.REMAINDER;
                m_str_IEG_Config = new JLabel(convertToMultiline("Please Set The IEG_Analysis Configuration by \n clicking the 'IEG_Analysis Configuration...' button   "));
                gridbag.setConstraints(l, c);
                panel.add(m_str_IEG_Config);                
                
                
                panel1 = new Panel();
                panel1.setLayout(new FlowLayout());
                panel1.add(panel);
		add("East", panel1);		
                
                scroller.revalidate();
                scroller.repaint();
                list.revalidate();
                try
                {
                    Thread.sleep(10);
                }
                catch(Exception e)
                {
                    int i=0;
                }
	}

	Button addButton( String label, GridBagLayout gridbag, GridBagConstraints c) 
        {
		Button b = new Button(label);
		b.addActionListener(this);
                gridbag.setConstraints(b, c);
		panel.add(b);
                return b;
	}
        
        Checkbox addCheckBox( String label, GridBagLayout gridbag, GridBagConstraints c) 
        {
		Checkbox b = new Checkbox(label);
		b.addItemListener(this);
                b.setState(true);
                gridbag.setConstraints(b, c);
		panel.add(b);
                return b;
            
        }
        
        public void  AddWBTitlePixStat(WritableSheet xlWs)
        {
            try
            {
                jxl.write.Label label = new jxl.write.Label(0, 0, "Image Title");
                xlWs.addCell(label);          

                label = new jxl.write.Label(1, 0, "Total Red Pixels");
                xlWs.addCell(label);
                label = new jxl.write.Label(2, 0, "Total Green Pixels");
                xlWs.addCell(label);
                label = new jxl.write.Label(3, 0, "Total Blue Pixels");
                xlWs.addCell(label);
                
            }
            catch(Exception e)
            {
                
            }
        }
        
        
        public void  AddWBTitle(WritableSheet xlWs)
        {
            try
            {
                jxl.write.Label label = new jxl.write.Label(0, 0, "Image Title");
                xlWs.addCell(label);          

                label = new jxl.write.Label(1, 0, "Foci No");
                xlWs.addCell(label);
                label = new jxl.write.Label(2, 0, "Core X");
                xlWs.addCell(label);
                label = new jxl.write.Label(3, 0, "Core Y");
                xlWs.addCell(label);
                label = new jxl.write.Label(4, 0, "Core Z");
                xlWs.addCell(label);
                label = new jxl.write.Label(5, 0, "Volume");
                xlWs.addCell(label);
                label = new jxl.write.Label(6, 0, "Area XY");
                xlWs.addCell(label);
                label = new jxl.write.Label(7, 0, "Area XZ");
                xlWs.addCell(label);
                label = new jxl.write.Label(8, 0, "Area YZ");
                xlWs.addCell(label);
                label = new jxl.write.Label(9, 0, "Intensity");
                xlWs.addCell(label);
                label = new jxl.write.Label(10, 0, "Background");
                xlWs.addCell(label);
                label = new jxl.write.Label(11, 0, "MinI");
                xlWs.addCell(label);
                label = new jxl.write.Label(12, 0, "MaxI");
                xlWs.addCell(label);
                label = new jxl.write.Label(13, 0, "Range");
                xlWs.addCell(label);
                label = new jxl.write.Label(14, 0, "Intensity Integral");
                xlWs.addCell(label);
                label = new jxl.write.Label(15, 0, "Saturated Pixels");
                xlWs.addCell(label);
                label = new jxl.write.Label(16, 0, "(%)Saturation");
                xlWs.addCell(label);
                
            }
            catch(Exception e)
            {
                
            }
        }
        
         boolean createGUI()
         { 

    //        gd=new GenericDialog("3D Foci Picker");
            gd=new GenericDialog("IEG Analysis");

            gd.addNumericField("Minimum Blue :", 60,0);
            gd.addNumericField("Minimum Blue % in a Blob:", 40, 0);
            
            gd.addNumericField("Minimum (Blue - Green) :", 250, 0);
            

            gd.addChoice("Process Channel",ChannelOption, "Both");
            gd.addNumericField("Minimum Green : ", 40, 0);
            gd.addNumericField("Minimum Peak Green : ", 60,0);
            gd.addNumericField("Minimum Green Blob Volume (pixels):", 8, 0);
            gd.addNumericField("Maximum Green Blob Volume (pixels):", 300, 0);

            
            gd.addNumericField("Minimum Red", 40,0);
            gd.addNumericField("Minimum Peak Red : ", 60,0);
            gd.addNumericField("Minimum Red Blob Volume (pixels)", 8, 0);
            gd.addNumericField("Maximum Red Blob Volume (pixels)", 300, 0);
            
            gd.addNumericField("Minimum Z Layers required for blob", 1, 0);
                
            gd.addMessage("Scale of image: pixels per um");
            gd.addNumericField("    VoxelX (pixels): X", 1,3);
            gd.addNumericField("    VoxelY (pixels): Y", 1,3);
            gd.addNumericField("    VoxelZ (pixels): Z", 1,3);
            
     //       gd.addCheckbox("Bleed Through", true);

            gd.addCheckbox("Nano Zoomer Images", false);
            gd.addCheckbox("Add Offset", false);
       //     gd.addCheckbox("Pixel Stats...", true);
            
            gd.showDialog();
            
            if (gd.wasCanceled())
            { 
                return false;
            }
            else
            {
                m_bck=(int)gd.getNextNumber();
                m_bckPer=(double)gd.getNextNumber();
                if (m_bckPer>100 || m_bckPer<0)
                    m_bckPer=100.0;

                m_minBlueToGreen = (int)gd.getNextNumber();
                
                m_chChoice = gd.getNextChoice();
                
                m_minG = (float)gd.getNextNumber();
                m_PeakGreen = (int)gd.getNextNumber();
                m_minGPix = (int)gd.getNextNumber();
                m_maxGPix = (int)gd.getNextNumber();
                
                m_minR = (float)gd.getNextNumber();
                m_PeakRed = (int)gd.getNextNumber();
                m_minRPix = (int)gd.getNextNumber();
                m_maxRPix = (int)gd.getNextNumber();
                
                
                m_minZLayers = (int)gd.getNextNumber();
                m_vX = (float)gd.getNextNumber();
                m_vY = (float)gd.getNextNumber();
                m_vZ = (float)gd.getNextNumber();
         //       m_BTU = gd.getNextBoolean();
                m_bNanoZoomer = gd.getNextBoolean();
                m_bOffset = gd.getNextBoolean();
                
                String configStr= "Configuration : \n --------------------\nMinimum Blue :" + m_bck + 
                        "\n Minimum Blue % in a Blob :" + m_bckPer +
                        "\nProcess Channel :" + m_chChoice + 
                        "\nMinimum Green : " + m_minG + "\nMinimum Green Blob Volume (pixels): " + m_minGPix +
                        "\nMinimum Red : " + m_minR + "\nMinimum Red Blob Volume (pixels): " + m_minRPix +
                        "\nMinimum Z layers : " + m_minZLayers + "\n(vX-vY-vZ) : " + m_vX + "-" + m_vY + "-" + m_vZ +
                        "\nBleed Through :" + (m_BTU?"True":"False") + 
                        "\nPexel Stat :" + (m_PixelStat?"True":"False");
                m_str_IEG_Config.setText(convertToMultiline(configStr));
                
                return true;
            }

            

        }
         
        public static String convertToMultiline(String orig)
        {
            return "<html>" + orig.replaceAll("\n", "<br>");
        }
 
        @Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if (source==m_chkPixelStat) {
                     m_PixelStat = m_chkPixelStat.getState();
                }
		if (source==m_chkIEG_BlobAnalysis) {
                     m_IEG_BlobAnalysis = m_chkIEG_BlobAnalysis.getState();
                }
		if (source==m_chkIEG_Scale) {
                     m_IEG_Scale = m_chkIEG_Scale.getState();
                }
		if (source==m_chkIEG_BT) {
                     m_IEG_BT = m_chkIEG_BT.getState();
                }
                
        }
        
        public void CombineImages()
        {
            
        }
        
        @Override
	public void actionPerformed(ActionEvent e) {
		String label = e.getActionCommand();
		if (label==null)
			return;
		String command = label;
		if (command.equals("Add Folder"))
                {
                    File[] FolderAndFilesPath = getSelectedFilesAndFolders(false, "Select Folders/Files");
                    if (FolderAndFilesPath !=null && FolderAndFilesPath.length > 0)
                    {
                        for ( int i = 0; i < FolderAndFilesPath.length; i++)
                        {
                            try
                            {
                                m_FolderList.addElement(FolderAndFilesPath[i].getCanonicalPath());
                            }
                            catch(IOException ee)
                            {
                                
                            }
                        }
                    }
                }
			//runCommand("add");
		else if (command.equals("Remove Folder"))
                {
                    m_FolderList.removeAllElements();
                    //int i = list.getSelectedIndex();
                    //if ( i >= 0)
                    //    m_FolderList.remove( i);
                }
		else if (command.equals("Combine Images"))
                {
                    IEGImageCombiner imgCombiner = new IEGImageCombiner(this);
                    imgCombiner.start();
                }
		else if (command.equals("Export Folder List"))
                {
                    exportFolderList();
                }
		else if (command.equals("Import Folder List"))
                {
                    ImportFolderList();
                }
		else if (command.equals("IEG_Analysis Configuration..."))
                {
                    createGUI();
                }
		else if (command.equals("Start Batch"))
                {
                   m_btnStopBatch.setEnabled(true);
                   
                   list.setEnabled(false);
                   m_btnStBatch.setEnabled(false);
                   m_btnAddFolder.setEnabled(false);
                   m_btnRemoveFolder.setEnabled(false);
                   m_btnExpFolderList.setEnabled(false);
                   m_btnImpFolderList.setEnabled(false);
                   m_btnIEG_Config.setEnabled(false);
                   m_chkIEG_BlobAnalysis.setEnabled(false);
                   m_chkPixelStat.setEnabled(false);
                   
                   IEGBatchExecuter IEG_B_Exe = new IEGBatchExecuter(this);
                   IEG_B_Exe.start();
                   
                    
                    
                   
                   
                   
                }
		else if (command.equals("Stop Batch"))
                {
                   m_btnStopBatch.setEnabled(false);
                   m_btnStBatch.setEnabled(true);
                   
                   m_btnAddFolder.setEnabled(true);
                   m_btnRemoveFolder.setEnabled(true);
                   m_btnExpFolderList.setEnabled(true);
                   m_btnImpFolderList.setEnabled(true);
                   m_btnIEG_Config.setEnabled(true);
                   m_chkIEG_BlobAnalysis.setEnabled(true);
                   m_chkPixelStat.setEnabled(true);
                   
                }
                
                
        }    
        
        public static void ThreadSleep(int mili)
        {
            try
            {
                Thread.sleep(mili);
            }
            catch(Exception ee)
                    {
                        
                    }
        }
        
        public File[] getSelectedFilesAndFolders(boolean bFolder, String dlgTitle){
            try
            {
                String lastPath = OpenDialog.getDefaultDirectory();
                JFileChooser chooser = new JFileChooser(); 

                if(lastPath == null)
                    lastPath = ".";

                chooser.setCurrentDirectory(new java.io.File(lastPath));
                chooser.setPreferredSize(new Dimension(500,800));

                if (bFolder)
                {
                    chooser.setDialogTitle(dlgTitle);
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setMultiSelectionEnabled(false);
                }
                else
                {
                    chooser.setDialogTitle(dlgTitle);
                    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    chooser.setMultiSelectionEnabled(true);
                }
                chooser.setAcceptAllFileFilterUsed(false);
                

               if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) 
               { 
                  return  chooser.getSelectedFiles();//.getCanonicalPath();
               }            
            }
            catch(Exception e)
            {
                return null;
            }
            
            return null;
            
        }

        
      	public static IEGBatchManager getInstance() {
		return (IEGBatchManager)instance;
	}
        
        
        class OnlyExt extends javax.swing.filechooser.FileFilter {
            public boolean accept(File file) {
                if (file.isDirectory())
                    return true;
                
                String filename = file.getName();
                return filename.endsWith(".txt");
            }
            public String getDescription() {
                return "Text Files";
            }
        }        

        private class RoiExt implements FilenameFilter {
            private String name; 

            private String extension; 

            public RoiExt(String name, String extension) {
                this.name = name;
                this.extension = extension;
            }

            public boolean accept(File directory, String filename) {
                boolean fileOK = true;

                if (name != null) {
                    fileOK &= filename.startsWith(name);
                }

                if (extension != null) {
                    fileOK &= filename.endsWith('.' + extension);
                }
                return fileOK;
            }        
        }        
        
        
        public void exportFolderList()
        {
            
            try 
            {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new OnlyExt());

                // Open file dialog.
                fileChooser.showSaveDialog(this);            
                String filePath = fileChooser.getSelectedFile().getCanonicalPath();
                if (!filePath.endsWith(".txt"))
                    filePath += ".txt";

                FileWriter fWriter = null;
                BufferedWriter writer = null;
                fWriter = new FileWriter(filePath);
                writer = new BufferedWriter(fWriter);

                for ( int i =0; i < m_FolderList.getSize();i++)
                {
                    writer.write(m_FolderList.get(i).toString());
                    writer.newLine();
                }

                writer.close();
                fWriter.close();
            }
            catch (Exception e) 
            {

            }
        }
        public void ImportFolderList()
        {
            try 
            {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new OnlyExt());

                // Open file dialog.
                fileChooser.showOpenDialog(this);            
                String filePath = fileChooser.getSelectedFile().getCanonicalPath();
                if (!filePath.endsWith(".txt"))
                    filePath += ".txt";

                FileInputStream fstream = null;
                BufferedReader reader = null;
                m_FolderList.clear();
                 fstream = new FileInputStream(filePath);
                  // Get the object of DataInputStream
                  DataInputStream in = new DataInputStream(fstream);
                  BufferedReader br = new BufferedReader(new InputStreamReader(in));
                  String strLine;
                  //Read File Line By Line
                  while ((strLine = br.readLine()) != null)  
                  {
                    // Print the content on the console
                    m_FolderList.addElement(strLine);     
                  }
            }
            catch (Exception e) 
            {

            }
        }
        
        static void AddToXLSheetPixStat(WritableSheet xlWs, String ImageTitle, int iRaw, IEG_ColorStat m_PixelStatCalculator)//, IEGAvgResults avgRes, boolean is2D)
        {
            try
            {

                jxl.write.Label label = new jxl.write.Label(0, iRaw, ImageTitle);
                xlWs.addCell(label);          


                 jxl.write.Number val = new jxl.write.Number(1, iRaw, m_PixelStatCalculator.m_Stack_RedPixWithinRange);
                 xlWs.addCell(val);
                 val = new jxl.write.Number(2, iRaw, m_PixelStatCalculator.m_Stack_GreenPixWithinRange);
                 xlWs.addCell(val);
                 val = new jxl.write.Number(3, iRaw, m_PixelStatCalculator.m_Stack_BluePixWithinRange);
                 xlWs.addCell(val);


            }
            catch(Exception e)
            {
                IJ.log("Unable to Write Pixel Stat data to the Batch Summary...");
            }

        }
        
        
        static void AddToXLSheet(WritableSheet xlWs, int iRow, IEGAvgResults avgRes, boolean is2D)
        {
            try
            {

                jxl.write.Label label = new jxl.write.Label(0, iRow, avgRes.m_imgTitle);
                xlWs.addCell(label);          


                 jxl.write.Number val = new jxl.write.Number(1, iRow, avgRes.m_totBlobs--);
                 xlWs.addCell(val);

                 val = new jxl.write.Number(2, iRow, avgRes.m_CoreX);
                 xlWs.addCell(val);
                 val = new jxl.write.Number(3, iRow, avgRes.m_CoreY);
                 xlWs.addCell(val);

                 if (!is2D)
                 {
                    val = new jxl.write.Number(4, iRow, avgRes.m_CoreZ);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(5, iRow, avgRes.Volume);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(6, iRow, avgRes.AreaXY);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(7, iRow, avgRes.AreaXZ);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(8, iRow, avgRes.AreaYZ);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(9, iRow, avgRes.Intensity); //mean intensity
                    xlWs.addCell(val);
                    val = new jxl.write.Number(10, iRow, avgRes.Background); //bck
                    xlWs.addCell(val);
                    val = new jxl.write.Number(11, iRow, avgRes.MinI); //minimum I
                    xlWs.addCell(val);
                    val = new jxl.write.Number(12, iRow, avgRes.MaxI); //maxI
                    xlWs.addCell(val);
                    val = new jxl.write.Number(13, iRow, avgRes.Range); //
                    xlWs.addCell(val);
                    val = new jxl.write.Number(14, iRow, avgRes.Intensity_Integral);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(15, iRow, avgRes.Saturated_Pixels);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(16, iRow, avgRes.per_Saturation );
                    xlWs.addCell(val);
                 }
                 else
                 {
                    val = new jxl.write.Number(4, iRow, avgRes.AreaXY);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(5, iRow, avgRes.Intensity);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(6, iRow, avgRes.Background);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(7, iRow, avgRes.MinI);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(8, iRow, avgRes.MaxI);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(9, iRow,avgRes.Range);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(10, iRow, avgRes.Intensity_Integral);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(11, iRow, avgRes.Saturated_Pixels);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(12, iRow, avgRes.per_Saturation );
                    xlWs.addCell(val);
                     
                 }

            }
            catch(Exception e)
            {
                IJ.log("Unable to Write Average data to the Batch Summary...");
            }

        }
        
}
