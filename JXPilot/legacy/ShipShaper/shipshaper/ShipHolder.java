/*    */ package shipshaper;
/*    */ 
/*    */ import java.awt.Color;
/*    */ import java.awt.Component;
/*    */ import java.awt.event.MouseEvent;
/*    */ 
/*    */ public final class ShipHolder extends java.awt.Panel implements java.awt.event.MouseListener, ShipListener
/*    */ {
/*    */   private ShipBrowser sb;
/*    */   private Ship ship;
/*    */   private ShipView sv;
/*    */   private java.awt.Label namelabel;
/*    */   private boolean selected;
/* 14 */   private static final Color colorUnselected = new Color(150, 150, 150);
/* 15 */   private static final Color colorSelected = Color.red;
/*    */   
/*    */   ShipHolder(ShipBrowser paramShipBrowser, Ship paramShip)
/*    */   {
/* 19 */     this.sb = paramShipBrowser;
/* 20 */     this.ship = paramShip;
/* 21 */     this.sv = new ShipView(paramShip);
/* 22 */     this.namelabel = new java.awt.Label(paramShip.name);
/* 23 */     this.namelabel.setFont(new java.awt.Font("TimesRoman", 0, 10));
/*    */     
/* 25 */     addMouseListener(this);
/* 26 */     this.namelabel.addMouseListener(this);
/* 27 */     this.sv.addMouseListener(this);
/* 28 */     paramShip.addShipListener(this);
/*    */     
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/* 34 */     java.awt.GridBagLayout localGridBagLayout = new java.awt.GridBagLayout();
/* 35 */     java.awt.GridBagConstraints localGridBagConstraints = new java.awt.GridBagConstraints();
/* 36 */     setLayout(localGridBagLayout);
/*    */     
/* 38 */     localGridBagConstraints.fill = 0;
/* 39 */     localGridBagConstraints.gridwidth = 0;
/* 40 */     localGridBagConstraints.weighty = 1.0D;
/* 41 */     localGridBagLayout.setConstraints(this.sv, localGridBagConstraints);
/* 42 */     add(this.sv);
/*    */     
/* 44 */     localGridBagConstraints.fill = 2;
/* 45 */     localGridBagConstraints.anchor = 17;
/* 46 */     localGridBagConstraints.weighty = 0.0D;
/* 47 */     localGridBagLayout.setConstraints(this.namelabel, localGridBagConstraints);
/* 48 */     add(this.namelabel);
/*    */     
/* 50 */     unselect();
/*    */   }
/*    */   
/*    */   public Ship getShip()
/*    */   {
/* 55 */     return this.ship;
/*    */   }
/*    */   
/*    */   public void shipChanged(ShipEvent paramShipEvent)
/*    */   {
/* 60 */     if (paramShipEvent.nameChanged()) {
/* 61 */       this.namelabel.setText(this.ship.name);
/*    */     }
/*    */   }
/*    */   
/*    */   public void select() {
/* 66 */     this.selected = true;
/* 67 */     setBackground(colorSelected);
/* 68 */     this.namelabel.setBackground(colorSelected);
/*    */   }
/*    */   
/*    */   public void unselect()
/*    */   {
/* 73 */     this.selected = false;
/* 74 */     setBackground(colorUnselected);
/* 75 */     this.namelabel.setBackground(colorUnselected);
/*    */   }
/*    */   
/*    */   public java.awt.Insets getInsets()
/*    */   {
/* 80 */     return new java.awt.Insets(5, 5, 5, 5);
/*    */   }
/*    */   
/*    */   public void mousePressed(MouseEvent paramMouseEvent)
/*    */   {
/* 85 */     this.sb.selectShipHolder(this);
/*    */   }
/*    */   
/*    */   public void mouseClicked(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseReleased(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseDragged(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseMoved(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseEntered(MouseEvent paramMouseEvent) {}
/*    */   
/*    */   public void mouseExited(MouseEvent paramMouseEvent) {}
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\ShipHolder.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */