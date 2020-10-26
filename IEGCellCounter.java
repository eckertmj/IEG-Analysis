
/**
 *
 * @author Vivek Trivedi ref from Kurt De Vos (http://www.gnu.org/licenses/gpl.txt 
 */


import ij.IJ;
import java.io.*;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.*;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.process.ImageProcessor;
import ij.CompositeImage;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.util.ListIterator;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import java.util.ArrayList;
import java.awt.*;
import ij.text.*;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import java.util.Iterator;
import java.awt.geom.*;
import javax.swing.*;
import ij.plugin.filter.*;

import javax.swing.JFileChooser;


public class IEGCellCounter extends JFrame implements ActionListener, ItemListener{
    private static final String ADD = "Add";
    private static final String REMOVE = "Remove";
    private static final String INITIALIZE = "Initialize";
    private static final String RESULTS = "Results";
    private static final String DELETE = "Delete";
    private static final String DELMODE = "Delete Mode";
    private static final String KEEPORIGINAL = "Keep Original";
    private static final String IEG_ANALYSIS = "IEG Analysis";
    private static final String IEG_HOMER = "Show Homer Map";
    private static final String IEG_ARC = "Show Arc Map";

    private static final String SHOWNUMBERS = "Show Numbers";
    private static final String SHOWALL = "Show All";
    private static final String HIDE_MARKERS = "Hide Markers";
    private static final String ROI_MODE = "ROI Mode";
    private static final String ROI_BLOBS = "Calc. ROI(s) Blobs";
    
    private static final String RESET = "Reset";
    private static final String EXPORTMARKERS = "Save Markers";
    private static final String LOADMARKERS = "Load Markers";
    private static final String EXPORT_NU_MARKERS = "Save Nuclei Markers";
    private static final String LOAD_NU_MARKERS = "Load Nuclei Markers";
    private static final String LOADMAP = "Load Maps";
    private static final String ROTATEMAP = "Rotate Maps";
    
    private static final String DELETEMAP = "Delete Maps";
    private static final String SAVEMAP = "Save Maps";
    
    private static final String LOADIEGRESULTS = "Load IEG Results";
    private static final String EXPORTIMG = "Export Image";
    private static final String MEASURE = "Measure...";
    private static final String SHOWMARKER = "Show Marker";
    
    
    
    public TextPanel confPanel;
    
    public String m_StrConfig;
    public int m_ShowMarkerMapOption = 3;
    
    public static final int HOMER_COLOR_ID=1;
    public static final short ARC_COLOR_ID=2;    
    String[] rFileName;

    
    ResultsTable ResultsTableHomer;
    ResultsTable ResultsTableArc;
    ResultsTable ResultTableROI;
    
    ResultsTable ResultsTableNu;
    ResultsTable ResultTableNuROI;
    
    
    public String curImgTitle = "";
    
    public String IEG_Ver="";
    
    public class MarkerMap
    {
        ArrayList<Point> m_HomerMap = null; 
        Color m_HColor = Color.cyan; 
        File m_HFile = null;
        JTextField m_txtH = null;
        
        ArrayList<Point> m_ArcMap = null; 
        Color m_AColor = Color.PINK; 
        File m_AFile = null;
        JTextField m_txtA = null;
        
        void PopulateMap(int id, File f)
        {
            try
            {
                if (id == HOMER_COLOR_ID)
                {
                    m_HFile = f;
                    m_HomerMap = ReadMapFile(f);
                }
                else
                {
                    m_AFile = f;
                    m_ArcMap = ReadMapFile(f);
                }
            }
            catch(Exception e)
            {
                IJ.log(e.toString());
            }
        }
        
        void SaveMaps()
        {
            try
            {
                if (m_HomerMap != null)
                {
                    FileWriter fwriter = new FileWriter( m_HFile, false );
                    BufferedWriter m_writer = new BufferedWriter(fwriter);
                    
                    for ( int i = 0; i < m_HomerMap.size(); i++)
                    {
                        Point p = m_HomerMap.get(i);
                        m_writer.write( Integer.toString(p.x) + "," + Integer.toString(p.y) );
                        m_writer.newLine();                        
                    }
                    m_writer.close();
                }

                if (m_ArcMap != null)
                {
                    FileWriter fwriter = new FileWriter( m_AFile, false );
                    BufferedWriter m_writer = new BufferedWriter(fwriter);
                    
                    for ( int i = 0; i < m_ArcMap.size(); i++)
                    {
                        Point p = m_ArcMap.get(i);
                        m_writer.write( Integer.toString(p.x) + "," + Integer.toString(p.y) );
                        m_writer.newLine();                        
                    }
                    m_writer.close();
                }
            
            
            }
            catch(Exception e)
            {
                IJ.log(e.toString());
            }
        }
        
        ArrayList<Point> ReadMapFile(File f)
        {
            ArrayList<Point> tMap = new ArrayList<Point>(); 
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                String pStr;
                while ((pStr = reader.readLine()) != null) 
                {
                    Point p = new Point();
                    String[] pStrs = pStr.split(",");
                    p.x = Integer.parseInt(pStrs[0]);
                    p.y = Integer.parseInt(pStrs[1]);
                    tMap.add(p);
                }
                reader.close();
            }
            catch(Exception e)
            {
                IJ.log(e.toString());
            }
            
            return tMap;
        }
        
        void RotateMaps( double angle, double cX, double cY )
        {
            if (m_HomerMap != null)
                RotatePoints(angle, cX, cY, m_HomerMap);
            
            if (m_ArcMap != null)
                RotatePoints(angle, cX, cY, m_ArcMap);
            
        }
        
        void AddOffsetToMaps(int xC, int yC)
        {
            if (m_HomerMap != null)
                AddOffset(xC,yC, m_HomerMap);
            
            if (m_ArcMap != null)
                AddOffset(xC,yC, m_ArcMap);
            
        }
        
        void AddOffset(int xC, int yC, ArrayList<Point> mapP)
        {
            for ( int i =0; i < mapP.size();i++)
            {
                Point p = mapP.get(i);
                
                p.x = p.x + xC;
                p.y = p.y + yC;
                
                mapP.set(i, p);
            }            
        }
        
        void RotatePoints( double angle, double cX, double cY, ArrayList<Point> mapP )
        {

            for ( int i =0; i < mapP.size();i++)
            {
                Point p = mapP.get(i);
                double[] pt = {p.x, p.y};

                AffineTransform.getRotateInstance( Math.toRadians(angle), cX, cY ).transform( pt, 0, pt, 0, 1 ); 
                
                p.x = (int)pt[0];
                p.y = (int)pt[1];
                
                mapP.set(i, p);
            
            }
        }
        
        void DeleteROI()
        {
            RoiManager rm=RoiManager.getInstance();
            ImagePlus imp = WindowManager.getCurrentImage();

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
                    DeletePoints(mRoi);
                }

            }
            else
            {
                mRoi = imp.getRoi();
                DeletePoints(mRoi);
            }
            
            imp.killRoi();
        }
        
        void DeletePoints( Roi mRoi )
        {
            int totHDel=0, totADel=0;
            if (m_HomerMap != null )
            {
                ListIterator it = m_HomerMap.listIterator();
//                for ( int i = 0; i < m_HomerMap.size(); i++ )
                while(it.hasNext())
                {
                    Point p = (Point)it.next();
                    if (mRoi.contains(p.x, p.y))
                    {
                    //    m_HomerMap.remove(i);
                        it.remove();
                       totHDel++ ;
                    }
                }
            }

            if (m_ArcMap != null )
            {
                ListIterator it = m_ArcMap.listIterator();
               // for ( int i = 0; i < m_ArcMap.size(); i++ )
                while(it.hasNext())
                {
                    Point p = (Point)it.next();
                    if (mRoi.contains(p.x, p.y))
                    {
                        //m_ArcMap.remove(i);
                        it.remove();
                        totHDel++;
                    }
                }
            }
        }
    }
    
    MarkerMap markerMap = new MarkerMap();
    
    private Vector typeVector;
    private Vector dynRadioVector;
    private Vector txtFieldVector;
    private IEGCellCntrMarkerVector markerVector;
    private IEGCellCntrMarkerVector currentMarkerVector;

    private IEGCellCntrMarkerVector markerNVector;
    private IEGCellCntrMarkerVector currentMarkerNVector;
    private Vector txtFieldNVector;
    private Vector typeNVector;
    private Vector dynRadioNVector;
    
    
    private JPanel dynParentPanel;
    private JPanel dynPanel;
    private JPanel dynButtonPanel;
    private JPanel dynNuPanel;
    private JPanel dynNuButtonPanel;
    private JPanel statButtonPanel;
    private JPanel dynTxtPanel;
    private JPanel dynNuTxtPanel;
    private JCheckBox delCheck;
    private JCheckBox newCheck;
    private JCheckBox IEGCheck;
    private JCheckBox numbersCheck;
    private JCheckBox showAllCheck;
    private JCheckBox HideMarkerCheck;
    private JCheckBox ROIModeCheck;
    private ButtonGroup radioGrp;
    private JSeparator separator;
    private JButton addButton;
    private JButton removeButton;
    private JButton initializeButton;
    private JButton resultsButton;
    private JButton deleteButton;
    private JButton calcROIButton;
    private JButton resetButton;
    private JButton exportButton;
    private JButton loadButton;
    private JButton exportNuButton;
    private JButton loadNuButton;
    
    private JButton loadMap;
    private JButton rotateMap;
    private JButton deleteMap;
    private JButton saveMap;
    
    private JButton loadIEGButton;
    private JCheckBox IEGMapHomerCheck;
    private JCheckBox IEGMapArcCheck;

    
    private JButton exportimgButton;
    private JButton measureButton;
    private JComboBox windowList;
    private JCheckBox chkWindowList;
    
    private JCheckBox newCheck1;    
    

    
    private boolean keepOriginal=false;
    private boolean bIEG_Analysis = false;
    private boolean HideMarker = false;
    private boolean ROIMode = false;
    
    private IEGCellCntrImageCanvas ic;
    
    private ImagePlus img;
    private ImagePlus counterImg;
    
    private GridLayout dynGrid;
    
    private boolean isJava14;
    
    static IEGCellCounter instance;
    
    public IEGCellCounter(){
        super("Cell Counter - McN Lab V"+IEG_Analysis.IEG_VER); //LM, 8Jan2013
        isJava14 = IJ.isJava14(); 
        if(!isJava14){
            IJ.showMessage("You are using a pre 1.4 version of java, exporting and loading marker data is disabled");
        }
        setResizable(false);
        typeVector = new Vector();
        typeNVector = new Vector();
        
        txtFieldVector = new Vector();
        txtFieldNVector = new Vector();
        
        dynRadioVector = new Vector();
        dynRadioNVector = new Vector();
        
        initGUI();
        populateTxtFields();
        instance = this;
        this.IEG_Ver = IEG_Analysis.IEG_VER;
    }
    
    /** Show the GUI threadsafe*/
    private static class GUIShower implements Runnable {
        final JFrame jFrame;
        public GUIShower(JFrame jFrame) {
            this.jFrame = jFrame;
        }
        public void run() {
            jFrame.pack();
            jFrame.setLocation(1000, 200);
            jFrame.setVisible(true);
        }
    }
    
    private void initGUI(){
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridBagLayout gb = new GridBagLayout();
        getContentPane().setLayout(gb);
        
        radioGrp = new ButtonGroup();//to group the radiobuttons
        
        dynGrid = new GridLayout(8,2);
        dynGrid.setVgap(2);
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        dynParentPanel = new JPanel();
        dynParentPanel.setBorder(BorderFactory.createTitledBorder("Counters"));
        dynParentPanel.setLayout(gb);
        
        
        
        
        dynPanel = new JPanel();
        dynPanel.setBorder(BorderFactory.createTitledBorder("IEG Foci"));
        dynPanel.setLayout(gb);
        
        
        dynButtonPanel = new JPanel();
        dynButtonPanel.setLayout(dynGrid);
  
        
        gbc.anchor = GridBagConstraints.EAST;        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipadx=5;
        gb.setConstraints(dynButtonPanel,gbc);
        dynPanel.add(dynButtonPanel);

        
        dynTxtPanel=new JPanel();
        dynTxtPanel.setLayout(dynGrid);
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipadx=5;
        gb.setConstraints(dynTxtPanel,gbc);
        dynPanel.add(dynTxtPanel);
        
        
        dynButtonPanel.add(makeDynRadioButton(1, dynTxtPanel, null));
        dynButtonPanel.add(makeDynRadioButton(2, dynTxtPanel, null));
        dynButtonPanel.add(makeDynRadioButton(3, dynTxtPanel, null));
        dynButtonPanel.add(makeDynRadioButton(4, dynTxtPanel, null));
        dynButtonPanel.add(makeDynRadioButton(5, dynTxtPanel, null));
        dynButtonPanel.add(makeDynRadioButton(6, dynTxtPanel, null));
        dynButtonPanel.add(makeDynRadioButton(7, dynTxtPanel, null));
        dynButtonPanel.add(makeDynRadioButton(8, dynTxtPanel, null));
        
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gb.setConstraints(dynPanel,gbc);
        dynParentPanel.add(dynPanel);
        
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        newCheck1 = new JCheckBox("Enable Nuclei Counter");
        newCheck1.setToolTipText("Enable Nuclei Counter");
        newCheck1.setSelected(false);
        newCheck1.addItemListener( this );
        gb.setConstraints( newCheck1, gbc );
        dynParentPanel.add( newCheck1 );
        
        
        
        // kepp score for nuclei
        dynNuPanel = new JPanel();
        dynNuPanel.setBorder(BorderFactory.createTitledBorder("Nuclei"));
        dynNuPanel.setLayout(gb);
        dynNuButtonPanel = new JPanel();
        dynNuButtonPanel.setLayout(dynGrid);
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipadx=5;
        gb.setConstraints(dynNuButtonPanel,gbc);
        dynNuPanel.add(dynNuButtonPanel);
        
        
        dynNuTxtPanel=new JPanel();
        dynNuTxtPanel.setLayout(dynGrid);
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipadx=5;
        gb.setConstraints(dynNuTxtPanel,gbc);
        dynNuPanel.add(dynNuTxtPanel);
        
        dynNuButtonPanel.add(makeDynRadioButton(1, dynNuTxtPanel, "H+"));
        dynNuButtonPanel.add(makeDynRadioButton(2, dynNuTxtPanel, "A+"));
        dynNuButtonPanel.add(makeDynRadioButton(3, dynNuTxtPanel, "A/H+"));
        dynNuButtonPanel.add(makeDynRadioButton(4, dynNuTxtPanel, "A/H-"));
        


      //  dynNuTxtPanel.add(makeDynamicTextArea());
        
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gb.setConstraints(dynNuPanel,gbc);
        dynParentPanel.add(dynNuPanel);
        
        
//-        
        
        
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gb.setConstraints(dynParentPanel,gbc);
        getContentPane().add(dynParentPanel);
 
        //-------------------------------------------------------------------
        
        
        // create a "static" panel to hold control buttons
        statButtonPanel = new JPanel();
        statButtonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        statButtonPanel.setLayout(gb);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        newCheck = new JCheckBox(KEEPORIGINAL);
        newCheck.setToolTipText("Keep original");
        newCheck.setSelected(false);
        newCheck.addItemListener(this);
        gb.setConstraints(newCheck,gbc);
        statButtonPanel.add(newCheck);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        IEGCheck = new JCheckBox(IEG_ANALYSIS);
        IEGCheck.setToolTipText("IEG Analysis");
        IEGCheck.setSelected(false);
        IEGCheck.addItemListener(this);
        gb.setConstraints(IEGCheck,gbc);
        statButtonPanel.add(IEGCheck);
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        initializeButton = makeButton(INITIALIZE, "Initialize image to count");
        gb.setConstraints(initializeButton,gbc);
        statButtonPanel.add(initializeButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        addButton = makeButton(ADD, "add a counter type");
        gb.setConstraints(addButton,gbc);
        statButtonPanel.add(addButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        removeButton = makeButton(REMOVE, "remove last counter type");
        gb.setConstraints(removeButton,gbc);
        statButtonPanel.add(removeButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.insets = new Insets(3,0,3,0);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        deleteButton = makeButton(DELETE, "delete last marker");
        deleteButton.setEnabled(false);
        gb.setConstraints(deleteButton,gbc);
        statButtonPanel.add(deleteButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        delCheck = new JCheckBox(DELMODE);
        delCheck.setToolTipText("When selected\nclick on the marker\nyou want to remove");
        delCheck.setSelected(false);
        delCheck.addItemListener(this);
        delCheck.setEnabled(false);
        gb.setConstraints(delCheck,gbc);
        statButtonPanel.add(delCheck);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        resultsButton = makeButton(RESULTS, "show results in results table");
        resultsButton.setEnabled(false);
        gb.setConstraints(resultsButton,gbc);
        statButtonPanel.add(resultsButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        resetButton=makeButton(RESET, "reset all counters");
        resetButton.setEnabled(false);
        gb.setConstraints(resetButton,gbc);
        statButtonPanel.add(resetButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        numbersCheck = new JCheckBox(SHOWNUMBERS);
        numbersCheck.setToolTipText("When selected, numbers are shown");
        numbersCheck.setSelected(true);
        numbersCheck.setEnabled(false);
        numbersCheck.addItemListener(this);
        gb.setConstraints(numbersCheck,gbc);
        statButtonPanel.add(numbersCheck);
        
        showAllCheck = new JCheckBox(SHOWALL);
        showAllCheck.setToolTipText("When selected, all stack markers are shown");
        showAllCheck.setSelected(false);
        showAllCheck.setEnabled(false);
        showAllCheck.addItemListener(this);
        gb.setConstraints(showAllCheck,gbc);
        statButtonPanel.add(showAllCheck);

        HideMarkerCheck = new JCheckBox(HIDE_MARKERS);
        HideMarkerCheck.setToolTipText("When selected, all stack markers are hidden");
        HideMarkerCheck.setSelected(false);
        HideMarkerCheck.setEnabled(true);
        HideMarkerCheck.addItemListener(this);
        gb.setConstraints(HideMarkerCheck,gbc);
        statButtonPanel.add(HideMarkerCheck);

        ROIModeCheck = new JCheckBox(ROI_MODE);
        ROIModeCheck.setToolTipText("When selected, allows ROI drawing");
        ROIModeCheck.setSelected(false);
        ROIModeCheck.setEnabled(true);
        ROIModeCheck.addItemListener(this);
        gb.setConstraints(ROIModeCheck,gbc);
        statButtonPanel.add(ROIModeCheck);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        calcROIButton = makeButton(ROI_BLOBS, "Calculate blob count in all ROIs....");
        calcROIButton.setEnabled(false);
        gb.setConstraints(calcROIButton,gbc);
        statButtonPanel.add(calcROIButton);
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        exportButton = makeButton(EXPORTMARKERS, "Save markers to file");
        exportButton.setEnabled(false);
        gb.setConstraints(exportButton,gbc);
        statButtonPanel.add(exportButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        loadButton = makeButton(LOADMARKERS, "Load markers from file");
        if (!isJava14) loadButton.setEnabled(false);
        gb.setConstraints(loadButton,gbc);
        statButtonPanel.add(loadButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        exportNuButton = makeButton(EXPORT_NU_MARKERS, "Save Nuclei markers to file");
        exportNuButton.setEnabled(true);
        gb.setConstraints(exportNuButton,gbc);
        statButtonPanel.add(exportNuButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        loadNuButton = makeButton(LOAD_NU_MARKERS, "Load Nuclei markers from file");
        if (!isJava14) loadNuButton.setEnabled(false);
        gb.setConstraints(loadNuButton,gbc);
        statButtonPanel.add(loadNuButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        loadMap = makeButton(LOADMAP, "Load blob maps");
        if (!isJava14) loadMap.setEnabled(false);
        gb.setConstraints(loadMap,gbc);
        statButtonPanel.add(loadMap);

        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        rotateMap = makeButton(ROTATEMAP, "Rotate blob maps");
        rotateMap.setEnabled(false);
        gb.setConstraints(rotateMap,gbc);
        statButtonPanel.add(rotateMap);
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        IEGMapHomerCheck = new JCheckBox(IEG_HOMER);
        IEGMapHomerCheck.setToolTipText("Show Homer Map");
        IEGMapHomerCheck.setEnabled(false);
        IEGMapHomerCheck.setSelected(true);
        IEGMapHomerCheck.addItemListener(this);
        gb.setConstraints(IEGMapHomerCheck,gbc);
        statButtonPanel.add(IEGMapHomerCheck);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        IEGMapArcCheck = new JCheckBox(IEG_ARC);
        IEGMapArcCheck.setToolTipText("Show Arc Map");
        IEGMapArcCheck.setEnabled(false);
        IEGMapArcCheck.setSelected(true);
        IEGMapArcCheck.addItemListener(this);
        gb.setConstraints(IEGMapArcCheck,gbc);
        statButtonPanel.add(IEGMapArcCheck);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        deleteMap = makeButton(DELETEMAP, "Delete ROI blobs maps");
        if (!isJava14) deleteMap.setEnabled(false);
        gb.setConstraints(deleteMap,gbc);
        statButtonPanel.add(deleteMap);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        saveMap = makeButton(SAVEMAP, "Save ROI blobs maps");
        if (!isJava14) saveMap.setEnabled(false);
        gb.setConstraints(saveMap,gbc);
        statButtonPanel.add(saveMap);
        
        
        
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        loadIEGButton = makeButton(LOADIEGRESULTS, "Load IEG Results From file");
        gb.setConstraints(loadIEGButton,gbc);
        statButtonPanel.add(loadIEGButton);
       
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        exportimgButton= makeButton(EXPORTIMG, "Export image with markers");
        exportimgButton.setEnabled(false);
        gb.setConstraints(exportimgButton,gbc);
        statButtonPanel.add(exportimgButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        measureButton = makeButton(MEASURE, "Measure pixel intensity of marker points");
        measureButton.setEnabled(false);
        gb.setConstraints(measureButton,gbc);
        statButtonPanel.add(measureButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);


        /*gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        chkWindowList = new JCheckBox("Show ", false);
        gb.setConstraints(chkWindowList,gbc);
        statButtonPanel.add(chkWindowList);*/
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        windowList = new JComboBox();
        gb.setConstraints(windowList,gbc);
        statButtonPanel.add(windowList);
        windowList.addActionListener(this);
        
   /*     int[] wList = WindowManager.getIDList();
        int x = (wList!=null?wList.length:-1);
        for (int i=0; i<x; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            String s = imp.getTitle();
            windowList.addItem(s);
        }*/
        
        
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.ipadx=5;
        gb.setConstraints(statButtonPanel,gbc);
        getContentPane().add(statButtonPanel);
        
        
        Runnable runner = new GUIShower(this);
        EventQueue.invokeLater(runner);
    }
    
    private JTextField makeDynamicTextArea(boolean bN){
        JTextField txtFld = new JTextField();
        txtFld.setHorizontalAlignment(JTextField.CENTER);
        txtFld.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        txtFld.setEditable(false);
        txtFld.setText("0");
        if (!bN)
            txtFieldVector.add(txtFld);
        else
            txtFieldNVector.add(txtFld);
        return txtFld;
    }
    
    void populateTxtFields(){
        ListIterator it = typeVector.listIterator();
        while (it.hasNext()){
            int index = it.nextIndex();
            IEGCellCntrMarkerVector markerVector = (IEGCellCntrMarkerVector)it.next();
            int count = markerVector.size();
            JTextField tArea = (JTextField)txtFieldVector.get(index);
            if (bIEG_Analysis && !ROIMode)
                tArea.setText(""+markerVector.getTotalCells());
            else
                tArea.setText(""+count);
        }

        it = typeNVector.listIterator();
        while (it.hasNext())
        {
            int index = it.nextIndex();
            IEGCellCntrMarkerVector _markerVector = (IEGCellCntrMarkerVector)it.next();
            int count = _markerVector.size();
            JTextField tArea = (JTextField)txtFieldNVector.get(index);
            tArea.setText(""+count);
            if (bIEG_Analysis && !ROIMode)
                tArea.setText(""+ _markerVector.getTotalCells());
            else
                tArea.setText(""+count);
        }
        validateLayout();
    }

    void populateTxtFields(int cCount){
        if (ROIMode)
        {
            ListIterator it = dynRadioVector.listIterator();
            JRadioButton[] vJ = new JRadioButton[dynRadioVector.size()];
            dynRadioVector.toArray(vJ);
            for ( int i = 0; i < vJ.length; i++ )
            {
                if (vJ[i].isSelected())
                {
                    JTextField tArea = (JTextField)txtFieldVector.get(i);
                    tArea.setText(""+cCount);
                }
            }
        }
        validateLayout();
    }
    
    void DeleteFromResultTable(int markerType, int markerId)
    {
        ResultsTable srcTable;
        String srcRT_Title;
        if (markerType == HOMER_COLOR_ID)
        {
            srcTable = ResultsTableHomer;
            srcRT_Title = "Homer Results";
        }
        else if (markerType == ARC_COLOR_ID)
        {
            srcTable = ResultsTableArc;
            srcRT_Title = "Arc Results";
        }
        else
            return;

        float[] idCol = srcTable.getColumn(srcTable.getColumnIndex("FocusNO"));
        if (idCol.length>0)
        {
            for ( int j = 0; j < idCol.length; j++)
            {
                if ( markerId == idCol[j] )
                {
                    srcTable.deleteRow(j);
                    srcTable.show(srcRT_Title);
                    break;
                }
            }            
        }            

        if (ResultTableROI!=null)
        {
            srcTable = ResultTableROI;
            srcRT_Title = "ROI Results";

            idCol = srcTable.getColumn(srcTable.getColumnIndex("FocusNO"));
            if (idCol.length>0)
            {
                for ( int j = 0; j < idCol.length; j++)
                {
                    if ( markerId == idCol[j] )
                    {
                        srcTable.deleteRow(j);
                        srcTable.show(srcRT_Title);
                        break;
                    }
                }            
            }            
            
        }
    
    }
    
    void DeleteFromNuResultTable(int markerId)
    {

        float[] idCol = ResultsTableNu.getColumn(ResultsTableNu.getColumnIndex( "Nuclei No" ));
        if ( idCol.length > 0 )
        {
            for ( int j = 0; j < idCol.length; j++)
            {
                if ( markerId == idCol[ j ] )
                {
                    ResultsTableNu.deleteRow( j );
                    ResultsTableNu.show( "Nuclei Results" );
                    break;
                }
            }            
        }            

        if ( ResultTableNuROI != null )
        {
            idCol = ResultsTableNu.getColumn( ResultsTableNu.getColumnIndex( "Nuclei No" ) );
            if ( idCol.length > 0 )
            {
                for ( int j = 0; j < idCol.length; j++)
                {
                    if ( markerId == idCol[ j ] )
                    {
                        ResultsTableNu.deleteRow( j );
                        ResultsTableNu.show( "ROI Nuclei Results" );
                        break;
                    }
                }            
            }            
        }
    }

    void AddToNuResultTable( String markerType, int markerId, int x, int y, int z )
    {
        if (ResultsTableNu==null)
        {
            ResultsTableNu = new ResultsTable();
        }
        
        ResultsTableNu.incrementCounter();
        ResultsTableNu.addLabel("ImageTitle", curImgTitle); //imgTitle
        ResultsTableNu.addLabel("Nuclei Type", markerType);
        ResultsTableNu.addValue("Nuclei No", markerId);        
        ResultsTableNu.addValue("CoreX", x);
        ResultsTableNu.addValue("CoreY", y);
        ResultsTableNu.addValue("CoreZ", z);
        
        ResultsTableNu.show( "Nuclei Results" );
        
        //get image title while init but presssed
    }
    
    void AddToResultTable(int markerType, int markerId, int x, int y, int z)
    {
        ResultsTable srcTable;
        String srcRT_Title;
        if (markerType == HOMER_COLOR_ID)
        {
            srcTable = ResultsTableHomer;
            srcRT_Title = "Homer Results";
        }
        else if (markerType == ARC_COLOR_ID)
        {
            srcTable = ResultsTableArc;
            srcRT_Title = "Arc Results";
        }
        else
            return;
        
        
        int lastRow = srcTable.getCounter()-1;
        if (lastRow<0)
        {
            IJ.log("Unable to add Blob to the result table...");
            return;
        }
        String imgTitle = srcTable.getLabel( lastRow  );
                
        srcTable.incrementCounter();
        srcTable.addLabel("ImageTitle",imgTitle); //imgTitle
        srcTable.addValue("FocusNO",markerId);
        srcTable.addValue("CoreX",x);
        srcTable.addValue("CoreY",y);
        srcTable.addValue("CoreZ",z);
        
        srcTable.show(srcRT_Title);
        
    }
    
    
    private JRadioButton makeDynRadioButton(int id, JPanel txtPanel, String cName){
        JRadioButton jrButton;
        if (cName == null )
        {
            jrButton = new JRadioButton("Type "+ id);
            dynRadioVector.add(jrButton);
            markerVector = new IEGCellCntrMarkerVector(id);
            markerVector.bIsFoci = true;
            typeVector.add(markerVector);
            txtPanel.add(makeDynamicTextArea(false));
        }
        else
        {
            jrButton = new JRadioButton(cName);
            dynRadioNVector.add(jrButton);
            markerVector = new IEGCellCntrMarkerVector(id);
            markerVector.bIsFoci = false;
            markerVector.m_tag = cName;
            typeNVector.add(markerVector);
            txtPanel.add(makeDynamicTextArea(true));
        }
        jrButton.addActionListener(this);
        radioGrp.add(jrButton);
        return jrButton;
    }
    
    private JButton makeButton(String name, String tooltip){
        JButton jButton = new JButton(name);
        jButton.setToolTipText(tooltip);
        jButton.addActionListener(this);
        return jButton;
    }
    
    private void initializeImage(){
        reset();
        img = WindowManager.getCurrentImage();
        boolean v139t = IJ.getVersion().compareTo("1.39t")>=0;
        if (img==null)
        {
            IJ.noImage();
        }
        else if (img.getStackSize() == 1) 
        {
            ImageProcessor ip = img.getProcessor();
            ip.resetRoi();
            if (keepOriginal)
                ip = ip.crop();
            counterImg = new ImagePlus("Counter Window - "+img.getTitle(), ip);
            Vector displayList = v139t?img.getCanvas().getDisplayList():null;
            ic = new IEGCellCntrImageCanvas(counterImg,typeVector, typeNVector,this,displayList);
            new ImageWindow(counterImg, ic);
        } 
        else if (img.getStackSize() > 1)
        {
            ImageStack stack = img.getStack();
            int size = stack.getSize();
            ImageStack counterStack = img.createEmptyStack();
            for (int i = 1; i <= size; i++)
            {
                ImageProcessor ip = stack.getProcessor(i);
                if (keepOriginal)
                   ip = ip.crop();
                counterStack.addSlice(stack.getSliceLabel(i), ip);
            }
            counterImg = new ImagePlus("Counter Window - "+img.getTitle(), counterStack);
            counterImg.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
            if (img.isComposite()) 
            {
                counterImg = new CompositeImage(counterImg, ((CompositeImage)img).getMode());
                ((CompositeImage) counterImg).copyLuts(img);
            }
            counterImg.setOpenAsHyperStack(img.isHyperStack());
            Vector displayList = v139t?img.getCanvas().getDisplayList():null;
            ic = new IEGCellCntrImageCanvas(counterImg,typeVector,typeNVector,this,displayList);
            new StackWindow(counterImg, ic);
        }
        
        curImgTitle = img.getTitle();
        if (!keepOriginal){
            img.changes = false;
            img.close();
        }
        delCheck.setEnabled(true);
        calcROIButton.setEnabled(true);
        numbersCheck.setEnabled(true);
        showAllCheck.setSelected(false);
        if (counterImg.getStackSize()>1)
            showAllCheck.setEnabled(true);
        addButton.setEnabled(true);
        removeButton.setEnabled(true);
        resultsButton.setEnabled(true);
        deleteButton.setEnabled(true);
        resetButton.setEnabled(true);
        if (isJava14) exportButton.setEnabled(true);
        exportimgButton.setEnabled(true);
        measureButton.setEnabled(true);
    }
    
    void validateLayout(){
        dynPanel.validate();
        dynButtonPanel.validate();
        dynTxtPanel.validate();
        statButtonPanel.validate();
        validate();
        pack();
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        
        if (command.compareTo(ADD) == 0) {
            int i = dynRadioVector.size() + 1;
            dynGrid.setRows(i);
            dynButtonPanel.add(makeDynRadioButton(i, dynTxtPanel, null));
            validateLayout();
            
            if (ic != null)
                ic.setTypeVector(typeVector);
        } else if (command.compareTo(REMOVE) == 0) {
            if (dynRadioVector.size() > 1) {
                JRadioButton rbutton = (JRadioButton)dynRadioVector.lastElement();
                dynButtonPanel.remove(rbutton);
                radioGrp.remove(rbutton);
                dynRadioVector.removeElementAt(dynRadioVector.size() - 1);
                dynGrid.setRows(dynRadioVector.size());
            }
            if (txtFieldVector.size() > 1) {
                JTextField field = (JTextField)txtFieldVector.lastElement();
                dynTxtPanel.remove(field);
                txtFieldVector.removeElementAt(txtFieldVector.size() - 1);
            }
            if (typeVector.size() > 1) {
                typeVector.removeElementAt(typeVector.size() - 1);
            }
            validateLayout();
            
            if (ic != null)
                ic.setTypeVector(typeVector);
        } else if (command.compareTo(INITIALIZE) == 0){
            initializeImage();
        } else if (command.startsWith("A/H-")){ //COUNT
            if (ic == null){
                IJ.error("You need to initialize first");
                return;
            }
            currentMarkerVector = (IEGCellCntrMarkerVector)typeNVector.get(3);
            ic.setCurrentMarkerVector(currentMarkerVector);
        } else if (command.startsWith("A/H+")){ //COUNT
            if (ic == null){
                IJ.error("You need to initialize first");
                return;
            }
            currentMarkerVector = (IEGCellCntrMarkerVector)typeNVector.get(2);
            ic.setCurrentMarkerVector(currentMarkerVector);
            
        } else if (command.startsWith("A+")){ //COUNT
            if (ic == null){
                IJ.error("You need to initialize first");
                return;
            }
            currentMarkerVector = (IEGCellCntrMarkerVector)typeNVector.get(1);
            ic.setCurrentMarkerVector(currentMarkerVector);
        } else if (command.startsWith("H+")){ //COUNT
            if (ic == null){
                IJ.error("You need to initialize first");
                return;
            }
            currentMarkerVector = (IEGCellCntrMarkerVector)typeNVector.get(0);
            ic.setCurrentMarkerVector(currentMarkerVector);
        } else if (command.startsWith("Type")){ //COUNT
            if (ic == null){
                IJ.error("You need to initialize first");
                return;
            }
            int index = Integer.parseInt(command.substring(command.indexOf(" ")+1,command.length()));
            //ic.setDelmode(false); // just in case
            currentMarkerVector = (IEGCellCntrMarkerVector)typeVector.get(index-1);
            ic.setCurrentMarkerVector(currentMarkerVector);
        } else if (command.compareTo("comboBoxChanged") == 0){
            ic.setHideMarkers(true);
            ic.repaint();
            String itemString = (String)windowList.getSelectedItem();
            if (itemString.contains("Both"))
            {
                ic.setHideMarkers(false);
            }
            else if (itemString.contains("Homer"))
            {
                currentMarkerVector = (IEGCellCntrMarkerVector)typeVector.get(0); 
                ic.setCurrentMarkerVector(currentMarkerVector);
                ic.setShowAllMarkers(false);
            }
            else if (itemString.contains("Arc"))
            {
                currentMarkerVector = (IEGCellCntrMarkerVector)typeVector.get(1); 
                ic.setCurrentMarkerVector(currentMarkerVector);
                ic.setShowAllMarkers(false);
            }
            ic.setHideMarkers(false);
            ic.repaint();
        } else if (command.compareTo(DELETE) == 0){
            ic.removeLastMarker();
        } else if (command.compareTo(ROI_BLOBS) == 0){
            ic.mouseReleased(null);
            return;
        }else if (command.compareTo(RESET) == 0){
            reset();
        } else if (command.compareTo(RESULTS) == 0){
            report();
        }else if (command.compareTo(EXPORTMARKERS) == 0){
            exportMarkers();
        }else if (command.compareTo(EXPORT_NU_MARKERS) == 0){
            exportNuMarkers();
        }else if (command.compareTo(LOADMARKERS) == 0){
            if (ic == null)
                initializeImage();
            loadMap.setEnabled(false);

            loadMarkers();
            if (bIEG_Analysis)
            {
                loadResultTables();
            }
            validateLayout();
        }else if (command.compareTo(LOAD_NU_MARKERS) == 0){
            if (ic == null)
                initializeImage();
            loadMap.setEnabled(false);

            loadNuMarkers();
            if (bIEG_Analysis)
            {
                loadNuResultTables();
                newCheck1.setSelected(true);
            }
            validateLayout();
        }else if (command.compareTo(LOADMAP) == 0){
            if (ic == null)
                initializeImage();
            loadMaps();
            ic.setShowAllMarkers(false);
            ic.setShowBlobMaps(true);
            loadButton.setEnabled(false);
            IEGMapHomerCheck.setEnabled(true);
            IEGMapArcCheck.setEnabled(true);
            rotateMap.setEnabled(true);
            validateLayout();
        }else if (command.compareTo(DELETEMAP) == 0){
            markerMap.DeleteROI();
        }else if (command.compareTo(SAVEMAP) == 0){
            markerMap.SaveMaps();
        }else if (command.compareTo(ROTATEMAP) == 0){
                ImagePlus imp = WindowManager.getCurrentImage();
                double rAngle=0.0;
                String strAngle = JOptionPane.showInputDialog(null,"Rotation Angle :", "Angle in Degree", JOptionPane.QUESTION_MESSAGE );
                if (strAngle == null)
                        return;

                rAngle = Double.parseDouble(strAngle);

                if (rAngle == 0.0)
                    return;



                ImageProcessor ip = imp.getProcessor();
                IJ.log( imp.getWidth() + "," + imp.getHeight() );
                   // markerMap.RotateMaps(rAngle, (imp.getWidth()-1)/2, (imp.getHeight()-1)/2 );
                int wOld = imp.getWidth();
                int hOld = imp.getHeight();
            
            
            
		imp.unlock();
		IJ.run("Select All");
		IJ.run("Rotate...", "angle="+rAngle);
		Roi roi = imp.getRoi();
		Rectangle r = roi.getBounds();
		if (r.width<imp.getWidth()) r.width = imp.getWidth();
		if (r.height<imp.getHeight()) r.height = imp.getHeight();
		IJ.showStatus("Rotate: Enlarging...");
		IJ.run("Canvas Size...", "width="+r.width+" height="+r.height+" position=Center zero");
		IJ.showStatus("Rotating...");
                
            
                
                imp = WindowManager.getCurrentImage();
                ip = imp.getProcessor();
                
                ip.setInterpolationMethod(ImageProcessor.BILINEAR);
                ip.setBackgroundValue(0);
                ip.rotate(rAngle);

                int wNew = imp.getWidth();
                int hNew = imp.getHeight();
            
		int xC = (wNew - wOld)/2;	// offset for centered
		int xR = (wNew - wOld);		// offset for right
		int yC = (hNew - hOld)/2;	// offset for centered
		int yB = (hNew - hOld);		// offset for bottom
                
                markerMap.AddOffsetToMaps(xC, yC);
                markerMap.RotateMaps(rAngle, (imp.getWidth()-1)/2, (imp.getHeight()-1)/2 );

            
            
            

        }else if (command.compareTo(EXPORTIMG) == 0){
            ic.imageWithMarkers().show();
        }else if (command.compareTo(MEASURE) == 0){
            measure();
        }
        if (ic!=null)
            ic.repaint();
        
        if (loadButton.isEnabled())
            populateTxtFields();
    }
    
    public void ShowROIResultTable(ArrayList<Integer> vId, int typeId)
    {
        ResultsTable srcTable;
        if (typeId == HOMER_COLOR_ID)
        {
            srcTable = ResultsTableHomer;
        }
        else if (typeId == ARC_COLOR_ID)
        {
            srcTable = ResultsTableArc;
        }
        else
            return;
        
        if (ResultTableROI != null)
        {
            ResultTableROI = null;
            System.gc();
        }
        
        ResultTableROI = new ResultsTable();
        
        for ( int i = 0; i < vId.size(); i++)
        {
            //Vivek Take care of it
            float bId = (float)vId.get(i);
         //   bId++;
            
            float[] idCol = srcTable.getColumn(srcTable.getColumnIndex("FocusNO"));
            if (idCol.length>0)
            {
                for ( int j = 0; j < idCol.length; j++)
                {
                    if ( bId == idCol[j] )
                    {
                        String strRow = srcTable.getRowAsString(j);
                        //ResultTableROI.se
                        ResultTableROI.incrementCounter();
                        ResultTableROI.addLabel("ImageTitle",srcTable.getLabel(j)); //imgTitle
                        ResultTableROI.addValue("FocusNO",srcTable.getValue("FocusNO", j));
                        ResultTableROI.addValue("CoreX",srcTable.getValue("CoreX", j));
                        ResultTableROI.addValue("CoreY",srcTable.getValue("CoreY", j));
                        ResultTableROI.addValue("CoreZ",srcTable.getValue("CoreZ", j));
                        ResultTableROI.addValue("Volume",srcTable.getValue("Volume", j));
                        ResultTableROI.addValue("AreaXY",srcTable.getValue("AreaXY", j));
                        ResultTableROI.addValue("AreaXZ",srcTable.getValue("AreaXZ", j));
                        ResultTableROI.addValue("AreaYZ",srcTable.getValue("AreaYZ", j));
                        ResultTableROI.addValue("Intensity",srcTable.getValue("Intensity", j));
                        ResultTableROI.addValue("Background",srcTable.getValue("Background", j));
                        ResultTableROI.addValue("MinI",srcTable.getValue("MinI", j));
                        ResultTableROI.addValue("MaxI",srcTable.getValue("MaxI", j));
                        ResultTableROI.addValue("Range",srcTable.getValue("Range", j));
//FociResultsTable.addValue("Intensity Integral",PixelsInObject[j].getIntensityINtegral());                        
                        ResultTableROI.addValue("Intensity Integral",srcTable.getValue("Intensity Integral", j));                        
                        ResultTableROI.addValue("Saturated Pixels",srcTable.getValue("Saturated Pixels", j));
        	         	ResultTableROI.addValue("(%)Saturation",srcTable.getValue("(%)Saturation", j));

                        break;
                                
                    }
                }
            }
            
        }
        ResultTableROI.show("ROI Results");
    }
    
    public void loadResultTables()
    {
        if (rFileName.length > 2)
        {
            try 
            {
                ResultsTableHomer = ResultsTable.open(filePathComponents[0]+rFileName[1]);
                ResultsTableArc = ResultsTable.open(filePathComponents[0]+rFileName[2]);
                ResultsTableHomer.updateResults();
                ResultsTableArc.updateResults();
                ResultsTableHomer.show("Homer Results");
                ResultsTableHomer.show("Arc Results");
            } 
            catch(IOException e) 
            {
                    IJ.error( "Unable to open Results File... " );
            }
            
        }
        else if (rFileName[1].indexOf("Homer") > 0 )
        {
            try 
            {
                ResultsTableHomer = ResultsTable.open(filePathComponents[0]+rFileName[1]);
                ResultsTableHomer.show("Homer Results");
            } 
            catch(IOException e) 
            {
                    IJ.error( "Unable to open Results File... " );
            }
        }
        else if (rFileName[1].indexOf("Arc") > 0 )
        {
            try 
            {
                ResultsTableArc = ResultsTable.open(filePathComponents[0]+rFileName[1]);
                ResultsTableArc.show("Arc Results");
            } 
            catch(IOException e) 
            {
                    IJ.error( "Unable to open Results File... " );
            }
            
        }
    }
    
    public void loadNuResultTables()
    {
        {
            try 
            {
                ResultsTableNu = ResultsTable.open(filePathComponents[0]+"Nu_Results_" + img.getTitle() + ".csv");
                ResultsTableNu.show("Nuclei Results");
            } 
            catch(IOException e) 
            {
                    IJ.error( "Unable to open Nuclei Results File... " );
            }
        }
    }
    
    
    public class OnlyExt implements FilenameFilter 
    {
        String ext;
        public OnlyExt(String ext) 
        {
            this.ext = "." + ext;
        }
        public boolean accept(File dir, String name) 
        {
            return name.endsWith(ext);
        }
    }
    
    public void EnDiRadio (boolean bN)
    {
        if (bN)
        {
            dynNuPanel.setEnabled(bN);
            dynPanel.setEnabled((!bN));
            
                ListIterator it = dynRadioVector.listIterator();
                JRadioButton[] vJ = new JRadioButton[dynRadioVector.size()];
                dynRadioVector.toArray(vJ);
                for ( int i = 0; i < vJ.length; i++ )
                {
                    vJ[i].setEnabled((!bN));
                }   
                
                 it = dynRadioNVector.listIterator();
                vJ = new JRadioButton[dynRadioNVector.size()];
                dynRadioNVector.toArray(vJ);
                for ( int i = 0; i < vJ.length; i++ )
                {
                    vJ[i].setEnabled(bN);
                }            
        }
        else
        {
            dynNuPanel.setEnabled((!bN));
            dynPanel.setEnabled(bN);

            ListIterator it = dynRadioVector.listIterator();
            JRadioButton[] vJ = new JRadioButton[dynRadioVector.size()];
            dynRadioVector.toArray(vJ);
            for ( int i = 0; i < vJ.length; i++ )
            {
                vJ[i].setEnabled((!bN));
            }   

            it = dynRadioNVector.listIterator();
            vJ = new JRadioButton[dynRadioNVector.size()];
            dynRadioNVector.toArray(vJ);
            for ( int i = 0; i < vJ.length; i++ )
            {
                vJ[i].setEnabled((bN));
            }            
        }
    }
    
    @Override
    public void itemStateChanged(ItemEvent e){
        if (e.getItem().equals(delCheck)){
            if (e.getStateChange()==ItemEvent.SELECTED){
                ic.setDelmode(true);
            }else{
                ic.setDelmode(false);
            }
        }else if (e.getItem().equals(newCheck)){
            if (e.getStateChange()==ItemEvent.SELECTED){
                keepOriginal=true;
            }else{
                keepOriginal=false;
            }
        }else if (e.getItem().equals(newCheck1)){
            if (e.getStateChange()==ItemEvent.SELECTED)
            {
                
              if (counterImg == null)
                 initializeImage();
 
                if (counterImg.getTitle().contains("Counter Window"))
                {   
                   EnDiRadio(true);
                   if (ic!=null)
                       ic.setNu(true);

                   IEGCheck.setSelected(true);
                }
                else
                {
                    IJ.log("Please Initialize the image first...");
                }
                
            }
            else
            {
               EnDiRadio(false);
               if (ic!=null)
                   ic.setNu(false);
            }
        }else if (e.getItem().equals(IEGCheck)){
            
            if (e.getStateChange()==ItemEvent.SELECTED)
            {
                if (counterImg == null)
                    initializeImage();
                
                if (counterImg.getTitle().contains("Counter Window"))
                {   
                    bIEG_Analysis=true;
                    if (ic!=null)
                        ic.setIEGAnalysis(bIEG_Analysis);
                }
                else
                {
                    IJ.log("Please Initialize the image first...");
                }
            }else
            {
                bIEG_Analysis=false;
                newCheck1.setSelected(false);
                if (ic!=null)
                    ic.setIEGAnalysis(bIEG_Analysis);
            }
        }else if (e.getItem().equals(ROIModeCheck)){
            if (e.getStateChange()==ItemEvent.SELECTED){
                ROIMode=true;
                ic.setROIMode(ROIMode);
            }else{
                ROIMode=false;
                ic.setROIMode(ROIMode);
            }
        }else if (e.getItem().equals(HideMarkerCheck)){
            if (e.getStateChange()==ItemEvent.SELECTED){
                HideMarker=true;
                ic.setHideMarkers(HideMarker);
            }else{
                HideMarker=false;
                ic.setHideMarkers(HideMarker);
            }
            ic.repaint();
        }else if (e.getItem().equals(numbersCheck)){
            if (e.getStateChange()==ItemEvent.SELECTED){
                ic.setShowNumbers(true);
            }else{
                ic.setShowNumbers(false);
            }
            ic.repaint();
        }else if (e.getItem().equals(showAllCheck)){
            if (e.getStateChange()==ItemEvent.SELECTED){
                ic.setShowAll(true);
            }else{
                ic.setShowAll(false);
            }
            ic.repaint();
        }else if (e.getItem().equals(IEGMapArcCheck)){
            if (e.getStateChange()==ItemEvent.SELECTED){
               m_ShowMarkerMapOption = m_ShowMarkerMapOption | ARC_COLOR_ID;
            }else{
                if ((m_ShowMarkerMapOption & HOMER_COLOR_ID) == 1)
                    m_ShowMarkerMapOption = HOMER_COLOR_ID;
                else
                    m_ShowMarkerMapOption = 0;
            }
            ic.repaint();
        }else if (e.getItem().equals(IEGMapHomerCheck)){
            if (e.getStateChange()==ItemEvent.SELECTED){
               m_ShowMarkerMapOption = m_ShowMarkerMapOption | HOMER_COLOR_ID;
            }else{
                if ((m_ShowMarkerMapOption & ARC_COLOR_ID) == 2)
                    m_ShowMarkerMapOption = ARC_COLOR_ID;
                else
                    m_ShowMarkerMapOption = 0;
            }
            ic.repaint();
        }
    }
    
     public void measure(){
        ic.measure();
    }
     
    public void reset(){
        if (typeVector.size()<1){
            return;
        }
        ListIterator mit = typeVector.listIterator();
        while (mit.hasNext()){
            IEGCellCntrMarkerVector mv = (IEGCellCntrMarkerVector)mit.next();
            mv.clear();
        }
        if (ic!=null)
            ic.repaint();
    }
    
    public void report(){
        String labels = "Slice\t";
        boolean isStack = counterImg.getStackSize()>1;
        //add the types according to the button vector!!!!
        ListIterator it = dynRadioVector.listIterator();
        while (it.hasNext()){
            JRadioButton button = (JRadioButton)it.next();
            String str = button.getText(); //System.out.println(str);
            labels = labels.concat(str+"\t");
        }
        IJ.setColumnHeadings(labels);
        String results = "";
        if (isStack){
            for (int slice=1; slice<=counterImg.getStackSize(); slice++){
                results="";
                ListIterator mit = typeVector.listIterator();
                int types = typeVector.size();
                int[] typeTotals = new int[types];
                while (mit.hasNext()){
                    int type = mit.nextIndex();
                    IEGCellCntrMarkerVector mv = (IEGCellCntrMarkerVector)mit.next();
                    ListIterator tit = mv.listIterator();
                    while(tit.hasNext()){
                        IEGCellCntrMarker m = (IEGCellCntrMarker)tit.next();
                        if (m.getZ() == slice){
                            typeTotals[type]++;
                        }
                    }
                }
                results=results.concat(slice+"\t");
                for(int i=0; i<typeTotals.length;i++){
                    results = results.concat(typeTotals[i]+"\t");
                }
                IJ.write(results);
            }
            IJ.write("");
        }
        results = "Total\t";
        ListIterator mit = typeVector.listIterator();
        while (mit.hasNext()){
            IEGCellCntrMarkerVector mv = (IEGCellCntrMarkerVector)mit.next();
            int count = mv.size();
            results = results.concat(count+"\t");
        }
        IJ.write(results);
    }
    
    public File[] getSelectedFiles()
    {
        try
        {
            JFileChooser chooser = new JFileChooser(); 
            chooser.setCurrentDirectory(new java.io.File("."));
            chooser.setDialogTitle("Select Map Files");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setMultiSelectionEnabled(true);

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
    
    
    public void loadMaps()
    {
        while(dynRadioVector.size() > 0 ){
            if (dynRadioVector.size() > 0) {
                JRadioButton rbutton = (JRadioButton)dynRadioVector.lastElement();
                dynButtonPanel.remove(rbutton);
                radioGrp.remove(rbutton);
                dynRadioVector.removeElementAt(dynRadioVector.size() - 1);
                dynGrid.setRows(dynRadioVector.size());
            }
            if (txtFieldVector.size() > 0) {
                JTextField field = (JTextField)txtFieldVector.lastElement();
                dynTxtPanel.remove(field);
                txtFieldVector.removeElementAt(txtFieldVector.size() - 1);
            }
        }
        
        try
        {
            File[] fileList = getSelectedFiles();

            for ( int i = 0; i < fileList.length; i++)
            {
                if (fileList[i].getCanonicalPath().indexOf("H_map") < 0 && fileList[i].getCanonicalPath().indexOf("A_map") < 0)
                    throw new FileNotFoundException("Unable to find Map Files...");
                else if (fileList[i].getCanonicalPath().indexOf("H_map") > 0)
                {
                    markerMap.PopulateMap(HOMER_COLOR_ID, fileList[i]);
                    JTextField txtFld = new JTextField();
                    txtFld.setHorizontalAlignment(JTextField.CENTER);
                    txtFld.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
                    txtFld.setEditable(false);
                    txtFld.setText("                                            ");

                    if (markerMap.m_txtH == null)
                    {
                        markerMap.m_txtH = txtFld;
                        dynTxtPanel.add(markerMap.m_txtH);
                    }
                    
                }
                else if (fileList[i].getCanonicalPath().indexOf("A_map") > 0)
                {
                    markerMap.PopulateMap(ARC_COLOR_ID, fileList[i]);
                    JTextField txtFld = new JTextField();
                    txtFld.setHorizontalAlignment(JTextField.CENTER);
                    txtFld.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
                    txtFld.setEditable(false);
                    txtFld.setText("                                            ");
                    
                    if (markerMap.m_txtA == null)
                    {
                        markerMap.m_txtA = txtFld;
                        dynTxtPanel.add(markerMap.m_txtA);
                    }
                }
            }
            
        }
        catch(Exception e)
        {
            IJ.log(e.toString());
            return;
        }
    }
    
    public void loadMarkers()
    {
        String filePath = getFilePath(new JFrame(), "Select Marker File", OPEN);
        IEGReadXML rxml = new IEGReadXML(filePath);
        String s = rxml.readImgProperties(rxml.IEG_TYPE);
        int TotalMarkers=0;
        boolean bIEG = false;
        if ( s != null )
        {
            bIEG = (rxml.readImgProperties(rxml.IEG_TYPE).equalsIgnoreCase("Y")?true:false);
            IEG_Ver = rxml.readImgProperties(rxml.IEG_VER);
            TotalMarkers = Integer.parseInt(rxml.readImgProperties(rxml.IEG_TOTAL_MARKER));
            m_StrConfig = rxml.readImgProperties(rxml.IEG_CONF);
            IJ.log(m_StrConfig);
            
            if (TotalMarkers>1)
            {
                rFileName = new String[3];
                rFileName[0] =  rxml.readImgProperties(rxml.IMAGE_FILE_PATH);
                rFileName[1] =  rxml.readImgProperties(rxml.IEG_HOMER_FILENAME);
                rFileName[2] =  rxml.readImgProperties(rxml.IEG_ARC_FILENAME);
            }
            else
            {
                rFileName = new String[2];
                rFileName[0] =  rxml.readImgProperties(rxml.IMAGE_FILE_PATH);
                rFileName[1] =  rxml.readImgProperties(rxml.IEG_HOMER_FILENAME);
                
            }
        }
        IEGCheck.setSelected(bIEG);
        IEGCheck.setEnabled(false);
        
        if (bIEG_Analysis)
        {
             rxml = new IEGReadXML(filePath,bIEG_Analysis);
        }
            
        String storedfilename = rxml.readImgProperties(rxml.IMAGE_FILE_PATH);
        
        
        {
            Vector loadedvector = rxml.readMarkerData();
            typeVector = loadedvector;
            ic.setTypeVector(typeVector);
            int index = Integer.parseInt(rxml.readImgProperties(rxml.CURRENT_TYPE));
            currentMarkerVector = (IEGCellCntrMarkerVector)typeVector.get(index); 
            ic.setCurrentMarkerVector(currentMarkerVector);

            
            JRadioButton bt = (JRadioButton)(dynRadioNVector.get(index));
            bt.setSelected(true);
            
        }
    }
    
    public void loadNuMarkers()
    {
        String filePath = getFilePath(new JFrame(), "Select Marker File", OPEN);
        
        if (filePath == null || filePath.contains("null"))
            return;
        
        IEGReadXML rxml = new IEGReadXML(filePath);
        String s = rxml.readImgProperties(rxml.IEG_TYPE);
        int TotalMarkers=0;
        boolean bIEG = false;
        if ( s != null )
        {
            bIEG = (rxml.readImgProperties(rxml.IEG_TYPE).equalsIgnoreCase("Y")?true:false);
            IEG_Ver = rxml.readImgProperties(rxml.IEG_VER);
            TotalMarkers = Integer.parseInt(rxml.readImgProperties(rxml.IEG_TOTAL_MARKER));
        }
        IEGCheck.setSelected(bIEG);
        IEGCheck.setEnabled(false);
        
        if (bIEG_Analysis)
        {
             rxml = new IEGReadXML(filePath,bIEG_Analysis);
        }
            
        String storedfilename = rxml.readImgProperties(rxml.IMAGE_FILE_PATH);
        
        
      //  if(storedfilename.equals(img.getTitle()))
        //if (1)
        {
            Vector loadedvector = rxml.readMarkerData();
            typeNVector = loadedvector;
            ic.setTypeNVector(typeNVector);
            int index = Integer.parseInt(rxml.readImgProperties(rxml.CURRENT_TYPE));
            currentMarkerVector = (IEGCellCntrMarkerVector)typeNVector.get(index); 
            ic.setCurrentMarkerVector(currentMarkerVector);

            
            
            JRadioButton bt = (JRadioButton)(dynRadioNVector.get(index));
            bt.setSelected(true);
            
        }
/*else{
            IJ.error("These Markers do not belong to the current image");
        }*/
    }
    public void exportMarkers(){
        try
        {
            String filePath = getFilePath(new JFrame(), "Save Marker File (.xml)", SAVE);
            if (!filePath.endsWith(".xml"))
                filePath += ".xml";

            IEGWriteXML wxml = new IEGWriteXML(filePath);
            if (bIEG_Analysis)
                wxml.writeXML(rFileName, typeVector, typeVector.indexOf(currentMarkerVector), true, IEG_Ver, m_StrConfig);
            else
                wxml.writeXML(img.getTitle(), typeVector, typeVector.indexOf(currentMarkerVector));

            if (ResultsTableArc!=null)
                ResultsTableArc.saveAs(filePathComponents[0]+rFileName[1]);

            if (ResultsTableHomer!=null)
                ResultsTableHomer.saveAs(filePathComponents[0]+rFileName[1]);
        }
        catch(Exception e)
        {
            IJ.log("Unable to save Results table...");
        }
    }

    public void exportNuMarkers(){
        try
        {
            String filePath = getFilePath(new JFrame(), "Save Nuclei Marker File (.xml)", SAVE);
            if (!filePath.endsWith(".xml"))
                filePath += ".xml";

            String[] sFileName = new String [2];
            sFileName[0] = img.getTitle();
            sFileName[1] = null;
            IEGWriteXML wxml = new IEGWriteXML(filePath);
            if (bIEG_Analysis)
                wxml.writeXML(sFileName, typeNVector, typeNVector.indexOf(currentMarkerVector), true, IEG_Ver, m_StrConfig);
            
            if (ResultsTableNu!=null)
                ResultsTableNu.saveAs(filePathComponents[0]+"Nu_Results_" + img.getTitle() + ".csv");
        }
        catch(Exception e)
        {
            
        }
    }
    
    
    public static final int SAVE=FileDialog.SAVE, OPEN=FileDialog.LOAD;
    public String[] filePathComponents = null;
    private String getFilePath(JFrame parent, String dialogMessage, int dialogType){
        switch(dialogType){
            case(SAVE):
                dialogMessage = "Save "+dialogMessage;
                break;
            case(OPEN):
                dialogMessage = "Open "+dialogMessage;
                break;
        }
        FileDialog fd ;
        filePathComponents = new String[2];
        int PATH = 0;
        int FILE = 1;
        fd = new FileDialog(parent, dialogMessage, dialogType);
        switch(dialogType){
            case(SAVE):
                String filename = img.getTitle();
                if (dialogMessage.contains("Nuclei"))
                    fd.setFile("CellCounter_Nu_"+filename.substring(0,filename.lastIndexOf(".")+1)+"xml");
                else
                    fd.setFile("CellCounter_"+filename.substring(0,filename.lastIndexOf(".")+1)+"xml");
                break;
        }
        fd.setVisible(true);
        filePathComponents[PATH] = fd.getDirectory();
        filePathComponents[FILE] = fd.getFile();
        return filePathComponents[PATH]+filePathComponents[FILE];
    }
    
    public Vector getButtonVector() {
        return dynRadioVector;
    }
    
    public void setButtonVector(Vector buttonVector) {
        this.dynRadioVector = buttonVector;
    }
    
    public IEGCellCntrMarkerVector getCurrentMarkerVector() {
        return currentMarkerVector;
    }
    
    public void setCurrentMarkerVector(IEGCellCntrMarkerVector currentMarkerVector) {
        this.currentMarkerVector = currentMarkerVector;
    }
    
    public static void setType(String type) {
		if (instance==null || instance.ic==null || type==null)
    		return;
    	int index = Integer.parseInt(type)-1;
    	int buttons = instance.dynRadioVector.size();
    	if (index<0 || index>=buttons)
    		return;
    	JRadioButton rbutton = (JRadioButton)instance.dynRadioVector.elementAt(index);
    	instance.radioGrp.setSelected(rbutton.getModel(), true);
		instance.currentMarkerVector = (IEGCellCntrMarkerVector)instance.typeVector.get(index);
		instance.ic.setCurrentMarkerVector(instance.currentMarkerVector);
    }
    
}
