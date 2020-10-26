import java.io.File;
import java.io.FilenameFilter;

public class myFileFilter implements FilenameFilter {

    @Override
    public boolean accept(File dname, String fname) {
        if(fname.endsWith(".tif")) {
            return true;
        }
        return false;
    }
}
