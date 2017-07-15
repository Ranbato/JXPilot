/*    */ package shipshaper.graph;
/*    */ 
/*    */ import java.awt.Rectangle;
/*    */ import java.io.PrintStream;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class Edge
/*    */ {
/*    */   public Vertex v1;
/*    */   public Vertex v2;
/*    */   
/*    */   public Edge(Vertex paramVertex1, Vertex paramVertex2)
/*    */   {
/* 16 */     this.v1 = paramVertex1;
/* 17 */     this.v1.addEdge(this);
/*    */     
/* 19 */     this.v2 = paramVertex2;
/* 20 */     this.v2.addEdge(this);
/*    */   }
/*    */   
/*    */   public Edge(Edge paramEdge)
/*    */   {
/* 25 */     this(new Vertex(paramEdge.v1), new Vertex(paramEdge.v2));
/*    */   }
/*    */   
/*    */   public boolean hasVertex(Vertex paramVertex)
/*    */   {
/* 30 */     return (this.v1 == paramVertex) || (this.v2 == paramVertex);
/*    */   }
/*    */   
/*    */   public boolean hasSameVerticesAs(Edge paramEdge)
/*    */   {
/* 35 */     return ((this.v1 == paramEdge.v1) && (this.v2 == paramEdge.v2)) || (
/* 36 */       (this.v1 == paramEdge.v2) && (this.v2 == paramEdge.v1));
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */   public boolean equals(Edge paramEdge)
/*    */   {
/* 43 */     return ((this.v1.equals(paramEdge.v1)) && (this.v2.equals(paramEdge.v2))) || (
/* 44 */       (this.v1.equals(paramEdge.v2)) && (this.v2.equals(paramEdge.v1)));
/*    */   }
/*    */   
/*    */ 
/*    */   public boolean equalsDir(Edge paramEdge)
/*    */   {
/* 50 */     return (this.v1.equals(paramEdge.v1)) && (this.v2.equals(paramEdge.v2));
/*    */   }
/*    */   
/*    */   public void translate(int paramInt1, int paramInt2)
/*    */   {
/* 55 */     this.v1.translate(paramInt1, paramInt2);
/* 56 */     this.v2.translate(paramInt1, paramInt2);
/*    */   }
/*    */   
/*    */   public Rectangle bounds()
/*    */   {
/* 61 */     return new Rectangle(Math.min(this.v1.x, this.v2.x), Math.min(this.v1.y, this.v2.y), 
/* 62 */       Math.abs(this.v2.x - this.v1.x), Math.abs(this.v2.y - this.v1.y));
/*    */   }
/*    */   
/*    */   public void print()
/*    */   {
/* 67 */     System.out.println("Edge " + toString());
/*    */   }
/*    */   
/*    */   public String toString()
/*    */   {
/* 72 */     return 
/* 73 */       "(" + this.v1.x + ", " + this.v1.y + ") to " + "(" + this.v2.x + ", " + this.v2.y + ")";
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\graph\Edge.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */