import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

class DraaiSchip
  extends Canvas
{
  int coordNum;
  int leftNum;
  int rightNum;
  int totaal;
  int totleft;
  int totright;
  int[][] coord = new int[24][2];
  int[][] left = new int[3][2];
  int[][] right = new int[3][2];
  double angle;
  int keeptrack;
  
  public void setPoint(int paramInt1, int paramInt2)
  {
    this.coord[this.coordNum][0] = paramInt1;
    this.coord[this.coordNum][1] = paramInt2;
    this.coordNum += 1;
  }
  
  public void setLPoint(int paramInt1, int paramInt2)
  {
    this.left[this.leftNum][0] = paramInt1;
    this.left[this.leftNum][1] = paramInt2;
    this.leftNum += 1;
  }
  
  public void setRPoint(int paramInt1, int paramInt2)
  {
    this.right[this.rightNum][0] = paramInt1;
    this.right[this.rightNum][1] = paramInt2;
    this.rightNum += 1;
  }
  
  public void setAngle(double paramDouble)
  {
    this.angle = paramDouble;
  }
  
  public void teken()
  {
    repaint();
    this.totaal = this.coordNum;
    this.coordNum = 0;
    this.totleft = this.leftNum;
    this.leftNum = 0;
    this.totright = this.rightNum;
    this.rightNum = 0;
  }
  
  public void paint(Graphics paramGraphics)
  {
    paramGraphics.setColor(Color.white);
    for (int i = 0; i < this.totaal - 1; i++) {
      paramGraphics.drawLine(23 + this.coord[i][0], 23 - this.coord[i][1], 23 + this.coord[(i + 1)][0], 23 - this.coord[(i + 1)][1]);
    }
    if ((this.totaal > 2) || (this.totaal == 1)) {
      paramGraphics.drawLine(23 + this.coord[0][0], 23 - this.coord[0][1], 23 + this.coord[(this.totaal - 1)][0], 23 - this.coord[(this.totaal - 1)][1]);
    }
    if ((this.keeptrack % 15 == 0) || (this.keeptrack % 15 == 1) || (this.keeptrack % 15 == 2))
    {
      paramGraphics.setColor(Color.green);
      for (int j = 0; j < this.totleft; j++) {
        paramGraphics.fillOval(21 + this.left[j][0], 21 - this.left[j][1], 5, 5);
      }
      paramGraphics.setColor(Color.red);
      for (int k = 0; k < this.totright; k++) {
        paramGraphics.fillOval(21 + this.right[k][0], 21 - this.right[k][1], 5, 5);
      }
    }
    paramGraphics.setColor(Color.red);
    paramGraphics.drawOval(23 + (int)(21.0D * Math.cos(this.angle)), 23 + (int)(21.0D * Math.sin(this.angle)), 2, 2);
    this.keeptrack += 1;
  }
}


/* Location:              D:\Java\src\JXPilot\legacy\xpeditorapplet\!\DraaiSchip.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */