/*
 * ODODD.java
 *
 * Created on 23 November 2004, 22:56
 */

import ij.*;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ListIterator;
import java.util.Vector;

/**
 *
 * @author  kurt
 */
public class IEGWriteXML{
    private OutputStream XMLFileOut;
    private OutputStream XMLBuffOut;
    private OutputStreamWriter out;
    
    /**
     * Creates a new instance of ODWriteXMLODD
     */
    public IEGWriteXML(String XMLFilepath) {
        try{
            XMLFileOut= new FileOutputStream(XMLFilepath); // add FilePath
            XMLBuffOut= new BufferedOutputStream(XMLFileOut);
            out = new OutputStreamWriter(XMLBuffOut, "UTF-8");
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found "+ e.getMessage());
        } catch (UnsupportedEncodingException e) {
            System.out.println("This VM does not support the UTF-8 character set. "+e.getMessage());
        }
    }
    
    public boolean writeXML(String imgFilename, Vector typeVector, int currentType){
        try {
            out.write("<?xml version=\"1.0\" ");
            out.write("encoding=\"UTF-8\"?>\r\n");
            out.write("<CellCounter_Marker_File>\r\n");
            
            // write the image properties
            out.write(" <Image_Properties>\r\n");
            out.write("     <Image_Filename>"+ imgFilename + "</Image_Filename>\r\n");
            out.write(" </Image_Properties>\r\n");
            
            // write the marker data
            out.write(" <Marker_Data>\r\n");
            out.write("     <Current_Type>"+ currentType + "</Current_Type>\r\n");
            ListIterator it = typeVector.listIterator();
            while(it.hasNext()){
                IEGCellCntrMarkerVector markerVector = (IEGCellCntrMarkerVector)it.next();
                int type = markerVector.getType();
                out.write("     <Marker_Type>\r\n");
                out.write("         <Type>" +type+ "</Type>\r\n");
                ListIterator lit = markerVector.listIterator();
                while(lit.hasNext()){
                    IEGCellCntrMarker marker = (IEGCellCntrMarker)lit.next();
                    int x = marker.getX();
                    int y = marker.getY();
                    int z = marker.getZ();
                    out.write("         <Marker>\r\n");
                    out.write("             <MarkerX>" +x+ "</MarkerX>\r\n");
                    out.write("             <MarkerY>" +y+ "</MarkerY>\r\n");
                    out.write("             <MarkerZ>" +z+ "</MarkerZ>\r\n");
                    out.write("         </Marker>\r\n");
                }
                out.write("     </Marker_Type>\r\n");
            }
            
            out.write(" </Marker_Data>\r\n");
            out.write("</CellCounter_Marker_File>\r\n");
            
            out.flush();  // Don't forget to flush!
            out.close();
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
    public boolean writeXML(String[] rFileNames, Vector typeVector, int currentType, boolean bZRange, String IEG_ver, String strConfig){
            if (rFileNames.length < 2 )
                return false;
        try {
            
            boolean isNu = true;
            
            out.write("<?xml version=\"1.0\" ");
            out.write("encoding=\"UTF-8\"?>\r\n");
            out.write("<CellCounter_Marker_File>\r\n");
            
            out.write(" <IEG>"+"Y"+"</IEG>\r\n");
            out.write(" <IEG_Version>"+IEG_ver+"</IEG_Version>\r\n");
            
            // write the image properties
            out.write(" <Image_Properties>\r\n");
            out.write("     <Image_Filename>"+ rFileNames[0] + "</Image_Filename>\r\n");
            
            out.write(" </Image_Properties>\r\n");
            
            ListIterator it = typeVector.listIterator();   
            IEGCellCntrMarkerVector _markerVector = (IEGCellCntrMarkerVector)it.next();            
            if (_markerVector.bIsFoci)
            {
                isNu = false;
                out.write(" <Config_Param>"+ strConfig + "</Config_Param>\r\n");

                for (int i = 1; i < rFileNames.length; i++) 
                {

                    if (rFileNames[i].indexOf("Homer")> 0)
                        out.write("     <Homer_ResultFileName>"+ rFileNames[i] + "</Homer_ResultFileName>\r\n");
                    if (rFileNames[i].indexOf("Arc")> 0)
                        out.write("     <Arc_ResultFileName>"+ rFileNames[i] + "</Arc_ResultFileName>\r\n");

                }
            }
            
            out.write(" <Total_IEG_Markers>"+typeVector.size()+"</Total_IEG_Markers>\r\n");
            // write the marker data
            out.write(" <Marker_Data>\r\n");
            out.write("     <Current_Type>"+ currentType + "</Current_Type>\r\n");
            it = typeVector.listIterator();
            while(it.hasNext()){
                IEGCellCntrMarkerVector markerVector = (IEGCellCntrMarkerVector)it.next();
                int type = markerVector.getType();
                out.write("     <Marker_Type>\r\n");
                if (!isNu)
                    out.write("         <Type>" +type+ "</Type>\r\n");
                else
                    out.write("         <Type>" +markerVector.m_tag+ "</Type>\r\n");
                    
                out.write("         <Cell_Count>" + markerVector.getTotalCells() + "</Cell_Count>\r\n");
                
              //  out.write("         <count>" +markerVector.size()+ "</count>\r\n");
                ListIterator lit = markerVector.listIterator();
                while(lit.hasNext()){
                    IEGCellCntrMarker marker = (IEGCellCntrMarker)lit.next();
                    int x = marker.getX();
                    int y = marker.getY();
                    int z = marker.getZ();
                    int id = marker.getId();
                    int z1, z2;
                /*    if ( bZRange )
                    {
                        IEGCellCntrMarker.ZRange zR = marker.getZRange();
                         z1 = zR.z1; z2 = zR.z2;
                    }
                    else*/
                    {
                        z1 = z2 = z;
                    }
                    
                    for ( int i = z1; i <= z2; i++ )
                    {
                        out.write("         <Marker>\r\n");
                        out.write("             <MarkerId>" +id+ "</MarkerId>\r\n");
                        out.write("             <MarkerX>" +x+ "</MarkerX>\r\n");
                        out.write("             <MarkerY>" +y+ "</MarkerY>\r\n");
                        out.write("             <MarkerZ>" +i+ "</MarkerZ>\r\n");
                        out.write("         </Marker>\r\n");
                    }
                }
                 out.write("     </Marker_Type>\r\n");
            }
            
            out.write(" </Marker_Data>\r\n");
            out.write("</CellCounter_Marker_File>\r\n");
            
            out.flush();  // Don't forget to flush!
            out.close();
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
    
}
