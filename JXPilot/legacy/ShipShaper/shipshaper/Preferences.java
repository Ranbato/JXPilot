/*     */ package shipshaper;
/*     */ 
/*     */ import java.io.BufferedInputStream;
/*     */ import java.io.BufferedOutputStream;
/*     */ import java.io.FileNotFoundException;
/*     */ import java.io.FileOutputStream;
/*     */ import java.io.IOException;
/*     */ import java.util.Hashtable;
/*     */ import java.util.Properties;
/*     */ 
/*     */ public abstract class Preferences
/*     */ {
/*     */   private static final String prefFile = "ss.cfg";
/*  14 */   private static Properties props = new Properties();
/*     */   
/*     */   public static boolean checkConnected;
/*     */   
/*     */   public static boolean checkMinMax;
/*     */   public static boolean checkGeometry;
/*     */   public static String startupFile;
/*     */   public static boolean verticalDraw;
/*     */   public static boolean markSelectedShip;
/*     */   public static boolean saveEvenIfErrors;
/*     */   public static boolean calcTrueNumberOfEdges;
/*     */   public static boolean showMirrorMarker;
/*     */   
/*     */   public static boolean load()
/*     */   {
/*     */     BufferedInputStream localBufferedInputStream;
/*     */     try
/*     */     {
/*  32 */       localBufferedInputStream = new BufferedInputStream(new java.io.FileInputStream("ss.cfg"));
/*     */     }
/*     */     catch (FileNotFoundException localFileNotFoundException)
/*     */     {
/*  36 */       getPrefs();
/*  37 */       return true;
/*     */     }
/*     */     
/*     */ 
/*  41 */     props.clear();
/*  42 */     try { props.load(localBufferedInputStream);localBufferedInputStream.close();
/*     */     }
/*     */     catch (IOException localIOException) {
/*  45 */       getPrefs();
/*  46 */       return false;
/*     */     }
/*     */     
/*  49 */     getPrefs();
/*     */     
/*  51 */     return true;
/*     */   }
/*     */   
/*     */   public static boolean save()
/*     */   {
/*     */     BufferedOutputStream localBufferedOutputStream;
/*     */     try
/*     */     {
/*  59 */       localBufferedOutputStream = new BufferedOutputStream(new FileOutputStream("ss.cfg"));
/*     */     }
/*     */     catch (IOException localIOException1) {
/*  62 */       return false;
/*     */     }
/*     */     
/*     */ 
/*  66 */     props.clear();
/*  67 */     setBoolean("checkConnected", checkConnected);
/*  68 */     setBoolean("checkMinMax", checkMinMax);
/*  69 */     setBoolean("checkGeometry", checkGeometry);
/*  70 */     setString("startupFile", startupFile);
/*  71 */     setBoolean("verticalDraw", verticalDraw);
/*  72 */     setBoolean("markSelectedShip", markSelectedShip);
/*  73 */     setBoolean("saveEvenIfErrors", saveEvenIfErrors);
/*  74 */     setBoolean("calcTrueNumberOfEdges", calcTrueNumberOfEdges);
/*  75 */     setBoolean("showMirrorMarker", showMirrorMarker);
/*     */     
/*     */ 
/*  78 */     props.save(localBufferedOutputStream, "ShipShaper config file, please don't edit manually");
/*  79 */     try { localBufferedOutputStream.close();
/*     */     } catch (IOException localIOException2) {}
/*  81 */     return true;
/*     */   }
/*     */   
/*     */   private static void getPrefs()
/*     */   {
/*  86 */     checkConnected = getBoolean("checkConnected", true);
/*  87 */     checkMinMax = getBoolean("checkMinMax", true);
/*  88 */     checkGeometry = getBoolean("checkGeometry", true);
/*  89 */     startupFile = getString("startupFile", "");
/*  90 */     verticalDraw = getBoolean("verticalDraw", true);
/*  91 */     markSelectedShip = getBoolean("markSelectedShip", false);
/*  92 */     saveEvenIfErrors = getBoolean("saveEvenIfErrors", true);
/*  93 */     calcTrueNumberOfEdges = getBoolean("calcTrueNumberOfEdges", true);
/*  94 */     showMirrorMarker = getBoolean("showMirrorMarker", true);
/*     */   }
/*     */   
/*     */   private static String getString(String paramString1, String paramString2)
/*     */   {
/*  99 */     String str = props.getProperty(paramString1);
/* 100 */     if (str == null) {
/* 101 */       return paramString2;
/*     */     }
/* 103 */     return str;
/*     */   }
/*     */   
/*     */   private static void setString(String paramString1, String paramString2)
/*     */   {
/* 108 */     props.put(paramString1, paramString2);
/*     */   }
/*     */   
/*     */   private static boolean getBoolean(String paramString, boolean paramBoolean)
/*     */   {
/* 113 */     String str = props.getProperty(paramString);
/* 114 */     if (str == null) {
/* 115 */       return paramBoolean;
/*     */     }
/* 117 */     return Boolean.valueOf(str).booleanValue();
/*     */   }
/*     */   
/*     */   private static void setBoolean(String paramString, boolean paramBoolean)
/*     */   {
/* 122 */     props.put(paramString, String.valueOf(paramBoolean));
/*     */   }
/*     */   
/*     */   private static int getInteger(String paramString, int paramInt)
/*     */   {
/* 127 */     String str = props.getProperty(paramString);
/* 128 */     if (str == null) {
/* 129 */       return paramInt;
/*     */     }
/* 131 */     return Integer.valueOf(str).intValue();
/*     */   }
/*     */   
/*     */   private static void setInteger(String paramString, int paramInt)
/*     */   {
/* 136 */     props.put(paramString, String.valueOf(paramInt));
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\Preferences.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */