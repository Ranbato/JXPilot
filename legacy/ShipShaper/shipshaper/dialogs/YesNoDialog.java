/*    */ package shipshaper.dialogs;
/*    */ 
/*    */ import java.awt.Component;
/*    */ import java.awt.Frame;
/*    */ import java.awt.Window;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class YesNoDialog
/*    */   extends MessageDialog
/*    */ {
/*    */   public YesNoDialog(Frame paramFrame, String paramString)
/*    */   {
/* 15 */     this(paramFrame, paramString, null);
/*    */   }
/*    */   
/*    */   public YesNoDialog(Frame paramFrame, String paramString, Component paramComponent)
/*    */   {
/* 20 */     super(paramFrame, paramString, paramComponent);
/* 21 */     addButton("Yes", this);
/* 22 */     addButton("No", this);
/*    */     
/* 24 */     pack();
/* 25 */     setVisible(true);
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\dialogs\YesNoDialog.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */