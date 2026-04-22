/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Graphics;
/*     */ import java.io.PrintStream;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class Item
/*     */ {
/*     */   public static final int UNKNOWN = -1;
/*     */   public static final int MAIN_GUN = 0;
/*     */   public static final int LEFT_GUN = 1;
/*     */   public static final int RIGHT_GUN = 2;
/*     */   public static final int LEFT_REAR_GUN = 3;
/*     */   public static final int RIGHT_REAR_GUN = 4;
/*     */   public static final int LEFT_LIGHT = 5;
/*     */   public static final int RIGHT_LIGHT = 6;
/*     */   public static final int ENGINE = 7;
/*     */   public static final int MISSILE_RACK = 8;
/*     */   public static final int FIRST_TYPE_ID = 0;
/*     */   public static final int LAST_TYPE_ID = 8;
/*     */   
/*     */   private static class ItemType
/*     */   {
/*     */     String name;
/*     */     int icon;
/*     */     String idFull;
/*     */     String idShort;
/*     */     int min;
/*     */     int max;
/*     */     
/*     */     ItemType(String paramString1, int paramInt1, String paramString2, String paramString3, int paramInt2, int paramInt3)
/*     */     {
/*  38 */       this.name = paramString1;
/*  39 */       this.icon = paramInt1;
/*  40 */       this.idFull = paramString2;
/*  41 */       this.idShort = paramString3;
/*  42 */       this.min = paramInt2;
/*  43 */       this.max = paramInt3;
/*     */     }
/*     */     
/*     */     public boolean typeMatch(String paramString)
/*     */     {
/*  48 */       if ((paramString.equalsIgnoreCase(this.idFull)) || (paramString.equalsIgnoreCase(this.idShort)))
/*  49 */         return true;
/*  50 */       return false;
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*  56 */   public static final ItemType[] types = {
/*  57 */     new ItemType("Main Gun", 3, 
/*  58 */     "mainGun", "MG", 0, 1), 
/*  59 */     new ItemType("Left Gun", 4, 
/*  60 */     "leftGun", "LG", 0, 3), 
/*  61 */     new ItemType("Right Gun", 5, 
/*  62 */     "rightGun", "RG", 0, 3), 
/*  63 */     new ItemType("Left Reargun", 6, 
/*  64 */     "leftRearGun", "LR", 0, 3), 
/*  65 */     new ItemType("Right Reargun", 7, 
/*  66 */     "rightRearGun", "RR", 0, 3), 
/*  67 */     new ItemType("Left Light", 8, 
/*  68 */     "leftLight", "LL", 0, 3), 
/*  69 */     new ItemType("Right Light", 9, 
/*  70 */     "rightLight", "RL", 0, 3), 
/*  71 */     new ItemType("Engine", 10, 
/*  72 */     "engine", "EN", 0, 1), 
/*  73 */     new ItemType("Missile Rack", 11, 
/*  74 */     "missileRack", "MR", 0, 4) };
/*     */   public int type;
/*     */   public int x;
/*     */   public int y;
/*     */   
/*     */   public static int parseKeyword(String paramString)
/*     */   {
/*  81 */     for (int i = 0; i < types.length; i++)
/*  82 */       if (types[i].typeMatch(paramString))
/*  83 */         return i;
/*  84 */     return -1;
/*     */   }
/*     */   
/*     */   public static String getName(int paramInt)
/*     */   {
/*  89 */     return types[paramInt].name;
/*     */   }
/*     */   
/*     */   public static int getImageId(int paramInt)
/*     */   {
/*  94 */     return types[paramInt].icon;
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
/*     */ 
/*     */   public static String getFullFileId(int paramInt)
/*     */   {
/* 108 */     return types[paramInt].idFull;
/*     */   }
/*     */   
/*     */   public static String getShortFileId(int paramInt)
/*     */   {
/* 113 */     return types[paramInt].idShort;
/*     */   }
/*     */   
/*     */   public static int getMin(int paramInt)
/*     */   {
/* 118 */     return types[paramInt].min;
/*     */   }
/*     */   
/*     */   public static int getMax(int paramInt)
/*     */   {
/* 123 */     return types[paramInt].max;
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
/*     */ 
/*     */   public void draw(Graphics paramGraphics, boolean paramBoolean, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5)
/*     */   {
/* 137 */     int i = paramInt4 / 2;
/* 138 */     int j = paramInt5 / 2;
/* 139 */     int k = paramInt4 * 3 / 16;
/* 140 */     int m = paramInt5 * 3 / 16;
/* 141 */     int n = paramInt4 * 4 / 16;
/* 142 */     int i1 = paramInt5 * 4 / 16;
/* 143 */     int i2 = paramInt4 * 5 / 16;
/* 144 */     int i3 = paramInt5 * 5 / 16;
/* 145 */     int i4 = paramInt4 * 6 / 16;
/* 146 */     int i5 = paramInt5 * 6 / 16;
/* 147 */     int i6 = paramInt4 * 7 / 16;
/* 148 */     int i7 = paramInt5 * 7 / 16;
/*     */     
/* 150 */     if (!paramBoolean) {}
/*     */     int[] arrayOfInt1;
/* 152 */     int[] arrayOfInt2; switch (paramInt1)
/*     */     {
/*     */     case 0: 
/* 155 */       paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 156 */       paramGraphics.drawLine(paramInt2, paramInt3 - i1, paramInt2, paramInt3 - j);
/* 157 */       return;
/*     */     case 1: 
/* 159 */       paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 160 */       paramGraphics.drawLine(paramInt2 - n, paramInt3, paramInt2 - i4, paramInt3 - i5);
/* 161 */       return;
/*     */     case 2: 
/* 163 */       paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 164 */       paramGraphics.drawLine(paramInt2 + n, paramInt3, paramInt2 + i4, paramInt3 - i5);
/* 165 */       return;
/*     */     case 3: 
/* 167 */       paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 168 */       paramGraphics.drawLine(paramInt2 - n, paramInt3, paramInt2 - i4, paramInt3 + i5);
/* 169 */       return;
/*     */     case 4: 
/* 171 */       paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 172 */       paramGraphics.drawLine(paramInt2 + n, paramInt3, paramInt2 + i4, paramInt3 + i5);
/* 173 */       return;
/*     */     case 5: 
/* 175 */       paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 176 */       paramGraphics.drawLine(paramInt2 - i2, paramInt3 - i3, paramInt2 - i, paramInt3 - i5);
/* 177 */       paramGraphics.drawLine(paramInt2 - i2, paramInt3, paramInt2 - i, paramInt3);
/* 178 */       paramGraphics.drawLine(paramInt2 - i2, paramInt3 + i3, paramInt2 - i, paramInt3 + i5);
/* 179 */       return;
/*     */     case 6: 
/* 181 */       paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 182 */       paramGraphics.drawLine(paramInt2 + i2, paramInt3 - i3, paramInt2 + i, paramInt3 - i5);
/* 183 */       paramGraphics.drawLine(paramInt2 + i2, paramInt3, paramInt2 + i, paramInt3);
/* 184 */       paramGraphics.drawLine(paramInt2 + i2, paramInt3 + i3, paramInt2 + i, paramInt3 + i5);
/* 185 */       return;
/*     */     case 7: 
/* 187 */       arrayOfInt1 = new int[] { paramInt2 - n, paramInt2 + n, paramInt2 + k, paramInt2 - k, 
/* 188 */         paramInt2 - n };
/* 189 */       arrayOfInt2 = new int[] { paramInt3 + i1, paramInt3 + i1, paramInt3 + i7, paramInt3 + i7, 
/* 190 */         paramInt3 + i1 };
/* 191 */       paramGraphics.drawRect(paramInt2 - n, paramInt3 - i1, i, j);
/* 192 */       paramGraphics.drawPolyline(arrayOfInt1, arrayOfInt2, 5);
/* 193 */       return;
/*     */     case 8: 
/* 195 */       int[] arrayOfInt3 = { paramInt2 - k, paramInt2 - k, paramInt2, paramInt2 + k, paramInt2 + k, 
/* 196 */         paramInt2 - k };
/* 197 */       int[] arrayOfInt4 = { paramInt3 + i7, paramInt3 - i7, paramInt3 - j, paramInt3 - i7, paramInt3 + i7, 
/* 198 */         paramInt3 + i7 };
/* 199 */       paramGraphics.drawLine(paramInt2 - k, paramInt3 + m, paramInt2 + k, paramInt3 + m);
/* 200 */       paramGraphics.drawLine(paramInt2 - k, paramInt3 - m, paramInt2 + k, paramInt3 - m);
/* 201 */       paramGraphics.drawPolyline(arrayOfInt3, arrayOfInt4, 6);
/* 202 */       return;
/*     */       
/*     */ 
/*     */ 
/*     */ 
/* 207 */       switch (paramInt1)
/*     */       {
/*     */       case 0: 
/* 210 */         paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 211 */         paramGraphics.drawLine(paramInt2 + n, paramInt3, paramInt2 + i, paramInt3);
/* 212 */         return;
/*     */       case 1: 
/* 214 */         paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 215 */         paramGraphics.drawLine(paramInt2, paramInt3 - i1, paramInt2 + i4, paramInt3 - i5);
/* 216 */         return;
/*     */       case 2: 
/* 218 */         paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 219 */         paramGraphics.drawLine(paramInt2, paramInt3 + i1, paramInt2 + i4, paramInt3 + i5);
/* 220 */         return;
/*     */       case 3: 
/* 222 */         paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 223 */         paramGraphics.drawLine(paramInt2, paramInt3 - i1, paramInt2 - i4, paramInt3 - i5);
/* 224 */         return;
/*     */       case 4: 
/* 226 */         paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 227 */         paramGraphics.drawLine(paramInt2, paramInt3 + i1, paramInt2 - i4, paramInt3 + i5);
/* 228 */         return;
/*     */       case 5: 
/* 230 */         paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 231 */         paramGraphics.drawLine(paramInt2 + i2, paramInt3 - i3, paramInt2 + i, paramInt3 - i5);
/* 232 */         paramGraphics.drawLine(paramInt2, paramInt3 - i3, paramInt2, paramInt3 - j);
/* 233 */         paramGraphics.drawLine(paramInt2 - i2, paramInt3 - i3, paramInt2 - i, paramInt3 - i5);
/* 234 */         return;
/*     */       case 6: 
/* 236 */         paramGraphics.drawOval(paramInt2 - n, paramInt3 - i1, i, j);
/* 237 */         paramGraphics.drawLine(paramInt2 + i2, paramInt3 + i3, paramInt2 + i, paramInt3 + i5);
/* 238 */         paramGraphics.drawLine(paramInt2, paramInt3 + i3, paramInt2, paramInt3 + j);
/* 239 */         paramGraphics.drawLine(paramInt2 - i2, paramInt3 + i3, paramInt2 - i, paramInt3 + i5);
/* 240 */         return;
/*     */       case 7: 
/* 242 */         arrayOfInt1 = new int[] { paramInt2 - n, paramInt2 - n, paramInt2 - i6, paramInt2 - i6, 
/* 243 */           paramInt2 - n };
/* 244 */         arrayOfInt2 = new int[] { paramInt3 - i1, paramInt3 + i1, paramInt3 + m, paramInt3 - m, 
/* 245 */           paramInt3 - i1 };
/* 246 */         paramGraphics.drawRect(paramInt2 - n, paramInt3 - i1, i, j);
/* 247 */         paramGraphics.drawPolyline(arrayOfInt1, arrayOfInt2, 5);
/* 248 */         return;
/*     */       case 8: 
/* 250 */         arrayOfInt3 = new int[] { paramInt2 - i6, paramInt2 + i6, paramInt2 + i, paramInt2 + i6, paramInt2 - i6, 
/* 251 */           paramInt2 - i6 };
/* 252 */         arrayOfInt4 = new int[] { paramInt3 - m, paramInt3 - m, paramInt3, paramInt3 + m, paramInt3 + m, 
/* 253 */           paramInt3 - m };
/* 254 */         paramGraphics.drawLine(paramInt2 - k, paramInt3 - m, paramInt2 - k, paramInt3 + m);
/* 255 */         paramGraphics.drawLine(paramInt2 + k, paramInt3 - m, paramInt2 + k, paramInt3 + m);
/* 256 */         paramGraphics.drawPolyline(arrayOfInt3, arrayOfInt4, 6); return;
/*     */       }
/*     */       
/*     */       
/*     */ 
/*     */ 
/*     */       break;
/*     */     }
/*     */     
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Item(int paramInt1, int paramInt2, int paramInt3)
/*     */   {
/* 272 */     this.type = paramInt1;
/* 273 */     this.x = paramInt2;
/* 274 */     this.y = paramInt3;
/*     */   }
/*     */   
/*     */   public Item(Item paramItem)
/*     */   {
/* 279 */     this.type = paramItem.type;
/* 280 */     this.x = paramItem.x;
/* 281 */     this.y = paramItem.y;
/*     */   }
/*     */   
/*     */   public void moveTo(int paramInt1, int paramInt2)
/*     */   {
/* 286 */     this.x = paramInt1;
/* 287 */     this.y = paramInt2;
/*     */   }
/*     */   
/*     */   public void translate(int paramInt1, int paramInt2)
/*     */   {
/* 292 */     this.x += paramInt1;
/* 293 */     this.y += paramInt2;
/*     */   }
/*     */   
/*     */   public String getName()
/*     */   {
/* 298 */     return getName(this.type);
/*     */   }
/*     */   
/*     */   public int getImageId()
/*     */   {
/* 303 */     return getImageId(this.type);
/*     */   }
/*     */   
/*     */   public String getFullFileId()
/*     */   {
/* 308 */     return getFullFileId(this.type);
/*     */   }
/*     */   
/*     */   public String getShortFileId()
/*     */   {
/* 313 */     return getShortFileId(this.type);
/*     */   }
/*     */   
/*     */   public int getMin()
/*     */   {
/* 318 */     return getMin(this.type);
/*     */   }
/*     */   
/*     */   public int getMax()
/*     */   {
/* 323 */     return getMax(this.type);
/*     */   }
/*     */   
/*     */   public void draw(Graphics paramGraphics, boolean paramBoolean, int paramInt1, int paramInt2, int paramInt3, int paramInt4)
/*     */   {
/* 328 */     draw(paramGraphics, paramBoolean, this.type, paramInt1, paramInt2, paramInt3, paramInt4);
/*     */   }
/*     */   
/*     */   public void print()
/*     */   {
/* 333 */     System.out.println(toString());
/*     */   }
/*     */   
/*     */   public String toString()
/*     */   {
/* 338 */     return getName() + " at (" + this.x + ", " + this.y + ")";
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\Item.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */