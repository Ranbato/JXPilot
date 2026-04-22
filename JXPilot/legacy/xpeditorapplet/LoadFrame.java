import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Event;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextComponent;

class LoadFrame
  extends Frame
{
  TextArea loadstring = new TextArea(2, 45);
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
  String shipname = "";
  String author = "";
  boolean change = false;
  
  LoadFrame()
  {
    setLayout(new BorderLayout());
    Panel localPanel1 = new Panel();
    Panel localPanel2 = new Panel();
    localPanel1.add(this.loadstring);
    localPanel2.add(new Button("Load"));
    localPanel2.add(new Button("Cancel"));
    add("North", localPanel1);
    add("Center", localPanel2);
  }
  
  public boolean action(Event paramEvent, Object paramObject)
  {
    if ((paramEvent.target instanceof Button)) {
      if (((String)paramObject).equals("Cancel"))
      {
        hide();
      }
      else
      {
        this.HullPoints = 0;
        this.MainGun = false;
        this.LeftGunPoints = 0;
        this.RightGunPoints = 0;
        this.LeftLightPoints = 0;
        this.RightLightPoints = 0;
        this.LeftRearGunPoints = 0;
        this.RightRearGunPoints = 0;
        this.MissilePoints = 0;
        this.Engine = false;
        this.shipname = "";
        this.author = "";
        int i = 0;
        int j = 0;
        hide();
        for (int k = 0; k < 2; k++)
        {
          i = this.loadstring.getText().indexOf("NM:") + 3;
          j = this.loadstring.getText().indexOf(")", i);
          if ((i > 5) && (j > 0))
          {
            this.shipname = this.loadstring.getText().substring(i, j);
            this.shipname = this.shipname.trim();
          }
          i = this.loadstring.getText().indexOf("AU:") + 3;
          j = this.loadstring.getText().indexOf(")", i);
          if ((i > 5) && (j > 0))
          {
            this.author = this.loadstring.getText().substring(i, j);
            this.author = this.author.trim();
          }
          i = this.loadstring.getText().indexOf("SH:");
          int m;
          int n;
          int i1;
          int i2;
          int i3;
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 0;
            i2 = 32;
            i3 = 0;
            while (i2 != 41)
            {
              i2 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i2 == 45) || ((i2 >= 48) && (i2 <= 57)))
              {
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 44)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.hull[n][0] = i1;
                i3 = 0;
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                this.hull[n][1] = i1;
                n++;
                i3 = 0;
                this.HullPoints = n;
              }
              m++;
            }
            this.change = true;
          }
          i = this.loadstring.getText().indexOf("MG:");
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 32;
            i2 = 0;
            while (i1 != 41)
            {
              i1 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i1 == 45) || ((i1 >= 48) && (i1 <= 57)))
              {
                if (i1 == 45)
                {
                  i2 = 1;
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i1 >= 48) && (i1 <= 57))
                {
                  n = 48 - i1;
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i1 >= 48) && (i1 <= 57))
                {
                  n = 10 * n + (48 - i1);
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 0) {
                  n = -n;
                }
                while (i1 == 32)
                {
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i1 == 44)
                {
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.maingun[0] = n;
                i2 = 0;
                while (i1 == 32)
                {
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i1 == 45)
                {
                  i2 = 1;
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i1 >= 48) && (i1 <= 57))
                {
                  n = 48 - i1;
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i1 >= 48) && (i1 <= 57))
                {
                  n = 10 * n + (48 - i1);
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 0) {
                  n = -n;
                }
                this.maingun[1] = n;
                i2 = 0;
                this.MainGun = true;
              }
              m++;
            }
          }
          i = this.loadstring.getText().indexOf("LG:");
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 0;
            i2 = 32;
            i3 = 0;
            while (i2 != 41)
            {
              i2 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i2 == 45) || ((i2 >= 48) && (i2 <= 57)))
              {
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 44)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.leftgun[n][0] = i1;
                i3 = 0;
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                this.leftgun[n][1] = i1;
                n++;
                i3 = 0;
                this.LeftGunPoints = n;
              }
              m++;
            }
          }
          i = this.loadstring.getText().indexOf("RG:");
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 0;
            i2 = 32;
            i3 = 0;
            while (i2 != 41)
            {
              i2 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i2 == 45) || ((i2 >= 48) && (i2 <= 57)))
              {
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 44)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.rightgun[n][0] = i1;
                i3 = 0;
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                this.rightgun[n][1] = i1;
                n++;
                i3 = 0;
                this.RightGunPoints = n;
              }
              m++;
            }
          }
          i = this.loadstring.getText().indexOf("LR:");
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 0;
            i2 = 32;
            i3 = 0;
            while (i2 != 41)
            {
              i2 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i2 == 45) || ((i2 >= 48) && (i2 <= 57)))
              {
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 44)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.leftreargun[n][0] = i1;
                i3 = 0;
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                this.leftreargun[n][1] = i1;
                n++;
                i3 = 0;
                this.LeftRearGunPoints = n;
              }
              m++;
            }
          }
          i = this.loadstring.getText().indexOf("RR:");
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 0;
            i2 = 32;
            i3 = 0;
            while (i2 != 41)
            {
              i2 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i2 == 45) || ((i2 >= 48) && (i2 <= 57)))
              {
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 44)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.rightreargun[n][0] = i1;
                i3 = 0;
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                this.rightreargun[n][1] = i1;
                n++;
                i3 = 0;
                this.RightRearGunPoints = n;
              }
              m++;
            }
          }
          i = this.loadstring.getText().indexOf("LL:");
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 0;
            i2 = 32;
            i3 = 0;
            while (i2 != 41)
            {
              i2 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i2 == 45) || ((i2 >= 48) && (i2 <= 57)))
              {
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 44)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.leftlight[n][0] = i1;
                i3 = 0;
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                this.leftlight[n][1] = i1;
                n++;
                i3 = 0;
                this.LeftLightPoints = n;
              }
              m++;
            }
          }
          i = this.loadstring.getText().indexOf("RL:");
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 0;
            i2 = 32;
            i3 = 0;
            while (i2 != 41)
            {
              i2 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i2 == 45) || ((i2 >= 48) && (i2 <= 57)))
              {
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 44)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.rightlight[n][0] = i1;
                i3 = 0;
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                this.rightlight[n][1] = i1;
                n++;
                i3 = 0;
                this.RightLightPoints = n;
              }
              m++;
            }
          }
          i = this.loadstring.getText().indexOf("MR:");
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 0;
            i2 = 32;
            i3 = 0;
            while (i2 != 41)
            {
              i2 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i2 == 45) || ((i2 >= 48) && (i2 <= 57)))
              {
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 44)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.missile[n][0] = i1;
                i3 = 0;
                while (i2 == 32)
                {
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 45)
                {
                  i3 = 1;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 48 - i2;
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i2 >= 48) && (i2 <= 57))
                {
                  i1 = 10 * i1 + (48 - i2);
                  m++;
                  i2 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i3 == 0) {
                  i1 = -i1;
                }
                this.missile[n][1] = i1;
                n++;
                i3 = 0;
                this.MissilePoints = n;
              }
              m++;
            }
          }
          i = this.loadstring.getText().indexOf("EN:");
          if (i >= 0)
          {
            m = 0;
            n = 0;
            i1 = 32;
            i2 = 0;
            while (i1 != 41)
            {
              i1 = this.loadstring.getText().charAt(i + 3 + m);
              if ((i1 == 45) || ((i1 >= 48) && (i1 <= 57)))
              {
                if (i1 == 45)
                {
                  i2 = 1;
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i1 >= 48) && (i1 <= 57))
                {
                  n = 48 - i1;
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i1 >= 48) && (i1 <= 57))
                {
                  n = 10 * n + (48 - i1);
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 0) {
                  n = -n;
                }
                while (i1 == 32)
                {
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i1 == 44)
                {
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                this.engine[0] = n;
                i2 = 0;
                while (i1 == 32)
                {
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i1 == 45)
                {
                  i2 = 1;
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i1 >= 48) && (i1 <= 57))
                {
                  n = 48 - i1;
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if ((i1 >= 48) && (i1 <= 57))
                {
                  n = 10 * n + (48 - i1);
                  m++;
                  i1 = this.loadstring.getText().charAt(i + 3 + m);
                }
                if (i2 == 0) {
                  n = -n;
                }
                this.engine[1] = n;
                i2 = 0;
                this.Engine = true;
              }
              m++;
            }
          }
        }
      }
    }
    return true;
  }
  
  public int[][] getHull()
  {
    return this.hull;
  }
  
  public int getHullPoints()
  {
    return this.HullPoints;
  }
  
  public boolean getMainGun()
  {
    return this.MainGun;
  }
  
  public int[] getmaingun()
  {
    return this.maingun;
  }
  
  public int getLeftGunPoints()
  {
    return this.LeftGunPoints;
  }
  
  public int[][] getLeftGun()
  {
    return this.leftgun;
  }
  
  public int getRightGunPoints()
  {
    return this.RightGunPoints;
  }
  
  public int[][] getRightGun()
  {
    return this.rightgun;
  }
  
  public int getLeftRearGunPoints()
  {
    return this.LeftRearGunPoints;
  }
  
  public int[][] getLeftRearGun()
  {
    return this.leftreargun;
  }
  
  public int getRightRearGunPoints()
  {
    return this.RightRearGunPoints;
  }
  
  public int[][] getRightRearGun()
  {
    return this.rightreargun;
  }
  
  public int getLeftLightPoints()
  {
    return this.LeftLightPoints;
  }
  
  public int[][] getLeftLight()
  {
    return this.leftlight;
  }
  
  public int getRightLightPoints()
  {
    return this.RightLightPoints;
  }
  
  public int[][] getRightLight()
  {
    return this.rightlight;
  }
  
  public int getMissilePoints()
  {
    return this.MissilePoints;
  }
  
  public int[][] getMissile()
  {
    return this.missile;
  }
  
  public boolean getEngine()
  {
    return this.Engine;
  }
  
  public int[] getengine()
  {
    return this.engine;
  }
  
  public String getName()
  {
    return this.shipname;
  }
  
  public String getAuthor()
  {
    return this.author;
  }
  
  public boolean changed()
  {
    boolean bool = this.change;
    this.change = false;
    return bool;
  }
}


/* Location:              D:\Java\src\JXPilot\legacy\xpeditorapplet\!\LoadFrame.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */