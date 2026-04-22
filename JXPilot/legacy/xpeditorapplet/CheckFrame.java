import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Event;
import java.awt.Frame;
import java.awt.Panel;

class CheckFrame
  extends Frame
{
  Doek doek = new Doek();
  
  CheckFrame()
  {
    setBackground(Color.black);
    setLayout(new BorderLayout());
    Panel localPanel1 = new Panel();
    Panel localPanel2 = new Panel();
    add("Center", localPanel1);
    add("South", localPanel2);
    this.doek.resize(400, 200);
    this.doek.setBackground(Color.black);
    localPanel1.add(this.doek);
    localPanel2.add(new Button("   Ok   "));
  }
  
  public void setErrors(boolean[] paramArrayOfBoolean)
  {
    this.doek.setErrors(paramArrayOfBoolean);
  }
  
  public boolean action(Event paramEvent, Object paramObject)
  {
    hide();
    return true;
  }
}


/* Location:              D:\Java\src\JXPilot\legacy\xpeditorapplet\!\CheckFrame.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */