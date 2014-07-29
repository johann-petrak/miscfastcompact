/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jpetrak.miscfastcompact.graph;

/**
 *
 * @author Johann Petrak
 */
public class Edge implements Comparable<Edge>  {
  public int edgeData;
  public int nodeId;

  public Edge(int edgeData, int nodeId) {
    this.edgeData = edgeData;
    this.nodeId = nodeId;
  }
  
  @Override
  public int compareTo(Edge o) {
    return (o.nodeId == this.nodeId) ? 0 : ((this.nodeId < o.nodeId) ? -1 : 1);
  }
}
