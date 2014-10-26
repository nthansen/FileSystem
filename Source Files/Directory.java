// Norman Hansen, Tenzin Choephel, Konstantin Stekhov
// Final Assignment
// CSS430B
// Directory class


public class Directory {
    private static int maxChars = 30;    //max length of a file name

    private int fileSize[];             // List containing file sizes
    private char fileNames[][];      // list of file names

    // constructor
    public Directory(int maxInumber) {
        fileSize = new int[maxInumber];     // initialize to max size
        for ( int i = 0; i < maxInumber; i++ ) {
            fileSize[i] = 0;                // All file Size set to 0
        }
        fileNames = new char[maxInumber][maxChars];  // allocate file name
        String root = "/";                              // root of directory
        fileSize[0] = root.length( );                   // first size is "/"
        root.getChars( 0, fileSize[0], fileNames[0], 0 );
    }

    // gets the data from the disk and stores in current instance
    public void bytes2directory(byte data[]) {
        int offset = 0;
        // getting the filesizes from the data
        for (int i = 0; i < fileSize.length; offset += 4) {
            fileSize[i] = SysLib.bytes2int(data, offset);
            i++;
        }
        // getting the filenames
        for (int i = 0; i < fileNames.length; offset += maxChars * 2) {
            String fname = new String(data, offset, maxChars * 2);
            fname.getChars(0, fileSize[i], fileNames[i], 0);
            i++;
        }
    }


    // Obtians the directory information and convert then into byte[]
    // bytes[] will be written to the disk
    public byte[] directory2bytes() {
        // creating the new array
        byte[] newData = new byte[(4 * fileSize.length) + (fileSize.length * maxChars * 2)];
        int offset = 0;
        for (int i = 0; i < fileSize.length; offset += 4) {
            SysLib.int2bytes(fileSize[i], newData, offset);
            i++;
        }

        for (int i = 0; i < fileNames.length; offset += maxChars * 2) {
            // get the file name
            String fname = new String(fileNames[i], 0, fileSize[i]);
            byte[] str_bytes = fname.getBytes(); // converting the filename string to bytes
            // write to the directory array
            System.arraycopy(str_bytes, 0, newData, offset, str_bytes.length);
            i++;
        }
        return newData;
    }


    // Allocates a new inode for the file with given filename
    public short ialloc(String filename){
        short newSize = 0;
        for (int i = 0; i < fileSize.length; i++) {
            if (fileSize[i] == 0) {
                newSize = (short)i;
                fileSize[i] = Math.min(filename.length(), maxChars); // get filesize
                filename.getChars(0, fileSize[i], fileNames[i], 0); // getting the name
                return newSize;
            }
        }
        return -1;
    }

    // deallocates the inode and deletes the file
    public boolean ifree(short iNumber){
        if (fileSize[iNumber] > 0) {
            fileSize[iNumber] = 0; // setting the filesize to 0
            return true;
        }
        return false;
    }


    // return the inode number corresponding to the file name
    public short namei(String filename) {

        for (int i = 0; i < fileSize.length; i++) {
            // setting filename
            String fname = new String(fileNames[i], 0, fileSize[i]);
            // if the right size and name
           if (fileSize[i] == filename.length() && filename.equals(fname)) {
                // return the inode number
                return (short)i;
            }
        }
        return -1;
    }

}