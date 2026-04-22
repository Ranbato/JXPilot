import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Event;
import java.awt.Graphics;

class TekenBord
  extends Canvas
{
  int[][] hull = new int[24][2];
  int[] maingun = new int[2];
  int[][] leftgun = new int[3][2];
  int[][] rightgun = new int[3][2];
  int[][] leftlight = new int[3][2];
  int[][] rightlight = new int[3][2];
  int[][] leftreargun = new int[3][2];
  int[][] rightreargun = new int[3][2];
  int[][] missile = new int[4][2];
  int[] engine = new int[2];
  int HullPoints;
  boolean MainGun = false;
  int LeftGunPoints;
  int RightGunPoints;
  int LeftLightPoints;
  int RightLightPoints;
  int LeftRearGunPoints;
  int RightRearGunPoints;
  int MissilePoints;
  boolean Engine = false;
  boolean change = true;
  String tool = new String();
  String item = new String();
  int blokjex_oud = -90;
  int blokjey_oud = -90;
  int blokjex_ouder = -90;
  int blokjey_ouder = -90;
  int dragging = -1;
  boolean firsttime;
  int selected = -1;
  
  public void paint(Graphics paramGraphics)
  {
    paramGraphics.setColor(Color.yellow);
    paramGraphics.drawRect(1, 1, 314, 314);
    paramGraphics.setColor(Color.white);
    paramGraphics.drawRect(3, 3, 310, 310);
    paramGraphics.setColor(Color.gray);
    for (int i = 0; i < 30; i++)
    {
      paramGraphics.drawLine(13 + 10 * i, 4, 13 + 10 * i, 312);
      paramGraphics.drawLine(4, 13 + 10 * i, 312, 13 + 10 * i);
    }
    paramGraphics.setColor(Color.lightGray);
    paramGraphics.drawLine(153, 4, 153, 312);
    paramGraphics.drawLine(163, 4, 163, 312);
    paramGraphics.drawLine(4, 153, 312, 153);
    paramGraphics.drawLine(4, 163, 312, 163);
    paramGraphics.setColor(Color.orange);
    paramGraphics.drawRect(73, 73, 170, 170);
    for (int j = 0; j < this.HullPoints; j++)
    {
      paramGraphics.setColor(Color.blue);
      paramGraphics.fillRect(154 + 10 * this.hull[j][0], 154 - 10 * this.hull[j][1], 9, 9);
    }
    for (int k = 0; k < this.HullPoints - 1; k++) {
      paramGraphics.drawLine(158 + 10 * this.hull[k][0], 158 - 10 * this.hull[k][1], 158 + 10 * this.hull[(k + 1)][0], 158 - 10 * this.hull[(k + 1)][1]);
    }
    if (this.HullPoints > 2)
    {
      paramGraphics.setColor(Color.cyan);
      paramGraphics.drawLine(158 + 10 * this.hull[0][0], 158 - 10 * this.hull[0][1], 158 + 10 * this.hull[(this.HullPoints - 1)][0], 158 - 10 * this.hull[(this.HullPoints - 1)][1]);
    }
    int[] arrayOfInt2;
    if (this.MainGun)
    {
      paramGraphics.setColor(Color.red);
      int[] arrayOfInt1 = { 154 + 10 * this.maingun[0], 154 + 10 * this.maingun[0], 163 + 10 * this.maingun[0] };
      arrayOfInt2 = new int[] { 153 - 10 * this.maingun[1], 163 - 10 * this.maingun[1], 158 - 10 * this.maingun[1] };
      paramGraphics.fillPolygon(arrayOfInt1, arrayOfInt2, 3);
    }
    int[] arrayOfInt3;
    for (int m = 0; m < this.LeftGunPoints; m++)
    {
      paramGraphics.setColor(Color.red);
      arrayOfInt2 = new int[] { 154 + 10 * this.leftgun[m][0], 154 + 10 * this.leftgun[m][0], 163 + 10 * this.leftgun[m][0] };
      arrayOfInt3 = new int[] { 154 - 10 * this.leftgun[m][1], 163 - 10 * this.leftgun[m][1], 154 - 10 * this.leftgun[m][1] };
      paramGraphics.fillPolygon(arrayOfInt2, arrayOfInt3, 3);
    }
    int[] arrayOfInt4;
    for (int n = 0; n < this.RightGunPoints; n++)
    {
      paramGraphics.setColor(Color.red);
      arrayOfInt3 = new int[] { 154 + 10 * this.rightgun[n][0], 154 + 10 * this.rightgun[n][0], 163 + 10 * this.rightgun[n][0] };
      arrayOfInt4 = new int[] { 153 - 10 * this.rightgun[n][1], 163 - 10 * this.rightgun[n][1], 163 - 10 * this.rightgun[n][1] };
      paramGraphics.fillPolygon(arrayOfInt3, arrayOfInt4, 3);
    }
    int[] arrayOfInt5;
    for (int i1 = 0; i1 < this.LeftRearGunPoints; i1++)
    {
      paramGraphics.setColor(Color.red);
      arrayOfInt4 = new int[] { 163 + 10 * this.leftreargun[i1][0], 163 + 10 * this.leftreargun[i1][0], 154 + 10 * this.leftreargun[i1][0] };
      arrayOfInt5 = new int[] { 154 - 10 * this.leftreargun[i1][1], 163 - 10 * this.leftreargun[i1][1], 154 - 10 * this.leftreargun[i1][1] };
      paramGraphics.fillPolygon(arrayOfInt4, arrayOfInt5, 3);
    }
    int[] arrayOfInt6;
    for (int i2 = 0; i2 < this.RightRearGunPoints; i2++)
    {
      paramGraphics.setColor(Color.red);
      arrayOfInt5 = new int[] { 163 + 10 * this.rightreargun[i2][0], 163 + 10 * this.rightreargun[i2][0], 154 + 10 * this.rightreargun[i2][0] };
      arrayOfInt6 = new int[] { 153 - 10 * this.rightreargun[i2][1], 163 - 10 * this.rightreargun[i2][1], 163 - 10 * this.rightreargun[i2][1] };
      paramGraphics.fillPolygon(arrayOfInt5, arrayOfInt6, 3);
    }
    for (int i3 = 0; i3 < this.MissilePoints; i3++)
    {
      paramGraphics.setColor(Color.pink);
      arrayOfInt6 = new int[] { 154 + 10 * this.missile[i3][0], 154 + 10 * this.missile[i3][0], 163 + 10 * this.missile[i3][0] };
      int[] arrayOfInt7 = { 155 - 10 * this.missile[i3][1], 160 - 10 * this.missile[i3][1], 158 - 10 * this.missile[i3][1] };
      paramGraphics.fillPolygon(arrayOfInt6, arrayOfInt7, 3);
    }
    paramGraphics.setColor(Color.orange);
    for (int i4 = 0; i4 < this.LeftLightPoints; i4++) {
      paramGraphics.fillOval(154 + 10 * this.leftlight[i4][0], 154 - 10 * this.leftlight[i4][1], 8, 8);
    }
    paramGraphics.setColor(Color.green);
    for (int i5 = 0; i5 < this.RightLightPoints; i5++) {
      paramGraphics.fillOval(154 + 10 * this.rightlight[i5][0], 154 - 10 * this.rightlight[i5][1], 8, 8);
    }
    if (this.Engine)
    {
      paramGraphics.setColor(Color.orange);
      paramGraphics.drawRect(154 + 10 * this.engine[0], 154 - 10 * this.engine[1], 8, 8);
      paramGraphics.drawRect(155 + 10 * this.engine[0], 155 - 10 * this.engine[1], 6, 6);
    }
    paramGraphics.setColor(Color.white);
    if ((this.HullPoints != 23) && (this.item.equals("Hull"))) {
      paramGraphics.drawString(24 - this.HullPoints + " hullpoints left", 110, 329);
    }
    if ((this.HullPoints == 23) && (this.item.equals("Hull"))) {
      paramGraphics.drawString(24 - this.HullPoints + " hullpoints left", 110, 329);
    }
    if ((this.MainGun) && (this.item.equals("Main gun"))) {
      paramGraphics.drawString("0 main guns left", 110, 329);
    }
    if ((!this.MainGun) && (this.item.equals("Main gun"))) {
      paramGraphics.drawString("1 main guns left", 110, 329);
    }
    if ((this.LeftGunPoints != 2) && (this.item.equals("Left gun"))) {
      paramGraphics.drawString(3 - this.LeftGunPoints + " left guns left", 110, 329);
    }
    if ((this.LeftGunPoints == 2) && (this.item.equals("Left gun"))) {
      paramGraphics.drawString(3 - this.LeftGunPoints + " left gun left", 110, 329);
    }
    if ((this.RightGunPoints != 2) && (this.item.equals("Right gun"))) {
      paramGraphics.drawString(3 - this.RightGunPoints + " right guns left", 110, 329);
    }
    if ((this.RightGunPoints == 2) && (this.item.equals("Right gun"))) {
      paramGraphics.drawString(3 - this.RightGunPoints + " right gun left", 110, 329);
    }
    if ((this.LeftRearGunPoints != 2) && (this.item.equals("Left rear gun"))) {
      paramGraphics.drawString(3 - this.LeftRearGunPoints + " left rear guns left", 110, 329);
    }
    if ((this.LeftRearGunPoints == 2) && (this.item.equals("Left rear gun"))) {
      paramGraphics.drawString(3 - this.LeftRearGunPoints + " left rear gun left", 110, 329);
    }
    if ((this.RightRearGunPoints != 2) && (this.item.equals("Right rear gun"))) {
      paramGraphics.drawString(3 - this.RightRearGunPoints + " right rear guns left", 110, 329);
    }
    if ((this.RightRearGunPoints == 2) && (this.item.equals("Right rear gun"))) {
      paramGraphics.drawString(3 - this.RightRearGunPoints + " right rear gun left", 110, 329);
    }
    if ((this.LeftLightPoints != 2) && (this.item.equals("Left light"))) {
      paramGraphics.drawString(3 - this.LeftLightPoints + " left lights left", 110, 329);
    }
    if ((this.LeftLightPoints == 2) && (this.item.equals("Left light"))) {
      paramGraphics.drawString(3 - this.LeftLightPoints + " left light left", 110, 329);
    }
    if ((this.RightLightPoints != 2) && (this.item.equals("Right light"))) {
      paramGraphics.drawString(3 - this.RightLightPoints + " right lights left", 110, 329);
    }
    if ((this.RightLightPoints == 2) && (this.item.equals("Right light"))) {
      paramGraphics.drawString(3 - this.RightLightPoints + " right light left", 110, 329);
    }
    if ((this.MissilePoints != 3) && (this.item.equals("Missile tube"))) {
      paramGraphics.drawString(4 - this.MissilePoints + " missile tubes left", 110, 329);
    }
    if ((this.MissilePoints == 3) && (this.item.equals("Missile tube"))) {
      paramGraphics.drawString(4 - this.MissilePoints + " missile tube left", 110, 329);
    }
    if ((this.Engine) && (this.item.equals("Engine"))) {
      paramGraphics.drawString("0 engines left", 110, 329);
    }
    if ((!this.Engine) && (this.item.equals("Engine"))) {
      paramGraphics.drawString("1 engine left", 110, 329);
    }
    if (this.selected >= 0)
    {
      paramGraphics.setColor(Color.red.brighter());
      paramGraphics.drawOval(148 + 10 * this.hull[this.selected][0], 148 - 10 * this.hull[this.selected][1], 20, 20);
      paramGraphics.setColor(Color.red.darker());
      if (this.selected > 0) {
        paramGraphics.drawOval(148 + 10 * this.hull[(this.selected - 1)][0], 148 - 10 * this.hull[(this.selected - 1)][1], 20, 20);
      }
      if (this.selected < this.HullPoints - 1) {
        paramGraphics.drawOval(148 + 10 * this.hull[(this.selected + 1)][0], 148 - 10 * this.hull[(this.selected + 1)][1], 20, 20);
      }
    }
  }
  
  public boolean handleEvent(Event paramEvent)
  {
    switch (paramEvent.id)
    {
    case 501: 
      mouseWentDown(paramEvent.x, paramEvent.y);
      break;
    case 506: 
      mouseWentDrag(paramEvent.x, paramEvent.y);
    }
    return true;
  }
  
  public void mouseWentDown(int paramInt1, int paramInt2)
  {
    if ((paramInt1 > 3) && (paramInt2 > 3) && (paramInt1 < 313) && (paramInt2 < 313))
    {
      int i = (paramInt1 - 153) / 10;
      int j = (paramInt2 - 153) / 10;
      if (paramInt1 < 153) {
        i--;
      }
      if (paramInt2 < 153) {
        j--;
      }
      if (this.tool.equals("Add"))
      {
        if ((this.HullPoints < 24) && (this.item.equals("Hull"))) {
          if (this.HullPoints > 0)
          {
            if ((this.hull[(this.HullPoints - 1)][0] != i) || (this.hull[(this.HullPoints - 1)][1] != -j))
            {
              this.hull[this.HullPoints][0] = i;
              this.hull[this.HullPoints][1] = (-j);
              this.HullPoints += 1;
              this.change = true;
              repaint();
            }
          }
          else
          {
            this.hull[this.HullPoints][0] = i;
            this.hull[this.HullPoints][1] = (-j);
            this.HullPoints += 1;
            this.change = true;
            repaint();
          }
        }
        if ((this.item.equals("Main gun")) && (!this.MainGun))
        {
          this.maingun[0] = i;
          this.maingun[1] = (-j);
          this.MainGun = true;
          repaint();
        }
        if ((this.LeftGunPoints < 3) && (this.item.equals("Left gun")))
        {
          if (this.LeftGunPoints == 0)
          {
            this.leftgun[this.LeftGunPoints][0] = i;
            this.leftgun[this.LeftGunPoints][1] = (-j);
            this.LeftGunPoints += 1;
            repaint();
          }
          if ((this.LeftGunPoints == 1) && ((this.leftgun[(this.LeftGunPoints - 1)][0] != i) || (this.leftgun[(this.LeftGunPoints - 1)][1] != -j)))
          {
            this.leftgun[this.LeftGunPoints][0] = i;
            this.leftgun[this.LeftGunPoints][1] = (-j);
            this.LeftGunPoints += 1;
            repaint();
          }
          if ((this.LeftGunPoints == 2) && ((this.leftgun[(this.LeftGunPoints - 1)][0] != i) || (this.leftgun[(this.LeftGunPoints - 1)][1] != -j)) && ((this.leftgun[(this.LeftGunPoints - 2)][0] != i) || (this.leftgun[(this.LeftGunPoints - 2)][1] != -j)))
          {
            this.leftgun[this.LeftGunPoints][0] = i;
            this.leftgun[this.LeftGunPoints][1] = (-j);
            this.LeftGunPoints += 1;
            repaint();
          }
        }
        if ((this.RightGunPoints < 3) && (this.item.equals("Right gun")))
        {
          if (this.RightGunPoints == 0)
          {
            this.rightgun[this.RightGunPoints][0] = i;
            this.rightgun[this.RightGunPoints][1] = (-j);
            this.RightGunPoints += 1;
            repaint();
          }
          if ((this.RightGunPoints == 1) && ((this.rightgun[(this.RightGunPoints - 1)][0] != i) || (this.rightgun[(this.RightGunPoints - 1)][1] != -j)))
          {
            this.rightgun[this.RightGunPoints][0] = i;
            this.rightgun[this.RightGunPoints][1] = (-j);
            this.RightGunPoints += 1;
            repaint();
          }
          if ((this.RightGunPoints == 2) && ((this.rightgun[(this.RightGunPoints - 1)][0] != i) || (this.rightgun[(this.RightGunPoints - 1)][1] != -j)) && ((this.rightgun[(this.RightGunPoints - 2)][0] != i) || (this.rightgun[(this.RightGunPoints - 2)][1] != -j)))
          {
            this.rightgun[this.RightGunPoints][0] = i;
            this.rightgun[this.RightGunPoints][1] = (-j);
            this.RightGunPoints += 1;
            repaint();
          }
        }
        if ((this.LeftRearGunPoints < 3) && (this.item.equals("Left rear gun")))
        {
          if (this.LeftRearGunPoints == 0)
          {
            this.leftreargun[this.LeftRearGunPoints][0] = i;
            this.leftreargun[this.LeftRearGunPoints][1] = (-j);
            this.LeftRearGunPoints += 1;
            repaint();
          }
          if ((this.LeftRearGunPoints == 1) && ((this.leftreargun[(this.LeftRearGunPoints - 1)][0] != i) || (this.leftreargun[(this.LeftRearGunPoints - 1)][1] != -j)))
          {
            this.leftreargun[this.LeftRearGunPoints][0] = i;
            this.leftreargun[this.LeftRearGunPoints][1] = (-j);
            this.LeftRearGunPoints += 1;
            repaint();
          }
          if ((this.LeftRearGunPoints == 2) && ((this.leftreargun[(this.LeftRearGunPoints - 1)][0] != i) || (this.leftreargun[(this.LeftRearGunPoints - 1)][1] != -j)) && ((this.leftreargun[(this.LeftRearGunPoints - 2)][0] != i) || (this.leftreargun[(this.LeftRearGunPoints - 2)][1] != -j)))
          {
            this.leftreargun[this.LeftRearGunPoints][0] = i;
            this.leftreargun[this.LeftRearGunPoints][1] = (-j);
            this.LeftRearGunPoints += 1;
            repaint();
          }
        }
        if ((this.RightRearGunPoints < 3) && (this.item.equals("Right rear gun")))
        {
          if (this.RightRearGunPoints == 0)
          {
            this.rightreargun[this.RightRearGunPoints][0] = i;
            this.rightreargun[this.RightRearGunPoints][1] = (-j);
            this.RightRearGunPoints += 1;
            repaint();
          }
          if ((this.RightRearGunPoints == 1) && ((this.rightreargun[(this.RightRearGunPoints - 1)][0] != i) || (this.rightreargun[(this.RightRearGunPoints - 1)][1] != -j)))
          {
            this.rightreargun[this.RightRearGunPoints][0] = i;
            this.rightreargun[this.RightRearGunPoints][1] = (-j);
            this.RightRearGunPoints += 1;
            repaint();
          }
          if ((this.RightRearGunPoints == 2) && ((this.rightreargun[(this.RightRearGunPoints - 1)][0] != i) || (this.rightreargun[(this.RightRearGunPoints - 1)][1] != -j)) && ((this.rightreargun[(this.RightRearGunPoints - 2)][0] != i) || (this.rightreargun[(this.RightRearGunPoints - 2)][1] != -j)))
          {
            this.rightreargun[this.RightRearGunPoints][0] = i;
            this.rightreargun[this.RightRearGunPoints][1] = (-j);
            this.RightRearGunPoints += 1;
            repaint();
          }
        }
        if ((this.LeftLightPoints < 3) && (this.item.equals("Left light")))
        {
          if (this.LeftLightPoints == 0)
          {
            this.leftlight[this.LeftLightPoints][0] = i;
            this.leftlight[this.LeftLightPoints][1] = (-j);
            this.LeftLightPoints += 1;
            this.change = true;
            repaint();
          }
          if ((this.LeftLightPoints == 1) && ((this.leftlight[(this.LeftLightPoints - 1)][0] != i) || (this.leftlight[(this.LeftLightPoints - 1)][1] != -j)))
          {
            this.leftlight[this.LeftLightPoints][0] = i;
            this.leftlight[this.LeftLightPoints][1] = (-j);
            this.LeftLightPoints += 1;
            this.change = true;
            repaint();
          }
          if ((this.LeftLightPoints == 2) && ((this.leftlight[(this.LeftLightPoints - 1)][0] != i) || (this.leftlight[(this.LeftLightPoints - 1)][1] != -j)) && ((this.leftlight[(this.LeftLightPoints - 2)][0] != i) || (this.leftlight[(this.LeftLightPoints - 2)][1] != -j)))
          {
            this.leftlight[this.LeftLightPoints][0] = i;
            this.leftlight[this.LeftLightPoints][1] = (-j);
            this.LeftLightPoints += 1;
            this.change = true;
            repaint();
          }
        }
        if ((this.RightLightPoints < 3) && (this.item.equals("Right light")))
        {
          if (this.RightLightPoints == 0)
          {
            this.rightlight[this.RightLightPoints][0] = i;
            this.rightlight[this.RightLightPoints][1] = (-j);
            this.RightLightPoints += 1;
            this.change = true;
            repaint();
          }
          if ((this.RightLightPoints == 1) && ((this.rightlight[(this.RightLightPoints - 1)][0] != i) || (this.rightlight[(this.RightLightPoints - 1)][1] != -j)))
          {
            this.rightlight[this.RightLightPoints][0] = i;
            this.rightlight[this.RightLightPoints][1] = (-j);
            this.RightLightPoints += 1;
            this.change = true;
            repaint();
          }
          if ((this.RightLightPoints == 2) && ((this.rightlight[(this.RightLightPoints - 1)][0] != i) || (this.rightlight[(this.RightLightPoints - 1)][1] != -j)) && ((this.rightlight[(this.RightLightPoints - 2)][0] != i) || (this.rightlight[(this.RightLightPoints - 2)][1] != -j)))
          {
            this.rightlight[this.RightLightPoints][0] = i;
            this.rightlight[this.RightLightPoints][1] = (-j);
            this.RightLightPoints += 1;
            this.change = true;
            repaint();
          }
        }
        if ((this.MissilePoints < 4) && (this.item.equals("Missile tube")))
        {
          if (this.MissilePoints == 0)
          {
            this.missile[this.MissilePoints][0] = i;
            this.missile[this.MissilePoints][1] = (-j);
            this.MissilePoints += 1;
            repaint();
          }
          if ((this.MissilePoints == 1) && ((this.missile[(this.MissilePoints - 1)][0] != i) || (this.missile[(this.MissilePoints - 1)][1] != -j)))
          {
            this.missile[this.MissilePoints][0] = i;
            this.missile[this.MissilePoints][1] = (-j);
            this.MissilePoints += 1;
            repaint();
          }
          if ((this.MissilePoints == 2) && ((this.missile[(this.MissilePoints - 1)][0] != i) || (this.missile[(this.MissilePoints - 1)][1] != -j)) && ((this.missile[(this.MissilePoints - 2)][0] != i) || (this.missile[(this.MissilePoints - 2)][1] != -j)))
          {
            this.missile[this.MissilePoints][0] = i;
            this.missile[this.MissilePoints][1] = (-j);
            this.MissilePoints += 1;
            repaint();
          }
          if ((this.MissilePoints == 3) && ((this.missile[(this.MissilePoints - 1)][0] != i) || (this.missile[(this.MissilePoints - 1)][1] != -j)) && ((this.missile[(this.MissilePoints - 2)][0] != i) || (this.missile[(this.MissilePoints - 2)][1] != -j)) && ((this.missile[(this.MissilePoints - 3)][0] != i) || (this.missile[(this.MissilePoints - 3)][1] != -j)))
          {
            this.missile[this.MissilePoints][0] = i;
            this.missile[this.MissilePoints][1] = (-j);
            this.MissilePoints += 1;
            repaint();
          }
        }
        if ((this.item.equals("Engine")) && (!this.Engine))
        {
          this.engine[0] = i;
          this.engine[1] = (-j);
          this.Engine = true;
          repaint();
        }
      }
      int m;
      int n;
      if (this.tool.equals("Remove"))
      {
        if ((this.HullPoints > 0) && (this.item.equals("Hull")))
        {
          k = -1;
          for (m = 0; m < this.HullPoints; m++) {
            if ((this.hull[m][0] == i) && (this.hull[m][1] == -j)) {
              k = m;
            }
          }
          if (k > -1)
          {
            for (n = k; n < this.HullPoints - 1; n++)
            {
              this.hull[n][0] = this.hull[(n + 1)][0];
              this.hull[n][1] = this.hull[(n + 1)][1];
            }
            this.HullPoints -= 1;
            this.change = true;
            repaint();
          }
        }
        if ((this.item.equals("Main gun")) && (this.MainGun) && (this.maingun[0] == i) && (this.maingun[1] == -j))
        {
          this.MainGun = false;
          repaint();
        }
        if ((this.LeftGunPoints > 0) && (this.item.equals("Left gun")))
        {
          k = -1;
          for (m = 0; m < this.LeftGunPoints; m++) {
            if ((this.leftgun[m][0] == i) && (this.leftgun[m][1] == -j)) {
              k = m;
            }
          }
          if (k > -1)
          {
            for (n = k; n < this.LeftGunPoints - 1; n++)
            {
              this.leftgun[n][0] = this.leftgun[(n + 1)][0];
              this.leftgun[n][1] = this.leftgun[(n + 1)][1];
            }
            this.LeftGunPoints -= 1;
            repaint();
          }
        }
        if ((this.RightGunPoints > 0) && (this.item.equals("Right gun")))
        {
          k = -1;
          for (m = 0; m < this.RightGunPoints; m++) {
            if ((this.rightgun[m][0] == i) && (this.rightgun[m][1] == -j)) {
              k = m;
            }
          }
          if (k > -1)
          {
            for (n = k; n < this.RightGunPoints - 1; n++)
            {
              this.rightgun[n][0] = this.rightgun[(n + 1)][0];
              this.rightgun[n][1] = this.rightgun[(n + 1)][1];
            }
            this.RightGunPoints -= 1;
            repaint();
          }
        }
        if ((this.LeftRearGunPoints > 0) && (this.item.equals("Left rear gun")))
        {
          k = -1;
          for (m = 0; m < this.LeftRearGunPoints; m++) {
            if ((this.leftreargun[m][0] == i) && (this.leftreargun[m][1] == -j)) {
              k = m;
            }
          }
          if (k > -1)
          {
            for (n = k; n < this.LeftRearGunPoints - 1; n++)
            {
              this.leftreargun[n][0] = this.leftreargun[(n + 1)][0];
              this.leftreargun[n][1] = this.leftreargun[(n + 1)][1];
            }
            this.LeftRearGunPoints -= 1;
            repaint();
          }
        }
        if ((this.RightRearGunPoints > 0) && (this.item.equals("Right rear gun")))
        {
          k = -1;
          for (m = 0; m < this.RightRearGunPoints; m++) {
            if ((this.rightreargun[m][0] == i) && (this.rightreargun[m][1] == -j)) {
              k = m;
            }
          }
          if (k > -1)
          {
            for (n = k; n < this.RightRearGunPoints - 1; n++)
            {
              this.rightreargun[n][0] = this.rightreargun[(n + 1)][0];
              this.rightreargun[n][1] = this.rightreargun[(n + 1)][1];
            }
            this.RightRearGunPoints -= 1;
            repaint();
          }
        }
        if ((this.LeftLightPoints > 0) && (this.item.equals("Left light")))
        {
          k = -1;
          for (m = 0; m < this.LeftLightPoints; m++) {
            if ((this.leftlight[m][0] == i) && (this.leftlight[m][1] == -j)) {
              k = m;
            }
          }
          if (k > -1)
          {
            for (n = k; n < this.LeftLightPoints - 1; n++)
            {
              this.leftlight[n][0] = this.leftlight[(n + 1)][0];
              this.leftlight[n][1] = this.leftlight[(n + 1)][1];
            }
            this.LeftLightPoints -= 1;
            this.change = true;
            repaint();
          }
        }
        if ((this.RightLightPoints > 0) && (this.item.equals("Right light")))
        {
          k = -1;
          for (m = 0; m < this.RightLightPoints; m++) {
            if ((this.rightlight[m][0] == i) && (this.rightlight[m][1] == -j)) {
              k = m;
            }
          }
          if (k > -1)
          {
            for (n = k; n < this.RightLightPoints - 1; n++)
            {
              this.rightlight[n][0] = this.rightlight[(n + 1)][0];
              this.rightlight[n][1] = this.rightlight[(n + 1)][1];
            }
            this.RightLightPoints -= 1;
            this.change = true;
            repaint();
          }
        }
        if ((this.MissilePoints > 0) && (this.item.equals("Missile tube")))
        {
          k = -1;
          for (m = 0; m < this.MissilePoints; m++) {
            if ((this.missile[m][0] == i) && (this.missile[m][1] == -j)) {
              k = m;
            }
          }
          if (k > -1)
          {
            for (n = k; n < this.MissilePoints - 1; n++)
            {
              this.missile[n][0] = this.missile[(n + 1)][0];
              this.missile[n][1] = this.missile[(n + 1)][1];
            }
            this.MissilePoints -= 1;
            repaint();
          }
        }
        if ((this.item.equals("Engine")) && (this.Engine) && (this.engine[0] == i) && (this.engine[1] == -j))
        {
          this.Engine = false;
          repaint();
        }
      }
      if (this.tool.equals("Move"))
      {
        if (this.item.equals("Hull"))
        {
          k = -1;
          for (m = 0; m < this.HullPoints; m++) {
            if ((this.hull[m][0] == i) && (this.hull[m][1] == -j)) {
              k = m;
            }
          }
          this.dragging = k;
        }
        if (this.item.equals("Main gun")) {
          if ((this.maingun[0] == i) && (this.maingun[1] == -j)) {
            this.dragging = 0;
          } else {
            this.dragging = -1;
          }
        }
        if (this.item.equals("Left gun"))
        {
          k = -1;
          for (m = 0; m < this.LeftGunPoints; m++) {
            if ((this.leftgun[m][0] == i) && (this.leftgun[m][1] == -j)) {
              k = m;
            }
          }
          this.dragging = k;
        }
        if (this.item.equals("Right gun"))
        {
          k = -1;
          for (m = 0; m < this.RightGunPoints; m++) {
            if ((this.rightgun[m][0] == i) && (this.rightgun[m][1] == -j)) {
              k = m;
            }
          }
          this.dragging = k;
        }
        if (this.item.equals("Left rear gun"))
        {
          k = -1;
          for (m = 0; m < this.LeftRearGunPoints; m++) {
            if ((this.leftreargun[m][0] == i) && (this.leftreargun[m][1] == -j)) {
              k = m;
            }
          }
          this.dragging = k;
        }
        if (this.item.equals("Right rear gun"))
        {
          k = -1;
          for (m = 0; m < this.RightRearGunPoints; m++) {
            if ((this.rightreargun[m][0] == i) && (this.rightreargun[m][1] == -j)) {
              k = m;
            }
          }
          this.dragging = k;
        }
        if (this.item.equals("Left light"))
        {
          k = -1;
          for (m = 0; m < this.LeftLightPoints; m++) {
            if ((this.leftlight[m][0] == i) && (this.leftlight[m][1] == -j)) {
              k = m;
            }
          }
          this.dragging = k;
        }
        if (this.item.equals("Right light"))
        {
          k = -1;
          for (m = 0; m < this.RightLightPoints; m++) {
            if ((this.rightlight[m][0] == i) && (this.rightlight[m][1] == -j)) {
              k = m;
            }
          }
          this.dragging = k;
        }
        if (this.item.equals("Missile tube"))
        {
          k = -1;
          for (m = 0; m < this.MissilePoints; m++) {
            if ((this.missile[m][0] == i) && (this.missile[m][1] == -j)) {
              k = m;
            }
          }
          this.dragging = k;
        }
        if (this.item.equals("Engine")) {
          if ((this.engine[0] == i) && (this.engine[1] == -j)) {
            this.dragging = 0;
          } else {
            this.dragging = -1;
          }
        }
        this.firsttime = true;
      }
      int k = 0;
      if ((this.tool.equals("Insert")) && (this.selected >= 0))
      {
        m = -1;
        for (n = 0; n < this.HullPoints; n++) {
          if ((this.hull[n][0] == i) && (this.hull[n][1] == -j)) {
            m = n;
          }
        }
        if (((m == this.selected - 1) || (m == this.selected + 1)) && (m != -1))
        {
          if (m < this.selected) {
            m = this.selected;
          }
          for (int i1 = this.HullPoints; i1 > m; i1--)
          {
            this.hull[i1][0] = this.hull[(i1 - 1)][0];
            this.hull[i1][1] = this.hull[(i1 - 1)][1];
          }
          for (int i2 = 0; i2 < 2; i2++) {
            this.hull[m][i2] = ((this.hull[m][i2] + this.hull[(m - 1)][i2]) / 2);
          }
          this.HullPoints += 1;
          this.selected = -1;
          this.change = true;
          repaint();
          k = 1;
        }
        else
        {
          this.selected = -1;
          k = 1;
          this.change = true;
          repaint();
        }
      }
      if ((this.tool.equals("Insert")) && (this.selected < 0) && (this.HullPoints < 24) && (k == 0))
      {
        m = -1;
        for (n = 0; n < this.HullPoints; n++) {
          if ((this.hull[n][0] == i) && (this.hull[n][1] == -j)) {
            m = n;
          }
        }
        this.selected = m;
        repaint();
      }
    }
  }
  
  public void mouseWentDrag(int paramInt1, int paramInt2)
  {
    int i = (paramInt1 - 153) / 10;
    int j = (paramInt2 - 153) / 10;
    if (paramInt1 < 153) {
      i--;
    }
    if (paramInt2 < 153) {
      j--;
    }
    if ((paramInt1 > 3) && (paramInt2 > 3) && (paramInt1 < 313) && (paramInt2 < 313) && ((i != this.blokjex_oud) || (j != this.blokjey_oud)))
    {
      if (this.tool.equals("Move"))
      {
        if ((this.item.equals("Hull")) && (this.dragging >= 0))
        {
          if ((this.dragging > 0) && (this.dragging < this.HullPoints - 1) && ((this.hull[(this.dragging - 1)][0] != i) || (this.hull[(this.dragging - 1)][1] != -j)) && ((this.hull[(this.dragging + 1)][0] != i) || (this.hull[(this.dragging + 1)][1] != -j)))
          {
            this.hull[this.dragging][0] = i;
            this.hull[this.dragging][1] = (-j);
            this.change = true;
            repaint();
          }
          if ((this.dragging == 0) && ((this.hull[(this.dragging + 1)][0] != i) || (this.hull[(this.dragging + 1)][1] != -j)) && ((this.hull[(this.HullPoints - 1)][0] != i) || (this.hull[(this.HullPoints - 1)][1] != -j)))
          {
            this.hull[this.dragging][0] = i;
            this.hull[this.dragging][1] = (-j);
            this.change = true;
            repaint();
          }
          if ((this.dragging == this.HullPoints - 1) && (this.HullPoints != 1) && ((this.hull[(this.dragging - 1)][0] != i) || (this.hull[(this.dragging - 1)][1] != -j)) && ((this.hull[0][0] != i) || (this.hull[0][1] != -j)))
          {
            this.hull[this.dragging][0] = i;
            this.hull[this.dragging][1] = (-j);
            this.change = true;
            repaint();
          }
          if (this.HullPoints == 1)
          {
            this.hull[this.dragging][0] = i;
            this.hull[this.dragging][1] = (-j);
            this.change = true;
            repaint();
          }
        }
        if ((this.item.equals("Main gun")) && (this.dragging == 0))
        {
          this.maingun[0] = i;
          this.maingun[1] = (-j);
          repaint(154 + 10 * this.blokjex_oud, 154 + 10 * this.blokjey_oud, 9, 9);
          repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
        }
        if ((this.item.equals("Left gun")) && (this.dragging >= 0))
        {
          if ((this.dragging == 0) && ((this.leftgun[1][0] != i) || (this.leftgun[1][1] != -j)) && ((this.leftgun[2][0] != i) || (this.leftgun[2][1] != -j)))
          {
            this.leftgun[0][0] = i;
            this.leftgun[0][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 1) && ((this.leftgun[0][0] != i) || (this.leftgun[0][1] != -j)) && ((this.leftgun[2][0] != i) || (this.leftgun[2][1] != -j)))
          {
            this.leftgun[1][0] = i;
            this.leftgun[1][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 2) && ((this.leftgun[1][0] != i) || (this.leftgun[1][1] != -j)) && ((this.leftgun[0][0] != i) || (this.leftgun[0][1] != -j)))
          {
            this.leftgun[2][0] = i;
            this.leftgun[2][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
        }
        if ((this.item.equals("Right gun")) && (this.dragging >= 0))
        {
          if ((this.dragging == 0) && ((this.rightgun[1][0] != i) || (this.rightgun[1][1] != -j)) && ((this.rightgun[2][0] != i) || (this.rightgun[2][1] != -j)))
          {
            this.rightgun[0][0] = i;
            this.rightgun[0][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 1) && ((this.rightgun[0][0] != i) || (this.rightgun[0][1] != -j)) && ((this.rightgun[2][0] != i) || (this.rightgun[2][1] != -j)))
          {
            this.rightgun[1][0] = i;
            this.rightgun[1][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 2) && ((this.rightgun[1][0] != i) || (this.rightgun[1][1] != -j)) && ((this.rightgun[0][0] != i) || (this.rightgun[0][1] != -j)))
          {
            this.rightgun[2][0] = i;
            this.rightgun[2][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
        }
        if ((this.item.equals("Left rear gun")) && (this.dragging >= 0))
        {
          if ((this.dragging == 0) && ((this.leftreargun[1][0] != i) || (this.leftreargun[1][1] != -j)) && ((this.leftreargun[2][0] != i) || (this.leftreargun[2][1] != -j)))
          {
            this.leftreargun[0][0] = i;
            this.leftreargun[0][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 1) && ((this.leftreargun[0][0] != i) || (this.leftreargun[0][1] != -j)) && ((this.leftreargun[2][0] != i) || (this.leftreargun[2][1] != -j)))
          {
            this.leftreargun[1][0] = i;
            this.leftreargun[1][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 2) && ((this.leftreargun[1][0] != i) || (this.leftreargun[1][1] != -j)) && ((this.leftreargun[0][0] != i) || (this.leftreargun[0][1] != -j)))
          {
            this.leftreargun[2][0] = i;
            this.leftreargun[2][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
        }
        if ((this.item.equals("Right rear gun")) && (this.dragging >= 0))
        {
          if ((this.dragging == 0) && ((this.rightreargun[1][0] != i) || (this.rightreargun[1][1] != -j)) && ((this.rightreargun[2][0] != i) || (this.rightreargun[2][1] != -j)))
          {
            this.rightreargun[0][0] = i;
            this.rightreargun[0][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 1) && ((this.rightreargun[0][0] != i) || (this.rightreargun[0][1] != -j)) && ((this.rightreargun[2][0] != i) || (this.rightreargun[2][1] != -j)))
          {
            this.rightreargun[1][0] = i;
            this.rightreargun[1][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 2) && ((this.rightreargun[1][0] != i) || (this.rightreargun[1][1] != -j)) && ((this.rightreargun[0][0] != i) || (this.rightreargun[0][1] != -j)))
          {
            this.rightreargun[2][0] = i;
            this.rightreargun[2][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
        }
        if ((this.item.equals("Left light")) && (this.dragging >= 0))
        {
          if ((this.dragging == 0) && ((this.leftlight[1][0] != i) || (this.leftlight[1][1] != -j)) && ((this.leftlight[2][0] != i) || (this.leftlight[2][1] != -j)))
          {
            this.leftlight[0][0] = i;
            this.leftlight[0][1] = (-j);
            this.change = true;
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 1) && ((this.leftlight[0][0] != i) || (this.leftlight[0][1] != -j)) && ((this.leftlight[2][0] != i) || (this.leftlight[2][1] != -j)))
          {
            this.leftlight[1][0] = i;
            this.leftlight[1][1] = (-j);
            this.change = true;
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 2) && ((this.leftlight[1][0] != i) || (this.leftlight[1][1] != -j)) && ((this.leftlight[0][0] != i) || (this.leftlight[0][1] != -j)))
          {
            this.leftlight[2][0] = i;
            this.leftlight[2][1] = (-j);
            this.change = true;
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
        }
        if ((this.item.equals("Right light")) && (this.dragging >= 0))
        {
          if ((this.dragging == 0) && ((this.rightlight[1][0] != i) || (this.rightlight[1][1] != -j)) && ((this.rightlight[2][0] != i) || (this.rightlight[2][1] != -j)))
          {
            this.rightlight[0][0] = i;
            this.rightlight[0][1] = (-j);
            this.change = true;
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 1) && ((this.rightlight[0][0] != i) || (this.rightlight[0][1] != -j)) && ((this.rightlight[2][0] != i) || (this.rightlight[2][1] != -j)))
          {
            this.rightlight[1][0] = i;
            this.rightlight[1][1] = (-j);
            this.change = true;
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 2) && ((this.rightlight[1][0] != i) || (this.rightlight[1][1] != -j)) && ((this.rightlight[0][0] != i) || (this.rightlight[0][1] != -j)))
          {
            this.rightlight[2][0] = i;
            this.rightlight[2][1] = (-j);
            this.change = true;
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
        }
        if ((this.item.equals("Missile tube")) && (this.dragging >= 0))
        {
          if ((this.dragging == 0) && ((this.missile[1][0] != i) || (this.missile[1][1] != -j)) && ((this.missile[2][0] != i) || (this.missile[2][1] != -j)) && ((this.missile[3][0] != i) || (this.missile[3][1] != -j)))
          {
            this.missile[0][0] = i;
            this.missile[0][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 1) && ((this.missile[0][0] != i) || (this.missile[0][1] != -j)) && ((this.missile[2][0] != i) || (this.missile[2][1] != -j)) && ((this.missile[3][0] != i) || (this.missile[3][1] != -j)))
          {
            this.missile[1][0] = i;
            this.missile[1][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 2) && ((this.missile[0][0] != i) || (this.missile[0][1] != -j)) && ((this.missile[1][0] != i) || (this.missile[1][1] != -j)) && ((this.missile[3][0] != i) || (this.missile[3][1] != -j)))
          {
            this.missile[2][0] = i;
            this.missile[2][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
          if ((this.dragging == 3) && ((this.missile[0][0] != i) || (this.missile[0][1] != -j)) && ((this.missile[1][0] != i) || (this.missile[1][1] != -j)) && ((this.missile[2][0] != i) || (this.missile[2][1] != -j)))
          {
            this.missile[3][0] = i;
            this.missile[3][1] = (-j);
            if (!this.firsttime)
            {
              repaint(154 + 10 * this.blokjex_ouder, 154 + 10 * this.blokjey_ouder, 9, 9);
              repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
            }
            else
            {
              repaint();
              this.firsttime = false;
            }
            this.blokjex_ouder = i;
            this.blokjey_ouder = j;
          }
        }
        if ((this.item.equals("Engine")) && (this.dragging == 0))
        {
          this.engine[0] = i;
          this.engine[1] = (-j);
          repaint(154 + 10 * this.blokjex_oud, 154 + 10 * this.blokjey_oud, 9, 9);
          repaint(154 + 10 * i, 154 + 10 * j, 9, 9);
        }
      }
      this.blokjex_oud = i;
      this.blokjey_oud = j;
    }
  }
  
  public void setHullPoints(int paramInt)
  {
    this.HullPoints = paramInt;
  }
  
  public int getHullPoints()
  {
    return this.HullPoints;
  }
  
  public void setHull(int paramInt1, int paramInt2, int paramInt3)
  {
    this.hull[paramInt3][0] = paramInt1;
    this.hull[paramInt3][1] = paramInt2;
  }
  
  public void setMainGun(int paramInt1, int paramInt2)
  {
    this.maingun[0] = paramInt1;
    this.maingun[1] = paramInt2;
  }
  
  public void setLeftGun(int paramInt1, int paramInt2, int paramInt3)
  {
    this.leftgun[paramInt3][0] = paramInt1;
    this.leftgun[paramInt3][1] = paramInt2;
  }
  
  public void setRightGun(int paramInt1, int paramInt2, int paramInt3)
  {
    this.rightgun[paramInt3][0] = paramInt1;
    this.rightgun[paramInt3][1] = paramInt2;
  }
  
  public void setLeftRearGun(int paramInt1, int paramInt2, int paramInt3)
  {
    this.leftreargun[paramInt3][0] = paramInt1;
    this.leftreargun[paramInt3][1] = paramInt2;
  }
  
  public void setRightRearGun(int paramInt1, int paramInt2, int paramInt3)
  {
    this.rightreargun[paramInt3][0] = paramInt1;
    this.rightreargun[paramInt3][1] = paramInt2;
  }
  
  public void setLeftLight(int paramInt1, int paramInt2, int paramInt3)
  {
    this.leftlight[paramInt3][0] = paramInt1;
    this.leftlight[paramInt3][1] = paramInt2;
  }
  
  public void setRightLight(int paramInt1, int paramInt2, int paramInt3)
  {
    this.rightlight[paramInt3][0] = paramInt1;
    this.rightlight[paramInt3][1] = paramInt2;
  }
  
  public void setMissile(int paramInt1, int paramInt2, int paramInt3)
  {
    this.missile[paramInt3][0] = paramInt1;
    this.missile[paramInt3][1] = paramInt2;
  }
  
  public void setEngine(int paramInt1, int paramInt2)
  {
    this.engine[0] = paramInt1;
    this.engine[1] = paramInt2;
  }
  
  public void setHullM(int[][] paramArrayOfInt, int paramInt)
  {
    this.hull = paramArrayOfInt;
    this.HullPoints = paramInt;
  }
  
  public void setMainGunM(int[] paramArrayOfInt, boolean paramBoolean)
  {
    this.maingun = paramArrayOfInt;
    this.MainGun = paramBoolean;
  }
  
  public void setLeftGunM(int[][] paramArrayOfInt, int paramInt)
  {
    this.leftgun = paramArrayOfInt;
    this.LeftGunPoints = paramInt;
  }
  
  public void setRightGunM(int[][] paramArrayOfInt, int paramInt)
  {
    this.rightgun = paramArrayOfInt;
    this.RightGunPoints = paramInt;
  }
  
  public void setLeftRearGunM(int[][] paramArrayOfInt, int paramInt)
  {
    this.leftreargun = paramArrayOfInt;
    this.LeftRearGunPoints = paramInt;
  }
  
  public void setRightRearGunM(int[][] paramArrayOfInt, int paramInt)
  {
    this.rightreargun = paramArrayOfInt;
    this.RightRearGunPoints = paramInt;
  }
  
  public void setLeftLightM(int[][] paramArrayOfInt, int paramInt)
  {
    this.leftlight = paramArrayOfInt;
    this.LeftLightPoints = paramInt;
  }
  
  public void setRightLightM(int[][] paramArrayOfInt, int paramInt)
  {
    this.rightlight = paramArrayOfInt;
    this.RightLightPoints = paramInt;
  }
  
  public void setMissileM(int[][] paramArrayOfInt, int paramInt)
  {
    this.missile = paramArrayOfInt;
    this.MissilePoints = paramInt;
  }
  
  public void setEngineM(int[] paramArrayOfInt, boolean paramBoolean)
  {
    this.engine = paramArrayOfInt;
    this.Engine = paramBoolean;
  }
  
  public int getHullX(int paramInt)
  {
    return this.hull[paramInt][0];
  }
  
  public int getHullY(int paramInt)
  {
    return this.hull[paramInt][1];
  }
  
  public boolean getEngine()
  {
    return this.Engine;
  }
  
  public int getEngineX()
  {
    return this.engine[0];
  }
  
  public int getEngineY()
  {
    return this.engine[1];
  }
  
  public boolean getMainGun()
  {
    return this.MainGun;
  }
  
  public int getMainGunX()
  {
    return this.maingun[0];
  }
  
  public int getMainGunY()
  {
    return this.maingun[1];
  }
  
  public int getLeftGunPoints()
  {
    return this.LeftGunPoints;
  }
  
  public int getLeftGunX(int paramInt)
  {
    return this.leftgun[paramInt][0];
  }
  
  public int getLeftGunY(int paramInt)
  {
    return this.leftgun[paramInt][1];
  }
  
  public int getRightGunPoints()
  {
    return this.RightGunPoints;
  }
  
  public int getRightGunX(int paramInt)
  {
    return this.rightgun[paramInt][0];
  }
  
  public int getRightGunY(int paramInt)
  {
    return this.rightgun[paramInt][1];
  }
  
  public int getLeftLightPoints()
  {
    return this.LeftLightPoints;
  }
  
  public int getLeftLightX(int paramInt)
  {
    return this.leftlight[paramInt][0];
  }
  
  public int getLeftLightY(int paramInt)
  {
    return this.leftlight[paramInt][1];
  }
  
  public int getRightLightPoints()
  {
    return this.RightLightPoints;
  }
  
  public int getRightLightX(int paramInt)
  {
    return this.rightlight[paramInt][0];
  }
  
  public int getRightLightY(int paramInt)
  {
    return this.rightlight[paramInt][1];
  }
  
  public int getLeftRearGunPoints()
  {
    return this.LeftRearGunPoints;
  }
  
  public int getLeftRearGunX(int paramInt)
  {
    return this.leftreargun[paramInt][0];
  }
  
  public int getLeftRearGunY(int paramInt)
  {
    return this.leftreargun[paramInt][1];
  }
  
  public int getRightRearGunPoints()
  {
    return this.RightRearGunPoints;
  }
  
  public int getRightRearGunX(int paramInt)
  {
    return this.rightreargun[paramInt][0];
  }
  
  public int getRightRearGunY(int paramInt)
  {
    return this.rightreargun[paramInt][1];
  }
  
  public int getMissilePoints()
  {
    return this.MissilePoints;
  }
  
  public int getMissileX(int paramInt)
  {
    return this.missile[paramInt][0];
  }
  
  public int getMissileY(int paramInt)
  {
    return this.missile[paramInt][1];
  }
  
  public boolean changed()
  {
    boolean bool = this.change;
    this.change = false;
    return bool;
  }
  
  public void setTool(String paramString)
  {
    this.tool = paramString;
    this.selected = -1;
    repaint();
  }
  
  public void setItem(String paramString)
  {
    this.item = paramString;
  }
  
  public void teken()
  {
    repaint();
    this.change = true;
  }
  
  public void reset()
  {
    this.HullPoints = 0;
    this.MainGun = false;
    this.LeftGunPoints = 0;
    this.RightGunPoints = 0;
    this.LeftRearGunPoints = 0;
    this.RightRearGunPoints = 0;
    this.LeftLightPoints = 0;
    this.RightLightPoints = 0;
    this.MissilePoints = 0;
    this.Engine = false;
    this.blokjex_oud = -90;
    this.blokjey_oud = -90;
    this.blokjex_ouder = -90;
    this.blokjey_ouder = -90;
    this.dragging = -1;
    this.selected = -1;
    repaint();
  }
}


/* Location:              D:\Java\src\JXPilot\legacy\xpeditorapplet\!\TekenBord.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */