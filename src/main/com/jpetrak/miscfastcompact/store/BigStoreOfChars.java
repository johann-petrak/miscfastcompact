package com.jpetrak.miscfastcompact.store;

import com.jpetrak.miscfastcompact.utils.Utils;
import it.unimi.dsi.fastutil.chars.CharBigArrayBigList;
import java.io.Serializable;

/**
 *
 * @author Johann Petrak
 */
public class BigStoreOfChars implements Serializable {
  
  /**
   * 
   */
  
  private static final long serialVersionUID = 1001L;
  
  // the backing array that holds all the actual data
  CharBigArrayBigList backingArray;
  // TODO: why is this an int when the backing array is BIG
  // TODO: do we needs this if we could use size instead?
  long curIndex = 0;
  
  public void StoreOfChars(long initialCapacity) {
    backingArray = new CharBigArrayBigList(initialCapacity);
  }
  public void StoreOfChars() {
    backingArray = new CharBigArrayBigList();
  }
  
  
  private char[] zeroChars = Utils.int2TwoChars(0); 
  private char[] oneChars = Utils.int2TwoChars(1);
  
  /**
   * Add some data and get back the index under which we can get it back
   * @param data
   * @return
   */
  public long addData(char[] data) {
    // remember where we store the data
    long oldIndex = curIndex;
    System.err.println("curIndex="+curIndex+", size="+backingArray.size64());
    // first store the length of the data
    // we split the int that represents the length into to chars
    int l = data.length;
    char[] lAsChars = Utils.int2TwoChars(l);
    addChars(lAsChars);
    addChars(data);
    // after storing, the new index is now moved by the length of the data
    // plus the two chars where we store the length
    curIndex += data.length+2;
    System.err.println("curIndex="+curIndex+", size="+backingArray.size64());
    return oldIndex;
  }

  /**
   * Add some data and get back the index under which we can get it back. This will 
   * add a chunk of data of known length to the store: no length is stored in the 
   * store for this chunk. This chunk can only be retrieved with the getFixedLengthData
   * method.
   * 
   * @param data
   * @return
   */
  public long addFixedLengthData(char[] data) {
    // remember where we store the data
    long oldIndex = curIndex;
    System.err.println("curIndex="+curIndex+", size="+backingArray.size64());
    addChars(data);
    // after storing, the new index is now moved by the length of the data
    curIndex += data.length;
    System.err.println("curIndex="+curIndex+", size="+backingArray.size64());
    return oldIndex;
  }
  
  public long replaceFixedLengthData(long index, char[] data) {
    for(int i = 0; i<data.length; i++) {
      backingArray.set(index+i,data[i]);
    }
    return index;
  }
 
  
  /** 
   * Get the data from at the given index.
   * @param index
   * @return
   */
  public char[] getData(long index) {
    // retrieve the length 
    int l = Utils.twoChars2Int(backingArray.get(index), backingArray.get(index+1));
    // now retrieve the characters for this data block
    char data[] = new char[l];
    for(int i=0; i<l; i++) {
      data[i] = backingArray.get(index+2+i);
    }
    return data;
  }

  /** 
   * Get data of known length from at the given index.
   * @param index
   * @return
   */
  public char[] getFixedLengthData(long index, int length) {
    char data[] = new char[length];
    for(int i=0; i<length; i++) {
      data[i] = backingArray.get(index+i);
    }
    return data;
  }
  
  /**
   * Add a new list to the store and return its index. After this, a list with 
   * length of 1 is stored.
   * 
   * @param data
   * @return
   */
  public long addListData(char[] data) {
    // create the special first list entry: 
    // = length of list (int=2 chars), set to 1
    // = index of next list entry (long=4 chars), set to 0
    // = actual data
    // remember where we store the data
    long oldIndex = curIndex;
    System.err.println("curIndex="+curIndex+", size="+backingArray.size64());
    // first store the length of the data: for the first list entry
    // this also includes size and next element index, so add 4
    // we split the int that represents the length into to chars
    int l = data.length+4;
    char[] lAsChars = Utils.int2TwoChars(l);
    addChars(lAsChars);
    addChars(oneChars);
    addChars(zeroChars);
    addChars(zeroChars);
    addChars(data);
    // after storing, the new index is now moved by the length of the data
    // plus the two chars where we store the length
    curIndex += data.length+8;
    System.err.println("curIndex="+curIndex+", size="+backingArray.size64());
    return oldIndex;    
  }
  
  /**
   * Append additional data blocks to a list that already exists in the store at 
   * the given index. if index is <=0, add a new list.
   * 
   * @param index
   * @param data
   * @return
   */
  // TODO: instead of doing getData to get the element indices, just directly access
  // the characters
  public long addListData(int index, char[] data) {
    if(index <= 0) {
      return addListData(data);
    }

    int size = getListSize(index); 
    if(size < 1) {
      throw new RuntimeException("Adding to a list, but size is <1: "+size);
    }

    
    // update the size
    char sz[] = Utils.int2TwoChars(size+1);
    backingArray.set(index+2,sz[0]); // just skip the data length characters: 2 characters
    backingArray.set(index+3,sz[1]);
    
    
    // store the new data
    long newBlockIndex = addNewListBlock(data);
    
    // now add the index of that block to either the first block or 
    // dereference until we are the correct block
    
    long curBlockIndex = index; 
    // if we need to append not at the first block (size!=1, get the next block which
    // corresponds to size=2    
    // then if size > 2, iterate as often as still needed
    if(size > 1) {
      curBlockIndex = getNextElementIndex4First(index);
      for(int i=2; i<size; i++) {
        curBlockIndex = getNextElementIndex4Other(curBlockIndex);
      }
    }
    
    // encode the new block index 
    char idx[] = Utils.long2FourChars(newBlockIndex);
    
    
    if(size == 1) {
      backingArray.set(index+4,idx[0]);
      backingArray.set(index+5,idx[1]);   
      backingArray.set(index+6,idx[2]);
      backingArray.set(index+7,idx[3]);
    } else {
      backingArray.set(curBlockIndex+2,idx[0]);
      backingArray.set(curBlockIndex+3,idx[1]);                  
      backingArray.set(curBlockIndex+4,idx[2]);
      backingArray.set(curBlockIndex+5,idx[3]);
    }
    
    
    return index;
  }
  
  /**
   * Return the list element at the given index.
   * @param index
   * @param element
   * @return
   */
  public char[] getListData(long index, int element) {
    // get the first block which must exist
    int size = getListSize(index);
    if(size <= 0) {
      throw new RuntimeException("getting list data but size is <=0: "+size);
    }
    assert(element<size);
    if(element >= size) {
      throw new RuntimeException("getting list data but element>=size: "+element+"/"+size);
    }
    
    if(size == 1 && element != 0) {
      throw new RuntimeException("getting list data but size=1 and element!=0: "+element);
    }
    if(element == 0) {
      return getDataWithout(index, 4);
    }
    // if we need an element >0, 
    // de-reference the current block "element" times and get the data from there
    long nextBlockIndex = getNextElementIndex4First(index);
    // we did already the first dereferencing from the first block, so if necessary
    // dereference element-1 more times
    for(int i=0;i<(element-1);i++) {
      nextBlockIndex = getNextElementIndex4Other(nextBlockIndex);
    }
    // we have the index of the block we want, return it
    return getDataWithout(nextBlockIndex,2);
  }
  
  /** 
   * Find the chunk among all the list elements stored at index and 
   * return the index of the element (>= 0) if found or -1 if not found.
   * 
   * @param index index of the list in the store
   * @param chunk the chunk to find
   * @return the index of the chunk in the list or -1 if not found
   */
  public int findListData(long index, char[] chunk) {
    int elementIndex = 0;
    
    // if the list exists at all, there always must be at least one element, so
    // always check the first element.
    // Find the start and the length of the first element and compare
    int length = Utils.twoChars2Int(backingArray.get(index), backingArray.get(index+1));
    long chunkIndex = index+6;  // 2 for the chunk length, 2 for list size,, 4 for next element index
    if(isChunkEqual(chunkIndex,length-4,chunk)) {
      return elementIndex;
    }
    // get the next chunk pointer
    int nextBlockIndex = getNextElementIndex4First(index);
    while(nextBlockIndex != 0) {
      elementIndex++;
      // now check the block at this index!
      length = Utils.twoChars2Int(backingArray.get(nextBlockIndex), backingArray.get(nextBlockIndex+1));
      chunkIndex = nextBlockIndex+4; // 2 for chunk length, 2 for next element index
      if(isChunkEqual(chunkIndex,length-2,chunk)) {
        return elementIndex;
      }
      nextBlockIndex = getNextElementIndex4Other(nextBlockIndex);
    }
    return -1;
  }

  
  /**
   * Check if the characters in the store, starting at index and having length length,
   * are identical to the characters of the chunk. 
   * @param index
   * @param length
   * @param chunk
   * @return
   */
  protected boolean isChunkEqual(long index, int length, char[] chunk) {
    if(chunk.length != length) {
      return false;
    }
    for(int i = 0; i<length; i++) {
      if(backingArray.get(index+i) != chunk[i]) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Return the size of the list at the given index
   * @param index
   * @return
   */
  public int getListSize(long index) {
   return Utils.twoChars2Int(backingArray.get(index+2), backingArray.get(index+3));
  }
  
  //*******************************************************************
  
  
  private int getNextElementIndex4First(long index) {
    return Utils.twoChars2Int(backingArray.get(index+4), backingArray.get(index+5));
  }
  private int getNextElementIndex4Other(long index) {
    return Utils.twoChars2Int(backingArray.get(index+2), backingArray.get(index+3));
  }
  
  // special method to retrieve just the data from a list element: same as 
  // the ordinary get data, except the we have to skip to chars at the 
  // beginning which is the next element pointer, or the size and next element pointer
  // without is 2 or 4 for these.
  private char[] getDataWithout(long index, int without) {
    // retrieve the length 
    int l = Utils.twoChars2Int(backingArray.get(index), backingArray.get(index+1));
    // now retrieve the characters for this data block
    char data[] = new char[l-without];
    for(int i=0; i<(l-without); i++) {
      data[i] = backingArray.get(index+2+without+i);
    }
    return data;
  }
  
  // similar to addData but also adds the empty next block entry at the beginning 
  private long addNewListBlock(char[] data) {
    // remember where we store the data
    long oldIndex = curIndex;
    // first store the length of the data
    // we split the int that represents the length into to chars
    int l = data.length+2;
    char[] lAsChars = Utils.int2TwoChars(l);
    addChars(lAsChars);
    addChars(zeroChars);
    addChars(data);
    // after storing, the new index is now moved by the length of the data
    // plus the two chars where we store the length
    curIndex += data.length+4;
    return oldIndex;    
  }
  
  
  private void addChars(char[] cs) {
    for(char c : cs) {
      backingArray.add(c);
    }
  }
}