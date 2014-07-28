package com.jpetrak.miscfastcompact.graph;

import com.jpetrak.miscfastcompact.store.StoreOfInts;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
  
  private StoreOfInts outEdges;
  private StoreOfInts inEdges;
  private Object2IntAVLTreeMap<String> uri2idMap;
  //private HashMap<String,Integer> uri2idMap;
  private IntArrayList id2OutEdgeChunk;
  private IntArrayList id2InEdgeChunk;
  private int nextId = 0;
  
  public GraphStore() {
    uri2idMap = new Object2IntAVLTreeMap<String>();
    //uri2idMap = new HashMap<String,Integer>();
    outEdges = new StoreOfInts();
    inEdges = new StoreOfInts();
    id2OutEdgeChunk = new IntArrayList();
    id2InEdgeChunk = new IntArrayList();
  }

  /**
   * Add a node to the node list and return the id (unless it already exists,
   * then just return the id). 
   * @param uri
   * @return 
   */
  public int addNode(String uri) {
    if(uri2idMap.containsKey(uri)) {
      return uri2idMap.get(uri);
    } else {
      int usedId = nextId;
      uri2idMap.put(uri, nextId);
      id2InEdgeChunk.add(-1);
      id2OutEdgeChunk.add(-1);
      nextId++;
      return usedId;
    }
  }
  
  // return the id or -1 if not found
  public int getNodeId(String uri) {
    if(uri2idMap.containsKey(uri)) {
      return uri2idMap.get(uri);
    } else {
      return -1;
    }
  }

  // TO LOAD A GRAPH:
  // First, add all the known nodes: addNode(uri) for all different uris
  // Then for each node that has outgoing edges, add all the outgoing edges
  // at once: addOutEdges(nodeId, listOfEdgeData, listOfNodeIds)
  // Then for each node that has incoming edges, add all the incoming edges
  // at once: addInEdges(nodeId, listOfEdgeData, listOfNodeIds)
  // This could be achieved by reading in three files: one with just the 
  // URIs, one sorted by first uri, one sorted by second uri, and the 
  // loading program gathers all the information for blocks of identical 
  // first or second uris
  
  /**
   * Add all the incoming edges for a node. This assumes that all the nodes
   * have already been added! For each node, this must only be called once!
   * @param nodeId
   * @param edgeData
   * @param nodeIds 
   */
  public void addInEdges(int nodeId, List<Integer> edgeData, List<Integer> nodeIds) {
    if(edgeData.size() != nodeIds.size()) {
      throw new RuntimeException("Not the same number of elements for edgeData and nodeIds: "+edgeData.size()+"/"+nodeIds.size());
    }
    int[] chunk = edgesLists2Chunk(edgeData, nodeIds);
    int chunkIndex = inEdges.addData(chunk);
    id2InEdgeChunk.set(nodeId, chunkIndex);
  }
  public void addOutEdges(int nodeId, List<Integer> edgeData, List<Integer> nodeIds) {
    if(edgeData.size() != nodeIds.size()) {
      throw new RuntimeException("Not the same number of elements for edgeData and nodeIds: "+edgeData.size()+"/"+nodeIds.size());
    }
    int[] chunk = edgesLists2Chunk(edgeData, nodeIds);
    int chunkIndex = outEdges.addData(chunk);
    id2OutEdgeChunk.set(nodeId, chunkIndex);
  }
  private int[] edgesLists2Chunk(List<Integer> edgeData, List<Integer> nodeIds) {
    if(edgeData.size() != nodeIds.size()) {
      throw new RuntimeException("Not the same number of elements for edgeData and nodeIds: "+edgeData.size()+"/"+nodeIds.size());
    }
    int size = edgeData.size()*2;
    int[] chunk = new int[size];
    for(int i=0; i<edgeData.size(); i++) {
      chunk[2*i] = edgeData.get(i);
      chunk[2*i+1] = nodeIds.get(i);
    }
    return chunk;
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
    int inChunk = id2InEdgeChunk.get(id);
    if(inChunk == -1) {
      System.out.println("No in edges for "+uri);
    } else {
      System.out.println("Chunk index for in edges "+inChunk);
      int[] chunk = inEdges.getData(inChunk);
      int size = chunk.length/2;
      System.out.println("Got in edges "+size);
      for(int i=0; i<chunk.length; i++) {
        int relData = chunk[2*i];
        int nodeId = chunk[2*i+1];
        System.out.println("In Edge "+i+": nodeid="+relData+", data="+nodeId);
      }
    }
    int outChunk = id2OutEdgeChunk.get(id);
    if(outChunk == -1) {
      System.out.println("No out edges for "+uri);
    } else {
      System.out.println("Chunk index for out edges "+outChunk);
      int[] chunk = outEdges.getData(outChunk);
      int size = chunk.length/2;
      System.out.println("Got out edges "+size);
      for(int i=0; i<size; i++) {
        int relData = chunk[2*i];
        int nodeId = chunk[2*i+1];
        System.out.println("Out Edge "+i+": nodeid="+relData+", data="+nodeId);
      }
    }
  }
  
  public int debugGetInEdgesSize() {
    return inEdges.size();
  }
  public int debugGetOutEdgesSize() {
    return outEdges.size();
  }
  public int debugGetInId2ChunkSize() {
    return id2InEdgeChunk.size();
  }
  public int debugGetOutId2ChunkSize() {
    return id2OutEdgeChunk.size();
  }
  
  
  // ******** HELPER METHODS for handling edge data
  private static void setEdge(int[] edges, int edgeNumber, int edgedata, int nodeId) {
    edges[edgeNumber << 1] = edgedata;
    edges[(edgeNumber << 1)+1] = nodeId;
  }
  
  public static void main(String[] args) {
    System.out.println("Running main ...");
    GraphStore gstore = new GraphStore();
    gstore.addNode("uri1");
    gstore.addNode("uri2");
    gstore.addNode("uri3");
    gstore.addNode("uri4");
    gstore.addNode("uri5");
    gstore.addNode("uri6");
    
    List<Integer> edgeData = new ArrayList<Integer>();
    List<Integer> nodeIds = new ArrayList<Integer>();
    
    edgeData.add(22);
    nodeIds.add(gstore.getNodeId("uri2"));
    gstore.addOutEdges(gstore.getNodeId("uri1"), edgeData, nodeIds);
    
    gstore.debugPrintEdges("uri1");
    gstore.debugPrintEdges("uri2");
    System.out.println("InEdgesSize="+gstore.debugGetInEdgesSize());
    System.out.println("OutEdgesSize="+gstore.debugGetOutEdgesSize());
    System.out.println("Finishing main ....");
  }
  
  
  
}
