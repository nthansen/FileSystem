Final Project Report (Unix like file system)

Team:
Norman Hansen	
Konstantin Stekhov	
Tenzin Choephel	

Table of contents:

Part 1: System Description:
Part 2: Specifications and Internal Design:
Part 3: How to execute:

Part 1: System Description:

This file system provides a root “ \” directory as it initialize and then all other directories are dynamically created by the user. The system will provide user thread to following system calls that allow them to format, open, read from, write to, update the seek pointer of, close, delete, and get the size of their files. Each user will keep track of file they have opened. Upon a boot, the file system instantiates a Directory object as the root directory, reads the file from the disk that can be found through the inode 0 at 32 bytes of the disk block 1, and initializes the Directory instance with the file contents. On the other hand, prior to a shutdown, the file system will write back the Directory information onto the disk. 

Part 2: Specifications and Internal Design:

Assumptions and Limitations
- The file system has to perform 8 basic system calls ()
- The manipulations on files can be done by multiple user threads
- The file system must provide stream-oriented files for user thread programs
- The file system has to maintain the list of all open files
- Users cannot edit the same file simultaneously
- There is no user interface that would make working with the file system much easier
- This file system can only work on ThreadOS
- The user won’t modify the current functions inside ThreadOS
- Inability to create user permissions so that only certain users can access certain files

1. FileSystem.java
File systems bring together all other classes, It instantiates an instances of the SuperBlock, Directory and the File Table. It maintains the File System by providing interface to the system calls. following are it’s behaviour: 

FileSystem Functions:
1.	int SysLib.format( int files );
formats the disk, (Disk.java's data contents).The parameter (files) will specifies the number of files to be created, So the system will allocate that number of inodes, return 0 on success and -1 otherwise.

2.	int fd = SysLib.open( String fileName, String mode );
Opens the file specified using the given fileName and mode (where "r" = ready only, "w" = write only, "w+" = read/write, "a" = append), and it will allocate a new file descriptor(fd). A new file is created if it does not exist in the mode "w", "w+" or "a". SysLib.open will return -1 as an error value if the file does not exist in the mode "r". File descriptors 0, 1, and 2 are reserved as the standard input, output, and error, and therefore a newly opened file will receive a new descriptor numbered in the range between 3 and 31. If the calling thread's user file descriptor table is full, SysLib.open should return the error value. The seek pointer is initialized to zero in the mode "r", "w", and "w+", whereas initialized at the end of the file in the mode "a".

3.	int read( int fd, byte buffer[] );
Starting at seek pointer’s position, it will read bytes up to buffer.length from the file indicated by fd. If bytes remaining between the current seek pointer and the end of file are less than buffer.length, SysLib.read reads as many bytes as possible, putting them into the beginning of buffer. It increments the seek pointer by the number of bytes to have been read. Returns number of bytes if success, otherwise -1 one for error.

4.	int write( int fd, byte buffer[] );
Starting at seek pointer’s position, it will write the contents of buffer to the file indicated by fd. The operation may overwrite existing data in the file and/or append to the end of the file. SysLib.write increments the seek pointer by the number of bytes to have been written. Returns number of bytes if success, otherwise -1 one for error.

5.	int seek( int fd, int offset, int whence );
User will not be able to set the seek pointer at any negative value or beyond the file size. these positions will automatically be adjusted to the beginning or end of the file respectively but will still be considered success. Other updates of the seek pointer corresponding to fd are as following:
a.	If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes from the beginning of the file
b.	If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current value plus the offset. The offset can be positive or negative.
c.	If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the file plus the offset. The offset can be positive or negative.

6.	int close( int fd );
closes the file corresponding to fd, commits all file transactions on this file, and unregisters fd from the user file descriptor table of the calling thread's TCB. The return value is 0 in success, otherwise -1.

7.	int delete( String fileName );
destroys the file specified by fileName. If the file is currently open, it is not destroyed until the last open on it is closed, but new attempts to open it will fail.

8.	int fsize( int fd );
returns the size in bytes of the file indicated by fd.

2. SuperBlock.java
This class is a strictly OS managed and no user thread is allowed to access or interact with this class. SuperBlock contains information regarding the number of disk blocks, number of inodes and block number for the head block of the free list. On disk, SuperBlock holds the first position at disk[0]. It also performs the formatting of the disk.

3. Directory.java
Directory store the file sizes of all the files and file names (Directory information) of all the files.The methods bytes2directory( ) will initialize the Directory instance with a byte array read from the disk and directory2bytes() converts the Directory instance into a byte array that will be written back to the disk.

4. FileTable.java
The file system maintains the file (structure) table shared among all user threads. When a user thread opens a file, The user thread will allocate a new entry of the user file descriptor table in its TCB. This entry number is the file descriptor number. The entry maintains a reference to an file (structure) table entry which will be obtained from the file system in the following sequence.
1.	The user thread will request the file system to allocate a new entry of the system-maintained file (structure) table. This entry includes the seek pointer of this file, a reference to the inode of the file, the inode number, the count to maintain number of threads sharing this file (structure) table, and the access mode. The seek pointer is set to the front or the tail of this file depending on the file access mode.
2.	The file system locates the corresponding inode and records it in this file (structure) table entry.
3.	The user thread finally registers a reference to this file (structure) table entry in its file descriptor table entry of the TCB.

5. Inode.java
This inode is a simplified version of the Unix inode, It includes 12 pointers of the index block. The first 11 of these pointers point to direct blocks. The last pointer points to an indirect block. Each inode also include the length of the corresponding file,  the number of file table entries that point to this inode, and flag of weather it is  unused = 0, used = 1, or in some other status = 2, 3, 4.
In order to keep the Inodes consistent among users, we check for the inode on the disk’s status before updating the local inode, if some other user updated the inode on disk, we immedialy write back the content. This content includes int length, short count, short flag, short direct[11], and short indirect, which is total of 32 bytes.

6. TCB.java
This class maintains a file table for all the files opened by the individual users. This table is called a user file descriptor table. It has 32 entries. Each file descriptor will include the file access mode and the reference to the corresponding file table entry which contain the information regarding the file status and read position (seek ptr). The file access mode indicates "read only", "write only", "read/write", or "append".  The system maintains the structure of the file and it is shared among all users. The Seek ptr is positioned according to the file access mode and it is either position at the beginning to read the file or at the end to write to the file. Even when same user reads the file multiple times and make several entries in the TCB’s user file descriptor table and have various file table entry references, only the seek ptr location will vary and all will refer to same inode. 

Part 3: How to execute

1.	Open the Compiled Project folder in command prompt 
2.	Enter java Boot
3.	Enter l Test5
4.	Type q to quit