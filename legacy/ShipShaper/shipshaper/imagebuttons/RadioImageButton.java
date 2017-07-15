/*    */ package shipshaper.imagebuttons;
/*    */ 
/*    */ import java.awt.Image;
/*    */ import java.awt.event.MouseEvent;
/*    */ 
/*    */ public class RadioImageButton extends ImageButton
/*    */ {
/*    */   private RadioImageButtonGroup group;
/*    */   
/*    */   public RadioImageButton(String paramString, Image paramImage, RadioImageButtonGroup paramRadioImageButtonGroup)
/*    */   {
/* 12 */     super(paramString, paramImage);
/* 13 */     this.group = paramRadioImageButtonGroup;
/* 14 */     paramRadioImageButtonGroup.add(this);
/*    */   }
/*    */   
/*    */   public boolean isDown()
/*    */   {
/* 19 */     return this.down;
/*    */   }
/*    */   
/*    */   public void toggle()
/*    */   {
/* 24 */     this.down = (this.down != true);
/* 25 */     repaint();
/*    */   }
/*    */   
/*    */   public void mousePressed(MouseEvent paramMouseEvent)
/*    */   {
/* 30 */     if ((isEnabled()) && (!isDown()))
/*    */     {
/* 32 */       toggle();
/* 33 */       this.group.select(this);
/* 34 */       if (this.actionListener != null) {
/* 35 */         this.actionListener.actionPerformed(
/* 36 */           new java.awt.event.ActionEvent(this, 1001, this.id));
/*    */       }
/*    */     }
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\imagebuttons\RadioImageButton.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */