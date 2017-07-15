/*    */ package shipshaper.tools;
/*    */ 
/*    */ import shipshaper.DrawArea;
/*    */ 
/*    */ public class PolyLineTool
/*    */   extends LineTool {
/*  7 */   public PolyLineTool(DrawArea paramDrawArea) { super(paramDrawArea); }
/*    */   
/*  9 */   public String getName() { return "Edges"; }
/*    */   
/* 11 */   public int getImageId() { return 12; }
/*    */   
/*    */   public void finishedLineAt(int paramInt1, int paramInt2)
/*    */   {
/* 15 */     newEdge(paramInt1, paramInt2);
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\tools\PolyLineTool.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */