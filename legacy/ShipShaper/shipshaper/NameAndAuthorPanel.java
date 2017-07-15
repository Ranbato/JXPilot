/*    */ package shipshaper;
/*    */ 
/*    */ import java.awt.GridBagLayout;
/*    */ import java.awt.TextComponent;
/*    */ 
/*    */ public final class NameAndAuthorPanel extends java.awt.Panel implements java.awt.event.TextListener
/*    */ {
/*    */   Ship ship;
/*    */   java.awt.Label nameLabel;
/*    */   java.awt.Label authorLabel;
/*    */   java.awt.TextField nameField;
/*    */   java.awt.TextField authorField;
/*    */   
/*    */   NameAndAuthorPanel()
/*    */   {
/* 16 */     this.nameLabel = new java.awt.Label("Name");
/* 17 */     this.authorLabel = new java.awt.Label("Author");
/* 18 */     this.nameField = new java.awt.TextField("", 15);
/* 19 */     this.authorField = new java.awt.TextField("", 15);
/*    */     
/* 21 */     GridBagLayout localGridBagLayout = new GridBagLayout();
/* 22 */     java.awt.GridBagConstraints localGridBagConstraints = new java.awt.GridBagConstraints();
/* 23 */     setLayout(localGridBagLayout);
/*    */     
/* 25 */     localGridBagConstraints.gridwidth = 1;
/* 26 */     localGridBagLayout.setConstraints(this.nameLabel, localGridBagConstraints);
/* 27 */     add(this.nameLabel);
/*    */     
/* 29 */     localGridBagConstraints.gridwidth = 0;
/* 30 */     localGridBagLayout.setConstraints(this.nameField, localGridBagConstraints);
/* 31 */     add(this.nameField);
/*    */     
/* 33 */     localGridBagConstraints.gridwidth = 1;
/* 34 */     localGridBagLayout.setConstraints(this.authorLabel, localGridBagConstraints);
/* 35 */     add(this.authorLabel);
/*    */     
/* 37 */     localGridBagConstraints.gridwidth = 0;
/* 38 */     localGridBagLayout.setConstraints(this.authorField, localGridBagConstraints);
/* 39 */     add(this.authorField);
/*    */     
/* 41 */     this.nameField.addTextListener(this);
/* 42 */     this.authorField.addTextListener(this);
/*    */   }
/*    */   
/*    */   NameAndAuthorPanel(Ship paramShip)
/*    */   {
/* 47 */     this();
/* 48 */     setShip(paramShip);
/*    */   }
/*    */   
/*    */   public void setShip(Ship paramShip)
/*    */   {
/* 53 */     this.ship = paramShip;
/* 54 */     if (paramShip != null)
/*    */     {
/* 56 */       this.nameField.setText(paramShip.name);
/* 57 */       this.authorField.setText(paramShip.author);
/* 58 */       this.nameField.setEnabled(true);
/* 59 */       this.authorField.setEnabled(true);
/*    */     }
/*    */     else
/*    */     {
/* 63 */       this.nameField.setEnabled(false);
/* 64 */       this.authorField.setEnabled(false);
/*    */     }
/*    */   }
/*    */   
/*    */   public Ship getShip()
/*    */   {
/* 70 */     return this.ship;
/*    */   }
/*    */   
/*    */   public void textValueChanged(java.awt.event.TextEvent paramTextEvent)
/*    */   {
/* 75 */     if (this.ship == null) {
/* 76 */       return;
/*    */     }
/* 78 */     if (!this.ship.name.equals(this.nameField.getText()))
/*    */     {
/* 80 */       this.ship.name = this.nameField.getText();
/* 81 */       this.ship.shipChanged(new ShipEvent(this, false, false, true, false));
/*    */ 
/*    */     }
/* 84 */     else if (!this.ship.author.equals(this.authorField.getText()))
/*    */     {
/* 86 */       this.ship.author = this.authorField.getText();
/* 87 */       this.ship.shipChanged(new ShipEvent(this, false, false, false, true));
/*    */     }
/*    */   }
/*    */ }


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\NameAndAuthorPanel.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */