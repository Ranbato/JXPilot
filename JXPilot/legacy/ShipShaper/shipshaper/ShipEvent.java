/*    */ package shipshaper;
/*    */ 
/*    */ import java.util.EventObject;
/*    */ 
/*    */ public final class ShipEvent extends EventObject
/*    */ {
/*    */   private boolean shape;
/*    */   private boolean items;
/*    */   private boolean name;
/*    */   private boolean author;
/*    */   
/*    */   public ShipEvent(Object paramObject, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3, boolean paramBoolean4) {
/* 13 */     super(paramObject);
/* 14 */     this.shape = paramBoolean1;
/* 15 */     this.items = paramBoolean2;
/* 16 */     this.name = paramBoolean3;
/* 17 */     this.author = paramBoolean4;
/*    */   }
/*    */   
/*    */   public boolean shapeChanged()
/*    */   {
/* 22 */     return this.shape;
/*    */   }
/*    */   
/*    */   public boolean itemsChanged()
/*    */   {
/* 27 */     return this.items;
/*    */   }
/*    */   
/*    */   public boolean nameChanged()
/*    */   {
/* 32 */     return this.name;
/*    */   }
/*    */   
/*    */   public boolean authorChanged()
/*    */   {
/* 37 */     return this.author;
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\ShipEvent.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */