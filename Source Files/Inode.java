// Norman Hansen, Tenzin Choephel, Konstantin Stekhov
// Final Assignment
// CSS430B
// Inode class

public class Inode {
    public static final int iNodeSize = 32; // inode size is 32 bytes
    public static final int directSize = 11; // direct pointers
    public int length; // size of a file
    public short count; // number of file table entries
    public short flag; // check if used, unused, read, etc
    public short[] direct = new short[11]; // setting up direct pointers
    public short indirect; // idirect pointer

    // Flags
    // UNUSED = 0 | USED = 1 | READ = 2 | WRITE = 3 | DELETE = 4;

    // default constructor
    public Inode() {
        length = 0;
        count = 0;
        flag = 1;
        for (int i = 0; i < directSize; i++)
            direct[i] = -1;
        indirect = -1;
    }

    // constructor
    // if given an iNumber, retrieve inode from disk
    public Inode(short iNumber) {
        // position in the disk
        int block = 1 + iNumber / 16;
        byte[] data = new byte[Disk.blockSize];
        // read the data from the disk
        SysLib.rawread(block, data);
        int offset = iNumber % 16 * iNodeSize;
        // setting values from the disk based on
        // the offset
        length = SysLib.bytes2int(data, offset);
        offset += 4;
        count = SysLib.bytes2short(data, offset);
        offset += 2;
        flag = SysLib.bytes2short(data, offset);
        offset += 2;

        // fix the pointers and keep moving the offset
        for (int i = 0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(data, offset);
            offset += 2;
        }
        // get the indirect because we are at the end
        indirect = SysLib.bytes2short(data, offset);
    }

    // toDisk
    // save to disk as the i-th inode
    public void toDisk(short iNumber) {

        // array for the data to get values
        byte[] someArr = new byte[iNodeSize];
        int offset = 0;

        // getting length
        SysLib.int2bytes(length, someArr, offset);
        offset += 4;
        // getting count
        SysLib.short2bytes(count, someArr, offset);
        offset += 2;
        // getting the flag
        SysLib.short2bytes(flag, someArr, offset);
        offset += 2;

        // retreving the pointers
        for (int i = 0; i < directSize; i++) {
            SysLib.short2bytes(direct[i], someArr, offset);
            offset += 2;
        }
        // last get the indirect
        SysLib.short2bytes(indirect, someArr, offset);
        offset += 2;

        // now lets get the data
        int block = 1 + iNumber / 16;
        byte[] data = new byte[512];
        SysLib.rawread(block, data);
        offset = iNumber % 16 * iNodeSize;

        // make a copy of the data now that we have it
        System.arraycopy(someArr, 0, data, offset, iNodeSize);
        // then write it back to disk
        SysLib.rawwrite(block, data);
    }

    // checkIfBlockSet
    // Checks if the inumber block is set
    public boolean checkIfBlockSet(short iNumber) {
        // go through the size and if any of it
        // is negative then we know it's not set
        for (int i = 0; i < directSize; i++) {
            if (direct[i] == -1) {
                return false;
            }
        }
        // if indirect is -1 then it's not set
        if (indirect != -1)
            return false;
        indirect = iNumber;
        byte[] data = new byte[512];

        // convert to bytes
        for (int j = 0; j < Disk.blockSize / 2; j++) {
            SysLib.short2bytes((short)-1, data, j * 2);
        }

        // then write to disk
        SysLib.rawwrite(iNumber, data);
        return true;
    }

    // findSpecificBlock
    // find a specific block based on the value given in
    // and return the pointer to it

    public int findSpecificBlock(int specificBlock) {
        int value = specificBlock / 512;
        // if the value is less then size
        // we'll return the pointer to our spot
        if (value < directSize) {
            return direct[value];
        }
        // if the indirect isn't valid then
        // return error
        if (indirect < 0) {
            return -1;
        }
        byte[] data = new byte[512];
        SysLib.rawread(indirect, data);
        // get the offset
        int offset = (value - directSize) * 2;
        return SysLib.bytes2short(data, offset);
    }

    // checkSpecificBlockStatus
    // 0 means it's fine to edit, 1 means it's in use
    // 2 means the indirect is null

    public int checkSpecificBlockStatus(int pointer, short freeBlock){
        // change pointer to location in the direct
        int location = pointer / 512;
        // if the location isn't valid then we just return 1
       // so we don't try to look at invalid location
        if (location < 0) {
            return 1;
        }
        // if it's below 11 then we know it's a direct spot
        if (location < 11) {
            // if the location already has a file pointer then
            // we know it's in use
            if (direct[location] >= 0)
                return 1;
            // if the location is greater then 0 and the position
            // before it is valid then we can write there
            if ((location > 0) && (direct[(location - 1)] == -1))
                return 0;
            // the pointer location now equals this spot
            direct[location] = freeBlock;
            // it's valid so let the filesystem know
            return 0;
        }
        // otherwise it's an indirect and we need to check
        else {
            // if the indirect isn't in use then it's null
            // and let the filesystem know
            if (indirect < 0) {
                return 2;
            }
            // byte array for the data
            byte[] data = new byte[512];
            // read the indirects data from disk
            SysLib.rawread(indirect, data);
            // then get the offset
            int offset = (location - directSize) * 2;
            // if there's data let the filesystem know it's in use
            if (SysLib.bytes2short(data, offset) > 0) {
                return 1;
            }
            // otherwise create the short to bytes
           SysLib.short2bytes(freeBlock, data, offset);
            // write this to disk with the indirect value
            // and return it's valid
            SysLib.rawwrite(indirect, data);
            return 0;
        }
    }

    // unsetIndirect
    // frees the indirect and returns the data that was freed

    byte[] unsetIndirect(){
        // if it's greater or equal to zero then get the data
        if (indirect >= 0) {
            byte[] data = new byte[Disk.blockSize];
            // read the data from disk
            SysLib.rawread(indirect, data);
            // set it to freed
            indirect = -1;
            // return the data
            return data;
        }
        // otherwise there was no to free so return null
        return null;
    }
}