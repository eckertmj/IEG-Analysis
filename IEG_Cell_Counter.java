

import ij.plugin.frame.PlugInFrame;

/**
 *
 * @author Vivek Trivedi ref from Kurt De Vos (http://www.gnu.org/licenses/gpl.txt 
 */

public class IEG_Cell_Counter extends PlugInFrame{
    
    /** Creates a new instance of Cell_Counter */
    public IEG_Cell_Counter() {
         super("IEG Cell Counter");
         new IEGCellCounter();
    }
    
    public void run(String arg){
    }
  
}
