/*    */ package shipshaper.imagebuttons;
/*    */ 
/*    */ import java.awt.Image;
/*    */ import java.awt.event.MouseEvent;
/*    */ 
/*    */ public class ToggleImageButton extends ImageButton
/*    */ {
/*    */   public ToggleImageButton(String paramString, Image paramImage)
/*    */   {
/* 10 */     super(paramString, paramImage);
/*    */   }
/*    */   
/*    */   public boolean isDown()
/*    */   {
/* 15 */     return this.down;
/*    */   }
/*    */   
/*    */   public void toggle()
/*    */   {
/* 20 */     this.down = (this.down != true);
/*    */   }
/*    */   
/*    */   public void mousePressed(MouseEvent paramMouseEvent)
/*    */   {
/* 25 */     if (isEnabled())
/*    */     {
/* 27 */       toggle();
/* 28 */       repaint();
/* 29 */       if (this.actionListener != null) {
/* 30 */         this.actionListener.actionPerformed(
/* 31 */           new java.awt.event.ActionEvent(this, 1001, this.id));
/*    */       }
/*    */     }
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\imagebuttons\ToggleImageButton.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */