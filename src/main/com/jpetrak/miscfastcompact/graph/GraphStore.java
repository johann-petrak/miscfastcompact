package com.jpetrak.miscfastcompact.graph;

import com.jpetrak.miscfastcompact.store.StoreOfChars;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;

/**
 * Simple Graph store. Just a first attempt to create a graph store, WIP...
 * @author Johann Petrak
 */
public class GraphStore {
  // The store consists of the following parts:
  // = a map from URI-String to Uri-id (an int, sequential 0...(n-1)
  // = an array that maps URI id to the out edge chunk index or -1 if no edge exists (yet)
  // = an array that maps URI id to the in edge chunk index or -1 if no edge exists (yet)
  // = out-edge store and in-edge store: two separate stores where we store,
  //   at position chunk-index, a variable block of edge-data. Edge data
  //   is a table with two integers per row: first the id or count of the edg
  //   second the id of the to/from node
  
  private StoreOfChars outEdges;
  private StoreOfChars inEdges;
  private Object2IntAVLTreeMap<String> uri2idMap;
  private CharArrayList id2OutEdgeChunk;
  private CharArrayList id2InEdgeChunk;
  private int nextId = 0;
  
  public GraphStore() {
    uri2idMap = new Object2IntAVLTreeMap<String>();
  }
  
  // TO LOAD A GRAPH: for new this needs to be done in the following way:
  // = for each (unique) URI, call addNode(uri)
  // = add all the incoming and outgoing edges by calling
  //   addInEdges(nodeid, listOfEdgeData, listOfNodeIds)
  //   addOutEdges(nodeid, listOfEdgeData, listOfNodeIds)

  // To make this easier, we allow a shortened approach that uses, temporarily
  // a lot of memory:
  // startImport(10000000);
  // addEdge(fromNode,toNode,edgeData):
  // finishImport();
  
  private StoreOfChars tmpInEdges;
  private StoreOfChars tmpOutEdges; 
  private IntArrayList tmpId2InChunk;
  private IntArrayList tmpId2OutChunk;
  
  public void startImport(int capacity) {
    tmpInEdges = new StoreOfChars(capacity);
    tmpOutEdges = new StoreOfChars(capacity);
    tmpId2InChunk = new IntArrayList();
    tmpId2OutChunk = new IntArrayList();
  }
  public void addEdge(String fromNode, String toNode, int edgeData) {
    
    int fromId = getNodeId(fromNode);
    if(fromId == -1) {
      // have to add the node
      fromId = addNode(fromNode);
      // since this node is new, we do not have the chunk for the outgoing
      // and incoming edges yet. For now, we simply store -1 
      tmpId2InChunk.add(-1);
      tmpId2OutChunk.add(-1);
      if(tmpId2OutChunk.size()-1 != fromId) {
        throw new RuntimeException("Error: id2OutChunk not equal to id");
      }
      if(tmpId2InChunk.size()-1 != fromId) {
        throw new RuntimeException("Error: id2InChunk not equal to id");
      }
    } 
    int toId = getNodeId(toNode);
    if(toId == -1) {
      // have to add the to node
      toId = addNode(toNode);
      tmpId2InChunk.add(-1);
      tmpId2OutChunk.add(-1);
      if(tmpId2OutChunk.size()-1 != toId) {
        throw new RuntimeException("Error: id2OutChunk not equal to id");
      }
      if(tmpId2InChunk.size()-1 != toId) {
        throw new RuntimeException("Error: id2InChunk not equal to id");
      }
    } 
    // now we have to two node ids, and our chunk index tables either contain
    // already an id or -1. For the edge we have, either create the necessary
    // chunks or add the list element to an existing chunk
    // 1) the outgoing edge from the fromNode to the toNode
    char[] edge = new char[2];
    edge[0] = (char) edgeData;
    edge[1] = (char) toId;
    int outChunk = tmpId2OutChunk.getInt(fromId);
    if(outChunk == -1) {
      // create a new chunk and store the index
      tmpId2OutChunk.set(fromId, tmpOutEdges.addListData(edge));
    } else {
      tmpOutEdges.addListData(outChunk, edge);
    }
    edge[1] = (char) fromId;
    int inChunk = tmpId2InChunk.getInt(toId);
    if(inChunk == -1) {
      tmpId2InChunk.set(toId, tmpInEdges.addListData(edge));
    } else {
      tmpInEdges.addListData(inChunk,edge);
    }
  }
  
  // for debugging mainly
  public void debugPrintEdges(String uri) {
    int id = getNodeId(uri);
    if(id == -1) {
      System.out.println("Node not known: "+uri);
    } else {
      System.out.println("Finding edges for node "+id);
    }
    // get the chunk for the in edges for uri
    int inChunk = tmpId2InChunk.get(id);
    if(inChunk == -1) {
      System.out.println("No in edges for "+uri);
    } else {
      System.out.println("Chunk index for in edges "+inChunk);
      int size = tmpInEdges.getListSize(inChunk);
      System.out.println("Got in edges "+size);
      for(int i=0; i<size; i++) {
        char[] edge = tmpInEdges.getListData(inChunk, i);
        System.out.println("In Edge "+i+": nodeid="+((int)edge[1])+", data="+((int)edge[0]));
      }
    }
    int outChunk = tmpId2OutChunk.get(id);
    if(outChunk == -1) {
      System.out.println("No out edges for "+uri);
    } else {
      System.out.println("Chunk index for out edges "+outChunk);
      int size = tmpOutEdges.getListSize(outChunk);
      System.out.println("Got out edges "+size);
      for(int i=0; i<size; i++) {
        char[] edge = tmpOutEdges.getListData(outChunk, i);
        System.out.println("Out Edge "+i+": nodeid="+((int)edge[1])+", data="+((int)edge[0]));
      }
     
    }
  }
  
  public void finishImport() {
    // convert from the temporary data structures to the final ones!
    
  }
  
  
  /**
   * Add a node to the node list and return the id (unless it already exists,
   * then just return the id). 
   * @param uri
   * @return 
   */
  public int addNode(String uri) {
    if(uri2idMap.containsKey(uri)) {
      return uri2idMap.getInt(uri);
    } else {
      int usedId = nextId;
      uri2idMap.put(uri, nextId);
      nextId++;
      return usedId;
    }
  }
  
  // return the id or -1 if not found
  public int getNodeId(String uri) {
    if(uri2idMap.containsKey(uri)) {
      return uri2idMap.getInt(uri);
    } else {
      return -1;
    }
  }
  
  
  // ******** HELPER METHODS for handling edge data
  private static void setEdge(int[] edges, int edgeNumber, int edgedata, int nodeId) {
    edges[edgeNumber << 1] = edgedata;
    edges[(edgeNumber << 1)+1] = nodeId;
  }
  
  // ******** HELPER METHODS for usign the char[] store
  
  // for storing the Edge data in a character chunk
  private static char[] edges2Chunk(int[] edges) {
    char[] ret = new char[edges.length];
    for(int i=0; i<edges.length; i++) {
      ret[i] = (char)edges[i];
    }
    return ret;
  }
  
  public static void main(String[] args) {
    System.out.println("Running main ...");
    GraphStore gstore = new GraphStore();
    gstore.startImport(1);
    
    gstore.addEdge("uri1", "uri2", 22);
    gstore.addEdge("uri1", "uri3", 21);
    gstore.addEdge("uri1", "uri4", 20);
    gstore.addEdge("uri4", "uri5", 20);
    //gstore.debugPrintEdges("uri1");
    //gstore.debugPrintEdges("uri2");
    //gstore.debugPrintEdges("uri4");
    System.out.println("Finishing main ....");
  }
  
  
  
}
