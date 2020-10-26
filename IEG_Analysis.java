/*
 * 
 * This clas is based on Guanghua Du, 23.04.2009,  
 * 3D object counter based on find local maximum and grow the maxima to object method. 

-----
 v5.3 Mike Jan, 2013
 Changes to blob detection algorithm to fix problems with detection
 1. When finding maxima, do not check against blue channel, for nanozoomer images
    there is already a subtraction done to correct bleedthrough
 2. Do not use threads (force nThread to 1) when growing maxima as this causes errors in counts   
 
 Also remove printing of status update which speeds things up a lot.
----- 

-----
v5.4 Mike March, 2013
1. Blobs are output in a 16-bit image like farsight.  All blob pixels identified by
   the algorithm are included.
2. The xyz coordinates have been corrected to indicate the blob center better.
3. A new folder for storing results is created now
-----   

-----
v5.4.2 Mike Jan, 2015
1. Results are written even if no blobs are detected.
-----

-----
v5.4.3 Mike Jan, 2016
1. Fixed bug with Arc min and max value parameters not being used correctly.
-----

-----
v5.5 Mike May, 2016
1. Changed to open images in batch mode without displaying on screen
-----

-----
v5.6 Mike Jan, 2018
1. Fixed problem with min Z layers, which wasn't working at all.  Wrote new 
   function to exclude blobs based on specified minimum.
-----   

 */
import ij.*;

import ij.ImagePlus.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import java.lang.*;
import java.lang.Math.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.frame.RoiManager;
import ij.util.*;
import java.sql.*;
import java.awt.*;
import java.util.*;
import java.util.ArrayList;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.Vector;
import javax.swing.JFrame;
import jxl.write.*;
import jxl.*;



public class IEG_Analysis implements PlugIn,  Runnable 
{

    public static String IEG_VER = "5.6";  //LM, 8Jan2013

    // constant
    static final String pluginName = "IEG_Analysis";
    static int[] Neighbor={0,0,1,0,1,0,0,1,1,1,0,0,1,0,1,1,1,0,1,1,1};
    static int[] Surrounding;
    static int[] Surrounding3D={1,-1,-1,1,-1,0,1,-1,1,1,0,-1,1,0,0,1,0,1,1,1,-1,1,1,0,1,1,1,0,-1,-1,0,-1,0,0,
        -1,1,0,0,-1,0,0,1,0,1,-1,0,1,0,0,1,1,-1,-1,-1,-1,-1,0,-1,-1,1,-1,0,-1,-1,0,0,-1,0,1,-1,1,-1,-1,1,0,-1,1,1,};
    static int[] Surrounding2D = {1,-1,0,1,0,0,1,1,0,0,-1,0,0,1,0,-1,-1,0,-1,0,0,-1,1,0};

    static int N_SURROUNDING = 0;

    static final int N_NEIGHBOR=7;
    static final int N_SURROUNDING3D=26;
    static final int N_SURROUNDING2D=8;

    static final byte RIM_VALUE=1;    
    static final boolean SUCCESS=true;
    static final boolean NOSUCCESS=false;

    public static final int HOMER_COLOR_ID=1;
    public static final short ARC_COLOR_ID=2;   

    public boolean m_bSaveMemory = false;

    public ImageProcessor m_ip = null;

    public IEGAvgResults m_avgHRes = null;
    public IEGAvgResults m_avgARes = null;

    double m_minBluePer = 0.0;


    public int m_BlueToGreen = 0;
    public int m_maxRPix = 0;
    public int m_maxGPix = 0;

    public boolean m_2D = false;

    public String m_imgTitle = "";

    public int m_offset = 0;

    public int m_XOffset = 0, m_YOffset = 0, m_ZOffset = 0;

    GenericDialog gd;  

    //Vivek Red Channel
    float ToleranceSettingRed,ToleranceValueRed;  	
    int MinVolumeRed;

    float MinISetting, MinIValue;
    float ToleranceSetting,ToleranceValue;  	
    int UniformBackground;			
    float VoxelX,VoxelY,VoxelZ;              	
    int UnitX,UnitY,UnitZ;			
    int MinVolume;				
    int AutoBKGRadius;				
    float ContrastFactor;			
    float ContrastBalance;			
    float ZTolerance;				
    float FociShapeR;
    int MinPeakIntensityGreen, MinPeakIntensityRed;

    String MinIType;
    String BackgroundType;			
    String SpontExclusionChoice;
    String FociShapeChoice;
    String[] AbsoRelaOption={"AbsoluteBrightness", "RelativetoMaximum"};
    String[] BackgroundOption={"uniform","automatic","balanced"};
    String[] YesNoOption={"Yes","No"};	
    String[] ChannelOption={"Both","Green", "Red"};

    ImagePlus img;   	

    ImagePlus imgWhole;
    ImagePlus imgROI;

    int Width;		
    int Height;
    int NbSlices;
    String imgTitle;    

    boolean m_IEG_Scale = false;

    /*  int [][][] imgPixel; 	
        short [][][] MaskPixel;	
        int [][][] imgPixelRed; 	
        short [][][] MaskPixelRed;	
        int [][][] imgPixelBlue; 	*/

    short [][][] imgPixel; 	
    short [][][] MaskPixel;	
    short [][] MaskPixel1;
    int [][] imgPixel1;
    short [][][] imgPixelRed; 	
    short [][][] MaskPixelRed;	
    short [][][] imgPixelBlue; 	


    //  int [][][] MaximumPixel;

    PixelCollection3D CollectionLocalMax;	
    PixelCollection3D CollectionMax;		
    PixelCollection3D CollectionBKG;		
    PixelCollection3D CollectionObjBKG;		
    PixelCollection3D [] PixelsInObject;	
    //
    PixelCollection3D CollectionLocalMaxRed;	
    PixelCollection3D CollectionMaxRed;		
    PixelCollection3D CollectionBKGRed;		
    PixelCollection3D CollectionObjBKGRed;	
    PixelCollection3D [] PixelsInObjectRed;	
    PixelCollection3D [] mikeGlobal;	

    ArrayList<PixelCollection3D> m_vFoci;
    private IEGCellCntrMarkerVector m_vCellMarker;
    ArrayList<PixelCollection3D> m_vFociRed;
    private IEGCellCntrMarkerVector m_vCellMarkerRed;
    private boolean m_bNitro = false;

    private Vector typeVector;

    String[] rFileNames;

    private boolean[][][] markedPix; 
    private boolean[][][] markedPixRed; 

    boolean bProcessGreen = false, bProcessRed = false;

    String m_FilePath = null;
    FileInfo m_FileInfo = null;
    String m_ResultsPath = null;


    int  nMax,nLocalMax;
    short nFoci;
    int PixVal;
    int nThreads,nRunningThreads;
    int RTStartCounter,RTEndCounter;
    int nFociExcluded;
    boolean[] SearchObject;
    long startTime;
    public String m_StrConfig;

    public ImageStack m_imageStack = null;

    ResultsTable FociResultsTable;
    public boolean m_BTU = false;

    WritableWorkbook xlWb = null;

    public ImagePlus imgRes = null;
    public ImageStack stackRes = null;



    public void run(String arg) {
        //Foci3DGUI g = new Foci3DGUI(null, true);
        //g.setVisible(SUCCESS);
        //return;
        if (! setupGUI(arg)) return;

        Width=img.getWidth();
        Height=img.getHeight();
        NbSlices=img.getStackSize();
        imgTitle = img.getTitle();

        if (imgWhole!=null)
        {
            m_FileInfo = imgWhole.getOriginalFileInfo();
            m_FilePath = imgWhole.getOriginalFileInfo().directory;
        }
        else
        {
            m_FileInfo = img.getOriginalFileInfo();
            m_FilePath = img.getOriginalFileInfo().directory;
        }

        initialize();
        analyzeOfParentClass();  

        System.gc();
        System.gc();

    }

    public boolean setupGUI(String arg) {
        img = WindowManager.getCurrentImage();
        imgWhole=imgROI=null;

        if (img==null){
            IJ.noImage();
            return false;
        }
        else if (img.getStackSize() == 1) 
        {
            m_2D = true;
            Surrounding =Surrounding2D;
            N_SURROUNDING = N_SURROUNDING2D;
            //            IJ.error("Stack required");
            //            return false;
        } 
        else if (img.getStackSize() > 1) 
        {
            m_2D = false;
            Surrounding =Surrounding3D;
            N_SURROUNDING = N_SURROUNDING3D;
            //            IJ.error("Stack required");
            //            return false;
        } 
        else if (img.getType() != ImagePlus.COLOR_RGB ) 
        {
            IJ.error("RGB Image required....");
            return false;
        }

        m_ip = img.getProcessor();

        createGUI();

        if (!getGUIInput()){return false;}

        if (gd.wasCanceled()){ img.updateAndDraw(); return false;}

        if (m_BTU)
        {
            Bleed_Th bt = new Bleed_Th();
            bt.setup(null, img);
            bt.run(img.getProcessor());
        }

        if (img.getRoi()!=null)
        {
            imgWhole = img;
            img = ExtractRoiImage(img);
        }

        IJ.register(IEG_Analysis.class); 

        return true;
    }

    void createGUI()
    { 

        //        gd=new GenericDialog("3D Foci Picker");
        gd=new GenericDialog("IEG Analysis V"+IEG_Analysis.IEG_VER); //LM, 8Jan2013);

        gd.addNumericField("Minimum Blue :", 60,0);
        gd.addNumericField("Minimum Blue % in a Blob:", 100, 0);
        gd.addNumericField("Minimum (Blue - Green) :", 0, 0);

        gd.addChoice("Process Channel",ChannelOption, "Green");

        gd.addNumericField("Minimum Green : ", 50,0);
        gd.addNumericField("Minimum Peak Green : ", 50,0);
        gd.addNumericField("Minimum Green Blob Volume (pixels):", 20,0);
        gd.addNumericField("Maximum Green Blob Volume (pixels):", 100, 0);

        gd.addNumericField("Minimum Red", 50,0);
        gd.addNumericField("Minimum Peak Red", 50,0);
        gd.addNumericField("Minimum Red Blob Volume (pixels)", 20,0);
        gd.addNumericField("Maximum Red Blob Volume (pixels):", 100, 0);

        if (!m_2D)
            gd.addNumericField("Minimum Z Layers required for blob", 5, 0);

        gd.addMessage("Scale of image: pixels per um");
        gd.addNumericField("    VoxelX (pixels): X", 1,3);
        gd.addNumericField("    VoxelY (pixels): Y", 1,3);

        if (!m_2D)
            gd.addNumericField("    VoxelZ (pixels): Z", 1,3);

        gd.addCheckbox("Bleed Through", true);

        gd.showDialog();

        Width=img.getWidth();
        Height=img.getHeight();
        NbSlices=img.getStackSize();


    }


    boolean getGUIInput( )
    {

        m_StrConfig = ""; 
        bProcessGreen = bProcessRed = false;
        BackgroundType= "uniform";
        UniformBackground= (int)gd.getNextNumber();//minBlue
        m_minBluePer = (double)gd.getNextNumber();//minBlue
        m_BlueToGreen =  (int)gd.getNextNumber();

        if (m_minBluePer>100 || m_minBluePer<0)
            m_minBluePer=100;

        m_StrConfig="Minimum Blue = " + UniformBackground + "; ";
        m_StrConfig+="Minimum Blue % in a Blob = " + m_minBluePer + "; ";
        AutoBKGRadius= 6;
        MinIType= "AbsoluteBrightness";
        MinISetting=1.00f;

        //	ToleranceType= gd. getNextChoice();
        String chOp = gd.getNextChoice();
        if (chOp.contains("Green"))
        {
            bProcessGreen = true;
        }
        else if (chOp.contains("Red"))
        {
            bProcessRed = true;
        }
        else if (chOp.contains("Both"))
        {
            bProcessGreen = bProcessRed = true;
            rFileNames = new String[3];
        }

        if (rFileNames==null)
            rFileNames = new String[2];

        ToleranceSetting= (float) gd.getNextNumber();
        ToleranceValue=ToleranceSetting; 
        MinPeakIntensityGreen = (int)gd.getNextNumber();
        MinVolume=(int)gd.getNextNumber();
        m_maxGPix = (int)gd.getNextNumber();

        if (bProcessGreen)
        {
            m_StrConfig += "Minimum Green Intensity (Homer) = " + ToleranceValue + "; ";
            m_StrConfig += "Min. Peak Homer(green) = " + MinPeakIntensityGreen + "; ";
            m_StrConfig += "Min. Volume Homer(green) = " + MinVolume + "; ";
        }

        //Red Channel
        ToleranceSettingRed= (float)gd.getNextNumber();//minRed
        ToleranceValueRed=ToleranceSettingRed; 
        MinPeakIntensityRed = (int)gd.getNextNumber();
        MinVolumeRed= (int)gd.getNextNumber();//min RedPix
        m_maxRPix = (int)gd.getNextNumber();
        if (bProcessRed)
        {
            m_StrConfig += "Minimum Red Intensity (Arc) = " + ToleranceValueRed + "; ";
            m_StrConfig += "Min. Peak Arc(Red) = " + MinPeakIntensityRed + "; ";
            m_StrConfig += "Min. Volume Arc(Red) = " + MinVolumeRed + "; ";
        }

        UnitX=3;
        UnitY=3;
        UnitZ=3;


        ContrastBalance = 0.0f;

        SpontExclusionChoice = "No"; //min Z layers

        if (!m_2D)
        {
            ZTolerance = (float)gd.getNextNumber();
            m_StrConfig+= "Min. Z Layers Required = " + ZTolerance + "; ";
        }

        VoxelX=(float)gd.getNextNumber();
        VoxelY=(float)gd.getNextNumber();
        if (!m_2D)
            VoxelZ = (float)gd.getNextNumber();
        else
            VoxelZ = ZTolerance = 1;

        m_BTU = gd.getNextBoolean();

        m_StrConfig+= "(vX-vY-vZ) = " + VoxelX + "-" + VoxelY + "-" + VoxelZ + "; ";

        FociShapeChoice="No";
        FociShapeR=6;	
        nThreads = 1;//(int) gd.getNextNumber();

        //	VoxXsq=VoxelX*VoxelX;
        //	VoxYsq=VoxelY*VoxelY;
        //	VoxZsq=VoxelZ*VoxelZ;

        return true;

    }

    public boolean setImageParameters(int w, int h, int d, String iTitle, FileInfo fInfo, String fPath)
    {
        Width = w;
        Height = h;
        NbSlices = d;
        m_imgTitle = iTitle;

        m_FileInfo = fInfo;
        m_FilePath = fPath;

        m_imageStack=null;

        if ( d == 1 )
        {
            m_2D = true;
            Surrounding =Surrounding2D;
            N_SURROUNDING = N_SURROUNDING2D;
        }             
        else if ( d > 1)
        {
            m_2D = false;
            Surrounding =Surrounding3D;
            N_SURROUNDING = N_SURROUNDING3D;

        }

        return true;
    }

    boolean setImageParameters(ImagePlus imp)
    {
        img = imp;
        if (img==null)
        {
            IJ.noImage();
            return false;
        }
        else if (img.getStackSize() == 1) 
        {
            m_2D = true;
            Surrounding =Surrounding2D;
            N_SURROUNDING = N_SURROUNDING2D;
            //            IJ.error("Stack required");
            //            return false;
        } 
        else if (img.getStackSize() > 1) 
        {
            m_2D = false;
            Surrounding =Surrounding3D;
            N_SURROUNDING = N_SURROUNDING3D;
            //            IJ.error("Stack required");
            //            return false;
        } 
        else if (img.getType() != ImagePlus.COLOR_RGB ) 
        {
            IJ.error("RGB Image required....");
            return false;
        }


        Width=img.getWidth();
        Height=img.getHeight();
        NbSlices=img.getStackSize();
        if (NbSlices>1)
            m_imageStack = img.getStack();

        m_ip = img.getProcessor();

        imgTitle = img.getTitle();

        if (imgWhole!=null)
        {
            m_FileInfo = imgWhole.getOriginalFileInfo();
            m_FilePath = imgWhole.getOriginalFileInfo().directory;
        }
        else
        {
            m_FileInfo = img.getOriginalFileInfo();
            m_FilePath = img.getOriginalFileInfo().directory;
        }
          
        return true;


    }

    void setGUIInput(int bck, double minBckPer, int BlueToGrren, String chchoice, float minG, int minGPix, int maxGPix, float minR, 
            int minRPix, int maxRPix, float minZLayers, float vX, float vY, float vZ, boolean is2D, int minPeakGreen, int minPeakRed, WritableWorkbook wb)
    {
        xlWb = wb;
        m_StrConfig = ""; 
        bProcessGreen = bProcessRed = false;
        BackgroundType= "uniform";
        UniformBackground= bck;//(int)gd.getNextNumber();//minBlue
        m_StrConfig="Minimum Blue = " + UniformBackground + "; ";
        m_minBluePer = minBckPer;
        if (m_minBluePer>100 || m_minBluePer<0)
            m_minBluePer=100.0;

        m_BlueToGreen = BlueToGrren;


        m_StrConfig += "Minimum Blue % in a Blob = " + m_minBluePer + "; ";

        AutoBKGRadius= 6;
        MinIType= "AbsoluteBrightness";
        MinISetting=1.00f;

        //	ToleranceType= gd. getNextChoice();
        String chOp = chchoice;//gd.getNextChoice();
        if (chOp.contains("Green"))
        {
            bProcessGreen = true;
        }
        else if (chOp.contains("Red"))
        {
            bProcessRed = true;
        }
        else if (chOp.contains("Both"))
        {
            bProcessGreen = bProcessRed = true;
            rFileNames = new String[3];
        }
        if (rFileNames==null)
            rFileNames = new String[2];

        ToleranceSetting= minG;//(float) gd.getNextNumber();
        ToleranceValue=ToleranceSetting; 
        MinPeakIntensityGreen = minPeakGreen;
        MinVolume=minGPix;//(int)gd.getNextNumber();
        m_maxGPix = maxGPix;


        if (bProcessGreen)
        {
            m_StrConfig+= "Minimum Green Intensity (Homer) = " + ToleranceValue + "; ";
            m_StrConfig+= "Min. Peak Homer(green) = " + minPeakGreen + "; ";
            m_StrConfig+= "Min. Volume Homer(green) = " + MinVolume + "; ";
        }
        //Red Channel
        ToleranceSettingRed= minR;//(float)gd.getNextNumber();//minRed
        ToleranceValueRed=ToleranceSettingRed; 
        MinPeakIntensityRed = minPeakRed;
        MinVolumeRed= minRPix;//(int)gd.getNextNumber();//min RedPix
        m_maxRPix = maxRPix;

        if (bProcessRed)
        {
            m_StrConfig+= "Minimum Red Intensity (Arc) = " + ToleranceValueRed + "; ";
            m_StrConfig+= "Min. Peak Arc(Red) = " + minPeakRed + "; ";
            m_StrConfig+= "Min. Volume Arc (Red) = " + MinVolumeRed + "; ";
        }

        UnitX=3;
        UnitY=3;
        UnitZ=3;

        VoxelX=vX;
        VoxelY=vY;

        if (!is2D)
        {
            VoxelZ=vZ;
            ZTolerance=minZLayers;
        }
        else
        {
            VoxelZ = ZTolerance = 1;
        }


        ContrastBalance=0.0f;

        SpontExclusionChoice="No"; //min Z layers
        ZTolerance=minZLayers;//(float)gd.getNextNumber();
        m_StrConfig += "Min. Z Layers Required = " + ZTolerance + "; ";


        FociShapeChoice="No";
        FociShapeR=6;	
        nThreads = 1;//(int) gd.getNextNumber();

        //	VoxXsq=VoxelX*VoxelX;
        //	VoxYsq=VoxelY*VoxelY;
        //	VoxZsq=VoxelZ*VoxelZ;

    }

    void analyzeOfParentClass () 
    {

        int totFoci=0;

        //test
        boolean bFlag = false;
        if (bFlag)
        {
            MaskPixel=new short [NbSlices][Height][Width];

            for (int z = 0; z < NbSlices; z++)
            {
                for (int y = 0; y < Height; y++)
                {
                    for (int x = 0; x < Width; x++)
                    {
                        MaskPixel[z][y][x] = (short)((z*NbSlices)+(y*Height)+x);
                    }
                }
            }
        }

        m_vFoci = new ArrayList<PixelCollection3D>();
        typeVector = new Vector();
        ImageProcessor ip;
        int[] pixVal=null;
        IJ.showStatus("Initializing image.... Please Wait...");
        img=null;

        if (rFileNames!=null)
        {
            if (img==null)
                rFileNames[0]= m_imgTitle;
            else
                rFileNames[0]=img.getTitle();
        }


        // Mike, March 2013
        // Create a separate folder to store results
        String fres = rFileNames[0];
        int ix = fres.lastIndexOf(".tif.frames");
        if(ix >= 0) fres = fres.substring(0,ix);
        fres += "_blobs";
        File resdir = new File(m_FilePath, fres);
        IJ.log("\nChecking for results folder: " + resdir.getPath());
        if(resdir.exists()) {
            IJ.log("folder exists, using it.");
            m_ResultsPath = resdir.getPath();
        }
        else {
            boolean dirok = resdir.mkdir();
            if(dirok) {
                IJ.log("created results directory");
                m_ResultsPath = resdir.getPath();
            }
            else {
                IJ.log("failed to create directory, using image directory");
                m_ResultsPath = m_FilePath;
            }
        }  
        IJ.log("");
        m_ResultsPath += resdir.separator;


        if (bProcessGreen)
        {
            FociResultsTable.showRowNumbers(false);
            String filename = "";
            if (img!=null)
                filename = img.getTitle();
            else
                filename = m_imgTitle;
            int lix = filename.lastIndexOf(".tif.frames");
            if(lix >= 0)
                filename = filename.substring(0,lix);

            String sampleFileName = "Results_Homer_"+filename+".csv";
            rFileNames[1]=sampleFileName;
            m_avgHRes = new IEGAvgResults();
            m_avgHRes.m_imgTitle = filename;

            //mike march 2013, create the results image
            imgRes = IJ.createImage("Results_Homer_" + filename, "16-bit black", Width, Height, NbSlices);
            stackRes = imgRes.getStack();

            IJ.log("Processing Green Channel...");

            WritableSheet xlWs =null;
            if ( xlWb != null )
            {
                xlWs = xlWb.createSheet("Homer_" + filename, 2); 
                AddWorkBookTitles(xlWs);
            }

            IJ.log("Finding potential blobs");
            find3DMaximum( imgPixel, Width, Height, NbSlices, UnitX, UnitY, UnitZ, imgPixelBlue, UniformBackground, 
                    ToleranceValue );
            IJ.log("Growing blobs");
            grow3DMaximum( imgPixel, Width, Height, NbSlices,  MinIValue, ToleranceValue, HOMER_COLOR_ID, MinVolume, 
                    "Homer ", MinPeakIntensityGreen, m_maxGPix, m_avgHRes, xlWs );
            totFoci += PopulateMarkerData(HOMER_COLOR_ID);

            if (m_IEG_Scale)
                AddToMapFiles(HOMER_COLOR_ID);


            IJ.log("Saving results");
            if ( totFoci > 0 )
            {
                m_avgHRes.CalculateAvg();
                //AddAveragesToResultTable( m_avgHRes );
                saveResultsTableToFile("Results_Homer_");
                String tablename = m_ResultsPath + "results_homer_" + filename + ".txt";
                try {
                    FociResultsTable.saveAs(tablename);
                }
                catch (IOException e) {
                    IJ.log(e.toString());
                }
            }
            else {
                String tablename = m_ResultsPath + "results_homer_" + filename + ".txt";
                String cols = "FocusNO\tCoreX\tCoreY\tCoreZ\tVolume\tAvg_Intensity\tMin_I\tMax_I\tVar\tIntegrated_Intensity\t%_Saturation\t\r\n";
                IJ.saveString(cols,tablename);
                String csvname = m_ResultsPath + "results_homer_" + filename + ".csv";
                String colscsv = "FocusNO,CoreX,CoreY,CoreZ,Volume,Avg_Intensity,Min_I,Max_I,Var,Integrated_Intensity,%_Saturation\r\n";
                IJ.saveString(colscsv,csvname);
            }
            //mike march 2013
            //blobs are added to results image in grow3d function so save the image now
            String resimgname = m_ResultsPath + "results_homer_" + filename + ".tif";
            IJ.saveAs(imgRes, "tiff", resimgname);

            if (xlWb!=null)
            {
                m_avgHRes.m_imgTitle = "Averages";
                IEGBatchManager.AddToXLSheet(xlWs, m_avgHRes.m_totBlobs + 1, m_avgHRes, m_2D);
                m_avgHRes.m_imgTitle = filename;
            }


            IJ.log("Green channel is Finished\n\n");

            imgRes.close();
            MaskPixel=null;
            CollectionLocalMax=null;
            CollectionMax=null;
            CollectionBKG=null;
            CollectionObjBKG=null;
            PixelsInObject=null;
            m_vFoci.clear();
            imgPixel = null;
            FociResultsTable.reset();

            System.gc();

        }

        if (bProcessRed)
        {
            String filename="";

            if (img!=null)
                filename = img.getTitle();
            else
                filename = m_imgTitle;
            int lix = filename.lastIndexOf(".tif.frames");
            if(lix >= 0)
                filename = filename.substring(0,lix);

            String sampleFileName = "Results_Arc_" + filename + ".csv";
            if (bProcessGreen)
            {
                rFileNames[2]=sampleFileName;
            }
            else
            {
                rFileNames[1]=sampleFileName;
            }

            boolean bGreenFlag = bProcessGreen;
            bProcessGreen=false;

            m_avgARes = new IEGAvgResults();
            m_avgARes.m_imgTitle = filename;

            //mike march 2013, create the results image
            imgRes = IJ.createImage("Results_Arc_" + filename, "16-bit black", Width, Height, NbSlices);
            stackRes = imgRes.getStack();

            initialize();
            FociResultsTable.showRowNumbers(false);
            imgPixel = imgPixelRed;

            MaskPixel1=null;
            MaskPixel=null;
            System.gc();
            IEGBatchManager.ThreadSleep(1000);

            if (m_bSaveMemory)
                MaskPixel1 = new short [NbSlices][Width*Height];
            else
                MaskPixel = new short [Width][Height][NbSlices];

            IJ.log("Processing Red Channel...");

            WritableSheet xlWs = null;
            if (xlWb!=null)
            {
                xlWs = xlWb.createSheet("Arc_" + filename, 3); 
                AddWorkBookTitles(xlWs);
            }


            IJ.log("Finding potential blobs");
            find3DMaximum( imgPixel, Width, Height, NbSlices, UnitX, UnitY, UnitZ, imgPixelBlue, UniformBackground, ToleranceValueRed );
            IJ.log("Growing blobs");
            grow3DMaximum( imgPixel, Width, Height, NbSlices,  MinIValue, ToleranceValueRed, ARC_COLOR_ID, MinVolumeRed, 
                    "Arc ", MinPeakIntensityRed, m_maxRPix, m_avgARes, xlWs ); 

            bProcessGreen = bGreenFlag;
            totFoci+=PopulateMarkerData(ARC_COLOR_ID);

            IJ.log("Saving results");
            if (totFoci>0)
            {
                m_avgARes.CalculateAvg();
                saveResultsTableToFile("Results_Arc_");
                String tablename = m_ResultsPath + "results_arc_" + filename + ".txt";
                try {
                    FociResultsTable.saveAs(tablename);
                }
                catch (IOException e) {
                    IJ.log(e.toString());
                }
            }
            else {
                String tablename = m_ResultsPath + "results_arc_" + filename + ".txt";
                String cols = "FocusNO\tCoreX\tCoreY\tCoreZ\tVolume\tAvg_Intensity\tMin_I\tMax_I\tVar\tIntegrated_Intensity\t%_Saturation\t\r\n";
                IJ.saveString(cols,tablename);
                String csvname = m_ResultsPath + "results_arc_" + filename + ".csv";
                String colscsv = "FocusNO,CoreX,CoreY,CoreZ,Volume,Avg_Intensity,Min_I,Max_I,Var,Integrated_Intensity,%_Saturation\r\n";
                IJ.saveString(colscsv,csvname);
            }

            //mike march 2013
            //blobs are added to results image in grow3d function so save the image now
            String resimgname = m_ResultsPath + "results_arc_" + filename + ".tif";
            IJ.saveAs(imgRes, "tiff", resimgname);

            if (xlWb!=null)
            {
                m_avgARes.m_imgTitle = "Averages";
                IEGBatchManager.AddToXLSheet(xlWs, m_avgARes.m_totBlobs + 1, m_avgARes,  m_2D);
                m_avgARes.m_imgTitle = filename;
            }

            if (m_IEG_Scale)
                AddToMapFiles(ARC_COLOR_ID);

            IJ.log("Red channel is finished\n");
        }

        if (totFoci>0)
        {
            WriteFociMarkersToXML();
        }
        else
        {
            IJ.log("Unable to find any blobs... (Please try with different parameters)");
        }


        m_vCellMarker=null;
        m_vCellMarkerRed=null;
        typeVector=null;


        imgPixel=null;
        imgPixelRed=null;
        imgPixelBlue=null;

        imgRes.close();
        MaskPixel=null;
        MaskPixel1=null;
        CollectionLocalMax=null;
        CollectionMax=null;
        CollectionBKG=null;
        CollectionObjBKG=null;
        PixelsInObject=null;
        m_vFoci.clear();
        //  WindowManager.getFrame("Results").dispose();
        //     FociResultsTable.getResultsWindow().dispose();

        m_vFoci = null;
        typeVector = null;

        IEGBatchManager.ThreadSleep(1000);

    }

    public short getColorVal(int x, int y, int z, int colorIdx)
    {
        int cVal = getImagePixelValue(x,y,z);
        short r,g,b;
        r = (short)((cVal&0xff0000)>>16);
        g =(short)((cVal&0xff00)>>8);
        b = (short)(cVal&0xff);

        if (colorIdx==1)
            return r;
        if (colorIdx==2)
            return g;
        if (colorIdx==3)
            return b;

        return -1;
    }

    public short getColorVal(int x, int y, int z, int colorIdx, int[][] imgPix)
    {
        int cVal = getImagePixelValue(x,y,z, imgPix);
        if (cVal == -1)
            return 0;

        short r,g,b;
        r = (short)((cVal&0xff0000)>>16);
        g =(short)((cVal&0xff00)>>8);
        b = (short)(cVal&0xff);

        if (colorIdx==1)
            return r;
        if (colorIdx==2)
            return g;
        if (colorIdx==3)
            return b;

        return -1;
    }

    void find3DMaximum ( short[][][] PixelArray3D, int pictW, int pictH, int pictSlices, int cellX, 
            int cellY, int cellZ, short[][][] PixelArray3DBlue, int backgroundlevel, float peakTol) 
    {

        int pixelvalue, pixelvalue1;
        int pixelvalueBlue;
        boolean isMaximum;

        CollectionMax=new PixelCollection3D( this );
        CollectionLocalMax=new PixelCollection3D( this );
        CollectionBKG= new PixelCollection3D( this );
        nLocalMax=0;
        nMax=0;

        ImageStack imgStack = null;
        if ( pictSlices > 1 && img != null && !m_bSaveMemory)
            imgStack = img.getStack();
        else
            imgStack=null;

        int cellXMax = cellX/2;
        int cellYMax = cellY/2;
        int cellZMax = cellZ/2;

        for (int z=0; z<(int)(pictSlices); z++) 
        {
            if (imgStack!=null)
            {
                m_ip = imgStack.getProcessor(z+1);
            }
            for (int y=0; y<(int) (pictH);y++) 
            {
                // Mike Jan24,2013
                // Remove status update, it slows things down A LOT
                //IJ.showStatus("Scanning Image for blobs...Please Wait...." + Integer.toString(z) + "," + Integer.toString(y)); 
                for (int x=0; x<(int) (pictW);x++) {

                    if (bProcessGreen) 
                        pixelvalue = getColorVal(x,y,z,2, imgPixel1);
                    else
                        pixelvalue = getColorVal(x,y,z,1, imgPixel1);

                    pixelvalueBlue = getColorVal(x,y,z,3, imgPixel1);
                    isMaximum=true;
                    backgroundlevel=getBackground(x,y,z,AutoBKGRadius,0);
                    // Mike changed Jan 24, 2013
                    // Changed so it does not check against blue channel at all
                    // If using a nanozoomer image, a bleedthrough correction is already done before getting here
                    if(pixelvalue >= peakTol)   // Mike Jan 2016, change to peakTol...ToleranceValue is min green 
                    {
ExitCompareLoop:
                        for (int icellx= (int) (-cellX/2); icellx<=cellX/2; icellx++){
                            for (int icelly=(int) (-cellY/2); icelly<=cellY/2; icelly++){

                                if (!m_2D)
                                {
                                    for (int icellz=(int) (-cellZ/2); icellz<=cellZ/2; icellz++)
                                    {
                                        int pVal;
                                        if (bProcessGreen)
                                            pVal = getColorVal((x+icellx),(y+icelly),(z+icellz), 2, imgPixel1 );
                                        else
                                            pVal = getColorVal((x+icellx),(y+icelly),(z+icellz), 1, imgPixel1 );
                                     
                                        if (pixelvalue < pVal) 
                                        {
                                            isMaximum=false;
                                            break ExitCompareLoop;
                                        }
                                    }
                                }
                                else
                                {
                                    int icellz = 0;    
                                    int pVal;
                                    if (bProcessGreen)
                                        pVal = getColorVal((x+icellx),(y+icelly),(z+icellz), 2, imgPixel1 );
                                    else
                                        pVal = getColorVal((x+icellx),(y+icelly),(z+icellz), 1, imgPixel1 );

                                    if ( pixelvalue < pVal ) 
                                    {
                                        isMaximum=false;
                                        break ExitCompareLoop;
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        isMaximum=false;
                    }

                    if ( isMaximum )
                    {
                        CollectionLocalMax.addPixelWithBlue(x,y,z, (int) pixelvalue, pixelvalueBlue);
                        if (!CollectionLocalMax.isANeighborMax(x,y,z, (int) pixelvalue))
                        {
                            CollectionMax.addPixelDescendWithBlue(x,y,z, (int) pixelvalue, 
                                    (int) backgroundlevel, (int)pixelvalueBlue);
                        }
                    }				
                }
            }
            IJ.showProgress( z+1, pictSlices );
        }
        nMax=CollectionMax.getPixelNumber();
        nLocalMax=CollectionLocalMax.getPixelNumber();


        CollectionLocalMax=null;
        System.gc();
    }

    void grow3DMaximum ( short[][][] PixelArray3D, int pictW, int pictH, int pictSlices, float minThre, 
            float peakTol, int cellMarkerColorId, int minFociVolume, String Msg, int minPeak, int maxVolume, IEGAvgResults avgRes, WritableSheet xlWs) 
    {
        float tmptol = ToleranceValue;
        ToleranceValue = peakTol;

        // Mike Jan 24, 2013
        // Set nThreads to 1.  As nThreads increases, accuracy goes down, blobs are missed
        nThreads=1;

        PixelsInObject=new PixelCollection3D [nThreads] ;
        SearchObject= new boolean [nThreads];

        int indexBorder, indexLineBorder;
        int afMask=128;

        for (int i = 0; i < CollectionMax.getPixelNumber();)
        {
            // int i =0;
            // if (i < CollectionMax.getPixelNumber()){
            nRunningThreads=0;
            for (int j=0;j<nThreads &(i+j)<CollectionMax.getPixelNumber();j++)
            {
                if (j > nThreads)
                    break;
                PixelsInObject[j]=new PixelCollection3D(this, afMask);
                PixelsInObject[j].IJ_STatus_MSG_Prifix = Msg;
                SearchObject[j]=NOSUCCESS;
                PixelsInObject[j].setMaxIndex(i+j,j);
                (PixelsInObject[j]).start();          
                nRunningThreads++;
                afMask = afMask >> 1;
                if (afMask<=1)
                    afMask=128;
            }

            for (int j=0;j<nRunningThreads;j++)
            {
                try{
                    PixelsInObject[j].join();
                }
                catch (InterruptedException e)
                {
                }
                IJ.showProgress(i+j,CollectionMax.getPixelNumber());
            }

            for (int j=0;j<nThreads;j++)
            {
                if (PixelsInObject[j]!=null )
                {
                    // mike; note that blob validation happens in saveResults
                    // bok indicates whether it passes/fails
                    boolean bok = saveResults(i+j, j,(int)peakTol, minFociVolume, minPeak, maxVolume, avgRes, xlWs);
                    if(bok) resultsImgAdd(j); // mike; add blob to results image 
                    PixelsInObject[j].clear(); // mike; this used to be hidden 2 Fns deep in saveResults
                    if(SearchObject[j]==SUCCESS)
                    {
                        i=i+1;
                    }
                }
            }
        }     

        if(SpontExclusionChoice.contains("Yes")){
            IJ.log("SPONTEXCL");
            nFociExcluded=0;
            RTEndCounter=FociResultsTable.getCounter();
            int nRemoved=removeSpontFoci(RTStartCounter,RTEndCounter,ZTolerance);
        }

        ToleranceValue = tmptol;
        CollectionMax=null;
        PixelsInObject=null;
        CollectionBKG=null;

        }

        int PopulateMarkerData(int markerId) 
        {
            int totFoci=0;
            m_vCellMarker = new IEGCellCntrMarkerVector(markerId); 
            for ( int i = 0; i < m_vFoci.size();i++)
            {
                PixelCollection3D foci = m_vFoci.get(i);

                //            CellCntrMarker fociCenter = new CellCntrMarker(foci.m_BlobId,foci.xCenterPix,foci.yCenterPix,foci.zCenterPix);
                IEGCellCntrMarker fociCenter = new IEGCellCntrMarker(foci.m_BlobId,foci.xCenterPix ,foci.yCenterPix ,foci.zCenterPix );
                IEGCellCntrMarker.ZRange zR = fociCenter.new ZRange(foci.zSt+1, (foci.zEnd>NbSlices?foci.zEnd:foci.zEnd+1));
                fociCenter.setZRange(zR);
                m_vCellMarker.add(fociCenter);
                totFoci++;

            }
            IJ.log("Total Blobs:" + Integer.toString(totFoci));
            m_vCellMarker.setTotalCells(m_vFoci.size());
            typeVector.add(m_vCellMarker);
            return totFoci;

        }

        // Mike March 8, 2013.  new function to write blobs into a 16-bit results image like farsight
        void resultsImgAdd(int j)
        {
            PixelCollection3D p = PixelsInObject[j];
            int n = p.getPixelNumber();

            for(int s=1; s<=NbSlices; s++) {
                ImageProcessor ip = stackRes.getProcessor(s);
                for(int i=0; i<n; i++) {
                    if(p.getZ(i)+1 == s)
                        ip.set(p.getX(i), p.getY(i), nFoci-2); //nfoci-2 is bc of vivek's counting with globals
                }
            }
        }


        void AddToMapFiles(int mId)
        {
            try
            {
                if (m_vCellMarker.size()>0)
                {
                    FileWriter m_fWriter;
                    if (mId == HOMER_COLOR_ID )
                        //                    m_fWriter = new FileWriter("C:\\Users\\vivek.trivedi\\Desktop\\a\\H_map.csv", true);
                        m_fWriter = new FileWriter(m_FilePath + "H_map.csv", true);
                    else
                        m_fWriter = new FileWriter( m_FilePath + "A_map.csv", true);
                    //                    m_fWriter = new FileWriter("C:\\Users\\vivek.trivedi\\Desktop\\a\\A_map.csv", true);

                    BufferedWriter m_writer = new BufferedWriter(m_fWriter);

                    for ( int i = 0; i < m_vCellMarker.size(); i++)
                    {
                        IEGCellCntrMarker fociCenter = (IEGCellCntrMarker)m_vCellMarker.get(i);
                        int x = Math.round(fociCenter.getX() * 0.25f);
                        int y = Math.round( ( fociCenter.getY() + m_YOffset ) * 0.25f );
                        m_writer.write(Integer.toString(x) +","+Integer.toString(y));
                        m_writer.newLine();
                    }
                    m_writer.close();
                }

                /*     if (m_vCellMarkerRed.size()>0)
                       {
                       FileWriter m_fWriter = new FileWriter("C:\\Users\\vivek.trivedi\\Desktop\\a\\A_map.csv", true);
                       BufferedWriter m_writer = new BufferedWriter(m_fWriter);

                       for ( int i = 0; i < m_vCellMarkerRed.size(); i++)
                       {
                       CellCntrMarker fociCenter = (CellCntrMarker)m_vCellMarkerRed.get(i);
                       int x = Math.round(fociCenter.getX() * 0.03125f);
                       int y = Math.round(fociCenter.getY() * 0.03125f);

                       m_writer.write(Integer.toString(x) +","+Integer.toString(y));
                       m_writer.newLine();
                       }
                       m_writer.close();
                       }*/

            }
            catch(Exception e)
            {
                IJ.log(e.toString());
            }

        }

        //
        public static final int SAVE=FileDialog.SAVE, OPEN=FileDialog.LOAD;
        private String getFilePath(JFrame parent, String dialogMessage, int dialogType, String sampleFileName){
            switch(dialogType){
                case(SAVE):
                    dialogMessage = "Save "+dialogMessage;
                    break;
                case(OPEN):
                    dialogMessage = "Open "+dialogMessage;
                    break;
            }
            FileDialog fd ;
            String[] filePathComponents = new String[2];
            int PATH = 0;
            int FILE = 1;
            fd = new FileDialog(parent, dialogMessage, dialogType);
            switch(dialogType){
                case(SAVE):

                    fd.setFile(sampleFileName);
                    break;
            }
            fd.setVisible(true);
            filePathComponents[PATH] = fd.getDirectory();
            filePathComponents[FILE] = fd.getFile();
            return filePathComponents[PATH]+filePathComponents[FILE];
        }


        public void WriteFociMarkersToXML(){
            String filename = "";
            if (img!=null)
                filename = img.getTitle();
            else
                filename = m_imgTitle;
            int lix = filename.lastIndexOf(".tif.frames");
            if(lix >= 0)
                filename = filename.substring(0,lix);


            String sampleFileName = "CellCounter_"+filename+".xml";
            //String filePath = m_FileInfo.directory + sampleFileName;
            String filePath = m_ResultsPath + sampleFileName;
            IEGWriteXML wxml = new IEGWriteXML(filePath);

            wxml.writeXML(rFileNames, typeVector, typeVector.indexOf(m_vCellMarker), true, IEG_VER, m_StrConfig);
        }


        // never been used for IEG purpose
        int getBackground(int x,int y, int z, int radius, int depth){
            return (int)UniformBackground;
        }


        public int getImagePixelValue(int x,int y, int z)
        {
            if (withInBoundary(x,y,z))
            {
                if ( m_bSaveMemory )
                {
                    if (m_imageStack==null)
                        return m_ip.getPixel(x, y);
                    else
                    {
                        m_ip = m_imageStack.getProcessor(z+1);
                        return m_ip.getPixel(x, y);
                    }

                }
                else
                {
                    return (int) imgPixel[x][y][z];
                }
            }
            else
            {
                return 0;
            }
        }

        public int getImagePixelValue(int x,int y, int z, short[][][] imagePixel)
        {
            if (withInBoundary(x,y,z))
            {
                if ( m_bSaveMemory )
                {
                    return m_ip.getPixel(x, y);

                }
                else
                {
                    return (int) imgPixel[x][y][z];
                }
            }
            else
            {
                return 0;
            }
        }

        public int getImagePixelValue(int x,int y, int z, int[][] imagePixel)
        {
            if ( x >= 0 && x < Width && y >= 0 && y < Height && z >= 0 && z < NbSlices )
            {
                if ( m_bSaveMemory )
                {
                    //                int currentOffset = z*m_offset+y*Width+x;
                    int currentOffset =y*Width+x;
                    return imagePixel[z][currentOffset];
                }
                else
                {
                    return (int) imgPixel[x][y][z];
                }
            }


            return -1;
        }

        void initialize(){
            startTime=System.currentTimeMillis();
            nFoci=2; 
            FociResultsTable= new ResultsTable();
            FociResultsTable.reset();
        }

        public boolean withInBoundary(int m,int n,int o) {
            return (m >= 0 && m < Width && n >=0 && n < Height && o >=0 && o < NbSlices );
        }

        float squareD(int x1,int y1, int z1,int x2, int y2, int z2){
            float square=(x1-x2)*(x1-x2)+(y1-y2)*(y1-y2)+(z1-z2)*(z1-z2);
            return square;
        }

        void saveResults(int i,int j, IEGAvgResults avgRes, WritableSheet xlWs){   
            if(!approveResult(i,j)){
                PixelsInObject[j].removeFromMask();
            }else{
                m_vFoci.add(PixelsInObject[j]);
                savetoResultsTable(i,j, avgRes, xlWs);
                nFoci++;
            }

            //  PixelsInObject[j].clear();
            //  System.gc();
            //  System.gc();

        }


        // mike march 2013, changed to return boolean success/fail so i know to add blob to results image
        boolean saveResults(int i,int j, int minIntensity, int minVolume, int minPeak, int maxVolume, IEGAvgResults avgRes, WritableSheet xlWs)
        {
            boolean ok = false;
            try
            {
                ok = approveResult(i,j, minIntensity, minVolume, minPeak, maxVolume);
                ok &= (PixelsInObject[j].getNumZLayers() >= ZTolerance); //mike add
                if(ok) {
                    m_vFoci.add(PixelsInObject[j]);
                    //savetoResultsTable(i,j, avgRes, xlWs);
                    savetoResultsTableMe(i,j, avgRes, xlWs);
                    nFoci++;
                    return true;
                }
                else {
                    PixelsInObject[j].removeFromMask();
                    return false;
                }

                //MIke, why the f** is it cleared in this function???
                //PixelsInObject[j].clear();
            }
            catch(Exception e)
            {
                int iii = 0;
                return false;
            }

        }


        void savetoResultsTable(int i, int j, IEGAvgResults avgRes, WritableSheet xlWs){ 
            FociResultsTable.incrementCounter();
            FociResultsTable.addLabel("ImageTitle",m_imgTitle); //imgTitle
            PixelsInObject[j].m_BlobId = nFoci-1;
            FociResultsTable.addValue("FocusNO",nFoci-1);
            FociResultsTable.addValue("CoreX",PixelsInObject[j].getCoreX());
            FociResultsTable.addValue("CoreY",PixelsInObject[j].getCoreY());
            float fociVol = PixelsInObject[j].getPixelNumber();

            if( !m_2D )
            {
                FociResultsTable.addValue("CoreZ",PixelsInObject[j].getCoreZ());
                FociResultsTable.addValue("Volume",fociVol);
                FociResultsTable.addValue("AreaXY",PixelsInObject[j].getAreaXY());
                FociResultsTable.addValue("AreaXZ",PixelsInObject[j].getAreaXZ());
                FociResultsTable.addValue("AreaYZ",PixelsInObject[j].getAreaYZ());
            }
            else
            {
                FociResultsTable.addValue("AreaXY",PixelsInObject[j].getAreaXY());
            }

            FociResultsTable.addValue("Intensity",PixelsInObject[j].getMeanValue());
            FociResultsTable.addValue("Background",CollectionMax.getBackgroundLevel(i));
            FociResultsTable.addValue("MinI",PixelsInObject[j].getPixMinimum());
            FociResultsTable.addValue("MaxI",PixelsInObject[j].getPixMaximum());
            FociResultsTable.addValue("Range",PixelsInObject[j].getPixRange());
            FociResultsTable.addValue("Intensity Integral",PixelsInObject[j].getIntensityINtegral());

            FociResultsTable.addValue("Saturated Pixels",PixelsInObject[j].m_TotalSaturatedPixels);
            FociResultsTable.addValue("(%)Saturation",((PixelsInObject[j].m_TotalSaturatedPixels*100.0f)/fociVol));

            avgRes.AddToAvg(PixelsInObject[j].getCoreX(),PixelsInObject[j].getCoreY(), PixelsInObject[j].getCoreZ(), fociVol,
                    PixelsInObject[j].getAreaXY(), PixelsInObject[j].getAreaXZ(), PixelsInObject[j].getAreaYZ(), 
                    CollectionMax.getBackgroundLevel(i), MinIValue, PixelsInObject[j].getValue(0),
                    PixelsInObject[j].getPixRange(), PixelsInObject[j].getMeanValue(), PixelsInObject[j].getIntensityINtegral(),
                    PixelsInObject[j].m_TotalSaturatedPixels, ((PixelsInObject[j].m_TotalSaturatedPixels*100.0f)/fociVol));

            if ( xlWs != null )
                AddFociResToWorkBook( xlWs, PixelsInObject[j] );

        }

        void savetoResultsTableMe(int i, int j, IEGAvgResults avgRes, WritableSheet xlWs){ 
            FociResultsTable.incrementCounter();
            PixelsInObject[j].m_BlobId = nFoci-1;
            FociResultsTable.addValue("FocusNO",nFoci-1);
            FociResultsTable.addValue("CoreX",Math.round(PixelsInObject[j].getCoreX()));
            FociResultsTable.addValue("CoreY",Math.round(PixelsInObject[j].getCoreY()));
            float fociVol = PixelsInObject[j].getPixelNumber();

            if( !m_2D )
            {
                FociResultsTable.addValue("CoreZ",Math.round(PixelsInObject[j].getCoreZ()));
                FociResultsTable.addValue("Volume",fociVol);
            }

            FociResultsTable.addValue("Avg_Intensity",PixelsInObject[j].getMeanValue());
            FociResultsTable.addValue("MinI",PixelsInObject[j].getPixMinimum());
            FociResultsTable.addValue("MaxI",PixelsInObject[j].getPixMaximum());
            //FociResultsTable.addValue("Range",PixelsInObject[j].getPixRange());
            FociResultsTable.addValue("Var",PixelsInObject[j].getVariance());
            FociResultsTable.addValue("Integrated_Intensity",PixelsInObject[j].getIntensityINtegral());
            FociResultsTable.addValue("%_Saturation",((PixelsInObject[j].m_TotalSaturatedPixels*100.0f)/fociVol));

            avgRes.AddToAvg(PixelsInObject[j].getCoreX(),PixelsInObject[j].getCoreY(), PixelsInObject[j].getCoreZ(), fociVol,
                    PixelsInObject[j].getAreaXY(), PixelsInObject[j].getAreaXZ(), PixelsInObject[j].getAreaYZ(), 
                    CollectionMax.getBackgroundLevel(i), MinIValue, PixelsInObject[j].getValue(0),
                    PixelsInObject[j].getPixRange(), PixelsInObject[j].getMeanValue(), PixelsInObject[j].getIntensityINtegral(),
                    PixelsInObject[j].m_TotalSaturatedPixels, ((PixelsInObject[j].m_TotalSaturatedPixels*100.0f)/fociVol));

            if ( xlWs != null )
                AddFociResToWorkBook( xlWs, PixelsInObject[j] );

        }

        void AddFociResToWorkBook(WritableSheet xlWs, PixelCollection3D foci)
        {
            int iRow = foci.m_BlobId+1;
            float fociVol = foci.getPixelNumber();

            try
            {

                jxl.write.Label label = new jxl.write.Label(0, iRow, m_imgTitle);
                xlWs.addCell(label);          


                jxl.write.Number val = new jxl.write.Number(1, iRow, foci.m_BlobId);
                xlWs.addCell(val);

                val = new jxl.write.Number(2, iRow, foci.getCoreX());
                xlWs.addCell(val);
                val = new jxl.write.Number(3, iRow, foci.getCoreY());
                xlWs.addCell(val);


                if (!m_2D)
                {
                    val = new jxl.write.Number(4, iRow, foci.getCoreZ());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(5, iRow, foci.getPixelNumber());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(6, iRow, foci.getAreaXY());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(7, iRow, foci.getAreaXZ());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(8, iRow, foci.getAreaYZ());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(9, iRow, foci.getMeanValue()); //mean intensity
                    xlWs.addCell(val);
                    val = new jxl.write.Number(10, iRow, foci.getBackgroundLevel(0)); //bck
                    xlWs.addCell(val);
                    val = new jxl.write.Number(11, iRow, MinIValue); //minimum I
                    xlWs.addCell(val);
                    val = new jxl.write.Number(12, iRow, foci.getValue(0)); //maxI
                    xlWs.addCell(val);
                    val = new jxl.write.Number(13, iRow, foci.getPixRange()); //
                    xlWs.addCell(val);
                    val = new jxl.write.Number(14, iRow, foci.getIntensityINtegral());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(15, iRow, foci.m_TotalSaturatedPixels);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(16, iRow, ( ( foci.m_TotalSaturatedPixels * 100.0f ) / fociVol ) );
                    xlWs.addCell(val);
                }
                else
                {
                    val = new jxl.write.Number(4, iRow, foci.getAreaXY());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(5, iRow, foci.getMeanValue());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(6, iRow, foci.getBackgroundLevel(0));
                    xlWs.addCell(val);
                    val = new jxl.write.Number(7, iRow, MinIValue);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(8, iRow, foci.getValue(0));
                    xlWs.addCell(val);
                    val = new jxl.write.Number(9, iRow, foci.getPixRange());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(10, iRow, foci.getIntensityINtegral());
                    xlWs.addCell(val);
                    val = new jxl.write.Number(11, iRow, foci.m_TotalSaturatedPixels);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(12, iRow, ( ( foci.m_TotalSaturatedPixels * 100.0f ) / fociVol ) );
                    xlWs.addCell(val);

                }   
            }
            catch(Exception e)
            {
                IJ.log("Summary workbook writing error..."); 
            }

        }

        void AddWorkBookTitles(WritableSheet xlWs)
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

                if (!m_2D)
                {
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
                else
                {
                    label = new jxl.write.Label(4, 0, "Area XY");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(5, 0, "Intensity");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(6, 0, "Background");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(7, 0, "MinI");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(8, 0, "MaxI");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(9, 0, "Range");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(10, 0, "Saturated Pixels");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(11, 0, "(%)Saturation");
                    xlWs.addCell(label);
                }   

            }
            catch(Exception e)
            {
                IJ.log("Summary workbook writing error..."); 
            }
        }

        void AddAveragesToResultTable(IEGAvgResults avgRes)
        {
            /*FociResultsTable.incrementCounter();
            FociResultsTable.addLabel("ImageTitle","Averages"); //imgTitle
            FociResultsTable.addValue("FocusNO", avgRes.m_totBlobs);
            FociResultsTable.addValue("CoreX", avgRes.m_CoreX);
            FociResultsTable.addValue("CoreY", avgRes.m_CoreY);

            if( !m_2D )
            {
                FociResultsTable.addValue("CoreZ", avgRes.m_CoreZ);
                FociResultsTable.addValue("Volume", avgRes.Volume);
                FociResultsTable.addValue("AreaXY", avgRes.AreaXY);
                FociResultsTable.addValue("AreaXZ", avgRes.AreaXY);
                FociResultsTable.addValue("AreaYZ", avgRes.AreaYZ);
            }
            else
            {
                FociResultsTable.addValue("AreaXY", avgRes.AreaXY);
            }

            FociResultsTable.addValue("Intensity", avgRes.Intensity);
            FociResultsTable.addValue("Background", avgRes.Background);
            FociResultsTable.addValue("MinI", avgRes.MinI);
            FociResultsTable.addValue("MaxI", avgRes.MaxI);
            FociResultsTable.addValue("Range", avgRes.Range);
            FociResultsTable.addValue("Intensity Integral", avgRes.Intensity_Integral);
            FociResultsTable.addValue("Saturated Pixels", avgRes.Saturated_Pixels);
            FociResultsTable.addValue("(%)Saturation", avgRes.per_Saturation);*/

        }

        void saveResultsTableToFile(String Prefix)
        {
            //FociResultsTable.incrementCounter();
            //FociResultsTable.addLabel( "ImageTitle", m_StrConfig );
            String filename = "";

            if (img!=null)
                filename = img.getTitle();
            else
                filename = m_imgTitle;
            int lix = filename.lastIndexOf(".tif.frames");
            if(lix >= 0)
                filename = filename.substring(0,lix);

            String sampleFileName = Prefix+filename+".csv";
            // String filePath = getFilePath(new JFrame(), "Save Marker File (.csv)", SAVE, sampleFileName);
            //String filePath = m_FileInfo.directory + sampleFileName;
            String filePath = m_ResultsPath + sampleFileName;

            try
            {
                FociResultsTable.saveAs(filePath);
            }
            catch(IOException e)
            {
                IJ.error("Result Table Save Error", e.toString());
            }
        }

        boolean approveResult(int i, int j)
        {
            float volume;
            if((SearchObject[j])!=SUCCESS) return false;
            if(PixelsInObject[j].getPixelNumber()< MinVolume) return false;
            //     if(PixelsInObject[j].getPixRange()< ToleranceValue) return false;
            if(PixelsInObject[j].GetBackgoundPer(UniformBackground) < m_minBluePer) return false;
            if(FociShapeChoice.contains("Yes"))
            {
                volume=(float)PixelsInObject[j].getVolume();
                if(volume/(PixelsInObject[j].getAreaXY())>FociShapeR/VoxelZ) return false;
                if(volume/(PixelsInObject[j].getAreaXZ())>FociShapeR/VoxelY) return false;
                if(volume/(PixelsInObject[j].getAreaYZ())>FociShapeR/VoxelX) return false;
            }
            return true;
        }

        boolean approveResult(int i, int j, int minIntensity, int minVolume, int minPeak, int maxVolume)
        {
            float volume;
            if((SearchObject[j])!=SUCCESS) return false;
            if(PixelsInObject[j].getPixelNumber()<minVolume) return false;
            if(PixelsInObject[j].getPixelNumber()>maxVolume) return false;

            //	     if(PixelsInObject[j].getPixRange()< minIntensity) return false;
            if(PixelsInObject[j].GetBackgoundPer(UniformBackground) < m_minBluePer) 
            {
                return false;
            }
            if (!PixelsInObject[j].isEnoughPeak(minPeak))
                return false;

            if(FociShapeChoice.contains("Yes"))
            {
                volume=(float)PixelsInObject[j].getVolume();
                if(volume/(PixelsInObject[j].getAreaXY())>FociShapeR/VoxelZ) 
                {
                    return false;
                }
                if(volume/(PixelsInObject[j].getAreaXZ())>FociShapeR/VoxelY) 
                {
                    return false;
                }
                if(volume/(PixelsInObject[j].getAreaYZ())>FociShapeR/VoxelX) 
                {
                    return false;
                }
            }

            return true;
        }

        int removeSpontFoci(int starti,int endi,float deltaz){
            float maxDeltaZ=deltaz/VoxelZ;
            FociResultsTable.updateResults();
            //      WindowManager.getFrame("Results").setVisible(false);

            float mediancorez;
            float corez;
            int i,j;
            mediancorez=medianCoreZ(starti,endi);
            IJ.log("");
            IJ.log("");
            IJ.log("i = " + Integer.toString(starti));
            IJ.log("j = " + Integer.toString(endi));
            IJ.log("maxDeltaZ = " + Float.toString(maxDeltaZ));
            IJ.log("mediancorez = " + Float.toString(mediancorez));

            for (i=starti;i<endi;i++){
                corez=(float)FociResultsTable.getValue("CoreZ",i);
                IJ.log("corez = " + Float.toString(corez));
                if(Math.abs(corez-mediancorez)>maxDeltaZ){
                    FociResultsTable.deleteRow(i);
                    //  FociResultsTable.setValue("Z Range issue", i,1);
                    nFociExcluded++;
                    FociResultsTable.updateResults();
                    endi=FociResultsTable.getCounter();
                    removeSpontFoci(starti,endi,deltaz);

                    break;
                }		     
            }

            return nFociExcluded;

        }
        float medianCoreZ(int starti, int endi){
            FociResultsTable.updateResults();
            float[] mZ=new float [endi-starti+1];
            int i;
            int j=0;
            for(i=starti;i<endi;i++){
                mZ[j]=(float)FociResultsTable.getValue("CoreZ",i);
                j++;
            }
            java.util.Arrays.sort(mZ);
            return (float) (mZ[(int)(j/2)]);
        }
        void CreateShow3DStack1( int [][][] PixelArray3D, int pictW, int pictH, int pictSlices, String imgtitle) 
        {
            IJ.newImage(imgtitle,"16-bit black",pictW,pictH,pictSlices);
            ImagePlus new3Dstack=WindowManager.getCurrentImage();
            ImageStack stack=new3Dstack.getStack();
            ImageProcessor ip;
            for (int z=0; z<NbSlices; z++)
            {
                ip=stack.getProcessor(z+1);
                for (int y=0; y<pictH; y++)
                {
                    for (int x=0; x<pictW; x++)
                    {
                        ip.setValue(PixelArray3D[x][y][z]);
                        ip.drawPixel(x, y);
                    }
                }

            }
            //	new3Dstack.show();
            //	IJ.run("FociPicker_256Colors");
            //	new3Dstack.updateAndDraw();
        }

        public short getMaskPixel(int x, int y, int z)
        {
            short retVal = -1;
            if (MaskPixel1==null)
                return retVal;

            if (x >= Width || y >= Height || z >= NbSlices)
                return retVal;

            /*  int offset = (z*NbSlices)+(y*Width)+x;
                if (offset >= NbSlices*Height*Width)
                return retVal;*/

            int offset = (y*Width)+x;
            if (offset >= Height*Width)
                return retVal;


            retVal = MaskPixel1[z][offset];
            return retVal;
        }

        public void  setMaskPixel(int x, int y, int z, short sValue)
        {
            if (MaskPixel1==null)
                return;

            if (x >= Width || y >= Height || z >= NbSlices)
                return;

            /*         int offset = (z*NbSlices)+(y*Width)+x;
                       if (offset >= NbSlices*Height*Width)
                       return;*/

            int offset = (y*Width)+x;
            if (offset >= Height*Width)
                return;


            MaskPixel1[z][offset] = sValue;
        }

        void CreateShow3DStack( short [][][] PixelArray3D, int pictW, int pictH, int pictSlices,String imgtitle) 
        {
            IJ.newImage(imgtitle,"8-bit black",pictW,pictH,pictSlices);
            ImagePlus new3Dstack=WindowManager.getCurrentImage();
            ImageStack stack=new3Dstack.getStack();
            ImageProcessor ip;
            for (int z=0; z<NbSlices; z++)
            {
                ip=stack.getProcessor((int)(z+1));
                for (int y=0; y<pictH; y++)
                {
                    for (int x=0; x<pictW; x++)
                    {
                        ip.setValue(PixelArray3D[x][y][z]);
                        ip.drawPixel(x, y);
                    }

                }

            }

            new3Dstack.show();

            IJ.run("LutLoader");
            IJ.run("FociPicker_256Colors");
            new3Dstack.updateAndDraw();

            IJ.log("256_Colors lookup table");
        }

        public void run() {
            Width=img.getWidth();
            Height=img.getHeight();
            NbSlices=img.getStackSize();
            imgTitle = img.getTitle();
            analyzeOfParentClass();

            System.gc();
            System.gc();
            System.gc();
            System.gc();

        }

        public ImagePlus ExtractRoiImage( ImagePlus imp) {
            int first, last;


            //  ImagePlus imp = IJ.getImage();
            int stackSize = imp.getStackSize();
            String title = imp.getTitle();

            first = 1;
            last = stackSize;

            String newTitle = title + "-ROI";

            if (!IJ.altKeyDown()||stackSize>1) 
                if (imp.isHyperStack() || imp.isComposite()) 
                    return null;

            if (newTitle==null)
                return null;

            ImagePlus imp2;
            Roi roi = imp.getRoi();
            if (first>1||last<stackSize)
                imp2 = ExtractFromStack(imp, first, last);
            else if (imp.getStackSize()==1)
                imp2 = ExtractFromSingleImage(imp);
            else
                imp2 = duplicateImage(imp);
            Calibration cal = imp2.getCalibration();
            if (roi!=null && (cal.xOrigin!=0.0||cal.yOrigin!=0.0)) {
                cal.xOrigin -= roi.getBounds().x;
                cal.yOrigin -= roi.getBounds().y;
            }
            imp2.setTitle(newTitle);
            //  imp2.show();
            if (roi!=null && roi.isArea() && roi.getType()!=Roi.RECTANGLE && roi.getBounds().width==imp2.getWidth())
                imp2.restoreRoi();

            return imp2;
        }

        public ImagePlus ExtractFromStack(ImagePlus imp, int firstSlice, int lastSlice) {
            Rectangle rect = null;
            Roi roi = imp.getRoi();
            if (roi!=null && roi.isArea())
                rect = roi.getBounds();
            ImageStack stack = imp.getStack();
            ImageStack stack2 = null;
            for (int i=firstSlice; i<=lastSlice; i++) {
                ImageProcessor ip2 = stack.getProcessor(i);
                ip2.setRoi(rect);
                ip2 = ip2.crop();
                if (stack2==null)
                    stack2 = new ImageStack(ip2.getWidth(), ip2.getHeight(), imp.getProcessor().getColorModel());
                stack2.addSlice(stack.getSliceLabel(i), ip2);
            }
            ImagePlus imp2 = imp.createImagePlus();
            imp2.setStack("DUP_"+imp.getTitle(), stack2);
            int size = stack2.getSize();
            boolean tseries = imp.getNFrames()==imp.getStackSize();
            if (tseries)
                imp2.setDimensions(1, 1, size);
            else
                imp2.setDimensions(1, size, 1);
            return imp2;
        }

        ImagePlus duplicateImage(ImagePlus imp) {
            ImageProcessor ip = imp.getProcessor();
            ImageProcessor ip2 = ip.crop();
            ImagePlus imp2 = imp.createImagePlus();
            imp2.setProcessor("DUP_"+imp.getTitle(), ip2);
            String info = (String)imp.getProperty("Info");
            if (info!=null)
                imp2.setProperty("Info", info);
            if (imp.getStackSize()>1) {
                ImageStack stack = imp.getStack();
                String label = stack.getSliceLabel(imp.getCurrentSlice());
                if (label!=null && label.indexOf('\n')>0)
                    imp2.setProperty("Info", label);
                if (imp.isComposite()) {
                    LUT lut = ((CompositeImage)imp).getChannelLut();
                    imp2.getProcessor().setColorModel(lut);
                }
            }
            Overlay overlay = imp.getOverlay();
            if (overlay!=null && !imp.getHideOverlay()) {
                overlay = overlay.duplicate();
                Rectangle r = ip.getRoi();
                if (r.x>0 || r.y>0)
                    overlay.translate(-r.x, -r.y);
                imp2.setOverlay(overlay);
            }
            return imp2;
        }

        public ImagePlus ExtractFromSingleImage(ImagePlus imp) {
            if (imp.getStackSize()==1)
                return duplicateImage(imp);
            Rectangle rect = null;
            Roi roi = imp.getRoi();
            if (roi!=null && roi.isArea())
                rect = roi.getBounds();
            ImageStack stack = imp.getStack();
            ImageStack stack2 = null;
            for (int i=1; i<=stack.getSize(); i++) {
                ImageProcessor ip2 = stack.getProcessor(i);
                ip2.setRoi(rect);
                ip2 = ip2.crop();
                if (stack2==null)
                    stack2 = new ImageStack(ip2.getWidth(), ip2.getHeight(), imp.getProcessor().getColorModel());
                stack2.addSlice(stack.getSliceLabel(i), ip2);
            }
            ImagePlus imp2 = imp.createImagePlus();
            imp2.setStack("DUP_"+imp.getTitle(), stack2);
            int[] dim = imp.getDimensions();
            imp2.setDimensions(dim[2], dim[3], dim[4]);
            if (imp.isComposite()) {
                imp2 = new CompositeImage(imp2, 0);
                ((CompositeImage)imp2).copyLuts(imp);
            }
            if (imp.isHyperStack())
                imp2.setOpenAsHyperStack(true);
            Overlay overlay = imp.getOverlay();
            if (overlay!=null && !imp.getHideOverlay()) {
                overlay = overlay.duplicate();
                if (rect!=null)
                    overlay.translate(-rect.x, -rect.y);
                imp2.setOverlay(overlay);
            }
            return imp2;
        }



    }

