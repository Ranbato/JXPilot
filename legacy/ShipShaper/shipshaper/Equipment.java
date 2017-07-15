/*    */ package shipshaper;
/*    */ 
/*    */ import java.util.Vector;
/*    */ 
/*    */ public final class Equipment
/*    */ {
/*    */   private Vector items;
/*    */   
/*    */   public Equipment()
/*    */   {
/* 11 */     this.items = new Vector();
/*    */   }
/*    */   
/*    */   public Equipment(Equipment paramEquipment)
/*    */   {
/* 16 */     this.items = new Vector();
/* 17 */     for (int i = 0; i < paramEquipment.countItems(); i++) {
/* 18 */       add(new Item(paramEquipment.item(i)));
/*    */     }
/*    */   }
/*    */   
/*    */   public void add(Item paramItem) {
/* 23 */     if (!this.items.contains(paramItem)) {
/* 24 */       this.items.addElement(paramItem);
/*    */     }
/*    */   }
/*    */   
/*    */   public void remove(Item paramItem) {
/* 29 */     if (this.items.contains(paramItem)) {
/* 30 */       this.items.removeElement(paramItem);
/*    */     }
/*    */   }
/*    */   
/*    */   public boolean contains(Item paramItem) {
/* 35 */     return this.items.contains(paramItem);
/*    */   }
/*    */   
/*    */   public int countItems()
/*    */   {
/* 40 */     return this.items.size();
/*    */   }
/*    */   
/*    */   public Item item(int paramInt)
/*    */   {
/* 45 */     return (Item)this.items.elementAt(paramInt);
/*    */   }
/*    */   
/*    */   public int countItemsOfType(int paramInt)
/*    */   {
/* 50 */     int i = 0;
/*    */     
/* 52 */     for (int j = 0; j < countItems(); j++)
/* 53 */       if (item(j).type == paramInt)
/* 54 */         i++;
/* 55 */     return i;
/*    */   }
/*    */   
/*    */   public Equipment getItemsOfType(int paramInt)
/*    */   {
/* 60 */     Equipment localEquipment = new Equipment();
/*    */     
/* 62 */     for (int i = 0; i < countItems(); i++)
/* 63 */       if (item(i).type == paramInt)
/* 64 */         localEquipment.add(item(i));
/* 65 */     return localEquipment;
/*    */   }
/*    */   
/*    */   public void print()
/*    */   {
/* 70 */     System.out.println("***** Items:");
/* 71 */     for (int i = 0; i < countItems(); i++) {
/* 72 */       item(i).print();
/*    */     }
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\Equipment.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */