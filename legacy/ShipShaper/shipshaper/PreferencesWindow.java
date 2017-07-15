/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Checkbox;
/*     */ import java.awt.Container;
/*     */ import java.awt.GridBagLayout;
/*     */ import java.awt.event.WindowEvent;
/*     */ 
/*     */ public final class PreferencesWindow extends java.awt.Dialog implements java.awt.event.ActionListener, java.awt.event.ItemListener, java.awt.event.WindowListener
/*     */ {
/*     */   java.awt.Frame parent;
/*     */   Checkbox verticalDraw;
/*     */   Checkbox calcTrueNumberOfEdges;
/*     */   Checkbox showMirrorMarker;
/*     */   Checkbox checkConnected;
/*     */   Checkbox checkMinMax;
/*     */   Checkbox checkGeometry;
/*     */   Checkbox markSelectedShip;
/*     */   Checkbox saveEvenIfErrors;
/*     */   java.awt.TextField startupField;
/*     */   java.awt.Button closeButton;
/*     */   java.awt.Button browseButton;
/*     */   
/*     */   PreferencesWindow(java.awt.Frame paramFrame)
/*     */   {
/*  25 */     super(paramFrame, "ShipShaper Preferences");
/*  26 */     setModal(true);
/*  27 */     this.parent = paramFrame;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*  32 */     if (System.getProperty("os.name").indexOf("Windows") != -1) {
/*  33 */       setResizable(false);
/*     */     }
/*  35 */     addWindowListener(this);
/*     */     
/*     */ 
/*  38 */     this.verticalDraw = new Checkbox("Nose facing up", Preferences.verticalDraw);
/*  39 */     this.verticalDraw.addItemListener(this);
/*     */     
/*  41 */     this.calcTrueNumberOfEdges = new Checkbox("Show real number of edges (can slow down)", 
/*  42 */       Preferences.calcTrueNumberOfEdges);
/*  43 */     this.calcTrueNumberOfEdges.addItemListener(this);
/*     */     
/*  45 */     this.showMirrorMarker = new Checkbox("Show mirror marker", 
/*  46 */       Preferences.showMirrorMarker);
/*  47 */     this.showMirrorMarker.addItemListener(this);
/*     */     
/*  49 */     this.checkConnected = new Checkbox("Check that ship is fully connected", 
/*  50 */       Preferences.checkConnected);
/*  51 */     this.checkConnected.addItemListener(this);
/*  52 */     this.checkMinMax = new Checkbox("Check min and max", 
/*  53 */       Preferences.checkMinMax);
/*  54 */     this.checkMinMax.addItemListener(this);
/*  55 */     this.checkGeometry = new Checkbox("Check ship geometry", 
/*  56 */       Preferences.checkGeometry);
/*  57 */     this.checkGeometry.addItemListener(this);
/*  58 */     this.markSelectedShip = new Checkbox("Prefix selected ship with xpilot.shipShape:", 
/*  59 */       Preferences.markSelectedShip);
/*  60 */     this.markSelectedShip.addItemListener(this);
/*  61 */     this.saveEvenIfErrors = new Checkbox("Save ship even if errors are found", 
/*  62 */       Preferences.saveEvenIfErrors);
/*  63 */     this.saveEvenIfErrors.addItemListener(this);
/*     */     
/*  65 */     this.startupField = new java.awt.TextField(Preferences.startupFile, 20);
/*     */     
/*     */ 
/*  68 */     GridBagLayout localGridBagLayout1 = new GridBagLayout();
/*  69 */     java.awt.GridBagConstraints localGridBagConstraints1 = new java.awt.GridBagConstraints();
/*  70 */     localGridBagConstraints1.gridwidth = 0;
/*  71 */     localGridBagConstraints1.fill = 2;
/*  72 */     localGridBagConstraints1.insets = new java.awt.Insets(20, 0, 0, 0);
/*  73 */     setLayout(localGridBagLayout1);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  82 */     java.awt.Panel localPanel = new java.awt.Panel();
/*  83 */     GridBagLayout localGridBagLayout2 = new GridBagLayout();
/*  84 */     java.awt.GridBagConstraints localGridBagConstraints2 = new java.awt.GridBagConstraints();
/*  85 */     localGridBagConstraints2.anchor = 17;
/*  86 */     localPanel.setLayout(localGridBagLayout2);
/*     */     
/*  88 */     java.awt.Label localLabel = new java.awt.Label("Startup file");
/*  89 */     localGridBagConstraints2.gridwidth = 1;
/*  90 */     localGridBagLayout2.setConstraints(localLabel, localGridBagConstraints2);
/*  91 */     localPanel.add(localLabel);
/*     */     
/*  93 */     localGridBagLayout2.setConstraints(this.startupField, localGridBagConstraints2);
/*  94 */     localPanel.add(this.startupField);
/*     */     
/*  96 */     this.browseButton = new java.awt.Button("Browse...");
/*  97 */     this.browseButton.addActionListener(this);
/*  98 */     localGridBagConstraints2.gridwidth = 0;
/*  99 */     localGridBagLayout2.setConstraints(this.browseButton, localGridBagConstraints2);
/* 100 */     localPanel.add(this.browseButton);
/*     */     
/* 102 */     BorderedPane localBorderedPane = new BorderedPane("On startup", localPanel, 0);
/* 103 */     localGridBagLayout1.setConstraints(localBorderedPane, localGridBagConstraints1);
/* 104 */     add(localBorderedPane);
/*     */     
/*     */ 
/* 107 */     localPanel = new java.awt.Panel();
/* 108 */     localGridBagLayout2 = new GridBagLayout();
/* 109 */     localGridBagConstraints2 = new java.awt.GridBagConstraints();
/* 110 */     localGridBagConstraints2.anchor = 17;
/* 111 */     localGridBagConstraints2.gridwidth = 0;
/* 112 */     localPanel.setLayout(localGridBagLayout2);
/*     */     
/* 114 */     localGridBagLayout2.setConstraints(this.verticalDraw, localGridBagConstraints2);
/* 115 */     localPanel.add(this.verticalDraw);
/*     */     
/* 117 */     localGridBagLayout2.setConstraints(this.calcTrueNumberOfEdges, localGridBagConstraints2);
/* 118 */     localPanel.add(this.calcTrueNumberOfEdges);
/*     */     
/* 120 */     localGridBagLayout2.setConstraints(this.showMirrorMarker, localGridBagConstraints2);
/* 121 */     localPanel.add(this.showMirrorMarker);
/*     */     
/* 123 */     localBorderedPane = new BorderedPane("Drawing settings", localPanel, 0);
/* 124 */     localGridBagLayout1.setConstraints(localBorderedPane, localGridBagConstraints1);
/* 125 */     add(localBorderedPane);
/*     */     
/*     */ 
/* 128 */     localPanel = new java.awt.Panel();
/* 129 */     localGridBagLayout2 = new GridBagLayout();
/* 130 */     localGridBagConstraints2 = new java.awt.GridBagConstraints();
/* 131 */     localGridBagConstraints2.gridwidth = 0;
/* 132 */     localGridBagConstraints2.anchor = 17;
/* 133 */     localPanel.setLayout(localGridBagLayout2);
/*     */     
/* 135 */     localGridBagLayout2.setConstraints(this.checkConnected, localGridBagConstraints2);
/* 136 */     localPanel.add(this.checkConnected);
/* 137 */     localGridBagLayout2.setConstraints(this.checkMinMax, localGridBagConstraints2);
/* 138 */     localPanel.add(this.checkMinMax);
/* 139 */     localGridBagLayout2.setConstraints(this.checkGeometry, localGridBagConstraints2);
/* 140 */     localPanel.add(this.checkGeometry);
/* 141 */     localGridBagLayout2.setConstraints(this.markSelectedShip, localGridBagConstraints2);
/* 142 */     localPanel.add(this.markSelectedShip);
/* 143 */     localGridBagLayout2.setConstraints(this.saveEvenIfErrors, localGridBagConstraints2);
/* 144 */     localPanel.add(this.saveEvenIfErrors);
/*     */     
/* 146 */     localBorderedPane = new BorderedPane("When saving", localPanel, 0);
/* 147 */     localGridBagLayout1.setConstraints(localBorderedPane, localGridBagConstraints1);
/* 148 */     add(localBorderedPane);
/*     */     
/* 150 */     java.awt.Button localButton = new java.awt.Button("Close");
/* 151 */     localGridBagConstraints1.fill = 0;
/* 152 */     localGridBagConstraints1.insets = new java.awt.Insets(20, 0, 0, 0);
/* 153 */     localGridBagLayout1.setConstraints(localButton, localGridBagConstraints1);
/* 154 */     add(localButton);
/* 155 */     localButton.addActionListener(this);
/*     */     
/* 157 */     pack();
/* 158 */     show();
/*     */   }
/*     */   
/*     */   public java.awt.Insets getInsets()
/*     */   {
/* 163 */     return new java.awt.Insets(20, 10, 10, 10);
/*     */   }
/*     */   
/*     */ 
/*     */   public void close()
/*     */   {
/* 169 */     Preferences.startupFile = this.startupField.getText();
/*     */     
/*     */ 
/* 172 */     dispose();
/* 173 */     Preferences.save();
/*     */   }
/*     */   
/*     */   public void actionPerformed(java.awt.event.ActionEvent paramActionEvent)
/*     */   {
/* 178 */     if (paramActionEvent.getSource() == this.browseButton)
/*     */     {
/* 180 */       java.awt.FileDialog localFileDialog = new java.awt.FileDialog(this.parent, 
/* 181 */         "Select startup shipshape file", 
/* 182 */         0);
/* 183 */       localFileDialog.setModal(true);
/* 184 */       localFileDialog.show();
/* 185 */       if ((localFileDialog.getDirectory() != null) && (localFileDialog.getFile() != null)) {
/* 186 */         this.startupField.setText(localFileDialog.getDirectory() + localFileDialog.getFile());
/*     */       }
/*     */     }
/*     */     else {
/* 190 */       close();
/*     */     }
/*     */   }
/*     */   
/*     */   public void itemStateChanged(java.awt.event.ItemEvent paramItemEvent)
/*     */   {
/* 196 */     java.awt.ItemSelectable localItemSelectable = paramItemEvent.getItemSelectable();
/*     */     
/* 198 */     if ((localItemSelectable instanceof Checkbox))
/*     */     {
/* 200 */       Checkbox localCheckbox = (Checkbox)localItemSelectable;
/*     */       
/*     */       boolean bool;
/* 203 */       if (paramItemEvent.getStateChange() == 1) {
/* 204 */         bool = true;
/*     */       } else {
/* 206 */         bool = false;
/*     */       }
/* 208 */       if (localCheckbox == this.checkConnected)
/* 209 */         Preferences.checkConnected = bool;
/* 210 */       if (localCheckbox == this.checkMinMax)
/* 211 */         Preferences.checkMinMax = bool;
/* 212 */       if (localCheckbox == this.checkGeometry)
/* 213 */         Preferences.checkGeometry = bool;
/* 214 */       if (localCheckbox == this.verticalDraw)
/* 215 */         Preferences.verticalDraw = bool;
/* 216 */       if (localCheckbox == this.markSelectedShip)
/* 217 */         Preferences.markSelectedShip = bool;
/* 218 */       if (localCheckbox == this.saveEvenIfErrors)
/* 219 */         Preferences.saveEvenIfErrors = bool;
/* 220 */       if (localCheckbox == this.calcTrueNumberOfEdges)
/* 221 */         Preferences.calcTrueNumberOfEdges = bool;
/* 222 */       if (localCheckbox == this.showMirrorMarker)
/* 223 */         Preferences.showMirrorMarker = bool;
/*     */     }
/*     */   }
/*     */   
/*     */   public void windowOpened(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowClosing(WindowEvent paramWindowEvent) {
/* 230 */     close();
/*     */   }
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
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\PreferencesWindow.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */