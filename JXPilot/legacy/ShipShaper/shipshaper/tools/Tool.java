/*    */ package shipshaper.tools;
/*    */ 
/*    */ import shipshaper.DrawArea;
/*    */ 
/*    */ public class Tool
/*    */ {
/*    */   protected DrawArea da;
/*    */   protected int oldx;
/*    */   protected int oldy;
/*    */   protected String status;
/*    */   
/*    */   public Tool(DrawArea paramDrawArea)
/*    */   {
/* 14 */     this.da = paramDrawArea;
/* 15 */     this.status = getName();
/*    */   }
/*    */   
/* 18 */   public String getName() { return "[Tool]"; }
/*    */   
/* 20 */   public boolean wantVertex() { return false; }
/*    */   
/* 22 */   public boolean wantEdge() { return false; }
/*    */   
/* 24 */   public boolean wantItem() { return false; }
/*    */   
/* 26 */   public boolean isActive() { return false; }
/*    */   
/*    */   public void mousePress(int paramInt1, int paramInt2) {}
/*    */   
/*    */   public void mouseMove(int paramInt1, int paramInt2) {}
/*    */   
/*    */   public void rememberOld(int paramInt1, int paramInt2)
/*    */   {
/* 34 */     this.oldx = paramInt1;
/* 35 */     this.oldy = paramInt2;
/*    */   }
/*    */   
/*    */   public void end() {}
/*    */   
/* 40 */   public String getStatus() { return this.status; }
/*    */   
/* 42 */   public int getImageId() { return 0; }
/*    */   
/* 44 */   public java.awt.Image getImage() { return shipshaper.Images.getImage(getImageId()); }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public void pr(String paramString)
/*    */   {
/* 52 */     System.out.println(paramString);
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\tools\Tool.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */