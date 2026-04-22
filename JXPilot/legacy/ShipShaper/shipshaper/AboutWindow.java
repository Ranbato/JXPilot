/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Dimension;
/*     */ import java.awt.Graphics;
/*     */ import java.awt.event.WindowEvent;
/*     */ 
/*     */ public final class AboutWindow extends java.awt.Dialog implements java.awt.event.ActionListener, java.awt.event.WindowListener
/*     */ {
/*     */   public AboutWindow(java.awt.Frame paramFrame)
/*     */   {
/*  11 */     super(paramFrame, "About ShipShaper", true);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*  16 */     if (System.getProperty("os.name").indexOf("Windows") != -1) {
/*  17 */       setResizable(false);
/*     */     }
/*  19 */     setBackground(java.awt.Color.lightGray);
/*     */     
/*  21 */     addWindowListener(this);
/*     */     
/*  23 */     java.awt.GridBagLayout localGridBagLayout = new java.awt.GridBagLayout();
/*  24 */     java.awt.GridBagConstraints localGridBagConstraints = new java.awt.GridBagConstraints();
/*  25 */     localGridBagConstraints.gridwidth = 0;
/*  26 */     setLayout(localGridBagLayout);
/*     */     
/*  28 */     AboutCanvas localAboutCanvas = new AboutCanvas();
/*  29 */     localGridBagLayout.setConstraints(localAboutCanvas, localGridBagConstraints);
/*  30 */     add(localAboutCanvas);
/*     */     
/*  32 */     java.awt.Button localButton = new java.awt.Button("Great to know");
/*  33 */     localButton.addActionListener(this);
/*  34 */     localGridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
/*  35 */     localGridBagLayout.setConstraints(localButton, localGridBagConstraints);
/*  36 */     add(localButton);
/*     */     
/*  38 */     pack();
/*     */     
/*  40 */     java.awt.Point localPoint = paramFrame.getLocation();
/*  41 */     Dimension localDimension1 = paramFrame.getSize();
/*  42 */     Dimension localDimension2 = getSize();
/*  43 */     setLocation(localPoint.x + localDimension1.width / 2 - localDimension2.width / 2, 
/*  44 */       localPoint.y + localDimension1.height / 2 - localDimension2.height / 2);
/*  45 */     show();
/*     */   }
/*     */   
/*     */   public java.awt.Insets getInsets()
/*     */   {
/*  50 */     return new java.awt.Insets(30, 10, 10, 10);
/*     */   }
/*     */   
/*     */   public void actionPerformed(java.awt.event.ActionEvent paramActionEvent)
/*     */   {
/*  55 */     dispose();
/*     */   }
/*     */   
/*     */ 
/*     */   public void windowOpened(WindowEvent paramWindowEvent) {}
/*     */   
/*  61 */   public void windowClosing(WindowEvent paramWindowEvent) { dispose(); }
/*     */   
/*     */   public void windowClosed(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowIconified(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowDeiconified(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowActivated(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowDeactivated(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   class AboutCanvas extends java.awt.Canvas {
/*     */     private java.awt.Image logo;
/*     */     private int w;
/*     */     
/*     */     public AboutCanvas() {
/*  78 */       this.logo = Images.getImage(0);
/*  79 */       this.w = this.logo.getWidth(this);
/*  80 */       this.h = this.logo.getHeight(this);
/*     */       
/*  82 */       setBackground(java.awt.Color.white);
/*     */       
/*  84 */       this.font = new java.awt.Font("TimesRoman", 0, 12);
/*  85 */       this.fm = getFontMetrics(this.font);
/*     */     }
/*     */     
/*     */     public Dimension getPreferredSize()
/*     */     {
/*  90 */       return new Dimension(this.w, this.h + 20);
/*     */     }
/*     */     
/*     */     public Dimension getMinimumSize()
/*     */     {
/*  95 */       return getPreferredSize();
/*     */     }
/*     */     
/*     */     private int h;
/*     */     private java.awt.Font font;
/*     */     private java.awt.FontMetrics fm;
/*     */     public void paint(Graphics paramGraphics) {
/* 102 */       paramGraphics.drawImage(this.logo, 0, 0, this);
/* 103 */       paramGraphics.setColor(java.awt.Color.lightGray);
/* 104 */       paramGraphics.draw3DRect(0, 0, this.w, this.h + 20, false);
/* 105 */       paramGraphics.draw3DRect(1, 1, this.w - 2, this.h + 20 - 2, false);
/*     */       
/* 107 */       paramGraphics.setColor(java.awt.Color.black);
/* 108 */       paramGraphics.setFont(this.font);
/* 109 */       paramGraphics.drawString("Version " + String.valueOf(1.0D), 10, 86);
/*     */       
/* 111 */       int i = this.fm.stringWidth("(c) 1998 Jonny Svärling");
/* 112 */       paramGraphics.drawString("(c) 1998 Jonny Svärling", this.w - 20 - i, 86);
/*     */       
/* 114 */       String str = "a.k.a HeadAce/Gnilrävs";
/* 115 */       i = this.fm.stringWidth(str);
/* 116 */       paramGraphics.drawString(str, this.w - 20 - i, 86 + this.fm.getHeight());
/*     */       
/* 118 */       i = this.fm.stringWidth("d93-jsv@nada.kth.se");
/* 119 */       paramGraphics.drawString("d93-jsv@nada.kth.se", this.w - 20 - i, 86 + this.fm.getHeight() * 2);
/*     */     }
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\AboutWindow.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */