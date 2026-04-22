/*    */ package shipshaper.tools;
/*    */ 
/*    */ import shipshaper.DrawArea;
/*    */ 
/*    */ public class ItemTool extends Tool
/*    */ {
/*    */   int type;
/*    */   
/*    */   public ItemTool(DrawArea paramDrawArea, int paramInt)
/*    */   {
/* 11 */     super(paramDrawArea);
/* 12 */     this.type = paramInt;
/*    */   }
/*    */   
/* 15 */   public String getName() { return shipshaper.Item.getName(this.type); }
/*    */   
/* 17 */   public int getType() { return this.type; }
/*    */   
/* 19 */   public int getImageId() { return shipshaper.Item.getImageId(this.type); }
/*    */   
/*    */   public void mousePress(int paramInt1, int paramInt2)
/*    */   {
/* 23 */     this.da.getShip().items.add(new shipshaper.Item(this.type, paramInt1, paramInt2));
/* 24 */     this.da.getShip().shipChanged(new shipshaper.ShipEvent(this, false, true, false, false));
/* 25 */     this.da.toolFinished();
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\tools\ItemTool.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */