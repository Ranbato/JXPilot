/*    */ package shipshaper.tools;
/*    */ 
/*    */ import shipshaper.graph.Vertex;
/*    */ 
/*    */ public class JoinTool
/*    */   extends Tool
/*    */ {
/*  8 */   public JoinTool(shipshaper.DrawArea paramDrawArea) { super(paramDrawArea); }
/*    */   
/* 10 */   public String getName() { return "Join two edges"; }
/*    */   
/* 12 */   public int getImageId() { return 16; }
/*    */   
/* 14 */   public boolean wantVertex() { return true; }
/*    */   
/*    */   public void mousePress(int paramInt1, int paramInt2)
/*    */   {
/* 18 */     Vertex localVertex3 = this.da.getSelectedVertex();
/* 19 */     if (localVertex3 != null)
/*    */     {
/* 21 */       if (localVertex3.valency() != 2)
/*    */       {
/* 23 */         System.err.println("Can only join two edges, no more, no less");
/*    */ 
/*    */       }
/*    */       else
/*    */       {
/*    */ 
/* 29 */         Vertex localVertex1 = localVertex3.oppositeVertex(0);
/* 30 */         Vertex localVertex2 = localVertex3.oppositeVertex(1);
/*    */         
/*    */ 
/* 33 */         shipshaper.Ship localShip = this.da.getShip();
/* 34 */         localShip.shape.remove(localVertex3.edge(0));
/* 35 */         localShip.shape.remove(localVertex3.edge(0));
/* 36 */         localShip.shape.remove(localVertex3);
/*    */         
/*    */ 
/* 39 */         localShip.shape.add(new shipshaper.graph.Edge(localVertex1, localVertex2));
/*    */         
/* 41 */         localShip.shipChanged(new shipshaper.ShipEvent(this, true, false, false, false));
/*    */       }
/*    */     }
/* 44 */     this.da.toolFinished();
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\tools\JoinTool.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */