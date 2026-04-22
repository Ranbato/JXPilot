/*     */ package shipshaper.dialogs;
/*     */ 
/*     */ import java.awt.Component;
/*     */ import java.awt.Container;
/*     */ import java.awt.Dialog;
/*     */ import java.awt.Dimension;
/*     */ import java.awt.Frame;
/*     */ import java.awt.GridBagLayout;
/*     */ import java.awt.Insets;
/*     */ import java.awt.event.ActionListener;
/*     */ import java.awt.event.WindowEvent;
/*     */ import java.util.StringTokenizer;
/*     */ 
/*     */ class MessageDialog extends Dialog implements ActionListener, java.awt.event.WindowListener
/*     */ {
/*     */   protected GridBagLayout gb;
/*     */   protected java.awt.GridBagConstraints gc;
/*     */   protected String answer;
/*     */   protected String closeCommand;
/*     */   
/*     */   public MessageDialog(Frame paramFrame, String paramString)
/*     */   {
/*  23 */     this(paramFrame, paramString, null);
/*     */   }
/*     */   
/*     */   public MessageDialog(Frame paramFrame, String paramString, Component paramComponent)
/*     */   {
/*  28 */     super(paramFrame, "ShipShaper");
/*  29 */     setModal(true);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*  34 */     if (System.getProperty("os.name").indexOf("Windows") != -1) {
/*  35 */       setResizable(false);
/*     */     }
/*     */     
/*  38 */     addWindowListener(this);
/*  39 */     this.gb = new GridBagLayout();
/*  40 */     this.gc = new java.awt.GridBagConstraints();
/*  41 */     this.gc.fill = 0;
/*  42 */     this.gc.gridwidth = 0;
/*  43 */     this.gc.insets = new Insets(20, 20, 0, 20);
/*     */     
/*  45 */     setFont(new java.awt.Font("TimesRoman", 0, 14));
/*  46 */     setLayout(this.gb);
/*     */     
/*     */ 
/*  49 */     StringTokenizer localStringTokenizer = new StringTokenizer(paramString, "\n");
/*     */     
/*     */ 
/*  52 */     Insets localInsets = new Insets(0, 20, 0, 20);
/*  53 */     while (localStringTokenizer.hasMoreTokens())
/*     */     {
/*  55 */       String str = localStringTokenizer.nextToken();
/*  56 */       java.awt.Label localLabel = new java.awt.Label(str);
/*  57 */       this.gb.setConstraints(localLabel, this.gc);
/*  58 */       add(localLabel);
/*  59 */       this.gc.insets = localInsets;
/*     */     }
/*     */     
/*     */ 
/*  63 */     if (paramComponent != null)
/*     */     {
/*  65 */       this.gc.insets = new Insets(20, 20, 0, 20);
/*  66 */       this.gb.setConstraints(paramComponent, this.gc);
/*  67 */       add(paramComponent);
/*     */     }
/*     */     
/*  70 */     this.gc.gridwidth = 1;
/*     */   }
/*     */   
/*     */ 
/*     */   public void setVisible(boolean paramBoolean)
/*     */   {
/*  76 */     if (paramBoolean)
/*     */     {
/*  78 */       Frame localFrame = (Frame)getParent();
/*  79 */       Dimension localDimension1 = localFrame.getSize();
/*  80 */       Dimension localDimension2 = getSize();
/*  81 */       int i = localDimension1.width / 2 - localDimension2.width / 2;
/*  82 */       int j = localDimension1.height / 2 - localDimension2.height / 2;
/*     */       
/*  84 */       setLocation(i, j);
/*  85 */       super.show();
/*     */     }
/*     */     else {
/*  88 */       setVisible(false);
/*     */     }
/*     */   }
/*     */   
/*     */   public void addButton(String paramString, ActionListener paramActionListener) {
/*  93 */     java.awt.Button localButton = new java.awt.Button(paramString);
/*     */     
/*  95 */     this.gc.insets = new Insets(20, 15, 20, 15);
/*  96 */     this.gc.ipadx = 20;
/*  97 */     this.gb.setConstraints(localButton, this.gc);
/*     */     
/*  99 */     localButton.addActionListener(paramActionListener);
/* 100 */     add(localButton);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 105 */     this.closeCommand = paramString;
/*     */   }
/*     */   
/*     */   public void actionPerformed(java.awt.event.ActionEvent paramActionEvent)
/*     */   {
/* 110 */     this.answer = paramActionEvent.getActionCommand();
/* 111 */     dispose();
/*     */   }
/*     */   
/*     */   public String getAnswer()
/*     */   {
/* 116 */     return this.answer;
/*     */   }
/*     */   
/*     */   public boolean answerIs(String paramString)
/*     */   {
/* 121 */     return this.answer.equals(paramString);
/*     */   }
/*     */   
/*     */   public void windowOpened(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowClosing(WindowEvent paramWindowEvent) {
/* 127 */     this.answer = this.closeCommand;
/* 128 */     dispose();
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


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\dialogs\MessageDialog.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */