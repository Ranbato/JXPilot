/*    */ package shipshaper.tools;
/*    */ 
/*    */ import shipshaper.Ship;
/*    */ 
/*    */ public class SplitTool
/*    */   extends Tool
/*    */ {
/*  8 */   public SplitTool(shipshaper.DrawArea paramDrawArea) { super(paramDrawArea); }
/*    */   
/* 10 */   public String getName() { return "Split edge"; }
/*    */   
/* 12 */   public int getImageId() { return 15; }
/*    */   
/* 14 */   public boolean wantEdge() { return true; }
/*    */   
/*    */   public void mousePress(int paramInt1, int paramInt2)
/*    */   {
/* 18 */     shipshaper.graph.Edge localEdge = this.da.getSelectedEdge();
/* 19 */     if (localEdge != null)
/*    */     {
/* 21 */       shipshaper.graph.Vertex localVertex = new shipshaper.graph.Vertex(paramInt1, paramInt2);
/* 22 */       Ship localShip = this.da.getShip();
/* 23 */       localShip.shape.add(localVertex);
/* 24 */       localShip.shape.add(new shipshaper.graph.Edge(localEdge.v1, localVertex));
/* 25 */       localShip.shape.add(new shipshaper.graph.Edge(localVertex, localEdge.v2));
/* 26 */       localShip.shape.remove(localEdge);
/* 27 */       localShip.shape.removeDoubles();
/* 28 */       localShip.shipChanged(new shipshaper.ShipEvent(this, true, false, false, false));
/*    */     }
/* 30 */     this.da.toolFinished();
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\tools\SplitTool.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */