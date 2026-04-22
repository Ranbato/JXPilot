/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Container;
/*     */ import java.awt.FileDialog;
/*     */ import java.awt.Menu;
/*     */ import java.awt.MenuItem;
/*     */ import java.awt.event.WindowEvent;
/*     */ import java.util.Vector;
/*     */ 
/*     */ public final class ShipEditor extends java.awt.Frame implements java.awt.event.ActionListener, java.awt.event.WindowListener
/*     */ {
/*  12 */   private boolean DEBUG = false;
/*     */   
/*     */   String currentFile;
/*     */   
/*     */   Ship copiedShip;
/*     */   
/*     */   Vector fileContents;
/*     */   
/*     */   private DrawArea drawArea;
/*     */   
/*     */   private NameAndAuthorPanel na;
/*     */   
/*     */   private OperationsPanel op;
/*     */   
/*     */   private PositionView pv;
/*     */   
/*     */   private ToolPanel tp;
/*     */   
/*     */   private ShipView sv;
/*     */   private ShipBrowser browser;
/*     */   private Menu menuFile;
/*     */   private Menu menuShip;
/*     */   private Menu menuHelp;
/*     */   private MenuItem miNew;
/*     */   private MenuItem miOpen;
/*     */   private MenuItem miSave;
/*     */   private MenuItem miSaveAs;
/*     */   private MenuItem miPreferences;
/*     */   private MenuItem miExit;
/*     */   
/*     */   public ShipEditor()
/*     */   {
/*  44 */     super("ShipEditor");
/*     */     
/*  46 */     this.DEBUG = ShipShaper.DEBUG;
/*  47 */     setSize(698, 700);
/*     */     
/*  49 */     setFont(new java.awt.Font("TimesRoman", 0, 12));
/*  50 */     setBackground(java.awt.Color.lightGray);
/*  51 */     setMenuBar(createMenuBar());
/*  52 */     setIconImage(Images.getImage(2));
/*     */     
/*  54 */     java.awt.GridBagLayout localGridBagLayout = new java.awt.GridBagLayout();
/*  55 */     java.awt.GridBagConstraints localGridBagConstraints = new java.awt.GridBagConstraints();
/*  56 */     localGridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
/*  57 */     setLayout(localGridBagLayout);
/*     */     
/*  59 */     this.pv = new PositionView(false);
/*     */     
/*  61 */     this.drawArea = new DrawArea(this, this.pv);
/*  62 */     localGridBagConstraints.fill = 1;
/*  63 */     localGridBagConstraints.gridwidth = 1;
/*  64 */     localGridBagConstraints.gridheight = 5;
/*  65 */     localGridBagConstraints.weightx = 1.0D;
/*  66 */     localGridBagConstraints.weighty = 1.0D;
/*  67 */     localGridBagLayout.setConstraints(this.drawArea, localGridBagConstraints);
/*  68 */     add(this.drawArea);
/*     */     
/*     */ 
/*  71 */     localGridBagConstraints.gridwidth = 0;
/*  72 */     localGridBagConstraints.fill = 2;
/*  73 */     localGridBagConstraints.gridheight = 1;
/*  74 */     localGridBagConstraints.weightx = 0.0D;
/*  75 */     localGridBagConstraints.weighty = 0.0D;
/*     */     
/*  77 */     ImageHolder localImageHolder = new ImageHolder(Images.getImage(1));
/*  78 */     localGridBagLayout.setConstraints(localImageHolder, localGridBagConstraints);
/*  79 */     add(localImageHolder);
/*     */     
/*  81 */     this.na = new NameAndAuthorPanel();
/*  82 */     localGridBagLayout.setConstraints(this.na, localGridBagConstraints);
/*  83 */     add(this.na);
/*     */     
/*  85 */     this.tp = new ToolPanel(this.drawArea);
/*  86 */     localGridBagLayout.setConstraints(this.tp, localGridBagConstraints);
/*  87 */     add(this.tp);
/*     */     
/*  89 */     this.op = new OperationsPanel();
/*  90 */     localGridBagLayout.setConstraints(this.op, localGridBagConstraints);
/*  91 */     add(this.op);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 101 */     localGridBagConstraints.fill = 0;
/* 102 */     localGridBagConstraints.gridwidth = 1;
/* 103 */     localGridBagLayout.setConstraints(this.pv, localGridBagConstraints);
/* 104 */     add(this.pv);
/*     */     
/* 106 */     this.sv = new ShipView();
/* 107 */     localGridBagConstraints.gridwidth = 0;
/* 108 */     localGridBagLayout.setConstraints(this.sv, localGridBagConstraints);
/* 109 */     add(this.sv);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 119 */     this.browser = new ShipBrowser(this);
/* 120 */     localGridBagConstraints.gridwidth = 0;
/* 121 */     localGridBagConstraints.fill = 2;
/* 122 */     localGridBagLayout.setConstraints(this.browser, localGridBagConstraints);
/* 123 */     add(this.browser);
/*     */     
/* 125 */     addWindowListener(this);
/* 126 */     show();
/*     */     
/* 128 */     if (!Preferences.startupFile.equals("")) {
/* 129 */       loadFile(Preferences.startupFile, false);
/*     */     } else
/* 131 */       newFile(false);
/* 132 */     validate();
/*     */   }
/*     */   
/*     */   public void shipSelected(Ship paramShip)
/*     */   {
/* 137 */     debug("Ship Changed: " + paramShip);
/* 138 */     this.drawArea.setShip(paramShip);
/* 139 */     this.na.setShip(paramShip);
/* 140 */     this.op.setShip(paramShip);
/* 141 */     this.tp.setShip(paramShip);
/* 142 */     this.sv.setShip(paramShip);
/*     */   }
/*     */   
/*     */ 
/*     */   public void windowOpened(WindowEvent paramWindowEvent) {}
/*     */   
/* 148 */   public void windowClosing(WindowEvent paramWindowEvent) { quit(); }
/*     */   
/*     */   public void windowClosed(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowIconified(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowDeiconified(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowActivated(WindowEvent paramWindowEvent) {}
/*     */   public void windowDeactivated(WindowEvent paramWindowEvent) {}
/* 158 */   private java.awt.MenuBar createMenuBar() { java.awt.MenuBar localMenuBar = new java.awt.MenuBar();
/*     */     
/* 160 */     this.menuFile = new Menu("File");
/* 161 */     this.miNew = addMenuItem(this.menuFile, "New");
/* 162 */     this.miOpen = addMenuItem(this.menuFile, "Open");
/* 163 */     this.miSave = addMenuItem(this.menuFile, "Save");
/* 164 */     this.miSaveAs = addMenuItem(this.menuFile, "Save As...");
/* 165 */     this.menuFile.add(new MenuItem("-"));
/* 166 */     this.miPreferences = addMenuItem(this.menuFile, "Preferences...");
/* 167 */     this.menuFile.add(new MenuItem("-"));
/* 168 */     this.miExit = addMenuItem(this.menuFile, "Exit");
/* 169 */     localMenuBar.add(this.menuFile);
/*     */     
/* 171 */     this.menuShip = new Menu("Ship");
/* 172 */     addMenuItem(this.menuShip, "New");
/* 173 */     addMenuItem(this.menuShip, "Copy");
/* 174 */     addMenuItem(this.menuShip, "Cut");
/* 175 */     addMenuItem(this.menuShip, "Paste");
/* 176 */     this.menuShip.add(new MenuItem("-"));
/* 177 */     addMenuItem(this.menuShip, "Delete");
/* 178 */     this.menuShip.add(new MenuItem("-"));
/* 179 */     addMenuItem(this.menuShip, "View definition string");
/* 180 */     localMenuBar.add(this.menuShip);
/*     */     
/* 182 */     this.menuHelp = new Menu("Help");
/* 183 */     addMenuItem(this.menuHelp, "About");
/* 184 */     addMenuItem(this.menuHelp, "Help");
/* 185 */     localMenuBar.add(this.menuHelp);
/* 186 */     localMenuBar.setHelpMenu(this.menuHelp);
/*     */     
/* 188 */     return localMenuBar;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private MenuItem addMenuItem(Menu paramMenu, String paramString)
/*     */   {
/* 195 */     MenuItem localMenuItem = new MenuItem(paramString);
/* 196 */     localMenuItem.addActionListener(this);
/* 197 */     localMenuItem.setActionCommand(paramMenu.getLabel() + ">" + paramString);
/* 198 */     paramMenu.add(localMenuItem);
/*     */     
/* 200 */     return localMenuItem;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void actionPerformed(java.awt.event.ActionEvent paramActionEvent)
/*     */   {
/* 207 */     String str1 = paramActionEvent.getActionCommand();
/*     */     
/*     */ 
/*     */ 
/* 211 */     String str2 = str1.substring(0, str1.indexOf(">"));
/* 212 */     String str3 = str1.substring(str1.indexOf(">") + 1);
/*     */     
/* 214 */     if (str2.equals("File"))
/*     */     {
/* 216 */       if (str3.equals("New"))
/* 217 */         newFile(true);
/* 218 */       if (str3.equals("Open"))
/* 219 */         loadFile(true);
/* 220 */       if (str3.equals("Save"))
/* 221 */         saveFile();
/* 222 */       if (str3.equals("Save As..."))
/* 223 */         saveFileAs();
/* 224 */       if (str3.equals("Preferences..."))
/* 225 */         editPreferences();
/* 226 */       if (str3.equals("Exit")) {
/* 227 */         quit();
/*     */       }
/*     */     }
/* 230 */     else if (str2.equals("Ship"))
/*     */     {
/* 232 */       if (str3.equals("New"))
/* 233 */         newShip();
/* 234 */       if (str3.equals("Copy"))
/* 235 */         copyShip(this.browser.getSelectedShip());
/* 236 */       if (str3.equals("Cut"))
/* 237 */         cutShip(this.browser.getSelectedShip());
/* 238 */       if (str3.equals("Paste"))
/* 239 */         pasteShip();
/* 240 */       if (str3.equals("Delete"))
/* 241 */         deleteShip(this.browser.getSelectedShip());
/* 242 */       if (str3.equals("View definition string")) {
/* 243 */         viewDefinitionString(this.browser.getSelectedShip());
/*     */       }
/*     */     }
/* 246 */     else if (str2.equals("Help"))
/*     */     {
/* 248 */       if (str3.equals("About"))
/* 249 */         viewAbout();
/* 250 */       if (str3.equals("Help")) {
/* 251 */         viewHelp();
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public void quit() {
/* 257 */     if (!checkUnsavedChangesAndContinue()) {
/* 258 */       return;
/*     */     }
/* 260 */     dispose();
/* 261 */     System.exit(0);
/*     */   }
/*     */   
/*     */   public void editPreferences()
/*     */   {
/* 266 */     new PreferencesWindow(this);
/*     */     
/*     */ 
/*     */ 
/* 270 */     this.drawArea.repaintAll();
/* 271 */     this.tp.updateNumbers();
/*     */   }
/*     */   
/*     */   public boolean anyUnsavedChanges()
/*     */   {
/* 276 */     for (int i = 0; i < this.browser.countShips(); i++)
/* 277 */       if (this.browser.getShip(i).anyChanges())
/* 278 */         return true;
/* 279 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean checkUnsavedChangesAndContinue()
/*     */   {
/* 289 */     if (!anyUnsavedChanges()) {
/* 290 */       return true;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 297 */     shipshaper.dialogs.ThreeButtonDialog localThreeButtonDialog = new shipshaper.dialogs.ThreeButtonDialog(this, 
/* 298 */       "You have some unsaved changes.\n", 
/* 299 */       "Save them", "Discard them", "Cancel");
/*     */     
/* 301 */     if (localThreeButtonDialog.getAnswer() == "Save them")
/*     */     {
/* 303 */       if (!saveFile())
/* 304 */         return false;
/* 305 */       return true;
/*     */     }
/*     */     
/* 308 */     if (localThreeButtonDialog.getAnswer() == "Discard them") {
/* 309 */       return true;
/*     */     }
/* 311 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean closeFile(boolean paramBoolean)
/*     */   {
/* 319 */     if (paramBoolean)
/*     */     {
/* 321 */       debug("Looking for unsaved changes...");
/* 322 */       if (!checkUnsavedChangesAndContinue()) {
/* 323 */         return false;
/*     */       }
/*     */     }
/* 326 */     debug("Closing file...");
/*     */     
/*     */ 
/* 329 */     this.currentFile = null;
/* 330 */     this.miSave.setEnabled(false);
/* 331 */     this.miSaveAs.setEnabled(false);
/* 332 */     this.menuShip.setEnabled(false);
/* 333 */     this.fileContents = new Vector();
/* 334 */     this.browser.removeAllShips();
/*     */     
/* 336 */     setTitle("ShipShaper");
/*     */     
/* 338 */     return true;
/*     */   }
/*     */   
/*     */ 
/*     */   public void newFile(boolean paramBoolean)
/*     */   {
/* 344 */     debug("Creating new file...");
/*     */     
/* 346 */     if (!closeFile(paramBoolean)) {
/* 347 */       return;
/*     */     }
/* 349 */     setTitle("ShipShaper - [new file]");
/*     */     
/*     */ 
/* 352 */     Ship localShip = newShip();
/* 353 */     localShip.setDefinition(localShip.generateShipDefinition(null));
/*     */     
/* 355 */     this.miSaveAs.setEnabled(true);
/* 356 */     this.menuShip.setEnabled(true);
/*     */   }
/*     */   
/*     */   public boolean loadFile(boolean paramBoolean)
/*     */   {
/* 361 */     FileDialog localFileDialog = new FileDialog(this, 
/* 362 */       "Load shipshape file...", 
/* 363 */       0);
/* 364 */     localFileDialog.setModal(true);
/* 365 */     localFileDialog.show();
/* 366 */     if ((localFileDialog.getDirectory() != null) && (localFileDialog.getFile() != null))
/*     */     {
/* 368 */       if (loadFile(localFileDialog.getDirectory() + localFileDialog.getFile(), paramBoolean)) {
/* 369 */         return true;
/*     */       }
/*     */     }
/* 372 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean loadFile(String paramString, boolean paramBoolean)
/*     */   {
/* 382 */     if (!closeFile(paramBoolean)) {
/* 383 */       return false;
/*     */     }
/*     */     
/* 386 */     debug("Loading file '" + paramString + "'");
/* 387 */     java.io.BufferedReader localBufferedReader; try { localBufferedReader = new java.io.BufferedReader(new java.io.FileReader(paramString));
/*     */     }
/*     */     catch (java.io.FileNotFoundException localFileNotFoundException) {
/* 390 */       new shipshaper.dialogs.OkDialog(this, "Couldn't find the file:\n" + paramString);
/* 391 */       return false;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     try
/*     */     {
/*     */       for (;;)
/*     */       {
/* 401 */         String str = localBufferedReader.readLine();
/*     */         
/* 403 */         if (str == null) {
/*     */           break;
/*     */         }
/* 406 */         int i = str.indexOf("(");
/* 407 */         if (i != -1)
/*     */         {
/* 409 */           if (i > 0)
/* 410 */             str = str.substring(i);
/* 411 */           debug("Parsing: " + str);
/* 412 */           Ship localShip = Ship.parseShipDefinition(str);
/* 413 */           if (localShip != null)
/*     */           {
/* 415 */             localShip.shape.removeDoubles();
/* 416 */             if (Preferences.verticalDraw)
/* 417 */               localShip.rotate90(-1);
/* 418 */             this.browser.addShip(localShip);
/* 419 */             localShip.setDefinition(str);
/*     */           }
/*     */           else
/*     */           {
/* 423 */             new shipshaper.dialogs.OkDialog(this, "Parse Error:" + Ship.getParseError());
/*     */           }
/*     */           
/*     */         }
/*     */         else
/*     */         {
/* 429 */           this.fileContents.addElement(str);
/*     */         } }
/* 431 */       this.currentFile = paramString;
/* 432 */       this.miSave.setEnabled(true);
/* 433 */       this.miSaveAs.setEnabled(true);
/* 434 */       this.menuShip.setEnabled(true);
/*     */       
/* 436 */       this.browser.selectShip(0);
/*     */       
/* 438 */       setTitle("ShipShaper - " + this.currentFile);
/*     */       
/* 440 */       localBufferedReader.close();
/*     */     }
/*     */     catch (java.io.IOException localIOException)
/*     */     {
/* 444 */       new shipshaper.dialogs.OkDialog(this, "Error reading file...arrghh!");
/* 445 */       this.browser.removeAllShips();
/* 446 */       return false;
/*     */     }
/*     */     
/* 449 */     return true;
/*     */   }
/*     */   
/*     */   public boolean saveFile()
/*     */   {
/* 454 */     if (this.currentFile == null) {
/* 455 */       return saveFileAs();
/*     */     }
/* 457 */     return saveFile(this.currentFile);
/*     */   }
/*     */   
/*     */   public boolean saveFileAs()
/*     */   {
/* 462 */     FileDialog localFileDialog = new FileDialog(this, 
/* 463 */       "Save shipshape file...", 
/* 464 */       1);
/* 465 */     localFileDialog.setModal(true);
/* 466 */     localFileDialog.show();
/* 467 */     if ((localFileDialog.getDirectory() != null) && (localFileDialog.getFile() != null))
/*     */     {
/*     */ 
/*     */ 
/*     */ 
/* 472 */       if (saveFile(localFileDialog.getDirectory() + localFileDialog.getFile()))
/* 473 */         return true;
/*     */     }
/* 475 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean saveFile(String paramString)
/*     */   {
/* 488 */     debug("Saving as file '" + paramString + "'");
/* 489 */     java.io.BufferedWriter localBufferedWriter; try { localBufferedWriter = new java.io.BufferedWriter(new java.io.FileWriter(paramString));
/*     */     }
/*     */     catch (java.io.IOException localIOException1) {
/* 492 */       new shipshaper.dialogs.OkDialog(this, "Couldn't write to the file:\n" + paramString);
/* 493 */       return false;
/*     */     }
/*     */     
/*     */     try
/*     */     {
/*     */       String str;
/* 499 */       for (int i = 0; i < this.fileContents.size(); i++)
/*     */       {
/* 501 */         str = (String)this.fileContents.elementAt(i);
/* 502 */         localBufferedWriter.write(str, 0, str.length());
/* 503 */         localBufferedWriter.newLine();
/*     */       }
/*     */       
/*     */ 
/* 507 */       for (i = 0; i < this.browser.countShips(); i++)
/*     */       {
/* 509 */         Ship localShip = this.browser.getShip(i);
/* 510 */         if (Preferences.verticalDraw)
/* 511 */           localShip.rotate90(1);
/* 512 */         if (localShip.anyChanges())
/*     */         {
/*     */ 
/*     */ 
/* 516 */           Vector localVector = new Vector();
/* 517 */           debug("Generating ship definition for: " + localShip.name);
/* 518 */           str = localShip.generateShipDefinition(localVector);
/* 519 */           localShip.setDefinition(str);
/*     */           
/*     */ 
/* 522 */           if (localVector.size() > 0)
/*     */           {
/* 524 */             java.awt.TextArea localTextArea = new java.awt.TextArea();
/*     */             
/* 526 */             for (int j = 0; j < localVector.size(); j++)
/* 527 */               localTextArea.append((String)localVector.elementAt(j) + "\n");
/* 528 */             new shipshaper.dialogs.OkDialog(this, "Problems with ship: " + localShip.name + 
/* 529 */               "\n\n", localTextArea);
/*     */           }
/*     */           
/*     */         }
/*     */         else
/*     */         {
/* 535 */           str = localShip.getCachedDefinition();
/* 536 */           debug("Using cached ship definition for: " + localShip.name);
/*     */         }
/*     */         
/* 539 */         if ((Preferences.markSelectedShip) && 
/* 540 */           (localShip == this.browser.getSelectedShip())) {
/* 541 */           str = "xpilot.shipShape: " + str;
/*     */         }
/* 543 */         if (Preferences.verticalDraw)
/* 544 */           localShip.rotate90(-1);
/* 545 */         localBufferedWriter.write(str, 0, str.length());
/* 546 */         localBufferedWriter.newLine();
/*     */       }
/*     */       
/* 549 */       localBufferedWriter.close();
/*     */       
/* 551 */       for (i = 0; i < this.browser.countShips(); i++) {
/* 552 */         this.browser.getShip(i).resetChanges();
/*     */       }
/* 554 */       this.currentFile = paramString;
/* 555 */       setTitle("ShipShaper - " + this.currentFile);
/* 556 */       this.miSave.setEnabled(true);
/* 557 */       debug("Saved.");
/*     */     }
/*     */     catch (java.io.IOException localIOException2)
/*     */     {
/* 561 */       new shipshaper.dialogs.OkDialog(this, "Error writing file!");
/* 562 */       return false;
/*     */     }
/* 564 */     return true;
/*     */   }
/*     */   
/*     */   public void deleteShip(Ship paramShip)
/*     */   {
/* 569 */     if (paramShip != null) {
/* 570 */       this.browser.removeShip(paramShip);
/*     */     }
/*     */   }
/*     */   
/*     */   public void copyShip(Ship paramShip) {
/* 575 */     if (paramShip != null) {
/* 576 */       this.copiedShip = new Ship(paramShip);
/*     */     }
/*     */   }
/*     */   
/*     */   public void cutShip(Ship paramShip) {
/* 581 */     if (paramShip != null)
/*     */     {
/* 583 */       this.copiedShip = paramShip;
/* 584 */       this.browser.removeShip(paramShip);
/*     */     }
/*     */   }
/*     */   
/*     */   public void pasteShip()
/*     */   {
/* 590 */     if (this.copiedShip != null)
/*     */     {
/* 592 */       this.browser.addShip(this.copiedShip);
/* 593 */       this.copiedShip = null;
/*     */     }
/*     */   }
/*     */   
/*     */   public void viewDefinitionString(Ship paramShip)
/*     */   {
/* 599 */     if (paramShip != null)
/*     */     {
/* 601 */       if (Preferences.verticalDraw)
/* 602 */         paramShip.rotate90(1);
/* 603 */       String str = paramShip.generateShipDefinition(null);
/* 604 */       if (Preferences.verticalDraw) {
/* 605 */         paramShip.rotate90(-1);
/*     */       }
/* 607 */       java.awt.TextArea localTextArea = new java.awt.TextArea(str, 1, 60, 
/* 608 */         2);
/* 609 */       localTextArea.selectAll();
/* 610 */       new shipshaper.dialogs.OkDialog(this, 
/*     */       
/* 612 */         "This is the current shipdefinition.\nNote that the coordinates does not match\nthe ones you see in the edit window (because\nof different x- and y-axis).", 
/* 613 */         localTextArea);
/*     */     }
/*     */   }
/*     */   
/*     */   public Ship newShip()
/*     */   {
/* 619 */     Ship localShip = new Ship();
/*     */     
/* 621 */     this.browser.addShip(localShip);
/* 622 */     this.browser.selectShip(localShip);
/*     */     
/* 624 */     return localShip;
/*     */   }
/*     */   
/*     */   public void viewAbout()
/*     */   {
/* 629 */     new AboutWindow(this);
/*     */   }
/*     */   
/*     */   public void viewHelp()
/*     */   {
/* 634 */     new shipshaper.dialogs.OkDialog(this, "Sorry, no help in this version!\n");
/*     */   }
/*     */   
/*     */   public void debug(String paramString)
/*     */   {
/* 639 */     if (this.DEBUG) {
/* 640 */       System.out.println(paramString);
/*     */     }
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\ShipEditor.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */