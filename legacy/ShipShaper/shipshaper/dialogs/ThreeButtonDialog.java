/*    */ package shipshaper.dialogs;
/*    */ 
/*    */ import java.awt.Component;
/*    */ import java.awt.Frame;
/*    */ import java.awt.Window;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class ThreeButtonDialog
/*    */   extends MessageDialog
/*    */ {
/*    */   public ThreeButtonDialog(Frame paramFrame, String paramString1, String paramString2, String paramString3, String paramString4)
/*    */   {
/* 16 */     this(paramFrame, paramString1, paramString2, paramString3, paramString4, null);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */   public ThreeButtonDialog(Frame paramFrame, String paramString1, String paramString2, String paramString3, String paramString4, Component paramComponent)
/*    */   {
/* 23 */     super(paramFrame, paramString1, paramComponent);
/* 24 */     addButton(paramString2, this);
/* 25 */     addButton(paramString3, this);
/* 26 */     addButton(paramString4, this);
/*    */     
/* 28 */     pack();
/* 29 */     setVisible(true);
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\dialogs\ThreeButtonDialog.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */