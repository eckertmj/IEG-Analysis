





import ij.IJ;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 *
 * @author  Vivek ref Kurt
 *
 */
public class IEGReadXML {
    private boolean verbose;
    private DocumentBuilderFactory dbf;
    private DocumentBuilder db;
    private Document doc;
    private String str;
    private boolean bIEG;
    private boolean isNu =false;
    public static final int IMAGE_FILE_PATH = 0;
    public static final int CURRENT_TYPE = 1;
    public static final int IEG_TYPE = 2;
    public static final int IEG_VER = 3;
    public static final int IEG_TOTAL_MARKER = 4;
    public static final int IEG_HOMER_FILENAME = 5;
    public static final int IEG_ARC_FILENAME = 6;
    public static final int IEG_CONF = 7;
    /**
     * Creates a new instance of ODReadXMLODD
     */
    public IEGReadXML(String XMLFilePath) {
        setVerbose(verbose);
        bIEG = false;
        try{
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File(XMLFilePath));
            doc.getDocumentElement().normalize();
            
            if (XMLFilePath.contains("_Nu_"))
                this.isNu = true;
            
        } catch (SAXException e) {
            System.out.println(e.getMessage());
            System.out.println(XMLFilePath + " is not well-formed.");
        } catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        } catch (ParserConfigurationException e){
            System.out.println("ParserConfigurationException " + e.getMessage());
        }
    }
    public IEGReadXML(String XMLFilePath, boolean bIEG) {
        this.bIEG = bIEG;
        setVerbose(verbose);
        try{
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File(XMLFilePath));
            doc.getDocumentElement().normalize();
            if (XMLFilePath.contains("_Nu_"))
                this.isNu = true;
        } catch (SAXException e) {
            System.out.println(e.getMessage());
            System.out.println(XMLFilePath + " is not well-formed.");
        } catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        } catch (ParserConfigurationException e){
            System.out.println("ParserConfigurationException " + e.getMessage());
        }
    }
    
    public String readImgProperties(int valueID){ //as URL
        try{
            switch(valueID){
                case(IMAGE_FILE_PATH):
                    str = readSingleValue(doc,"Image_Filename");
                    break;
                case(CURRENT_TYPE):
                    str = readSingleValue(doc,"Current_Type");
                    break;
                case(IEG_TYPE):
                    str = readSingleValue(doc,"IEG");
                    break;
                case(IEG_VER):
                    str = readSingleValue(doc,"IEG_Version");
                    break;
                case(IEG_TOTAL_MARKER):
                    str = readSingleValue(doc,"Total_IEG_Markers");
                    break;
                case(IEG_HOMER_FILENAME):
                    str = readSingleValue(doc,"Homer_ResultFileName");
                    break;
                case(IEG_ARC_FILENAME):
                    str = readSingleValue(doc,"Arc_ResultFileName");
                    break;
                case(IEG_CONF):
                    str = readSingleValue(doc,"Config_Param");
                    break;
            }
            if (str !=null){
                return str;
            }
        }
        catch(Exception e)
        {
           IJ.log("XML Tag Reading Error: "); 
        }
        return null;
    }
    
    public Vector readMarkerData(){
        Vector typeVector = new Vector();
        
        NodeList markerTypeNodeList = getNodeListFromTag(doc,"Marker_Type");
        for (int i=0; i<markerTypeNodeList.getLength(); i++){
            Element markerTypeElement = getElement(markerTypeNodeList, i);
            NodeList typeNodeList = markerTypeElement.getElementsByTagName("Type");
            IEGCellCntrMarkerVector markerVector;
            if (!isNu)
                markerVector = new IEGCellCntrMarkerVector(Integer.parseInt(readValue(typeNodeList , 0)));
            else
            {
               // String tName = typeNodeList.getEle
                markerVector = new IEGCellCntrMarkerVector(i);
                markerVector.bIsFoci = false;
                markerVector.m_tag = readValue(typeNodeList,0);
            }
            int cellCount = 0;

            if (bIEG)
            {
                typeNodeList = markerTypeElement.getElementsByTagName("Cell_Count");
                cellCount = Integer.parseInt(readValue(typeNodeList , 0));
            }
            
            markerVector.setTotalCells(cellCount);
            NodeList markerNodeList = markerTypeElement.getElementsByTagName("Marker");
            for(int j=0; j<markerNodeList.getLength(); j++){
                Element markerElement = getElement(markerNodeList, j);
                
                 NodeList markerIdNodeList = null;
                if (bIEG)
                    markerIdNodeList = markerElement.getElementsByTagName("MarkerId");
                NodeList markerXNodeList = markerElement.getElementsByTagName("MarkerX");
                NodeList markerYNodeList = markerElement.getElementsByTagName("MarkerY");
                NodeList markerZNodeList = markerElement.getElementsByTagName("MarkerZ");
                IEGCellCntrMarker marker = new IEGCellCntrMarker();
                if(bIEG)
                    marker.setId(Integer.parseInt(readValue(markerIdNodeList,0)));
                marker.setX(Integer.parseInt(readValue(markerXNodeList,0)));
                marker.setY(Integer.parseInt(readValue(markerYNodeList,0)));
                marker.setZ(Integer.parseInt(readValue(markerZNodeList,0)));
                markerVector.add(marker);
            }
            typeVector.add(markerVector);
        }
        return typeVector;
    }
    
    private String readValue(NodeList nodeList, int index) throws NullPointerException{
        Element element = getElement(nodeList, index);
        debugReport("Element = "+element.getNodeName());
        NodeList elementNodeList = getChildNodes(element);
        String str = getValue(elementNodeList, 0);
        return str;
    }
    private String[] readMarker(NodeList nodeList, int index) throws NullPointerException{
        Element element = getElement(nodeList, index);
        debugReport("Element = "+element.getNodeName());
        NodeList elementNodeList = getChildNodes(element);
        String str[] = {getValue(elementNodeList, 0),getValue(elementNodeList, 1),getValue(elementNodeList, 2)};
        return str;
    }
    private String readSingleValue(Document doc, String elementName){
        try
        {
        NodeList nodeList = getNodeListFromTag(doc,elementName);
        Element element = getElement(nodeList, 0);
        if (element==null)
            return null;
        nodeList = getChildNodes(element);
        String str = getValue(nodeList, 0);
        return str;
        }
        catch(Exception e)
        {
            IJ.log("Marker File Reading Exception ("+elementName+")");
            return null;
        }
    }
    private NodeList getNodeListFromTag(Document doc, String elementName){
        NodeList nodeList = doc.getElementsByTagName(elementName);
        return nodeList;
    }
    private NodeList getChildNodes(Element element){
        NodeList nodeList = element.getChildNodes();
        return nodeList;
    }
    private Element getElement(NodeList nodeList, int index){
        Element element = (Element)nodeList.item(index);
        return element;
    }
    private String getValue(NodeList nodeList, int index){
        String str = ((Node)nodeList.item(index)).getNodeValue().trim();
        return str;
    }
    
    
    public void debugReport(String report){
        if (verbose)
            System.out.println(report);
    }
    public void setVerbose(boolean verbose){
        this.verbose = verbose;
    }
    public boolean isVerbose(){
        return verbose;
    }
}
