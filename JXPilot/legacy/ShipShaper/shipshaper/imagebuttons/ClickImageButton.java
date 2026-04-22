/*    */ package shipshaper.imagebuttons;
/*    */ 
/*    */ import java.awt.Component;
/*    */ import java.awt.event.MouseEvent;
/*    */ 
/*    */ public class ClickImageButton extends ImageButton
/*    */ {
/*    */   public ClickImageButton(String paramString, java.awt.Image paramImage)
/*    */   {
/* 10 */     super(paramString, paramImage);
/*    */   }
/*    */   
/*    */ 
/*    */   public void mousePressed(MouseEvent paramMouseEvent)
/*    */   {
/* 16 */     if (isEnabled())
/*    */     {
/* 18 */       this.down = true;
/* 19 */       repaint();
/*    */     }
/*    */   }
/*    */   
/*    */   public void mouseReleased(MouseEvent paramMouseEvent)
/*    */   {
/* 25 */     if ((this.down) && (isEnabled()))
/*    */     {
/* 27 */       this.down = false;
/* 28 */       repaint();
/* 29 */       if (this.actionListener != null) {
/* 30 */         this.actionListener.actionPerformed(
/* 31 */           new java.awt.event.ActionEvent(this, 1001, this.id));
/*    */       }
/*    */     }
/*    */   }
/*    */   
/*    */   public void mouseExited(MouseEvent paramMouseEvent) {
/* 37 */     if ((this.down) && (isEnabled()))
/*    */     {
/* 39 */       this.down = false;
/* 40 */       repaint();
/*    */     }
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\imagebuttons\ClickImageButton.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */