// Norman Hansen, Tenzin Choephel, Konstantin Stekhov
// Final Assignment
// CSS430B
// FileTable class

import java.lang.System;
import java.util.Vector;

public class FileTable {

    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable(Directory directory) { // constructor
        table = new Vector();     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // Flags
    // UNUSED = 0 | USED = 1 | READ = 2 | WRITE = 3 | DELETE = 4;

    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc(String filename, String mode) {
        // the inumber for this file
        short iNum = -1;
        // for the file
        Inode inode = null;
        // checks if we need to allocate a new inode
        boolean checker = false;
        // if the filename == then we know we are creating the root
        if (filename.equals("/")) {
            iNum = 0;
        }
        // otherwise we need to get the file from the directory
        else {
            iNum = dir.namei(filename);
        }
        // run this statement until we know what to do
        while (true) {
            // if the number isn't in the directory then we know there's no inumber
            // corresponding to this file
            if (iNum < 0) {
                break;
            }
            // create an inode with the number given
            inode = new Inode(iNum);
            // if we are trying to read then go here
            if (mode.compareTo("r") == 0) {
                // if it's not used set to in use
                if (inode.flag == 0) {
                    inode.flag = 1;
                    // else if read, write, or delete try waiting
                    // to see if it's free
                } else if (inode.flag > 1) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                    // woke up so restart
                    continue;
                }
                //  we don't need to create a new
                // inode
                checker = true;
                break;
            }
            // not a read
            else {
                // if it's unused or write change to read now
                // since write is finished
                if (inode.flag == 0 || inode.flag == 3) {
                    inode.flag = 2;
                    // and we don't need to allocate a new inode
                    checker = true;
                    break;
                }
                // if it's used or already read then now
                // set to delete
                if (inode.flag == 1 || inode.flag == 2) {
                    inode.flag = 4;
                    // write this to disk
                    inode.toDisk(iNum);
                }
                // wait to see if it's unused now
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }

        }
        //  if we aren't reading and need to create a new
        // inode then do it
        if (mode.compareTo("r") != 0 && checker == false) {
            iNum = dir.ialloc(filename);
            inode = new Inode();
            inode.flag = 2;
        }
        // otherwise we are trying to create a new inode and read
        // so return null since we can't
        else if (checker == false) {
            return null;
        }
        // update the inode count and return this
        // new file table entry we allocated
        inode.count++;
        inode.toDisk(iNum);
        FileTableEntry fte = new FileTableEntry(inode, iNum, mode);
        table.addElement(fte);
        return fte;
    }

    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table

    public synchronized boolean ffree(FileTableEntry filetableentry) {
        // if we can remove this file table entry then do it
        if (table.removeElement(filetableentry)) {
            // fix the info to show this is removed
            filetableentry.inode.count--;
            filetableentry.inode.flag = 0;
            // write this to disk
            filetableentry.inode.toDisk(filetableentry.iNumber);
            // get rid of reference to this
            filetableentry = null;
            // wake up any thread waiting on this position
            notify();
            // did it so return true
            return true;
        }
        // otherwise this fte wasn't in our table so let them know
        // its false
        else {
            return false;
        }
    }

    public synchronized boolean fempty() {
        return table.isEmpty();  // return if table is empty
    }                            // should be called before starting a format}

}