import java.awt.Button;
import java.awt.Component;
import java.awt.Event;
import java.awt.Frame;

class SaveFrame
  extends Frame
{
  public boolean action(Event paramEvent, Object paramObject)
  {
    if ((paramEvent.target instanceof Button)) {
      hide();
    }
    return true;
  }
}


/* Location:              D:\Java\src\JXPilot\legacy\xpeditorapplet\!\SaveFrame.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */