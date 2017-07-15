/*    */ package shipshaper.dialogs;
/*    */ 
/*    */ import java.awt.Component;
/*    */ import java.awt.Frame;
/*    */ import java.awt.Window;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class OkDialog
/*    */   extends MessageDialog
/*    */ {
/*    */   public OkDialog(Frame paramFrame, String paramString)
/*    */   {
/* 15 */     this(paramFrame, paramString, null);
/*    */   }
/*    */   
/*    */   public OkDialog(Frame paramFrame, String paramString, Component paramComponent)
/*    */   {
/* 20 */     super(paramFrame, paramString, paramComponent);
/* 21 */     addButton("Ok", this);
/*    */     
/* 23 */     pack();
/* 24 */     setVisible(true);
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\dialogs\OkDialog.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */