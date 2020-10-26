
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
import ij.measure.*;
import ij.util.*;
import java.sql.*;
import java.awt.*;
import java.util.*;
import java.util.ArrayList;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.JFrame;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef.HWND;
import java.lang.Math.*;


/**
 *
 * @author vivek.trivedi
 */
public class PixelCollection3D extends Win32Thread 
{
        private int Maximum, Minimum;
        private short nMarker;
        private ArrayList<Integer> pX,pY,pZ,pValue,pBKGLevel, pBKValue;
        private boolean isDescend=false;
        private int MaxIndex,ThreadIndex;
        public ArrayList<IEG_MaximaPoint> m_vMaximaPoints;
        public int m_TotalSaturatedPixels=0;
        int zSt, zEnd;
        int xCenterPix, yCenterPix, zCenterPix;
        public int m_BlobId;
        public String IJ_STatus_MSG_Prifix = null;
        
        public int m_intIntegral = 0;
        
        public int m_afMask = 0;
        
        public IEG_Analysis m_Parent = null;


        public class IEG_MaximaPoint
        {
            public int pX,pY,pZ,pValue,pBKGLevel,pBKValue;

            public IEG_MaximaPoint(int pX, int pY, int pZ, int pValue, int pBKGLevel, int pBKValue) {
                this.pX = pX;
                this.pY = pY;
                this.pZ = pZ;
                this.pValue = pValue;
                this.pBKGLevel = pBKGLevel;
                this.pBKValue = pBKValue;
                
            }

            public IEG_MaximaPoint() {
            }

        }


        PixelCollection3D (int indexofthread, IEG_Analysis rParent)
        {
                pX=new ArrayList<Integer>();
                pY=new ArrayList<Integer>();
                pZ=new ArrayList<Integer>();
                pBKGLevel=new ArrayList<Integer>();
                pBKValue=new ArrayList<Integer>();
                pValue=new ArrayList<Integer>();
                this.Maximum=Integer.MIN_VALUE;
                this.Minimum=Integer.MAX_VALUE;
                this.ThreadIndex=indexofthread;
                m_vMaximaPoints = new  ArrayList<IEG_MaximaPoint>();
                m_Parent = rParent;
                m_BlobId = -1;
        }
        
        PixelCollection3D (IEG_Analysis rParent)
        {
                pX=new ArrayList<Integer>();
                pY=new ArrayList<Integer>();
                pZ=new ArrayList<Integer>();
                pValue=new ArrayList<Integer>();
                pBKValue=new ArrayList<Integer>();
                pBKGLevel=new ArrayList<Integer>();
                this.Maximum=Integer.MIN_VALUE;
                this.Minimum=Integer.MAX_VALUE;
                m_vMaximaPoints = new  ArrayList<IEG_MaximaPoint>();
                m_Parent = rParent;
                m_BlobId = -1;
        }

        PixelCollection3D (IEG_Analysis rParent, int afMask)
        {
                pX=new ArrayList<Integer>();
                pY=new ArrayList<Integer>();
                pZ=new ArrayList<Integer>();
                pValue=new ArrayList<Integer>();
                pBKValue=new ArrayList<Integer>();
                pBKGLevel=new ArrayList<Integer>();
                this.Maximum=Integer.MIN_VALUE;
                this.Minimum=Integer.MAX_VALUE;
                m_vMaximaPoints = new  ArrayList<IEG_MaximaPoint>();
                m_Parent = rParent;
                m_BlobId = -1;
                m_afMask = afMask;
        }

        int addPixel(int x1, int y1, int z1, int pixelvalue){
                this.pX.add( new Integer(x1));
                this.pY.add( new Integer(y1));
                this.pZ.add( new Integer(z1));
                this.pValue.add(new Integer(pixelvalue));
                this.pBKValue.add(new Integer(0));
                this.m_vMaximaPoints.add(new IEG_MaximaPoint(x1, y1, z1, pixelvalue, 0, 0));
                if (pixelvalue>=255)
                    m_TotalSaturatedPixels++;
                
                m_intIntegral += pixelvalue;
                return pX.size();
        }

        int addPixelWithBlue(int x1, int y1, int z1, int pixelvalue, int pixelBlueValue){
                this.pX.add( new Integer(x1));
                this.pY.add( new Integer(y1));
                this.pZ.add( new Integer(z1));
                this.pValue.add(new Integer(pixelvalue));
                this.pBKValue.add(new Integer(pixelBlueValue));
                this.m_vMaximaPoints.add(new IEG_MaximaPoint(x1, y1, z1, pixelvalue, (int)0, (int)pixelBlueValue));
                if (pixelvalue>=255)
                    m_TotalSaturatedPixels++;
                
                m_intIntegral += pixelvalue;
                return pX.size();
        }
        
        
        int addPixel(int index, int x1, int y1, int z1, int pixelvalue){   	 	
                this.pX.add(index, new Integer(x1));
                this.pY.add(index, new Integer(y1));
                this.pZ.add(index, new Integer(z1));
                this.pValue.add(index, new Integer(pixelvalue));
                this.pBKValue.add(new Integer(0));
                this.m_vMaximaPoints.add(new IEG_MaximaPoint(x1, y1, z1, pixelvalue, 0, 0));
                if (pixelvalue>=255)
                    m_TotalSaturatedPixels++;
                m_intIntegral += pixelvalue;
                return index;
        }

        int addPixelWithBlue(int index, int x1, int y1, int z1, int pixelvalue, int pixelBlueValue){   	 	
                this.pX.add(index, new Integer(x1));
                this.pY.add(index, new Integer(y1));
                this.pZ.add(index, new Integer(z1));
                this.pValue.add(index, new Integer(pixelvalue));
                this.pBKValue.add(new Integer(pixelBlueValue));
                this.m_vMaximaPoints.add(new IEG_MaximaPoint(x1, y1, z1, pixelvalue, 0, pixelBlueValue));
                if (pixelvalue>=255)
                    m_TotalSaturatedPixels++;
                m_intIntegral += pixelvalue;
                return index;
        }
        

        int addPixelDescend(int x1, int y1, int z1, int pixelvalue){			
                if(getPixelNumber()==0 || pixelvalue<=this.getValue(getPixelNumber()-1)){
                        return addPixel(x1,y1,z1,pixelvalue);
                }else{
                        for (int i=getPixelNumber()-1; i>=0; i--){
                                if (pixelvalue<=this.getValue(i)){
                                        return addPixel(i+1,x1,y1,z1,pixelvalue);
                                }
                        }
                        return addPixel(0,x1,y1,z1,pixelvalue);
                }

        }

        // mike, this is the one that's being used
        // pixels are ordered brightest to darkest??
        // this would cause errors in specifying the blob center as he calls getX(0) for the center
        // which will contain the FIRST brightest pixel.  If there are several pixels with the same
        // intensity (eg, saturated) then center will be off.
        int addPixelDescendWithBlue(int x1, int y1, int z1, int pixelvalue, int pixelBlueValue){			
                if(getPixelNumber()==0 || pixelvalue<=this.getValue(getPixelNumber()-1)){
                        return addPixelWithBlue(x1,y1,z1,pixelvalue, pixelBlueValue);
                }else{
                        for (int i=getPixelNumber()-1; i>=0; i--){
                                if (pixelvalue<=this.getValue(i)){
                                        return addPixelWithBlue(i+1,x1,y1,z1,pixelvalue, pixelBlueValue);
                                }
                        }
                        return addPixelWithBlue(0,x1,y1,z1,pixelvalue, pixelBlueValue);
                }

        }

        int addPixel(int index, int x1, int y1, int z1, int pixelvalue,int backgroundlevel){
                this.pX.add(index, new Integer(x1));
                this.pY.add(index, new Integer(y1));
                this.pZ.add(index, new Integer(z1));
                this.pValue.add(index, new Integer(pixelvalue));
                this.pBKGLevel.add(index,new Integer(backgroundlevel));
                this.m_vMaximaPoints.add(new IEG_MaximaPoint(x1, y1, z1, pixelvalue, backgroundlevel,0));
                if (pixelvalue>=255)
                   m_TotalSaturatedPixels++;
                m_intIntegral += pixelvalue;

                return index;
        }
        
        int addPixelWithBlue(int index, int x1, int y1, int z1, int pixelvalue,int backgroundlevel, int pixelBlueValue){
                this.pX.add(index, new Integer(x1));
                this.pY.add(index, new Integer(y1));
                this.pZ.add(index, new Integer(z1));
                this.pValue.add(index, new Integer(pixelvalue));
                this.pBKGLevel.add(index,new Integer(backgroundlevel));
                this.pBKValue.add(new Integer(pixelBlueValue));
                this.m_vMaximaPoints.add(new IEG_MaximaPoint(x1, y1, z1, pixelvalue, backgroundlevel,pixelBlueValue));
                if (pixelvalue>=255)
                   m_TotalSaturatedPixels++;
                m_intIntegral += pixelvalue;
                return index;
        }
        
        
        int addPixelDescend(int x1, int y1, int z1, int pixelvalue,int backgroundlevel){
                if(getPixelNumber()==0 || pixelvalue<=this.getValue(getPixelNumber()-1)){
                        return addPixel(getPixelNumber(),x1,y1,z1,pixelvalue,backgroundlevel);
                }
                else
                {
                        for (int i=getPixelNumber()-1; i>=0; i--)
                        {
                                if (pixelvalue<=this.getValue(i))
                                {
                                        return addPixel(i+1,x1,y1,z1,pixelvalue,backgroundlevel);

                                }
                        }
                        return addPixel(0,x1,y1,z1,pixelvalue,backgroundlevel);
                }

        }
        
        int addPixelDescendWithBlue(int x1, int y1, int z1, int pixelvalue,int backgroundlevel, int pixelBlueValue){
                if(getPixelNumber()==0 || pixelvalue<=this.getValue(getPixelNumber()-1)){
                        return addPixelWithBlue(getPixelNumber(),x1,y1,z1,pixelvalue,backgroundlevel,pixelBlueValue);
                }
                else
                {
                        for (int i=getPixelNumber()-1; i>=0; i--)
                        {
                                if (pixelvalue<=this.getValue(i))
                                {
                                        return addPixelWithBlue(i+1,x1,y1,z1,pixelvalue,backgroundlevel, pixelBlueValue);

                                }
                        }
                        return addPixelWithBlue(0,x1,y1,z1,pixelvalue,backgroundlevel, pixelBlueValue);
                }

        }
        
        
        boolean analyze(int i,int j)
        {  
                int x0, y0, z0, pixValue0;
                int n;
                int backgroundlevel;
                int bck;
                float factor;

                x0= m_Parent.CollectionMax.getX(i);
                y0= m_Parent.CollectionMax.getY(i);
                z0 =m_Parent.CollectionMax.getZ(i);
                bck = m_Parent.CollectionMax.getBackGround(i);
                backgroundlevel= m_Parent.CollectionMax.getBackgroundLevel(i);

                pixValue0 = m_Parent.CollectionMax.getValue(i);
                
                if (pixValue0<0)
                    return false;
                
                m_Parent.MinIValue = 1;
                short maskVal= -1;
                if (m_Parent.m_bSaveMemory)
                    maskVal = m_Parent.getMaskPixel(x0,y0,z0);
                else
                    maskVal = m_Parent.MaskPixel [x0][y0][z0];


                if((maskVal>1))
                {
                        m_Parent.CollectionMax.removePixel(i);		
                        return false;
                }
                else
                { 

                        this.nMarker=(short)(m_Parent.nFoci+this.ThreadIndex);
                        this.clear();
                        this.addPixelDescendWithBlue(x0, y0, z0, pixValue0, bck);
                        m_Parent.setMaskPixel(x0,y0,z0, (short)(this.nMarker));
                        growFromOpenBorderIndex(0);

                        return true;
                }			

        }
        
        void cleanSingePixels(){
                int x,y,z,x1,y1,z1;

                for (int j=0;j<getPixelNumber();j++){
                        x=getX(j);
                        y=getY(j);
                        z=getZ(j);
                        nextPixel:
                        for(int i=0;i<m_Parent.N_SURROUNDING;i++)
                        {
                                x1=x+m_Parent.Surrounding[i*3];
                                y1=y+m_Parent.Surrounding[i*3+1];
                                z1=z+m_Parent.Surrounding[i*3+2];
                                
                                if (m_Parent.withInBoundary(x1,y1,z1))
                                {
                                    
                                    short maskVal= -1;
                                    maskVal = m_Parent.getMaskPixel(x1,y1,z1);
                                    
                                    if( maskVal == this.nMarker ) 
                                    {
                                            break nextPixel;	
                                    }
                                    else if( i == m_Parent.N_SURROUNDING-1 )
                                    {
                                                m_Parent.setMaskPixel(x,y,z, (short) m_Parent.RIM_VALUE );
                                                this.removePixel(x,y,z);
                                    }
                                }
                        }
                }
                return ;   
        }
        
        void clear(){
                this.pX.clear();
                this.pY.clear();
                this.pZ.clear();
                this.pValue.clear();
                this.pBKValue.clear();
                this.Maximum=Integer.MIN_VALUE;
        }

        // mike; NOTE: center coordinates are set inside here for some reason
        float getAreaXY(){
                float a;
                int n=getPixelNumber();
                if (n>0)
                {
                    a=0;
                    xCenterPix = Math.round(getCoreX()); //getX(0); 
                    yCenterPix = Math.round(getCoreY()); //getY(0);
                    zCenterPix = Math.round(getCoreZ()) + 1; //getZ(0);
                }
                else 
                    return 0;

                boolean isExisted=false;
                int x1,y1;
                for (int i=0;i<n;i++){
                        x1=getX(i);
                        y1=getY(i);
                        isExisted=false;
                        for (int j=0;j<i;j++){
                                if(x1==getX(j)&y1==getY(j))isExisted=true;
                        }
                        if(!isExisted) a++;
                }
                return a/(m_Parent.VoxelX*m_Parent.VoxelY);	
        }
        
        int getIntensityINtegral()
        {
            int v=0;
            for(int i=0; i<getPixelNumber(); i++) {
                v += getValue(i);
            }
            return v;
        }
        float getAreaYZ(){
                float a;
                int n=getPixelNumber();
                if (n>0) a=0;
                else return 0;
                boolean isExisted=false;
                int y1,z1;
                for (int i=0;i<n;i++){
                        z1=getZ(i);
                        y1=getY(i);
                        isExisted=false;
                        for (int j=0;j<i;j++){
                                if(z1==getZ(j)&y1==getY(j)) isExisted=true;
                        }
                        if(!isExisted) a++;
                }
                return a/(m_Parent.VoxelY*m_Parent.VoxelZ);	
        }
        float getAreaXZ(){

                float a;
                int n=getPixelNumber();
                if (n>0) a=0;
                else return 0;
                boolean isExisted=false;
                int x1,z1;
                zSt = Integer.MAX_VALUE;
                zEnd = Integer.MIN_VALUE;
                for (int i=0;i<n;i++){
                        z1=getZ(i);
                        x1=getX(i);
                        {
                            if (z1<zSt)
                                zSt=z1;
                            if (z1>zEnd)
                                zEnd=z1;
                        }
                        isExisted=false;
                        for (int j=0;j<i;j++){
                                if(z1==getZ(j)&x1==getX(j))isExisted=true;
                        }
                        if(!isExisted) a++;
                }
                return a/(m_Parent.VoxelX*m_Parent.VoxelZ);	
        }
        
        int getBackGround(int index)
        {
            if (index >= pBKGLevel.size())
                return 0;
            else
                return (pBKValue.get(index)).intValue();
        }
        int getBackgroundLevel(int index){
            if (index >= pBKGLevel.size())
                return 0;
            else
                return (pBKGLevel.get(index)).intValue();

        }
        float getCenterX(){
            int mn, mx, x;

            mn = mx = pX.get(0);
            for(int i=1; i < getPixelNumber(); i++) {
                x = pX.get(i);
                if(x < mn) mn = x;
                else if(x > mx) mx = x;
            }
            return (mx-mn)/2.0f + mn;
        }
        float getCenterY(){
            int mn, mx, x;

            mn = mx = pY.get(0);
            for(int i=1; i < getPixelNumber(); i++) {
                x = pY.get(i);
                if(x < mn) mn = x;
                else if(x > mx) mx = x;
            }
            return (mx-mn)/2.0f + mn;
        }
        float getCenterZ(){
            int mn, mx, x;

            mn = mx = pZ.get(0);
            for(int i=1; i < getPixelNumber(); i++) {
                x = pZ.get(i);
                if(x < mn) mn = x;
                else if(x > mx) mx = x;
            }
            return (mx-mn)/2.0f + mn;
        }
        float getCoreX(){		
                long a=0;
                float b=getMeanValue();
                for (int i=0;i<getPixelNumber();i++){
                        a += pX.get(i) * pValue.get(i);
                }
                return a / b / getPixelNumber();
        }
       
        float getCoreY(){		
                long a=0;
                float b=getMeanValue();
                for (int i=0;i<getPixelNumber();i++){
                        a += pY.get(i) * pValue.get(i);
                }
                return  a / b / getPixelNumber();
        }

        float getCoreZ(){		
                long a=0;
                float b=getMeanValue();
                for (int i=0;i<getPixelNumber();i++){
                        a += pZ.get(i) * pValue.get(i);
                }
                return a / b / getPixelNumber();
        }

        int getNumZLayers(){
            int mn = 1000;
            int mx = 0;
            int z;
            
            for(int i=0; i<getPixelNumber(); i++) {
                z = getZ(i);
                if(z<mn) {
                    mn = z;
                }
                else if(z>mx) {
                    mx = z;
                }
            }
            return mx-mn+1;
        }

        int getLowerOneThirdAverage(){
                float a=0;
                int n=getPixelNumber();
                int m=n-(int)(n*2/3);

                for(int i=(int)(n*2/3);i<n;i++){
                        a=a+getValue(i)/m;
                }
                return (int)a;
        }
        int getMiddleOneThirdAverage(){
                float a=0;
                int n=getPixelNumber();
                int m=(int)(n/3);

                for(int i=m;i<2*m;i++){
                        a=a+getValue(i)/m;
                }
                return (int)a;
        }

        float getMeanValue(){
                int a=0;
                for (int i=0;i<getPixelNumber();i++){
                        a += pValue.get(i);
                }
                return (float)a/getPixelNumber();
        }

        float getVariance(){
            float m = getMeanValue();
            float s=0;

            for(int i=0; i<getPixelNumber(); i++) {
                s += (pValue.get(i)-m) * (pValue.get(i)-m);
            }
            return s/(getPixelNumber()-1);
        }

        int getPixelIndex(int x1,int y1, int z1){
                for (int i=0;i<getPixelNumber();i++){
                        if(getX(i)==x1 & getY(i)==y1 & getZ(i)==z1){
                                return i;
                        }			
                }
                return -1;
        }

        int getPixMaximum(){
            int v = getValue(0);
            for (int i=1;i<getPixelNumber(); i++) {
                if(getValue(i) > v) v = getValue(i);
            }
            return v;
        }

        int getPixMinimum(){
            int v = getValue(0);
            for (int i=1;i<getPixelNumber(); i++){
                if(getValue(i) < v) v = getValue(i);
            }
            return v;
        }

        int getPixelNumber(){
                return pX.size();
        }

        int getPixRange(){ 
                return getPixMaximum()-getPixMinimum();
        }

        int [] getValueList(){
                Integer a;
                int []b=new int[getPixelNumber()];
                for (int i=0;i<getPixelNumber();i++){
                        a=pValue.get(i);
                        b[i]=a.intValue();
                }
                return b;
        }
        int getX(int i){
            if (pX.size()>0 && i >= 0 && i < pX.size())
                return (pX.get(i)).intValue();
            else
            {
                int ii=0;
                ii=1;
                return 0;
            }
        }
        int getY(int i){
            
            //    return (pY.get(i)).intValue();
            if (pY.size()>0 && i >= 0 && i < pY.size())
                return (pY.get(i)).intValue();
            else
            {
                int ii=0;
                ii=1;
                return 0;
            }
        }
        int getZ(int i){
                //return (pZ.get(i)).intValue();
            if (pZ.size()>0 && i >= 0 && i < pZ.size())
                return (pZ.get(i)).intValue();
            else
            {
                int ii=0;
                ii=1;
                return 0;
            }
        }
        int getValue(int i){
            if (i < pValue.size())
                return (pValue.get(i)).intValue();
            else
                return -1;
        }

        float getVolume(){
                return getPixelNumber()/(m_Parent.VoxelX*m_Parent.VoxelY*m_Parent.VoxelZ);
        }
        
        double GetBackgoundPer(int minBlue)
        {
            int totBPix = 0;
            for ( int i = 0; i < pBKValue.size(); i++ )
            {
                if ( pBKValue.get(i) >= minBlue )
                    totBPix++;
            }
            
            if ( pBKValue.size() > 0 )
            {
                return ( ( totBPix * 100.0 ) / pBKValue.size() );
            }
            
            return 0.0;
        }
        
        boolean isEnoughPeak(int minPeak)
        {
            if (pValue.size()>0)
            {
                if (pValue.get(0) >= minPeak)
                    return true;
            }
            return false;
        }

        boolean isANeighborMax(int x1,int y1,int z1, int pixelvalue){   
                int x2,y2,z2;
                
                float sqaureDistance=0;

                for (int i=0;i<getPixelNumber()-1; i++){
                        x2=getX(i);
                        y2=getY(i);
                        z2=getZ(i);
                        float sqD = m_Parent.squareD(x1,y1,z1,x2,y2,z2);
                        if (sqD<=3)
                        {
                            return true;
                        }

                }
                return false;

        }

        boolean isANeighborMaxFast(int x1,int y1,int z1, int pixelvalue, boolean[][][] neighborMatrix)
        {   //check whether an input pixel is a neighbor of the pixels in the collection or not
            //find the scanned neighbour (left and top) -- will be very fast
            return false;    
        }



        boolean growFromOpenBorderIndex(int indexstartfrom){  

                int x=0;int y=0;int z=0;int x1=0;int y1=0;int z1=0;
                int openneighbourindex=0;int openborderindex=0; 
                int neighbourpixelvalue=0; int neighbourpixelvalueBck=0; int borderpixelvalue=0; 
                boolean isOpen=false;
                //IJ.log("\n\ngrow function, savemem: " + m_Parent.m_bSaveMemory);


                openneighbourindex=0;
                findOpenBorderIndex:
                for (int j=indexstartfrom;j<getPixelNumber();j++){
                        x=getX(j);
                        y=getY(j);
                        z=getZ(j);
                        for(int i=0;i<m_Parent.N_SURROUNDING;i++){	
                                x1=x+m_Parent.Surrounding[i*3];
                                y1=y+m_Parent.Surrounding[i*3+1];
                                z1=z+m_Parent.Surrounding[i*3+2];
                                if (m_Parent.withInBoundary(x1,y1,z1)) 
                                {
                                    short maskVal= -1;
                                    maskVal = m_Parent.getMaskPixel(x1,y1,z1);
                                    if(maskVal==0) 
                                    {
                                            isOpen=true;
                                            openneighbourindex=i;
                                            openborderindex=j;
                                            borderpixelvalue=getValue(j);
                                            break findOpenBorderIndex;
                                    }        
                                }
                        }
                }
                if(!isOpen) 
                    return m_Parent.SUCCESS; 


                checkOpenBorderIndex:
                for(int i=openneighbourindex;i<m_Parent.N_SURROUNDING;i++)
                {			
                        x1=x+m_Parent.Surrounding[i*3];
                        y1=y+m_Parent.Surrounding[i*3+1];
                        z1=z+m_Parent.Surrounding[i*3+2];
                        if (m_Parent.withInBoundary(x1,y1,z1))
                        {

                                if (m_Parent.bProcessGreen)
                                    neighbourpixelvalue = m_Parent.getColorVal(x1, y1, z1, 2, m_Parent.imgPixel1);
                                else
                                    neighbourpixelvalue = m_Parent.getColorVal(x1, y1, z1, 1, m_Parent.imgPixel1);
                                        
                                short maskVal= -1;
                                maskVal = m_Parent.getMaskPixel(x1,y1,z1);
                                if(maskVal==0 & neighbourpixelvalue>borderpixelvalue)
                                {
                                    m_Parent.setMaskPixel( x1, y1, z1, m_Parent.RIM_VALUE ); 
                                    this.removePixel(x,y,z);
                                    if(growFromOpenBorderIndex(indexstartfrom) == m_Parent.SUCCESS) 
                                    {
                                            return m_Parent.SUCCESS;
                                    }
                                    else
                                    {
                                            return m_Parent.NOSUCCESS;
                                    }
                                }        
                        }
                }

                //IJ.log("x: " + x + ", y: " + y + ", z: " + z);
                Grow:
                for(int i=openneighbourindex;i<m_Parent.N_SURROUNDING;i++){			
                        x1=x+m_Parent.Surrounding[i*3];
                        y1=y+m_Parent.Surrounding[i*3+1];
                        z1=z+m_Parent.Surrounding[i*3+2];
                        //IJ.log("x: " + x1 + ", y: " + y1 + ", z: " + z1);

                        if (m_Parent.withInBoundary(x1,y1,z1))
                        {
                            if (m_Parent.bProcessGreen)
                                neighbourpixelvalue = m_Parent.getColorVal(x1, y1, z1, 2, m_Parent.imgPixel1);
                            else
                                neighbourpixelvalue = m_Parent.getColorVal(x1, y1, z1, 1, m_Parent.imgPixel1);
                                        
                            short maskVal= -1;
                            maskVal = m_Parent.getMaskPixel(x1,y1,z1);
                            if( maskVal == 0 ) 
                            {
                                if( neighbourpixelvalue <= borderpixelvalue )
                                {
                                    neighbourpixelvalueBck = m_Parent.getColorVal(x1, y1, z1, 3, m_Parent.imgPixel1);
                                    if (neighbourpixelvalue>=m_Parent.ToleranceValue /*&& minBlue >= m_Parent.UniformBackground*/)
                                    {
                                        m_Parent.setMaskPixel(x1,y1,z1,(short)this.nMarker); 
                                        addPixelDescendWithBlue(x1,y1,z1,neighbourpixelvalue,neighbourpixelvalueBck);
                                    }
                                    else
                                    {
                                        m_Parent.setMaskPixel(x1,y1,z1,m_Parent.RIM_VALUE); 
                                    }
                                }
                                else
                                {
                                    IJ.log("growFromOpenBorderIndex(): Error-->surrounding unmarked pixel value > open border pixel value");
                                }
                            }
                        }

                }
                if(growFromOpenBorderIndex(indexstartfrom++)==m_Parent.SUCCESS)
                {
                        return m_Parent.SUCCESS;
                }else
                {
                        return m_Parent.NOSUCCESS;
                }


        }
        
        void print(){
                for (int i=0;i<getPixelNumber(); i++){
                        IJ.log(i+"  x=  "+getX(i)+"   y= "+getY(i)+"   z= "+getZ(i)+" pixValue ="+getValue(i));		
                }

        }
        void removePixel(int i){            
                if (i<getPixelNumber() & i>=0){
                        this.pX.remove(i);
                        this.pY.remove(i);
                        this.pZ.remove(i);
                        int intVal = this.pValue.get(i);
                        this.pBKValue.remove(i);
                        
                        m_intIntegral -= intVal;
                        if (intVal>=255)
                            m_TotalSaturatedPixels--;
                        
                        this.pValue.remove(i);
                        this.pBKGLevel.remove(i);
                        this.m_vMaximaPoints.remove(i);
                }else{
                        IJ.log("pixel index_"+i+"_ is out of range of the collection ");
                }

        }
        void removePixel(short x1,short y1,short z1) {
                for (int i=0;i<getPixelNumber();i++){
                        if(getX(i)==x1 & getY(i)==y1 & getZ(i)==z1){
                                this.pX.remove(i);
                                this.pY.remove(i);
                                this.pZ.remove(i);
                                this.pBKValue.remove(i);
                                int intVal = this.pValue.get(i);
                                m_intIntegral -= intVal;
                                if (intVal>=255)
                                    m_TotalSaturatedPixels--;
                                
                                this.pValue.remove(i);
                                this.m_vMaximaPoints.remove(i);

                                return;
                        }			
                }
                IJ.log("removePixel(short x,y,z): Error-->can not remove pixel("+x1+", "+y1+ ", "+z1+"), it does not exist!!");
        }
        void removePixel(int x1,int y1,int z1){
                for (int i=0;i<getPixelNumber();i++){
                        if(getX(i)==x1 & getY(i)==y1 & getZ(i)==z1){
                                this.pX.remove(i);
                                this.pY.remove(i);
                                this.pZ.remove(i);
                                this.pBKValue.remove(i);
                                int intVal = this.pValue.get(i);
                                m_intIntegral -= intVal;
                                if (intVal>=255)
                                    m_TotalSaturatedPixels--;

                                this.pValue.remove(i);
                                this.m_vMaximaPoints.remove(i);
                                return;
                        }			
                }
                IJ.log("removePixel(int x,y,z): Error-->can not remove pixel("+x1+", "+y1+ ", "+z1+"), it does not exist!!");

        }
        void removeFromMask(){
                for (int i=0;i<getPixelNumber(); i++)
                {
                    if (m_Parent.m_bSaveMemory)
                        m_Parent.setMaskPixel(getX(i), getY(i), getZ(i),(short) 0);
                    else
                        m_Parent.MaskPixel[getX(i)][getY(i)][getZ(i)]=0;
                }
        }
        public void run (){
            
               setAffinity(m_afMask); 
               m_Parent.SearchObject[ThreadIndex]=this.analyze(MaxIndex,ThreadIndex);
        }

        int setPixel(int index, int x1, int y1, int z1, int pixelvalue){   	 	//replace the element at position (index) with a new 
                this.pX.set(index, new Integer(x1));
                this.pY.set(index, new Integer(y1));
                this.pZ.set(index, new Integer(z1));
                this.pValue.set(index, new Integer(pixelvalue));
                return index;
        }
        void setMaxIndex(int maxindex,int threadindex){
                this.MaxIndex=maxindex;
                this.ThreadIndex=threadindex;
        }
}
