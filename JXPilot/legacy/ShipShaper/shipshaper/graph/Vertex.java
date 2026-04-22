/*    */ package shipshaper.graph;
/*    */ 
/*    */ import java.io.PrintStream;
/*    */ import java.util.Vector;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class Vertex
/*    */ {
/*    */   public int x;
/*    */   public int y;
/*    */   public Vector edges;
/*    */   
/*    */   public Vertex()
/*    */   {
/* 17 */     this(0, 0);
/*    */   }
/*    */   
/*    */   public Vertex(int paramInt1, int paramInt2)
/*    */   {
/* 22 */     this.x = paramInt1;
/* 23 */     this.y = paramInt2;
/* 24 */     this.edges = new Vector();
/*    */   }
/*    */   
/*    */   public Vertex(Vertex paramVertex)
/*    */   {
/* 29 */     this(paramVertex.x, paramVertex.y);
/*    */   }
/*    */   
/*    */   public void moveTo(int paramInt1, int paramInt2)
/*    */   {
/* 34 */     this.x = paramInt1;
/* 35 */     this.y = paramInt2;
/*    */   }
/*    */   
/*    */   public void translate(int paramInt1, int paramInt2)
/*    */   {
/* 40 */     this.x += paramInt1;
/* 41 */     this.y += paramInt2;
/*    */   }
/*    */   
/*    */   public boolean equals(Vertex paramVertex)
/*    */   {
/* 46 */     return (this.x == paramVertex.x) && (this.y == paramVertex.y);
/*    */   }
/*    */   
/*    */   public void addEdge(Edge paramEdge)
/*    */   {
/* 51 */     if (!this.edges.contains(paramEdge)) {
/* 52 */       this.edges.addElement(paramEdge);
/*    */     }
/*    */   }
/*    */   
/*    */   public void removeEdge(Edge paramEdge) {
/* 57 */     this.edges.removeElement(paramEdge);
/*    */   }
/*    */   
/*    */   public int valency()
/*    */   {
/* 62 */     return this.edges.size();
/*    */   }
/*    */   
/*    */   public Edge edge(int paramInt)
/*    */   {
/* 67 */     return (Edge)this.edges.elementAt(paramInt);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */   public Vertex oppositeVertex(int paramInt)
/*    */   {
/* 74 */     Edge localEdge = edge(paramInt);
/* 75 */     if (localEdge.v1 == this) {
/* 76 */       return localEdge.v2;
/*    */     }
/* 78 */     return localEdge.v1;
/*    */   }
/*    */   
/*    */   public void print()
/*    */   {
/* 83 */     System.out.println("Vertex " + toString());
/*    */   }
/*    */   
/*    */   public String toString()
/*    */   {
/* 88 */     return "(" + this.x + ", " + this.y + "), valency " + valency();
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\graph\Vertex.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */