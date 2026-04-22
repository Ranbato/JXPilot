import java.awt.Canvas;
import java.awt.Component;
import java.awt.Graphics;

class Message
  extends Canvas
{
  Message()
  {
    resize(480, 25);
  }
  
  public void paint(Graphics paramGraphics)
  {
    paramGraphics.drawString("Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot", 0, 17);
  }
}


/* Location:              D:\Java\src\JXPilot\legacy\xpeditorapplet\!\Message.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */