/*     */ package shipshaper.graph;
/*     */ 
/*     */ import java.awt.Rectangle;
/*     */ import java.io.PrintStream;
/*     */ import java.util.Vector;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class Graph
/*     */ {
/*  17 */   private boolean DEBUG = false;
/*     */   
/*     */ 
/*     */ 
/*  21 */   private Vector edges = new Vector();
/*  22 */   private Vector vertices = new Vector();
/*     */   
/*     */   public Graph() {}
/*     */   
/*     */   public Graph(Graph paramGraph) {
/*  27 */     this();
/*     */     
/*     */ 
/*  30 */     for (int i = 0; i < paramGraph.countEdges(); i++)
/*  31 */       add(new Edge(paramGraph.edge(i)));
/*  32 */     removeDoubles();
/*     */   }
/*     */   
/*     */   public Vector getVertices()
/*     */   {
/*  37 */     return this.vertices;
/*     */   }
/*     */   
/*     */   public Vector getEdges()
/*     */   {
/*  42 */     return this.edges;
/*     */   }
/*     */   
/*     */   public void add(Vertex paramVertex)
/*     */   {
/*  47 */     if (!contains(paramVertex)) {
/*  48 */       this.vertices.addElement(paramVertex);
/*     */     }
/*     */   }
/*     */   
/*     */   public void remove(Vertex paramVertex) {
/*  53 */     if (contains(paramVertex)) {
/*  54 */       this.vertices.removeElement(paramVertex);
/*     */     }
/*     */   }
/*     */   
/*     */   public void add(Edge paramEdge) {
/*  59 */     if (!contains(paramEdge))
/*     */     {
/*  61 */       this.edges.addElement(paramEdge);
/*  62 */       add(paramEdge.v1);
/*  63 */       add(paramEdge.v2);
/*     */     }
/*     */   }
/*     */   
/*     */   public void remove(Edge paramEdge)
/*     */   {
/*  69 */     if (contains(paramEdge))
/*     */     {
/*  71 */       if (paramEdge.v1.valency() == 1)
/*  72 */         this.vertices.removeElement(paramEdge.v1);
/*  73 */       paramEdge.v1.removeEdge(paramEdge);
/*  74 */       if (paramEdge.v2.valency() == 1)
/*  75 */         this.vertices.removeElement(paramEdge.v2);
/*  76 */       paramEdge.v2.removeEdge(paramEdge);
/*  77 */       this.edges.removeElement(paramEdge);
/*     */     }
/*     */   }
/*     */   
/*     */   public void add(Graph paramGraph)
/*     */   {
/*  83 */     for (int i = 0; i < paramGraph.countEdges(); i++) {
/*  84 */       add(paramGraph.edge(i));
/*     */     }
/*     */   }
/*     */   
/*     */   public void remove(Graph paramGraph) {
/*  89 */     for (int i = 0; i < paramGraph.countEdges(); i++) {
/*  90 */       remove(paramGraph.edge(i));
/*     */     }
/*     */   }
/*     */   
/*     */   public boolean contains(Vertex paramVertex) {
/*  95 */     return this.vertices.contains(paramVertex);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean contains(Edge paramEdge)
/*     */   {
/* 106 */     return this.edges.contains(paramEdge);
/*     */   }
/*     */   
/*     */   public int countVertices()
/*     */   {
/* 111 */     return this.vertices.size();
/*     */   }
/*     */   
/*     */   public int countEdges()
/*     */   {
/* 116 */     return this.edges.size();
/*     */   }
/*     */   
/*     */ 
/*     */   public int valency(Vertex paramVertex)
/*     */   {
/* 122 */     int i = 0;
/* 123 */     for (int j = 0; j < countEdges(); j++)
/* 124 */       if (edge(j).hasVertex(paramVertex))
/* 125 */         i++;
/* 126 */     return i;
/*     */   }
/*     */   
/*     */   public Vertex vertex(int paramInt)
/*     */   {
/* 131 */     return (Vertex)this.vertices.elementAt(paramInt);
/*     */   }
/*     */   
/*     */   public Edge edge(int paramInt)
/*     */   {
/* 136 */     return (Edge)this.edges.elementAt(paramInt);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public boolean inside(int paramInt1, int paramInt2)
/*     */   {
/* 143 */     return true;
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean isConnected()
/*     */   {
/* 149 */     int i = countEdges();
/* 150 */     if (i < 2) {
/* 151 */       return true;
/*     */     }
/* 153 */     Graph localGraph = getGraphConnectedTo(edge(0));
/* 154 */     if (localGraph.countEdges() != i) {
/* 155 */       return false;
/*     */     }
/* 157 */     return true;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public Graph getGraphConnectedTo(Edge paramEdge)
/*     */   {
/* 164 */     Graph localGraph = new Graph();
/* 165 */     Vector localVector = new Vector();
/*     */     
/*     */ 
/*     */ 
/* 169 */     localVector.addElement(paramEdge);
/*     */     
/* 171 */     while (localVector.size() > 0)
/*     */     {
/* 173 */       Edge localEdge = (Edge)localVector.elementAt(0);
/* 174 */       localVector.removeElementAt(0);
/* 175 */       localGraph.add(localEdge);
/* 176 */       int i; if (localEdge.v1.valency() > 1)
/* 177 */         for (i = 0; i < localEdge.v1.valency(); i++)
/* 178 */           if ((localEdge.v1.edge(i) != localEdge) && (!localGraph.contains(localEdge.v1.edge(i))))
/* 179 */             localVector.addElement(localEdge.v1.edge(i));
/* 180 */       if (localEdge.v2.valency() > 1)
/* 181 */         for (i = 0; i < localEdge.v2.valency(); i++)
/* 182 */           if ((localEdge.v2.edge(i) != localEdge) && (!localGraph.contains(localEdge.v2.edge(i))))
/* 183 */             localVector.addElement(localEdge.v2.edge(i));
/*     */     }
/* 185 */     return localGraph;
/*     */   }
/*     */   
/*     */ 
/*     */   public Vector getIslands()
/*     */   {
/* 191 */     Vector localVector1 = new Vector();
/* 192 */     Vector localVector2 = new Vector(countEdges());
/*     */     
/*     */ 
/*     */ 
/* 196 */     for (int i = 0; i < countEdges(); i++) {
/* 197 */       if (!localVector2.contains(edge(i)))
/*     */       {
/* 199 */         Graph localGraph = getGraphConnectedTo(edge(i));
/* 200 */         localVector1.addElement(localGraph);
/* 201 */         for (int j = 0; j < localGraph.countEdges(); j++)
/* 202 */           localVector2.addElement(localGraph.edge(j));
/*     */       }
/*     */     }
/* 205 */     return localVector1;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Vector findEulerPath()
/*     */   {
/* 254 */     Vector localVector1 = new Vector();
/* 255 */     Vector localVector2 = (Vector)this.edges.clone();
/*     */     
/*     */ 
/* 258 */     Vertex localVertex = vertex(0);
/*     */     
/* 260 */     debug("FindEulerPath:\nStarting at " + localVertex);
/*     */     
/* 262 */     walkPath(scoutPath(localVertex, localVertex, localVector2), 
/* 263 */       localVector2, 
/* 264 */       localVector1);
/*     */     
/*     */ 
/* 267 */     localVector1.removeElementAt(localVector1.size() - 1);
/*     */     
/* 269 */     return localVector1;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public int findEulerPathLength()
/*     */   {
/* 277 */     Graph localGraph = new Graph(this);
/* 278 */     localGraph.makeAllVerticesEven();
/* 279 */     return localGraph.countEdges();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private Vector walkPath(Vector paramVector1, Vector paramVector2, Vector paramVector3)
/*     */   {
/* 291 */     for (int i = 0; i < paramVector1.size(); i++)
/*     */     {
/* 293 */       Vertex localVertex = (Vertex)paramVector1.elementAt(i);
/*     */       
/*     */ 
/* 296 */       paramVector3.addElement(localVertex);
/* 297 */       debug("Walking to " + localVertex);
/*     */       
/*     */ 
/*     */ 
/* 301 */       for (int j = 0; j < localVertex.valency(); j++)
/*     */       {
/* 303 */         if (paramVector2.contains(localVertex.edge(j)))
/*     */         {
/*     */ 
/* 306 */           paramVector2.removeElement(localVertex.edge(j));
/* 307 */           Vector localVector = scoutPath(localVertex.oppositeVertex(j), localVertex, paramVector2);
/*     */           
/*     */ 
/* 310 */           walkPath(localVector, paramVector2, paramVector3);
/*     */         }
/*     */       }
/*     */     }
/*     */     
/* 315 */     return paramVector3;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private Vector scoutPath(Vertex paramVertex1, Vertex paramVertex2, Vector paramVector)
/*     */   {
/* 327 */     Vector localVector = new Vector();
/*     */     
/*     */ 
/*     */ 
/* 331 */     Vertex localVertex = paramVertex1;
/* 332 */     int j = 0;
/*     */     
/*     */     int k;
/*     */     
/*     */     do
/*     */     {
/* 338 */       localVector.addElement(localVertex);
/* 339 */       debug("Scouting " + localVertex);
/*     */       
/*     */ 
/* 342 */       if ((localVertex == paramVertex2) && (j != 0)) {
/*     */         break;
/*     */       }
/*     */       
/* 346 */       k = 0;
/* 347 */       for (int i = 0; i < localVertex.valency(); i++)
/*     */       {
/* 349 */         if (paramVector.contains(localVertex.edge(i)))
/*     */         {
/*     */ 
/*     */ 
/*     */ 
/* 354 */           paramVector.removeElement(localVertex.edge(i));
/* 355 */           localVertex = localVertex.oppositeVertex(i);
/*     */           
/* 357 */           j = 1;
/* 358 */           k = 1;
/* 359 */           break;
/*     */         }
/*     */         
/*     */       }
/* 363 */     } while (k != 0);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 369 */     debug("Couldn't find any unmarked edges!");
/*     */     
/*     */ 
/*     */ 
/* 373 */     return localVector;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean eulerPathExists()
/*     */   {
/* 381 */     for (int i = 0; i < countVertices(); i++)
/* 382 */       if (vertex(i).valency() % 2 != 0)
/* 383 */         return false;
/* 384 */     return true;
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
/*     */   public int makeAllVerticesEven()
/*     */   {
/* 402 */     Object localObject = null;
/*     */     
/*     */ 
/*     */ 
/* 406 */     int k = 0;
/*     */     
/* 408 */     int i1 = 0;
/*     */     
/*     */ 
/*     */ 
/* 412 */     for (int i = 0; i < countVertices(); i++) {
/* 413 */       if (vertex(i).valency() == 1) {
/* 414 */         add(new Edge(vertex(i), vertex(i).oppositeVertex(0)));
/*     */       }
/*     */     }
/* 417 */     Vector localVector = new Vector();
/* 418 */     for (i = 0; i < countVertices(); i++) {
/* 419 */       if (vertex(i).valency() % 2 != 0)
/* 420 */         localVector.addElement(vertex(i));
/*     */     }
/* 422 */     if (this.DEBUG)
/*     */     {
/* 424 */       System.out.println("Odd vertices after adding an edge to all vertices with valency 1: ");
/*     */       
/* 426 */       for (i = 0; i < localVector.size(); i++) {
/* 427 */         System.out.println("   " + (Vertex)localVector.elementAt(i));
/*     */       }
/*     */     }
/*     */     int j;
/* 432 */     for (; 
/* 432 */         localVector.size() > 0; 
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 479 */         k != j)
/*     */     {
/* 435 */       Vertex localVertex1 = (Vertex)localVector.firstElement();
/* 436 */       j = this.vertices.indexOf(localVertex1);
/* 437 */       debug("Odd vertex: " + localVertex1);
/*     */       
/*     */ 
/* 440 */       int[][] arrayOfInt = findShortestPaths(localVertex1);
/* 441 */       int[] arrayOfInt1 = arrayOfInt[0];
/* 442 */       int[] arrayOfInt2 = arrayOfInt[1];
/*     */       
/* 444 */       if (this.DEBUG)
/*     */       {
/* 446 */         System.out.print("Distance to other vertices: ");
/* 447 */         for (i = 0; i < arrayOfInt1.length; i++)
/* 448 */           System.out.print(arrayOfInt1[i] + " ");
/* 449 */         System.out.println("");
/*     */       }
/*     */       
/*     */ 
/* 453 */       int n = 9999;
/* 454 */       for (i = 0; i < countVertices(); i++)
/*     */       {
/* 456 */         localVertex2 = (Vertex)this.vertices.elementAt(i);
/* 457 */         if ((localVector.contains(localVertex2)) && (localVertex2 != localVertex1))
/*     */         {
/* 459 */           if (arrayOfInt1[i] < n)
/*     */           {
/* 461 */             n = arrayOfInt1[i];
/* 462 */             localObject = localVertex2;
/* 463 */             k = i;
/*     */           }
/*     */         }
/*     */       }
/*     */       
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 472 */       localVector.removeElement(localVertex1);
/* 473 */       localVector.removeElement(localObject);
/*     */       
/* 475 */       debug("Building edges between " + localObject + " and " + localVertex1);
/*     */       
/*     */ 
/*     */ 
/* 479 */       continue;
/*     */       
/* 481 */       int m = arrayOfInt2[k];
/* 482 */       Vertex localVertex2 = (Vertex)this.vertices.elementAt(m);
/*     */       
/* 484 */       debug("Creating new edge from " + localObject + " to " + localVertex1);
/* 485 */       add(new Edge((Vertex)localObject, localVertex2));
/* 486 */       i1++;
/*     */       
/* 488 */       k = m;
/* 489 */       localObject = localVertex2;
/*     */     }
/*     */     
/*     */ 
/* 493 */     return i1;
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
/*     */   public int[][] findShortestPaths(Vertex paramVertex)
/*     */   {
/* 509 */     Object localObject = null;
/* 510 */     int n = 0;
/*     */     
/*     */ 
/* 513 */     int m = countVertices();
/* 514 */     int[] arrayOfInt1 = new int[m];
/* 515 */     int[] arrayOfInt2 = new int[m];
/* 516 */     for (int i = 0; i < m; i++)
/* 517 */       arrayOfInt1[i] = 100;
/* 518 */     int i2 = this.vertices.indexOf(paramVertex);
/* 519 */     arrayOfInt1[i2] = 0;
/* 520 */     Vector localVector = (Vector)this.vertices.clone();
/* 522 */     for (; 
/* 522 */         localVector.size() > 0; 
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
/* 540 */         i < ((Vertex)localObject).valency())
/*     */     {
/* 525 */       int j = 9999;
/* 526 */       for (i = 0; i < localVector.size(); i++)
/*     */       {
/* 528 */         Vertex localVertex2 = (Vertex)localVector.elementAt(i);
/* 529 */         int k = this.vertices.indexOf(localVertex2);
/* 530 */         if (arrayOfInt1[k] < j)
/*     */         {
/* 532 */           j = arrayOfInt1[k];
/* 533 */           n = k;
/* 534 */           localObject = localVertex2;
/*     */         }
/*     */       }
/* 537 */       localVector.removeElement(localObject);
/*     */       
/*     */ 
/* 540 */       i = 0; continue;
/*     */       
/* 542 */       Vertex localVertex1 = ((Vertex)localObject).oppositeVertex(i);
/* 543 */       int i1 = this.vertices.indexOf(localVertex1);
/*     */       
/*     */ 
/* 546 */       if (arrayOfInt1[i1] > arrayOfInt1[n] + 1)
/*     */       {
/* 548 */         arrayOfInt1[n] += 1;
/* 549 */         arrayOfInt2[i1] = n;
/*     */       }
/* 540 */       i++;
/*     */     }
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
/* 554 */     int[][] arrayOfInt = new int[2][];
/* 555 */     arrayOfInt[0] = arrayOfInt1;
/* 556 */     arrayOfInt[1] = arrayOfInt2;
/*     */     
/* 558 */     return arrayOfInt;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void removeDoubles()
/*     */   {
/* 568 */     debug("Removing doubles (edges = " + countEdges() + 
/* 569 */       ", vertices = " + countVertices());
/* 570 */     if (this.DEBUG) print();
/*     */     int j;
/*     */     Edge localEdge1;
/* 573 */     for (int i = 0; i < countVertices(); i++)
/*     */     {
/* 575 */       Vertex localVertex1 = vertex(i);
/*     */       
/* 577 */       j = i + 1;
/* 578 */       while (j < countVertices())
/*     */       {
/* 580 */         Vertex localVertex2 = vertex(j);
/*     */         
/* 582 */         if (localVertex1 == localVertex2) {
/* 583 */           debug("Internal error, v = vv!");
/*     */         }
/* 585 */         if (localVertex1.equals(localVertex2))
/*     */         {
/*     */ 
/*     */ 
/* 589 */           debug("Found double vertex at " + localVertex1);
/* 590 */           for (int k = 0; k < localVertex2.valency(); k++)
/*     */           {
/* 592 */             localEdge1 = localVertex2.edge(k);
/* 593 */             if (localEdge1.v1 == localVertex2) {
/* 594 */               localEdge1.v1 = localVertex1;
/*     */             } else
/* 596 */               localEdge1.v2 = localVertex1;
/* 597 */             localVertex1.addEdge(localEdge1);
/*     */           }
/* 599 */           remove(localVertex2);
/*     */         }
/*     */         else {
/* 602 */           j++;
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 610 */     for (i = 0; i < countEdges(); i++)
/*     */     {
/* 612 */       localEdge1 = edge(i);
/*     */       
/* 614 */       j = i + 1;
/* 615 */       while (j < countEdges())
/*     */       {
/* 617 */         Edge localEdge2 = edge(j);
/* 618 */         if (localEdge1.equals(localEdge2))
/*     */         {
/* 620 */           debug("Found double edge " + localEdge1);
/*     */           
/* 622 */           remove(localEdge2);
/*     */         }
/*     */         else {
/* 625 */           j++;
/*     */         }
/*     */       } }
/* 628 */     debug("Doubles removed (edges = " + countEdges() + 
/* 629 */       ", vertices = " + countVertices());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Rectangle bounds()
/*     */   {
/* 638 */     Rectangle localRectangle = new Rectangle(0, 0, 0, 0);
/* 639 */     for (int i = 0; i < countEdges(); i++) {
/* 640 */       localRectangle = localRectangle.union(edge(i).bounds());
/*     */     }
/* 642 */     return localRectangle;
/*     */   }
/*     */   
/*     */   public void debug(String paramString)
/*     */   {
/* 647 */     if (this.DEBUG) {
/* 648 */       System.out.println(paramString);
/*     */     }
/*     */   }
/*     */   
/*     */   public void print()
/*     */   {
/* 654 */     System.out.println("***** Vertices:");
/* 655 */     for (int i = 0; i < countVertices(); i++)
/* 656 */       vertex(i).print();
/* 657 */     System.out.println("***** Edges:");
/* 658 */     for (i = 0; i < countEdges(); i++) {
/* 659 */       edge(i).print();
/*     */     }
/*     */   }
/*     */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\graph\Graph.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */