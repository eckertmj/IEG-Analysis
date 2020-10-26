/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;


/**
 *
 * @author vivek.trivedi
 */
public class Win32Thread extends Thread
{
    
    Win32ThreadUtility k32 = (Win32ThreadUtility)Native.loadLibrary("kernel32", Win32ThreadUtility.class);
    int AfMask;
    
    public Win32Thread(){}
    
    public Win32Thread(String thName)
    {
        super(thName);
    }
    
    public int setAffinity(int mask)
    {
        int mask1 = Runtime.getRuntime().availableProcessors();
        
        if ( mask > 64)
            return -1;
        
        this.AfMask = mask;
        
        return k32.SetThreadAffinityMask( k32.GetCurrentThread(), AfMask );
    }
    
    public int getAffinity()
    {
        return AfMask;
    }
    
    public int getCurrentThreadId()
    {
        return k32.GetCurrentThreadId();
    }
    
    public int context(int duration)
    {
        return k32.Sleep(duration);
    }    
}
