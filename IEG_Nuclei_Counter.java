/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * Based On RoiManager but with few difference
 */

/**
 *
 * @author vivek.trivedi
 */
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import java.util.zip.*;
import ij.plugin.frame.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.plugin.Colors;
import ij.util.*;
import ij.macro.*;
import ij.measure.*;
//import ij.plugin.PolygonIntersect;
import java.awt.geom.Point2D;
import java.text.*;
//import sun.security.jca.GetInstance;

/** This plugin implements the Analyze/Tools/ROI Manager command. */
public class IEG_Nuclei_Counter extends PlugInFrame implements ActionListener, ItemListener, KeyListener , MouseListener, MouseWheelListener {
	public static final String LOC_KEY = "manager.loc";
	private static final int BUTTONS = 11;
	private static final int DRAW=0, FILL=1, LABEL=2;
	private static final int SHOW_ALL=0, SHOW_NONE=1, LABELS=2, NO_LABELS=3;
	private static final int MENU=0, COMMAND=1;
	private static int rows = 15;
	private static int lastNonShiftClick = -1;
	private static boolean allowMultipleSelections = true; 
	private static String moreButtonLabel = "More "+'\u00bb';
	private Panel panel;
	private static Frame instance;
	private static int colorIndex = 4;
	private java.awt.List list;
	private Hashtable rois = new Hashtable();
	private Hashtable roi_stats = new Hashtable();
	private boolean canceled;
	private boolean macro;
	private boolean ignoreInterrupts;
	private PopupMenu pm;
	private Button moreButton, colorButton;
	private Checkbox showAllCheckbox = new Checkbox("Show All", false);
	private Checkbox labelsCheckbox = new Checkbox("Edit Mode", false);
	private Checkbox showCheckbox_active = new Checkbox("Show Active Selected ", false);
        private Checkbox showCheckbox_active_all  = new Checkbox("Show All Active ", false);      
//	private Checkbox labelsCheckbox = new Checkbox("Edit Mode", false);
        public java.util.ArrayList[] m_Slice_ROIs;

	private static boolean measureAll = true;
	private static boolean onePerSlice = true;
	private static boolean restoreCentered;
	private int prevID;
	private boolean noUpdateMode;
	private int defaultLineWidth = 1;
	private Color defaultColor;
	private boolean firstTime = true;
	private int[] selectedIndexes;
        
        ImagePlus m_mapImg = null;
        ImagePlus m_maskImg = null;
        ImagePlus m_CounterImg = null;
        
        NucleiImageCanvas ic = null;
        
        
        
        //Vivek
        public class OverlapROI
        {
            public RoiWithStat m_OverlapRoi;
            public double m_OverlapArea;
            public double m_OverlapAreaPer;
            public boolean bKeep = false;
            
            public OverlapROI()
            {
            }
        }
        
        //
        public class IEG_Moment_Calc // based on plugin from Francois Richard  University of Ottawa - Earth Sciences
        {
            ImagePlus imp;
            boolean done;
            boolean firstTime = true;
            double dCutoff = 0.0; // default cutoff (minimum) value for calcs
                           //  (only values >= dCutoff are used)
                           //  (use "0" to include all positive pixel values)
            double dFactor = 1.0; // default factor                              
                           //  (multiplies pixel values prior to calculations)
              
            double zero = 0.0;
            double m00 = zero;
            double m10 = zero, m01 = zero;
            double m20 = zero, m02 = zero, m11 = zero;
            double m30 = zero, m03 = zero, m21 = zero, m12 = zero;
            double m40 = zero, m04 = zero, m31 = zero, m13 = zero;
            double xC=zero, yC=zero;
            double xxVar = zero, yyVar = zero, xyVar = zero;
            double xSkew = zero, ySkew = zero;
            double xKurt = zero, yKurt = zero;
            double orientation = zero, eccentricity = zero;
            double currentPixel, xCoord, yCoord;


            public IEG_Moment_Calc(ImagePlus imp) 
            {
                this.imp = imp;
            }
            
            /*
                ** Interpretation of spatial moments **

                * order 0  = TOTAL MASS [units: concentration, density, etc.]
                * order 1  = location of CENTRE OF MASS in x and y from 0,0 [units: L]
                * order 2  = VARIANCE around centroid in x and y [units: L^2]
                * order 3  = coeff. of SKEWNESS (symmetry) in x and y [units: n/a]
                                 -->  =0  : SYMMETRIC distribution
                                 -->  <0  : Distribution asymmetric to the LEFT
                                           (tail extends left of centre of mass)
                                 -->  >0  : Distribution asymmetric to the RIGHT
                                           (tail extends right of centre of mass)
                * order 4  = KURTOSIS (flatness) in x and y [units: n/a]
                                 --> =0   : Gaussian (NORMAL) distribution
                                 --> <0   : Distribution FLATTER than normal
                                 --> >0   : Distribution MORE PEAKED than normal
                                 --> <-1.2: BIMODAL (or multimodal) distribution

                ** Parameters derived from 2nd moments ** (from Awcock (1995)
                      "Applied Image Processing")

                  * ELONGATION (ECCENTRICITY) = Ratio of longest to shortest
                   distance vectors rom the object's centroid to its boundaries
                  * ORIENTATION = For elongated objects, describes the
                    orientation (in degrees) of the "long" direction with
                    respect to horizontal (x axis)
             */

            public void CalculateMoments(ImageProcessor ip)
            {
                String imageName = imp.getTitle();
                int width = ip.getWidth();
                int height = ip.getHeight();
                double pw = 1;
                double ph = 1;
/*                boolean isScaled = false;
                boolean isCalibrated = false;
                String calUnits = "pixels";
                String units = "";
                Roi roi = imp.getRoi();*/
                Rectangle r = ip.getRoi();
                byte[] mask = ip.getMaskArray();
                int maskCounter = 0;

                
                // Compute moments of order 0 & 1

                for (int y=r.y; y<(r.y+r.height); y++) {
                  for (int x=r.x; x<(r.x+r.width); x++) {
                     if (mask==null || mask[maskCounter++]!=0) {
                       xCoord = (x+0.5)*pw; //this pixel's X calibrated coord. (e.g. cm)
                       yCoord = (y+0.5)*ph; //this pixel's Y calibrated coord. (e.g. cm)
                       currentPixel=ip.getPixelValue(x,y);
                       currentPixel=currentPixel-dCutoff;
                       if (currentPixel < 0) currentPixel = zero; //gets rid of negative pixel values
                       currentPixel = dFactor*currentPixel;
             /*0*/       m00+=currentPixel;
             /*1*/       m10+=currentPixel*xCoord;
                         m01+=currentPixel*yCoord;
                     }
                  }
                }

                //
                // Compute coordinates of centre of mass

                xC = m10/m00;
                yC = m01/m00;

                // Compute moments of orders 2, 3, 4

                 // Reset index on "mask"
                maskCounter = 0;
                for (int y=r.y; y<(r.y+r.height); y++) {
                  for (int x=r.x; x<(r.x+r.width); x++) {
                     if (mask==null || mask[maskCounter++]!=0) {
                       xCoord = (x+0.5)*pw; //this pixel's X calibrated coord. (e.g. cm)
                       yCoord = (y+0.5)*ph; //this pixel's Y calibrated coord. (e.g. cm)
                       currentPixel=ip.getPixelValue(x,y);
                       currentPixel=currentPixel-dCutoff;
                       if (currentPixel < 0) currentPixel = zero; //gets rid of negative pixel values
                       currentPixel = dFactor*currentPixel;
                /*2*/       m20+=currentPixel*(xCoord-xC)*(xCoord-xC);
                         m02+=currentPixel*(yCoord-yC)*(yCoord-yC);
                         m11+=currentPixel*(xCoord-xC)*(yCoord-yC);

                /*3*/       m30+=currentPixel*(xCoord-xC)*(xCoord-xC)*(xCoord-xC);
                         m03+=currentPixel*(yCoord-yC)*(yCoord-yC)*(yCoord-yC);
                         m21+=currentPixel*(xCoord-xC)*(xCoord-xC)*(yCoord-yC);
                         m12+=currentPixel*(xCoord-xC)*(yCoord-yC)*(yCoord-yC);

                /*4*/       m40+=currentPixel*(xCoord-xC)*(xCoord-xC)*(xCoord-xC)*(xCoord-xC);
                         m04+=currentPixel*(yCoord-yC)*(yCoord-yC)*(yCoord-yC)*(yCoord-yC);
                         m31+=currentPixel*(xCoord-xC)*(xCoord-xC)*(xCoord-xC)*(yCoord-yC);
                         m13+=currentPixel*(xCoord-xC)*(yCoord-yC)*(yCoord-yC)*(yCoord-yC);
                     }
                  }
                }

                // Normalize 2nd moments & compute VARIANCE around centre of mass
                xxVar = m20/m00;
                yyVar = m02/m00;
                xyVar = m11/m00;
                
              // Normalize 2nd moments & compute VARIANCE around centre of mass
                xxVar = m20/m00;
                yyVar = m02/m00;
                xyVar = m11/m00;

              // Normalize 3rd moments & compute SKEWNESS (symmetry) around centre of mass
              // source: Farrell et al, 1994, Water Resources Research, 30(11):3213-3223
                xSkew = m30 / (m00 * Math.pow(xxVar,(3.0/2.0)));
                ySkew = m03 / (m00 * Math.pow(yyVar,(3.0/2.0)));

              // Normalize 4th moments & compute KURTOSIS (peakedness) around centre of mass
              // source: Farrell et al, 1994, Water Resources Research, 30(11):3213-3223
                xKurt = m40 / (m00 * Math.pow(xxVar,2.0)) - 3.0;
                yKurt = m04 / (m00 * Math.pow(yyVar,2.0)) - 3.0;

              // Compute Orientation and Eccentricity
              // source: Awcock, G.J., 1995, "Applied Image Processing", pp. 162-165
                orientation = 0.5*Math.atan2((2.0*m11),(m20-m02));
                orientation = orientation*180./Math.PI; //convert from radians to degrees
                eccentricity = (Math.pow((m20-m02),2.0)+(4.0*m11*m11))/m00;
                
            }
            
            public void printMoments()
            {
                IJ.log( "<<M>>" 
                        + "<cutOff> " + roundTwoDecimals(dCutoff)                 
                        + "<Factor> " + roundTwoDecimals(dFactor)                 

                        + "<xC> " + roundTwoDecimals(xC)                 
                        + "<yC> " + roundTwoDecimals(yC)                 
                        
                        + "<xxVar> " + roundTwoDecimals(xxVar)                 
                        + "<yyVar> " + roundTwoDecimals(yyVar)
                        + "<xyVar> " + roundTwoDecimals(xyVar)

                        + "<xSkew> " + roundTwoDecimals(xSkew)                 
                        + "<ySkew> " + roundTwoDecimals(ySkew)
                        
                        + "<xKurt> " + roundTwoDecimals(xKurt)
                        + "<yKurt> " + roundTwoDecimals(yKurt)
                        
                        + "<Orient> " + roundTwoDecimals(orientation)
                        + "<Eccent> " + roundTwoDecimals(eccentricity)
                  );
            }
              
              
            
        }

        String roundTwoDecimals(double d) {
            DecimalFormat twoDForm = new DecimalFormat("#.##");
            return twoDForm.format(d);
        }
        
        
        public class RoiWithStat
        {
            public int m_id;
            public boolean m_IsSeed = false;
            public boolean m_IsProcessed = false;
            
            public int cx = 0;
            public int cy = 0;
            public int cz = 0;
            public int cid = 0;

            
            public boolean m_IsUsable = true;
            
            public IEGPolygonRoi m_Roi;
            public ImageStatistics  m_RoiStat;
            public ImagePlus m_Image;
            public Point[] m_PolyPoints;
            public double m_Circularity;
            public double m_AxisAspectRatio;
            public double m_Solidity;
            public double m_Roundness;
            public int m_3DLevels = 0;
            
            public boolean bSub = false;
            
            public IEG_Moment_Calc m_Moments;
            
            public String m_Name;
            
            public java.util.ArrayList m_Over_ROIs_b = new java.util.ArrayList();
            public java.util.ArrayList m_Over_ROIs_f__a_b = new java.util.ArrayList();
            public java.util.ArrayList m_Over_ROIs_f__b_a = new java.util.ArrayList();
            public java.util.ArrayList m_3D_f = new java.util.ArrayList();
            
            public int getListId()
            {
                int sId = m_Name.lastIndexOf("-");
                sId++;
               // int sIdLen = m_Name.length() - sId;
                String strId = m_Name.substring(sId, m_Name.length());
                
                if (isInteger(strId))
                {
                    return Integer.parseInt(strId);
                }
                else
                {
                    return -1;
                }
            }
            
            public boolean IsActive(int sliceId)
            {
                if (cz == sliceId)
                    return true;
                for ( int i = 0; i < m_Over_ROIs_f__a_b.size(); i++ )
                {
                    IEG_Nuclei_Counter.RoiWithStat roiS = (IEG_Nuclei_Counter.RoiWithStat)m_Over_ROIs_f__a_b.get(i);
                    if (roiS.m_Roi.getPosition()==sliceId)
                        return true;

                }
                
                return false;
            }
            
            public RoiWithStat(PolygonRoi r, ImagePlus img)
            {
                m_Roi = (IEGPolygonRoi)r;
                if (r==null)
                {
                    int iii=0;
                }
                
                
                if ( m_Roi.getName() != null && !m_Roi.getName().isEmpty())
                {
                    PopulateCField(m_Roi.getName());   
                }
                
                m_Image = img;
                m_Roi.setImage(img);
                CalcRoiStat(m_Image);
                calcPolyPoints();
                
            }

            public RoiWithStat(IEGPolygonRoi r, ImagePlus img) 
            {
                m_Roi = r;
                if (r==null)
                {
                    int iii=0;
                }
                
                
                if ( m_Roi.getName() != null && !m_Roi.getName().isEmpty())
                {
                    PopulateCField(m_Roi.getName());   
                }
                
                m_Image = img;
                m_Roi.setImage(img);
                CalcRoiStat(m_Image);
                calcPolyPoints();
            }
            
            public void PopulateCField(String rName)
            {
                String[] splitNames = rName.split("-");
                
                cz = Integer.parseInt(splitNames[0]);
                cy = Integer.parseInt(splitNames[1]);
                cx = Integer.parseInt(splitNames[2]);
                //cid = Integer.parseInt(splitNames[3]);
            }
            
            void CalcRoiStat(ImagePlus img)
            {
                /*ImageProcessor ip = img.getProcessor();
                ip.setRoi(img.getRoi());
                m_RoiStat = ImageStatistics.getStatistics(ip, Measurements.MEAN, img.getCalibration());*/
                img.setSlice(m_Roi.getPosition());
                img.setRoi(m_Roi);
                ImageProcessor ip1 = img.getProcessor();
                prepareProcessor(ip1, img);
                
                m_RoiStat = img.getStatistics(2091775);
                
                m_Moments = new IEG_Moment_Calc(img);
                m_Moments.CalculateMoments(ip1);
                
                double perimeter = m_Roi.getLength();
                
                m_Circularity = perimeter==0.0?0.0:4.0*Math.PI*(m_RoiStat.area/(perimeter*perimeter));
		if (m_Circularity>1.0) 
                    m_Circularity = 1.0;
                
                Polygon ch = null;
                boolean isArea = m_Roi==null || m_Roi.isArea();
                double convexArea = m_Roi!=null?getArea_1(m_Roi.getConvexHull()):m_RoiStat.pixelCount;
                m_AxisAspectRatio = isArea?m_RoiStat.major/m_RoiStat.minor:0.0;
                m_Solidity = isArea?4.0*m_RoiStat.area/(Math.PI*m_RoiStat.major*m_RoiStat.major):0.0;
                m_Roundness = isArea?m_RoiStat.pixelCount/convexArea:Double.NaN;
                

                
                img.killRoi();
                ip1.resetRoi();
            }
            
            private void calcPolyPoints()
            {
                int totPoints = m_Roi.getPolygon().npoints;
                java.awt.Polygon plg = m_Roi.getPolygon();
                m_PolyPoints = new Point[totPoints];
                
                for (int i =0; i <totPoints;i++)
                {
                    m_PolyPoints[i] = new Point(plg.xpoints[i], plg.ypoints[i]);
                    
                }
            }
            
	    private double getArea_1(Polygon p) 
            {
		if (p==null) return Double.NaN;
		int carea = 0;
		int iminus1;
		for (int i=0; i<p.npoints; i++) 
                {
			iminus1 = i-1;
			if (iminus1<0) iminus1=p.npoints-1;
			carea += (p.xpoints[i]+p.xpoints[iminus1])*(p.ypoints[i]-p.ypoints[iminus1]);
		}
		return (Math.abs(carea/2.0));
            }   
            
            
            
        }
        //end Vivek
	private void prepareProcessor(ImageProcessor ip, ImagePlus imp) {
		ImageProcessor mask = imp.getMask();
		Roi roi = imp.getRoi();
		if (roi!=null && roi.isArea())
			ip.setRoi(roi);
		else
			ip.setRoi((Roi)null);
		if (imp.getStackSize()>1) {
			ImageProcessor ip2 = imp.getProcessor();
			double min1 = ip2.getMinThreshold();
			double max1 = ip2.getMaxThreshold();
			double min2 = ip.getMinThreshold();
			double max2 = ip.getMaxThreshold();
			if (min1!=ImageProcessor.NO_THRESHOLD && (min1!=min2||max1!=max2))
				ip.setThreshold(min1, max1, ImageProcessor.NO_LUT_UPDATE);
		}
		//float[] cTable = imp.getCalibration().getCTable();
		//ip.setCalibrationTable(cTable);
	}

        public static boolean isInteger( String input )  
        {  
           try  
           {  
              Integer.parseInt( input );  
              return true;  
           }  
           catch( Exception e)  
           {  
              return false;  
           }  
        }
        
        
        //Key Listner 
        public void keyTyped(KeyEvent e) {
            //displayInfo(e, "KEY TYPED: ");
            int i =0;
        }

        /** Handle the key-pressed event from the text field. */
        public void keyPressed(KeyEvent e) {
            //displayInfo(e, "KEY PRESSED: ");
            int i =0;
        }

        public void keyReleased(KeyEvent e) {
            Object source = e.getSource();
            if (source == list)
            {
                int index = list.getSelectedIndex();
                if (!IJ.shiftKeyDown() && !IJ.controlKeyDown()) 
                {  //simple click, deselect everything else
                        int[] indexes = getSelectedIndexes();
                        for (int i=0; i<indexes.length; i++) 
                        {
                             list.deselect(indexes[i]);
                        }
                }
                
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN)
                {
                    index++;
                    if (index >= list.getItemCount())
                    {
                        index = list.getItemCount()-1;
                    }
                }
                else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP)
                {
                    index--;
                    if (index < 0)
                    {
                        index = 0;
                    }
                }
                list.select(index);
                if (WindowManager.getCurrentImage()!=null)
                        restore(getImage(), index, true);
                
            }
        }        
        
        //---------------------------------------------------------------------
	
	public IEG_Nuclei_Counter() {
		super("IEG Nuclei Counter V"+IEG_Analysis.IEG_VER); //LM, 8Jan2013);
		if (instance!=null) {
			WindowManager.toFront(instance);
			return;
		}
		instance = this;
		list = new List(rows, allowMultipleSelections);
		showWindow();
	}
	
	public IEG_Nuclei_Counter(boolean hideWindow) {
		super("IEG Nuclei Counter");
		list = new List(rows, allowMultipleSelections);
	}
        
        public IEG_Nuclei_Counter GetInstance()
        {
            if (instance==null)
            {
                instance = new IEG_Nuclei_Counter();
            }
            return (IEG_Nuclei_Counter)instance;
        }

	void showWindow() {
		ImageJ ij = IJ.getInstance();
 		addKeyListener(ij);
 		addMouseListener(this);
		addMouseWheelListener(this);
		WindowManager.addWindow(this);
		//setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
		setLayout(new BorderLayout());
		list.add("012345678901234");
		list.addItemListener(this);
// 		list.addKeyListener(ij);
 		list.addKeyListener(this);
 		list.addMouseListener(this);
 		list.addMouseWheelListener(this);
		if (IJ.isLinux()) list.setBackground(Color.white);
		add("Center", list);
		panel = new Panel();
		int nButtons = BUTTONS;
		panel.setLayout(new GridLayout(20, 1, 20, 10));
//		addButton("Add [t]");
		addButton("Reset");
		addButton("Update");
		addButton("Delete");
		addButton("Rename...");
		addButton("Measure");
		addButton("Deselect");
		addButton("Properties...");
		addButton("Flatten [F]");
		addButton(moreButtonLabel);
                
                showCheckbox_active_all.addItemListener(this);
                panel.add(showCheckbox_active_all);
                showCheckbox_active_all.setEnabled(true);

                showCheckbox_active.addItemListener(this);
                panel.add(showCheckbox_active);
                showCheckbox_active.setEnabled(true);
                
		showAllCheckbox.addItemListener(this);
		panel.add(showAllCheckbox);
                showAllCheckbox.setEnabled(true);
                
		labelsCheckbox.addItemListener(this);
                labelsCheckbox.setEnabled(false);
		panel.add(labelsCheckbox);
		add("East", panel);		
		addPopupMenu();
		pack();
		Dimension size = getSize();
		if (size.width>270)
			setSize(size.width-40, size.height);
		list.remove(0);
		Point loc = Prefs.getLocation(LOC_KEY);
		if (loc!=null)
			setLocation(loc);
		else
			GUI.center(this);
		show();
		if (IJ.isMacOSX() && IJ.isJava16()) {
			list.setMultipleMode(false);
			list.setMultipleMode(true);
			//EventQueue.invokeLater(new Runnable() {
			//	public void run() {
			//		list.setMultipleMode(false);
			//		list.setMultipleMode(true);
			//	}
			//});
		}
	}

	void addButton(String label) {
		Button b = new Button(label);
		b.addActionListener(this);
		b.addKeyListener(IJ.getInstance());
 		b.addMouseListener(this);
 		if (label.equals(moreButtonLabel)) moreButton = b;
		panel.add(b);
	}

	void addPopupMenu() {
		pm=new PopupMenu();
		//addPopupItem("Select All");
		addPopupItem("Open...");
		addPopupItem("Save...");
		addPopupItem("Fill");
		addPopupItem("Draw");
		addPopupItem("AND");
		addPopupItem("OR (Combine)");
		addPopupItem("XOR");
		addPopupItem("Split");
		addPopupItem("Add Particles");
		addPopupItem("Multi Measure");
		addPopupItem("Multi Plot");
		addPopupItem("Sort");
		addPopupItem("Specify...");
		addPopupItem("Remove Slice Info");
		addPopupItem("Help");
		addPopupItem("Options...");
		add(pm);
	}

	void addPopupItem(String s) {
		MenuItem mi=new MenuItem(s);
		mi.addActionListener(this);
		pm.add(mi);
	}
	
	public void actionPerformed(ActionEvent e) {
		String label = e.getActionCommand();
		if (label==null)
			return;
		String command = label;
		if (command.equals("Reset"))
                {
                    ic.bReset=true;
                    ic.repaint();
                }
		else if (command.equals("Update"))
			update(true);
		else if (command.equals("Delete"))
			delete(false);
		else if (command.equals("Rename..."))
			rename(null);
		else if (command.equals("Properties..."))
			setProperties(null, -1, null);
		else if (command.equals("Flatten [F]"))
			flatten();
		else if (command.equals("Measure"))
			measure(MENU);
		else if (command.equals("Open..."))
			open(null);
		else if (command.equals("Save..."))
			save();
		else if (command.equals("Fill"))
			drawOrFill(FILL);
		else if (command.equals("Draw"))
			drawOrFill(DRAW);
		else if (command.equals("Deselect"))
			select(-1);
		else if (command.equals(moreButtonLabel)) {
			Point ploc = panel.getLocation();
			Point bloc = moreButton.getLocation();
			pm.show(this, ploc.x, bloc.y);
		} else if (command.equals("OR (Combine)"))
			combine();
		else if (command.equals("Split"))
			split();
		else if (command.equals("AND"))
			and();
		else if (command.equals("XOR"))
			xor();
		else if (command.equals("Add Particles"))
			addParticles();
		else if (command.equals("Multi Measure"))
			multiMeasure();
		else if (command.equals("Multi Plot"))
			multiPlot();
		else if (command.equals("Sort"))
			sort();
		else if (command.equals("Specify..."))
			specify();
		else if (command.equals("Remove Slice Info"))
			removeSliceInfo();
		else if (command.equals("Help"))
			help();
		else if (command.equals("Options..."))
			options();
		else if (command.equals("\"Show All\" Color..."))
			setShowAllColor();
	}

	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();

                if ( source == showCheckbox_active_all )
                {
                    ic.bShowAll_only_active = showCheckbox_active_all.getState();
                    
                    ic.repaint();
                    return;
                        
                }
                
                if ( source == showCheckbox_active )
                {
                    ic.bShow_only_active = showCheckbox_active.getState();
                    
                    ic.repaint();
                    return;
                        
                }
		if (source==showAllCheckbox) {

                    ic.bShowAll = showAllCheckbox.getState();
                    
                    ic.repaint();
                    return;
		}
		if (source==labelsCheckbox) {
			if (firstTime)
				showAllCheckbox.setState(true);
			boolean editState = labelsCheckbox.getState();
			boolean showAllState = showAllCheckbox.getState();
			if (!showAllState && !editState)
				showAll(SHOW_NONE);
			else {
				showAll(editState?LABELS:NO_LABELS);
				if (editState) showAllCheckbox.setState(true);
			}
			firstTime = false;
			return;
		}
		if (e.getStateChange()==ItemEvent.SELECTED && !ignoreInterrupts) {
			int index = 0;
			//IJ.log("item="+e.getItem()+" shift="+IJ.shiftKeyDown()+" ctrl="+IJ. controlKeyDown());
            try {index = Integer.parseInt(e.getItem().toString());}
            catch (NumberFormatException ex) {}
			if (index<0) index = 0;
            if (!IJ.isMacintosh()) {      //handle shift-click, ctrl-click (on Mac, OS takes care of this)
                if (!IJ.shiftKeyDown()) lastNonShiftClick = index;
    			if (!IJ.shiftKeyDown() && !IJ.controlKeyDown()) {  //simple click, deselect everything else
    				int[] indexes = getSelectedIndexes();
    				for (int i=0; i<indexes.length; i++) {
    					if (indexes[i]!=index)
    						list.deselect(indexes[i]);
    				}
    			} else if (IJ.shiftKeyDown() && lastNonShiftClick>=0 && lastNonShiftClick<list.getItemCount()) {
                    int firstIndex = Math.min(index, lastNonShiftClick);
                    int lastIndex = Math.max(index, lastNonShiftClick);
    				int[] indexes = getSelectedIndexes();
    				for (int i=0; i<indexes.length; i++)
    					if (indexes[i]<firstIndex || indexes[i]>lastIndex)
    						list.deselect(indexes[i]);      //deselect everything else
                    for (int i=firstIndex; i<=lastIndex; i++)
                        list.select(i);                     //select range
                }
            }
            if (WindowManager.getCurrentImage()!=null) 
            {
                    restore(getImage(), index, true);
                    if (record()) {
                            if (Recorder.scriptMode())
                                    Recorder.recordCall("rm.select(imp, "+index+");");
                            else
                                    Recorder.record("roiManager", "Select", index);
                    }
            }
		}
	}
	
	void add(boolean shiftKeyDown, boolean altKeyDown) {
		if (shiftKeyDown)
			addAndDraw(altKeyDown);
		else if (altKeyDown)
			addRoi(true);
		else
			addRoi(false);
	}
	
	/** Adds the specified ROI. */
	public void addRoi(Roi roi) {
		addRoi(roi, false, null, -1);
	}
	
	boolean addRoi(boolean promptForName) {
		return addRoi(null, promptForName, null, -1);
	}

	boolean addRoi(Roi roi, boolean promptForName, Color color, int lineWidth) {
		ImagePlus imp = roi==null?getImage():WindowManager.getCurrentImage();
		if (roi==null) {
			if (imp==null)
				return false;
			roi = imp.getRoi();
			if (roi==null) {
				error("The active image does not have a selection.");
				return false;
			}
		}
		if (color==null && roi.getStrokeColor()!=null)
			color = roi.getStrokeColor();
		else if (color==null && defaultColor!=null)
			color = defaultColor;
		if (lineWidth<0) {
			int sw = (int)roi.getStrokeWidth();
			lineWidth = sw>1?sw:defaultLineWidth;
		}
		if (lineWidth>100) lineWidth = 1;
		int n = list.getItemCount();
		if (n>0 && !IJ.isMacro() && imp!=null) {
			// check for duplicate
			String label = list.getItem(n-1);
			Roi roi2 = (Roi)rois.get(label);
			if (roi2!=null) {
				int slice2 = getSliceNumber(roi2, label);
				if (roi.equals(roi2) && (slice2==-1||slice2==imp.getCurrentSlice()) 
                                        && imp.getID()==prevID && !Interpreter.isBatchMode())
					return false;
			}
		}
		prevID = imp!=null?imp.getID():0;
		String name = roi.getName();
		if (isStandardName(name))
			name = null;
		String label = name!=null?name:getLabel(imp, roi, -1);
		if (promptForName)
			label = promptForName(label);
		else
			label = getUniqueName(label);
		if (label==null) return false;
		list.add(label);
		roi.setName(label);
		Roi roiCopy = (Roi)roi.clone();
		if (lineWidth>1)
			roiCopy.setStrokeWidth(lineWidth);
		if (color!=null)
			roiCopy.setStrokeColor(color);
		rois.put(label, roiCopy);
		updateShowAll();
		if (record())
			recordAdd(defaultColor, defaultLineWidth);
		return true;
	}
	
	void recordAdd(Color color, int lineWidth) {
		if (Recorder.scriptMode())
			Recorder.recordCall("rm.addRoi(imp.getRoi());");
		else if (color!=null && lineWidth==1)
			Recorder.recordString("roiManager(\"Add\", \""+getHex(color)+"\");\n");
		else if (lineWidth>1)
			Recorder.recordString("roiManager(\"Add\", \""+getHex(color)+"\", "+lineWidth+");\n");
		else
			Recorder.record("roiManager", "Add");
	}
	
	String getHex(Color color) {
		if (color==null) color = ImageCanvas.getShowAllColor();
		String hex = Integer.toHexString(color.getRGB());
		if (hex.length()==8) hex = hex.substring(2);
		return hex;
	}
	
	/** Adds the specified ROI to the list. The third argument ('n') will 
		be used to form the first part of the ROI label if it is >= 0. */
	public void add(ImagePlus imp, Roi roi, int n) {
		if (roi==null) return;
		String label = roi.getName();
		if (label==null)
			label = getLabel(imp, roi, n);
		if (label==null) return;
		list.add(label);
		roi.setName(label);
		rois.put(label, (Roi)roi.clone());
	}

	boolean isStandardName(String name) {
		if (name==null) return false;
		boolean isStandard = false;
		int len = name.length();
		if (len>=14 && name.charAt(4)=='-' && name.charAt(9)=='-' )
			isStandard = true;
		else if (len>=17 && name.charAt(5)=='-' && name.charAt(11)=='-' )
			isStandard = true;
		else if (len>=9 && name.charAt(4)=='-')
			isStandard = true;
		else if (len>=11 && name.charAt(5)=='-')
			isStandard = true;
		return isStandard;
	}
	
	String getLabel(ImagePlus imp, Roi roi, int n) {
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

	void addAndDraw(boolean altKeyDown) {
		if (altKeyDown) {
			if (!addRoi(true)) return;
		} else if (!addRoi(false))
			return;
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp!=null) {
			Undo.setup(Undo.COMPOUND_FILTER, imp);
			IJ.run(imp, "Draw", "slice");
			Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
		}
		if (record()) Recorder.record("roiManager", "Add & Draw");
	}
	
	boolean delete(boolean replacing) {
		int count = list.getItemCount();
		if (count==0)
			return error("The list is empty.");
		int index[] = getSelectedIndexes();
		if (index.length==0 || (replacing&&count>1)) {
			String msg = "Delete all items on the list?";
			if (replacing)
				msg = "Replace items on the list?";
			canceled = false;
			if (!IJ.isMacro() && !macro) {
				YesNoCancelDialog d = new YesNoCancelDialog(this, "ROI Manager", msg);
				if (d.cancelPressed())
					{canceled = true; return false;}
				if (!d.yesPressed()) return false;
			}
			index = getAllIndexes();
		}
		if (count==index.length && !replacing) {
			rois.clear();
			list.removeAll();
		} else {
			for (int i=count-1; i>=0; i--) {
				boolean delete = false;
				for (int j=0; j<index.length; j++) {
					if (index[j]==i)
						delete = true;
				}
				if (delete) {
					rois.remove(list.getItem(i));
					list.remove(i);
				}
			}
		}
		ImagePlus imp = WindowManager.getCurrentImage();
		if (count>1 && index.length==1 && imp!=null)
			imp.killRoi();
		updateShowAll();
		if (record()) Recorder.record("roiManager", "Delete");
		return true;
	}
	
	boolean update(boolean clone) {
		ImagePlus imp = getImage();
		if (imp==null) return false;
		ImageCanvas ic = imp.getCanvas();
		boolean showingAll = ic!=null &&  ic.getShowAllROIs();
		Roi roi = imp.getRoi();
		if (roi==null) {
			error("The active image does not have a selection.");
			return false;
		}
		int index = list.getSelectedIndex();
		if (index<0 && !showingAll)
			return error("Exactly one item in the list must be selected.");
		if (index>=0) {
			String name = list.getItem(index);
			rois.remove(name);
			if (clone) {
				Roi roi2 = (Roi)roi.clone();
				int position = roi.getPosition();
				if (imp.getStackSize()>1)
					roi2.setPosition(imp.getCurrentSlice());
				roi.setName(name);
				roi2.setName(name);
				rois.put(name, roi2);
			} else
				rois.put(name, roi);
		}
		if (record()) Recorder.record("roiManager", "Update");
		if (showingAll) imp.draw();
		return true;
	}

	boolean rename(String name2) {
		int index = list.getSelectedIndex();
		if (index<0)
			return error("Exactly one item in the list must be selected.");
		String name = list.getItem(index);
		if (name2==null) name2 = promptForName(name);
		if (name2==null) return false;
		Roi roi = (Roi)rois.get(name);
		rois.remove(name);
		roi.setName(name2);
		rois.put(name2, roi);
		list.replaceItem(name2, index);
		list.select(index);
		if (Prefs.useNamesAsLabels && labelsCheckbox.getState()) {
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp!=null) imp.draw();
		}
		if (record())
			Recorder.record("roiManager", "Rename", name2);
		return true;
	}
	
	String promptForName(String name) {
		GenericDialog gd = new GenericDialog("ROI Manager");
		gd.addStringField("Rename As:", name, 20);
		gd.showDialog();
		if (gd.wasCanceled())
			return null;
		String name2 = gd.getNextString();
		name2 = getUniqueName(name2);
		return name2;
	}

	boolean restore(ImagePlus imp, int index, boolean setSlice) {
		String label = list.getItem(index);
		Roi roi = (Roi)rois.get(label);
		if (imp==null || roi==null)
			return false;
        if (setSlice) {
            int n = getSliceNumber(roi, label);
            if (n>=1 && n<=imp.getStackSize()) {
            	if (imp.isHyperStack()||imp.isComposite())
                	imp.setPosition(n);
                else
                	imp.setSlice(n);
            }
        }
        Roi roi2 = (Roi)roi.clone();
		Calibration cal = imp.getCalibration();
		Rectangle r = roi2.getBounds();
		int width= imp.getWidth(), height=imp.getHeight();
		if (restoreCentered) {
			ImageCanvas ic = imp.getCanvas();
			if (ic!=null) {
				Rectangle r1 = ic.getSrcRect();
				Rectangle r2 = roi2.getBounds();
				roi2.setLocation(r1.x+r1.width/2-r2.width/2, r1.y+r1.height/2-r2.height/2);
			}
		}
		if (r.x>=width || r.y>=height || (r.x+r.width)<=0 || (r.y+r.height)<=0)
			roi2.setLocation((width-r.width)/2, (height-r.height)/2);
		if (noUpdateMode) {
			imp.setRoi(roi2, false);
			noUpdateMode = false;
		} else
			imp.setRoi(roi2, true);
		return true;
	}
	
	boolean restoreWithoutUpdate(int index) {
		noUpdateMode = true;
		return restore(getImage(), index, false);
	}
	
	/** Returns the slice number associated with the specified name,
		or -1 if the name does not include a slice number. */
	public int getSliceNumber(String label) {
		int slice = -1;
		if (label.length()>=14 && label.charAt(4)=='-' && label.charAt(9)=='-')
			slice = (int)Tools.parseDouble(label.substring(0,4),-1);
		else if (label.length()>=17 && label.charAt(5)=='-' && label.charAt(11)=='-')
			slice = (int)Tools.parseDouble(label.substring(0,5),-1);
		else if (label.length()>=20 && label.charAt(6)=='-' && label.charAt(13)=='-')
			slice = (int)Tools.parseDouble(label.substring(0,6),-1);
		return slice;
	}
	
	/** Returns the slice number associated with the specified ROI or name,
		or -1 if the ROI or name does not include a slice number. */
	int getSliceNumber(Roi roi, String label) {
		int slice = roi!=null?roi.getPosition():-1;
		if (slice==0)
			slice=-1;
		if (slice==-1)
			slice = getSliceNumber(label);
		return slice;
	}

	void open(String path) {
		Macro.setOptions(null);
		String name = null;
		if (path==null || path.equals("")) {
			OpenDialog od = new OpenDialog("Open Selection(s)...", "");
			String directory = od.getDirectory();
			name = od.getFileName();
			if (name==null)
				return;
			path = directory + name;
		}
		if (record()) Recorder.record("roiManager", "Open", path);
		if (path.endsWith(".zip")) {
			openZip(path);
			return;
		}
		Opener o = new Opener();
		if (name==null) name = o.getName(path);
		Roi roi = o.openRoi(path);
		if (roi!=null) {
			if (name.endsWith(".roi"))
				name = name.substring(0, name.length()-4);
			name = getUniqueName(name);
			list.add(name);
			rois.put(name, roi);
		}		
		updateShowAll();
	}
        

	
	// Modified on 2005/11/15 by Ulrik Stervbo to only read .roi files and to not empty the current list
	void openZip(String path) { 
		ZipInputStream in = null; 
		ByteArrayOutputStream out;
                int temp = 0;
		int nRois = 0; 
                ImagePlus img = IJ.getImage();
		if (img==null)
                {
                    error("There are no images open."); return;
                }    
                else
                {
                   initializeImage(img);
                   if (m_CounterImg!=null)
                        img = m_CounterImg;
                   else
                   {
                        error("Unable to Initialize Image for Nuclei Counter."); return;                       
                   }
                       
                }
                
                img.setCalibration(null);
   //             Analyzer an = new Analyzer();

                m_Slice_ROIs = new java.util.ArrayList[100];
		try { 
			in = new ZipInputStream(new FileInputStream(path)); 
                       // initializeImage(img);
			byte[] buf = new byte[1024]; 
			int len; 
			ZipEntry entry = in.getNextEntry(); 
                        openZip1:
			while (entry!=null) { 
				String name = entry.getName();
				if (name.endsWith(".roi")) { 
					out = new ByteArrayOutputStream(); 
					while ((len = in.read(buf)) > 0) 
						out.write(buf, 0, len); 
					out.close(); 
					byte[] bytes = out.toByteArray(); 
					RoiDecoder rd = new RoiDecoder(bytes, name); 
                                        
                                        Roi roiOriginal = rd.getRoi(); 
					if (roiOriginal!=null) 
                                        { 
                                            String cType = roiOriginal.getClass().getName();
                                                if (cType.indexOf("PolygonRoi") <0 )
                                                    continue openZip1;
                                        }                                        
                                        
//					IEGPolygonRoi roi = (IEGPolygonRoi) roiOriginal;
					IEGPolygonRoi roi = new IEGPolygonRoi( roiOriginal.getPolygon(), 
                                                roiOriginal.getType() );
                                        roi.setName(roiOriginal.getName());
                                        roi.setStrokeColor(Color.ORANGE);
                                        roi.setPosition(roiOriginal.getPosition());
                                        roi.setPrototypeOverlay(roiOriginal.getPrototypeOverlay());
                                        boolean bSub = false;
					if (roi!=null) 
                                        { 
                                            String cType = roi.getClass().getName();
                                                if (cType.indexOf("IEGPolygonRoi") <0 )
                                                    continue openZip1;
                                                
                                                boolean bDelPName = false;
                                                String delName="";
                                                
                                                int idx = name.indexOf(".roi");
                                                if (idx >-1)
                                                    name = name.substring(0,idx);
                                                    
                                                idx = name.indexOf("_sub_0");
                                                if (idx>-1)
                                                {
                                                    delName = name.substring(0, idx);                                                    
                                                    delName = delName + "-" + Integer.toString(temp);
                                                    bDelPName = true;
                                                    
                                                }
                                                
                                                idx = name.indexOf("_sub_");
                                                if (idx>-1)
                                                {
                                                    name = getLabel(img, roi, -1);
                                                    bSub = true;
                                                }//  name = name.substring(0, name.length()-4); 
                                                String pName = name.substring(0, 4); 
						//name = getUniqueName(name); 
                                             //   int SliceNo = getSliceNumber(pName);
                                                int SliceNo = Integer.parseInt(pName);
                                                
                                                if (bDelPName) 
                                                {
                                                    DeleteParentROI(delName, SliceNo);
                                                    temp--;
                                                }
                                                
                                                
                                                
                                                temp++;
                                              //  if (SliceNo>27)
                                                {
                                                    
                                                    name = name + "-" + Integer.toString(temp);
                                                   if ( name.indexOf("0040-0402-0015") >- 1 )
                                                   {
                                                       int k =0;
                                                       if (roi.contains(15, 17))
                                                       {
                                                           int cc=0;
                                                       }
                                                       if (roi.contains(17, 15))
                                                       {
                                                           int cc=0;
                                                       }
                                                       
                                                   }
                                                    
                                                    list.add(name); 
                                                    rois.put(name, roi); 
                                                    roi.setName(name);
                                                    nRois++;
                                                    if (m_Slice_ROIs[SliceNo] == null)
                                                    {
                                                        m_Slice_ROIs[SliceNo] = new java.util.ArrayList();
                                                    }
                                                    img.setSlice(SliceNo);
                                                    roi.setPosition(SliceNo);
                                                    RoiWithStat roiStat = new RoiWithStat(roi, img);
                                                    if (bSub)
                                                    {
                                                        roiStat.bSub = true;
                                                    }
                                                    roiStat.m_id = temp;
                                                    roiStat.m_IsUsable = true;
                                                    roiStat.m_Name = name;
                                                    m_Slice_ROIs[SliceNo].add(roiStat);
                                                    roi.m_pParent = roiStat;
                                                    roi_stats.put(name, roiStat);
                                                    
                                                    
                                                   // if (roiStat.m_Circularity < 0.75 || roiStat.m_RoiStat.area > m_maxArea)
                                                    boolean bt = false;
                                                    if (bt)
                                                    {
                                                     //   IJ.log("\\Clear");
                                                     //  if ( name.indexOf("0044-0454-0152") > -1 )
                                                       {
                                                           int k =0;
                                                      /* }
                                                        {*/
                                                          // ProcessSubRoi_Method1(roiStat);
                                                             
                                                            java.util.ArrayList subRois1 = ProcessSubRoi_Method1(roiStat);
                                                            java.util.ArrayList subRois = ProcessSubRoi_Method2(roiStat);
                                                             
                                                            java.util.ArrayDeque qRoiList = new ArrayDeque();
                                                            qRoiList.add(roiStat);
                                                            boolean bIsSubRois = false;
                                                             
                                                            int stX, stY;
                                                            boolean processSubRois = true;
                                                            if( subRois != null && subRois.size() > 0)
                                                            {
                                                                if (name.indexOf("0006-0068-0087")>-1)
                                                                {
                                                                    int vivek=0;
                                                                    vivek=5;
                                                                }
                                                                
                                                                //--------
                                                                // tag the original Roi
                                                                roiStat.m_IsUsable = false;
                                                                list.remove(name);
                                                                roi_stats.remove(name);
                                                                rois.remove(name);

                                                                name = roi.getName() + "_untouched";
                                                                roi.setName(name);
                                                                roiStat.m_Name = name;
                                                                list.add(name); 
                                                                rois.put(name, roi); 
                                                                //--------
                                                                
                                                                for ( int sIdx = 0; sIdx < subRois.size(); sIdx++ )
                                                                {
                                                                    RoiWithStat sRs = (RoiWithStat)subRois.get(sIdx);
                                                                    Rectangle r = roi.getBounds();
                                                                    stX = r.x;
                                                                    stY = r.y;
                                                                    temp++;
                                                                    nRois++;
                                                                    AddSubRoi(sRs,stX, stY, roi, sIdx, temp, SliceNo);
                                                                }

                                                                bIsSubRois = true;
                                                            }
                                                             /*   if ( subRois.size() == 1)
                                                                {
                                                                     RoiWithStat sRs = (RoiWithStat)subRois.get(0);
                                                                     if (sRs.m_Circularity < roiStat.m_Circularity )
                                                                         processSubRois = false;

                                                                     if (sRs.m_AxisAspectRatio > m_minAxisAspectRatio)
                                                                     {

                                                                     }
                                                                }

                                                                if (processSubRois)
                                                                {


                                                                        if (sRs.m_RoiStat.area >= m_minArea_1 && 
                                                                                sRs.m_Circularity >= m_minCircularity_1 )
                                                                        {
                                                                            temp++;
                                                                            nRois++;
                                                                            AddSubRoi(sRs,stX, stY, roi, sIdx, temp, SliceNo);
                                                                            bIsSubRois = true;
                                                                        }
                                                                        else if (sRs.m_RoiStat.area < m_minArea_1 && 
                                                                                sRs.m_Circularity >= m_minCircularity_2 )
                                                                        {
                                                                            temp++;
                                                                            nRois++;
                                                                            AddSubRoi(sRs,stX, stY, roi, sIdx, temp, SliceNo);
                                                                            bIsSubRois = true;
                                                                        }
                                                                        else if (sRs.m_RoiStat.area >= m_minArea_1)
                                                                        {
                                                                            Rectangle r1 = sRs.m_Roi.getBounds();
                                                                            sRs.m_Roi.setLocation(stX + r1.x, stY + r1.y);
                                                                            sRs.m_Roi.setPosition(roi.getPosition());

                                                                            qRoiList.add(sRs);
                                                                        }

                                                                     }
                                                                }
                                                            }*/
                                                                 
                                                             if (!bIsSubRois)
                                                             {
                                                                 list.remove(name); 
                                                                 rois.remove(name); 
                                                                 roi_stats.remove(name); 
                                                             }
                                                        }
                                                   }
                                                    
                                                    System.gc();
                                                    
                                                    
                                                }
					} 
				} //while
				entry = in.getNextEntry(); 
			} 
			in.close(); 
		} catch (IOException e) {error(e.toString());} 
		if(nRois==0)
                    error("This ZIP archive does not appear to contain \".roi\" files");
                
//------              
                
                
           //     CalcOverlappingROIs();
           //     
                boolean _3d = true;
                if (_3d)
                {
                Create3DObjects();
                
                
                int nCount = 0;
                for ( int i = 0; i < m_Slice_ROIs.length; i++ )
                {
                    if ( m_Slice_ROIs[i]==null )
                        continue;
                    temp:
                    for ( int j =0; j < m_Slice_ROIs[i].size(); j++ )
                    {
                         RoiWithStat roiS = (RoiWithStat)m_Slice_ROIs[i].get(j);
                        if (roiS.m_Name.indexOf("0001-0275-0175")>-1)
                        {
                            int vivek=0;
                            vivek=5;

                        }
                         
                        if (!roiS.m_IsSeed && roiS.m_IsProcessed )
                        {
                            //if (roiS.m_Name.lastIndexOf("_untouched") == -1 && roiS.m_Name.lastIndexOf("_sub_") == -1)
                            try
                            {
                                rois.remove(roiS.m_Name);
                                list.remove(roiS.m_Name);
                                roi_stats.remove(roiS.m_Name);
                                continue temp;
                            }
                            catch(Exception e)
                            {
                                int k = 0;
                                continue temp;
                            }
                        }
                        else if (!roiS.m_IsUsable)
                        {
                            try
                            {
                                rois.remove(roiS.m_Name);
                                list.remove(roiS.m_Name);
                            }
                            catch(Exception e)
                            {
                                int k = 0;
                                continue temp;
                            }
                        }
                        else if (roiS.m_3DLevels < m_min3DLevels)
                        {
                            try
                            {
                                rois.remove(roiS.m_Name);
                                list.remove(roiS.m_Name);
                            }
                            catch(Exception e)
                            {
                                int k = 0;
                                continue temp;
                            }
                            
                        }
                        else
                        {
                            try
                            {
                                nCount++;
                                if (!roiS.bSub)
                                {
                                    rois.remove(roiS.m_Name);
                                    list.remove(roiS.m_Name);
                                }
                                String newName = roiS.m_Name + "_" + Integer.toString(nCount);
                                roiS.m_Roi.m_bIsSubRoi = true;
                                rois.put(newName, roiS.m_Roi );
                                list.add(newName);
                            }
                            catch(Exception e)
                            {
                                int k = 0;
                                continue temp;
                            }
                        }
                        
                        
                        

                    }
                }
                }
                //
                
               /* IJ.log("total Nuclie:" +  list.getItemCount());
                for ( int i = 0; i < list.getItemCount() ; i++ )
                {
                    RoiWithStat rs = (RoiWithStat)roi_stats.get(list.getItem(i));
                    CheckROICorrelationIn3DObject((RoiWithStat)roi_stats.get(list.getItem(i)));
                    int xx =0;
                }*/
                
                
//------                
                
                
                
                ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{error("There are no images open."); return;}
                imp.setSlice(1);
		ImageCanvas ic1 = imp.getCanvas();
		if (ic1==null) return;

                updateShowAll();

                
                
            //    ic.showSelectiveROIs=true;
        	IJ.newImage("nu_map","16-bit black",imp.getWidth(), imp.getHeight(), imp.getStackSize());
        	m_mapImg = WindowManager.getCurrentImage();
                
        	//IJ.newImage("Nuclei_mask","16-bit black",imp.getWidth(), imp.getHeight(), imp.getStackSize());
        	//m_maskImg = WindowManager.getCurrentImage();
                
                m_maskImg = new ImagePlus("Nuclei_mask", m_mapImg.getStack());
                for ( int i = 1; i <= m_maskImg.getStackSize(); i++)
                {
                    m_maskImg.setSlice(i);
                    m_maskImg.getProcessor().setMinAndMax(0, 50);
                }
                m_maskImg.setSlice(1);
                m_maskImg.show();
                
                
                CreateMapImage();
                ic.setMapImage(m_mapImg);
                m_mapImg.hide();
                
                
                
                //initializeImage(imp);
	} 
        
        long m_totMapPixels = 0;
        void CreateMapImage()
        {
            try
            {
                IEGPolygonRoi[] pRois = getPolyRoisAsArray();
                for ( int i = 0; i < pRois.length; i++ )
                {
                    IJ.showStatus("Processing map :" + Integer.toString(i) + "/" + Integer.toString(pRois.length));
                    //IJ.log("Processing map :" + Integer.toString(i) +  "/" + Integer.toString(pRois.length));
                    RoiWithStat rsPoly = pRois[i].m_pParent;

                    int id = rsPoly.getListId();
                    if ( id > 0)
                    {
                        MarkRoiArea( rsPoly, id );

                        for ( int iSub = 0; iSub < rsPoly.m_Over_ROIs_f__a_b.size(); iSub++ )
                        {
                                IEG_Nuclei_Counter.RoiWithStat rsPSub = 
                                        (IEG_Nuclei_Counter.RoiWithStat)rsPoly.m_Over_ROIs_f__a_b.get(iSub);
                                MarkRoiArea( rsPSub, id );
                               /* IEGPolygonRoi pr = (IEGPolygonRoi)rsPSub.m_Roi;
                                roiRect = pr.getBounds();

                                m_mapImg.setSlice(rsPSub.cz);
                                mapIp = m_mapImg.getProcessor();

                                for ( int rH = roiRect.y; rH < roiRect.y + roiRect.height; rH++ )
                                {
                                    for ( int rW = roiRect.x; rW < roiRect.x + roiRect.width; rW++ )
                                    {
                                        if (pr.contains(rW, rH))
                                        {
                                            m_mapImg.getProcessor().setValue( rsPSub.m_id );
                                        }
                                    }
                                }*/
                        }
                    }
                }
            }
            catch(Exception e)
            {
                IJ.log("Nuclei map error...");
            }
            
            
            IJ.log("total pixels written on map image : "  + Long.toString( m_totMapPixels ));
          /*  m_mapImg.setTitle("Map Image");
            m_mapImg.updateAndDraw();
            m_mapImg.show();
            m_mapImg.changes=true;
            IJ.saveAs(m_mapImg, "Tif", "C:\\Users\\vivek.trivedi\\Desktop\\a\\map");*/
        }
        
        void MarkRoiArea(IEG_Nuclei_Counter.RoiWithStat rsPSub, int id)
        {
             //   RoiManager.RoiWithStat rsPSub = (RoiManager.RoiWithStat)rsPoly.m_Over_ROIs_f__a_b.get(i);
                IEGPolygonRoi pr = (IEGPolygonRoi)rsPSub.m_Roi;
                Rectangle roiRect = pr.getBounds();

                ImageStack stack=m_mapImg.getStack();
//                m_mapImg.setSlice(rsPSub.cz);
  //              ip=
                ImageProcessor mapIp =stack.getProcessor((int)rsPSub.cz);

                for ( int rH = roiRect.y; rH < roiRect.y + roiRect.height; rH++ )
                {
                    for ( int rW = roiRect.x; rW < roiRect.x + roiRect.width; rW++ )
                    {
                        if (pr.contains(rW, rH))
                        {
                           mapIp.set(rW, rH, id);
                           m_totMapPixels++;
                        }
                    }
                }
            
        }
        
        
        void DeleteParentROI(String name, int SliceNo)
        {
            //int idx = name.indexOf("_sub_0");
           // if ( idx >= 0 )
            {
               // String pName = name.substring(idx);
                RoiWithStat  roiStat = (RoiWithStat)m_Slice_ROIs[SliceNo].get(m_Slice_ROIs[SliceNo].size()-1);
                roiStat.m_IsUsable = false;
                list.remove(name);
                roi_stats.remove(name);
                rois.remove(name);
                
            }
                
        }
        
        
        
        
        ArrayList ProcessSubRoi_Method1(RoiWithStat roiStat)
        {
            ArrayList retRois = new ArrayList();
            ImagePlus img = IJ.getImage();    
            java.util.ArrayList subRois = getSubRois( img, img.getProcessor(), roiStat.m_Roi, m_minCircularity, m_minArea, roiStat.m_Name );
            if (subRois==null)
                return null;
            
            if( subRois != null && subRois.size() > 0)
            {
                if ( subRois.size() == 1)
                {
                     RoiWithStat sRs = (RoiWithStat)subRois.get(0);
                     if (sRs.m_Circularity > roiStat.m_Circularity)
                     {
                        if (sRs.m_RoiStat.area >= m_minArea_1 && 
                                sRs.m_Circularity >= m_minCircularity_1 )
                        {
                            retRois.add(sRs);
                        }
                        else if (sRs.m_RoiStat.area < m_minArea_1 && 
                                sRs.m_Circularity >= m_minCircularity_2 )
                        {
                            retRois.add(sRs);
                        }
                        if (sRs.m_AxisAspectRatio > m_minAxisAspectRatio)
                        {
                             int i = 0;
                             ArrayList subRois1 = ProcessSubRoi_Method2(sRs);
                             if (subRois1!=null)
                                 retRois.addAll(subRois1);
                        }
                     }
                }
                else
                {
                    for ( int sIdx = 0; sIdx < subRois.size(); sIdx++ )
                    {
                        RoiWithStat sRs = (RoiWithStat)subRois.get(sIdx);

                        if (sRs.m_RoiStat.area >= m_minArea_1 && 
                                sRs.m_Circularity >= m_minCircularity_1 )
                        {
                            retRois.add(sRs);
                        }
                        else if (sRs.m_RoiStat.area < m_minArea_1 && 
                                sRs.m_Circularity >= m_minCircularity_2 )
                        {
                            retRois.add(sRs);
                        }
                    }
                }
            }
            
            return (retRois.size()> 0 ? retRois : null);
        }
        
        ArrayList ProcessSubRoi_Method2(RoiWithStat roiStat)
        {
            ArrayList retRois = new ArrayList();
            
             java.util.ArrayDeque qRoiList = new ArrayDeque();
             qRoiList.add(roiStat);
             boolean bIsSubRois = false;
             
            int stX, stY;
            IEGPolygonRoi roi = roiStat.m_Roi;
            Rectangle r = roi.getBounds();
            stX = r.x;
            stY = r.y;


             while(qRoiList.size() > 0 )
             {
                 RoiWithStat rStat = (RoiWithStat)qRoiList.poll();
                 java.util.ArrayList subRois = rStat.m_Roi.getCleBasedSubRois();
                 boolean processSubRois = true;
                 if( subRois != null && subRois.size() > 0)
                 {

                    if ( subRois.size() == 1)
                    {
                         RoiWithStat sRs = (RoiWithStat)subRois.get(0);
                         if (sRs.m_Circularity > roiStat.m_Circularity )
                             retRois.add(sRs);

                         if (sRs.m_AxisAspectRatio > m_minAxisAspectRatio)
                         {
                             int i = 0;
                         }
                     }
                     else
                     {

                        for ( int sIdx = 0; sIdx < subRois.size(); sIdx++ )
                        {
                            RoiWithStat sRs = (RoiWithStat)subRois.get(sIdx);

                            if (sRs.m_RoiStat.area >= m_minArea_1 && 
                                    sRs.m_Circularity >= m_minCircularity_1 )
                            {
                                retRois.add(sRs);
                            }
                            else if (sRs.m_RoiStat.area < m_minArea_1 && 
                                    sRs.m_Circularity >= m_minCircularity_2 )
                            {
                                retRois.add(sRs);
                            }
                            else if (sRs.m_RoiStat.area >= m_minArea_1)
                            {
                                Rectangle r1 = sRs.m_Roi.getBounds();
                                sRs.m_Roi.setLocation(stX + r1.x, stY + r1.y);
                                sRs.m_Roi.setPosition(roi.getPosition());
                                qRoiList.add(sRs);
                            }
                        }
                     }
                 }
             }            
            
            return (retRois.size()> 0 ? retRois : null); 
        }
        
        void AddSubRoi(RoiWithStat sRs, int stX, int stY, IEGPolygonRoi roi, int sIdx, int rId, int SliceNo)
        {
          //  RoiWithStat sRs = (RoiWithStat)subRois.get(sIdx);
            ImagePlus img = IJ.getImage();
            sRs.m_Image = img;
            sRs.m_IsSeed = true;
            sRs.m_IsUsable = true;
            sRs.m_IsProcessed = false;
            IEGPolygonRoi roi1 = sRs.m_Roi;
            Rectangle r1 = roi1.getBounds();
            roi1.setLocation(stX + r1.x, stY + r1.y);
            roi1.setPosition(roi.getPosition());
            sRs.calcPolyPoints();
            String newName = roi1.getName() + "_sub_" + Integer.toString(sIdx+1);
            roi1.setName(newName);
            roi1.setImage(img);
            sRs.m_Name = newName;
            sRs.m_id = rId;
            m_Slice_ROIs[SliceNo].add(sRs);    
            roi1.m_pParent = sRs;
            list.add(newName); 
            rois.put(newName, sRs.m_Roi); 
            roi_stats.put(newName, sRs); 
            
        }
        
        
        void Create3DObjects()
        {
            ImagePlus img = IJ.getImage();            
            int StackSize = img.getStackSize();
            
            int colorId = 0;
            
            Create3DObjectsl1:
            for ( int i =0; i < m_Slice_ROIs.length; i++ )
            {
                if ( m_Slice_ROIs[i]==null )
                    continue Create3DObjectsl1;
                
                Create3DObjectsl2:
                for ( int j = 0; j < m_Slice_ROIs[i].size(); j++ )
                {
                   if (!((RoiWithStat)m_Slice_ROIs[i].get(j)).m_IsUsable)
                        continue;
                    
                    RoiWithStat roiS = (RoiWithStat)m_Slice_ROIs[i].get(j);
                    
                    if (roiS.m_Circularity < 0.65)
                    {
                        roiS.m_IsSeed = false;
                        continue Create3DObjectsl2;
                    }
                    
                    if (roiS.m_IsProcessed)
                    {
                        roiS.m_IsSeed = false;
                         continue Create3DObjectsl2;
                    }
                    
if (roiS.m_Name.indexOf("0011-0321-0421")>-1)
{
int vivek=0;
vivek=5;
}

                    roiS.m_3DLevels=0;
                   // roiS.m_3D_f.add(roiS);
                   // AddRoiTo3D(roiS, roiS);
                    if (roiS.cz <=StackSize - 4 )
                    {
                        AddRoiTo3D_CT(roiS, i+1);
                        if ( roiS.m_3DLevels >= m_min3DLevels )
                        {
                           // Check3DOverlap(roiS, 4, 70.0d );

                           // if ( roiS.m_3DLevels > m_min3DLevels ) //megic
                                roiS.m_IsSeed = true;
                                RoiWithStat roiS1 = (RoiWithStat)roiS.m_Over_ROIs_f__a_b.get(roiS.m_Over_ROIs_f__a_b.size()-1);
                                roiS1.m_IsUsable = true;
                                
                                AssignColorId(roiS, colorId);
                                
                                colorId++;
                                if ( colorId > 19 )
                                    colorId = 0;
                        }
                        else
                        {
                            roiS.m_IsSeed = false;
                        }
                    }
                    else
                       roiS.m_IsSeed = false; 
                }        
            }
        }
        
        void AssignColorId(RoiWithStat roiS1, int colorId)
        {
            roiS1.m_Roi.m_cIdx = colorId;
            for ( int i = 0; i < roiS1.m_Over_ROIs_f__a_b.size(); i++ )
            {
                RoiWithStat roiSub = (RoiWithStat)roiS1.m_Over_ROIs_f__a_b.get(i);
                if (!roiSub.m_IsUsable)
                    roiSub.m_Roi.m_cIdx = colorId;
            }
        }
        
        void AddRoiTo3D(RoiWithStat rs, RoiWithStat seedRoi)
        {
            if ( !rs.m_IsProcessed && rs.m_Over_ROIs_f__a_b.size() > 0)
            {
                seedRoi.m_3DLevels++;
                seedRoi.m_3D_f.add(((OverlapROI)rs.m_Over_ROIs_f__a_b.get(0)).m_OverlapRoi);
                AddRoiTo3D(((OverlapROI)rs.m_Over_ROIs_f__a_b.get(0)).m_OverlapRoi, seedRoi);
                ((OverlapROI)rs.m_Over_ROIs_f__a_b.get(0)).m_OverlapRoi.m_IsProcessed = true;
            }
        }

        void AddRoiTo3D_CT(RoiWithStat rs, int nxtLevel)
        {
            ImagePlus img = IJ.getImage();            
            int StackSize = img.getStackSize();
            int cAtp = 0;
            AddRoiTo3D_CT_1:
            for ( int i =nxtLevel; i <= StackSize && i < m_Slice_ROIs.length; i++ )
            {
                if (rs.m_3DLevels >= 6)
                    return;
                
                if (cAtp >= 2)
                    return;
                
                if (m_Slice_ROIs[i]==null)
                    continue  AddRoiTo3D_CT_1;
                
                AddRoiTo3D_CT_2:
                for ( int j = 0; j < m_Slice_ROIs[i].size(); j++ )
                {
                    RoiWithStat roiS1 = (RoiWithStat)m_Slice_ROIs[i].get(j);
                    if (!roiS1.m_IsUsable)
                        continue AddRoiTo3D_CT_2;
                    int cxDiff = Math.abs((rs.cx-roiS1.cx));
                    int cyDiff = Math.abs((rs.cy-roiS1.cy));
                    if ( cxDiff<=15 && cyDiff <= 15)
                    {
                        cAtp=0;
                        
                        roiS1.m_IsProcessed = true;
                        rs.m_Over_ROIs_f__a_b.add(roiS1);
                        
                        if (cAtp > 0)
                            rs.m_Over_ROIs_f__a_b.add(roiS1);
                        
                        rs.m_3DLevels++;
                        continue  AddRoiTo3D_CT_1;
                    }
                }    
                cAtp++;
            }            
        }
        
        
        void Check3DOverlap(RoiWithStat rs, int downLevel, double minOverlapPer)
        {
            int delId = -1;
            Check3DOverlap1:
            for ( int i = 0; i < rs.m_3D_f.size(); i++ )
            {
                RoiWithStat r1 = (RoiWithStat)rs.m_3D_f.get(i);
                Point2D[] p1 = r1.m_PolyPoints;
                Check3DOverlap2:
                for ( int j = i+1; j < ( i + downLevel ) && j < rs.m_3D_f.size(); j++ )
                {
                     RoiWithStat r2 = (RoiWithStat)rs.m_3D_f.get(j);
                     Point2D[] p2 = r2.m_PolyPoints;
                     
                     Double iArea = PolygonIntersect.intersectionArea(p1, p2);
                     double OverlapAreaPer_a_b = iArea * 100 / r1.m_RoiStat.area;
                     double OverlapAreaPer_b_a = iArea * 100 / r2.m_RoiStat.area;
                     
                     if (OverlapAreaPer_a_b < minOverlapPer || OverlapAreaPer_b_a < minOverlapPer )
                     {
                         delId = j;
                         MakeROIUnProcessed(r2);
                         break Check3DOverlap1;
                            
                     }
                }
            }
            
            if ( delId >= 0 )
            {
                int lSize = rs.m_3D_f.size();
                while(lSize > delId)
                {
                    rs.m_3D_f.remove(lSize-1);
                    lSize= rs.m_3D_f.size();
                    rs.m_3DLevels--;
                }
            }
            
        }
        
        void MakeROIUnProcessed(RoiWithStat rs)
        {
            rs.m_IsProcessed = false;
            for (int i =0; i < rs.m_Over_ROIs_f__a_b.size(); i++ )
            {
                ((OverlapROI)rs.m_Over_ROIs_f__a_b.get(i)).m_OverlapRoi.m_IsProcessed =false;
            }
            
        }
        
        void CalcOverlappingROIs()
        {
            for (int i = 0; i < m_Slice_ROIs.length;i++)
            {
                if ( m_Slice_ROIs[i]==null || m_Slice_ROIs[i].size() <= 0 )
                    continue;
                
                if ( i > 0 )
                {
                    for ( int j = 0; j < m_Slice_ROIs[i].size(); j++ )
                    {
                        IJ.log(i+"_"+j);
                        if (!((RoiWithStat)m_Slice_ROIs[i].get(j)).m_IsUsable)
                            continue;
                        
                        double mainArea1 = ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_RoiStat.area;
                        Point2D[] p1 = ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_PolyPoints;
                        
                        if (m_Slice_ROIs[i-1] == null || m_Slice_ROIs[i-1].size() <= 0 )
                            break;
                        for ( int k = 0; k < m_Slice_ROIs[i-1].size(); k++ )
                        {
                            if (!((RoiWithStat)m_Slice_ROIs[i-1].get(k)).m_IsUsable)
                                continue;
                            
                            
                            RoiWithStat rs= ((RoiWithStat)m_Slice_ROIs[i-1].get(k));
                            Point2D[] p2 = rs.m_PolyPoints;
                            
                            Double iArea = PolygonIntersect.intersectionArea(p1, p2);
                            
                            if (iArea>0)
                            {
                                OverlapROI a = new OverlapROI();
                                a.m_OverlapRoi =rs;
                                a.m_OverlapArea = iArea;
                                a.m_OverlapAreaPer=a.m_OverlapArea*100 / mainArea1;
                                
                                ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.add(a);
                                
                            }
                        }
                        
                        if ( ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.size()>0)
                        {
                            double mainArea = ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_RoiStat.area;
                            double[] AreaDiff = new double[ ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.size()];
                            int keepId=-1;
                            double minAreaDiff=mainArea*2;
                            
                            IJ.log( ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Roi.getName()+ " = " + ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_RoiStat.area + " Circularity:" + ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Circularity);

                            for ( int ii = 0; ii < ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.size();ii++)
                            {
                                OverlapROI rs =  (OverlapROI)((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.get(ii);
 //                               IJ.log("\t(b)>>"+ rs.m_OverlapRoi.m_Roi.getName() + " = " + rs.m_OverlapArea + " Circularity :"+ rs.m_OverlapRoi.m_Circularity);
                                
                                AreaDiff[ii] =  Math.abs( mainArea - rs.m_OverlapArea);
                                if ( minAreaDiff > AreaDiff[ii])
                                {
                                    minAreaDiff =  AreaDiff[ii];
                                    keepId = ii;
                                }
                            }
                            
                            
                            if (keepId > -1)
                            {
                                OverlapROI keepRoi =  (OverlapROI)((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.get(keepId); 
                              //  ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.clear();
                               // keepRoi.m_OverlapAreaPer = keepRoi.m_OverlapArea*100/mainArea;
               //                 if (keepRoi.m_OverlapAreaPer > 50.0)
                                {
                                  //  ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.add(keepRoi);
                                    for ( int ii = 0; ii < ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.size();ii++)
                                    {
                                        if (ii != keepId)
                                        {
                                            ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.remove(ii);
                                            ii=-1;
                                        }
                                        else
                                        {
                                            if (keepRoi.m_OverlapAreaPer< 50.0)
                                            {
                                                ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_b.remove(ii);
                                                keepId=-1;
                                                ii=-1;
                                            }
                                        }
                                    }
                                }
                            }
                            
                            
                            
                        }
                    }
                }

                if (i < m_Slice_ROIs.length-1)
                {
                    for ( int j = 0; j < m_Slice_ROIs[i].size(); j++ )
                    {
                        if (!((RoiWithStat)m_Slice_ROIs[i].get(j)).m_IsUsable)
                            continue;
                        
                        String roiName11 = ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Roi.getName();
//                           String roiName1 = rs.m_Roi.getName();

                        if (roiName11.indexOf("0013-0168-0072") >-1) 
                        {
                            int ix=0;
                        }
                        

                        
                        double mainArea1 = ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_RoiStat.area;
                        Point2D[] p1 = ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_PolyPoints;
                        
                        if (m_Slice_ROIs[i+1]==null || m_Slice_ROIs[i+1].size() <=0 )
                            break;
                        
                        for ( int k = 0; k < m_Slice_ROIs[i+1].size(); k++ )
                        {
                            if (!((RoiWithStat)m_Slice_ROIs[i+1].get(k)).m_IsUsable)
                                continue;
                            
                            RoiWithStat rs= ((RoiWithStat)m_Slice_ROIs[i+1].get(k));
                            Point2D[] p2 = rs.m_PolyPoints;
                            
                            Double iArea = PolygonIntersect.intersectionArea(p1, p2);
                            
//                       String roiName1 = ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Roi.getName();
                           String roiName1 = rs.m_Roi.getName();

                            if (roiName1.indexOf("0008-0094-0065") >-1)
                            {
                                int ix=0;
                            }

                            
                            if (iArea>0)
                            {
                                OverlapROI a_b = new OverlapROI();
                                a_b.m_OverlapRoi = rs;
                                a_b.m_OverlapArea = iArea;
                                a_b.m_OverlapAreaPer=a_b.m_OverlapArea*100/mainArea1;
                                
                                OverlapROI b_a = new OverlapROI();
                                b_a.m_OverlapRoi = rs;
                                b_a.m_OverlapArea = iArea;
                                b_a.m_OverlapAreaPer = b_a.m_OverlapArea*100/rs.m_RoiStat.area;
                                
                                
                                ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_f__a_b.add(a_b);
                                ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_f__b_a.add(b_a);
                                
                            }
                        }
                        
                        if ( ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_f__a_b.size() > 0 )
                        {
                            double mainArea = ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_RoiStat.area;
                            double[] AreaDiff = new double[ ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_f__a_b.size()];
                            int keepId=-1;
                            double minAreaDiff=mainArea*2;
                            

//                            IJ.log( ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Roi.getName() + " = " + ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_RoiStat.area);
                            IJ.log( ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Roi.getName()+ " = " + 
                                    ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_RoiStat.area + " Circularity:" + 
                                    ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Circularity);

                            for ( int ii = 0; ii < ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_f__a_b.size();ii++)
                            {
                                OverlapROI rs =  (OverlapROI)((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_f__a_b.get(ii);
                                //IJ.log("\t(f)>>"+ rs.m_OverlapRoi.m_Roi.getName() + " = " + rs.m_OverlapArea + " Circularity :"+ rs.m_OverlapRoi.m_Circularity);
                                
                                AreaDiff[ii] =  Math.abs( mainArea - rs.m_OverlapArea);
                                if ( minAreaDiff > AreaDiff[ii])
                                {
                                    minAreaDiff =  AreaDiff[ii];
                                    keepId = ii;
                                }
                            }
                            
                            if (keepId >= 0)
                            {
                                OverlapROI keepRoi =  (OverlapROI)((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_f__a_b.get(keepId); 
                                keepRoi.bKeep = true;
                                if (keepRoi.m_OverlapAreaPer < 80.0)
                                {
                                    OverlapROI rs__b_a = (OverlapROI)((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_f__b_a.get(keepId);
                                    if (rs__b_a.m_OverlapAreaPer < 80.0)
                                    {
                                        keepRoi.bKeep = false;
                                    }
                                }
                            }
                            
                            Iterator itr = ((RoiWithStat)m_Slice_ROIs[i].get(j)).m_Over_ROIs_f__a_b.iterator();  
                            while(itr.hasNext())
                            {
                                OverlapROI rs = (OverlapROI)(itr.next());
                                if (!rs.bKeep)
                                    itr.remove();
                            }  
                        }
                    }
                }
            }
        }

                                
        
	String getUniqueName(String name) {
			String name2 = name;
			int n = 1;
			Roi roi2 = (Roi)rois.get(name2);
			while (roi2!=null) {
				roi2 = (Roi)rois.get(name2);
				if (roi2!=null) {
					int lastDash = name2.lastIndexOf("-");
					if (lastDash!=-1 && name2.length()-lastDash<5)
						name2 = name2.substring(0, lastDash);
					name2 = name2+"-"+n;
					n++;
				}
				roi2 = (Roi)rois.get(name2);
			}
			return name2;
	}
	
        
        
        
	boolean save() {
		if (list.getItemCount()==0)
			return error("The selection list is empty.");
		int[] indexes = getSelectedIndexes();
		if (indexes.length==0)
			indexes = getAllIndexes();
		if (indexes.length>1)
			return saveMultiple(indexes, null);
		String name = list.getItem(indexes[0]);
		Macro.setOptions(null);
		SaveDialog sd = new SaveDialog("Save Selection...", name, ".roi");
		String name2 = sd.getFileName();
		if (name2 == null)
			return false;
		String dir = sd.getDirectory();
		Roi roi = (Roi)rois.get(name);
		rois.remove(name);
		if (!name2.endsWith(".roi")) name2 = name2+".roi";
		String newName = name2.substring(0, name2.length()-4);
		rois.put(newName, roi);
		roi.setName(newName);
		list.replaceItem(newName, indexes[0]);
		RoiEncoder re = new RoiEncoder(dir+name2);
		try {
			re.write(roi);
		} catch (IOException e) {
			IJ.error("ROI Manager", e.getMessage());
		}
		return true;
	}

	public boolean saveMultiple(int[] indexes, String path) {
		Macro.setOptions(null);
		if (path==null) {
			SaveDialog sd = new SaveDialog("Save ROIs...", "RoiSet", ".zip");
			String name = sd.getFileName();
			if (name == null)
				return false;
			if (!(name.endsWith(".zip") || name.endsWith(".ZIP")))
				name = name + ".zip";
			String dir = sd.getDirectory();
			path = dir+name;
		}
		try {
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
			RoiEncoder re = new RoiEncoder(out);
			for (int i=0; i<indexes.length; i++) {
				String label = list.getItem(indexes[i]);
				Roi roi = (Roi)rois.get(label);
				if (!label.endsWith(".roi")) label += ".roi";
        		zos.putNextEntry(new ZipEntry(label));
				re.write(roi);
				out.flush();
			}
			out.close();
		}
		catch (IOException e) {
			error(""+e);
			return false;
		}
		if (record()) Recorder.record("roiManager", "Save", path);
		return true;
	}
		
	boolean measure(int mode) {
		ImagePlus imp = getImage();
		if (imp==null)
			return false;
		int[] indexes = getSelectedIndexes();
		if (indexes.length==0)
			indexes = getAllIndexes();
        if (indexes.length==0) return false;
		boolean allSliceOne = true;
		for (int i=0; i<indexes.length; i++) {
			String label = list.getItem(indexes[i]);
			Roi roi = (Roi)rois.get(label);
			if (getSliceNumber(roi,label)>1) allSliceOne = false;
		}
		int measurements = Analyzer.getMeasurements();
		if (imp.getStackSize()>1)
			Analyzer.setMeasurements(measurements|Measurements.SLICE);
		int currentSlice = imp.getCurrentSlice();
		for (int i=0; i<indexes.length; i++) {
			if (restore(getImage(), indexes[i], !allSliceOne))
				IJ.run("Measure");
			else
				break;
		}
		imp.setSlice(currentSlice);
		Analyzer.setMeasurements(measurements);
		if (indexes.length>1)
			IJ.run("Select None");
		if (record()) Recorder.record("roiManager", "Measure");
		return true;
	}	
	
	/*
	void showIndexes(int[] indexes) {
		for (int i=0; i<indexes.length; i++) {
			String label = list.getItem(indexes[i]);
			Roi roi = (Roi)rois.get(label);
			IJ.log(i+" "+roi.getName());
		}
	}
	*/

	/* This method performs measurements for several ROI's in a stack
		and arranges the results with one line per slice.  By constast, the 
		measure() method produces several lines per slice.  The results 
		from multiMeasure() may be easier to import into a spreadsheet 
		program for plotting or additional analysis. Based on the multi() 
		method in Bob Dougherty's Multi_Measure plugin
		(http://www.optinav.com/Multi-Measure.htm).
	*/
 	boolean multiMeasure() {
		ImagePlus imp = getImage();
		if (imp==null) return false;
		int[] indexes = getSelectedIndexes();
		if (indexes.length==0)
			indexes = getAllIndexes();
        if (indexes.length==0) return false;
		int measurements = Analyzer.getMeasurements();

		int nSlices = imp.getStackSize();
		if (IJ.isMacro()) {
			if (nSlices>1) measureAll = true;
			onePerSlice = true;
		} else {
			GenericDialog gd = new GenericDialog("Multi Measure");
			if (nSlices>1)
				gd.addCheckbox("Measure All "+nSlices+" Slices", measureAll);
			gd.addCheckbox("One Row Per Slice", onePerSlice);
			int columns = getColumnCount(imp, measurements)*indexes.length;
			String str = nSlices==1?"this option":"both options";
			gd.setInsets(10, 25, 0);
			gd.addMessage(
				"Enabling "+str+" will result\n"+
				"in a table with "+columns+" columns."
			);
			gd.showDialog();
			if (gd.wasCanceled()) return false;
			if (nSlices>1)
				measureAll = gd.getNextBoolean();
			onePerSlice = gd.getNextBoolean();
		}
		if (!measureAll) nSlices = 1;
		int currentSlice = imp.getCurrentSlice();
		
		if (!onePerSlice) {
			int measurements2 = nSlices>1?measurements|Measurements.SLICE:measurements;
			ResultsTable rt = new ResultsTable();
			Analyzer analyzer = new Analyzer(imp, measurements2, rt);
			for (int slice=1; slice<=nSlices; slice++) {
				if (nSlices>1) imp.setSliceWithoutUpdate(slice);
				for (int i=0; i<indexes.length; i++) {
					if (restoreWithoutUpdate(indexes[i]))
						analyzer.measure();
					else
						break;
				}
			}
			rt.show("Results");
			if (nSlices>1) imp.setSlice(currentSlice);
			return true;
		}

		Analyzer aSys = new Analyzer(imp); //System Analyzer
		ResultsTable rtSys = Analyzer.getResultsTable();
		ResultsTable rtMulti = new ResultsTable();
		Analyzer aMulti = new Analyzer(imp, measurements, rtMulti); //Private Analyzer

		for (int slice=1; slice<=nSlices; slice++) {
			int sliceUse = slice;
			if(nSlices == 1)sliceUse = currentSlice;
			imp.setSliceWithoutUpdate(sliceUse);
			rtMulti.incrementCounter();
			int roiIndex = 0;
			for (int i=0; i<indexes.length; i++) {
				if (restoreWithoutUpdate(indexes[i])) {
					roiIndex++;
					aSys.measure();
					for (int j=0; j<=rtSys.getLastColumn(); j++){
						float[] col = rtSys.getColumn(j);
						String head = rtSys.getColumnHeading(j);
						String suffix = ""+roiIndex;
						Roi roi = imp.getRoi();
						if (roi!=null) {
							String name = roi.getName();
							if (name!=null && name.length()>0 && (name.length()<9||!Character.isDigit(name.charAt(0))))
								suffix = "("+name+")";
						}
						if (head!=null && col!=null && !head.equals("Slice"))
							rtMulti.addValue(head+suffix,rtSys.getValue(j,rtSys.getCounter()-1));
					}
				} else
					break;
			}
			//aMulti.displayResults();
			//aMulti.updateHeadings();
		}
		rtMulti.show("Results");

		imp.setSlice(currentSlice);
		if (indexes.length>1)
			IJ.run("Select None");
		if (record()) Recorder.record("roiManager", "Multi Measure");
		return true;
	}
	
	int getColumnCount(ImagePlus imp, int measurements) {
		ImageStatistics stats = imp.getStatistics(measurements);
		ResultsTable rt = new ResultsTable();
		Analyzer analyzer = new Analyzer(imp, measurements, rt);
		analyzer.saveResults(stats, null);
		int count = 0;
		for (int i=0; i<=rt.getLastColumn(); i++) {
			float[] col = rt.getColumn(i);
			String head = rt.getColumnHeading(i);
			if (head!=null && col!=null)
				count++;
		}
		return count;
	}
	
	void multiPlot() {
		ImagePlus imp = getImage();
		if (imp==null) return;
		int[] indexes = getSelectedIndexes();
		if (indexes.length==0) indexes = getAllIndexes();
		int n = indexes.length;
        if (n==0) return;
		Color[] colors = {Color.blue, Color.green, Color.magenta, Color.red, Color.cyan, Color.yellow};
		if (n>colors.length) {
			colors = new Color[n];
			double c = 0;
			double inc =150.0/n;
			for (int i=0; i<n; i++) {
				colors[i] = new Color((int)c, (int)c, (int)c);
				c += inc;
			}
		}
		int currentSlice = imp.getCurrentSlice();
		double[][] x = new double[n][];
		double[][] y = new double[n][];
		double minY = Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		int maxX = 0;
		Calibration cal = imp.getCalibration();
		double xinc = cal.pixelWidth;
		for (int i=0; i<indexes.length; i++) {
			if (!restore(getImage(), indexes[i], true)) break;
			Roi roi = imp.getRoi();
			if (roi==null) break;
			if (roi.isArea() && roi.getType()!=Roi.RECTANGLE)
				IJ.run(imp, "Area to Line", "");
			ProfilePlot pp = new ProfilePlot(imp, IJ.altKeyDown());
			y[i] = pp.getProfile();
			if (y[i]==null) break;
			if (y[i].length>maxX) maxX = y[i].length;
			double[] a = Tools.getMinMax(y[i]);
			if (a[0]<minY) minY=a[0];
			if (a[1]>maxY) maxY = a[1];
			double[] xx = new double[y[i].length];
			for (int j=0; j<xx.length; j++)
				xx[j] = j*xinc;
			x[i] = xx;
		}
		String xlabel = "Distance ("+cal.getUnits()+")";
		Plot plot = new Plot("Profiles",xlabel, "Value", x[0], y[0]);
		plot.setLimits(0, maxX*xinc, minY, maxY);
		for (int i=1; i<indexes.length; i++) {
			plot.setColor(colors[i]);
			if (x[i]!=null)
				plot.addPoints(x[i], y[i], Plot.LINE);
		}
		plot.setColor(colors[0]);
		if (x[0]!=null)
			plot.show();
		imp.setSlice(currentSlice);
		if (indexes.length>1)
			IJ.run("Select None");
		if (record()) Recorder.record("roiManager", "Multi Plot");
	}	

	boolean drawOrFill(int mode) {
		int[] indexes = getSelectedIndexes();
		if (indexes.length==0)
			indexes = getAllIndexes();
		ImagePlus imp = WindowManager.getCurrentImage();
		imp.killRoi();
		ImageProcessor ip = imp.getProcessor();
		ip.setColor(Toolbar.getForegroundColor());
		ip.snapshot();
		Undo.setup(Undo.FILTER, imp);
		Filler filler = mode==LABEL?new Filler():null;
		int slice = imp.getCurrentSlice();
		for (int i=0; i<indexes.length; i++) {
			String name = list.getItem(indexes[i]);
			Roi roi = (Roi)rois.get(name);
			int type = roi.getType();
			if (roi==null) continue;
			if (mode==FILL&&(type==Roi.POLYLINE||type==Roi.FREELINE||type==Roi.ANGLE))
				mode = DRAW;
            int slice2 = getSliceNumber(roi, name);
            if (slice2>=1 && slice2<=imp.getStackSize()) {
                imp.setSlice(slice2);
				ip = imp.getProcessor();
				ip.setColor(Toolbar.getForegroundColor());
				if (slice2!=slice) Undo.reset();
            }
 			switch (mode) {
				case DRAW: roi.drawPixels(ip); break;
				case FILL: ip.fill(roi); break;
				case LABEL:
					roi.drawPixels(ip);
					filler.drawLabel(imp, ip, i+1, roi.getBounds());
					break;
			}
		}
		ImageCanvas ic = imp.getCanvas();
		if (ic!=null) ic.setShowAllROIs(false);
		imp.updateAndDraw();
		return true;
	}

	void setProperties(Color color, int lineWidth, Color fillColor) {
		boolean showDialog = color==null && lineWidth==-1 && fillColor==null;
		int[] indexes = getSelectedIndexes();
		if (indexes.length==0)
			indexes = getAllIndexes();
		int n = indexes.length;
        if (n==0) return;
		Roi rpRoi = null;
		String rpName = null;
		Font font = null;
		int justification = TextRoi.LEFT;
		double opacity = -1;
        if (showDialog) {
			String label = list.getItem(indexes[0]);
			rpRoi = (Roi)rois.get(label);
			if (n==1) {
				fillColor =  rpRoi.getFillColor();
				rpName = rpRoi.getName();
			}
			if (rpRoi.getStrokeColor()==null)
				rpRoi.setStrokeColor(ImageCanvas.getShowAllColor());
			rpRoi = (Roi) rpRoi.clone();
			if (n>1)
				rpRoi.setName("range: "+(indexes[0]+1)+"-"+(indexes[n-1]+1));
			rpRoi.setFillColor(fillColor);
			RoiProperties rp = new RoiProperties("Properties", rpRoi);
			if (!rp.showDialog())
				return;
			lineWidth =  (int)rpRoi.getStrokeWidth();
			defaultLineWidth = lineWidth;
			color =  rpRoi.getStrokeColor();
			fillColor =  rpRoi.getFillColor();
			defaultColor = color;
			if (rpRoi instanceof TextRoi) {
				font = ((TextRoi)rpRoi).getCurrentFont();
				justification = ((TextRoi)rpRoi).getJustification();
			}
			if (rpRoi instanceof ImageRoi)
				opacity = ((ImageRoi)rpRoi).getOpacity();
		}
		ImagePlus imp = WindowManager.getCurrentImage();
		if (n==list.getItemCount() && n>1 && !IJ.isMacro()) {
			GenericDialog gd = new GenericDialog("ROI Manager");
			gd.addMessage("Apply changes to all "+n+" selections?");
			gd.showDialog();
			if (gd.wasCanceled()) return;
		}
		for (int i=0; i<n; i++) {
			String label = list.getItem(indexes[i]);
			Roi roi = (Roi)rois.get(label);
			//IJ.log("set "+color+"  "+lineWidth+"  "+fillColor);
			if (color!=null) roi.setStrokeColor(color);
			if (lineWidth>=0) roi.setStrokeWidth(lineWidth);
			roi.setFillColor(fillColor);
			if (roi!=null && (roi instanceof TextRoi)) {
				roi.setImage(imp);
				if (font!=null)
					((TextRoi)roi).setCurrentFont(font);
				((TextRoi)roi).setJustification(justification);
				roi.setImage(null);
			}
			if (roi!=null && (roi instanceof ImageRoi) && opacity!=-1)
				((ImageRoi)roi).setOpacity(opacity);
		}
		if (rpRoi!=null && rpName!=null && !rpRoi.getName().equals(rpName))
			rename(rpRoi.getName());
		ImageCanvas ic = imp!=null?imp.getCanvas():null;
		Roi roi = imp!=null?imp.getRoi():null;
		boolean showingAll = ic!=null &&  ic.getShowAllROIs();
		if (roi!=null && (n==1||!showingAll)) {
			if (lineWidth>=0) roi.setStrokeWidth(lineWidth);
			if (color!=null) roi.setStrokeColor(color);
			if (fillColor!=null) roi.setFillColor(fillColor);
			if (roi!=null && (roi instanceof TextRoi)) {
				((TextRoi)roi).setCurrentFont(font);
				((TextRoi)roi).setJustification(justification);
			}
			if (roi!=null && (roi instanceof ImageRoi) && opacity!=-1)
				((ImageRoi)roi).setOpacity(opacity);
		}
		if (lineWidth>1 && !showingAll && roi==null) {
			showAll(SHOW_ALL);
			showingAll = true;
		}
		if (imp!=null) imp.draw();
		if (record()) {
			if (fillColor!=null)
				Recorder.record("roiManager", "Set Fill Color", Colors.colorToString(fillColor));
			else {
				Recorder.record("roiManager", "Set Color", Colors.colorToString(color!=null?color:Color.red));
				Recorder.record("roiManager", "Set Line Width", lineWidth);
			}
		}
	}
	
	void flatten() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		ImageCanvas ic = imp.getCanvas();
		if (!ic.getShowAllROIs() && ic.getDisplayList()==null && imp.getRoi()==null)
			error("Image does not have an overlay or ROI");
		else
			IJ.doCommand("Flatten"); // run Image>Flatten in separate thread
	}
			
	public boolean getDrawLabels() {
		return labelsCheckbox.getState();
	}

	void combine() {
		ImagePlus imp = getImage();
		if (imp==null) return;
		int[] indexes = getSelectedIndexes();
		if (indexes.length==1) {
			error("More than one item must be selected, or none");
			return;
		}
		if (indexes.length==0)
			indexes = getAllIndexes();
		int nPointRois = 0;
		for (int i=0; i<indexes.length; i++) {
			Roi roi = (Roi)rois.get(list.getItem(indexes[i]));
			if (roi.getType()==Roi.POINT)
				nPointRois++;
			else
				break;
		}
		if (nPointRois==indexes.length)
			combinePoints(imp, indexes);
		else
			combineRois(imp, indexes);
		if (record()) Recorder.record("roiManager", "Combine");
	}
	
	void combineRois(ImagePlus imp, int[] indexes) {
		ShapeRoi s1=null, s2=null;
		for (int i=0; i<indexes.length; i++) {
			Roi roi = (Roi)rois.get(list.getItem(indexes[i]));
			if (roi.isLine() || roi.getType()==Roi.POINT)
				continue;
			if (s1==null) {
				if (roi instanceof ShapeRoi)
					s1 = (ShapeRoi)roi;
				else
					s1 = new ShapeRoi(roi);
				if (s1==null) return;
			} else {
				if (roi instanceof ShapeRoi)
					s2 = (ShapeRoi)roi;
				else
					s2 = new ShapeRoi(roi);
				if (s2==null) continue;
				if (roi.isArea())
					s1.or(s2);
			}
		}
		if (s1!=null)
			imp.setRoi(s1);
	}

	void combinePoints(ImagePlus imp, int[] indexes) {
		int n = indexes.length;
		Polygon[] p = new Polygon[n];
		int points = 0;
		for (int i=0; i<n; i++) {
			Roi roi = (Roi)rois.get(list.getItem(indexes[i]));
			p[i] = roi.getPolygon();
			points += p[i].npoints;
		}
		if (points==0) return;
		int[] xpoints = new int[points];
		int[] ypoints = new int[points];
		int index = 0;
		for (int i=0; i<p.length; i++) {
			for (int j=0; j<p[i].npoints; j++) {
				xpoints[index] = p[i].xpoints[j];
				ypoints[index] = p[i].ypoints[j];
				index++;
			}	
		}
		imp.setRoi(new PointRoi(xpoints, ypoints, xpoints.length));
	}

	void and() {
		ImagePlus imp = getImage();
		if (imp==null) return;
		int[] indexes = getSelectedIndexes();
		if (indexes.length==1) {
			error("More than one item must be selected, or none");
			return;
		}
		if (indexes.length==0)
			indexes = getAllIndexes();
		ShapeRoi s1=null, s2=null;
		for (int i=0; i<indexes.length; i++) {
			Roi roi = (Roi)rois.get(list.getItem(indexes[i]));
			if (!roi.isArea()) continue;
			if (s1==null) {
				if (roi instanceof ShapeRoi)
					s1 = (ShapeRoi)roi.clone();
				else
					s1 = new ShapeRoi(roi);
				if (s1==null) return;
			} else {
				if (roi instanceof ShapeRoi)
					s2 = (ShapeRoi)roi.clone();
				else
					s2 = new ShapeRoi(roi);
				if (s2==null) continue;
				s1.and(s2);
			}
		}
		if (s1!=null) imp.setRoi(s1);
		if (record()) Recorder.record("roiManager", "AND");
	}

	void xor() {
		ImagePlus imp = getImage();
		if (imp==null) return;
		int[] indexes = getSelectedIndexes();
		if (indexes.length==1) {
			error("More than one item must be selected, or none");
			return;
		}
		if (indexes.length==0)
			indexes = getAllIndexes();
		ShapeRoi s1=null, s2=null;
		for (int i=0; i<indexes.length; i++) {
			Roi roi = (Roi)rois.get(list.getItem(indexes[i]));
			if (!roi.isArea()) continue;
			if (s1==null) {
				if (roi instanceof ShapeRoi)
					s1 = (ShapeRoi)roi.clone();
				else
					s1 = new ShapeRoi(roi);
				if (s1==null) return;
			} else {
				if (roi instanceof ShapeRoi)
					s2 = (ShapeRoi)roi.clone();
				else
					s2 = new ShapeRoi(roi);
				if (s2==null) continue;
				s1.xor(s2);
			}
		}
		if (s1!=null) imp.setRoi(s1);
		if (record()) Recorder.record("roiManager", "XOR");
	}

	void addParticles() {
		String err = IJ.runMacroFile("ij.jar:AddParticles", null);
		if (err!=null && err.length()>0)
			error(err);
	}

	void sort() {
		int n = rois.size();
		if (n==0) return;
		String[] labels = new String[n];
		int index = 0;
		for (Enumeration en=rois.keys(); en.hasMoreElements();)
			labels[index++] = (String)en.nextElement();
		list.removeAll();
		StringSorter.sort(labels);
		for (int i=0; i<labels.length; i++)
			list.add(labels[i]);
		if (record()) Recorder.record("roiManager", "Sort");
	}
	
	void specify() {
		try {IJ.run("Specify...");}
		catch (Exception e) {return;}
		runCommand("add");
	}
	
	void removeSliceInfo() {
		int[] indexes = getSelectedIndexes();
		if (indexes.length==0)
			indexes = getAllIndexes();
		for (int i=0; i<indexes.length; i++) {
			int index = indexes[i];
			String name = list.getItem(index);
			int n = getSliceNumber(name);
			if (n==-1) continue;
			String name2 = name.substring(5, name.length());
			name2 = getUniqueName(name2);
			Roi roi = (Roi)rois.get(name);
			rois.remove(name);
			roi.setName(name2);
			roi.setPosition(0);
			rois.put(name2, roi);
			list.replaceItem(name2, index);
		}
		if (record()) Recorder.record("roiManager", "Remove Slice Info");
	}

	void help() {
		String macro = "run('URL...', 'url="+IJ.URL+"/docs/menus/analyze.html#manager');";
		new MacroRunner(macro);
	}

	void options() {
		Color c = ImageCanvas.getShowAllColor();
		GenericDialog gd = new GenericDialog("Options");
		gd.addPanel(makeButtonPanel(gd), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));
		gd.addCheckbox("Associate \"Show All\" ROIs with slices", Prefs.showAllSliceOnly);
		gd.addCheckbox("Restore ROIs centered", restoreCentered);
		gd.addCheckbox("Use ROI names as labels", Prefs.useNamesAsLabels);
		gd.showDialog();
		if (gd.wasCanceled()) {
			if (c!=ImageCanvas.getShowAllColor())
				ImageCanvas.setShowAllColor(c);
			return;
		}
		Prefs.showAllSliceOnly = gd.getNextBoolean();
		restoreCentered = gd.getNextBoolean();
		Prefs.useNamesAsLabels = gd.getNextBoolean();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp!=null) imp.draw();
		if (record()) {
			Recorder.record("roiManager", "Associate", Prefs.showAllSliceOnly?"true":"false");
			Recorder.record("roiManager", "Centered", restoreCentered?"true":"false");
			Recorder.record("roiManager", "UseNames", Prefs.useNamesAsLabels?"true":"false");
		}
	}

	Panel makeButtonPanel(GenericDialog gd) {
		Panel panel = new Panel();
    	//buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
		colorButton = new Button("\"Show All\" Color...");
		colorButton.addActionListener(this);
		panel.add(colorButton);
		return panel;
	}
	
	void setShowAllColor() {
            ColorChooser cc = new ColorChooser("\"Show All\" Color", ImageCanvas.getShowAllColor(),  false);
            ImageCanvas.setShowAllColor(cc.getColor());
	}

	void split() {
		ImagePlus imp = getImage();
		if (imp==null) return;
		Roi roi = imp.getRoi();
		if (roi==null || roi.getType()!=Roi.COMPOSITE) {
			error("Image with composite selection required");
			return;
		}
		boolean record = Recorder.record;
		Recorder.record = false;
		Roi[] rois = ((ShapeRoi)roi).getRois();
		for (int i=0; i<rois.length; i++) {
			imp.setRoi(rois[i]);
			addRoi(false);
		}
		Recorder.record = record;
		if (record()) Recorder.record("roiManager", "Split");
	}
	
	void showAll(int mode) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{error("There are no images open."); return;}
		ImageCanvas ic = imp.getCanvas();
		if (ic==null) return;
		boolean showAll = mode==SHOW_ALL;
		if (mode==LABELS) {
			showAll = true;
			if (record())
				Recorder.record("roiManager", "Show All with labels");
		} else if (mode==NO_LABELS) {
			showAll = true;
			if (record())
				Recorder.record("roiManager", "Show All without labels");
		}
		if (showAll) imp.killRoi();
		ic.setShowAllROIs(showAll);
		if (record())
			Recorder.record("roiManager", showAll?"Show All":"Show None");
		imp.draw();
	}

	void updateShowAll() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null) return;
		ImageCanvas ic = imp.getCanvas();
		if (ic!=null && ic.getShowAllROIs())
			imp.draw();
	}

	int[] getAllIndexes() {
		int count = list.getItemCount();
		int[] indexes = new int[count];
		for (int i=0; i<count; i++)
			indexes[i] = i;
		return indexes;
	}
		
	ImagePlus getImage() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null) {
			error("There are no images open.");
			return null;
		} else
			return imp;
	}

	boolean error(String msg) {
		new MessageDialog(this, "ROI Manager", msg);
		Macro.abort();
		return false;
	}
	
	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) {
			instance = null;	
		}
		if (!IJ.isMacro())
			ignoreInterrupts = false;
	}
        
        private void initializeImage(ImagePlus imp1){
            //ImagePlus img = WindowManager.getCurrentImage();
            ImagePlus img = imp1;
            boolean v139t = IJ.getVersion().compareTo("1.39t")>=0;
            if (img==null){
                IJ.noImage();
            }else if (img.getStackSize() == 1) {
                ImageProcessor ip = img.getProcessor();
                ip.resetRoi();

                m_CounterImg = new ImagePlus("Counter Window - " + img.getTitle(), ip);
                Vector displayList = v139t?img.getCanvas().getDisplayList():null;
                ic = new NucleiImageCanvas( m_CounterImg, m_mapImg, this, displayList );
                new ImageWindow(m_CounterImg, ic);
            } else if (img.getStackSize() > 1){
                ImageStack stack = img.getStack();
                int size = stack.getSize();
                ImageStack counterStack = img.createEmptyStack();
                for (int i = 1; i <= size; i++){
                    ImageProcessor ip = stack.getProcessor(i);
                    counterStack.addSlice(stack.getSliceLabel(i), ip);
                }
                m_CounterImg = new ImagePlus("Counter Window - "+img.getTitle(), counterStack);
                m_CounterImg.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
                if (img.isComposite()) {
                    m_CounterImg = new CompositeImage(m_CounterImg, ((CompositeImage)img).getMode());
                    ((CompositeImage) m_CounterImg).copyLuts(img);
                }
                m_CounterImg.setOpenAsHyperStack(img.isHyperStack());
                Vector displayList = v139t?img.getCanvas().getDisplayList():null;
                ic = new NucleiImageCanvas( m_CounterImg, m_mapImg, this, displayList );
                new StackWindow(m_CounterImg, ic);
            }

            img.changes = false;
            img.close();
        }
        
	
	/** Returns a reference to the ROI Manager
		or null if it is not open. */
	public static IEG_Nuclei_Counter getInstance() {
		return (IEG_Nuclei_Counter)instance;
	}
	
	/** Returns a reference to the ROI Manager window or to the
		macro batch mode RoiManager, or null if neither exists. */


	/**	Returns the ROI Hashtable.
		@see getCount
		@see getRoisAsArray
	*/
	public Hashtable getROIs() {
		return rois;
	}

	/** Returns the selection list.
		@see getCount
		@see getRoisAsArray
	*/
	public List getList() {
		return list;
	}
	
	/** Returns the ROI count. */
	public int getCount() {
		return list.getItemCount();
	}

	/** Returns the ROIs as an array. */
	public Roi[] getRoisAsArray() {
		int n = list.getItemCount();
		Roi[] array = new Roi[n];
		for (int i=0; i<n; i++) {
			String label = list.getItem(i);
			array[i] = (Roi)rois.get(label);
		}
		return array;
	}

	/** Returns the ROIs as an array. */
	public IEGPolygonRoi[] getPolyRoisAsArray() 
        {
            int n = list.getItemCount();
            IEGPolygonRoi[] array = new IEGPolygonRoi[n];
            for (int i=0; i<n; i++) {
                    String label = list.getItem(i);
                    array[i] = (IEGPolygonRoi)rois.get(label);
            }
            return array;
	}
        
        public IEGPolygonRoi getPolyRoiById( int id )
        {
            IEGPolygonRoi[] pRois = getPolyRoisAsArray();
            
            for ( int i = 0; i < pRois.length; i++ )
            {
                if (pRois[i].m_pParent.getListId() == id)
                    return pRois[i];
            }
            
            return null;
        }
        
        
        
	/** Returns the selected ROIs as an array, or
		all the ROIs if none are selected. */
	public Roi[] getSelectedRoisAsArray() {
		int[] indexes = getSelectedIndexes();
		if (indexes.length==0)
			indexes = getAllIndexes();
		int n = indexes.length;
		Roi[] array = new Roi[n];
		for (int i=0; i<n; i++) {
			String label = list.getItem(indexes[i]);
			array[i] = (Roi)rois.get(label);
		}
		return array;
	}
			
	/** Returns the name of the ROI with the specified index,
		or null if the index is out of range. */
	public String getName(int index) {
		if (index>=0 && index<list.getItemCount())
       	 	return  list.getItem(index);
		else
			return null;
	}

	/** Returns the name of the ROI with the specified index.
		Can be called from a macro using
		<pre>call("ij.plugin.frame.RoiManager.getName", index)</pre>
		Returns "null" if the Roi Manager is not open or index is
		out of range.
	*/
	public static String getName(String index) {
		int i = (int)Tools.parseDouble(index, -1);
		IEG_Nuclei_Counter instance = getInstance();
		if (instance!=null && i>=0 && i<instance.list.getItemCount())
       	 	return  instance.list.getItem(i);
		else
			return "null";
	}

	/** Executes the ROI Manager "Add", "Add & Draw", "Update", "Delete", "Measure", "Draw",
		"Show All", Show None", "Fill", "Deselect", "Select All", "Combine", "AND", "XOR", "Split",
		"Sort" or "Multi Measure" command.  Returns false if <code>cmd</code>
		is not one of these strings. */
	public boolean runCommand(String cmd) {
		cmd = cmd.toLowerCase();
		macro = true;
		boolean ok = true;
		if (cmd.equals("Reset")) {
                    ic.bReset = true;
		} else if (cmd.equals("add & draw"))
			addAndDraw(false);
		else if (cmd.equals("update"))
			update(true);
		else if (cmd.equals("update2"))
			update(false);
		else if (cmd.equals("delete"))
			delete(false);
		else if (cmd.equals("measure"))
			measure(COMMAND);
		else if (cmd.equals("draw"))
			drawOrFill(DRAW);
		else if (cmd.equals("fill"))
			drawOrFill(FILL);
		else if (cmd.equals("label"))
			drawOrFill(LABEL);
		else if (cmd.equals("and"))
			and();
		else if (cmd.equals("or") || cmd.equals("combine"))
			combine();
		else if (cmd.equals("xor"))
			xor();
		else if (cmd.equals("split"))
			split();
		else if (cmd.equals("sort"))
			sort();
		else if (cmd.equals("multi measure"))
			multiMeasure();
		else if (cmd.equals("multi plot"))
			multiPlot();
		else if (cmd.equals("show all")) {
			if (WindowManager.getCurrentImage()!=null) {
				showAll(SHOW_ALL);
				showAllCheckbox.setState(true);
			}
		} else if (cmd.equals("show none")) {
			if (WindowManager.getCurrentImage()!=null) {
				showAll(SHOW_NONE);
				showAllCheckbox.setState(false);
			}
		} else if (cmd.equals("show all with labels")) {
			labelsCheckbox.setState(true);
			showAll(LABELS);
			if (Interpreter.isBatchMode()) IJ.wait(250);
		} else if (cmd.equals("show all without labels")) {
			labelsCheckbox.setState(false);
			showAll(NO_LABELS);
			if (Interpreter.isBatchMode()) IJ.wait(250);
		} else if (cmd.equals("deselect")||cmd.indexOf("all")!=-1) {
			if (IJ.isMacOSX()) ignoreInterrupts = true;
			select(-1);
			IJ.wait(50);
		} else if (cmd.equals("reset")) {
			if (IJ.isMacOSX() && IJ.isMacro())
				ignoreInterrupts = true;
			list.removeAll();
			rois.clear();
			updateShowAll();
		} else if (cmd.equals("debug")) {
			//IJ.log("Debug: "+debugCount);
			//for (int i=0; i<debugCount; i++)
    		//	IJ.log(debug[i]);
		} else if (cmd.equals("enable interrupts")) {
			ignoreInterrupts = false;
		} else if (cmd.equals("remove slice info")) {
			removeSliceInfo();
		} else
			ok = false;
		macro = false;
		return ok;
	}

	/** Executes the ROI Manager "Open", "Save" or "Rename" command. Returns false if 
	<code>cmd</code> is not "Open", "Save" or "Rename", or if an error occurs. */
	public boolean runCommand(String cmd, String name) {
		cmd = cmd.toLowerCase();
		macro = true;
		if (cmd.equals("open")) {
			open(name);
			macro = false;
			return true;
		} else if (cmd.equals("save")) {
			if (!name.endsWith(".zip") && !name.equals(""))
				return error("Name must end with '.zip'");
			if (list.getItemCount()==0)
				return error("The selection list is empty.");
			int[] indexes = getAllIndexes();
			boolean ok = false;
			if (name.equals(""))
				ok = saveMultiple(indexes, null);
			else
				ok = saveMultiple(indexes, name);
			macro = false;
			return ok;
		} else if (cmd.equals("rename")) {
			rename(name);
			macro = false;
			return true;
		} else if (cmd.equals("set color")) {
			Color color = Colors.decode(name, Color.cyan);
			setProperties(color, -1, null);
			macro = false;
			return true;
		} else if (cmd.equals("set fill color")) {
			Color fillColor = Colors.decode(name, Color.cyan);
			setProperties(null, -1, fillColor);
			macro = false;
			return true;
		} else if (cmd.equals("set line width")) {
			int lineWidth = (int)Tools.parseDouble(name, 0);
			if (lineWidth>=0)
				setProperties(null, lineWidth, null);
			macro = false;
			return true;
		} else if (cmd.equals("associate")) {
			Prefs.showAllSliceOnly = name.equals("true")?true:false;
			macro = false;
			return true;
		} else if (cmd.equals("centered")) {
			restoreCentered = name.equals("true")?true:false;
			macro = false;
			return true;
		} else if (cmd.equals("usenames")) {
			Prefs.useNamesAsLabels = name.equals("true")?true:false;
			macro = false;
			if (labelsCheckbox.getState()) {
				ImagePlus imp = WindowManager.getCurrentImage();
				if (imp!=null) imp.draw();
			}
			return true;
		}
		return false;
	}
	
	/** Adds the current selection to the ROI Manager, using the
		specified color (a 6 digit hex string) and line width. */
	public boolean runCommand(String cmd, String hexColor, double lineWidth) {
		ImagePlus imp = WindowManager.getCurrentImage();
		Roi roi = imp!=null?imp.getRoi():null;
		if (roi!=null) roi.setPosition(0);
		if (hexColor==null && lineWidth==1.0 && (IJ.altKeyDown()&&!Interpreter.isBatchMode()))
			addRoi(true);
		else {
			Color color = hexColor!=null?Colors.decode(hexColor, Color.cyan):null;
			addRoi(null, false, color, (int)Math.round(lineWidth));
		}
		return true;	
	}
	
	/** Assigns the ROI at the specified index to the current image. */
	public void select(int index) {
		select(null, index);
	}
	
	/** Assigns the ROI at the specified index to 'imp'. */
	public void select(ImagePlus imp, int index) {
		selectedIndexes = null;
		int n = list.getItemCount();
		if (index<0) {
			for (int i=0; i<n; i++)
				list.deselect(i);
			if (record()) Recorder.record("roiManager", "Deselect");
			return;
		}
		if (index>=n) return;
		boolean mm = list.isMultipleMode();
		if (mm) list.setMultipleMode(false);
		int delay = 1;
		long start = System.currentTimeMillis();
		while (true) {
			list.select(index);
			if (delay>1) IJ.wait(delay);
			if (list.isIndexSelected(index))
				break;
			for (int i=0; i<n; i++)
				if (list.isSelected(i)) list.deselect(i);
			IJ.wait(delay);
			delay *= 2; if (delay>32) delay=32;
			if ((System.currentTimeMillis()-start)>1000L)
				error("Failed to select ROI "+index);
		}
		if (imp==null) imp=getImage();
		restore(imp, index, true);	
		if (mm) list.setMultipleMode(true);
	}
	
	public void select(int index, boolean shiftKeyDown, boolean altKeyDown) {
		if (!(shiftKeyDown||altKeyDown))
			select(index);
		ImagePlus imp = IJ.getImage();
		if (imp==null) return;
		Roi previousRoi = imp.getRoi();
		if (previousRoi==null)
			{select(index); return;}
		Roi.previousRoi = (Roi)previousRoi.clone();
		String label = list.getItem(index);
		Roi roi = (Roi)rois.get(label);
		if (roi!=null) {
			roi.setImage(imp);
			roi.update(shiftKeyDown, altKeyDown);
		}
	}
	
	public void setEditMode(ImagePlus imp, boolean editMode) {
		ImageCanvas ic = imp.getCanvas();
		boolean showAll = false;
		if (ic!=null) {
			showAll = ic.getShowAllROIs() | editMode;
			ic.setShowAllROIs(showAll);
			imp.draw();
		}
		showAllCheckbox.setState(showAll);
		labelsCheckbox.setState(editMode);
	}
	
	/*
	void selectAll() {
		boolean allSelected = true;
		int count = list.getItemCount();
		for (int i=0; i<count; i++) {
			if (!list.isIndexSelected(i))
				allSelected = false;
		}
		if (allSelected)
			select(-1);
		else {
			for (int i=0; i<count; i++)
				if (!list.isSelected(i)) list.select(i);
		}
	}
	*/

    /** Overrides PlugInFrame.close(). */
    public void close() {
    	super.close();
    	instance = null;
		Prefs.saveLocation(LOC_KEY, getLocation());
    }
    
    public void mousePressed (MouseEvent e) {
		int x=e.getX(), y=e.getY();
		if (e.isPopupTrigger() || e.isMetaDown())
			pm.show(e.getComponent(),x,y);
	}

	public void mouseWheelMoved(MouseWheelEvent event) {
		synchronized(this) {
			int index = list.getSelectedIndex();
			int rot = event.getWheelRotation();
			if (rot<-1) rot = -1;
			if (rot>1) rot = 1;
			index += rot;
			if (index<0) index = 0;
			if (index>=list.getItemCount()) index = list.getItemCount();
			//IJ.log(index+"  "+rot);
			select(index);
			if (IJ.isWindows())
				list.requestFocusInWindow();
		}
	}
	
	/** Temporarily selects multiple ROIs, where 'indexes' is an array of integers, 
		each greater than or equal to 0 and less than the value returned by getCount().
		The selected ROIs are not highlighted in the ROI Manager list and are no 
		longer selected after the next ROI Manager command is executed.
	*/
	public void setSelectedIndexes(int[] indexes) {
		int count = getCount();
		if (count==0) return;
		for (int i=0; i<indexes.length; i++) {
			if (indexes[i]<0) indexes[i]=0;
			if (indexes[i]>=count) indexes[i]=count-1;
		}
		selectedIndexes = indexes;
	}
	
	public int[] getSelectedIndexes() 
        {
		if (selectedIndexes!=null) 
                {
			int[] indexes = selectedIndexes;
			selectedIndexes = null;
			return indexes;
		} else
			return list.getSelectedIndexes();
	}

	private boolean record() {
		return Recorder.record && !IJ.isMacro();
	}

 	public void mouseReleased (MouseEvent e) {}
	public void mouseClicked (MouseEvent e) {}
	public void mouseEntered (MouseEvent e) {}
	public void mouseExited (MouseEvent e) {}
        

        public int stThres = 30;
        public int thresStep = 10;
        public double m_minArea = 200;
        public double m_maxArea = 700;
        public double m_minCircularity = 0.75;
        
        public double m_minCircularity_1 = 0.70;
        public double m_minCircularity_2 = 0.85;
        
        public double m_minAxisAspectRatio = 1.8;
        public double m_minArea_1 = 400;
        public int m_min3DLevels = 4;
        
        
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
        
        public java.util.ArrayList getSubRois(ImagePlus img1, ImageProcessor ip, IEGPolygonRoi roi, 
                double minCircularity, double minArea, String roiName)
        {
                Rectangle r = roi.getBounds();
                Point roiStLocation = new Point(r.x,r.y);
                
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
                RoiWithStat parentRoiS = new RoiWithStat(roi, imgSub);
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
                        for ( int ith = stThres; ith < 255; ith += thresStep)
                        {
                            
                            makeBinary(imgSub, ith, 255);
                            if (bDebug)
                            {
                                String iName = "c:\\ij\\vj\\dist\\d\\v.tif";
                                saveSubImage(imgSub, iName);
                            }

                            RankFilters rFilter1 = new RankFilters();
                            rFilter1.setup(filterTypeStr[i], imgSub);
                            rFilter1.rank(ipSub, filterRad[j], filterType[i]);

                            
//                             imgSub.updateAndDraw();
//                            if (true)
//                                return null;
                        //    ipSub.setThreshold(ith, 255, ImageProcessor.BLACK_AND_WHITE_LUT);
                            
                            Binary b = new Binary();
                            
                            b.setup("fill", imgSub);
                            b.run(ipSub);
                            
                            EDM ed = new EDM();
                            ed.setup("watershed", imgSub);
                            ed.run(ipSub);
                            
                            
                            imgSub.updateAndDraw();
                            
                            
                            for ( int fIdx=0; fIdx < vFinalSubRois.size(); fIdx++ )
                            {
                                setRoiPixToBlack(ipSub, ((RoiWithStat)vFinalSubRois.get(fIdx)).m_Roi);
                            }
                            imgSub.updateAndDraw();
                            
                            if (bDebug)
                            {
                                String iName = "c:\\ij\\vj\\dist\\d\\vFilt.tif";
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
                                                    RoiWithStat tRs = (RoiWithStat)vSubRois.get(idx);
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
                                                    wRoi.setName(roiName);
                                                    wRoi.setLocation(stX, stY);
                                                    wRoi.setPosition(roi.getPosition());
                                                    RoiWithStat wRs = new RoiWithStat(wRoi, imgSub);
                                                    
                                                    if (wRs.m_RoiStat.area >= m_minArea)
                                                        bMaxArea = true;
                                                        
                                                    
                                                    if ( wRs.m_RoiStat.area >= m_minArea && wRs.m_RoiStat.area <= m_maxArea  )
                                                    {
                                                        
                                                        if (wRs.m_RoiStat.area >= m_minArea_1 && wRs.m_Circularity >= m_minCircularity_1 )
                                                        {
                                                            if (wRs.m_RoiStat.histogram[0] <= 0 )
                                                                vSubRois.add(wRs);
                                                        }
                                                        else if (wRs.m_RoiStat.area < m_minArea_1 && wRs.m_Circularity >= m_minCircularity_2 )
                                                        {
                                                            if (wRs.m_RoiStat.histogram[0] <= 0 )
                                                                vSubRois.add(wRs);
                                                        }
                                                        /*if (wRs.m_RoiStat.histogram[0] > 0 )
                                                            IJ.log( "V>> "+ wRoi.getName() + Integer.toString(wRs.m_RoiStat.histogram[0]) + "-Area:"+ Double.toString(wRs.m_RoiStat.area));*/
                                                    }

                                                    setRoiPixToBlack(ipSub, wRoi);
                                                    imgSub.updateAndDraw();
                                                    
                                                    if (bDebug)
                                                    {
                                                        String iName = "c:\\ij\\vj\\dist\\d\\v" + Integer.toString(vSubRois.size()) + ".tif";
                                                        saveSubImage(imgSub, iName);
                                                    }
                                                    
                                                }   
                                            }
                                    }
                            }
                            
                            
                            
                            ipSub.setThreshold(ImageProcessor.NO_THRESHOLD, 255, ImageProcessor.NO_LUT_UPDATE);
                            ipSub.swapPixelArrays();
                            imgSub.updateAndDraw();
                            if (bDebug)
                            {
                                String iName = "c:\\ij\\vj\\dist\\d\\vDone.tif";
                                saveSubImage(imgSub, iName);
                            }
                            
                            ipSub.snapshot();
                            System.gc();
                            
                            
                            double totRoiArea=0;
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
                                    RoiWithStat tRs = (RoiWithStat)vSubRois.get(idx);
                                 //   if (tRs.m_Circularity>=m_minCircularity)
                                    {
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
            return vFinalSubRois;
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
        
        void saveSubImage(ImagePlus sImg, String name)
        {
            WindowManager.setTempCurrentImage(sImg); 
            IJ.saveAs("Tiff", name);
        }
        
        boolean makeBinary(ImagePlus img,  int minTh, int maxTh)
        {
         
            ImageProcessor ip = img.getProcessor();
            int width = ip.getWidth();
            int height = ip.getHeight();

            if (minTh < 0)
                minTh=0;
            
            if (maxTh > 255)
                maxTh = 255;
            
            ip.resetBinaryThreshold();
            
            int[] lut = new int[256];
            for (int i=0; i<256; i++) {
                    if (i>=minTh && i<=maxTh)
                            lut[i] = 255;
                    else {
                            lut[i] = 0;
                    }
            }
            
            ip.applyTable(lut);
            
            return true;
 /*            byte[] pixels = null;
             pixels = (byte[])ip.getPixels();
             
             if (pixels==null)
                 return false;
             
             Rectangle r = roi.getBounds();
             int offset=0;
             
            makeBinary1:
            for (int y=r.y; y<(r.y+r.height); y++) 
            {
                    offset = y*width;

                    makeBinary2:
                    for (int x=r.x; x<(r.x+r.width); x++) 
                    {
                            if ( pixels[offset+x] >= minTh && pixels[offset+x] <= maxTh )
                                pixels[offset+x]=1;
                    }
            }*/
        }

        public static int getMaxValue(int[] numbers, int maxNum){  
          int maxValue = numbers[0];  
          for(int i=1;i < maxNum;i++){  
            if(numbers[i] > maxValue){  
              maxValue = numbers[i];  
            }  
          }  
          return maxValue;  
        }  

        public static int getMinValue(int[] numbers, int maxNum){  
          int minValue = numbers[0];  
          for(int i=1;i< maxNum;i++){  
            if(numbers[i] < minValue){  
              minValue = numbers[i];  
            }  
          }  
          return minValue;  
        }          
        

}

/*
 *         void CheckROICorrelationIn3DObject(RoiWithStat rs)
        {
            if (rs.m_Over_ROIs_f__a_b.size() > 0)
            {
                CheckRoiCorrelation(rs);
                OverlapROI ors = (OverlapROI)rs.m_Over_ROIs_f__a_b.get(0);
                CheckROICorrelationIn3DObject(ors.m_OverlapRoi);
            }
            
        }
        
        void CheckRoiCorrelation(RoiWithStat rs)
        {
            ImagePlus img = IJ.getImage();
            boolean bAltered = false;
            if (rs.m_Over_ROIs_f__a_b != null)
            {
                if (rs.m_Over_ROIs_f__a_b.size() > 0)
                {
                    RoiManager.OverlapROI ors = (RoiManager.OverlapROI)rs.m_Over_ROIs_f__a_b.get(0);
                    
                    double diffArea = 0; 
                    double areaDiffPer = 0;
                    
                   // if(rs.m_RoiStat.area<ors.m_OverlapRoi.m_RoiStat.area )
                    if ( ors.m_OverlapRoi.m_Circularity < 0.8 )
                    {
                        if ( ors.m_OverlapRoi.m_Over_ROIs_f__a_b != null &&  ors.m_OverlapRoi.m_Over_ROIs_f__a_b.size() > 0 )
                        {
                            RoiManager.OverlapROI ors1 = (RoiManager.OverlapROI)ors.m_OverlapRoi.m_Over_ROIs_f__a_b.get(0);
                            if ( ors1.m_OverlapRoi.m_Circularity < 0.8 )
                            {

                                AlterNextROI(rs);
                                bAltered = true;
                                //diffArea = Math.abs( rs.m_RoiStat.area - ors.m_OverlapRoi.m_RoiStat.area );
                                //areaDiffPer = diffArea * 100 / rs.m_RoiStat.area;

                             //   if ( ors.m_OverlapRoi.m_Circularity < 0.8 && areaDiffPer > 70.0 )
                                //                 {
                            }
                            
                        }
                    }
                    
                    if (ors.m_OverlapRoi.m_RoiStat.area > m_MAXNuArea && !bAltered)
                    {
                        AlterNextROI(rs);
                    }
                }
            }
            else
            {
                if (!rs.m_IsSeed && rs.m_RoiStat.area > m_MAXNuArea)
                {
                    AlterNextROI(rs);
                }
            }
        }
        public static int m_MAXNuArea = 2500;
        
        void AlterNextROI(RoiWithStat rs)
        {
            ImagePlus img = IJ.getImage();
            RoiManager.OverlapROI ors = (RoiManager.OverlapROI)rs.m_Over_ROIs_f__a_b.get(0);
            IEGPolygonRoi p = new IEGPolygonRoi( rs.m_Roi.getPolygon(), 
                    rs.m_Roi.getType() );
            p.setPosition( rs.m_Roi.getPosition() );
            p.setName(rs.m_Roi.getName());
            //img.setSlice(getSliceNumber(ors.m_OverlapRoi.m_Roi.getName()));                            
            RoiWithStat roiS = new RoiWithStat(p, img);
            p.m_pParent = roiS;

            OverlapROI o = new OverlapROI();
            o.m_OverlapArea = roiS.m_RoiStat.area;
            o.m_OverlapAreaPer = 100;
            o.m_OverlapRoi = roiS;
            o.m_OverlapRoi.m_id = ors.m_OverlapRoi.m_id;
            o.m_OverlapRoi.m_Name = ors.m_OverlapRoi.m_Name;

            if (ors.m_OverlapRoi.m_Over_ROIs_f__a_b!=null && ors.m_OverlapRoi.m_Over_ROIs_f__a_b.size() > 0 )
                roiS.m_Over_ROIs_f__a_b.add(ors.m_OverlapRoi.m_Over_ROIs_f__a_b.get(0));

            if (ors.m_OverlapRoi.m_Over_ROIs_b!=null)
                roiS.m_Over_ROIs_b.add(rs);

            rs.m_Over_ROIs_f__a_b.remove(0);
            rs.m_Over_ROIs_f__a_b.add(o);
            
        }

        public Roi ValidateROIShape(ImagePlus img, Roi roi)
        {
            IJ.log("ValidateROIShape");

            Roi wRoi= null;
            Rectangle br1 = roi.getBounds();
            ImageStack st = img.getStack();
            ImageProcessor ip = st.getProcessor(roi.getPosition());
            ip.resetRoi();
            ip.setRoi(roi);
            
            for ( int i = br1.x; i < (br1.x + br1.width); i = i + 5 )
            {
                for ( int j = br1.y; j < (br1.y + br1.height); j++ )
                {
                    if (roi.contains(i,j))
                    {
                        
                        wRoi = getWandRoi(img, ip, i, j, m_minCircularity, m_minArea,roi.getName());
                        if ( wRoi != null )
                        {
                            ip.resetRoi();
                            wRoi.setPosition(roi.getPosition());
                            System.gc();

                            return wRoi;
                        }
                    }

                }
                System.gc();

            }
            return null;
            
        }
        
       
	public Roi getWandRoi( ImagePlus img, ImageProcessor ip, int x, int y, double minCircularity, double minArea, String roiName) 
        {
                ImageStack st = img.getStack();
                
                IJ.log("getWandRoi");
                
                
                
                Roi wRoi = null;
                
		
                if ( (img.getType()==ImagePlus.GRAY32) && 
                        Double.isNaN( ip.getPixelValue(x,y) ))
			return null;
                
		int imode = Wand.EIGHT_CONNECTED;
                
                String[] filterTypeStr = {"median","mean"};
                int[] filterType = {4,0};
                int[] filterRad = {2,4,6};
                
                _getWandRoi1:
                for ( int i = 0; i < filterType.length; i++)
                {
                    _getWandRoi2:
                    for ( int j = 0; j < filterRad.length; j++ )    
                    {
                        
                        ip.snapshot();
                        RankFilters rFilter = new RankFilters();
                        rFilter.setup(filterTypeStr[i], img);
                        rFilter.rank(ip, filterRad[j], filterType[i]);
                        int itr = 0;
                        _getWandRoi3:
                        for ( int ith = stThres; ith < 255; ith += thresStep)
                        {
                            
                            ip.setThreshold(ith, 255, ImageProcessor.BLACK_AND_WHITE_LUT);
                            Binary b = new Binary();
                            b.setup("fill", img);
                            b.run(ip);
                            
                            EDM ed = new EDM();
                            ed.setup("watershed", img);
                            ed.run(ip);
                            
                            
                            
                            Wand w = new Wand(ip);
                            w.autoOutline(x, y, 0, 255, imode);

                            if (w.npoints>0) 
                            {
                                int type = Roi.FREEROI;
                                wRoi = new IEGPolygonRoi(w.xpoints, w.ypoints, w.npoints, type);
                                wRoi.setName(roiName);
                                RoiWithStat wRs = new RoiWithStat(wRoi, img);
                                if (wRs.m_RoiStat.area<minArea && itr>3)
                                {
                                    ip.swapPixelArrays();
                                    img.updateAndDraw();
                                    System.gc();

                                    break;
                                    
                                }
                                if (wRs.m_Circularity >= minCircularity && wRs.m_RoiStat.area >= minArea)
                                {
                                    
//                                    if ( wRs.m_RoiStat.area < minArea)
//                                        IJ.log
                                    //undo
                                    ip.swapPixelArrays();
                                    img.updateAndDraw();
                                    System.gc();

                                    return wRoi;
                                }
                                
                                if (wRs.m_RoiStat.area<minArea)
                                    itr++;
                            }

                        }
                        System.gc();

                    //undo
                    ip.swapPixelArrays();
                    img.updateAndDraw();
                    }
                }
		
                return null;
	}        

 */