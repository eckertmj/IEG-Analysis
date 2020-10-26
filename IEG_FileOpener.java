/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.plugin.Colors;
import ij.util.*;
import ij.macro.*;
import ij.measure.*;


/**
 *
 * @author vivek.trivedi
 */
public class IEG_FileOpener extends Thread {
    
    public boolean bStatus = false;
    
    public String strFileName = "";
    
    public void run ()
    {
        IJ.open(strFileName);
        bStatus=true;
        
    }
    
    
}
