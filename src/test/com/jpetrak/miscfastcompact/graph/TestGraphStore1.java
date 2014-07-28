
package com.jpetrak.miscfastcompact.graph;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestGraphStore1 {
  @Test
  public void importTest1() {
    GraphStore gstore = new GraphStore();
    gstore.startImport(1000);
    gstore.addEdge("uri1", "uri2", 1);
    gstore.addEdge("uri1", "uri3", 2);
    gstore.addEdge("uri1", "uri4", 3);
    gstore.addEdge("uri1", "uri5", 4);
    gstore.addEdge("uri4", "uri1", 5);
    gstore.addEdge("uri4", "uri6", 6);
    gstore.addEdge("uri8", "uri9", 7);
    assertEquals("Dummy test",0,0);
  }
}
