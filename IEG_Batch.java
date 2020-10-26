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
import ij.util.*;
import java.sql.*;
import java.awt.*;
import java.util.*;
import java.util.ArrayList;
import java.awt.event.*;
import java.io.IOException;
import java.util.Vector;
import javax.swing.JFrame;


/**
 *
 * @author vivek.trivedi
 */
public class IEG_Batch implements PlugIn
{
    
    public void run(String arg) 
    {
        //IEGBatchManager.OpenIEGFile("C:\\IJ\\VJ\\dist\\1.bmp");
         IEGBatchManager bm = new IEGBatchManager();
      //   bm.showWindow();
         bm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
         bm.setVisible(true);
         
         return;
        
        
    }
    
    
}
