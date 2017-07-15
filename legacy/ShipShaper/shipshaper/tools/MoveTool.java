/*    */ package shipshaper.tools;
/*    */ 
/*    */ import shipshaper.DrawArea;
/*    */ import shipshaper.graph.Edge;
/*    */ import shipshaper.graph.Vertex;
/*    */ 
/*    */ 
/*    */ public class MoveTool
/*    */   extends Tool
/*    */ {
/*    */   static final int NOT_MOVING = 0;
/*    */   static final int MOVING_VERTEX = 1;
/*    */   static final int MOVING_EDGE = 2;
/*    */   static final int MOVING_ITEM = 3;
/*    */   
/* 16 */   public MoveTool(DrawArea paramDrawArea) { super(paramDrawArea); }
/* 17 */   private int mode = 0;
/*    */   
/*    */ 
/* 20 */   public String getName() { return "Move"; }
/*    */   
/* 22 */   public int getImageId() { return 13; }
/*    */   
/* 24 */   public boolean wantVertex() { return true; }
/*    */   
/* 26 */   public boolean wantEdge() { return true; }
/*    */   
/* 28 */   public boolean wantItem() { return true; }
/*    */   
/* 30 */   public boolean isActive() { return this.mode != 0; }
/*    */   
/*    */   public void mousePress(int paramInt1, int paramInt2)
/*    */   {
/* 34 */     if (this.mode == 0)
/*    */     {
/* 36 */       if (this.da.getSelectedVertex() != null) {
/* 37 */         this.mode = 1;
/* 38 */       } else if (this.da.getSelectedEdge() != null) {
/* 39 */         this.mode = 2;
/* 40 */       } else if (this.da.getSelectedItem() != null) {
/* 41 */         this.mode = 3;
/*    */       }
/*    */     }
/*    */     else {
/* 45 */       if ((this.mode == 1) || (this.mode == 2))
/* 46 */         this.da.getShip().shape.removeDoubles();
/* 47 */       end();
/*    */     }
/* 49 */     rememberOld(paramInt1, paramInt2);
/*    */   }
/*    */   
/*    */   public void mouseMove(int paramInt1, int paramInt2)
/*    */   {
/* 54 */     if (this.mode == 1) {
/* 55 */       this.da.getSelectedVertex().moveTo(paramInt1, paramInt2);
/* 56 */     } else if (this.mode == 2)
/*    */     {
/* 58 */       int i = paramInt1 - this.oldx;int j = paramInt2 - this.oldy;
/* 59 */       Edge localEdge = this.da.getSelectedEdge();
/*    */       
/* 61 */       if ((this.da.getShip().withinShipBorders(localEdge.v1.x + i, localEdge.v1.y + j)) && 
/* 62 */         (this.da.getShip().withinShipBorders(localEdge.v2.x + i, localEdge.v2.y + j))) {
/* 63 */         localEdge.translate(i, j);
/*    */       }
/* 65 */     } else if (this.mode == 3) {
/* 66 */       this.da.getSelectedItem().moveTo(paramInt1, paramInt2);
/*    */     }
/* 68 */     this.da.getShip().shipChanged(new shipshaper.ShipEvent(this, true, true, false, false));
/* 69 */     rememberOld(paramInt1, paramInt2);
/* 70 */     this.da.repaint();
/*    */   }
/*    */   
/*    */   public void end()
/*    */   {
/* 75 */     this.mode = 0;
/* 76 */     this.da.toolFinished();
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\tools\MoveTool.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */