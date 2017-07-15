import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;

class Doek
  extends Canvas
{
  boolean[] error = new boolean[6];
  
  public void setErrors(boolean[] paramArrayOfBoolean)
  {
    this.error = paramArrayOfBoolean;
  }
  
  public void paint(Graphics paramGraphics)
  {
    paramGraphics.setColor(Color.white);
    if (this.error[0] != 0)
    {
      paramGraphics.setColor(Color.green);
      paramGraphics.drawString("One hull point is at least eight points to the left of the center.", 5, 20);
    }
    else
    {
      paramGraphics.setColor(Color.red);
      paramGraphics.drawString("One hullpoint must be at at least eight points to the left from the center", 5, 20);
    }
    if (this.error[1] != 0)
    {
      paramGraphics.setColor(Color.green);
      paramGraphics.drawString("One hull point is at least eight points to the right of the center.", 5, 40);
    }
    else
    {
      paramGraphics.setColor(Color.red);
      paramGraphics.drawString("One hull point must be at least eight points to the right of the center.", 5, 40);
    }
    if (this.error[2] != 0)
    {
      paramGraphics.setColor(Color.green);
      paramGraphics.drawString("One hull point is at least eight points above the center.", 5, 60);
    }
    else
    {
      paramGraphics.setColor(Color.red);
      paramGraphics.drawString("One hull point must be at least eight points above the center.", 5, 60);
    }
    if (this.error[3] != 0)
    {
      paramGraphics.setColor(Color.green);
      paramGraphics.drawString("One hull point is at least eight points below the center.", 5, 80);
    }
    else
    {
      paramGraphics.setColor(Color.red);
      paramGraphics.drawString("One hull point must be at least eight points below the center.", 5, 80);
    }
    if (this.error[4] != 0)
    {
      paramGraphics.setColor(Color.green);
      paramGraphics.drawString("Height plus width is at least 38", 5, 100);
    }
    else
    {
      paramGraphics.setColor(Color.red);
      paramGraphics.drawString("Height plus width must be at least 38", 5, 100);
    }
    if (this.error[5] == 0)
    {
      paramGraphics.setColor(Color.green);
      paramGraphics.drawString("All ship components are inside shipshape", 5, 120);
      return;
    }
    paramGraphics.setColor(Color.red);
    paramGraphics.drawString("All ship components must be inside shipshape", 5, 120);
  }
}


/* Location:              D:\Java\src\JXPilot\legacy\xpeditorapplet\!\Doek.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */