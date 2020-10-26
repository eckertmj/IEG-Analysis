/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vivek.trivedi
 */
import com.sun.jna.Library;
import com.sun.jna.platform.win32.WinDef.HWND;


/**
 *
 * @author vivek.trivedi
 */
public interface Win32ThreadUtility extends Library
{
    // Select the CPU
    int SetThreadAffinityMask(HWND thHnadle,int mask);
    int GetCurrentThreadId(); // Get thread Id
    HWND GetCurrentThread();
    int Sleep(long Milliseconds); // Assign waiting tim    
}