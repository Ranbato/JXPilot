/*    */ package shipshaper.tools;
/*    */ 
/*    */ import shipshaper.DrawArea;
/*    */ 
/*    */ public class DeleteTool
/*    */   extends Tool
/*    */ {
/*    */   private shipshaper.Ship ship;
/*    */   
/* 10 */   public DeleteTool(DrawArea paramDrawArea) { super(paramDrawArea); }
/*    */   
/* 12 */   public String getName() { return "Delete"; }
/*    */   
/* 14 */   public int getImageId() { return 14; }
/*    */   
/* 16 */   public boolean wantVertex() { return true; }
/*    */   
/* 18 */   public boolean wantEdge() { return true; }
/*    */   
/* 20 */   public boolean wantItem() { return true; }
/*    */   
/*    */   public void mousePress(int paramInt1, int paramInt2)
/*    */   {
/* 24 */     this.ship = this.da.getShip();
/*    */     
/* 26 */     if (this.da.getSelectedVertex() != null)
/*    */     {
/* 28 */       shipshaper.graph.Vertex localVertex = this.da.getSelectedVertex();
/* 29 */       while (localVertex.valency() > 1)
/* 30 */         this.ship.shape.remove(localVertex.edge(0));
/* 31 */       this.ship.shape.remove(localVertex.edge(0));
/* 32 */       this.ship.shape.remove(localVertex);
/* 33 */       this.ship.shipChanged(new shipshaper.ShipEvent(this, true, false, false, false));
/*    */ 
/*    */     }
/* 36 */     else if (this.da.getSelectedEdge() != null)
/*    */     {
/* 38 */       this.ship.shape.remove(this.da.getSelectedEdge());
/* 39 */       this.ship.shipChanged(new shipshaper.ShipEvent(this, true, false, false, false));
/*    */ 
/*    */     }
/* 42 */     else if (this.da.getSelectedItem() != null)
/*    */     {
/* 44 */       this.ship.items.remove(this.da.getSelectedItem());
/* 45 */       this.ship.shipChanged(new shipshaper.ShipEvent(this, false, true, false, false));
/*    */     }
/* 47 */     this.da.toolFinished();
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\tools\DeleteTool.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */