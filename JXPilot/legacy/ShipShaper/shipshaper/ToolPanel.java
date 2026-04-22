/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Component;
/*     */ import java.awt.Container;
/*     */ import java.awt.GridBagLayout;
/*     */ import java.awt.Label;
/*     */ import java.awt.Panel;
/*     */ import java.awt.TextComponent;
/*     */ import java.util.Vector;
/*     */ import shipshaper.tools.ItemTool;
/*     */ import shipshaper.tools.Tool;
/*     */ 
/*     */ public final class ToolPanel extends Panel implements java.awt.event.ActionListener, ShipListener
/*     */ {
/*     */   private Ship ship;
/*     */   private Vector tools;
/*     */   private Tool currentTool;
/*     */   private Vector buttons;
/*     */   private shipshaper.imagebuttons.RadioImageButtonGroup toolgroup;
/*     */   private int nTools;
/*     */   private DrawArea drawArea;
/*     */   private Label nameLabel;
/*     */   private Label numberOfLabel;
/*     */   private Label maxLabel;
/*     */   private java.awt.TextField numberOfField;
/*     */   private java.awt.TextField maxField;
/*     */   
/*     */   public ToolPanel(DrawArea paramDrawArea)
/*     */   {
/*  30 */     this.drawArea = paramDrawArea;
/*  31 */     this.tools = new Vector();
/*  32 */     this.toolgroup = new shipshaper.imagebuttons.RadioImageButtonGroup();
/*  33 */     this.buttons = new Vector();
/*  34 */     this.nTools = 0;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  40 */     Panel localPanel1 = new Panel();
/*  41 */     localPanel1.setLayout(new java.awt.GridLayout(2, 5, 3, 3));
/*  42 */     shipshaper.tools.PolyLineTool localPolyLineTool = new shipshaper.tools.PolyLineTool(paramDrawArea);
/*  43 */     shipshaper.imagebuttons.RadioImageButton localRadioImageButton = newTool(localPolyLineTool);
/*  44 */     localPanel1.add(localRadioImageButton);
/*  45 */     localPanel1.add(newTool(new ItemTool(paramDrawArea, 7)));
/*  46 */     localPanel1.add(newTool(new ItemTool(paramDrawArea, 8)));
/*  47 */     localPanel1.add(newTool(new ItemTool(paramDrawArea, 5)));
/*  48 */     localPanel1.add(newTool(new ItemTool(paramDrawArea, 6)));
/*  49 */     localPanel1.add(newTool(new ItemTool(paramDrawArea, 1)));
/*  50 */     localPanel1.add(newTool(new ItemTool(paramDrawArea, 0)));
/*  51 */     localPanel1.add(newTool(new ItemTool(paramDrawArea, 2)));
/*  52 */     localPanel1.add(newTool(new ItemTool(paramDrawArea, 3)));
/*  53 */     localPanel1.add(newTool(new ItemTool(paramDrawArea, 4)));
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  59 */     Panel localPanel2 = new Panel();
/*  60 */     localPanel2.setLayout(new java.awt.GridLayout(1, 3, 3, 3));
/*  61 */     localPanel2.add(newTool(new shipshaper.tools.MoveTool(paramDrawArea)));
/*  62 */     localPanel2.add(newTool(new shipshaper.tools.DeleteTool(paramDrawArea)));
/*  63 */     localPanel2.add(newTool(new shipshaper.tools.SplitTool(paramDrawArea)));
/*  64 */     localPanel2.add(newTool(new shipshaper.tools.JoinTool(paramDrawArea)));
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  71 */     this.nameLabel = new Label("");
/*  72 */     this.numberOfLabel = new Label("Number of");
/*  73 */     this.maxLabel = new Label("Max");
/*  74 */     this.numberOfField = new java.awt.TextField("", 7);
/*  75 */     this.numberOfField.setEditable(false);
/*  76 */     this.maxField = new java.awt.TextField("", 7);
/*  77 */     this.maxField.setEditable(false);
/*     */     
/*  79 */     Panel localPanel3 = new Panel();
/*  80 */     GridBagLayout localGridBagLayout1 = new GridBagLayout();
/*  81 */     java.awt.GridBagConstraints localGridBagConstraints1 = new java.awt.GridBagConstraints();
/*  82 */     localGridBagConstraints1.anchor = 17;
/*  83 */     localPanel3.setLayout(localGridBagLayout1);
/*     */     
/*  85 */     localGridBagConstraints1.gridwidth = 0;
/*  86 */     localGridBagConstraints1.fill = 2;
/*  87 */     localGridBagLayout1.setConstraints(this.nameLabel, localGridBagConstraints1);
/*  88 */     localPanel3.add(this.nameLabel);
/*     */     
/*  90 */     localGridBagConstraints1.fill = 0;
/*  91 */     localGridBagConstraints1.gridwidth = 1;
/*  92 */     localGridBagLayout1.setConstraints(this.numberOfLabel, localGridBagConstraints1);
/*  93 */     localPanel3.add(this.numberOfLabel);
/*     */     
/*  95 */     localGridBagConstraints1.gridwidth = 0;
/*  96 */     localGridBagLayout1.setConstraints(this.numberOfField, localGridBagConstraints1);
/*  97 */     localPanel3.add(this.numberOfField);
/*     */     
/*  99 */     localGridBagConstraints1.gridwidth = 1;
/* 100 */     localGridBagLayout1.setConstraints(this.maxLabel, localGridBagConstraints1);
/* 101 */     localPanel3.add(this.maxLabel);
/*     */     
/* 103 */     localGridBagConstraints1.gridwidth = 0;
/* 104 */     localGridBagLayout1.setConstraints(this.maxField, localGridBagConstraints1);
/* 105 */     localPanel3.add(this.maxField);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 111 */     GridBagLayout localGridBagLayout2 = new GridBagLayout();
/* 112 */     java.awt.GridBagConstraints localGridBagConstraints2 = new java.awt.GridBagConstraints();
/* 113 */     localGridBagConstraints2.gridwidth = 0;
/* 114 */     localGridBagConstraints2.anchor = 17;
/* 115 */     setLayout(localGridBagLayout2);
/*     */     
/* 117 */     Panel localPanel4 = new Panel();
/* 118 */     localPanel4.setLayout(localGridBagLayout2);
/* 119 */     localGridBagConstraints2.insets = new java.awt.Insets(0, 0, 5, 0);
/* 120 */     localGridBagLayout2.setConstraints(localPanel1, localGridBagConstraints2);
/* 121 */     localPanel4.add(localPanel1);
/*     */     
/* 123 */     localGridBagLayout2.setConstraints(localPanel2, localGridBagConstraints2);
/* 124 */     localPanel4.add(localPanel2);
/*     */     
/* 126 */     localGridBagConstraints2.insets = new java.awt.Insets(0, 0, 0, 0);
/* 127 */     localGridBagLayout2.setConstraints(localPanel3, localGridBagConstraints2);
/* 128 */     localPanel4.add(localPanel3);
/*     */     
/* 130 */     BorderedPane localBorderedPane = new BorderedPane("Tools", localPanel4);
/* 131 */     localGridBagLayout2.setConstraints(localBorderedPane, localGridBagConstraints2);
/* 132 */     add(localBorderedPane);
/*     */     
/* 134 */     paramDrawArea.setTool(localPolyLineTool);
/* 135 */     localRadioImageButton.setState(true);
/* 136 */     setCurrentTool(localPolyLineTool);
/*     */   }
/*     */   
/*     */ 
/*     */   public ToolPanel(DrawArea paramDrawArea, Ship paramShip)
/*     */   {
/* 142 */     this(paramDrawArea);
/* 143 */     setShip(paramShip);
/*     */   }
/*     */   
/*     */   public void setShip(Ship paramShip)
/*     */   {
/* 148 */     this.ship = paramShip;
/* 149 */     if (paramShip != null)
/* 150 */       paramShip.addShipListener(this);
/* 151 */     updateNumbers();
/*     */   }
/*     */   
/*     */   public Tool getCurrentTool()
/*     */   {
/* 156 */     return this.currentTool;
/*     */   }
/*     */   
/*     */   public void setCurrentTool(Tool paramTool)
/*     */   {
/* 161 */     this.currentTool = paramTool;
/* 162 */     this.drawArea.setTool(paramTool);
/* 163 */     this.nameLabel.setText(paramTool.getName());
/* 164 */     updateNumbers();
/*     */   }
/*     */   
/*     */   public void shipChanged(ShipEvent paramShipEvent)
/*     */   {
/* 169 */     updateNumbers();
/*     */   }
/*     */   
/*     */ 
/*     */   public void updateNumbers()
/*     */   {
/* 175 */     int j = 0;int k = 0;
/*     */     
/*     */     int i;
/* 178 */     if (this.ship == null) {
/* 179 */       i = 0;
/*     */     } else { int m;
/* 181 */       if ((this.currentTool instanceof ItemTool))
/*     */       {
/* 183 */         m = ((ItemTool)this.currentTool).getType();
/* 184 */         j = this.ship.items.countItemsOfType(m);
/* 185 */         k = Item.getMax(m);
/* 186 */         this.numberOfField.setText(String.valueOf(j));
/* 187 */         this.maxField.setText(String.valueOf(k));
/* 188 */         i = 1;
/*     */ 
/*     */       }
/* 191 */       else if ((this.currentTool instanceof shipshaper.tools.LineTool))
/*     */       {
/* 193 */         k = 24;
/*     */         
/* 195 */         j = this.ship.shape.countEdges();
/*     */         
/* 197 */         if (Preferences.calcTrueNumberOfEdges)
/*     */         {
/* 199 */           if (this.ship.shape.isConnected())
/*     */           {
/* 201 */             m = j;
/* 202 */             j = this.ship.shape.findEulerPathLength();
/* 203 */             this.numberOfField.setText(String.valueOf(m) + " (" + 
/* 204 */               String.valueOf(j) + ")");
/*     */           }
/*     */           else
/*     */           {
/* 208 */             this.numberOfField.setText(String.valueOf(j) + " (-)");
/*     */           }
/*     */           
/*     */         }
/*     */         else {
/* 213 */           this.numberOfField.setText(String.valueOf(j) + " (?)");
/*     */         }
/*     */         
/* 216 */         this.maxField.setText(String.valueOf(k));
/* 217 */         i = 1;
/*     */       }
/*     */       else {
/* 220 */         i = 0;
/*     */       } }
/* 222 */     if (i != 0)
/*     */     {
/* 224 */       if (j > k) {
/* 225 */         this.numberOfField.setBackground(java.awt.Color.red);
/*     */       } else {
/* 227 */         this.numberOfField.setBackground(java.awt.Color.lightGray);
/*     */       }
/*     */     }
/* 230 */     if ((i != 0) && (!this.numberOfField.isVisible()))
/*     */     {
/* 232 */       this.numberOfLabel.setVisible(true);
/* 233 */       this.numberOfField.setVisible(true);
/* 234 */       this.maxLabel.setVisible(true);
/* 235 */       this.maxField.setVisible(true);
/*     */ 
/*     */     }
/* 238 */     else if ((i == 0) && (this.numberOfField.isVisible()))
/*     */     {
/* 240 */       this.numberOfLabel.setVisible(false);
/* 241 */       this.numberOfField.setVisible(false);
/* 242 */       this.maxLabel.setVisible(false);
/* 243 */       this.maxField.setVisible(false);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public shipshaper.imagebuttons.RadioImageButton newTool(Tool paramTool)
/*     */   {
/* 250 */     shipshaper.imagebuttons.RadioImageButton localRadioImageButton = new shipshaper.imagebuttons.RadioImageButton(String.valueOf(this.nTools), 
/* 251 */       paramTool.getImage(), this.toolgroup);
/* 252 */     this.buttons.addElement(localRadioImageButton);
/* 253 */     localRadioImageButton.addActionListener(this);
/* 254 */     this.tools.addElement(paramTool);
/*     */     
/* 256 */     this.nTools += 1;
/* 257 */     return localRadioImageButton;
/*     */   }
/*     */   
/*     */   public void actionPerformed(java.awt.event.ActionEvent paramActionEvent)
/*     */   {
/* 262 */     int i = Integer.parseInt(paramActionEvent.getActionCommand());
/* 263 */     setCurrentTool((Tool)this.tools.elementAt(i));
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\ToolPanel.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */