/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Image;
/*     */ import java.awt.Toolkit;
/*     */ import java.awt.image.ImageObserver;
/*     */ import java.net.URL;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public abstract class Images
/*     */ {
/*     */   public static final int LOGO = 0;
/*     */   public static final int SMALL_LOGO = 1;
/*     */   public static final int ICON = 2;
/*     */   public static final int MAIN_GUN = 3;
/*     */   public static final int LEFT_GUN = 4;
/*     */   public static final int RIGHT_GUN = 5;
/*     */   public static final int LEFT_REAR_GUN = 6;
/*     */   public static final int RIGHT_REAR_GUN = 7;
/*     */   public static final int LEFT_LIGHT = 8;
/*     */   public static final int RIGHT_LIGHT = 9;
/*     */   public static final int ENGINE = 10;
/*     */   public static final int MISSILE_RACK = 11;
/*     */   public static final int POLY_LINE_TOOL = 12;
/*     */   public static final int MOVE_TOOL = 13;
/*     */   public static final int DELETE_TOOL = 14;
/*     */   public static final int SPLIT_TOOL = 15;
/*     */   public static final int JOIN_TOOL = 16;
/*     */   public static final int CLEAR = 17;
/*     */   public static final int ROTATE_CLOCKWISE = 18;
/*     */   public static final int ROTATE_ANTI_CLOCKWISE = 19;
/*     */   public static final int FLIP_HORIZ = 20;
/*     */   public static final int FLIP_VERT = 21;
/*     */   public static final int SCROLL_UP = 22;
/*     */   public static final int SCROLL_DOWN = 23;
/*     */   public static final int SCROLL_LEFT = 24;
/*     */   public static final int SCROLL_RIGHT = 25;
/*     */   public static final int NOT_FINISHED = 0;
/*     */   public static final int FINISHED = 1;
/*     */   public static final int LOADING_ERROR = 2;
/*  51 */   private static final String[] imageURLs = {
/*  52 */     "logo.gif", 
/*  53 */     "small_logo.gif", 
/*  54 */     "icon.gif", 
/*     */     
/*  56 */     "i_main_gun.gif", 
/*  57 */     "i_left_gun.gif", 
/*  58 */     "i_right_gun.gif", 
/*  59 */     "i_left_rgun.gif", 
/*  60 */     "i_right_rgun.gif", 
/*  61 */     "i_left_light.gif", 
/*  62 */     "i_right_light.gif", 
/*  63 */     "i_engine.gif", 
/*  64 */     "i_missile_rack.gif", 
/*     */     
/*  66 */     "i_line.gif", 
/*  67 */     "i_move.gif", 
/*  68 */     "i_delete.gif", 
/*  69 */     "i_split.gif", 
/*  70 */     "i_join.gif", 
/*     */     
/*  72 */     "o_clear.gif", 
/*  73 */     "o_rotate1.gif", 
/*  74 */     "o_rotate2.gif", 
/*  75 */     "o_flip_horiz.gif", 
/*  76 */     "o_flip_vert.gif", 
/*  77 */     "o_scroll_up.gif", 
/*  78 */     "o_scroll_down.gif", 
/*  79 */     "o_scroll_left.gif", 
/*  80 */     "o_scroll_right.gif" };
/*     */   
/*  82 */   private static int nImages = imageURLs.length;
/*  83 */   private static Image[] images = new Image[nImages];
/*  84 */   private static int[] imageStatus = new int[nImages];
/*     */   private static int nDone;
/*     */   private static int nErrors;
/*     */   private static Thread thread;
/*     */   
/*     */   private static class TheObserver implements ImageObserver
/*     */   {
/*     */     public boolean imageUpdate(Image paramImage, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5)
/*     */     {
/*  93 */       for (int i = 0; i < Images.nImages; i++)
/*     */       {
/*  95 */         if (Images.images[i] == paramImage)
/*     */         {
/*  97 */           if ((paramInt1 & 0x20) != 0)
/*     */           {
/*  99 */             Images.imageStatus[i] = 1;
/* 100 */             Images.nDone += 1;break;
/*     */           }
/*     */           
/*     */ 
/* 103 */           if ((paramInt1 & 0x40) == 0)
/*     */             break;
/* 105 */           Images.imageStatus[i] = 2;
/* 106 */           Images.nDone += 1;
/* 107 */           Images.nErrors += 1;
/*     */           
/* 109 */           break;
/*     */         }
/*     */       }
/*     */       
/* 113 */       return true;
/*     */     }
/*     */   }
/*     */   
/*     */   public static String getImageFile(int paramInt)
/*     */   {
/* 119 */     return imageURLs[paramInt];
/*     */   }
/*     */   
/*     */   public static int countImages()
/*     */   {
/* 124 */     return nImages;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static void initImages()
/*     */   {
/* 134 */     Toolkit localToolkit = Toolkit.getDefaultToolkit();
/* 135 */     nDone = 0;
/* 136 */     nErrors = 0;
/*     */     
/* 138 */     for (int i = 0; i < nImages; i++)
/*     */     {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 144 */       URL localURL = Images.class.getResource("graphics/" + getImageFile(i));
/*     */       
/*     */ 
/* 147 */       images[i] = localToolkit.getImage(localURL);
/* 148 */       imageStatus[i] = 0;
/* 149 */       localToolkit.prepareImage(images[i], -1, -1, new TheObserver());
/*     */     }
/*     */   }
/*     */   
/*     */   public static boolean anyErrors()
/*     */   {
/* 155 */     return nErrors > 0;
/*     */   }
/*     */   
/*     */   public static int[] getErrorImages()
/*     */   {
/* 160 */     if (anyErrors())
/*     */     {
/* 162 */       int[] arrayOfInt = new int[nErrors];
/* 163 */       int i = 0;
/*     */       
/* 165 */       for (int j = 0; j < nImages; j++) {
/* 166 */         if (imageStatus[j] == 2)
/* 167 */           arrayOfInt[(i++)] = j;
/*     */       }
/* 169 */       return arrayOfInt;
/*     */     }
/* 171 */     return null;
/*     */   }
/*     */   
/*     */   public static int getImageStatus(int paramInt)
/*     */   {
/* 176 */     return imageStatus[paramInt];
/*     */   }
/*     */   
/*     */   public static boolean loadImage(int paramInt)
/*     */   {
/* 181 */     while (imageStatus[paramInt] == 0) {}
/*     */     
/* 183 */     return imageStatus[paramInt] == 1;
/*     */   }
/*     */   
/*     */   public static boolean loadAllImages()
/*     */   {
/* 188 */     while (nDone != nImages)
/*     */     {
/*     */       try
/*     */       {
/* 192 */         Thread.sleep(1000L);
/*     */       }
/*     */       catch (Exception localException) {}
/*     */       
/* 196 */       ShipShaper.debug("nDone = " + nDone + ", images = " + nImages);
/*     */     }
/* 198 */     return !anyErrors();
/*     */   }
/*     */   
/*     */   public static Image loadAndGetImage(int paramInt)
/*     */   {
/* 203 */     if (imageStatus[paramInt] == 0)
/* 204 */       loadImage(paramInt);
/* 205 */     return images[paramInt];
/*     */   }
/*     */   
/*     */   public static Image getImage(int paramInt)
/*     */   {
/* 210 */     return images[paramInt];
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\Images.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */