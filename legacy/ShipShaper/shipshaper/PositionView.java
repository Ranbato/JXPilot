/*    */ package shipshaper;
/*    */ 
/*    */ import java.awt.GridBagLayout;
/*    */ 
/*    */ public final class PositionView extends java.awt.Panel
/*    */ {
/*    */   private java.awt.TextField xField;
/*    */   private java.awt.TextField yField;
/*    */   
/*    */   public PositionView(boolean paramBoolean) {
/* 11 */     GridBagLayout localGridBagLayout = new GridBagLayout();
/* 12 */     java.awt.GridBagConstraints localGridBagConstraints = new java.awt.GridBagConstraints();
/*    */     
/* 14 */     setLayout(localGridBagLayout);
/*    */     
/* 16 */     java.awt.Label localLabel1 = new java.awt.Label("X");
/* 17 */     java.awt.Label localLabel2 = new java.awt.Label("Y");
/* 18 */     this.xField = new java.awt.TextField("", 3);
/* 19 */     this.yField = new java.awt.TextField("", 3);
/* 20 */     this.xField.setEditable(false);
/* 21 */     this.yField.setEditable(false);
/*    */     
/* 23 */     localGridBagConstraints.gridwidth = 1;
/* 24 */     localGridBagLayout.setConstraints(localLabel1, localGridBagConstraints);
/* 25 */     add(localLabel1);
/*    */     
/* 27 */     if (paramBoolean) {
/* 28 */       localGridBagConstraints.gridwidth = 1;
/*    */     } else
/* 30 */       localGridBagConstraints.gridwidth = 0;
/* 31 */     localGridBagLayout.setConstraints(this.xField, localGridBagConstraints);
/* 32 */     add(this.xField);
/*    */     
/* 34 */     localGridBagConstraints.gridwidth = 1;
/* 35 */     localGridBagLayout.setConstraints(localLabel2, localGridBagConstraints);
/* 36 */     add(localLabel2);
/*    */     
/* 38 */     localGridBagConstraints.gridwidth = 0;
/* 39 */     localGridBagLayout.setConstraints(this.yField, localGridBagConstraints);
/* 40 */     add(this.yField);
/*    */     
/* 42 */     coordsNotAvailable();
/*    */   }
/*    */   
/*    */   public void coordsNotAvailable()
/*    */   {
/* 47 */     this.xField.setText("");
/* 48 */     this.yField.setText("");
/*    */   }
/*    */   
/*    */   public void coordsChanged(int paramInt1, int paramInt2)
/*    */   {
/* 53 */     this.xField.setText(String.valueOf(paramInt1));
/* 54 */     this.yField.setText(String.valueOf(paramInt2));
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\PositionView.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */