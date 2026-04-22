/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Component;
/*     */ import java.awt.event.MouseEvent;
/*     */ import shipshaper.graph.Edge;
/*     */ import shipshaper.graph.Vertex;
/*     */ 
/*     */ public final class ShipView extends java.awt.Canvas implements Runnable, java.awt.event.MouseListener, ShipListener
/*     */ {
/*     */   static java.awt.Image offImage;
/*     */   static java.awt.Graphics offG;
/*     */   static ShipView offOwner;
/*     */   Ship ship;
/*     */   double angle;
/*     */   java.awt.Dimension sz;
/*     */   int xc;
/*     */   int yc;
/*     */   Thread thread;
/*     */   
/*     */   public ShipView()
/*     */   {
/*  22 */     setAngle(0);
/*  23 */     setSize(getPreferredSize());
/*     */     
/*  25 */     setBackground(Colors.SHIPVIEWER_BG);
/*  26 */     setForeground(Colors.SHIPVIEWER_FG);
/*     */     
/*  28 */     addMouseListener(this);
/*     */   }
/*     */   
/*     */   public ShipView(Ship paramShip)
/*     */   {
/*  33 */     this();
/*  34 */     setShip(paramShip);
/*     */   }
/*     */   
/*     */   public void setShip(Ship paramShip)
/*     */   {
/*  39 */     this.ship = paramShip;
/*  40 */     if (paramShip != null)
/*  41 */       paramShip.addShipListener(this);
/*  42 */     repaint();
/*     */   }
/*     */   
/*     */   public java.awt.Dimension getPreferredSize()
/*     */   {
/*  47 */     return new java.awt.Dimension(50, 50);
/*     */   }
/*     */   
/*     */   public java.awt.Dimension getMinimumSize()
/*     */   {
/*  52 */     return getPreferredSize();
/*     */   }
/*     */   
/*     */   public int radianToDegree(double paramDouble)
/*     */   {
/*  57 */     return (int)(paramDouble * 180.0D / 3.141592653589793D);
/*     */   }
/*     */   
/*     */   public double degreeToRadian(int paramInt)
/*     */   {
/*  62 */     return paramInt * 3.141592653589793D / 180.0D;
/*     */   }
/*     */   
/*     */   public void setAngle(int paramInt)
/*     */   {
/*  67 */     this.angle = degreeToRadian(paramInt);
/*     */   }
/*     */   
/*     */   public int getAngle()
/*     */   {
/*  72 */     return radianToDegree(this.angle);
/*     */   }
/*     */   
/*     */   public void shipChanged(ShipEvent paramShipEvent)
/*     */   {
/*  77 */     if (paramShipEvent.shapeChanged())
/*  78 */       repaint(); }
/*     */   
/*     */   public void mouseClicked(MouseEvent paramMouseEvent) {}
/*     */   
/*     */   public void mousePressed(MouseEvent paramMouseEvent) {}
/*     */   
/*     */   public void mouseReleased(MouseEvent paramMouseEvent) {}
/*     */   
/*     */   public void mouseMoved(MouseEvent paramMouseEvent) {}
/*     */   
/*     */   public void mouseDragged(MouseEvent paramMouseEvent) {}
/*  89 */   public void mouseEntered(MouseEvent paramMouseEvent) { startRotating(); }
/*     */   
/*     */ 
/*     */   public void mouseExited(MouseEvent paramMouseEvent)
/*     */   {
/*  94 */     stopRotating();
/*  95 */     setAngle(0);
/*  96 */     repaint();
/*     */   }
/*     */   
/*     */   public void startRotating()
/*     */   {
/* 101 */     if (this.ship == null)
/* 102 */       return;
/* 103 */     if (this.thread == null)
/*     */     {
/* 105 */       this.thread = new Thread(this);
/* 106 */       this.thread.start();
/*     */     }
/*     */   }
/*     */   
/*     */   public void stopRotating()
/*     */   {
/* 112 */     if (this.thread != null)
/*     */     {
/* 114 */       this.thread.stop();
/* 115 */       this.thread = null;
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void run()
/*     */   {
/* 123 */     int i = 0;
/*     */     for (;;)
/*     */     {
/* 126 */       setAngle(360 - i);
/* 127 */       repaint();
/* 128 */       try { Thread.sleep(100L);
/*     */       } catch (InterruptedException localInterruptedException) {}
/* 130 */       i = (i + 20) % 360;
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void update(java.awt.Graphics paramGraphics)
/*     */   {
/* 141 */     if ((offImage == null) || (this.sz == null) || (!this.sz.equals(getSize())))
/*     */     {
/* 143 */       this.sz = getSize();
/* 144 */       this.xc = (this.sz.width / 2 - 1);
/* 145 */       this.yc = (this.sz.height / 2 - 1);
/*     */       
/* 147 */       offImage = createImage(this.sz.width, this.sz.height);
/* 148 */       offG = offImage.getGraphics();
/*     */     }
/*     */     
/* 151 */     offG.setColor(getBackground());
/* 152 */     offG.fillRect(0, 0, this.sz.width, this.sz.height);
/* 153 */     offG.setColor(getForeground());
/*     */     
/* 155 */     if (this.ship != null)
/*     */     {
/* 157 */       for (int i = 0; i < this.ship.shape.countEdges(); i++)
/*     */       {
/* 159 */         Edge localEdge = this.ship.shape.edge(i);
/* 160 */         double d1 = Math.sqrt(localEdge.v1.x * localEdge.v1.x + localEdge.v1.y * localEdge.v1.y);
/* 161 */         double d2 = Math.sqrt(localEdge.v2.x * localEdge.v2.x + localEdge.v2.y * localEdge.v2.y);
/* 162 */         double d3 = Math.atan2(localEdge.v1.y, localEdge.v1.x) - this.angle;
/* 163 */         double d4 = Math.atan2(localEdge.v2.y, localEdge.v2.x) - this.angle;
/*     */         
/* 165 */         offG.drawLine(this.xc + (int)(d1 * Math.cos(d3) + 0.5D), 
/* 166 */           this.yc + (int)(d1 * Math.sin(d3) + 0.5D), 
/* 167 */           this.xc + (int)(d2 * Math.cos(d4) + 0.5D), 
/* 168 */           this.yc + (int)(d2 * Math.sin(d4) + 0.5D));
/*     */       }
/*     */     }
/*     */     
/* 172 */     offOwner = this;
/*     */     
/* 174 */     paint(paramGraphics);
/*     */   }
/*     */   
/*     */   public void paint(java.awt.Graphics paramGraphics)
/*     */   {
/* 179 */     if ((offImage == null) || (offOwner != this)) {
/* 180 */       repaint();
/*     */     } else {
/* 182 */       paramGraphics.drawImage(offImage, 0, 0, this);
/*     */     }
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\ShipView.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */