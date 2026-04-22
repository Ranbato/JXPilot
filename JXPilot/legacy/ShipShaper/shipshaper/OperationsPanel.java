/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Container;
/*     */ import java.awt.GridBagConstraints;
/*     */ import java.awt.GridBagLayout;
/*     */ import java.awt.GridLayout;
/*     */ import java.awt.Panel;
/*     */ import java.awt.event.ActionEvent;
/*     */ import java.awt.event.ActionListener;
/*     */ import shipshaper.imagebuttons.ClickImageButton;
/*     */ import shipshaper.imagebuttons.ImageButton;
/*     */ 
/*     */ public final class OperationsPanel extends Panel implements ActionListener
/*     */ {
/*     */   private static final int CLEAR = 0;
/*     */   private static final int ROTATE_CLOCKWISE = 1;
/*     */   private static final int ROTATE_ANTI_CLOCKWISE = 2;
/*     */   private static final int FLIP_HORIZ = 3;
/*     */   private static final int FLIP_VERT = 4;
/*     */   private static final int SCROLL_UP = 5;
/*     */   private static final int SCROLL_DOWN = 6;
/*     */   private static final int SCROLL_LEFT = 7;
/*     */   private static final int SCROLL_RIGHT = 8;
/*     */   private Ship ship;
/*     */   
/*     */   OperationsPanel()
/*     */   {
/*  28 */     GridBagLayout localGridBagLayout = new GridBagLayout();
/*  29 */     GridBagConstraints localGridBagConstraints = new GridBagConstraints();
/*  30 */     localGridBagConstraints.gridwidth = 0;
/*  31 */     setLayout(localGridBagLayout);
/*     */     
/*  33 */     Panel localPanel = new Panel();
/*  34 */     localPanel.setLayout(new GridLayout(0, 5, 3, 3));
/*  35 */     localPanel.add(newOperation(0, 17));
/*  36 */     localPanel.add(newOperation(1, 18));
/*  37 */     localPanel.add(newOperation(2, 19));
/*  38 */     localPanel.add(newOperation(3, 20));
/*  39 */     localPanel.add(newOperation(4, 21));
/*  40 */     localPanel.add(newOperation(5, 22));
/*  41 */     localPanel.add(newOperation(6, 23));
/*  42 */     localPanel.add(newOperation(7, 24));
/*  43 */     localPanel.add(newOperation(8, 25));
/*  44 */     BorderedPane localBorderedPane = new BorderedPane("Operations", localPanel);
/*  45 */     localGridBagLayout.setConstraints(localBorderedPane, localGridBagConstraints);
/*  46 */     add(localBorderedPane);
/*     */   }
/*     */   
/*     */   OperationsPanel(Ship paramShip)
/*     */   {
/*  51 */     this();
/*  52 */     setShip(paramShip);
/*     */   }
/*     */   
/*     */   private ClickImageButton newOperation(int paramInt1, int paramInt2)
/*     */   {
/*  57 */     ClickImageButton localClickImageButton = new ClickImageButton(String.valueOf(paramInt1), 
/*  58 */       Images.getImage(paramInt2));
/*  59 */     localClickImageButton.addActionListener(this);
/*  60 */     return localClickImageButton;
/*     */   }
/*     */   
/*     */   public void setShip(Ship paramShip)
/*     */   {
/*  65 */     this.ship = paramShip;
/*     */   }
/*     */   
/*     */   public void actionPerformed(ActionEvent paramActionEvent)
/*     */   {
/*  70 */     if (this.ship == null) {
/*  71 */       return;
/*     */     }
/*  73 */     int i = Integer.parseInt(paramActionEvent.getActionCommand());
/*     */     
/*  75 */     switch (i)
/*     */     {
/*     */     case 0: 
/*  78 */       this.ship.clear();
/*  79 */       break;
/*     */     case 1: 
/*  81 */       this.ship.rotate90(1);
/*  82 */       break;
/*     */     case 2: 
/*  84 */       this.ship.rotate90(-1);
/*  85 */       break;
/*     */     case 3: 
/*  87 */       this.ship.flip(1);
/*  88 */       break;
/*     */     case 4: 
/*  90 */       this.ship.flip(-1);
/*  91 */       break;
/*     */     case 5: 
/*  93 */       this.ship.scroll(0);
/*  94 */       break;
/*     */     case 6: 
/*  96 */       this.ship.scroll(1);
/*  97 */       break;
/*     */     case 7: 
/*  99 */       this.ship.scroll(2);
/* 100 */       break;
/*     */     case 8: 
/* 102 */       this.ship.scroll(3);
/* 103 */       break;
/*     */     }
/* 105 */     this.ship.shipChanged(new ShipEvent(this, true, true, false, true));
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\OperationsPanel.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */