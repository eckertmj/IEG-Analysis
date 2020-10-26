

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

/**
 * A simple example for an ImageJ Plugin that uses JCuda.
 */
public class GPU_Info implements PlugInFilter
{
	/**
	 * The current image to operate on
	 */
    private ImagePlus currentImage = null;
    long startTime = 0; 
    cudaDeviceProp cudaDeviceDef;
    char[] intMat = new char[256];
    int[] intMatJ = new int[256];

    
    /**
     * The KernelLauncher which will be used to launch the
     * CUDA kernel that was read from a CUBIN file.
     */
    private KernelLauncher kernelLauncher = null;
    
    @Override
    public void run(ImageProcessor imageProcessor) 
    {
           return; 

    }


    @Override
    public int setup(String arg, ImagePlus imagePlus)
    {
    	if (arg != null && arg.equals("about"))
    	{
            IJ.showMessage(
                    "About Simple JCuda Plugin...",
                    "An example of an ImageJ plugin using JCuda\n");
    		return DOES_RGB;
    	}
        // Create the kernelLauncher that will execute the kernel
        {
            cudaDeviceDef = new cudaDeviceProp();
            int retVal = 0;
            
            JCuda.cudaGetDeviceProperties(cudaDeviceDef, retVal);
            if (cudaDeviceDef!=null)
                IJ.log(cudaDeviceDef.toFormattedString());

        }
    	
    	return DOES_RGB;
    }
    

}