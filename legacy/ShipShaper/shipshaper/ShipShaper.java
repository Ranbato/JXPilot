/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Color;
/*     */ import java.awt.Component;
/*     */ import java.awt.Dimension;
/*     */ import java.awt.Font;
/*     */ import java.awt.FontMetrics;
/*     */ import java.awt.Frame;
/*     */ import java.awt.Graphics;
/*     */ import java.awt.Insets;
/*     */ import java.awt.Toolkit;
/*     */ import java.awt.Window;
/*     */ import java.awt.event.WindowEvent;
/*     */ import java.awt.event.WindowListener;
/*     */ import java.io.PrintStream;
/*     */ import shipshaper.dialogs.OkDialog;
/*     */ 
/*     */ public final class ShipShaper
/*     */   extends Frame implements Runnable, WindowListener
/*     */ {
/*     */   public static final double VERSION = 1.0D;
/*     */   public static final String COPYRIGHT = "(c) 1998 Jonny Svärling";
/*     */   public static final String EMAIL = "d93-jsv@nada.kth.se";
/*     */   public static boolean DEBUG;
/*     */   private static final int w = 330;
/*     */   private static final int h = 160;
/*     */   private static final int infoY = 150;
/*     */   private Thread ssthread;
/*     */   private String statusString;
/*     */   private String prevStatusString;
/*     */   private Font statusFont;
/*     */   private FontMetrics sFM;
/*     */   public ShipEditor editor;
/*     */   
/*     */   public static void main(String[] paramArrayOfString)
/*     */   {
/*  37 */     parseArgs(paramArrayOfString);
/*  38 */     debug("Creating ShipShaper...");
/*  39 */     ShipShaper localShipShaper = new ShipShaper();
/*  40 */     debug("Showing ShipShaper...");
/*  41 */     localShipShaper.show();
/*  42 */     debug("Starting ShipShaper...");
/*  43 */     localShipShaper.start();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static void parseArgs(String[] paramArrayOfString)
/*     */   {
/*  52 */     for (int i = 0; i < paramArrayOfString.length; i++)
/*     */     {
/*  54 */       if (paramArrayOfString[i].equals("-debug")) {
/*  55 */         DEBUG = true;
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public ShipShaper() {
/*  61 */     super("ShipShaper");
/*     */     
/*  63 */     debug("Initiating images...");
/*  64 */     Images.initImages();
/*  65 */     debug("Images initiated...");
/*     */     
/*  67 */     debug("Setting size, loading font, etc.");
/*  68 */     setSize(330, 160);
/*  69 */     Dimension localDimension = Toolkit.getDefaultToolkit().getScreenSize();
/*  70 */     setLocation(localDimension.width / 2 - 165, localDimension.height / 2 - 80);
/*  71 */     setResizable(false);
/*  72 */     this.statusFont = new Font("TimesRoman", 0, 12);
/*  73 */     this.sFM = getFontMetrics(this.statusFont);
/*  74 */     setBackground(Color.white);
/*     */     
/*  76 */     debug("Loading icon image...");
/*  77 */     if (Images.getImage(2) != null) {
/*  78 */       setIconImage(Images.getImage(2));
/*     */     }
/*  80 */     addWindowListener(this);
/*  81 */     debug("End of ShipShaper contructor.");
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void start()
/*     */   {
/*  90 */     if (this.ssthread == null)
/*     */     {
/*  92 */       this.ssthread = new Thread(this);
/*  93 */       this.ssthread.start();
/*     */     }
/*     */   }
/*     */   
/*     */   public void stop()
/*     */   {
/*  99 */     if (this.ssthread != null)
/*     */     {
/* 101 */       this.ssthread.stop();
/* 102 */       this.ssthread = null;
/*     */     }
/*     */   }
/*     */   
/*     */   public void run()
/*     */   {
/* 108 */     debug("Running...");
/* 109 */     setStatus("Loading graphics...");
/* 110 */     Images.loadAllImages();
/*     */     
/* 112 */     if (Images.anyErrors())
/*     */     {
/* 114 */       setStatus("Error loading graphics. Close the window to exit.");
/* 115 */       return;
/*     */     }
/*     */     
/* 118 */     setStatus("Loading preferences...");
/* 119 */     if (!Preferences.load()) {
/* 120 */       new OkDialog(this, "Error reading preferences. Using defaults.");
/*     */     }
/* 122 */     setStatus("Just a moment...");
/* 123 */     this.editor = new ShipEditor();
/*     */     
/* 125 */     setStatus("");
/* 126 */     dispose();
/*     */   }
/*     */   
/*     */   public void windowOpened(WindowEvent paramWindowEvent) {}
/*     */   
/*     */   public void windowClosing(WindowEvent paramWindowEvent) {
/* 132 */     dispose();
/* 133 */     System.exit(0);
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
/*     */   
/*     */   public Insets getInsets() {
/* 147 */     return new Insets(30, 0, 0, 0);
/*     */   }
/*     */   
/*     */   private void setStatus(String paramString)
/*     */   {
/* 152 */     this.prevStatusString = this.statusString;
/* 153 */     this.statusString = paramString;
/* 154 */     repaint();
/*     */   }
/*     */   
/*     */   public void update(Graphics paramGraphics)
/*     */   {
/* 159 */     paint(paramGraphics);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void paint(Graphics paramGraphics)
/*     */   {
/* 166 */     paramGraphics.drawImage(Images.getImage(0), 0, 30, this);
/* 167 */     paramGraphics.setColor(Color.black);
/*     */     
/* 169 */     paramGraphics.setFont(this.statusFont);
/*     */     
/* 171 */     paramGraphics.drawString("Version " + String.valueOf(1.0D), 10, 116);
/*     */     
/* 173 */     int i = this.sFM.stringWidth("(c) 1998 Jonny Svärling");
/* 174 */     paramGraphics.drawString("(c) 1998 Jonny Svärling", 300 - i, 116);
/*     */     
/* 176 */     if (this.statusString != null)
/*     */     {
/* 178 */       if (this.prevStatusString != null)
/*     */       {
/* 180 */         i = this.sFM.stringWidth(this.prevStatusString);
/* 181 */         paramGraphics.setColor(getBackground());
/* 182 */         paramGraphics.fillRect(165 - i / 2, 150 - this.sFM.getAscent() - 1, 
/* 183 */           i, this.sFM.getHeight() + 4);
/*     */       }
/*     */       
/* 186 */       i = this.sFM.stringWidth(this.statusString);
/* 187 */       paramGraphics.setColor(Color.black);
/* 188 */       paramGraphics.drawString(this.statusString, 165 - i / 2, 150);
/*     */     }
/*     */   }
/*     */   
/*     */   public static void debug(String paramString)
/*     */   {
/* 194 */     if (DEBUG) System.out.println(paramString);
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\ShipShaper.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */