import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

class Knoppen
  extends Canvas
{
  boolean[] keuze = { false, false, false, false, false, false, false, false };
  String richting = "nowhere";
  int nummer;
  
  public void paint(Graphics paramGraphics)
  {
    for (int i = 0; i < 2; i++)
    {
      for (int j = 0; j < 4; j++)
      {
        paramGraphics.drawRect(3 + 33 * i, 3 + 33 * j, 30, 30);
        paramGraphics.setColor(Color.blue.darker());
        if (this.keuze[(4 * i + j)] != 0) {
          paramGraphics.fillRect(4 + 33 * i, 4 + 33 * j, 29, 29);
        }
        paramGraphics.setColor(Color.black);
      }
      int[] arrayOfInt1 = { 18, 12, 24 };
      int[] arrayOfInt2 = { 10, 26, 26 };
      paramGraphics.drawPolygon(arrayOfInt1, arrayOfInt2, 3);
      paramGraphics.drawLine(7, 11, 11, 14);
      paramGraphics.drawLine(25, 14, 29, 11);
      paramGraphics.drawLine(6, 16, 10, 18);
      paramGraphics.drawLine(26, 18, 30, 16);
      paramGraphics.drawLine(6, 22, 9, 22);
      paramGraphics.drawLine(27, 22, 30, 22);
      paramGraphics.drawLine(6, 27, 8, 26);
      paramGraphics.drawLine(28, 26, 30, 27);
      arrayOfInt1[0] = 45;
      arrayOfInt1[1] = 42;
      arrayOfInt1[2] = 39;
      arrayOfInt2[0] = 12;
      arrayOfInt2[1] = 5;
      arrayOfInt2[2] = 12;
      paramGraphics.drawPolygon(arrayOfInt1, arrayOfInt2, 3);
      int[] arrayOfInt3 = { 64, 64, 55, 53, 53 };
      int[] arrayOfInt4 = { 20, 31, 31, 29, 20 };
      paramGraphics.drawPolygon(arrayOfInt3, arrayOfInt4, 5);
      paramGraphics.drawRect(55, 20, 7, 7);
      paramGraphics.drawArc(41, 9, 15, 15, 0, 90);
      paramGraphics.drawLine(56, 16, 58, 13);
      paramGraphics.drawLine(56, 16, 54, 13);
      arrayOfInt1[0] = 12;
      arrayOfInt1[1] = 9;
      arrayOfInt1[2] = 6;
      arrayOfInt2[0] = 45;
      arrayOfInt2[1] = 38;
      arrayOfInt2[2] = 45;
      paramGraphics.drawPolygon(arrayOfInt1, arrayOfInt2, 3);
      int[] arrayOfInt5 = { 31, 31, 22, 20, 20 };
      int[] arrayOfInt6 = { 53, 64, 64, 62, 53 };
      paramGraphics.drawPolygon(arrayOfInt5, arrayOfInt6, 5);
      paramGraphics.drawRect(22, 53, 7, 7);
      paramGraphics.drawArc(8, 42, 15, 15, 0, 90);
      paramGraphics.drawLine(16, 42, 18, 44);
      paramGraphics.drawLine(16, 42, 18, 40);
      arrayOfInt1[0] = 47;
      arrayOfInt1[1] = 43;
      arrayOfInt1[2] = 52;
      arrayOfInt2[0] = 42;
      arrayOfInt2[1] = 50;
      arrayOfInt2[2] = 50;
      paramGraphics.drawPolygon(arrayOfInt1, arrayOfInt2, 3);
      paramGraphics.drawOval(40, 39, 15, 15);
      paramGraphics.drawOval(39, 38, 17, 17);
      paramGraphics.drawLine(53, 52, 60, 60);
      paramGraphics.drawLine(53, 53, 60, 61);
      paramGraphics.drawArc(8, 74, 20, 20, 50, 310);
      paramGraphics.drawLine(28, 84, 31, 87);
      paramGraphics.drawLine(28, 84, 25, 87);
      paramGraphics.drawArc(41, 74, 20, 20, 180, 310);
      paramGraphics.drawLine(41, 84, 38, 87);
      paramGraphics.drawLine(41, 84, 44, 87);
      arrayOfInt1[0] = 5;
      arrayOfInt1[1] = 10;
      arrayOfInt1[2] = 15;
      arrayOfInt2[0] = 120;
      arrayOfInt2[1] = 110;
      arrayOfInt2[2] = 120;
      paramGraphics.drawPolygon(arrayOfInt1, arrayOfInt2, 3);
      arrayOfInt1[0] = 31;
      arrayOfInt1[1] = 26;
      arrayOfInt1[2] = 21;
      arrayOfInt2[0] = 120;
      arrayOfInt2[1] = 110;
      arrayOfInt2[2] = 120;
      paramGraphics.drawPolygon(arrayOfInt1, arrayOfInt2, 3);
      paramGraphics.drawLine(18, 106, 18, 127);
      arrayOfInt1[0] = 56;
      arrayOfInt1[1] = 46;
      arrayOfInt1[2] = 56;
      arrayOfInt2[0] = 104;
      arrayOfInt2[1] = 109;
      arrayOfInt2[2] = 114;
      paramGraphics.drawPolygon(arrayOfInt1, arrayOfInt2, 3);
      arrayOfInt1[0] = 56;
      arrayOfInt1[1] = 46;
      arrayOfInt1[2] = 56;
      arrayOfInt2[0] = 120;
      arrayOfInt2[1] = 125;
      arrayOfInt2[2] = 130;
      paramGraphics.drawPolygon(arrayOfInt1, arrayOfInt2, 3);
      paramGraphics.drawLine(40, 117, 62, 117);
      paramGraphics.setColor(Color.blue.darker());
      int[] arrayOfInt7;
      int[] arrayOfInt8;
      if (this.richting.equals("left"))
      {
        arrayOfInt7 = new int[] { 5, 19, 33, 19 };
        arrayOfInt8 = new int[] { 175, 161, 175, 189 };
        paramGraphics.fillPolygon(arrayOfInt7, arrayOfInt8, 4);
      }
      if (this.richting.equals("right"))
      {
        arrayOfInt7 = new int[] { 35, 49, 63, 49 };
        arrayOfInt8 = new int[] { 175, 161, 175, 189 };
        paramGraphics.fillPolygon(arrayOfInt7, arrayOfInt8, 4);
      }
      if (this.richting.equals("up"))
      {
        arrayOfInt7 = new int[] { 20, 34, 48, 34 };
        arrayOfInt8 = new int[] { 160, 146, 160, 174 };
        paramGraphics.fillPolygon(arrayOfInt7, arrayOfInt8, 4);
      }
      if (this.richting.equals("down"))
      {
        arrayOfInt7 = new int[] { 20, 34, 48, 34 };
        arrayOfInt8 = new int[] { 190, 176, 190, 204 };
        paramGraphics.fillPolygon(arrayOfInt7, arrayOfInt8, 4);
      }
      if (this.richting.equals("center")) {
        paramGraphics.setColor(Color.blue.darker());
      } else {
        paramGraphics.setColor(Color.blue);
      }
      paramGraphics.fillOval(24, 165, 20, 20);
      paramGraphics.setColor(Color.black);
      paramGraphics.drawLine(4, 175, 34, 145);
      paramGraphics.drawLine(4, 175, 34, 205);
      paramGraphics.drawLine(34, 145, 64, 175);
      paramGraphics.drawLine(34, 205, 64, 175);
      paramGraphics.drawLine(19, 160, 26, 167);
      paramGraphics.drawLine(42, 183, 49, 190);
      paramGraphics.drawLine(19, 190, 26, 183);
      paramGraphics.drawLine(42, 167, 49, 160);
      paramGraphics.drawOval(24, 165, 20, 20);
    }
  }
  
  public void turnOn(int paramInt)
  {
    this.nummer = paramInt;
    this.keuze[paramInt] = true;
    repaint(4 + 33 * (paramInt - paramInt % 4) / 4, 4 + 33 * (paramInt % 4), 29, 29);
  }
  
  public void turnOff()
  {
    this.keuze[this.nummer] = false;
    repaint(4 + 33 * (this.nummer - this.nummer % 4) / 4, 4 + 33 * (this.nummer % 4), 29, 29);
  }
  
  public void move(String paramString)
  {
    this.richting = paramString;
    repaint(4, 145, 60, 60);
  }
}


/* Location:              D:\Java\src\JXPilot\legacy\xpeditorapplet\!\Knoppen.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */