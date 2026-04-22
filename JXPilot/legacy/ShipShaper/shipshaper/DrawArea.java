/*     */ package shipshaper;
/*     */ 
/*     */ import java.awt.Color;
/*     */ import java.awt.Component;
/*     */ import java.awt.Cursor;
/*     */ import java.awt.Dimension;
/*     */ import java.awt.Graphics;
/*     */ import java.awt.Image;
/*     */ import java.awt.Panel;
/*     */ import java.awt.Rectangle;
/*     */ import java.awt.event.InputEvent;
/*     */ import java.awt.event.MouseEvent;
/*     */ import java.awt.event.MouseListener;
/*     */ import java.awt.event.MouseMotionListener;
/*     */ import shipshaper.graph.Edge;
/*     */ import shipshaper.graph.Graph;
/*     */ import shipshaper.graph.Vertex;
/*     */ import shipshaper.tools.MoveTool;
/*     */ import shipshaper.tools.Tool;
/*     */ 
/*     */ public final class DrawArea
/*     */   extends Panel
/*     */   implements MouseListener, MouseMotionListener, ShipListener
/*     */ {
/*     */   Ship ship;
/*     */   ShipEditor shipEditor;
/*     */   PositionView positionView;
/*     */   Tool tool;
/*     */   Tool activeTool;
/*     */   Vertex selectedVertex;
/*     */   Edge selectedEdge;
/*     */   Item selectedItem;
/*     */   Dimension daSize;
/*     */   Dimension daCenter;
/*     */   Dimension gridSize;
/*  36 */   final int daBorder = 5;
/*  37 */   final int nearDist = 6;
/*     */   
/*     */   boolean mouseInside;
/*     */   int mx;
/*     */   int my;
/*  42 */   int oldmx = -1;
/*  43 */   int oldmy = -1;
/*     */   
/*     */   Image backImage;
/*     */   
/*     */   Graphics backG;
/*     */   
/*     */   Image offImage;
/*     */   
/*     */   Dimension offSize;
/*     */   Graphics offG;
/*     */   
/*     */   DrawArea(ShipEditor paramShipEditor, PositionView paramPositionView)
/*     */   {
/*  56 */     this.shipEditor = paramShipEditor;
/*  57 */     this.positionView = paramPositionView;
/*     */     
/*  59 */     addMouseListener(this);
/*  60 */     addMouseMotionListener(this);
/*     */     
/*  62 */     setBackground(Color.lightGray);
/*  63 */     setForeground(Color.black);
/*  64 */     setCursor(new Cursor(1));
/*     */     
/*  66 */     this.daSize = new Dimension();
/*  67 */     this.daCenter = new Dimension();
/*  68 */     this.gridSize = new Dimension();
/*  69 */     this.offSize = new Dimension();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   DrawArea(ShipEditor paramShipEditor, PositionView paramPositionView, Ship paramShip)
/*     */   {
/*  76 */     this(paramShipEditor, paramPositionView);
/*  77 */     setShip(paramShip);
/*     */   }
/*     */   
/*     */   public void setShip(Ship paramShip)
/*     */   {
/*  82 */     this.ship = paramShip;
/*  83 */     if (paramShip != null) {
/*  84 */       paramShip.addShipListener(this);
/*     */     }
/*  86 */     repaint();
/*     */   }
/*     */   
/*     */   public Ship getShip()
/*     */   {
/*  91 */     return this.ship;
/*     */   }
/*     */   
/*     */   public void shipChanged(ShipEvent paramShipEvent)
/*     */   {
/*  96 */     if ((paramShipEvent.shapeChanged()) || (paramShipEvent.itemsChanged())) {
/*  97 */       repaint();
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private int dir(int paramInt)
/*     */   {
/* 107 */     if (paramInt >= 0) return 1; return -1;
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean isNear(int paramInt1, int paramInt2, int paramInt3)
/*     */   {
/* 113 */     return (paramInt1 > paramInt2 - paramInt3) && (paramInt1 < paramInt2 + paramInt3);
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean inRange(int paramInt1, int paramInt2, int paramInt3)
/*     */   {
/* 119 */     return ((paramInt1 >= paramInt2) && (paramInt1 <= paramInt3)) || ((paramInt1 >= paramInt3) && (paramInt1 <= paramInt2));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private int toPCX(int paramInt)
/*     */   {
/* 129 */     return this.daCenter.width + paramInt * this.gridSize.width;
/*     */   }
/*     */   
/*     */ 
/*     */   private int toPCY(int paramInt)
/*     */   {
/* 135 */     return this.daCenter.height + paramInt * this.gridSize.height;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private int toSCX(int paramInt)
/*     */   {
/* 144 */     int i = (paramInt - this.daCenter.width + dir(paramInt - this.daCenter.width) * 
/* 145 */       this.gridSize.width / 2) / this.gridSize.width;
/* 146 */     if (i > 15)
/* 147 */       i = 15;
/* 148 */     if (i < -15)
/* 149 */       i = -15;
/* 150 */     return i;
/*     */   }
/*     */   
/*     */ 
/*     */   private int toSCY(int paramInt)
/*     */   {
/* 156 */     int i = (paramInt - this.daCenter.height + dir(paramInt - this.daCenter.height) * 
/* 157 */       this.gridSize.height / 2) / this.gridSize.height;
/* 158 */     if (i > 15)
/* 159 */       i = 15;
/* 160 */     if (i < -15)
/* 161 */       i = -15;
/* 162 */     return i;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean vertexNear(Vertex paramVertex, int paramInt1, int paramInt2)
/*     */   {
/* 172 */     return (isNear(toPCX(paramVertex.x), paramInt1, 6)) && 
/* 173 */       (isNear(toPCY(paramVertex.y), paramInt2, 6));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Vertex vertexAtPC(int paramInt1, int paramInt2)
/*     */   {
/* 181 */     for (int i = 0; i < this.ship.shape.countVertices(); i++)
/*     */     {
/* 183 */       if (vertexNear(this.ship.shape.vertex(i), paramInt1, paramInt2))
/* 184 */         return this.ship.shape.vertex(i);
/*     */     }
/* 186 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */   public Vertex vertexAtSC(int paramInt1, int paramInt2)
/*     */   {
/* 192 */     return vertexAtPC(toPCX(paramInt1), toPCY(paramInt2));
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean edgeNear(Edge paramEdge, int paramInt1, int paramInt2)
/*     */   {
/* 198 */     int i = toPCX(paramEdge.v1.x);
/* 199 */     int j = toPCY(paramEdge.v1.y);
/* 200 */     int k = toPCX(paramEdge.v2.x);
/* 201 */     int m = toPCY(paramEdge.v2.y);
/*     */     
/*     */ 
/* 204 */     if (i == k)
/* 205 */       return (isNear(paramInt1, i, 6)) && (inRange(paramInt2, j, m));
/* 206 */     if (j == m) {
/* 207 */       return (isNear(paramInt2, j, 6)) && (inRange(paramInt1, i, k));
/*     */     }
/*     */     
/* 210 */     Rectangle localRectangle = new Rectangle(Math.min(i, k), 
/* 211 */       Math.min(j, m), 
/* 212 */       Math.abs(k - i) + 1, 
/* 213 */       Math.abs(m - j) + 1);
/* 214 */     if (!localRectangle.contains(paramInt1, paramInt2)) {
/* 215 */       return false;
/*     */     }
/*     */     
/*     */ 
/* 219 */     float f = (paramInt1 - i) / (k - i);
/*     */     
/*     */ 
/* 222 */     int n = j + (int)(f * (m - j));
/*     */     
/*     */ 
/* 225 */     if ((paramInt2 < n + 6) && (paramInt2 > n - 6)) {
/* 226 */       return true;
/*     */     }
/* 228 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Edge edgeAtPC(int paramInt1, int paramInt2)
/*     */   {
/* 236 */     for (int i = 0; i < this.ship.shape.countEdges(); i++)
/*     */     {
/* 238 */       if (edgeNear(this.ship.shape.edge(i), paramInt1, paramInt2))
/* 239 */         return this.ship.shape.edge(i);
/*     */     }
/* 241 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */   public Edge edgeAtSC(int paramInt1, int paramInt2)
/*     */   {
/* 247 */     return edgeAtPC(toPCX(paramInt1), toPCY(paramInt2));
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean itemNear(Item paramItem, int paramInt1, int paramInt2)
/*     */   {
/* 253 */     return (isNear(toPCX(paramItem.x), paramInt1, 6)) && 
/* 254 */       (isNear(toPCY(paramItem.y), paramInt2, 6));
/*     */   }
/*     */   
/*     */ 
/*     */   public Item itemAtSC(int paramInt1, int paramInt2)
/*     */   {
/* 260 */     return itemAtPC(toPCX(paramInt1), toPCY(paramInt2));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Item itemAtPC(int paramInt1, int paramInt2)
/*     */   {
/* 268 */     for (int i = 0; i < this.ship.items.countItems(); i++)
/*     */     {
/* 270 */       if (itemNear(this.ship.items.item(i), paramInt1, paramInt2))
/* 271 */         return this.ship.items.item(i);
/*     */     }
/* 273 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void selectVertex(Vertex paramVertex)
/*     */   {
/* 283 */     this.selectedVertex = paramVertex;
/*     */   }
/*     */   
/*     */   public Vertex getSelectedVertex()
/*     */   {
/* 288 */     return this.selectedVertex;
/*     */   }
/*     */   
/*     */ 
/*     */   public void selectVertexAtPC(int paramInt1, int paramInt2)
/*     */   {
/* 294 */     Vertex localVertex = vertexAtPC(paramInt1, paramInt2);
/* 295 */     if (localVertex != null) {
/* 296 */       selectVertex(localVertex);
/*     */     }
/*     */   }
/*     */   
/*     */   public void selectEdge(Edge paramEdge)
/*     */   {
/* 302 */     this.selectedEdge = paramEdge;
/*     */   }
/*     */   
/*     */   public Edge getSelectedEdge()
/*     */   {
/* 307 */     return this.selectedEdge;
/*     */   }
/*     */   
/*     */ 
/*     */   public void selectEdgeAtPC(int paramInt1, int paramInt2)
/*     */   {
/* 313 */     Edge localEdge = edgeAtPC(paramInt1, paramInt2);
/* 314 */     if (localEdge != null) {
/* 315 */       selectEdge(localEdge);
/*     */     }
/*     */   }
/*     */   
/*     */   public void selectItem(Item paramItem)
/*     */   {
/* 321 */     this.selectedItem = paramItem;
/*     */   }
/*     */   
/*     */   public Item getSelectedItem()
/*     */   {
/* 326 */     return this.selectedItem;
/*     */   }
/*     */   
/*     */ 
/*     */   public void selectItemAtPC(int paramInt1, int paramInt2)
/*     */   {
/* 332 */     Item localItem = itemAtPC(paramInt1, paramInt2);
/* 333 */     if (localItem != null) {
/* 334 */       selectItem(localItem);
/*     */     }
/*     */   }
/*     */   
/*     */   public void unselectAll()
/*     */   {
/* 340 */     this.selectedVertex = null;
/* 341 */     this.selectedEdge = null;
/* 342 */     this.selectedItem = null;
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean anySelected()
/*     */   {
/* 348 */     return (this.selectedVertex != null) || (this.selectedEdge != null) || 
/* 349 */       (this.selectedItem != null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setTool(Tool paramTool)
/*     */   {
/* 358 */     this.tool = paramTool;
/*     */   }
/*     */   
/*     */   public void toolFinished()
/*     */   {
/* 363 */     unselectAll();
/* 364 */     this.activeTool = null;
/* 365 */     repaint();
/*     */   }
/*     */   
/*     */   public void mouseClicked(MouseEvent paramMouseEvent) {}
/*     */   
/*     */   public void mouseReleased(MouseEvent paramMouseEvent) {}
/*     */   
/*     */   public void mouseDragged(MouseEvent paramMouseEvent) {}
/*     */   
/*     */   public void mousePressed(MouseEvent paramMouseEvent)
/*     */   {
/* 376 */     if ((this.ship == null) || (this.tool == null)) {
/* 377 */       return;
/*     */     }
/* 379 */     newMouseCoords(paramMouseEvent);
/*     */     
/* 381 */     if (paramMouseEvent.isMetaDown())
/*     */     {
/*     */ 
/*     */ 
/* 385 */       if ((this.activeTool != null) && (this.activeTool.isActive())) {
/* 386 */         this.activeTool.end();
/*     */ 
/*     */       }
/*     */       else
/*     */       {
/* 391 */         this.activeTool = new MoveTool(this);
/* 392 */         this.activeTool.mousePress(this.mx, this.my);
/*     */       }
/*     */       
/*     */     }
/*     */     else
/*     */     {
/* 398 */       this.activeTool = this.tool;
/* 399 */       this.activeTool.mousePress(this.mx, this.my);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public void mouseMoved(MouseEvent paramMouseEvent)
/*     */   {
/* 406 */     if ((this.ship == null) || (this.tool == null)) {
/* 407 */       return;
/*     */     }
/* 409 */     int i = paramMouseEvent.getX();
/* 410 */     int j = paramMouseEvent.getY();
/*     */     
/* 412 */     newMouseCoords(paramMouseEvent);
/* 413 */     this.positionView.coordsChanged(this.mx, this.my);
/*     */     
/* 415 */     if ((this.activeTool != null) && (this.activeTool.isActive())) {
/* 416 */       this.activeTool.mouseMove(this.mx, this.my);
/*     */     }
/*     */     else {
/* 419 */       unselectAll();
/* 420 */       if (this.tool.wantVertex())
/* 421 */         selectVertexAtPC(i, j);
/* 422 */       if ((this.tool.wantEdge()) && (!anySelected()))
/* 423 */         selectEdgeAtPC(i, j);
/* 424 */       if ((this.tool.wantItem()) && (!anySelected())) {
/* 425 */         selectItemAtPC(i, j);
/*     */       }
/*     */       
/* 428 */       repaint();
/*     */     }
/*     */   }
/*     */   
/*     */   public void mouseEntered(MouseEvent paramMouseEvent)
/*     */   {
/* 434 */     if ((this.ship == null) || (this.tool == null)) {
/* 435 */       return;
/*     */     }
/* 437 */     this.mouseInside = true;
/* 438 */     newMouseCoords(paramMouseEvent);
/* 439 */     repaint();
/*     */   }
/*     */   
/*     */   public void mouseExited(MouseEvent paramMouseEvent)
/*     */   {
/* 444 */     if ((this.ship == null) || (this.tool == null)) {
/* 445 */       return;
/*     */     }
/* 447 */     this.mouseInside = false;
/* 448 */     newMouseCoords(paramMouseEvent);
/* 449 */     this.positionView.coordsNotAvailable();
/* 450 */     if ((this.activeTool != null) && (this.activeTool.isActive()))
/* 451 */       this.activeTool.end();
/* 452 */     repaint();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private boolean newMouseCoords(MouseEvent paramMouseEvent)
/*     */   {
/* 463 */     if (this.daSize.width == 0) {
/* 464 */       return false;
/*     */     }
/* 466 */     this.mx = toSCX(paramMouseEvent.getX());
/* 467 */     this.my = toSCY(paramMouseEvent.getY());
/*     */     
/*     */ 
/*     */ 
/* 471 */     if ((this.oldmx == this.mx) && (this.oldmy == this.my))
/* 472 */       return false;
/* 473 */     this.oldmx = this.mx;
/* 474 */     this.oldmy = this.my;
/*     */     
/* 476 */     return true;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void drawGrid(Graphics paramGraphics)
/*     */   {
/* 488 */     paramGraphics.setColor(Colors.GRID);
/* 489 */     for (int j = -15; j <= 15; j++) {
/* 490 */       for (int i = -15; i <= 15; i++) {
/* 491 */         paramGraphics.drawLine(toPCX(i), toPCY(j), toPCX(i), toPCY(j));
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void createBackground(Graphics paramGraphics) {
/* 497 */     paramGraphics.setColor(getBackground());
/* 498 */     paramGraphics.fillRect(0, 0, this.daSize.width, this.daSize.height);
/*     */     
/*     */ 
/* 501 */     drawGrid(paramGraphics);
/*     */     
/*     */ 
/* 504 */     paramGraphics.setColor(Colors.HELP_LINES);
/* 505 */     paramGraphics.drawLine(toPCX(-15), toPCY(0), 
/* 506 */       toPCX(15), toPCY(0));
/* 507 */     paramGraphics.drawLine(toPCX(0), toPCY(-15), 
/* 508 */       toPCX(0), toPCY(15));
/* 509 */     paramGraphics.drawRect(toPCX(-8), 
/* 510 */       toPCY(-8), 
/* 511 */       16 * this.gridSize.width, 
/* 512 */       16 * this.gridSize.height);
/*     */     
/*     */ 
/*     */ 
/* 516 */     if (Preferences.verticalDraw)
/*     */     {
/*     */ 
/* 519 */       paramGraphics.drawLine(toPCX(-1), toPCY(-14), 
/* 520 */         toPCX(0), toPCY(-15));
/* 521 */       paramGraphics.drawLine(toPCX(1), toPCY(-14), 
/* 522 */         toPCX(0), toPCY(-15));
/*     */ 
/*     */     }
/*     */     else
/*     */     {
/* 527 */       paramGraphics.drawLine(toPCX(14), toPCY(-1), 
/* 528 */         toPCX(15), toPCY(0));
/* 529 */       paramGraphics.drawLine(toPCX(14), toPCY(1), 
/* 530 */         toPCX(15), toPCY(0));
/*     */     }
/*     */     
/*     */ 
/* 534 */     paramGraphics.setColor(Color.lightGray);
/* 535 */     paramGraphics.draw3DRect(0, 0, this.daSize.width, this.daSize.height, false);
/* 536 */     paramGraphics.draw3DRect(1, 1, this.daSize.width - 2, this.daSize.height - 2, false);
/*     */   }
/*     */   
/*     */ 
/*     */   public void drawBackground(Graphics paramGraphics)
/*     */   {
/* 542 */     paramGraphics.drawImage(this.backImage, 0, 0, this);
/*     */   }
/*     */   
/*     */ 
/*     */   private void drawVertex(Graphics paramGraphics, Vertex paramVertex)
/*     */   {
/* 548 */     if (paramVertex == this.selectedVertex)
/*     */     {
/* 550 */       paramGraphics.setColor(Colors.VERTEX_SELECTED);
/* 551 */       paramGraphics.fillOval(toPCX(paramVertex.x) - 3, toPCY(paramVertex.y) - 3, 6, 6);
/*     */     }
/*     */     else
/*     */     {
/* 555 */       if (paramVertex.valency() % 2 == 0) {
/* 556 */         paramGraphics.setColor(Colors.VERTEX_EVEN);
/*     */       } else
/* 558 */         paramGraphics.setColor(Colors.VERTEX_ODD);
/* 559 */       paramGraphics.fillOval(toPCX(paramVertex.x) - 2, toPCY(paramVertex.y) - 2, 4, 4);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private void drawMarker(Graphics paramGraphics)
/*     */   {
/* 566 */     if (this.mouseInside)
/*     */     {
/* 568 */       paramGraphics.setColor(Colors.VERTEX_MARKER);
/* 569 */       paramGraphics.drawOval(toPCX(this.mx) - 3, toPCY(this.my) - 3, 6, 6);
/* 570 */       if (Preferences.showMirrorMarker)
/*     */       {
/* 572 */         if (Preferences.verticalDraw) {
/* 573 */           paramGraphics.drawOval(toPCX(-this.mx) - 3, toPCY(this.my) - 3, 6, 6);
/*     */         } else {
/* 575 */           paramGraphics.drawOval(toPCX(this.mx) - 3, toPCY(-this.my) - 3, 6, 6);
/*     */         }
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private void drawEdge(Graphics paramGraphics, Edge paramEdge)
/*     */   {
/* 584 */     if (paramEdge == this.selectedEdge) {
/* 585 */       paramGraphics.setColor(Colors.EDGE_SELECTED);
/*     */     } else {
/* 587 */       paramGraphics.setColor(Colors.EDGE);
/*     */     }
/* 589 */     paramGraphics.drawLine(toPCX(paramEdge.v1.x), toPCY(paramEdge.v1.y), toPCX(paramEdge.v2.x), toPCY(paramEdge.v2.y));
/* 590 */     drawVertex(paramGraphics, paramEdge.v1);
/* 591 */     drawVertex(paramGraphics, paramEdge.v2);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private void drawAllEdges(Graphics paramGraphics)
/*     */   {
/* 598 */     for (int i = 0; i < this.ship.shape.countEdges(); i++) {
/* 599 */       drawEdge(paramGraphics, this.ship.shape.edge(i));
/*     */     }
/*     */   }
/*     */   
/*     */   private void drawItem(Graphics paramGraphics, Item paramItem)
/*     */   {
/* 605 */     if (paramItem == this.selectedItem) {
/* 606 */       paramGraphics.setColor(Colors.ITEM_SELECTED);
/*     */     } else {
/* 608 */       paramGraphics.setColor(Colors.ITEM);
/*     */     }
/* 610 */     paramItem.draw(paramGraphics, !Preferences.verticalDraw, 
/* 611 */       toPCX(paramItem.x), toPCY(paramItem.y), 
/* 612 */       this.gridSize.width * 3 / 2, this.gridSize.height * 3 / 2);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private void drawAllItems(Graphics paramGraphics)
/*     */   {
/* 619 */     for (int i = 0; i < this.ship.items.countItems(); i++) {
/* 620 */       drawItem(paramGraphics, this.ship.items.item(i));
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public void update(Graphics paramGraphics)
/*     */   {
/* 627 */     if (!this.daSize.equals(getSize()))
/*     */     {
/* 629 */       int i = getSize().width;
/* 630 */       int j = getSize().height;
/*     */       
/* 632 */       this.daSize.setSize(i, j);
/* 633 */       this.daCenter.setSize(i / 2, j / 2);
/* 634 */       this.gridSize.setSize((this.daSize.width - 10) / 
/* 635 */         31, 
/* 636 */         (this.daSize.height - 10) / 
/* 637 */         31);
/*     */     }
/*     */     
/*     */ 
/* 641 */     if ((this.offImage == null) || (!this.offSize.equals(this.daSize)))
/*     */     {
/* 643 */       this.offImage = createImage(this.daSize.width, this.daSize.height);
/* 644 */       this.offSize.setSize(this.daSize);
/* 645 */       this.offG = this.offImage.getGraphics();
/*     */       
/* 647 */       this.backImage = createImage(this.daSize.width, this.daSize.height);
/* 648 */       createBackground(this.backImage.getGraphics());
/*     */     }
/*     */     
/*     */ 
/* 652 */     drawBackground(this.offG);
/* 653 */     if (this.ship != null)
/*     */     {
/* 655 */       drawAllEdges(this.offG);
/* 656 */       drawAllItems(this.offG);
/* 657 */       drawMarker(this.offG);
/*     */     }
/*     */     
/*     */ 
/* 661 */     paint(paramGraphics);
/*     */   }
/*     */   
/*     */ 
/*     */   public void paint(Graphics paramGraphics)
/*     */   {
/* 667 */     if ((this.offImage == null) || (!this.offSize.equals(getSize()))) {
/* 668 */       repaint();
/*     */     } else {
/* 670 */       paramGraphics.drawImage(this.offImage, 0, 0, this);
/*     */     }
/*     */   }
/*     */   
/*     */   public void repaintAll() {
/* 675 */     if (this.backImage != null)
/* 676 */       createBackground(this.backImage.getGraphics());
/* 677 */     repaint();
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\DrawArea.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */