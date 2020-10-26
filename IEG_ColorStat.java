/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.Vector;
import java.awt.event.*;

import ij.gui.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.InputStream;
import ij.*;
import ij.plugin.Duplicator;
import java.io.*;


import jxl.write.*;
import jxl.*;


/**
 *
 * @author vivek.trivedi
 */
public class IEG_ColorStat  implements PlugInFilter
{
    
    private ImagePlus currentImage = null;
    long startTime = 0; 
    int m_minBlue = 0, m_maxBlue = 255;
    int m_minGreen = 0, m_maxGreen = 255;
    int m_minRed = 0, m_maxRed = 255;
    int m_RedPixWithinRange = 0;
    int m_GreenPixWithinRange = 0;
    int m_BluePixWithinRange = 0;
    
    public boolean m_OChoice = false;
    public boolean m_StatChoice = false;
    public boolean m_bRoi = false;
    
    public WritableSheet ws = null;
    
    int m_ORed=0, m_OGreen=0, m_OBlue=0;
    
    
    String m_FolderPath;
    FileWriter m_fWriter = null;
    BufferedWriter m_writer = null;
    public boolean bGetColorRange = false;
    
    public boolean m_bSaveMemory = false;
    public int[][] m_imgPix = null;
    
    public int m_imgWidth = 0;
    public int m_imgHeight = 0;
    public int m_imgStackSize = 0;
    


    public int m_Stack_RedPixWithinRange = 0;
    public int m_Stack_GreenPixWithinRange = 0;
    public int m_Stack_BluePixWithinRange = 0;
    
    public Roi m_roi = null;

    public void run(ImageProcessor imageProcessor)
    {
        
        if (m_fWriter==null)
        {
            if (!bGetColorRange)
            {
                if (!SetColorRange())
                    return;
            }
        }
        
        if (!m_StatChoice)
        {
            try
            {
                if (m_writer!=null)
                {
                    m_writer.close();
                }
                m_writer=null;
                m_fWriter=null;
            }
            catch(Exception e)
            {
                
            }
            
        }
        m_Stack_BluePixWithinRange = m_Stack_GreenPixWithinRange = m_Stack_RedPixWithinRange = 0;         
        m_RedPixWithinRange = m_GreenPixWithinRange = m_BluePixWithinRange = 0;
        
        if (m_imgStackSize == 1)
        {
            int[] pixels = null;
            int w=0,h=0;
            if (m_bSaveMemory)
            {
                w = m_imgWidth;
                h = m_imgHeight;
                pixels = m_imgPix[0];
            }
            else
            {
                if (currentImage==null)
                    return;
                
                imageProcessor = currentImage.getProcessor();
                ImageProcessor ip2 = null;
                Roi roi = currentImage.getRoi();

                if (roi!=null )
                {
                    ip2 = currentImage.getProcessor().crop();
                    pixels = (int[])ip2.getPixels();

                    w = ip2.getWidth();
                    h = ip2.getHeight();
                    
                }
                else
                {
                    w = m_imgWidth;
                    h = m_imgHeight;

                    pixels = (int[])imageProcessor.getPixels();
                }
            }
            
            m_RedPixWithinRange = m_GreenPixWithinRange = m_BluePixWithinRange = 0;

            if ( m_OChoice)
                ApplyOffset(pixels, w, h);
            if (m_StatChoice)
                CalculateStat(pixels, w, h);
                
            if (ws!=null)
            {
                AddToXLSheetPixStat(ws, currentImage.getTitle(), 1, m_RedPixWithinRange, m_GreenPixWithinRange, m_BluePixWithinRange, false);
            }
            if (m_writer!=null)
            {
                try
                {
                    m_writer.write( ",Red(Pixels),Green(Pixels),Blue(Pixels)");
                    m_writer.write(Integer.toString(m_RedPixWithinRange) + "," + Integer.toString(m_GreenPixWithinRange) + "," + Integer.toString(m_BluePixWithinRange));
                    m_writer.newLine();
                    
                }
                catch(Exception e)
                {

                }
            }
           /* else
                IJ.log( m_RedPixWithinRange + "," + m_GreenPixWithinRange + "," + m_BluePixWithinRange );*/
            
            
        }
        else
        {
            startTime=System.currentTimeMillis();
         //   currentImage.hide();
            if (m_writer!=null)
            {
                try
                {
                    m_writer.write( ",Red(Pixels),Green(Pixels),Blue(Pixels)");
                    m_writer.newLine();
                }
                catch(Exception e)
                {

                }
            }
            
            ImageProcessor ip = null;
            ImageProcessor ip2 = null;
            ImageStack stack = null;
            Roi roi = null;
            
            if (!m_bSaveMemory)
            {
                stack = currentImage.getStack();    
                roi = currentImage.getRoi();
                m_roi = roi;
                m_imgStackSize = stack.getSize();
            }
            
            AddWBTitlePixStat(ws, true);           
            
            for ( int i = 1; i <= m_imgStackSize; i++)
            {
                
                int w = 0;
                int h = 0;
                ImagePlus ipClip = null;
                currentImage.setSlice(i);
                
                //
                int[] pixels = null;
                if (m_bSaveMemory)
                {
                    w = m_imgWidth;
                    h = m_imgHeight;
                    pixels = new int[h*w];
                    System.arraycopy(m_imgPix[i-1], 0, pixels, 0, (h*w));
                }
                else
                {
                    if (currentImage==null)
                        return;

                    ip = stack.getProcessor(i);
                    
                    ip2 = null;
                    if ( roi != null )
                    {
                        ip.setRoi(roi);
                        
                        IJ.run("Copy");
                        //ImagePlus iptry = new ImagePlus("Try" + Integer.toString(i), ip2);                        
                        
                        IJ.run("Internal Clipboard");   
                        ipClip = IJ.getImage();
                        ipClip.hide();
                        ip2 = ipClip.getProcessor();
//                        ip2.setRoi(roi);
                        
                     /*   ImagePlus iptry = new ImagePlus("Try" + Integer.toString(i), ip2);
                        iptry.show();
                        IJ.save(iptry, "C:\\IJ\\try\\");*/
                        
                        
                        pixels = (int[])ip2.getPixels();
                        w = ip2.getWidth();
                        h = ip2.getHeight();
                        
                    }
                    else
                    {
                        pixels = (int[])ip.getPixels();
                        w = ip.getWidth();
                        h = ip.getHeight();

                    }

                }
                
                //
                
                
                m_RedPixWithinRange = m_GreenPixWithinRange = m_BluePixWithinRange = 0;
                
                if (m_OChoice)
                    ApplyOffset(pixels, w, h);
                if (m_StatChoice)
                    CalculateStat(pixels,w,h); 
                
                if (ipClip!=null)
                {
                    ipClip.close();
                    ipClip = null;
                }
                m_Stack_RedPixWithinRange += m_RedPixWithinRange;
                m_Stack_GreenPixWithinRange += m_GreenPixWithinRange;
                m_Stack_BluePixWithinRange += m_BluePixWithinRange;
                
                
                if (m_StatChoice)
                {
                    if (ws!=null)
                    {
                        AddToXLSheetPixStat(ws, currentImage.getTitle(), i, m_RedPixWithinRange, m_GreenPixWithinRange, m_BluePixWithinRange, true);
                    }
                    if (m_writer!=null )
                    {

                        try
                        {
                            m_writer.write(i + "," + m_RedPixWithinRange + "," + m_GreenPixWithinRange + "," + m_BluePixWithinRange);
                            m_writer.newLine();
                            
                        }
                        catch(Exception e)
                        {

                        }
                    }
                   /* else
                    {
                        IJ.log( i + "," + m_RedPixWithinRange + "," + m_GreenPixWithinRange + "," + m_BluePixWithinRange );
                        IJ.log( i + "," + m_Stack_RedPixWithinRange + "," + m_Stack_GreenPixWithinRange + "," + m_Stack_BluePixWithinRange );
                    }*/
                }
                
            }
            
            currentImage.show();
            
            if (m_StatChoice)
            {
                /*if (ws!=null)
                {
                    AddToXLSheetPixStat(ws, currentImage.getTitle(), 1, m_RedPixWithinRange, m_GreenPixWithinRange, m_BluePixWithinRange,false);
                }*/
                if (m_writer!=null)
                {
                    try
                    {
                        m_writer.write("Total" + "," + m_Stack_RedPixWithinRange + "," + m_Stack_GreenPixWithinRange + "," + m_Stack_BluePixWithinRange);
                        m_writer.newLine();
                    }
                    catch(Exception e)
                    {

                    }
                }           
               /* else
                    IJ.log( "Total" + "," + m_Stack_RedPixWithinRange + "," + m_Stack_GreenPixWithinRange + "," + m_Stack_BluePixWithinRange);*/
            }
        
        }
        
        if (m_fWriter!=null)
        {
            try
            {
                m_writer.close();
            }
            catch(IOException ee)
            {
                
            }
        }

    }
    
    
        public void  AddWBTitlePixStat(WritableSheet xlWs, boolean bStack)
        {
            try
            {
                jxl.write.Label label = new jxl.write.Label(0, 0, "Image Title");
                xlWs.addCell(label);          

                if (bStack)
                {
                    label = new jxl.write.Label(1, 0, "Slice No");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(2, 0, "Total Red Pixels");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(3, 0, "Total Green Pixels");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(4, 0, "Total Blue Pixels");
                    xlWs.addCell(label);
                    
                }
                else
                {
                    label = new jxl.write.Label(1, 0, "Total Red Pixels");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(2, 0, "Total Green Pixels");
                    xlWs.addCell(label);
                    label = new jxl.write.Label(3, 0, "Total Blue Pixels");
                    xlWs.addCell(label);
                }
            }
            catch(Exception e)
            {
                
            }
        }
    
        private void AddToXLSheetPixStat(WritableSheet xlWs, String ImageTitle, int iRaw, int pr, int pg, int pb, boolean bAddSliceNo)//, IEGAvgResults avgRes, boolean is2D)
        {
            try
            {

                jxl.write.Label label = new jxl.write.Label(0, iRaw, ImageTitle);
                xlWs.addCell(label);          


                if (bAddSliceNo)
                {
                    jxl.write.Number val = new jxl.write.Number(1, iRaw, iRaw);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(2, iRaw, pr);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(3, iRaw, pg);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(4, iRaw, pb);
                    xlWs.addCell(val);
                }
                else
                {
                    jxl.write.Number val = new jxl.write.Number(1, iRaw, pr);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(2, iRaw, pg);
                    xlWs.addCell(val);
                    val = new jxl.write.Number(3, iRaw, pb);
                    xlWs.addCell(val);
                    
                }

            }
            catch(Exception e)
            {
                IJ.log("Unable to Write Pixel Stat data to the Batch Summary...");
            }

        }
    
    
    @Override
    public int setup(String arg, ImagePlus imagePlus)
    {
            currentImage = imagePlus;
            if (arg!=null)
            {
                m_FolderPath = arg;
                try
                {
                    m_fWriter = new FileWriter(m_FolderPath, true);
                    m_writer = new BufferedWriter(m_fWriter);
                }
                catch(Exception e)
                {
                
                }
                
            }
            return DOES_RGB;
    }
    
    public boolean  SetColorRange() {
        GenericDialog gd = new GenericDialog("Color Range");

        gd.addCheckbox("Offset Adjustment", true);
        gd.addNumericField("    Offset Red :", 9,0);
        gd.addNumericField("    Offset Green :", 9,0);
        gd.addNumericField("    Offset Blue :", 9, 0);
        gd.addMessage("--------------------");

        gd.addCheckbox("Generate Color Stat ", true);
        gd.addSlider("  Min Red:", 0, 255, 10);
        gd.addSlider("  Max Red:", 0, 255, 200);
        gd.addMessage("                           ");
        gd.addSlider("  Min Green:", 0, 255, 10);
        gd.addSlider("  Max Green:", 0, 255, 200);
        gd.addMessage("                           ");
        gd.addSlider("  Min Blue:", 0, 255, 10);
        gd.addSlider("  Max Blue:", 0, 255, 200);
        
/*        panel = new ColorPanel(initialColor);
        gd.addPanel(panel, GridBagConstraints.CENTER, new Insets(10, 0, 0, 0));
        colors = gd.getNumericFields();
        for (int i=0; i<colors.size(); i++)
            ((TextField)colors.elementAt(i)).addTextListener(this);
        sliders = gd.getSliders();
        for (int i=0; i<sliders.size(); i++)
            ((Scrollbar)sliders.elementAt(i)).addAdjustmentListener(this);*/
        gd.showDialog();
        if (gd.wasCanceled()) return false;
        
        m_OChoice = gd.getNextBoolean();
        m_ORed = (int)gd.getNextNumber();
        m_OGreen = (int)gd.getNextNumber();
        m_OBlue = (int)gd.getNextNumber();
        
        
        m_StatChoice = gd.getNextBoolean();
        m_minRed = (int)gd.getNextNumber();
        m_maxRed = (int)gd.getNextNumber();
        m_minGreen = (int)gd.getNextNumber();
        m_maxGreen = (int)gd.getNextNumber();
        m_minBlue = (int)gd.getNextNumber();
        m_maxBlue = (int)gd.getNextNumber();
        
        bGetColorRange = true;
        return bGetColorRange;
    }
    
    
    private void ApplyOffset(int[] pixels, int w, int h)
    {
        int pVal=0;

        for (int i = 0; i < w*h; i++)
        {
            {
                pixels[i] = pixels[i] & 0x00ffffff;
                
                int r,g,b;
                r = (pixels[i]>>16);
                r = r & 0xff;
                g = (pixels[i]>>8);
                g = g & 0xff;
                b = pixels[i] & 0xff;
                
                
                r = r - m_ORed;
                if (r < 0)
                    r=0;

                g = g - m_OGreen;
                if (g < 0)
                    g=0;

                b = b - m_OBlue;
                if (b < 0)
                    b=0;
                
                
                
                
                pVal=0;
               
                pVal=(r<<16);
                pVal|=(g<<8);
                pVal|=b;
                
                pixels[i]=pVal;
                
            }
        }
        
    }
    
    private void CalculateStat(int[] pixels, int w, int h)
    {
        int pVal=0;
        
        for (int i = 0; i < w*h; i++)
        {
            pixels[i] = pixels[i] & 0x00ffffff;

            int r,g,b;
            r = (pixels[i]>>16);
            r = r & 0xff;
            g = (pixels[i]>>8);
            g = g & 0xff;

            b = pixels[i] & 0xff;
            
            if ( r == 255 && g == 255 && b == 255 ) //just to avoid white color mess 
                continue;

            if ( r >= m_minRed && r <= m_maxRed )
            {
                m_RedPixWithinRange++;
            }
            if ( g >= m_minGreen && g <= m_maxGreen )
            {
                m_GreenPixWithinRange++;
            }
            if ( b >= m_minBlue && b <= m_maxBlue )
            {
                m_BluePixWithinRange++;
            }
        }
    }

    
    
}
