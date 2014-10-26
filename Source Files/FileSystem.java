// Norman Hansen, Tenzin Choephel, Konstantin Stekhov
// Final Assignment
// CSS430B
// FileSystem class

public class FileSystem {

    private SuperBlock superblock; // superblock variable
    private Directory directory; // directory variable
    private FileTable filetable; // filetable variable

    // constructor
    public FileSystem(int blocks) {
        // initiating variables
        superblock = new SuperBlock(blocks);
        directory = new Directory(superblock.inodeBlocks);
        filetable = new FileTable(directory);
        FileTableEntry localfte = open("/", "r"); // read directory entry

        int dirSize = fsize(localfte);
        if (dirSize > 0) {
            // directory has the data
            byte[] data = new byte[dirSize];
            read(localfte, data);
            directory.bytes2directory(data);
        }
        close(localfte);
    }

    // sync
    // used in the kernel, sync the data in the directory
    // to the disk

    public void sync() {
        // open the root
        FileTableEntry localfte = open("/", "w");
        // convert the directory into a byte array
        byte[] data = directory.directory2bytes();
        // write this to disk
        write(localfte, data);
        // close once we are done
        close(localfte);
        // sync the superblock as well
        superblock.sync();
    }

    // This function performs formating
    public boolean format(int files) {
        // keep emptying the file table until it's empty
        while (!filetable.fempty()) {
        }
        // format the file amount given
        superblock.format(files);
        // create the new variables
        directory = new Directory(superblock.inodeBlocks);
        filetable = new FileTable(directory);
        return true;
    }

    // This function opens a file
    FileTableEntry open(String file, String mode) {
        //
        FileTableEntry localfte = filetable.falloc(file, mode);
        // if we are trying to write and there's nowhere to write
        // return null
        if (mode == "w" && !deallocAllBlocks(localfte)) {
            return null;
        }
        // otherwise return the position to write
        return localfte;
    }

    // this function closes the file that corresponds to the
    // input file table
    public boolean close(FileTableEntry fte) {
        // if the entry we are given isn't valid return
        // false
        if (fte == null) {
            return false;
        }
        // otherwise work on this process
        synchronized (fte) {
            // decrease the count in this filetable entry cause we are
            // closing this
            fte.count -= 1;
            // if the count is still greater then zero we know it's closed
            // for sure and can return true
            if (fte.count > 0)
                return true;
        }
        // otherwise try freeing this in the table and return the
        // result
        return filetable.ffree(fte);
    }

    // this function returns the size in bytes for the given file
    public int fsize(FileTableEntry ftEnt) {
        if (ftEnt != null) {
            return ftEnt.inode.length;
        } else {
            return -1;
        }
    }

    // this function reads the disk and returns the amount of bytes that
    // have been read or negative value if error.
    public int read(FileTableEntry fte, byte[] buffer) {
        // if no file table entry/write/append then return error
        if (fte == null || (fte.mode == "w") || (fte.mode == "a")) {
            return -1;
        }
        int location = 0;
        // total length is the length of the buffer
        int length = buffer.length;

        // critical state
        synchronized (fte) {
            // while there is still more to read into the buffer
            // keep reading and make sure our pointer is never greater
            // then the size of the file
            while (length > 0 && fte.seekPtr < fsize(fte)) {
                int blockNumber = fte.inode.findSpecificBlock(fte.seekPtr);
                // if not found
                if (blockNumber == -1) {
                    break;
                }
                // to get the data from disk
                byte[] data = new byte[Disk.blockSize];
                // read this spot from disk
                SysLib.rawread(blockNumber, data);

                // get where to read on disk
                int diskReadLocation = fte.seekPtr % Disk.blockSize;

                // find where it is relative to blocksize
                int whereInBlock = Disk.blockSize - diskReadLocation;

                int sizeOfFile = fsize(fte) - fte.seekPtr;

                // get the smallest amount
                int amount = Math.min(Math.min(whereInBlock, length), sizeOfFile);

                // copy the data to the buffer
                System.arraycopy(data, diskReadLocation, buffer, location, amount);

                // move up the seek pointer
                fte.seekPtr += amount;
                // move the location as well
                location += amount;
                // shrink the length since it's getting smaller from us copying
                length -= amount;
            }
            // return the location
            return location;
        }
    }

    // this function writes the data from the buffer to the file
    //
    public int write(FileTableEntry fte, byte[] buffer) {
        int location = 0;

        // not given a valid file table entry or we are suppose to
        // read instead of write
        if (fte == null || fte.mode == "r") {
            return -1;
        }
        // go into critical section
        synchronized (fte) {
            int buffLength = buffer.length;

            // while the buffer has space then write
            while (buffLength > 0) {
                // find the block to write at
                int blockLocation = fte.inode.findSpecificBlock(fte.seekPtr);
                // if the blockLocation is invalid then find a new free location
                if (blockLocation == -1) {

                    short newLocation = (short) superblock.getFreeBlock();
                    // if at this location it's okay to edit we'll get a 0, otherwise
                    // 1 == it's in use, 2 means the indirect is null
                    int status = fte.inode.checkSpecificBlockStatus(fte.seekPtr, newLocation);
                    // nowhere to write so print an error and return -1 to signal it's an error
                    if (status == 1) {
                        SysLib.cerr("Filesystem error on write\n");
                        return -1;
                    }
                    // indirect is null so look for a new spot
                    if (status == 2) {
                        // find a new location
                        newLocation = (short) superblock.getFreeBlock();
                        // get the status ofthis new location
                       status = fte.inode.checkSpecificBlockStatus(fte.seekPtr, newLocation);
                        // if this new location is set then return error
                       if (!fte.inode.checkIfBlockSet((short) status)) {
                            SysLib.cerr("Filesystem error on write\n");
                            return -1;
                        }
                        // if the status of this spot isn't okay to edit then we'll return an
                         // error because we tried twice
                        if (fte.inode.checkSpecificBlockStatus(fte.seekPtr, newLocation) != 0) {
                            SysLib.cerr("Filesystem error on write\n");
                            return -1;
                        }
                    }
                    // the block location is now our new location
                    blockLocation = newLocation;
                }

                byte[] data = new byte[Disk.blockSize];

                // if we read and there's an error at this location then we exit
                if (SysLib.rawread(blockLocation, data) == -1) {
                    System.exit(2);
                }

                // get where to read on this disk
                int diskReadLocation = fte.seekPtr % Disk.blockSize;
                // get where in the block to start
                int whereInBlock = Disk.blockSize - diskReadLocation;
                // the amount we can write
                int amountToWrite = Math.min(whereInBlock, buffLength);

                // copy the amount from the buffer to the data
                System.arraycopy(buffer, location, data, diskReadLocation, amountToWrite);

                // now rewrite to this location
                SysLib.rawwrite(blockLocation, data);

                // update the pointer
                fte.seekPtr += amountToWrite;
                // update the location
                location += amountToWrite;
                // decrease the buffLength since we wrote that amount
                buffLength -= amountToWrite;

                // if the pointer length is longer then the inode length
                // update the inode length to the pointer
                if (fte.seekPtr > fte.inode.length) {
                    fte.inode.length = fte.seekPtr;
                }
            }
            // write this number to disk
            fte.inode.toDisk(fte.iNumber);

            // return the location of where we wrote
            return location;
        }
    }

    // This function empties the inode and delete blocks that it frees in the process
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        // if inode doesn't exist then return false
        if (ftEnt == null || ftEnt.inode.count != 1) {
            return false;
        }
        //
        byte[] freedBlocks = ftEnt.inode.unsetIndirect(); // free indirect nodes
        if (freedBlocks != null) { // if not free
            // dealocate indirect blocks
            int num = SysLib.bytes2short(freedBlocks, 0);
            while (num != -1) {
                superblock.returnBlock(num);
            }
        }
        // dealocating direct blocks
        for (int i = 0; i < 11; i++)
            if (ftEnt.inode.direct[i] != -1) { // if direct block exists
                superblock.returnBlock(ftEnt.inode.direct[i]); // dealocate it
                ftEnt.inode.direct[i] = -1; // and mark as empty
            }
        ftEnt.inode.toDisk(ftEnt.iNumber); // write inode to back to the disk
        return true;
    }

    // This function deletes the file
    public boolean delete(String fileName) {
        FileTableEntry lfte = open(fileName, "w");
        // if there is no file then return false
        if (lfte == null) {
            return false;
        }
        short inum = lfte.iNumber; // get the inumber for this filename
        return directory.ifree(inum) && close(lfte); // dealocating and closing the file
    }

    // This function sets the seek pointer in the entry or returns error if no
    // file table entry was passed
    public int seek(FileTableEntry fte, int offset, int whence) {
        // file table is not valid return the error
        if (fte == null) {
            return -1;
        }

        // go into critical section
        synchronized (fte) {
            // seek pointer is set to offset bytes from the beginning of the file
            if (whence == 0) {
                // checking if valid
                if (offset >= 0 && offset <= fsize(fte)) {
                    fte.seekPtr = offset;
                } else {
                    return -1;
                }
                // seek pointer is set to its current value plus the offset
            } else if (whence == 1) {
                // checking if valid
                if (fte.seekPtr + offset >= 0 && fte.seekPtr + offset <= fsize(fte)) {
                    fte.seekPtr += offset;
                } else {
                    return -1;
                }
                // seek pointer is set to the size of the file plus the offset
            } else if (whence == 2) {
                // checking if valid
                if (fsize(fte) + offset >= 0 && fsize(fte) + offset <= fsize(fte)) {
                    fte.seekPtr = (fsize(fte) + offset);
                } else {
                    return -1;
                }
            }
            return fte.seekPtr;
        }
    }
}