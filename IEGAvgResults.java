/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import javax.swing.JFrame;

import jxl.write.*;
import jxl.*;


/**
 *
 * @author vivek.trivedi
 */
public class IEGAvgResults 
{
    
    public String m_imgTitle="";
    public int  m_totBlobs=0;
    public double  m_CoreX=0;
    public double  m_CoreY=0;
    
    public double  m_CoreZ=0;
    
    public double  Volume=0;
    public double  AreaXY=0;
    public double  AreaXZ=0;
    public double  AreaYZ=0;
    public double  MinI=0;
    public double  MaxI=0;
    public double  Range=0;
    public double Background=0; 

    public double  Intensity=0;


    public double  Intensity_Integral=0;

    public double  Saturated_Pixels=0;
    public double  per_Saturation=0;
    
    public void AddToAvg( double CoreX, double CoreY, double CoreZ, double volume, 
            double areaXY, double areaXZ, 
            double areaYZ, double Background, 
            double minI, double maxI, double range, double intensity, 
            double int_intergal, double sat_pixels, double per_sat )
    
    {
       m_totBlobs++;
       m_CoreX += CoreX;
       m_CoreY += CoreY;
       m_CoreZ += CoreZ;
       
       Volume += volume;
       AreaXY += areaXY;
       AreaXZ += areaXZ;
       AreaYZ += areaYZ;
       
       Background += Background;
       
       MinI += minI;
       MaxI += maxI;
       Range += range;
       Intensity += intensity;
       
       Intensity_Integral += int_intergal;
       Saturated_Pixels += sat_pixels;
       per_Saturation += per_sat;
    }
    
    public void CalculateAvg()
    {
        if (m_totBlobs==0)
            return;
    
        m_CoreX = m_CoreX / m_totBlobs;
        m_CoreY = m_CoreY / m_totBlobs;
        m_CoreZ = m_CoreZ / m_totBlobs;
        
        Volume = Volume / m_totBlobs;
        
        AreaXY = AreaXY / m_totBlobs;
        AreaXZ = AreaXZ / m_totBlobs;
        AreaYZ = AreaYZ / m_totBlobs;
        
        MinI = MinI / m_totBlobs;
        MaxI = MaxI / m_totBlobs;
        Range = Range / m_totBlobs;
        
        Intensity = Intensity / m_totBlobs;
        Intensity_Integral = Intensity_Integral / m_totBlobs;
        Saturated_Pixels = Saturated_Pixels / m_totBlobs;
        per_Saturation = per_Saturation / m_totBlobs;

    }
    
    public String toCSVString( boolean bIs2D)
    {
        String cStr = "";
        
        if (bIs2D)
        {
            cStr =  m_imgTitle + "," + Double.toString(m_totBlobs) + "," + Double.toString(m_CoreX) + ","
                    + Double.toString(m_CoreY) + "," 
                    + Double.toString(AreaXY) + "," + Double.toString(Intensity) + ","
                    + Double.toString(Background) + "," + Double.toString(MinI) + ","
                    + Double.toString(MaxI) + "," + Double.toString(Range) + ","
                    + Double.toString(Intensity_Integral) + "," + Double.toString(Saturated_Pixels) + "," 
                    + Double.toString(per_Saturation);
        }
        else
        {
            cStr =  m_imgTitle + "," + Double.toString(m_totBlobs) + "," + Double.toString(m_CoreX) + ","
                    + Double.toString(m_CoreY) + "," + Double.toString(m_CoreZ) + "," + 
                    Double.toString(Volume) + "," 
                    + Double.toString(AreaXY) + "," + Double.toString(AreaXZ) + "," + 
                    Double.toString(AreaYZ) + "," 
                    + Double.toString(Intensity) + ","
                    + Double.toString(Background) + "," + Double.toString(MinI) + ","
                    + Double.toString(MaxI) + "," + Double.toString(Range) + ","
                    + Double.toString(Intensity_Integral) + "," + 
                    Double.toString(Saturated_Pixels) + "," 
                    + Double.toString(per_Saturation);
        }
        
        return cStr;
        
    }
    
    public void SetImageName(String imgName)
    {
        m_imgTitle = imgName;
    }
    

    
    
}
