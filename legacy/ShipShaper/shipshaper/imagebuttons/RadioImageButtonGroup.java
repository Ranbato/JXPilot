/*    */ package shipshaper.imagebuttons;
/*    */ 
/*    */ import java.util.Vector;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class RadioImageButtonGroup
/*    */ {
/* 17 */   private Vector buttons = new Vector();
/* 18 */   private RadioImageButton selected = null;
/*    */   
/*    */ 
/*    */   public void add(RadioImageButton paramRadioImageButton)
/*    */   {
/* 23 */     this.buttons.addElement(paramRadioImageButton);
/*    */   }
/*    */   
/*    */   public RadioImageButton getSelected()
/*    */   {
/* 28 */     return this.selected;
/*    */   }
/*    */   
/*    */   public void select(RadioImageButton paramRadioImageButton)
/*    */   {
/* 33 */     if (paramRadioImageButton == this.selected) {
/* 34 */       return;
/*    */     }
/*    */     
/* 37 */     for (int i = 0; i < this.buttons.size(); i++)
/*    */     {
/* 39 */       RadioImageButton localRadioImageButton = (RadioImageButton)this.buttons.elementAt(i);
/* 40 */       if ((localRadioImageButton != paramRadioImageButton) && (localRadioImageButton.isDown()))
/* 41 */         localRadioImageButton.toggle();
/*    */     }
/* 43 */     this.selected = paramRadioImageButton;
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\imagebuttons\RadioImageButtonGroup.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */