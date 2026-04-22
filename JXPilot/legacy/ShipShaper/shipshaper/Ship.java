/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Point;
/*     */ import java.awt.Rectangle;
/*     */ import java.util.EventObject;
/*     */ import java.util.StringTokenizer;
/*     */ import java.util.Vector;
/*     */ import shipshaper.graph.Edge;
/*     */ import shipshaper.graph.Graph;
/*     */ import shipshaper.graph.Vertex;
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
/*     */ 
/*     */ 
/*     */ public final class Ship
/*     */ {
/*     */   public static final int MAX_SIZE = 15;
/*     */   public static final int MIN_SIZE = 8;
/*     */   public static final int MIN_WIDTH_PLUS_HEIGHT = 38;
/*     */   public static final int MAX_VERTICES = 24;
/*     */   public static final int MAX_NAME_LEN = 15;
/*     */   public static final int MAX_AUTHOR_LEN = 15;
/*     */   public static final int ROTATE_CLOCKWISE = 1;
/*     */   public static final int ROTATE_ANTI_CLOCKWISE = -1;
/*     */   public static final int FLIP_HORIZ = 1;
/*     */   public static final int FLIP_VERT = -1;
/*     */   public static final int SCROLL_UP = 0;
/*     */   public static final int SCROLL_DOWN = 1;
/*     */   public static final int SCROLL_LEFT = 2;
/*     */   public static final int SCROLL_RIGHT = 3;
/*     */   public static final int KEYWORD_UNKNOWN = 0;
/*     */   public static final int KEYWORD_ITEM = 1;
/*     */   public static final int KEYWORD_SHAPE = 2;
/*     */   public static final int KEYWORD_NAME = 3;
/*     */   public static final int KEYWORD_AUTHOR = 4;
/*     */   protected Vector shipListeners;
/*     */   public Graph shape;
/*     */   public Equipment items;
/*     */   public String name;
/*     */   public String author;
/*     */   private boolean changes;
/*     */   private String cachedDefinition;
/*     */   private static String parseErrorString;
/*     */   private static Ship parsingShip;
/*     */   private static String parseReadSoFar;
/*     */   private Vector problemsVector;
/*     */   private boolean checkProblems;
/*     */   
/*     */   public Ship()
/*     */   {
/*  62 */     this.shape = new Graph();
/*  63 */     this.items = new Equipment();
/*  64 */     this.name = "";
/*  65 */     this.author = "";
/*  66 */     this.shipListeners = new Vector();
/*     */     
/*  68 */     this.changes = true;
/*     */   }
/*     */   
/*     */   public Ship(Ship paramShip)
/*     */   {
/*  73 */     this.shape = new Graph(paramShip.shape);
/*  74 */     this.items = new Equipment(paramShip.items);
/*  75 */     this.name = paramShip.name;
/*  76 */     this.author = paramShip.author;
/*  77 */     this.shipListeners = new Vector();
/*     */     
/*  79 */     this.changes = true;
/*     */   }
/*     */   
/*     */   public void addShipListener(ShipListener paramShipListener)
/*     */   {
/*  84 */     if (!this.shipListeners.contains(paramShipListener)) {
/*  85 */       this.shipListeners.addElement(paramShipListener);
/*     */     }
/*     */   }
/*     */   
/*     */   public void removeShipListener(ShipListener paramShipListener) {
/*  90 */     this.shipListeners.removeElement(paramShipListener);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setDefinition(String paramString)
/*     */   {
/*  99 */     this.cachedDefinition = paramString;
/* 100 */     this.changes = false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void shipChanged(ShipEvent paramShipEvent)
/*     */   {
/* 107 */     for (int i = 0; i < this.shipListeners.size(); i++)
/*     */     {
/* 109 */       ShipListener localShipListener = (ShipListener)this.shipListeners.elementAt(i);
/*     */       
/*     */ 
/* 112 */       if (localShipListener != paramShipEvent.getSource())
/* 113 */         localShipListener.shipChanged(paramShipEvent);
/*     */     }
/* 115 */     this.changes = true;
/*     */   }
/*     */   
/*     */ 
/*     */   public void resetChanges()
/*     */   {
/* 121 */     this.changes = false;
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean anyChanges()
/*     */   {
/* 127 */     return this.changes;
/*     */   }
/*     */   
/*     */   public void clear()
/*     */   {
/* 132 */     this.shape = new Graph();
/* 133 */     this.items = new Equipment();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void rotate90(int paramInt)
/*     */   {
/* 143 */     for (int i = 0; i < this.shape.countVertices(); i++)
/*     */     {
/* 145 */       Vertex localVertex = this.shape.vertex(i);
/*     */       
/* 147 */       if (paramInt == 1) {
/* 148 */         localVertex.moveTo(-localVertex.y, localVertex.x);
/*     */       } else {
/* 150 */         localVertex.moveTo(localVertex.y, -localVertex.x);
/*     */       }
/*     */     }
/*     */     
/* 154 */     for (i = 0; i < this.items.countItems(); i++)
/*     */     {
/* 156 */       Item localItem = this.items.item(i);
/*     */       
/* 158 */       if (paramInt == 1) {
/* 159 */         localItem.moveTo(-localItem.y, localItem.x);
/*     */       } else {
/* 161 */         localItem.moveTo(localItem.y, -localItem.x);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void flip(int paramInt)
/*     */   {
/* 170 */     for (int i = 0; i < this.shape.countVertices(); i++)
/*     */     {
/* 172 */       Vertex localVertex = this.shape.vertex(i);
/*     */       
/* 174 */       if (paramInt == 1) {
/* 175 */         localVertex.moveTo(-localVertex.x, localVertex.y);
/*     */       } else {
/* 177 */         localVertex.moveTo(localVertex.x, -localVertex.y);
/*     */       }
/*     */     }
/*     */     
/* 181 */     for (i = 0; i < this.items.countItems(); i++)
/*     */     {
/* 183 */       Item localItem = this.items.item(i);
/*     */       
/* 185 */       if (paramInt == 1) {
/* 186 */         localItem.moveTo(-localItem.x, localItem.y);
/*     */       } else {
/* 188 */         localItem.moveTo(localItem.x, -localItem.y);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean withinShipBorders(int paramInt1, int paramInt2)
/*     */   {
/* 196 */     return (Math.abs(paramInt1) <= 15) && 
/* 197 */       (Math.abs(paramInt2) <= 15);
/*     */   }
/*     */   
/*     */   public void scroll(int paramInt)
/*     */   {
/* 202 */     int j = 0;int k = 0;
/*     */     
/*     */ 
/*     */ 
/* 206 */     if (paramInt == 0) k = -1;
/* 207 */     if (paramInt == 1) k = 1;
/* 208 */     if (paramInt == 2) j = -1;
/* 209 */     if (paramInt == 3) { j = 1;
/*     */     }
/*     */     
/* 212 */     for (int i = 0; i < this.shape.countVertices(); i++)
/*     */     {
/* 214 */       Vertex localVertex = this.shape.vertex(i);
/* 215 */       if (!withinShipBorders(localVertex.x + j, localVertex.y + k))
/* 216 */         return;
/*     */     }
/* 218 */     for (i = 0; i < this.items.countItems(); i++)
/*     */     {
/* 220 */       Item localItem = this.items.item(i);
/* 221 */       if (!withinShipBorders(localItem.x + j, localItem.y + k)) {
/* 222 */         return;
/*     */       }
/*     */     }
/*     */     
/* 226 */     for (i = 0; i < this.shape.countVertices(); i++)
/* 227 */       this.shape.vertex(i).translate(j, k);
/* 228 */     for (i = 0; i < this.items.countItems(); i++) {
/* 229 */       this.items.item(i).translate(j, k);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public static int parseKeyword(String paramString)
/*     */   {
/* 236 */     int i = Item.parseKeyword(paramString);
/*     */     
/*     */ 
/* 239 */     if (i != -1) {
/* 240 */       return 1;
/*     */     }
/*     */     
/* 243 */     if ((paramString.equals("SH")) || (paramString.equals("shape"))) {
/* 244 */       return 2;
/*     */     }
/*     */     
/* 247 */     if ((paramString.equals("NM")) || (paramString.equals("name"))) {
/* 248 */       return 3;
/*     */     }
/*     */     
/* 251 */     if ((paramString.equals("AU")) || (paramString.equals("author"))) {
/* 252 */       return 4;
/*     */     }
/* 254 */     return 0;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static Ship parseShipDefinition(String paramString)
/*     */   {
/* 274 */     String str2 = null;Object localObject = null;
/*     */     
/* 276 */     int k = 0;int m = 0;
/*     */     
/* 278 */     String str3 = null;
/* 279 */     int n = 0;int i1 = 0;
/* 280 */     int i2 = 0;
/* 281 */     int i3 = 0;
/*     */     
/* 283 */     Ship localShip = new Ship();
/* 284 */     localShip.name = null;
/* 285 */     parsingShip = localShip;
/* 286 */     Vector localVector = new Vector();
/* 287 */     StringTokenizer localStringTokenizer = new StringTokenizer(paramString, " \t(:,)", true);
/*     */     
/* 289 */     parseReadSoFar = "";
/* 290 */     int j = 0;
/* 291 */     while ((localStringTokenizer.hasMoreTokens()) || (i2 != 0)) {
/*     */       String str1;
/* 293 */       if (i2 != 0)
/*     */       {
/* 295 */         str1 = str2;
/* 296 */         i2 = 0;
/*     */       }
/*     */       else {
/* 299 */         str1 = localStringTokenizer.nextToken(); }
/* 300 */       parseReadSoFar += str1;
/*     */       
/* 302 */       if ((!str1.equals(" ")) && (!str1.equals("\t")))
/*     */       {
/*     */         int i;
/* 305 */         switch (j)
/*     */         {
/*     */ 
/*     */ 
/*     */         case 0: 
/* 310 */           if (!str1.equals("("))
/* 311 */             return parseError("\"(\"", "\"" + str1 + "\"");
/* 312 */           j = 1;
/* 313 */           break;
/*     */         
/*     */ 
/*     */ 
/*     */ 
/*     */         case 1: 
/* 319 */           localObject = str1;
/*     */           
/* 321 */           n = parseKeyword((String)localObject);
/*     */           
/* 323 */           if (n == 0)
/*     */           {
/*     */ 
/* 326 */             while ((goto 271) || ((localStringTokenizer.hasMoreTokens()) && (localStringTokenizer.nextToken() != ")"))) {}
/*     */             
/* 328 */             j = 0;
/*     */           }
/*     */           else
/*     */           {
/* 332 */             if (n == 1) {
/* 333 */               i1 = Item.parseKeyword((String)localObject);
/*     */             }
/* 335 */             localVector.removeAllElements();
/* 336 */             str3 = "";
/* 337 */             j = 2; }
/* 338 */           break;
/*     */         
/*     */ 
/*     */         case 2: 
/* 342 */           if (!str1.equals(":")) {
/* 343 */             return parseError("\":\"", "\"" + str1 + "\"");
/*     */           }
/*     */           
/* 346 */           if ((n == 1) || (n == 2)) {
/* 347 */             j = 10;
/*     */           }
/*     */           else {
/* 350 */             i3 = 1;
/* 351 */             j = 15;
/*     */           }
/*     */           
/* 354 */           break;
/*     */         
/*     */ 
/*     */ 
/*     */         case 10: 
/* 359 */           if (str1.equals(")"))
/*     */           {
/*     */             Point localPoint1;
/* 362 */             if (n == 2)
/*     */             {
/* 364 */               for (i = 0; i < localVector.size(); i++)
/*     */               {
/* 366 */                 localPoint1 = (Point)localVector.elementAt(i);
/* 367 */                 Point localPoint2 = (Point)localVector.elementAt((i + 1) % localVector.size());
/*     */                 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 375 */                 if (!localPoint1.equals(localPoint2)) {
/* 376 */                   localShip.shape.add(new Edge(new Vertex(localPoint1.x, -localPoint1.y), 
/* 377 */                     new Vertex(localPoint2.x, -localPoint2.y)));
/*     */                 }
/*     */                 
/*     */               }
/* 381 */             } else if (n == 1)
/*     */             {
/* 383 */               for (i = 0; i < localVector.size(); i++)
/*     */               {
/* 385 */                 localPoint1 = (Point)localVector.elementAt(i);
/* 386 */                 localShip.items.add(new Item(i1, localPoint1.x, -localPoint1.y));
/*     */               }
/*     */             }
/* 389 */             j = 0;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */           }
/* 397 */           else if (str1.equals(","))
/*     */           {
/* 399 */             j = 10;
/*     */           }
/*     */           else
/*     */           {
/*     */             try {
/* 404 */               k = Integer.parseInt(str1);
/*     */             } catch (NumberFormatException localNumberFormatException1) {
/* 406 */               return parseError("a x-coordinate", 
/* 407 */                 "a non-number (\"" + str1 + "\")");
/*     */             }
/* 409 */             j = 12; }
/* 410 */           break;
/*     */         
/*     */ 
/*     */ 
/*     */         case 12: 
/* 415 */           if (!str1.equals(",")) {
/* 416 */             return parseError("\",\"", "\"" + str1 + "\"");
/*     */           }
/* 418 */           j = 13;
/* 419 */           break;
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
/*     */         case 13: 
/* 431 */           i = str1.indexOf("-");
/* 432 */           if (i > 0)
/*     */           {
/* 434 */             str2 = str1.substring(i);
/* 435 */             str1 = str1.substring(0, i);
/* 436 */             i2 = 1;
/*     */           }
/*     */           try {
/* 439 */             m = Integer.parseInt(str1);
/*     */           } catch (NumberFormatException localNumberFormatException2) {
/* 441 */             return parseError("a y-coordinate", 
/* 442 */               "a non-number (\"" + str1 + "\")");
/*     */           }
/*     */           
/*     */ 
/* 446 */           localVector.addElement(new Point(k, m));
/*     */           
/* 448 */           j = 10;
/* 449 */           break;
/*     */         
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */         case 15: 
/* 456 */           if (str1.equals("(")) {
/* 457 */             i3++;
/*     */           }
/*     */           
/* 460 */           if (str1.equals(")"))
/*     */           {
/* 462 */             if (i3 == 1)
/*     */             {
/* 464 */               j = 0;
/*     */             }
/*     */             else
/*     */             {
/* 468 */               i3--;
/*     */             }
/*     */           }
/*     */           else {
/* 472 */             str3 = str3 + " " + str1;
/*     */             
/* 474 */             if (n == 3) {
/* 475 */               localShip.name = str3;
/*     */             }
/* 477 */             else if (n == 4) {
/* 478 */               localShip.author = str3;
/*     */             }
/* 480 */             j = 15; }
/* 481 */           break;
/*     */         
/*     */         default: 
/* 484 */           return parseError("Internal error...crazy state: " + j);
/*     */         }
/*     */         
/*     */       }
/*     */     }
/*     */     
/*     */ 
/* 491 */     if (j != 0)
/*     */     {
/*     */ 
/* 494 */       parseError("Unexpected end of input.");
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 499 */     return localShip;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private static Ship parseError(String paramString1, String paramString2)
/*     */   {
/* 509 */     if (paramString2 == null) {
/* 510 */       parseErrorString = paramString1;
/*     */     } else
/* 512 */       parseErrorString = " Expected " + paramString1 + ", but found " + paramString2;
/* 513 */     parseErrorString = parseErrorString + "\nRead so far: \"" + parseReadSoFar + "\"\n";
/* 514 */     if (parsingShip.name == null) {
/* 515 */       parseErrorString += "Don't know the ship name yet.";
/*     */     } else
/* 517 */       parseErrorString = parseErrorString + "Ship: " + parsingShip.name;
/* 518 */     return null;
/*     */   }
/*     */   
/*     */   private static Ship parseError(String paramString)
/*     */   {
/* 523 */     return parseError(paramString, null);
/*     */   }
/*     */   
/*     */   public static String getParseError()
/*     */   {
/* 528 */     return parseErrorString;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public String getCachedDefinition()
/*     */   {
/* 535 */     return this.cachedDefinition;
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
/*     */ 
/*     */ 
/*     */   public String generateShipDefinition(Vector paramVector)
/*     */   {
/* 577 */     int n = 0;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 582 */     this.problemsVector = paramVector;
/* 583 */     if (paramVector != null) {
/* 584 */       this.checkProblems = true;
/*     */     }
/*     */     
/* 587 */     String str = "(NM: " + this.name + ")(AU: " + this.author + ")";
/*     */     
/*     */ 
/* 590 */     Graph localGraph2 = new Graph(this.shape);
/*     */     
/*     */ 
/* 593 */     Vector localVector1 = localGraph2.getIslands();
/*     */     
/*     */ 
/*     */ 
/* 597 */     if (localVector1.size() > 1)
/*     */     {
/* 599 */       if (Preferences.checkConnected)
/*     */       {
/* 601 */         addProblem(
/*     */         
/* 603 */           "The ship is not fully connected. ShipShaper will still\nbe able to load and save it, but XPilot can't load it.\n(No further problems will be reported.)");
/*     */       }
/*     */       
/*     */ 
/*     */ 
/* 608 */       this.checkProblems = false;
/*     */     }
/*     */     
/*     */     int m;
/* 612 */     for (int i = 0; i < localVector1.size(); i++)
/*     */     {
/* 614 */       Graph localGraph1 = (Graph)localVector1.elementAt(i);
/*     */       
/* 616 */       if (!localGraph1.eulerPathExists())
/*     */       {
/*     */ 
/*     */ 
/* 620 */         n = localGraph1.makeAllVerticesEven();
/*     */       }
/*     */       
/*     */ 
/* 624 */       str = str + "(SH:";
/* 625 */       Vector localVector2 = localGraph1.findEulerPath();
/* 626 */       m = localVector2.size();
/* 627 */       for (int j = 0; j < m; j++)
/*     */       {
/* 629 */         Vertex localVertex = (Vertex)localVector2.elementAt(j);
/*     */         
/* 631 */         str = str + " " + localVertex.x + "," + -localVertex.y;
/*     */       }
/* 633 */       str = str + ")";
/*     */       
/*     */ 
/* 636 */       if (Preferences.checkMinMax)
/*     */       {
/* 638 */         if (m < 3)
/* 639 */           addProblem("Too few shape points.");
/* 640 */         if ((m > 24) && (this.checkProblems))
/*     */         {
/* 642 */           if (n > 0) {
/* 643 */             addProblem("Too many shape points (" + localVector2.size() + "), " + 
/* 644 */               "maximum is " + 24 + ".\n" + 
/* 645 */               "This may be the result of the need of adding " + 
/* 646 */               n + " edges to\n" + 
/* 647 */               "   the ship to convert it to a single polyline.");
/*     */           } else {
/* 649 */             addProblem("Too many shape points (" + localVector2.size() + "), " + 
/* 650 */               "maximum is " + 24 + ".");
/*     */           }
/*     */         }
/*     */       }
/*     */       
/*     */ 
/* 656 */       if ((this.checkProblems) && (Preferences.checkGeometry))
/*     */       {
/* 658 */         Rectangle localRectangle = localGraph1.bounds();
/* 659 */         int i1 = localRectangle.x;
/* 660 */         int i2 = localRectangle.y;
/* 661 */         int i3 = i1 + localRectangle.width - 1;
/* 662 */         int i4 = i2 + localRectangle.height - 1;
/* 663 */         if ((i1 > -8) || (i3 < 8) || 
/* 664 */           (i2 > -8) || (i4 < 8)) {
/* 665 */           addProblem("There should be at least one point on or outside\nthe rectangle in the middle in each direction.\n");
/*     */         }
/*     */         
/* 668 */         if (localRectangle.width + localRectangle.height < 38) {
/* 669 */           addProblem("The ship is not big enough. The ship's width and\nheight added together should be at least " + 
/*     */           
/* 671 */             38 + ".\n" + 
/* 672 */             "Current width (" + localRectangle.height + ") plus height (" + 
/* 673 */             localRectangle.width + ") is only " + (
/* 674 */             localRectangle.width + localRectangle.height));
/*     */         }
/*     */       }
/*     */       
/*     */ 
/*     */ 
/* 680 */       if ((this.checkProblems) && (Preferences.checkGeometry))
/*     */       {
/* 682 */         for (i = 0; i < this.items.countItems(); i++)
/*     */         {
/* 684 */           Item localItem = this.items.item(i);
/* 685 */           if (!localGraph1.inside(localItem.x, localItem.y)) {
/* 686 */             addProblem(localItem.getName() + " at (" + localItem.x + ", " + 
/* 687 */               -localItem.y + ") doesn't lie inside ship borders.");
/*     */           }
/*     */         }
/*     */       }
/*     */       
/* 692 */       localGraph1.removeDoubles();
/*     */     }
/*     */     
/*     */ 
/* 696 */     for (int k = 0; k <= 8; k++)
/*     */     {
/* 698 */       Equipment localEquipment = this.items.getItemsOfType(k);
/* 699 */       m = localEquipment.countItems();
/* 700 */       if (m > 0)
/*     */       {
/* 702 */         str = str + "(" + Item.getShortFileId(k) + ":";
/* 703 */         for (i = 0; i < localEquipment.countItems(); i++)
/* 704 */           str = str + " " + localEquipment.item(i).x + "," + -localEquipment.item(i).y;
/* 705 */         str = str + ")";
/*     */       }
/*     */       
/* 708 */       if (Preferences.checkMinMax)
/*     */       {
/* 710 */         if (m < Item.getMin(k))
/* 711 */           addProblem("Too few (" + m + ") " + Item.getName(k) + 
/* 712 */             "'s, minimum is " + Item.getMin(k));
/* 713 */         if (m > Item.getMax(k)) {
/* 714 */           addProblem("Too many (" + m + ") " + Item.getName(k) + 
/* 715 */             "'s, maximum is " + Item.getMax(k));
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 724 */     return str;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private void addProblem(String paramString)
/*     */   {
/* 732 */     if (this.checkProblems) {
/* 733 */       this.problemsVector.addElement(paramString);
/*     */     }
/*     */   }
/*     */   
/*     */   public String toString() {
/* 738 */     return this.name + " (" + super.toString() + ")";
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\Ship.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */