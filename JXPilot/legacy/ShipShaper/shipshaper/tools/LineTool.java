/*    */ package shipshaper.tools;
/*    */ 
/*    */ import shipshaper.Ship;
/*    */ import shipshaper.graph.Vertex;
/*    */ 
/*    */ public class LineTool extends Tool
/*    */ {
/*  8 */   protected boolean drawing = false;
/*    */   
/*    */   protected Vertex v1;
/*    */   protected Vertex v2;
/*    */   
/* 13 */   public LineTool(shipshaper.DrawArea paramDrawArea) { super(paramDrawArea); }
/*    */   
/* 15 */   public boolean wantVertex() { return true; }
/*    */   
/*    */   protected shipshaper.graph.Edge tmpedge;
/*    */   protected Ship ship;
/* 19 */   public void newEdge(int paramInt1, int paramInt2) { this.v1 = this.da.vertexAtSC(paramInt1, paramInt2);
/* 20 */     if (this.v1 == null)
/* 21 */       this.v1 = new Vertex(paramInt1, paramInt2);
/* 22 */     this.v2 = new Vertex(paramInt1, paramInt2);
/* 23 */     this.tmpedge = new shipshaper.graph.Edge(this.v1, this.v2);
/* 24 */     this.ship.shape.add(this.tmpedge);
/*    */   }
/*    */   
/* 27 */   public boolean isActive() { return this.drawing == true; }
/*    */   
/*    */   public void mousePress(int paramInt1, int paramInt2)
/*    */   {
/* 31 */     if (this.drawing == false)
/*    */     {
/* 33 */       this.ship = this.da.getShip();
/* 34 */       newEdge(paramInt1, paramInt2);
/* 35 */       this.drawing = true;
/*    */ 
/*    */     }
/*    */     else
/*    */     {
/*    */ 
/* 41 */       this.ship.shape.remove(this.tmpedge);
/* 42 */       this.v2 = this.da.vertexAtSC(paramInt1, paramInt2);
/* 43 */       if (this.v2 == null)
/* 44 */         this.v2 = new Vertex(paramInt1, paramInt2);
/* 45 */       if (!this.v1.equals(this.v2))
/* 46 */         this.ship.shape.add(new shipshaper.graph.Edge(this.v1, this.v2));
/* 47 */       this.ship.shipChanged(new shipshaper.ShipEvent(this, true, true, false, false));
/* 48 */       finishedLineAt(paramInt1, paramInt2);
/*    */     }
/*    */   }
/*    */   
/*    */   public void mouseMove(int paramInt1, int paramInt2)
/*    */   {
/* 54 */     if (this.drawing)
/*    */     {
/* 56 */       this.tmpedge.v2.moveTo(paramInt1, paramInt2);
/* 57 */       this.status = "Click LB to place next point (RB cancels)";
/* 58 */       this.da.repaint();
/*    */     }
/*    */     else {
/* 61 */       this.status = "Click LB to place first edge endpoint";
/*    */     }
/*    */   }
/*    */   
/*    */   public void end() {
/* 66 */     this.drawing = false;
/* 67 */     this.ship.shape.remove(this.tmpedge);
/* 68 */     this.da.toolFinished();
/* 69 */     this.ship.shape.removeDoubles();
/*    */   }
/*    */   
/*    */   public void finishedLineAt(int paramInt1, int paramInt2)
/*    */   {
/* 74 */     end();
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\tools\LineTool.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */