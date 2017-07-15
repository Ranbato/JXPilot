/*    */ package shipshaper.imagebuttons;
/*    */ 
/*    */ import java.awt.Component;
/*    */ import java.awt.Graphics;
/*    */ import java.awt.Image;
/*    */ import java.awt.event.MouseEvent;
/*    */ 
/*    */ class ImageButton extends java.awt.Canvas implements java.awt.event.MouseListener
/*    */ {
/*    */   protected String id;
/*    */   protected Image img;
/*    */   protected boolean down;
/*    */   protected java.awt.event.ActionListener actionListener;
/* 14 */   protected final int border = 1;
/*    */   
/*    */   public ImageButton(String paramString, Image paramImage)
/*    */   {
/* 18 */     this.id = paramString;
/* 19 */     this.img = paramImage;
/* 20 */     this.down = false;
/* 21 */     addMouseListener(this);
/* 22 */     fitSize();
/*    */   }
/*    */   
/*    */   public void addActionListener(java.awt.event.ActionListener paramActionListener)
/*    */   {
/* 27 */     this.actionListener = java.awt.AWTEventMulticaster.add(this.actionListener, paramActionListener);
/*    */   }
/*    */   
/*    */   public void setEnabled(boolean paramBoolean)
/*    */   {
/* 32 */     if ((paramBoolean == true) && (!isEnabled()))
/*    */     {
/* 34 */       this.down = false;
/* 35 */       super.setEnabled(true);
/* 36 */       repaint();
/*    */     }
/* 38 */     else if (isEnabled())
/*    */     {
/* 40 */       this.down = false;
/* 41 */       super.setEnabled(false);
/* 42 */       repaint();
/*    */     }
/*    */   }
/*    */   
/*    */   public void fitSize()
/*    */   {
/* 48 */     int i = this.img.getWidth(this);
/* 49 */     int j = this.img.getHeight(this);
/*    */     
/* 51 */     setSize(i + 2, j + 2);
/*    */   }
/*    */   
/*    */   public void setState(boolean paramBoolean)
/*    */   {
/* 56 */     this.down = paramBoolean;
/* 57 */     repaint();
/*    */   }
/*    */   
/*    */   public void update(Graphics paramGraphics)
/*    */   {
/* 62 */     paint(paramGraphics);
/*    */   }
/*    */   
/*    */   public void paint(Graphics paramGraphics)
/*    */   {
/* 67 */     int i = getSize().width;
/* 68 */     int j = getSize().height;
/*    */     
/*    */ 
/*    */ 
/* 72 */     if (this.down) {
/* 73 */       paramGraphics.drawImage(this.img, 2, 2, this);
/*    */     } else {
/* 75 */       paramGraphics.drawImage(this.img, 1, 1, this);
/*    */     }
/*    */     
/* 78 */     paramGraphics.setColor(shipshaper.Colors.BUTTON);
/* 79 */     for (int k = 0; k < 1; k++) {
/* 80 */       paramGraphics.draw3DRect(k, k, i - k * 2 - 1, j - k * 2 - 1, this.down == false);
/*    */     }
/*    */     
/* 83 */     if (!isEnabled())
/*    */     {
/* 85 */       for (k = 0; k < i / 2; k++)
/* 86 */         paramGraphics.drawLine(k * 2, 0, k * 2, j - 1);
/* 87 */       for (k = 0; k < j / 2; k++) {
/* 88 */         paramGraphics.drawLine(0, k * 2, i - 1, k * 2);
/*    */       }
/*    */     }
/*    */   }
/*    */   
/*    */   public void mouseClicked(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mousePressed(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseReleased(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseEntered(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseExited(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseDragged(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseMoved(MouseEvent paramMouseEvent) {}
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\imagebuttons\ImageButton.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */