package shipshaper;

import java.util.EventListener;

public interface ShipListener
  extends EventListener
{
  public abstract void shipChanged(ShipEvent paramShipEvent);
}


/* Location:              D:\Java\src\JXPilot\legacy\ShipShaper\ss.jar!\shipshaper\ShipListener.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */