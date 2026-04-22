/*    */ package shipshaper;
/*    */ 
/*    */ import java.awt.Dimension;
/*    */ import java.awt.Graphics;
/*    */ import java.awt.Image;
/*    */ 
/*    */ public final class ImageHolder extends java.awt.Canvas
/*    */ {
/*    */   public Image image;
/*    */   
/*    */   ImageHolder(Image paramImage)
/*    */   {
/* 13 */     this.image = paramImage;
/*    */   }
/*    */   
/*    */   public Dimension getPreferredSize()
/*    */   {
/* 18 */     return new Dimension(this.image.getWidth(this), this.image.getHeight(this));
/*    */   }
/*    */   
/*    */   public Dimension getMinimumSize()
/*    */   {
/* 23 */     return getPreferredSize();
/*    */   }
/*    */   
/*    */   public Dimension getMaximumSize()
/*    */   {
/* 28 */     return getPreferredSize();
/*    */   }
/*    */   
/*    */   public void paint(Graphics paramGraphics)
/*    */   {
/* 33 */     if (this.image != null) {
/* 34 */       paramGraphics.drawImage(this.image, 0, 0, this);
/*    */     }
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\ImageHolder.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */