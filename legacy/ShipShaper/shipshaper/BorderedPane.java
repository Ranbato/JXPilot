/*    */ package shipshaper;
/*    */ 
/*    */ import java.awt.Component;
/*    */ import java.awt.Graphics;
/*    */ 
/*    */ public final class BorderedPane extends java.awt.Panel
/*    */ {
/*    */   public static final int LEFT = 0;
/*    */   public static final int RIGHT = 2;
/*    */   public static final int CENTER = 1;
/*    */   protected String title;
/*    */   protected java.awt.FontMetrics fm;
/*    */   protected int strw;
/*    */   protected int strh;
/*    */   
/*    */   public BorderedPane(String paramString, Component paramComponent)
/*    */   {
/* 18 */     this.title = paramString;
/*    */     
/* 20 */     setLayout(new java.awt.FlowLayout(1));
/* 21 */     setBackground(java.awt.Color.lightGray);
/* 22 */     add(paramComponent);
/*    */   }
/*    */   
/*    */   public BorderedPane(String paramString, Component paramComponent, int paramInt)
/*    */   {
/* 27 */     this.title = paramString;
/*    */     
/* 29 */     setLayout(new java.awt.FlowLayout(paramInt));
/* 30 */     setBackground(java.awt.Color.lightGray);
/* 31 */     add(paramComponent);
/*    */   }
/*    */   
/*    */   public java.awt.Insets getInsets()
/*    */   {
/* 36 */     this.fm = getFontMetrics(getFont());
/* 37 */     return new java.awt.Insets(this.fm.getDescent() + 8, 8, 8, 8);
/*    */   }
/*    */   
/*    */   public void paint(Graphics paramGraphics)
/*    */   {
/* 42 */     int i = getSize().width;
/* 43 */     int j = getSize().height;
/* 44 */     this.fm = getFontMetrics(getFont());
/* 45 */     this.strw = this.fm.stringWidth(this.title);
/* 46 */     this.strh = this.fm.getHeight();
/*    */     
/* 48 */     paramGraphics.setColor(java.awt.Color.lightGray);
/*    */     
/* 50 */     paramGraphics.draw3DRect(4, 4, i - 8, j - 8, false);
/* 51 */     paramGraphics.draw3DRect(5, 5, i - 10, j - 10, true);
/* 52 */     paramGraphics.fillRect(10, 0, this.strw + 4, this.strh);
/*    */     
/* 54 */     paramGraphics.setColor(java.awt.Color.black);
/* 55 */     paramGraphics.drawString(this.title, 12, 10);
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\BorderedPane.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */