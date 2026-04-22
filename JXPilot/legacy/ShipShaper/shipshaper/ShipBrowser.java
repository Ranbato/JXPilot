/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Container;
/*     */ import java.util.Vector;
/*     */ 
/*     */ public final class ShipBrowser extends java.awt.ScrollPane
/*     */ {
/*     */   private Vector ships;
/*     */   private Ship selectedShip;
/*     */   private ShipHolder selectedShipHolder;
/*     */   private ShipEditor editor;
/*     */   private java.awt.Panel hp;
/*     */   
/*     */   public ShipBrowser(ShipEditor paramShipEditor)
/*     */   {
/*  16 */     super(0);
/*  17 */     this.editor = paramShipEditor;
/*     */     
/*  19 */     this.hp = new java.awt.Panel();
/*  20 */     add(this.hp, 0);
/*     */     
/*     */ 
/*  23 */     this.hp.setLayout(new java.awt.FlowLayout(0));
/*  24 */     removeAllShips();
/*     */   }
/*     */   
/*     */   public java.awt.Dimension getPreferredSize()
/*     */   {
/*  29 */     java.awt.Dimension localDimension = super.getPreferredSize();
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*  34 */     return new java.awt.Dimension(localDimension.width, localDimension.height + 3);
/*     */   }
/*     */   
/*     */   public void addShips(Vector paramVector)
/*     */   {
/*  39 */     for (int i = 0; i < paramVector.size(); i++) {
/*  40 */       addShip((Ship)paramVector.elementAt(i));
/*     */     }
/*     */   }
/*     */   
/*     */   public void removeAllShips() {
/*  45 */     this.hp.removeAll();
/*  46 */     this.ships = new Vector();
/*  47 */     this.selectedShip = null;
/*  48 */     this.selectedShipHolder = null;
/*  49 */     this.editor.shipSelected(null);
/*     */   }
/*     */   
/*     */   public void removeShip(Ship paramShip)
/*     */   {
/*  54 */     int i = 0;
/*     */     
/*  56 */     if ((paramShip == null) || (!this.ships.contains(paramShip))) {
/*  57 */       return;
/*     */     }
/*  59 */     if (paramShip == this.selectedShip) {
/*  60 */       i = this.ships.indexOf(paramShip);
/*     */     }
/*  62 */     this.ships.removeElement(paramShip);
/*  63 */     this.hp.remove(findShipHolderFor(paramShip));
/*     */     
/*  65 */     if ((paramShip == this.selectedShip) && (this.ships.size() > 0))
/*     */     {
/*     */ 
/*     */ 
/*     */ 
/*  70 */       if (i > 0) {
/*  71 */         i--;
/*     */       }
/*  73 */       selectShip(i);
/*     */     }
/*     */     else
/*     */     {
/*  77 */       this.selectedShip = null;
/*  78 */       this.selectedShipHolder = null;
/*  79 */       this.editor.shipSelected(null);
/*     */     }
/*  81 */     validate();
/*  82 */     if (getParent().isValid()) {
/*  83 */       getParent().invalidate();
/*     */     }
/*     */   }
/*     */   
/*     */   public Vector getShips() {
/*  88 */     return this.ships;
/*     */   }
/*     */   
/*     */   public Ship getShip(int paramInt)
/*     */   {
/*  93 */     return (Ship)this.ships.elementAt(paramInt);
/*     */   }
/*     */   
/*     */   public int countShips()
/*     */   {
/*  98 */     return this.ships.size();
/*     */   }
/*     */   
/*     */ 
/*     */   public void addShip(Ship paramShip)
/*     */   {
/* 104 */     ShipHolder localShipHolder = new ShipHolder(this, paramShip);
/*     */     
/*     */ 
/* 107 */     if (this.selectedShip == null)
/*     */     {
/* 109 */       this.ships.addElement(paramShip);
/* 110 */       this.hp.add(localShipHolder);
/*     */     }
/*     */     else
/*     */     {
/* 114 */       for (int i = 0; i < this.hp.getComponentCount(); i++)
/* 115 */         if (this.hp.getComponent(i) == this.selectedShipHolder)
/* 116 */           this.hp.add(localShipHolder, i);
/* 117 */       this.ships.insertElementAt(paramShip, this.ships.indexOf(this.selectedShip));
/*     */     }
/*     */     
/* 120 */     validate();
/* 121 */     if (getParent().isValid()) {
/* 122 */       getParent().invalidate();
/*     */     }
/*     */   }
/*     */   
/*     */   public Ship getSelectedShip() {
/* 127 */     return this.selectedShip;
/*     */   }
/*     */   
/*     */   public void selectShip(int paramInt)
/*     */   {
/* 132 */     selectShip((Ship)this.ships.elementAt(paramInt));
/*     */   }
/*     */   
/*     */   public void selectShip(Ship paramShip)
/*     */   {
/* 137 */     if (this.selectedShip == paramShip) {
/* 138 */       return;
/*     */     }
/* 140 */     this.selectedShip = paramShip;
/*     */     
/*     */ 
/* 143 */     selectShipHolder(findShipHolderFor(paramShip));
/*     */     
/*     */ 
/* 146 */     this.editor.shipSelected(paramShip);
/*     */   }
/*     */   
/*     */   public void selectShipHolder(ShipHolder paramShipHolder)
/*     */   {
/* 151 */     if (this.selectedShipHolder == paramShipHolder) {
/* 152 */       return;
/*     */     }
/* 154 */     if (this.selectedShipHolder != null) {
/* 155 */       this.selectedShipHolder.unselect();
/*     */     }
/* 157 */     this.selectedShipHolder = paramShipHolder;
/* 158 */     paramShipHolder.select();
/*     */     
/* 160 */     if (paramShipHolder.getShip() != this.selectedShip) {
/* 161 */       selectShip(paramShipHolder.getShip());
/*     */     }
/*     */   }
/*     */   
/*     */   public ShipHolder findShipHolderFor(Ship paramShip)
/*     */   {
/* 167 */     java.awt.Component[] arrayOfComponent = this.hp.getComponents();
/* 168 */     for (int i = 0; i < arrayOfComponent.length; i++)
/*     */     {
/* 170 */       ShipHolder localShipHolder = (ShipHolder)arrayOfComponent[i];
/* 171 */       if (localShipHolder.getShip() == paramShip)
/* 172 */         return localShipHolder;
/*     */     }
/* 174 */     return null;
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\ShipBrowser.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */