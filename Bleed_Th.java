
import static jcuda.runtime.JCuda.cudaFree;
import static jcuda.runtime.JCuda.cudaMalloc;
import static jcuda.runtime.JCuda.cudaMemcpy;
import static jcuda.runtime.cudaMemcpyKind.cudaMemcpyDeviceToHost;
import static jcuda.runtime.cudaMemcpyKind.cudaMemcpyHostToDevice;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jcuda.runtime.*;
import jcuda.runtime.JCuda.*;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.utils.KernelLauncher;
import java.io.InputStream;
import jcuda.runtime.cudaDeviceProp;
import ij.*;
import java.io.*;
import ij.gui.*;
import java.awt.*;
import javax.swing.JOptionPane;

/**
 * A simple example for an ImageJ Plugin that uses JCuda.
 */
public class Bleed_Th implements PlugInFilter
{
	/**
	 * The current image to operate on
	 */
    private ImagePlus currentImage = null;
    long startTime = 0; 
    cudaDeviceProp cudaDeviceDef;
    char[] intMat = new char[256];
    int[] intMatJ = new int[256];
    boolean m_bGPU = false;
    boolean m_bCreateChartQue = false;
    
    public boolean m_bSaveMemory = false;
//    public int[] m_imgPix = null;
    public int[][] m_imgPix = null;
    
    public int m_imgWidth = 0;
    public int m_imgHeight = 0;
    public int m_imgStackSize = 0;
    
    public long m_totPixProcessed = 0;
    
    
    public static Plot m_plotBtu;
    
    public Bleed_Th()
    {
        
    }
    
    
    public Bleed_Th(boolean bPlotQue)
    {
        m_bCreateChartQue = bPlotQue;
    }

    
    /**
     * The KernelLauncher which will be used to launch the
     * CUDA kernel that was read from a CUBIN file.
     */
    private KernelLauncher kernelLauncher = null;
    
    @Override
    public void run(ImageProcessor imageProcessor) 
    {
        m_totPixProcessed=0;
    /*	int[] pixels = (int[])imageProcessor.getPixels();
        int w = imageProcessor.getWidth();
        int h = imageProcessor.getHeight();
        //vivek
        for (int i = 0; i < 256; i++ )
        {
            intMat[i] = Integer.MAX_VALUE;
            intMatJ[i] = Integer.MAX_VALUE;
        }
        //
        executeBTG(pixels, intMat, w, h);
        currentImage.updateAndDraw();
        
        GetBLTIntensityMat(imageProcessor);
        
        for (int i = 0; i < 256; i++ )
            IJ.log( i + "," + intMat[i] + "," + intMatJ[i] );*/
        
        
       // ImagePlus imp = IJ.getImage();
        
        /*Color Stat testing
        
        IEG_ColorStat cs = new IEG_ColorStat();
        cs.setup(null, imp);
        cs.run(imageProcessor);
        
        if (!m_bGPU)
            return;
        */
        //ImagePlus imp = IJ.getImage();
        boolean bCreateChart = false;
        int retValue = -1;
        if (m_bCreateChartQue)
            retValue = JOptionPane.showConfirmDialog(null, "Create cross correlation chart(s) for Bleed-through analysis?", "Bleed-through", JOptionPane.YES_NO_OPTION );
        
        if (retValue==0)
            bCreateChart=true;
            
        
        ImageStack stack = null;
        if (currentImage!=null)
        {
           stack = currentImage.getStack();
           m_imgHeight = currentImage.getHeight();
           m_imgWidth = currentImage.getWidth();
           m_imgStackSize = stack.getSize();
           
        }
        
        if (m_imgStackSize == 1)
        {
            int[] pixels = null;
            int w = m_imgWidth;
            int h = m_imgHeight;       

            if (!m_bSaveMemory)
            {
                pixels = (int[])imageProcessor.getPixels();
            }
            else
                pixels = m_imgPix[0];
            
            
            //DumpRowDataToCSV(imageProcessor, 0);  
//            plotBT(imageProcessor, -1);
            
            
            if (bCreateChart  && !m_bSaveMemory)
            {
                plotBT(imageProcessor, -1);
                DumpRowDataToCSV(pixels, 0);
            }
            else
            {
                if (m_bGPU)
                    executeBTG(pixels, intMat, m_imgWidth, m_imgHeight);
                else
                {
                    //vivek
                    for (int i = 0; i < 256; i++ )
                    {
                        intMatJ[i] = Integer.MAX_VALUE;
                    }

                    startTime = System.currentTimeMillis();


                    GetBLTIntensityMat(pixels);
                    ApplyBL(pixels);

                    IJ.log(">> Total Time without GPU : "+IJ.d2s((System.currentTimeMillis()-startTime)/1000.0,2)+" sec.");


                }
                
                if (currentImage!=null)
                    currentImage.updateAndDraw();
            }
        /*    GetBLTIntensityMat(imageProcessor);
        
            for (int i = 0; i < 256; i++ )
                IJ.log( i + "," + (int)intMat[i] + "," + intMatJ[i] );*/
            
            
        }
        else
        {
            ImageProcessor ip=null;
            startTime=System.currentTimeMillis();
            int[] pixels = null;
            int w = m_imgWidth;
            int h = m_imgHeight;        
            
            yyll:
            for ( int i = 1; i <= m_imgStackSize; i++)
            {

                if (!m_bSaveMemory)
                {
                    ip = stack.getProcessor(i);
                    pixels = (int[])ip.getPixels();
                }
                else
                {
                    pixels = new int[h*w];
                    System.arraycopy(m_imgPix[i-1], 0, pixels, 0, (h*w));
                }
                
                if (bCreateChart && !m_bSaveMemory)
                {
                    plotBT(ip, i);
                    DumpRowDataToCSV(pixels, i);
                }
                else
                {
                    if (m_bGPU)
                        executeBTG(pixels, intMat, w, h);
                    else
                    {

                        //vivek
                        for (int j = 0; j < 256; j++ )
                        {
                            intMatJ[j] = Integer.MAX_VALUE;
                        }

                        GetBLTIntensityMat(pixels);
                        ApplyBL(pixels);
                        
                        if (currentImage!=null)
                            currentImage.updateAndDraw();
                    }
                }
                

               
            }
            IJ.log(">> Bleed Through Total Time without GPU : "+IJ.d2s((System.currentTimeMillis()-startTime)/1000.0,2)+" sec. -- " + Long.toString(m_totPixProcessed));
           
            if (currentImage!=null)
                currentImage.updateAndDraw();
            
        }
    }
    
    void DumpRowDataToCSV(int[] pixels, int postFix )
    {
        
        int w = m_imgWidth;
        int h = m_imgHeight;        
        
            FileWriter m_fWriter ;
            BufferedWriter m_writer;
        
            try
            {
                m_fWriter = new FileWriter("c:\\IJ\\VJ\\rowData\\rowData_"+ Integer.toString(postFix) + ".csv", true);
                m_writer = new BufferedWriter(m_fWriter);

                for (int i = 0; i < w*h; i++)
                {
                    {
                        pixels[i] = pixels[i] & 0x00ffffff;

                        int r,g,b;

                        r = (pixels[i]>>16);
                        r = r & 0xff;

                        g = (pixels[i]>>8);
                        g = g & 0xff;

                        b = (pixels[i]& 0xff);
                        
                        m_writer.write(r + "," + g + "," + b);
                        m_writer.newLine();
                    }
                }
                
                m_writer.close();
            }
            catch(Exception e)
            {

            }
    }

    /**
     * Will execute the CUDA kernel with the given parameters
     * 
     * @param pixels An array containing the pixels of the
     * image as RGB integers
     * @param w The width of the image
     * @param h The height of the image
     */
    
    void execute(int pixels[], int w, int h)
    {
    	// Allocate memory on the device, and copy the host data to the device 
        int size = w*h*Sizeof.INT;
        Pointer pointer = new Pointer();
        cudaMalloc(pointer, size);
        cudaMemcpy(pointer, Pointer.to(pixels), size, cudaMemcpyHostToDevice);


        // Set up and call the kernel
        int blockSizeX = 32;
        int blockSizeY = 32;
        int gridSize = (int)Math.ceil((double)Math.max(w, h)/(blockSizeX));
        IJ.log("Grid size: " + gridSize + "x" + gridSize);
        IJ.log("Bloc size: x=" + blockSizeX + ", y=" + blockSizeY);
        kernelLauncher.setGridSize(gridSize, gridSize);
        kernelLauncher.setBlockSize(blockSizeX, blockSizeY, 1);
        startTime=System.currentTimeMillis();
        kernelLauncher.call(pointer, w, h);
        
        IJ.log(">> Total Time: "+IJ.d2s((System.currentTimeMillis()-startTime)/1000.0,4)+" sec.");
        
        
        // Copy the data from the device back to the host and clean up
        cudaMemcpy(Pointer.to(pixels), pointer, size, cudaMemcpyDeviceToHost);
        cudaFree(pointer);
    }
    void executeBTG(int pixels[], char[] intMat, int w, int h)
    {
    	// Allocate memory on the device, and copy the host data to the device 
        int size = w*h*Sizeof.INT;
        Pointer pointer = new Pointer();
        startTime=System.currentTimeMillis();
        cudaMalloc(pointer, size);
        cudaMemcpy(pointer, Pointer.to(pixels), size, cudaMemcpyHostToDevice);
        IJ.log(">> MemCpy(MemTDevice) Time: "+IJ.d2s((System.currentTimeMillis()-startTime)/1000.0,6)+" sec.");

        //vivek
        int intMatSize = 256*Sizeof.CHAR;
        Pointer pointer1 = new Pointer();
        cudaMalloc(pointer1, intMatSize);
        JCuda.cudaMemset(pointer1, 65535, intMatSize);
        //cudaMemcpy(pointer1, Pointer.to(intMat), intMatSize, cudaMemcpyHostToDevice);
        //vivek

        // Set up and call the kernel
        int blockSizeX = 32;
        int blockSizeY = 32;
        if ( w <= 5000)
        {
            blockSizeX = 16;
            blockSizeY = 16;
        }

        int gridSize = (int)Math.ceil((double)Math.max(w, h)/(blockSizeX));
        IJ.log("Image Size: " + w + "x" + h);
        IJ.log("Grid size: " + gridSize + "x" + gridSize);
        IJ.log("Bloc size: x=" + blockSizeX + ", y=" + blockSizeY);
        kernelLauncher.setGridSize(gridSize, gridSize);
        kernelLauncher.setBlockSize(blockSizeX, blockSizeY, 1);
        startTime=System.currentTimeMillis();
        kernelLauncher.call(pointer, pointer1, w, h);
        
        IJ.log(">> Total Time: "+IJ.d2s((System.currentTimeMillis()-startTime)/1000.0,6)+" sec.");
        
        
        // Copy the data from the device back to the host and clean up
        startTime=System.currentTimeMillis();
        cudaMemcpy(Pointer.to(pixels), pointer, size, cudaMemcpyDeviceToHost);
        cudaMemcpy(pointer1.to(intMat), pointer1, intMatSize, cudaMemcpyDeviceToHost);
        IJ.log(">> MemCpy(DeviceToMem) Time: "+IJ.d2s((System.currentTimeMillis()-startTime)/1000.0,6)+" sec.");

        
        
        cudaFree(pointer);
        cudaFree(pointer1);
    }

    @Override
    public int setup(String arg, ImagePlus imagePlus)
    {
        if (m_bGPU)
        {
            if (arg != null && arg.equals("about"))
            {
                IJ.showMessage(
                        "About Simple JCuda Plugin...",
                        "An example of an ImageJ plugin using JCuda\n");
                    return DOES_RGB;
            }
            currentImage = imagePlus;

            // Obtain the inputStream for the CUBIN file
            String cubinFileName = "BTG.cubin";
    /*        InputStream cubinInputStream = 
                    getClass().getResourceAsStream(cubinFileName);*/
            InputStream cubinInputStream=null;
            try
            {
                 cubinInputStream = new FileInputStream(cubinFileName);
            }
            catch(IOException e)
            {
                IJ.log(e.toString());
            }
            // Create the kernelLauncher that will execute the kernel
            if (cubinInputStream!=null)
            {
    //            kernelLauncher = KernelLauncher.load(cubinInputStream, "invert");
                kernelLauncher = KernelLauncher.load(cubinInputStream, "BLTMain");
              /*  cudaDeviceDef = new cudaDeviceProp();
                int retVal = 0;

                JCuda.cudaGetDeviceProperties(cudaDeviceDef, retVal);
                if (cudaDeviceDef!=null)
                    IJ.log(cudaDeviceDef.toFormattedString());*/

            }

            return DOES_RGB;
        }
        else
        {
            currentImage = imagePlus;
            return DOES_RGB;
        }
    }
    
    private void GetBLTIntensityMat(int[] pixels)
    {
        
        int w = m_imgWidth;
        int h = m_imgHeight;

        for (int i = 0; i < w*h; i++)
        {
            {
                pixels[i] = pixels[i] & 0x00ffffff;
                
                int r,g,b;
                
                r = (pixels[i]>>16);
                r = r & 0xff;

                g = (pixels[i]>>8);
                g = g & 0xff;

                b = (pixels[i]& 0xff);
                
                if ( intMatJ[b] > g)
                {
                    intMatJ[b] = g;
                }
                
            }
        }
        
        for (int j = 0; j < 256; j++ )
        {
            if (intMatJ[j]>256)
                intMatJ[j]=0;
          
        //    IJ.log( j + "," +  intMatJ[j] );
        }

        
    }
    private void ApplyBL(int[] pixels)
    {
        
        int w = m_imgWidth;
        int h = m_imgHeight;
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
                
                
                if ( intMatJ[b]<256 && intMatJ[b] >= 0 )
                {
                    g = g - intMatJ[b];
                    m_totPixProcessed++;
                }
                
                if (g<0)
                    g=0;
                
                pVal=0;
               
                pVal=(r<<16);
                pVal|=(g<<8);
                pVal|=b;
                
                pixels[i]=pVal;
                
                
            }
        }
    }
    
    void plotBT1(ImageProcessor imageProcessor, int layer)
    {
        
    	int[] pixels = (int[])imageProcessor.getPixels();
        int w = imageProcessor.getWidth();
        int h = imageProcessor.getHeight();

        
        float[] x = null; //= new float[w*h];
        float[] y = null; //= new float[w*h];

        PlotWindow.noGridLines = false; // draw grid lines
        
        String strTitle = null;
        strTitle = "Bleed-through Correction Raw Data Plot";
        if (layer>=0)
            strTitle = "Bleed-through Correction Raw Data Plot (" + layer + ")";

        int oWidth = PlotWindow.plotWidth;
        int oHeight = PlotWindow.plotHeight;
   //     PlotWindow.plotWidth = 2000;
   //     PlotWindow.plotHeight = 2000;
        
        Plot plot = new Plot(strTitle,"Blue Intensity","Green Intensity", x, y);

        x = new float[w*h];
        y = new float[w*h];

        
        for (int i = 0; i < w*h; i++)
        {
            {
                int r,g,b;
                r = (pixels[i]>>16);
                r = r & 0xff;
                g = (pixels[i]>>8);
                g = g & 0xff;
                
                b = pixels[i] & 0xff;
                x[i]= b;
                y[i]=g;

            }
        }
        
        
       // plot.setColor(Color.red);
//        plot.setSize(2000, 2000);
  //      plot.setFrameSize(1800, 1800);
        plot.setColor(Color.blue);
        plot.setLineWidth(2);
        plot.setLimits(0, 255, 0, 255);
        plot.addPoints(x, y, Plot.DOT);
      //  plot.setLineWidth(2);

        // add a second curve
    /*    float x2[] = {.4f,.5f,.6f,.7f,.8f};
        float y2[] = {4,3,3,4,5};
        plot.setColor(Color.red);
        plot.addPoints(x2,y2,PlotWindow.X);
        plot.addPoints(x2,y2,PlotWindow.LINE);*/

        // add label
   /*     plot.setColor(Color.black);
        plot.changeFont(new Font("Helvetica", Font.PLAIN, 24));
        plot.addLabel(0.15, 0.95, "This is a label");*/

  //      plot.changeFont(new Font("Helvetica", Font.PLAIN, 16));
//        plot.setColor(Color.blue);
        plot.getImagePlus().show();
        //plot.show();   
  //      PlotWindow.plotWidth = oWidth;
   //     PlotWindow.plotHeight = oHeight;
        
    }

    boolean plotBT(ImageProcessor imageProcessor, int layer)
    {
        float[] x1 = null;//{0.1f, 0.25f, 0.35f, 0.5f, 0.61f,0.7f,0.85f,0.89f,0.95f}; // x-coordinates
        float[] y1 = null;//{2f,5.6f,7.4f,9f,9.4f,8.7f,6.3f,4.5f,1f}; // x-coordinates
        //float[] e = {.8f,.6f,.5f,.4f,.3f,.5f,.6f,.7f,.8f}; // error bars

        String strTitle = null;
        strTitle = "Bleed-through Correction Raw Data Plot";
        if (layer>=0)
            strTitle = "Bleed-through Correction Raw Data Plot (" + layer + ")";

        PlotWindow.noGridLines = false; // draw grid lines
        Plot plot = new Plot(strTitle,"X Axis","Y Axis",x1,y1);
        plot.setLimits(0, 260, 0, 260);
        plot.setLineWidth(2);
      //  plot.addErrorBars(e);
        
        
    	int[] pixels = (int[])imageProcessor.getPixels();
        int w = imageProcessor.getWidth();
        int h = imageProcessor.getHeight();

        
        float[] x = null; //= new float[w*h];
        float[] y = null; //= new float[w*h];

        PlotWindow.noGridLines = false; // draw grid lines
        

        int oWidth = PlotWindow.plotWidth;
        int oHeight = PlotWindow.plotHeight;
   //     PlotWindow.plotWidth = 2000;
   //     PlotWindow.plotHeight = 2000;
        
    //    Plot plot = new Plot(strTitle,"Blue Intensity","Green Intensity", x, y);

        x = new float[w*h];
        y = new float[w*h];

        
        for (int i = 0; i < w*h; i++)
        {
            {
                int r,g,b;
                r = (pixels[i]>>16);
                r = r & 0xff;
                g = (pixels[i]>>8);
                g = g & 0xff;
                
                b = pixels[i] & 0xff;
                x[i]= b;
                y[i]=g;

            }
        }
        

        // add a second curve
     //   float x2[] = {.4f,.5f,.6f,.7f,.8f};
     //   float y2[] = {4,3,3,4,5};
        plot.setColor(Color.red);
        plot.addPoints(x,y,PlotWindow.CIRCLE);
     //   plot.addPoints(x2,y2,PlotWindow.LINE);

        // add label
   //     plot.setColor(Color.black);
     //   plot.changeFont(new Font("Helvetica", Font.PLAIN, 24));
     //   plot.addLabel(0.15, 0.95, strTitle);

        plot.changeFont(new Font("Helvetica", Font.PLAIN, 16));
        plot.setColor(Color.blue);
        plot.show();  
        
        return false;
    }
}