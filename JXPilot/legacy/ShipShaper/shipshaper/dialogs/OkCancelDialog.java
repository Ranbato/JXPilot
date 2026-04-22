/*    */ package shipshaper.dialogs;
/*    */ 
/*    */ import java.awt.Component;
/*    */ import java.awt.Frame;
/*    */ import java.awt.Window;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class OkCancelDialog
/*    */   extends MessageDialog
/*    */ {
/*    */   public OkCancelDialog(Frame paramFrame, String paramString)
/*    */   {
/* 14 */     this(paramFrame, paramString, null);
/*    */   }
/*    */   
/*    */   public OkCancelDialog(Frame paramFrame, String paramString, Component paramComponent)
/*    */   {
/* 19 */     super(paramFrame, paramString, paramComponent);
/* 20 */     addButton("Ok", this);
/* 21 */     addButton("Cancel", this);
/*    */     
/* 23 */     pack();
/* 24 */     setVisible(true);
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\dialogs\OkCancelDialog.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */