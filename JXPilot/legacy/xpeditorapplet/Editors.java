import java.applet.*;
import java.awt.*;
import java.net.*;

public class Editors extends Applet implements Runnable
{
	Thread t1;
	boolean working=true;
	double[][] var_coord = new double[24][2];
	double[][] var_left = new double[3][2];
	double[][] var_right = new double[3][2];
	double[] hoek = new double[24];
	double[] lhoek = new double[3];
	double[] rhoek = new double[3];
	double[] straal = new double[24];
	double[] lstraal = new double[3];
	double[] rstraal = new double[3];
	DraaiSchip preview = new DraaiSchip();
	TekenBord editzone = new TekenBord();
	Knoppen knoppen = new Knoppen();
	SaveFrame saveframe = new SaveFrame();
	LoadFrame loadframe = new LoadFrame();
	CheckFrame checkframe = new CheckFrame();
	Message message = new Message();
	TextField shipname = new TextField(33);
	TextField author = new TextField("http://www.j-a-r-n-o.nl/xhome.html",36);
	TextArea savestring = new TextArea(2, 45);
	Choice item = new Choice();
	int num=-10;
	String str="nowhere";
	int[][] ship = new int[33][33];
	int modifier=1;

	public void init()
	{
		resize(500,445);
		setBackground(Color.blue.darker());

		author.setBackground(Color.white);
		shipname.setBackground(Color.white);

		Choice tool = new Choice();
                tool.addItem("Add");
                tool.addItem("Move");
                tool.addItem("Remove");
                tool.addItem("Insert");

                item.addItem("Hull");
                item.addItem("Main gun");
                item.addItem("Left gun");
                item.addItem("Right gun");
                item.addItem("Left rear gun");
                item.addItem("Right rear gun");
                item.addItem("Left light");
                item.addItem("Right light");
                item.addItem("Missile tube");
                item.addItem("Engine");

		setLayout(new BorderLayout());

		preview.setBackground(Color.black);
		preview.resize(47,47);

		editzone.setBackground(Color.black);
		editzone.resize(317,332);
		editzone.setTool("Add");
		editzone.setItem("Hull");

		knoppen.setBackground(Color.blue);
		knoppen.resize(70,206);

		Panel rightPanel = new Panel();
		rightPanel.setLayout(new BorderLayout());

		Panel toolPanel = new Panel();
		Panel itemPanel = new Panel();
		Panel previewPanel = new Panel();
		Panel topRightPanel = new Panel();
		Panel knoppenPanel = new Panel();

		toolPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		itemPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		topRightPanel.setLayout(new GridLayout(2,1));

		toolPanel.add(tool);
		itemPanel.add(item);
		previewPanel.add(preview);
		topRightPanel.add(toolPanel);
		topRightPanel.add(itemPanel);
		knoppenPanel.add(knoppen);

                rightPanel.add("North",topRightPanel);
                rightPanel.add("Center",knoppenPanel);
                rightPanel.add("South",previewPanel);

		Panel editzonePanel = new Panel();
		editzonePanel.add(editzone);

		Panel labelPanel = new Panel();
		labelPanel.add(message);
//		message.start();

		Panel namesPanel = new Panel();

		namesPanel.setLayout(new GridLayout(2,1));

		Panel shipnamePanel = new Panel();
		Panel authorPanel = new Panel();

		shipnamePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		authorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		shipnamePanel.add(new Label("ShipName : "));
		shipnamePanel.add(shipname);

		authorPanel.add(new Label("Author : "));
		authorPanel.add(author);

		namesPanel.add(shipnamePanel);
		namesPanel.add(authorPanel);

                add("North",labelPanel);
                add("East",rightPanel);
                add("Center",editzonePanel);
                add("South",namesPanel);

		Panel temp1 = new Panel();
		Panel temp2 = new Panel();

		saveframe.resize(400,170);
		saveframe.setTitle("Copy ALL of this text");
		saveframe.setLayout(new BorderLayout());
		temp1.add(savestring);
		temp2.add(new Button("Close"));
		saveframe.add("North", temp1);
		saveframe.add("South", temp2);

		savestring.setEditable(false);

		loadframe.resize(400,170);
		loadframe.setTitle("Paste text here...");
		loadframe.setLayout(new FlowLayout());

		checkframe.resize(400,240);
		checkframe.setTitle("What errors?");
	}

	public void start()
	{
		if (t1 == null)
		{
			t1 = new Thread(this);
			t1.start();
		}
	}

	public void stop()
	{
		if (t1 != null)
		{
			t1.stop();
			t1 = null;
		}
	}

	public void pause(int duur)
	{
		try {t1.sleep(duur);}
		catch (InterruptedException e) {}
	}

	public void run()
	{
		double x=0;

		for(;;)
		{
			for(int i=0;i<editzone.getHullPoints();i++)
			{
				var_coord[i][0] = straal[i]*Math.sin(hoek[i]+x);
				var_coord[i][1] = straal[i]*Math.cos(hoek[i]+x);
				preview.setPoint((int)(var_coord[i][0]+.5),(int)(var_coord[i][1]+.5));
			}

			for(int i=0;i<editzone.getLeftLightPoints();i++)
			{
				var_left[i][0] = lstraal[i]*Math.sin(lhoek[i]+x);
				var_left[i][1] = lstraal[i]*Math.cos(lhoek[i]+x);
				preview.setLPoint((int)(var_left[i][0]+.5),(int)(var_left[i][1]+.5));
			}

			for(int i=0;i<editzone.getRightLightPoints();i++)
			{
				var_right[i][0] = rstraal[i]*Math.sin(rhoek[i]+x);
				var_right[i][1] = rstraal[i]*Math.cos(rhoek[i]+x);
				preview.setRPoint((int)(var_right[i][0]+.5),(int)(var_right[i][1]+.5));
			}

			preview.setAngle(x);
			preview.teken();

			if(working)
			{
				x += modifier*Math.PI / 20;
				if(x>2*Math.PI || x<-2*Math.PI) x = 0;
			}

			if(working) pause(60); else pause(500);

			if(editzone.changed()) recalculate();
			if(loadframe.changed())
			{
				editzone.setHullM(loadframe.getHull(), loadframe.getHullPoints());
				editzone.setMainGunM(loadframe.getmaingun(), loadframe.getMainGun());
				editzone.setLeftGunM(loadframe.getLeftGun(), loadframe.getLeftGunPoints());
				editzone.setRightGunM(loadframe.getRightGun(), loadframe.getRightGunPoints());
				editzone.setLeftRearGunM(loadframe.getLeftRearGun(), loadframe.getLeftRearGunPoints());
				editzone.setRightRearGunM(loadframe.getRightRearGun(), loadframe.getRightRearGunPoints());
				editzone.setLeftLightM(loadframe.getLeftLight(), loadframe.getLeftLightPoints());
				editzone.setRightLightM(loadframe.getRightLight(), loadframe.getRightLightPoints());
				editzone.setMissileM(loadframe.getMissile(), loadframe.getMissilePoints());
				editzone.setEngineM(loadframe.getengine(), loadframe.getEngine());
				shipname.setText(loadframe.getName());
				author.setText(loadframe.getAuthor());
				editzone.teken();
				recalculate();
			}
		}
	}

	public void recalculate()
	{
		for(int i=0;i<editzone.getHullPoints();i++)
		{
			straal[i] = Math.sqrt(editzone.getHullX(i)*editzone.getHullX(i)+editzone.getHullY(i)*editzone.getHullY(i));
			hoek[i] = Math.acos(editzone.getHullY(i) / straal[i]);
			if(editzone.getHullX(i)< 0) hoek[i] = 2*Math.PI - hoek[i];
		}

		for(int i=0;i<editzone.getLeftLightPoints();i++)
		{
			lstraal[i] = Math.sqrt(editzone.getLeftLightX(i)*editzone.getLeftLightX(i)+editzone.getLeftLightY(i)*editzone.getLeftLightY(i));
			lhoek[i] = Math.acos(editzone.getLeftLightY(i) / lstraal[i]);
			if(editzone.getLeftLightX(i)< 0) lhoek[i] = 2*Math.PI - lhoek[i];
		}

		for(int i=0;i<editzone.getRightLightPoints();i++)
		{
			rstraal[i] = Math.sqrt(editzone.getRightLightX(i)*editzone.getRightLightX(i)+editzone.getRightLightY(i)*editzone.getRightLightY(i));
			rhoek[i] = Math.acos(editzone.getRightLightY(i) / rstraal[i]);
			if(editzone.getRightLightX(i)< 0) rhoek[i] = 2*Math.PI - rhoek[i];
		}
	}

	public boolean action(Event e, Object arg)
	{
		if(e.target instanceof Choice)
		{
			if(((String)arg).equals("Add") || ((String)arg).equals("Remove") || ((String)arg).equals("Move") || ((String)arg).equals("Insert"))
			{
				editzone.setTool((String)arg);
			}
			else
			{
				editzone.setItem((String)arg);
			}

			if(((String)arg).equals("Insert"))
			{
				item.select(0);
				editzone.setItem("Hull");
			}
			if(((String)arg).equals("Add") || ((String)arg).equals("Remove") || ((String)arg).equals("Move"))
			{
				if(item.countItems() == 1)
				{
                                        item.addItem("Main gun");
                                        item.addItem("Left gun");
                                        item.addItem("Right gun");
                                        item.addItem("Left rear gun");
                                        item.addItem("Right rear gun");
                                        item.addItem("Left light");
                                        item.addItem("Right light");
                                        item.addItem("Missile tube");
                                        item.addItem("Engine");
				}
			}
			editzone.teken();
		}

		return true;
	}

	public boolean mouseDown(Event e, int x, int y)
	{
		if(x>410 && x<473 && y<240 && y>110)
		{
			num=-10;
			boolean good=false;

			if(x>410 && x<440) num = 0; else
			if(x>443 && x<473) num = 4;

			if(y<240 && y>210) {num+=3;good=true;}
			if(y<207 && y>177) {num+=2;good=true;}
			if(y<174 && y>144) {num+=1;good=true;}
			if(y<141 && y>111) good=true;

			if(num >= 0 && good) knoppen.turnOn(num);
		}

		if(y>x-441+253 && y>-x+441+253 && y<x-441+313 && y<-x+441+313)
		{
			str="not";

			if(y>x-441+283)
			{
				if(y>-x+441+283)
				{
					str = "down";
				}
				else
				{
					str = "left";
				}
			}
			else
			{
				if(y>-x+441+283)
				{
					str = "right";
				}
				else
				{
					str = "up";
				}
			}

			if(((x-441)*(x-441) + (y-283)*(y-283)) < 120) str = "center";

			knoppen.move(str);
		}

		if(x>418 && x<465 && y>327 && y<372)
		{
			working = !working;
		}

		return true;
	}

	public boolean mouseUp(Event e, int x, int y)
	{
		boolean good=false;
		int tempNum=-10;
		String tempStr="ejhjlk";

		knoppen.move("nothing");
		knoppen.turnOff();

		if(x>410 && x<440) tempNum = 0; else
		if(x>443 && x<473) tempNum = 4;

		if(y<240 && y>210) {tempNum+=3;good=true;}
		if(y<207 && y>177) {tempNum+=2;good=true;}
		if(y<174 && y>144) {tempNum+=1;good=true;}
		if(y<141 && y>111) good=true;

		if(y>x-441+253 && y>-x+441+253 && y<x-441+313 && y<-x+441+313)
		{
			tempStr="not";

			if(y>x-441+283)
			{
				if(y>-x+441+283)
				{
					tempStr = "down";
				}
				else
				{
					tempStr = "left";
				}
			}
			else
			{
				if(y>-x+441+283)
				{
					tempStr = "right";
				}
				else
				{
					tempStr = "up";
				}
			}

			if(((x-441)*(x-441) + (y-283)*(y-283)) < 120) tempStr = "center";
		}

		if(tempNum >= 0 && good && tempNum == num)
		{
			if(num == 0)
			{
				editzone.reset();
				shipname.setText("");
				author.setText("http://www.j-a-r-n-o.nl/xhome.html");
			}

			if(num == 1)
			{
				if(!loadframe.isShowing()) loadframe.show(); else loadframe.toFront();
				if(saveframe.isShowing()) saveframe.hide();
			}

			if(num == 2 || num == 3|| num == 6 || num == 7)
			{
				int[] temp = new int[24];

				for(int i=0;i<editzone.getHullPoints();i++)
				{
					if(num == 2) editzone.setHull(-editzone.getHullY(i),editzone.getHullX(i),i);
					if(num == 6) editzone.setHull(editzone.getHullY(i),-editzone.getHullX(i),i);
					if(num == 3) editzone.setHull(-editzone.getHullX(i),editzone.getHullY(i),i);
					if(num == 7) editzone.setHull(editzone.getHullX(i),-editzone.getHullY(i),i);
				}

				if(editzone.getMainGun())
				{
					if(num == 2) editzone.setMainGun(-editzone.getMainGunY(),editzone.getMainGunX());
					if(num == 6) editzone.setMainGun(editzone.getMainGunY(),-editzone.getMainGunX());
					if(num == 3) editzone.setMainGun(-editzone.getMainGunX(),editzone.getMainGunY());
					if(num == 7) editzone.setMainGun(editzone.getMainGunX(),-editzone.getMainGunY());
				}

				for(int i=0;i<editzone.getLeftGunPoints();i++)
				{
					if(num == 2) editzone.setLeftGun(-editzone.getLeftGunY(i),editzone.getLeftGunX(i),i);
					if(num == 6) editzone.setLeftGun(editzone.getLeftGunY(i),-editzone.getLeftGunX(i),i);
					if(num == 3) editzone.setLeftGun(-editzone.getLeftGunX(i),editzone.getLeftGunY(i),i);
					if(num == 7) editzone.setLeftGun(editzone.getLeftGunX(i),-editzone.getLeftGunY(i),i);
				}

				for(int i=0;i<editzone.getRightGunPoints();i++)
				{
					if(num == 2) editzone.setRightGun(-editzone.getRightGunY(i),editzone.getRightGunX(i),i);
					if(num == 6) editzone.setRightGun(editzone.getRightGunY(i),-editzone.getRightGunX(i),i);
					if(num == 3) editzone.setRightGun(-editzone.getRightGunX(i),editzone.getRightGunY(i),i);
					if(num == 7) editzone.setRightGun(editzone.getRightGunX(i),-editzone.getRightGunY(i),i);
				}

				for(int i=0;i<editzone.getLeftRearGunPoints();i++)
				{
					if(num == 2) editzone.setLeftRearGun(-editzone.getLeftRearGunY(i),editzone.getLeftRearGunX(i),i);
					if(num == 6) editzone.setLeftRearGun(editzone.getLeftRearGunY(i),-editzone.getLeftRearGunX(i),i);
					if(num == 3) editzone.setLeftRearGun(-editzone.getLeftRearGunX(i),editzone.getLeftRearGunY(i),i);
					if(num == 7) editzone.setLeftRearGun(editzone.getLeftRearGunX(i),-editzone.getLeftRearGunY(i),i);
				}

				for(int i=0;i<editzone.getRightRearGunPoints();i++)
				{
					if(num == 2) editzone.setRightRearGun(-editzone.getRightRearGunY(i),editzone.getRightRearGunX(i),i);
					if(num == 6) editzone.setRightRearGun(editzone.getRightRearGunY(i),-editzone.getRightRearGunX(i),i);
					if(num == 3) editzone.setRightRearGun(-editzone.getRightRearGunX(i),editzone.getRightRearGunY(i),i);
					if(num == 7) editzone.setRightRearGun(editzone.getRightRearGunX(i),-editzone.getRightRearGunY(i),i);
				}

				for(int i=0;i<editzone.getLeftLightPoints();i++)
				{
					if(num == 2) editzone.setLeftLight(-editzone.getLeftLightY(i),editzone.getLeftLightX(i),i);
					if(num == 6) editzone.setLeftLight(editzone.getLeftLightY(i),-editzone.getLeftLightX(i),i);
					if(num == 3) editzone.setLeftLight(-editzone.getLeftLightX(i),editzone.getLeftLightY(i),i);
					if(num == 7) editzone.setLeftLight(editzone.getLeftLightX(i),-editzone.getLeftLightY(i),i);
				}

				for(int i=0;i<editzone.getRightLightPoints();i++)
				{
					if(num == 2) editzone.setRightLight(-editzone.getRightLightY(i),editzone.getRightLightX(i),i);
					if(num == 6) editzone.setRightLight(editzone.getRightLightY(i),-editzone.getRightLightX(i),i);
					if(num == 3) editzone.setRightLight(-editzone.getRightLightX(i),editzone.getRightLightY(i),i);
					if(num == 7) editzone.setRightLight(editzone.getRightLightX(i),-editzone.getRightLightY(i),i);
				}

				for(int i=0;i<editzone.getMissilePoints();i++)
				{
					if(num == 2) editzone.setMissile(-editzone.getMissileY(i),editzone.getMissileX(i),i);
					if(num == 6) editzone.setMissile(editzone.getMissileY(i),-editzone.getMissileX(i),i);
					if(num == 3) editzone.setMissile(-editzone.getMissileX(i),editzone.getMissileY(i),i);
					if(num == 7) editzone.setMissile(editzone.getMissileX(i),-editzone.getMissileY(i),i);
				}

				if(editzone.getEngine())
				{
					if(num == 2) editzone.setEngine(-editzone.getEngineY(),editzone.getEngineX());
					if(num == 6) editzone.setEngine(editzone.getEngineY(),-editzone.getEngineX());
					if(num == 3) editzone.setEngine(-editzone.getEngineX(),editzone.getEngineY());
					if(num == 7) editzone.setEngine(editzone.getEngineX(),-editzone.getEngineY());
				}

				editzone.teken();
			}

			if(num == 4)
			{
				String str="xpilot.shipShape: ";

				if(!saveframe.isShowing()) saveframe.show(); else saveframe.toFront();
				if(loadframe.isShowing()) loadframe.hide();

				if(shipname.getText().length() != 0) str += "(NM: "+shipname.getText()+")";
				if(author.getText().length() != 0) str += "(AU: "+author.getText()+")";

				str+= "(SH:";
				for(int i=0;i<editzone.getHullPoints();i++)
				{
					str+= " "+editzone.getHullX(i)+","+editzone.getHullY(i);
				}
				str+= ")";

				if(editzone.getEngine()) str+= "(EN: "+editzone.getEngineX()+","+editzone.getEngineY()+")";
				if(editzone.getMainGun()) str+= "(MG: "+editzone.getMainGunX()+","+editzone.getMainGunY()+")";

				if(editzone.getLeftGunPoints() != 0)
				{
					str+= "(LG:";
					for(int i=0;i<editzone.getLeftGunPoints();i++)
					{
						str+= " "+editzone.getLeftGunX(i)+","+editzone.getLeftGunY(i);
					}
					str+= ")";
				}

				if(editzone.getRightGunPoints() != 0)
				{
					str+= "(RG:";
					for(int i=0;i<editzone.getRightGunPoints();i++)
					{
						str+= " "+editzone.getRightGunX(i)+","+editzone.getRightGunY(i);
					}
					str+= ")";
				}

				if(editzone.getLeftRearGunPoints() != 0)
				{
					str+= "(LR:";
					for(int i=0;i<editzone.getLeftRearGunPoints();i++)
					{
						str+= " "+editzone.getLeftRearGunX(i)+","+editzone.getLeftRearGunY(i);
					}
					str+= ")";
				}

				if(editzone.getRightRearGunPoints() != 0)
				{
					str+= "(RR:";
					for(int i=0;i<editzone.getRightRearGunPoints();i++)
					{
						str+= " "+editzone.getRightRearGunX(i)+","+editzone.getRightRearGunY(i);
					}
					str+= ")";
				}

				if(editzone.getLeftLightPoints() != 0)
				{
					str+= "(LL:";
					for(int i=0;i<editzone.getLeftLightPoints();i++)
					{
						str+= " "+editzone.getLeftLightX(i)+","+editzone.getLeftLightY(i);
					}
					str+= ")";
				}

				if(editzone.getRightLightPoints() != 0)
				{
					str+= "(RL:";
					for(int i=0;i<editzone.getRightLightPoints();i++)
					{
						str+= " "+editzone.getRightLightX(i)+","+editzone.getRightLightY(i);
					}
					str+= ")";
				}

				if(editzone.getMissilePoints() != 0)
				{
					str+= "(MR:";
					for(int i=0;i<editzone.getMissilePoints();i++)
					{
						str+= " "+editzone.getMissileX(i)+","+editzone.getMissileY(i);
					}
					str+= ")";
				}



				if(editzone.getHullPoints() == 0) savestring.setText("Error, no hull");
				else
				{
					savestring.setText(str);
					savestring.selectAll();
				}
			}

			if(num == 5)
			{
				boolean[] error = {false,false,false,false,false,false};

				for(int i=0;i<33;i++)
				{
					for(int j=0;j<33;j++)
					{
						ship[i][j] = 0;
					}
				}

				String debug="";

				for(int i=0;i<editzone.getHullPoints();i++)
				{
					int j=i+1;
					if (j == editzone.getHullPoints()) j = 0;

					ship[editzone.getHullX(i)+16][editzone.getHullY(i)+16] = 1;

					int dx = editzone.getHullX(j) - editzone.getHullX(i);
					int dy = editzone.getHullY(j) - editzone.getHullY(i);
					if(Math.abs(dx) >= Math.abs(dy))
					{
						if(dx>0)
						{
							for(int xje = editzone.getHullX(i) + 1; xje<editzone.getHullX(j);xje++)
							{
								int yje=editzone.getHullY(i)+(dy*(xje-editzone.getHullX(i)))/dx;
								ship[xje+16][yje+16] = 1;
							}
						}
						else
						{
							for(int xje=editzone.getHullX(j)+1;xje<editzone.getHullX(i);xje++)
							{
								int yje=editzone.getHullY(j)+(dy*(xje-editzone.getHullX(j))) / dx;
								ship[xje+16][yje+16] = 1;
							}
						}
					}
					else
					{
						if(dy>0)
						{
							for(int yje = editzone.getHullY(i) + 1; yje<editzone.getHullY(j);yje++)
							{
								int xje=editzone.getHullX(i)+(dx*(yje-editzone.getHullY(i)))/dy;
								ship[xje+16][yje+16] = 1;
							}
						}
						else
						{
							for(int yje=editzone.getHullY(j)+1;yje<editzone.getHullY(i);yje++)
							{
								int xje=editzone.getHullX(j)+(dx*(yje-editzone.getHullY(j))) / dy;
								ship[xje+16][yje+16] = 1;
							}
						}
					}
				}

				fill(0,0);

				for(int i=0;i<editzone.getHullPoints();i++)
				{
					int j=i+1;
					if (j == editzone.getHullPoints()) j = 0;

					ship[editzone.getHullX(i)+16][editzone.getHullY(i)+16] = 0;

					int dx = editzone.getHullX(j) - editzone.getHullX(i);
					int dy = editzone.getHullY(j) - editzone.getHullY(i);
					if(Math.abs(dx) >= Math.abs(dy))
					{
						if(dx>0)
						{
							for(int xje = editzone.getHullX(i) + 1; xje<editzone.getHullX(j);xje++)
							{
								int yje=editzone.getHullY(i)+(dy*(xje-editzone.getHullX(i)))/dx;
								ship[xje+16][yje+16] = 0;
							}
						}
						else
						{
							for(int xje=editzone.getHullX(j)+1;xje<editzone.getHullX(i);xje++)
							{
								int yje=editzone.getHullY(j)+(dy*(xje-editzone.getHullX(j))) / dx;
								ship[xje+16][yje+16] = 0;
							}
						}
					}
					else
					{
						if(dy>0)
						{
							for(int yje = editzone.getHullY(i) + 1; yje<editzone.getHullY(j);yje++)
							{
								int xje=editzone.getHullX(i)+(dx*(yje-editzone.getHullY(i)))/dy;
								ship[xje+16][yje+16] = 0;
							}
						}
						else
						{
							for(int yje=editzone.getHullY(j)+1;yje<editzone.getHullY(i);yje++)
							{
								int xje=editzone.getHullX(j)+(dx*(yje-editzone.getHullY(j))) / dy;
								ship[xje+16][yje+16] = 0;
							}
						}
					}
				}

				int min_x=0;
				int max_x=0;
				int min_y=0;
				int max_y=0;

				for(int i=0;i<editzone.getHullPoints();i++)
				{
					min_x = Math.min(min_x, editzone.getHullX(i));
					max_x = Math.max(max_x, editzone.getHullX(i));
					min_y = Math.min(min_y, editzone.getHullY(i));
					max_y = Math.max(max_y, editzone.getHullY(i));
				}

				if(min_x<-7) error[0] = true;
				if(max_x>7) error[1] = true;
				if(min_y<-7) error[3] = true;
				if(max_y>7) error[2] = true;
				if(max_x-min_x+max_y-min_y>37) error[4] = true;

				if(editzone.getMainGun())
				{
					if(ship[16+editzone.getMainGunX()][16+editzone.getMainGunY()] == 1)
					{
						error[5] = true;
					}
				}

				for(int i=0;i<editzone.getLeftGunPoints();i++)
				{
					if(ship[16+editzone.getLeftGunX(i)][16+editzone.getLeftGunY(i)] == 1)
					{
						error[5] = true;
					}
				}

				for(int i=0;i<editzone.getRightGunPoints();i++)
				{
					if(ship[16+editzone.getRightGunX(i)][16+editzone.getRightGunY(i)] == 1)
					{
						error[5] = true;
					}
				}

				for(int i=0;i<editzone.getLeftRearGunPoints();i++)
				{
					if(ship[16+editzone.getLeftRearGunX(i)][16+editzone.getLeftRearGunY(i)] == 1)
					{
						error[5] = true;
					}
				}

				for(int i=0;i<editzone.getRightRearGunPoints();i++)
				{
					if(ship[16+editzone.getRightRearGunX(i)][16+editzone.getRightRearGunY(i)] == 1)
					{
						error[5] = true;
					}
				}

				for(int i=0;i<editzone.getLeftLightPoints();i++)
				{
					if(ship[16+editzone.getLeftLightX(i)][16+editzone.getLeftLightY(i)] == 1)
					{
						error[5] = true;
					}
				}

				for(int i=0;i<editzone.getRightLightPoints();i++)
				{
					if(ship[16+editzone.getRightLightX(i)][16+editzone.getRightLightY(i)] == 1)
					{
						error[5] = true;
					}
				}

				for(int i=0;i<editzone.getMissilePoints();i++)
				{
					if(ship[16+editzone.getMissileX(i)][16+editzone.getMissileY(i)] == 1)
					{
						error[5] = true;
					}
				}

				if(editzone.getEngine())
				{
					if(ship[16+editzone.getEngineX()][16+editzone.getEngineY()] == 1)
					{
						error[5] = true;
					}
				}

				if(!editzone.getMainGun() && editzone.getLeftGunPoints() == 0 && editzone.getRightGunPoints() == 0 && editzone.getLeftRearGunPoints() == 0 && editzone.getRightRearGunPoints() == 0 && editzone.getLeftLightPoints() == 0 && editzone.getRightLightPoints() == 0 && editzone.getMissilePoints() == 0 && !editzone.getEngine())
				{
					error[5] = false;
				}

				checkframe.setErrors(error);
				checkframe.show();
			}
		}

		if(str.equals(tempStr))
		{
			if(str.equals("down"))
			{
				for(int i=0;i<editzone.getHullPoints();i++)
				{
					if(editzone.getHullY(i)>-15)
					{
						editzone.setHull(editzone.getHullX(i), editzone.getHullY(i)-1, i);
					}
				}

				if(editzone.getMainGun())
				{
					if(editzone.getMainGunY()>-15)
					{
						editzone.setMainGun(editzone.getMainGunX(), editzone.getMainGunY()-1);
					}
				}

				for(int i=0;i<editzone.getLeftGunPoints();i++)
				{
					if(editzone.getLeftGunY(i)>-15)
					{
						editzone.setLeftGun(editzone.getLeftGunX(i), editzone.getLeftGunY(i)-1, i);
					}
				}

				for(int i=0;i<editzone.getRightGunPoints();i++)
				{
					if(editzone.getRightGunY(i)>-15)
					{
						editzone.setRightGun(editzone.getRightGunX(i), editzone.getRightGunY(i)-1, i);
					}
				}

				for(int i=0;i<editzone.getLeftRearGunPoints();i++)
				{
					if(editzone.getLeftRearGunY(i)>-15)
					{
						editzone.setLeftRearGun(editzone.getLeftRearGunX(i), editzone.getLeftRearGunY(i)-1, i);
					}
				}

				for(int i=0;i<editzone.getRightRearGunPoints();i++)
				{
					if(editzone.getRightRearGunY(i)>-15)
					{
						editzone.setRightRearGun(editzone.getRightRearGunX(i), editzone.getRightRearGunY(i)-1, i);
					}
				}

				for(int i=0;i<editzone.getLeftLightPoints();i++)
				{
					if(editzone.getLeftLightY(i)>-15)
					{
						editzone.setLeftLight(editzone.getLeftLightX(i), editzone.getLeftLightY(i)-1, i);
					}
				}

				for(int i=0;i<editzone.getRightLightPoints();i++)
				{
					if(editzone.getRightLightY(i)>-15)
					{
						editzone.setRightLight(editzone.getRightLightX(i), editzone.getRightLightY(i)-1, i);
					}
				}

				for(int i=0;i<editzone.getMissilePoints();i++)
				{
					if(editzone.getMissileY(i)>-15)
					{
						editzone.setMissile(editzone.getMissileX(i), editzone.getMissileY(i)-1, i);
					}
				}

				if(editzone.getEngine())
				{
					if(editzone.getEngineY()>-15)
					{
						editzone.setEngine(editzone.getEngineX(), editzone.getEngineY()-1);
					}
				}

				editzone.teken();
			}

			if(str.equals("up"))
			{
				for(int i=0;i<editzone.getHullPoints();i++)
				{
					if(editzone.getHullY(i)<15)
					{
						editzone.setHull(editzone.getHullX(i), editzone.getHullY(i)+1, i);
					}
				}

				if(editzone.getMainGun())
				{
					if(editzone.getMainGunY()<15)
					{
						editzone.setMainGun(editzone.getMainGunX(), editzone.getMainGunY()+1);
					}
				}

				for(int i=0;i<editzone.getLeftGunPoints();i++)
				{
					if(editzone.getLeftGunY(i)<15)
					{
						editzone.setLeftGun(editzone.getLeftGunX(i), editzone.getLeftGunY(i)+1, i);
					}
				}

				for(int i=0;i<editzone.getRightGunPoints();i++)
				{
					if(editzone.getRightGunY(i)<15)
					{
						editzone.setRightGun(editzone.getRightGunX(i), editzone.getRightGunY(i)+1, i);
					}
				}

				for(int i=0;i<editzone.getLeftRearGunPoints();i++)
				{
					if(editzone.getLeftRearGunY(i)<15)
					{
						editzone.setLeftRearGun(editzone.getLeftRearGunX(i), editzone.getLeftRearGunY(i)+1, i);
					}
				}

				for(int i=0;i<editzone.getRightRearGunPoints();i++)
				{
					if(editzone.getRightRearGunY(i)<15)
					{
						editzone.setRightRearGun(editzone.getRightRearGunX(i), editzone.getRightRearGunY(i)+1, i);
					}
				}

				for(int i=0;i<editzone.getLeftLightPoints();i++)
				{
					if(editzone.getLeftLightY(i)<15)
					{
						editzone.setLeftLight(editzone.getLeftLightX(i), editzone.getLeftLightY(i)+1, i);
					}
				}

				for(int i=0;i<editzone.getRightLightPoints();i++)
				{
					if(editzone.getRightLightY(i)<15)
					{
						editzone.setRightLight(editzone.getRightLightX(i), editzone.getRightLightY(i)+1, i);
					}
				}

				for(int i=0;i<editzone.getMissilePoints();i++)
				{
					if(editzone.getMissileY(i)<15)
					{
						editzone.setMissile(editzone.getMissileX(i), editzone.getMissileY(i)+1, i);
					}
				}

				if(editzone.getEngine())
				{
					if(editzone.getEngineY()<15)
					{
						editzone.setEngine(editzone.getEngineX(), editzone.getEngineY()+1);
					}
				}

				editzone.teken();
			}

			if(str.equals("right"))
			{
				for(int i=0;i<editzone.getHullPoints();i++)
				{
					if(editzone.getHullX(i)<15)
					{
						editzone.setHull(editzone.getHullX(i)+1, editzone.getHullY(i), i);
					}
				}

				if(editzone.getMainGun())
				{
					if(editzone.getMainGunX()<15)
					{
						editzone.setMainGun(editzone.getMainGunX()+1, editzone.getMainGunY());
					}
				}

				for(int i=0;i<editzone.getLeftGunPoints();i++)
				{
					if(editzone.getLeftGunX(i)<15)
					{
						editzone.setLeftGun(editzone.getLeftGunX(i)+1, editzone.getLeftGunY(i), i);
					}
				}

				for(int i=0;i<editzone.getRightGunPoints();i++)
				{
					if(editzone.getRightGunX(i)<15)
					{
						editzone.setRightGun(editzone.getRightGunX(i)+1, editzone.getRightGunY(i), i);
					}
				}

				for(int i=0;i<editzone.getLeftRearGunPoints();i++)
				{
					if(editzone.getLeftRearGunX(i)<15)
					{
						editzone.setLeftRearGun(editzone.getLeftRearGunX(i)+1, editzone.getLeftRearGunY(i), i);
					}
				}

				for(int i=0;i<editzone.getRightRearGunPoints();i++)
				{
					if(editzone.getRightRearGunX(i)<15)
					{
						editzone.setRightRearGun(editzone.getRightRearGunX(i)+1, editzone.getRightRearGunY(i), i);
					}
				}

				for(int i=0;i<editzone.getLeftLightPoints();i++)
				{
					if(editzone.getLeftLightX(i)<15)
					{
						editzone.setLeftLight(editzone.getLeftLightX(i)+1, editzone.getLeftLightY(i), i);
					}
				}

				for(int i=0;i<editzone.getRightLightPoints();i++)
				{
					if(editzone.getRightLightX(i)<15)
					{
						editzone.setRightLight(editzone.getRightLightX(i)+1, editzone.getRightLightY(i), i);
					}
				}

				for(int i=0;i<editzone.getMissilePoints();i++)
				{
					if(editzone.getMissileX(i)<15)
					{
						editzone.setMissile(editzone.getMissileX(i)+1, editzone.getMissileY(i), i);
					}
				}

				if(editzone.getEngine())
				{
					if(editzone.getEngineX()<15)
					{
						editzone.setEngine(editzone.getEngineX()+1, editzone.getEngineY());
					}
				}

				editzone.teken();
			}

			if(str.equals("left"))
			{
				for(int i=0;i<editzone.getHullPoints();i++)
				{
					if(editzone.getHullX(i)>-15)
					{
						editzone.setHull(editzone.getHullX(i)-1, editzone.getHullY(i), i);
					}
				}

				if(editzone.getMainGun())
				{
					if(editzone.getMainGunX()>-15)
					{
						editzone.setMainGun(editzone.getMainGunX()-1, editzone.getMainGunY());
					}
				}

				for(int i=0;i<editzone.getLeftGunPoints();i++)
				{
					if(editzone.getLeftGunX(i)>-15)
					{
						editzone.setLeftGun(editzone.getLeftGunX(i)-1, editzone.getLeftGunY(i), i);
					}
				}

				for(int i=0;i<editzone.getRightGunPoints();i++)
				{
					if(editzone.getRightGunX(i)>-15)
					{
						editzone.setRightGun(editzone.getRightGunX(i)-1, editzone.getRightGunY(i), i);
					}
				}

				for(int i=0;i<editzone.getLeftRearGunPoints();i++)
				{
					if(editzone.getLeftRearGunX(i)>-15)
					{
						editzone.setLeftRearGun(editzone.getLeftRearGunX(i)-1, editzone.getLeftRearGunY(i), i);
					}
				}

				for(int i=0;i<editzone.getRightRearGunPoints();i++)
				{
					if(editzone.getRightRearGunX(i)>-15)
					{
						editzone.setRightRearGun(editzone.getRightRearGunX(i)-1, editzone.getRightRearGunY(i), i);
					}
				}

				for(int i=0;i<editzone.getLeftLightPoints();i++)
				{
					if(editzone.getLeftLightX(i)>-15)
					{
						editzone.setLeftLight(editzone.getLeftLightX(i)-1, editzone.getLeftLightY(i), i);
					}
				}

				for(int i=0;i<editzone.getRightLightPoints();i++)
				{
					if(editzone.getRightLightX(i)>-15)
					{
						editzone.setRightLight(editzone.getRightLightX(i)-1, editzone.getRightLightY(i), i);
					}
				}

				for(int i=0;i<editzone.getMissilePoints();i++)
				{
					if(editzone.getMissileX(i)>-15)
					{
						editzone.setMissile(editzone.getMissileX(i)-1, editzone.getMissileY(i), i);
					}
				}

				if(editzone.getEngine())
				{
					if(editzone.getEngineX()>-15)
					{
						editzone.setEngine(editzone.getEngineX()-1, editzone.getEngineY());
					}
				}

				editzone.teken();
			}

			if(str.equals("center"))
			{
				modifier=-modifier;
			}
		}

		return true;
	}

	public void fill(int i, int j)
	{
		ship[i][j] = 1;

		if(i>0)
		{
			if(ship[i-1][j] == 0) fill(i-1,j);
		}

		if(i<32)
		{
			if(ship[i+1][j] == 0) fill(i+1,j);
		}

		if(j>0)
		{
			if(ship[i][j-1] == 0) fill(i,j-1);
		}

		if(j<32)
		{
			if(ship[i][j+1] == 0) fill(i,j+1);
		}
	}
}









/*

NEXT CLASS STARTS HERE

*/













class DraaiSchip extends Canvas
{
	int coordNum=0;
	int leftNum=0;
	int rightNum=0;
	int totaal;
	int totleft;
	int totright;
	int[][] coord = new int[24][2];
	int[][] left = new int[3][2];
	int[][] right = new int[3][2];
	double angle=0;
	int keeptrack=0;

	public void setPoint(int coordX, int coordY)
	{
		coord[coordNum][0] = coordX;
		coord[coordNum][1] = coordY;
		coordNum++;
	}

	public void setLPoint(int coordX, int coordY)
	{
		left[leftNum][0] = coordX;
		left[leftNum][1] = coordY;
		leftNum++;
	}

	public void setRPoint(int coordX, int coordY)
	{
		right[rightNum][0] = coordX;
		right[rightNum][1] = coordY;
		rightNum++;
	}

	public void setAngle(double x)
	{
		angle = x;
	}

	public void teken()
	{
		repaint();
		totaal=coordNum;
		coordNum=0;
		totleft=leftNum;
		leftNum=0;
		totright=rightNum;
		rightNum=0;
	}

	public void paint(Graphics g)
	{
		g.setColor(Color.white);

		for(int i=0;i<totaal-1;i++)
		{
			g.drawLine(23+coord[i][0],23-coord[i][1],23+coord[i+1][0],23-coord[i+1][1]);
		}

		if(totaal > 2 || totaal==1) g.drawLine(23+coord[0][0],23-coord[0][1],23+coord[totaal-1][0],23-coord[totaal-1][1]);

		if(keeptrack%15 == 0 || keeptrack%15 == 1 || keeptrack%15 == 2)
		{
			g.setColor(Color.green);
			for(int i=0;i<totleft;i++)
			{
				g.fillOval(21+left[i][0], 21-left[i][1],5,5);
			}

			g.setColor(Color.red);
			for(int i=0;i<totright;i++)
			{
				g.fillOval(21+right[i][0], 21-right[i][1],5,5);
			}
		}

		g.setColor(Color.red);
		g.drawOval(23+(int)(21*Math.cos(angle)),23+(int)(21*Math.sin(angle)),2,2);

		keeptrack++;
	}
}












/*

NEXT CLASS STARTS HERE

*/












class TekenBord extends Canvas
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

	int HullPoints=0;
	boolean MainGun=false;
	int LeftGunPoints = 0;
	int RightGunPoints = 0;
	int LeftLightPoints = 0;
	int RightLightPoints = 0;
	int LeftRearGunPoints = 0;
	int RightRearGunPoints = 0;
	int MissilePoints = 0;
	boolean Engine = false;

	boolean change = true;
	String tool = new String();
	String item = new String();
	int blokjex_oud=-90;
	int blokjey_oud=-90;
	int blokjex_ouder=-90;
	int blokjey_ouder=-90;
	int dragging = -1;
	boolean firsttime;
	int selected = -1;

	public void paint(Graphics g)
	{
		g.setColor(Color.yellow);
		g.drawRect(1,1,314,314);
		g.setColor(Color.white);
		g.drawRect(3,3,310,310);

		g.setColor(Color.gray);
		for(int i=0;i<30;i++)
		{
			g.drawLine(13+10*i,4,13+10*i,312);
			g.drawLine(4,13+10*i,312,13+10*i);
		}

		g.setColor(Color.lightGray);
		g.drawLine(153,4,153,312);
		g.drawLine(163,4,163,312);
		g.drawLine(4,153,312,153);
		g.drawLine(4,163,312,163);

		g.setColor(Color.orange);
		g.drawRect(73,73,170,170);

		for(int i=0;i<HullPoints;i++)
		{
			g.setColor(Color.blue);
			g.fillRect(154+10*hull[i][0],154-10*hull[i][1],9,9);
		}

		for(int i=0;i<HullPoints-1;i++)
		{
			g.drawLine(158+10*hull[i][0],158-10*hull[i][1],158+10*hull[i+1][0],158-10*hull[i+1][1]);
		}

		if(HullPoints>2)
		{
			g.setColor(Color.cyan);
			g.drawLine(158+10*hull[0][0],158-10*hull[0][1],158+10*hull[HullPoints-1][0],158-10*hull[HullPoints-1][1]);
		}

		if(MainGun)
		{
			g.setColor(Color.red);

			int[] tempX = {154+10*maingun[0],154+10*maingun[0], 163+10*maingun[0]};
			int[] tempY = {153-10*maingun[1],163-10*maingun[1], 158-10*maingun[1]};

			g.fillPolygon(tempX,tempY,3);
		}

		for(int i=0;i<LeftGunPoints;i++)
		{
			g.setColor(Color.red);

			int[] tempX = {154+10*leftgun[i][0],154+10*leftgun[i][0], 163+10*leftgun[i][0]};
			int[] tempY = {154-10*leftgun[i][1],163-10*leftgun[i][1], 154-10*leftgun[i][1]};

			g.fillPolygon(tempX,tempY,3);
		}

		for(int i=0;i<RightGunPoints;i++)
		{
			g.setColor(Color.red);

			int[] tempX = {154+10*rightgun[i][0],154+10*rightgun[i][0], 163+10*rightgun[i][0]};
			int[] tempY = {153-10*rightgun[i][1],163-10*rightgun[i][1], 163-10*rightgun[i][1]};

			g.fillPolygon(tempX,tempY,3);
		}

		for(int i=0;i<LeftRearGunPoints;i++)
		{
			g.setColor(Color.red);

			int[] tempX = {163+10*leftreargun[i][0],163+10*leftreargun[i][0], 154+10*leftreargun[i][0]};
			int[] tempY = {154-10*leftreargun[i][1],163-10*leftreargun[i][1], 154-10*leftreargun[i][1]};

			g.fillPolygon(tempX,tempY,3);
		}

		for(int i=0;i<RightRearGunPoints;i++)
		{
			g.setColor(Color.red);

			int[] tempX = {163+10*rightreargun[i][0],163+10*rightreargun[i][0], 154+10*rightreargun[i][0]};
			int[] tempY = {153-10*rightreargun[i][1],163-10*rightreargun[i][1], 163-10*rightreargun[i][1]};

			g.fillPolygon(tempX,tempY,3);
		}

		for(int i=0;i<MissilePoints;i++)
		{
			g.setColor(Color.pink);

			int[] tempX = {154+10*missile[i][0],154+10*missile[i][0], 163+10*missile[i][0]};
			int[] tempY = {155-10*missile[i][1],160-10*missile[i][1], 158-10*missile[i][1]};

			g.fillPolygon(tempX,tempY,3);
		}

		g.setColor(Color.orange);
		for(int i=0;i<LeftLightPoints;i++)
		{
			g.fillOval(154+10*leftlight[i][0],154-10*leftlight[i][1],8,8);
		}

		g.setColor(Color.green);
		for(int i=0;i<RightLightPoints;i++)
		{
			g.fillOval(154+10*rightlight[i][0],154-10*rightlight[i][1],8,8);
		}

		if(Engine)
		{
			g.setColor(Color.orange);

			g.drawRect(154+10*engine[0],154-10*engine[1],8,8);
			g.drawRect(155+10*engine[0],155-10*engine[1],6,6);
		}

		g.setColor(Color.white);

		if(HullPoints!=23 && item.equals("Hull")) g.drawString(24-HullPoints+" hullpoints left",110,329);
		if(HullPoints==23 && item.equals("Hull")) g.drawString(24-HullPoints+" hullpoints left",110,329);
		if(MainGun && item.equals("Main gun")) g.drawString("0 main guns left",110,329);
		if(!MainGun && item.equals("Main gun")) g.drawString("1 main guns left",110,329);
		if(LeftGunPoints!=2 && item.equals("Left gun")) g.drawString(3-LeftGunPoints+" left guns left",110,329);
		if(LeftGunPoints==2 && item.equals("Left gun")) g.drawString(3-LeftGunPoints+" left gun left",110,329);
		if(RightGunPoints!=2 && item.equals("Right gun")) g.drawString(3-RightGunPoints+" right guns left",110,329);
		if(RightGunPoints==2 && item.equals("Right gun")) g.drawString(3-RightGunPoints+" right gun left",110,329);
		if(LeftRearGunPoints!=2 && item.equals("Left rear gun")) g.drawString(3-LeftRearGunPoints+" left rear guns left",110,329);
		if(LeftRearGunPoints==2 && item.equals("Left rear gun")) g.drawString(3-LeftRearGunPoints+" left rear gun left",110,329);
		if(RightRearGunPoints!=2 && item.equals("Right rear gun")) g.drawString(3-RightRearGunPoints+" right rear guns left",110,329);
		if(RightRearGunPoints==2 && item.equals("Right rear gun")) g.drawString(3-RightRearGunPoints+" right rear gun left",110,329);
		if(LeftLightPoints!=2 && item.equals("Left light")) g.drawString(3-LeftLightPoints+" left lights left",110,329);
		if(LeftLightPoints==2 && item.equals("Left light")) g.drawString(3-LeftLightPoints+" left light left",110,329);
		if(RightLightPoints!=2 && item.equals("Right light")) g.drawString(3-RightLightPoints+" right lights left",110,329);
		if(RightLightPoints==2 && item.equals("Right light")) g.drawString(3-RightLightPoints+" right light left",110,329);
		if(MissilePoints!=3 && item.equals("Missile tube")) g.drawString(4-MissilePoints+" missile tubes left",110,329);
		if(MissilePoints==3 && item.equals("Missile tube")) g.drawString(4-MissilePoints+" missile tube left",110,329);
		if(Engine && item.equals("Engine")) g.drawString("0 engines left",110,329);
		if(!Engine && item.equals("Engine")) g.drawString("1 engine left",110,329);

		if(selected >= 0)
		{
			g.setColor(Color.red.brighter());
			g.drawOval(148+10*hull[selected][0],148-10*hull[selected][1],20,20);
			g.setColor(Color.red.darker());
			if(selected>0) g.drawOval(148+10*hull[selected-1][0],148-10*hull[selected-1][1],20,20);
			if(selected<HullPoints-1) g.drawOval(148+10*hull[selected+1][0],148-10*hull[selected+1][1],20,20);
		}
	}










	public boolean handleEvent(Event e)
	{
		switch (e.id)
		{
			case Event.MOUSE_DOWN:
				this.mouseWentDown(e.x, e.y);
				break;
			case Event.MOUSE_DRAG:
				this.mouseWentDrag(e.x, e.y);
				break;
			default:
				break;
		}
		return true;
	}




/*
*
*
*
*
*
*
*
*/







	public void mouseWentDown(int x, int y)
	{
		if(x>3 && y>3 && x<313 && y<313)
		{
			int blokjex;
			int blokjey;

			blokjex = (int)((x-153)/10);
			blokjey = (int)((y-153)/10);

			if(x<153) blokjex--;
			if(y<153) blokjey--;

			if(tool.equals("Add"))
			{
				if(HullPoints<24 && item.equals("Hull"))
				{
					if(HullPoints>0)
					{
						if(!(hull[HullPoints-1][0] == blokjex && hull[HullPoints-1][1] == -blokjey))
						{
							hull[HullPoints][0] = blokjex;
							hull[HullPoints][1] = -blokjey;

							HullPoints++;

							change = true;
							repaint();
						}
					}
					else
					{
						hull[HullPoints][0] = blokjex;
						hull[HullPoints][1] = -blokjey;

						HullPoints++;

						change = true;
						repaint();
					}
				}

				if(item.equals("Main gun") && !MainGun)
				{
					maingun[0] = blokjex;
					maingun[1] = -blokjey;

					MainGun = true;

					repaint();
				}

				if(LeftGunPoints<3 && item.equals("Left gun"))
				{
					if(LeftGunPoints==0)
					{
						leftgun[LeftGunPoints][0] = blokjex;
						leftgun[LeftGunPoints][1] = -blokjey;

						LeftGunPoints++;

						repaint();
					}
					if(LeftGunPoints==1)
					{
						if(!(leftgun[LeftGunPoints-1][0] == blokjex && leftgun[LeftGunPoints-1][1] == -blokjey))
						{
							leftgun[LeftGunPoints][0] = blokjex;
							leftgun[LeftGunPoints][1] = -blokjey;

							LeftGunPoints++;

							repaint();
						}
					}
					if(LeftGunPoints==2)
					{
						if(!(leftgun[LeftGunPoints-1][0] == blokjex && leftgun[LeftGunPoints-1][1] == -blokjey) && !(leftgun[LeftGunPoints-2][0] == blokjex && leftgun[LeftGunPoints-2][1] == -blokjey))
						{
							leftgun[LeftGunPoints][0] = blokjex;
							leftgun[LeftGunPoints][1] = -blokjey;

							LeftGunPoints++;

							repaint();
						}
					}
				}

				if(RightGunPoints<3 && item.equals("Right gun"))
				{
					if(RightGunPoints==0)
					{
						rightgun[RightGunPoints][0] = blokjex;
						rightgun[RightGunPoints][1] = -blokjey;

						RightGunPoints++;

						repaint();
					}
					if(RightGunPoints==1)
					{
						if(!(rightgun[RightGunPoints-1][0] == blokjex && rightgun[RightGunPoints-1][1] == -blokjey))
						{
							rightgun[RightGunPoints][0] = blokjex;
							rightgun[RightGunPoints][1] = -blokjey;

							RightGunPoints++;

							repaint();
						}
					}
					if(RightGunPoints==2)
					{
						if(!(rightgun[RightGunPoints-1][0] == blokjex && rightgun[RightGunPoints-1][1] == -blokjey) && !(rightgun[RightGunPoints-2][0] == blokjex && rightgun[RightGunPoints-2][1] == -blokjey))
						{
							rightgun[RightGunPoints][0] = blokjex;
							rightgun[RightGunPoints][1] = -blokjey;

							RightGunPoints++;

							repaint();
						}
					}
				}

				if(LeftRearGunPoints<3 && item.equals("Left rear gun"))
				{
					if(LeftRearGunPoints==0)
					{
						leftreargun[LeftRearGunPoints][0] = blokjex;
						leftreargun[LeftRearGunPoints][1] = -blokjey;

						LeftRearGunPoints++;

						repaint();
					}
					if(LeftRearGunPoints==1)
					{
						if(!(leftreargun[LeftRearGunPoints-1][0] == blokjex && leftreargun[LeftRearGunPoints-1][1] == -blokjey))
						{
							leftreargun[LeftRearGunPoints][0] = blokjex;
							leftreargun[LeftRearGunPoints][1] = -blokjey;

							LeftRearGunPoints++;

							repaint();
						}
					}
					if(LeftRearGunPoints==2)
					{
						if(!(leftreargun[LeftRearGunPoints-1][0] == blokjex && leftreargun[LeftRearGunPoints-1][1] == -blokjey) && !(leftreargun[LeftRearGunPoints-2][0] == blokjex && leftreargun[LeftRearGunPoints-2][1] == -blokjey))
						{
							leftreargun[LeftRearGunPoints][0] = blokjex;
							leftreargun[LeftRearGunPoints][1] = -blokjey;

							LeftRearGunPoints++;

							repaint();
						}
					}
				}

				if(RightRearGunPoints<3 && item.equals("Right rear gun"))
				{
					if(RightRearGunPoints==0)
					{
						rightreargun[RightRearGunPoints][0] = blokjex;
						rightreargun[RightRearGunPoints][1] = -blokjey;

						RightRearGunPoints++;

						repaint();
					}
					if(RightRearGunPoints==1)
					{
						if(!(rightreargun[RightRearGunPoints-1][0] == blokjex && rightreargun[RightRearGunPoints-1][1] == -blokjey))
						{
							rightreargun[RightRearGunPoints][0] = blokjex;
							rightreargun[RightRearGunPoints][1] = -blokjey;

							RightRearGunPoints++;

							repaint();
						}
					}
					if(RightRearGunPoints==2)
					{
						if(!(rightreargun[RightRearGunPoints-1][0] == blokjex && rightreargun[RightRearGunPoints-1][1] == -blokjey) && !(rightreargun[RightRearGunPoints-2][0] == blokjex && rightreargun[RightRearGunPoints-2][1] == -blokjey))
						{
							rightreargun[RightRearGunPoints][0] = blokjex;
							rightreargun[RightRearGunPoints][1] = -blokjey;

							RightRearGunPoints++;

							repaint();
						}
					}
				}

				if(LeftLightPoints<3 && item.equals("Left light"))
				{
					if(LeftLightPoints==0)
					{
						leftlight[LeftLightPoints][0] = blokjex;
						leftlight[LeftLightPoints][1] = -blokjey;

						LeftLightPoints++;

						change = true;
						repaint();
					}
					if(LeftLightPoints==1)
					{
						if(!(leftlight[LeftLightPoints-1][0] == blokjex && leftlight[LeftLightPoints-1][1] == -blokjey))
						{
							leftlight[LeftLightPoints][0] = blokjex;
							leftlight[LeftLightPoints][1] = -blokjey;

							LeftLightPoints++;

							change = true;
							repaint();
						}
					}
					if(LeftLightPoints==2)
					{
						if(!(leftlight[LeftLightPoints-1][0] == blokjex && leftlight[LeftLightPoints-1][1] == -blokjey) && !(leftlight[LeftLightPoints-2][0] == blokjex && leftlight[LeftLightPoints-2][1] == -blokjey))
						{
							leftlight[LeftLightPoints][0] = blokjex;
							leftlight[LeftLightPoints][1] = -blokjey;

							LeftLightPoints++;

							change = true;
							repaint();
						}
					}
				}

				if(RightLightPoints<3 && item.equals("Right light"))
				{
					if(RightLightPoints==0)
					{
						rightlight[RightLightPoints][0] = blokjex;
						rightlight[RightLightPoints][1] = -blokjey;

						RightLightPoints++;

						change = true;
						repaint();
					}
					if(RightLightPoints==1)
					{
						if(!(rightlight[RightLightPoints-1][0] == blokjex && rightlight[RightLightPoints-1][1] == -blokjey))
						{
							rightlight[RightLightPoints][0] = blokjex;
							rightlight[RightLightPoints][1] = -blokjey;

							RightLightPoints++;

							change = true;
							repaint();
						}
					}
					if(RightLightPoints==2)
					{
						if(!(rightlight[RightLightPoints-1][0] == blokjex && rightlight[RightLightPoints-1][1] == -blokjey) && !(rightlight[RightLightPoints-2][0] == blokjex && rightlight[RightLightPoints-2][1] == -blokjey))
						{
							rightlight[RightLightPoints][0] = blokjex;
							rightlight[RightLightPoints][1] = -blokjey;

							RightLightPoints++;

							change = true;
							repaint();
						}
					}
				}

				if(MissilePoints<4 && item.equals("Missile tube"))
				{
					if(MissilePoints==0)
					{
						missile[MissilePoints][0] = blokjex;
						missile[MissilePoints][1] = -blokjey;

						MissilePoints++;

						repaint();
					}
					if(MissilePoints==1)
					{
						if(!(missile[MissilePoints-1][0] == blokjex && missile[MissilePoints-1][1] == -blokjey))
						{
							missile[MissilePoints][0] = blokjex;
							missile[MissilePoints][1] = -blokjey;

							MissilePoints++;

							repaint();
						}
					}
					if(MissilePoints==2)
					{
						if(!(missile[MissilePoints-1][0] == blokjex && missile[MissilePoints-1][1] == -blokjey) && !(missile[MissilePoints-2][0] == blokjex && missile[MissilePoints-2][1] == -blokjey))
						{
							missile[MissilePoints][0] = blokjex;
							missile[MissilePoints][1] = -blokjey;

							MissilePoints++;

							repaint();
						}
					}
					if(MissilePoints==3)
					{
						if(!(missile[MissilePoints-1][0] == blokjex && missile[MissilePoints-1][1] == -blokjey) && !(missile[MissilePoints-2][0] == blokjex && missile[MissilePoints-2][1] == -blokjey) && !(missile[MissilePoints-3][0] == blokjex && missile[MissilePoints-3][1] == -blokjey))
						{
							missile[MissilePoints][0] = blokjex;
							missile[MissilePoints][1] = -blokjey;

							MissilePoints++;

							repaint();
						}
					}
				}

				if(item.equals("Engine") && !Engine)
				{
					engine[0] = blokjex;
					engine[1] = -blokjey;

					Engine = true;

					repaint();
				}
			}


/*
VANAF HIER REMOVE
*/
			if(tool.equals("Remove"))
			{
				if(HullPoints>0 && item.equals("Hull"))
				{
					int subject=-1;

					for(int i=0;i<HullPoints;i++)
					{
						if(hull[i][0] == blokjex && hull[i][1] == -blokjey) subject = i;
					}

					if(subject>-1)
					{
						for(int i=subject;i<HullPoints-1;i++)
						{
							hull[i][0] = hull[i+1][0];
							hull[i][1] = hull[i+1][1];
						}

						HullPoints--;

						change = true;
						repaint();
					}
				}

				if(item.equals("Main gun") && MainGun && maingun[0] == blokjex && maingun[1] == -blokjey)
				{
					MainGun = false;
					repaint();
				}

				if(LeftGunPoints>0 && item.equals("Left gun"))
				{
					int subject=-1;

					for(int i=0;i<LeftGunPoints;i++)
					{
						if(leftgun[i][0] == blokjex && leftgun[i][1] == -blokjey) subject = i;
					}

					if(subject>-1)
					{
						for(int i=subject;i<LeftGunPoints-1;i++)
						{
							leftgun[i][0] = leftgun[i+1][0];
							leftgun[i][1] = leftgun[i+1][1];
						}

						LeftGunPoints--;

						repaint();
					}
				}

				if(RightGunPoints>0 && item.equals("Right gun"))
				{
					int subject=-1;

					for(int i=0;i<RightGunPoints;i++)
					{
						if(rightgun[i][0] == blokjex && rightgun[i][1] == -blokjey) subject = i;
					}

					if(subject>-1)
					{
						for(int i=subject;i<RightGunPoints-1;i++)
						{
							rightgun[i][0] = rightgun[i+1][0];
							rightgun[i][1] = rightgun[i+1][1];
						}

						RightGunPoints--;

						repaint();
					}
				}

				if(LeftRearGunPoints>0 && item.equals("Left rear gun"))
				{
					int subject=-1;

					for(int i=0;i<LeftRearGunPoints;i++)
					{
						if(leftreargun[i][0] == blokjex && leftreargun[i][1] == -blokjey) subject = i;
					}

					if(subject>-1)
					{
						for(int i=subject;i<LeftRearGunPoints-1;i++)
						{
							leftreargun[i][0] = leftreargun[i+1][0];
							leftreargun[i][1] = leftreargun[i+1][1];
						}

						LeftRearGunPoints--;

						repaint();
					}
				}

				if(RightRearGunPoints>0 && item.equals("Right rear gun"))
				{
					int subject=-1;

					for(int i=0;i<RightRearGunPoints;i++)
					{
						if(rightreargun[i][0] == blokjex && rightreargun[i][1] == -blokjey) subject = i;
					}

					if(subject>-1)
					{
						for(int i=subject;i<RightRearGunPoints-1;i++)
						{
							rightreargun[i][0] = rightreargun[i+1][0];
							rightreargun[i][1] = rightreargun[i+1][1];
						}

						RightRearGunPoints--;

						repaint();
					}
				}

				if(LeftLightPoints>0 && item.equals("Left light"))
				{
					int subject=-1;

					for(int i=0;i<LeftLightPoints;i++)
					{
						if(leftlight[i][0] == blokjex && leftlight[i][1] == -blokjey) subject = i;
					}

					if(subject>-1)
					{
						for(int i=subject;i<LeftLightPoints-1;i++)
						{
							leftlight[i][0] = leftlight[i+1][0];
							leftlight[i][1] = leftlight[i+1][1];
						}

						LeftLightPoints--;

						change = true;
						repaint();
					}
				}

				if(RightLightPoints>0 && item.equals("Right light"))
				{
					int subject=-1;

					for(int i=0;i<RightLightPoints;i++)
					{
						if(rightlight[i][0] == blokjex && rightlight[i][1] == -blokjey) subject = i;
					}

					if(subject>-1)
					{
						for(int i=subject;i<RightLightPoints-1;i++)
						{
							rightlight[i][0] = rightlight[i+1][0];
							rightlight[i][1] = rightlight[i+1][1];
						}

						RightLightPoints--;

						change = true;
						repaint();
					}
				}

				if(MissilePoints>0 && item.equals("Missile tube"))
				{
					int subject=-1;

					for(int i=0;i<MissilePoints;i++)
					{
						if(missile[i][0] == blokjex && missile[i][1] == -blokjey) subject = i;
					}

					if(subject>-1)
					{
						for(int i=subject;i<MissilePoints-1;i++)
						{
							missile[i][0] = missile[i+1][0];
							missile[i][1] = missile[i+1][1];
						}

						MissilePoints--;

						repaint();
					}
				}

				if(item.equals("Engine") && Engine && engine[0] == blokjex && engine[1] == -blokjey)
				{
					Engine = false;
					repaint();
				}
			}




/*

VANAF HIER MOVE

*/








			if(tool.equals("Move"))
			{
				if(item.equals("Hull"))
				{
					int subject=-1;

					for(int i=0;i<HullPoints;i++)
					{
						if(hull[i][0] == blokjex && hull[i][1] == -blokjey) subject = i;
					}

					dragging = subject;
				}

				if(item.equals("Main gun"))
				{
					if(maingun[0] == blokjex && maingun[1] == -blokjey)
					{
						dragging = 0;
					}
					else
					{
						dragging = -1;
					}
				}

				if(item.equals("Left gun"))
				{
					int subject=-1;

					for(int i=0;i<LeftGunPoints;i++)
					{
						if(leftgun[i][0] == blokjex && leftgun[i][1] == -blokjey) subject = i;
					}

					dragging = subject;
				}

				if(item.equals("Right gun"))
				{
					int subject=-1;

					for(int i=0;i<RightGunPoints;i++)
					{
						if(rightgun[i][0] == blokjex && rightgun[i][1] == -blokjey) subject = i;
					}

					dragging = subject;
				}

				if(item.equals("Left rear gun"))
				{
					int subject=-1;

					for(int i=0;i<LeftRearGunPoints;i++)
					{
						if(leftreargun[i][0] == blokjex && leftreargun[i][1] == -blokjey) subject = i;
					}

					dragging = subject;
				}

				if(item.equals("Right rear gun"))
				{
					int subject=-1;

					for(int i=0;i<RightRearGunPoints;i++)
					{
						if(rightreargun[i][0] == blokjex && rightreargun[i][1] == -blokjey) subject = i;
					}

					dragging = subject;
				}

				if(item.equals( "Left light"))
				{
					int subject=-1;

					for(int i=0;i<LeftLightPoints;i++)
					{
						if(leftlight[i][0] == blokjex && leftlight[i][1] == -blokjey) subject = i;
					}

					dragging = subject;
				}

				if(item.equals("Right light"))
				{
					int subject=-1;

					for(int i=0;i<RightLightPoints;i++)
					{
						if(rightlight[i][0] == blokjex && rightlight[i][1] == -blokjey) subject = i;
					}

					dragging = subject;
				}

				if(item.equals("Missile tube"))
				{
					int subject=-1;

					for(int i=0;i<MissilePoints;i++)
					{
						if(missile[i][0] == blokjex && missile[i][1] == -blokjey) subject = i;
					}

					dragging = subject;
				}

				if(item.equals("Engine"))
				{
					if(engine[0] == blokjex && engine[1] == -blokjey)
					{
						dragging = 0;
					}
					else
					{
						dragging = -1;
					}
				}

				firsttime = true;
			}


/*

VANAF HIER INSERT

*/

			boolean just_did_that = false;

			if(tool.equals("Insert") && selected >= 0)
			{
				int subject=-1;

				for(int i=0;i<HullPoints;i++)
				{
					if(hull[i][0] == blokjex && hull[i][1] == -blokjey) subject = i;
				}

				if((subject == selected-1 || subject == selected+1) && subject != -1)
				{
					if(subject < selected) subject = selected;

					for(int i=HullPoints;i>subject;i--)
					{
						hull[i][0] = hull[i-1][0];
						hull[i][1] = hull[i-1][1];
					}

					for(int i=0;i<2;i++)
					{
						hull[subject][i] = (int)((hull[subject][i] + hull[subject-1][i]) / 2);
					}

					HullPoints++;
					selected=-1;
					change=true;
					repaint();
					just_did_that = true;
				}
				else
				{
					selected = -1;
					just_did_that = true;
					change=true;
					repaint();
				}
			}

			if(tool.equals("Insert") && selected < 0 && HullPoints < 24 && !just_did_that)
			{
				int subject=-1;

				for(int i=0;i<HullPoints;i++)
				{
					if(hull[i][0] == blokjex && hull[i][1] == -blokjey) subject = i;
				}

				selected = subject;
				repaint();
			}
		}
	}


























	public void mouseWentDrag(int x, int y)
	{
		int blokjex;
		int blokjey;

		blokjex = (int)((x-153)/10);
		blokjey = (int)((y-153)/10);

		if(x<153) blokjex--;
		if(y<153) blokjey--;

		if(x>3 && y>3 && x<313 && y<313 && (blokjex != blokjex_oud || blokjey != blokjey_oud))
		{
			if(tool.equals("Move"))
			{
				if(item.equals("Hull") && dragging>=0)
				{
					if(dragging>0 && dragging<HullPoints-1)
					{
						if(!(hull[dragging-1][0] == blokjex && hull[dragging-1][1] == -blokjey) && !(hull[dragging+1][0] == blokjex && hull[dragging+1][1] == -blokjey))
						{
							hull[dragging][0] = blokjex;
							hull[dragging][1] = -blokjey;

							change = true;
							repaint();
						}
					}

					if(dragging == 0)
					{
						if(!(hull[dragging+1][0] == blokjex && hull[dragging+1][1] == -blokjey) && !(hull[HullPoints-1][0] == blokjex && hull[HullPoints-1][1] == -blokjey))
						{
							hull[dragging][0] = blokjex;
							hull[dragging][1] = -blokjey;

							change = true;
							repaint();
						}
					}

					if(dragging == HullPoints-1 && HullPoints != 1)
					{
						if(!(hull[dragging-1][0] == blokjex && hull[dragging-1][1] == -blokjey) && !(hull[0][0] == blokjex && hull[0][1] == -blokjey))
						{
							hull[dragging][0] = blokjex;
							hull[dragging][1] = -blokjey;

							change = true;
							repaint();
						}
					}

					if(HullPoints == 1)
					{
						hull[dragging][0] = blokjex;
						hull[dragging][1] = -blokjey;

						change = true;
						repaint();
					}
				}

				if(item.equals("Main gun") && dragging == 0)
				{
					maingun[0] = blokjex;
					maingun[1] = -blokjey;

					repaint(154+10*(blokjex_oud), 154 + 10*(blokjey_oud), 9, 9);
					repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
				}

				if(item.equals("Left gun") && dragging >= 0)
				{
					if(dragging == 0)
					{
						if(!(leftgun[1][0] == blokjex && leftgun[1][1] == -blokjey) &&!(leftgun[2][0] == blokjex && leftgun[2][1] == -blokjey))
						{
							leftgun[0][0] = blokjex;
							leftgun[0][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 1)
					{
						if(!(leftgun[0][0] == blokjex && leftgun[0][1] == -blokjey) &&!(leftgun[2][0] == blokjex && leftgun[2][1] == -blokjey))
						{
							leftgun[1][0] = blokjex;
							leftgun[1][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 2)
					{
						if(!(leftgun[1][0] == blokjex && leftgun[1][1] == -blokjey) &&!(leftgun[0][0] == blokjex && leftgun[0][1] == -blokjey))
						{
							leftgun[2][0] = blokjex;
							leftgun[2][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}
				}

				if(item.equals("Right gun") && dragging >= 0)
				{
					if(dragging == 0)
					{
						if(!(rightgun[1][0] == blokjex && rightgun[1][1] == -blokjey) &&!(rightgun[2][0] == blokjex && rightgun[2][1] == -blokjey))
						{
							rightgun[0][0] = blokjex;
							rightgun[0][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 1)
					{
						if(!(rightgun[0][0] == blokjex && rightgun[0][1] == -blokjey) &&!(rightgun[2][0] == blokjex && rightgun[2][1] == -blokjey))
						{
							rightgun[1][0] = blokjex;
							rightgun[1][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 2)
					{
						if(!(rightgun[1][0] == blokjex && rightgun[1][1] == -blokjey) &&!(rightgun[0][0] == blokjex && rightgun[0][1] == -blokjey))
						{
							rightgun[2][0] = blokjex;
							rightgun[2][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}
				}

				if(item.equals("Left rear gun") && dragging >= 0)
				{
					if(dragging == 0)
					{
						if(!(leftreargun[1][0] == blokjex && leftreargun[1][1] == -blokjey) &&!(leftreargun[2][0] == blokjex && leftreargun[2][1] == -blokjey))
						{
							leftreargun[0][0] = blokjex;
							leftreargun[0][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 1)
					{
						if(!(leftreargun[0][0] == blokjex && leftreargun[0][1] == -blokjey) &&!(leftreargun[2][0] == blokjex && leftreargun[2][1] == -blokjey))
						{
							leftreargun[1][0] = blokjex;
							leftreargun[1][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 2)
					{
						if(!(leftreargun[1][0] == blokjex && leftreargun[1][1] == -blokjey) &&!(leftreargun[0][0] == blokjex && leftreargun[0][1] == -blokjey))
						{
							leftreargun[2][0] = blokjex;
							leftreargun[2][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}
				}

				if(item.equals("Right rear gun") && dragging >= 0)
				{
					if(dragging == 0)
					{
						if(!(rightreargun[1][0] == blokjex && rightreargun[1][1] == -blokjey) &&!(rightreargun[2][0] == blokjex && rightreargun[2][1] == -blokjey))
						{
							rightreargun[0][0] = blokjex;
							rightreargun[0][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 1)
					{
						if(!(rightreargun[0][0] == blokjex && rightreargun[0][1] == -blokjey) &&!(rightreargun[2][0] == blokjex && rightreargun[2][1] == -blokjey))
						{
							rightreargun[1][0] = blokjex;
							rightreargun[1][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 2)
					{
						if(!(rightreargun[1][0] == blokjex && rightreargun[1][1] == -blokjey) &&!(rightreargun[0][0] == blokjex && rightreargun[0][1] == -blokjey))
						{
							rightreargun[2][0] = blokjex;
							rightreargun[2][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}
				}

				if(item.equals("Left light") && dragging >= 0)
				{
					if(dragging == 0)
					{
						if(!(leftlight[1][0] == blokjex && leftlight[1][1] == -blokjey) &&!(leftlight[2][0] == blokjex && leftlight[2][1] == -blokjey))
						{
							leftlight[0][0] = blokjex;
							leftlight[0][1] = -blokjey;

							change = true;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 1)
					{
						if(!(leftlight[0][0] == blokjex && leftlight[0][1] == -blokjey) &&!(leftlight[2][0] == blokjex && leftlight[2][1] == -blokjey))
						{
							leftlight[1][0] = blokjex;
							leftlight[1][1] = -blokjey;

							change = true;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 2)
					{
						if(!(leftlight[1][0] == blokjex && leftlight[1][1] == -blokjey) &&!(leftlight[0][0] == blokjex && leftlight[0][1] == -blokjey))
						{
							leftlight[2][0] = blokjex;
							leftlight[2][1] = -blokjey;

							change = true;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}
				}

				if(item.equals("Right light") && dragging >= 0)
				{
					if(dragging == 0)
					{
						if(!(rightlight[1][0] == blokjex && rightlight[1][1] == -blokjey) &&!(rightlight[2][0] == blokjex && rightlight[2][1] == -blokjey))
						{
							rightlight[0][0] = blokjex;
							rightlight[0][1] = -blokjey;

							change = true;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 1)
					{
						if(!(rightlight[0][0] == blokjex && rightlight[0][1] == -blokjey) &&!(rightlight[2][0] == blokjex && rightlight[2][1] == -blokjey))
						{
							rightlight[1][0] = blokjex;
							rightlight[1][1] = -blokjey;

							change = true;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 2)
					{
						if(!(rightlight[1][0] == blokjex && rightlight[1][1] == -blokjey) &&!(rightlight[0][0] == blokjex && rightlight[0][1] == -blokjey))
						{
							rightlight[2][0] = blokjex;
							rightlight[2][1] = -blokjey;

							change = true;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}
				}

				if(item.equals("Missile tube") && dragging >= 0)
				{
					if(dragging == 0)
					{
						if(!(missile[1][0] == blokjex && missile[1][1] == -blokjey) &&!(missile[2][0] == blokjex && missile[2][1] == -blokjey) &&!(missile[3][0] == blokjex && missile[3][1] == -blokjey))
						{
							missile[0][0] = blokjex;
							missile[0][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 1)
					{
						if(!(missile[0][0] == blokjex && missile[0][1] == -blokjey) &&!(missile[2][0] == blokjex && missile[2][1] == -blokjey) &&!(missile[3][0] == blokjex && missile[3][1] == -blokjey))
						{
							missile[1][0] = blokjex;
							missile[1][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 2)
					{
						if(!(missile[0][0] == blokjex && missile[0][1] == -blokjey) &&!(missile[1][0] == blokjex && missile[1][1] == -blokjey) &&!(missile[3][0] == blokjex && missile[3][1] == -blokjey))
						{
							missile[2][0] = blokjex;
							missile[2][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}

					if(dragging == 3)
					{
						if(!(missile[0][0] == blokjex && missile[0][1] == -blokjey) &&!(missile[1][0] == blokjex && missile[1][1] == -blokjey) &&!(missile[2][0] == blokjex && missile[2][1] == -blokjey))
						{
							missile[3][0] = blokjex;
							missile[3][1] = -blokjey;

							if(!firsttime)
							{
								repaint(154+10*(blokjex_ouder), 154 + 10*(blokjey_ouder), 9, 9);
								repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
							}
							else
							{
								repaint();
								firsttime = false;
							}

							blokjex_ouder = blokjex;
							blokjey_ouder = blokjey;
						}
					}
				}

				if(item.equals("Engine") && dragging == 0)
				{
					engine[0] = blokjex;
					engine[1] = -blokjey;

					repaint(154+10*(blokjex_oud), 154 + 10*(blokjey_oud), 9, 9);
					repaint(154+10*(blokjex), 154 + 10*(blokjey), 9, 9);
				}
			}

			blokjex_oud = blokjex;
			blokjey_oud = blokjey;
		}
	}

	public void setHullPoints(int num)
	{
		HullPoints = num;
	}

	public int getHullPoints()
	{
		return HullPoints;
	}

	public void setHull(int x, int y, int pos)
	{
		hull[pos][0] = x;
		hull[pos][1] = y;
	}

	public void setMainGun(int x, int y)
	{
		maingun[0] = x;
		maingun[1] = y;
	}

	public void setLeftGun(int x, int y, int pos)
	{
		leftgun[pos][0] = x;
		leftgun[pos][1] = y;
	}

	public void setRightGun(int x, int y, int pos)
	{
		rightgun[pos][0] = x;
		rightgun[pos][1] = y;
	}

	public void setLeftRearGun(int x, int y, int pos)
	{
		leftreargun[pos][0] = x;
		leftreargun[pos][1] = y;
	}

	public void setRightRearGun(int x, int y, int pos)
	{
		rightreargun[pos][0] = x;
		rightreargun[pos][1] = y;
	}

	public void setLeftLight(int x, int y, int pos)
	{
		leftlight[pos][0] = x;
		leftlight[pos][1] = y;
	}

	public void setRightLight(int x, int y, int pos)
	{
		rightlight[pos][0] = x;
		rightlight[pos][1] = y;
	}

	public void setMissile(int x, int y, int pos)
	{
		missile[pos][0] = x;
		missile[pos][1] = y;
	}

	public void setEngine(int x, int y)
	{
		engine[0] = x;
		engine[1] = y;
	}

	public void setHullM(int[][] temp, int num)
	{
		hull = temp;
		HullPoints = num;
	}

	public void setMainGunM(int[] temp, boolean num)
	{
		maingun = temp;
		MainGun = num;
	}

	public void setLeftGunM(int[][] temp, int num)
	{
		leftgun = temp;
		LeftGunPoints = num;
	}

	public void setRightGunM(int[][] temp, int num)
	{
		rightgun = temp;
		RightGunPoints = num;
	}

	public void setLeftRearGunM(int[][] temp, int num)
	{
		leftreargun = temp;
		LeftRearGunPoints = num;
	}

	public void setRightRearGunM(int[][] temp, int num)
	{
		rightreargun = temp;
		RightRearGunPoints = num;
	}

	public void setLeftLightM(int[][] temp, int num)
	{
		leftlight = temp;
		LeftLightPoints = num;
	}

	public void setRightLightM(int[][] temp, int num)
	{
		rightlight = temp;
		RightLightPoints = num;
	}

	public void setMissileM(int[][] temp, int num)
	{
		missile = temp;
		MissilePoints = num;
	}

	public void setEngineM(int[] temp, boolean num)
	{
		engine = temp;
		Engine = num;
	}

	public int getHullX(int pos)
	{
		return hull[pos][0];
	}

	public int getHullY(int pos)
	{
		return hull[pos][1];
	}

	public boolean getEngine()
	{
		return Engine;
	}

	public int getEngineX()
	{
		return engine[0];
	}

	public int getEngineY()
	{
		return engine[1];
	}

	public boolean getMainGun()
	{
		return MainGun;
	}

	public int getMainGunX()
	{
		return maingun[0];
	}

	public int getMainGunY()
	{
		return maingun[1];
	}

	public int getLeftGunPoints()
	{
		return LeftGunPoints;
	}

	public int getLeftGunX(int pos)
	{
		return leftgun[pos][0];
	}

	public int getLeftGunY(int pos)
	{
		return leftgun[pos][1];
	}

	public int getRightGunPoints()
	{
		return RightGunPoints;
	}

	public int getRightGunX(int pos)
	{
		return rightgun[pos][0];
	}

	public int getRightGunY(int pos)
	{
		return rightgun[pos][1];
	}

	public int getLeftLightPoints()
	{
		return LeftLightPoints;
	}

	public int getLeftLightX(int pos)
	{
		return leftlight[pos][0];
	}

	public int getLeftLightY(int pos)
	{
		return leftlight[pos][1];
	}

	public int getRightLightPoints()
	{
		return RightLightPoints;
	}

	public int getRightLightX(int pos)
	{
		return rightlight[pos][0];
	}

	public int getRightLightY(int pos)
	{
		return rightlight[pos][1];
	}

	public int getLeftRearGunPoints()
	{
		return LeftRearGunPoints;
	}

	public int getLeftRearGunX(int pos)
	{
		return leftreargun[pos][0];
	}

	public int getLeftRearGunY(int pos)
	{
		return leftreargun[pos][1];
	}

	public int getRightRearGunPoints()
	{
		return RightRearGunPoints;
	}

	public int getRightRearGunX(int pos)
	{
		return rightreargun[pos][0];
	}

	public int getRightRearGunY(int pos)
	{
		return rightreargun[pos][1];
	}

	public int getMissilePoints()
	{
		return MissilePoints;
	}

	public int getMissileX(int pos)
	{
		return missile[pos][0];
	}

	public int getMissileY(int pos)
	{
		return missile[pos][1];
	}

	public boolean changed()
	{
		boolean tmp;
		tmp = change;
		change = false;

		return tmp;
	}

	public void setTool(String str)
	{
		tool = str;
		selected = -1;
		repaint();
	}

	public void setItem(String str)
	{
		item = str;
	}

	public void teken()
	{
		repaint();
		change = true;
	}

	public void reset()
	{
		HullPoints=0;
		MainGun=false;
		LeftGunPoints = 0;
		RightGunPoints = 0;
		LeftRearGunPoints = 0;
		RightRearGunPoints = 0;
		LeftLightPoints = 0;
		RightLightPoints = 0;
		MissilePoints = 0;
		Engine = false;

		boolean change = true;
		blokjex_oud=-90;
		blokjey_oud=-90;
		blokjex_ouder=-90;
		blokjey_ouder=-90;
		dragging = -1;
		selected = -1;

		repaint();
	}
}















class Knoppen extends Canvas
{
	boolean[] keuze = {false, false, false, false, false, false, false, false};
	String richting = "nowhere";
	int nummer;

	public void paint(Graphics g)
	{
		for(int i=0;i<2;i++)
		{
			for(int j=0;j<4;j++)
			{
				g.drawRect(3+33*i,3+33*j,30,30);

				g.setColor(Color.blue.darker());
				if(keuze[4*i+j])
				{
					g.fillRect(4+33*i,4+33*j,29,29);
				}
				g.setColor(Color.black);
			}

// New
			int[] tmpx = {18,12,24};
			int[] tmpy = {10,26,26};
			g.drawPolygon(tmpx,tmpy,3);
			g.drawLine(7,11,11,14);
			g.drawLine(25,14,29,11);
			g.drawLine(6,16,10,18);
			g.drawLine(26,18,30,16);
			g.drawLine(6,22,9,22);
			g.drawLine(27,22,30,22);
			g.drawLine(6,27,8,26);
			g.drawLine(28,26,30,27);

// Save
			tmpx[0] = 45;tmpx[1] = 42;tmpx[2] = 39;
			tmpy[0] = 12;tmpy[1] = 5;tmpy[2] = 12;
			g.drawPolygon(tmpx,tmpy,3);
			int[] tmpx2 = {64,64,55,53,53};
			int[] tmpy2 = {20,31,31,29,20};
			g.drawPolygon(tmpx2,tmpy2,5);
			g.drawRect(55,20,7,7);
			g.drawArc(41,9,15,15,0,90);
			g.drawLine(56,16,58,13);
			g.drawLine(56,16,54,13);

// Load
			tmpx[0] = 12;tmpx[1] = 9;tmpx[2] = 6;
			tmpy[0] = 45;tmpy[1] = 38;tmpy[2] = 45;
			g.drawPolygon(tmpx,tmpy,3);
			int[] tmpx1 = {31,31,22,20,20};
			int[] tmpy1 = {53,64,64,62,53};
			g.drawPolygon(tmpx1,tmpy1,5);
			g.drawRect(22,53,7,7);
			g.drawArc(8,42,15,15,0,90);
			g.drawLine(16,42,18,44);
			g.drawLine(16,42,18,40);

// Check
			tmpx[0] = 47;tmpx[1] = 43;tmpx[2] = 52;
			tmpy[0] = 42;tmpy[1] = 50;tmpy[2] = 50;
			g.drawPolygon(tmpx,tmpy,3);
			g.drawOval(40,39,15,15);
			g.drawOval(39,38,17,17);
			g.drawLine(53,52,60,60);
			g.drawLine(53,53,60,61);

// Rotate left
			g.drawArc(8,74,20,20,50,310);
			g.drawLine(28,84,31,87);
			g.drawLine(28,84,25,87);

// Rotate right
			g.drawArc(41,74,20,20,180,310);
			g.drawLine(41,84,38,87);
			g.drawLine(41,84,44,87);

// Mirror horizontal
			tmpx[0] = 5;tmpx[1] = 10;tmpx[2] = 15;
			tmpy[0] = 120;tmpy[1] = 110;tmpy[2] = 120;
			g.drawPolygon(tmpx,tmpy,3);
			tmpx[0] = 31;tmpx[1] = 26;tmpx[2] = 21;
			tmpy[0] = 120;tmpy[1] = 110;tmpy[2] = 120;
			g.drawPolygon(tmpx,tmpy,3);
			g.drawLine(18,106,18,127);

// Mirror vertical
			tmpx[0] = 56;tmpx[1] = 46;tmpx[2] = 56;
			tmpy[0] = 104;tmpy[1] = 109;tmpy[2] = 114;
			g.drawPolygon(tmpx,tmpy,3);
			tmpx[0] = 56;tmpx[1] = 46;tmpx[2] = 56;
			tmpy[0] = 120;tmpy[1] = 125;tmpy[2] = 130;
			g.drawPolygon(tmpx,tmpy,3);
			g.drawLine(40,117,62,117);


			g.setColor(Color.blue.darker());
			if(richting.equals("left"))
			{
				int[] tempx = {5,19,33,19};
				int[] tempy = {175,161,175,189};
				g.fillPolygon(tempx, tempy, 4);
			}

			if(richting.equals("right"))
			{
				int[] tempx = {35,49,63,49};
				int[] tempy = {175,161,175,189};
				g.fillPolygon(tempx, tempy, 4);
			}

			if(richting.equals("up"))
			{
				int[] tempx = {20,34,48,34};
				int[] tempy = {160,146,160,174};
				g.fillPolygon(tempx, tempy, 4);
			}

			if(richting.equals("down"))
			{
				int[] tempx = {20,34,48,34};
				int[] tempy = {190,176,190,204};
				g.fillPolygon(tempx, tempy, 4);
			}

			if(richting.equals("center"))
			{
				g.setColor(Color.blue.darker());
			}
			else
			{
				g.setColor(Color.blue);
			}
			g.fillOval(24,165,20,20);

			g.setColor(Color.black);

			g.drawLine(4,175,34,145);
			g.drawLine(4,175,34,205);
			g.drawLine(34,145,64,175);
			g.drawLine(34,205,64,175);

			g.drawLine(19,160,26,167);
			g.drawLine(42,183,49,190);
			g.drawLine(19,190,26,183);
			g.drawLine(42,167,49,160);

			g.drawOval(24,165,20,20);
//			g.drawString("?",31,180);
		}
	}

	public void turnOn(int num)
	{
		nummer = num;
		keuze[num] = true;
		repaint(4+33*(num-num%4) / 4,4+33*(num%4),29,29);
	}

	public void turnOff()
	{
		keuze[nummer] = false;
		repaint(4+33*(nummer-nummer%4) / 4,4+33*(nummer%4),29,29);
	}

	public void move(String str)
	{
		richting = str;
		repaint(4, 145, 60, 60);
	}
}








/*

NEXT CLASS STARTS HERE

*/









class SaveFrame extends Frame
{
	public boolean action(Event e, Object arg)
	{
		if(e.target instanceof Button)
		{
			this.hide();
		}

		return true;
	}
}










class LoadFrame extends Frame
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

	int HullPoints=0;
	boolean MainGun=false;
	int LeftGunPoints = 0;
	int RightGunPoints = 0;
	int LeftLightPoints = 0;
	int RightLightPoints = 0;
	int LeftRearGunPoints = 0;
	int RightRearGunPoints = 0;
	int MissilePoints = 0;
	boolean Engine = false;

	String shipname="";
	String author="";

	boolean change = false;

	LoadFrame()
	{
		this.setLayout(new BorderLayout());

		Panel p1 = new Panel();
		Panel p2 = new Panel();

		p1.add(loadstring);
		p2.add(new Button("Load"));
		p2.add(new Button("Cancel"));

                this.add("North",p1);
                this.add("Center",p2);
	}


	public boolean action(Event e, Object arg)
	{
		if(e.target instanceof Button)
		{
			if(((String)arg).equals("Cancel")) this.hide();
			else
			{
			HullPoints=0;
			MainGun=false;
			LeftGunPoints = 0;
			RightGunPoints = 0;
			LeftLightPoints = 0;
			RightLightPoints = 0;
			LeftRearGunPoints = 0;
			RightRearGunPoints = 0;
			MissilePoints = 0;
			Engine = false;

			shipname="";
			author="";

			int i=0;
			int ii=0;
			this.hide();

			for(int again=0;again<2;again++)
			{
				i=loadstring.getText().indexOf("NM:")+3;
				ii=loadstring.getText().indexOf(")",i);

				if(i>5 && ii>0)
				{
					shipname = loadstring.getText().substring(i,ii);
					shipname = shipname.trim();
				}

				i=loadstring.getText().indexOf("AU:")+3;
				ii=loadstring.getText().indexOf(")",i);

				if(i>5 && ii>0)
				{
					author = loadstring.getText().substring(i,ii);
					author = author.trim();
				}

				i=loadstring.getText().indexOf("SH:");

				if(i>=0)
				{
					int j=0;
					int k=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;

					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);

						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;

							while(pointer == ' ')
							{
									j++;
									pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							hull[k][0] = temp;
							negative=false;

							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;

							hull[k][1] = temp;
							k++;
							negative=false;
							HullPoints=k;
						}

						j++;
					}
	
					change = true;
				}
	
	
				i=loadstring.getText().indexOf("MG:");
	
				if(i>=0)
				{
					int j=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;
	
					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);
	
						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(!negative) temp = -temp;
	
							while(pointer == ' ')
							{
									j++;
									pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							maingun[0] = temp;
							negative=false;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							maingun[1] = temp;
							negative=false;
							MainGun=true;
						}
	
						j++;
					}
				}
	
	
				i=loadstring.getText().indexOf("LG:");
	
				if(i>=0)
				{
					int j=0;
					int k=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;
	
					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);
	
						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(!negative) temp = -temp;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							leftgun[k][0] = temp;
							negative=false;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;

							leftgun[k][1] = temp;
							k++;
							negative=false;
							LeftGunPoints=k;
						}
	
						j++;
					}
				}
	
	
	
	
	
	
	
				i=loadstring.getText().indexOf("RG:");
	
				if(i>=0)
				{
					int j=0;
					int k=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;
	
					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);
	
						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(!negative) temp = -temp;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							rightgun[k][0] = temp;
							negative=false;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							rightgun[k][1] = temp;
							k++;
							negative=false;
							RightGunPoints=k;
						}	

						j++;
					}
				}
	
	



				i=loadstring.getText().indexOf("LR:");
	
				if(i>=0)
				{
					int j=0;
					int k=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;
	
					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);
	
						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							leftreargun[k][0] = temp;
							negative=false;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;

							leftreargun[k][1] = temp;
							k++;
							negative=false;
							LeftRearGunPoints=k;
						}
	
						j++;
					}
				}
	
	





				i=loadstring.getText().indexOf("RR:");

				if(i>=0)
				{
					int j=0;
					int k=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;

					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);
	
						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							while(pointer == ' ')
							{
									j++;
									pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							rightreargun[k][0] = temp;
							negative=false;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;

							rightreargun[k][1] = temp;
							k++;
							negative=false;
							RightRearGunPoints=k;
						}
	
						j++;
					}
				}
	

				i=loadstring.getText().indexOf("LL:");

				if(i>=0)
				{
					int j=0;
					int k=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;

					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);
	
						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							leftlight[k][0] = temp;
							negative=false;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;

							leftlight[k][1] = temp;
							k++;
							negative=false;
							LeftLightPoints=k;
						}

						j++;
					}
				}
	






				i=loadstring.getText().indexOf("RL:");

				if(i>=0)
				{
					int j=0;
					int k=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;
	
					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);

						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							while(pointer == ' ')
							{
									j++;
									pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							rightlight[k][0] = temp;
							negative=false;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							rightlight[k][1] = temp;
							k++;
							negative=false;
							RightLightPoints=k;
						}

						j++;
					}
				}
	



				i=loadstring.getText().indexOf("MR:");

				if(i>=0)
				{
					int j=0;
					int k=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;
	
					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);
	
						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							while(pointer == ' ')
							{
									j++;
									pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							missile[k][0] = temp;
							negative=false;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;

							missile[k][1] = temp;
							k++;
							negative=false;
							MissilePoints=k;
						}
	
						j++;
					}
				}


				i=loadstring.getText().indexOf("EN:");
	
				if(i>=0)
				{
					int j=0;
					int k=0;
					int temp=0;
					char pointer=' ';
					boolean negative = false;
	
					while(pointer != ')')
					{
						pointer = loadstring.getText().charAt(i+3+j);
	
						if(pointer == '-' || (pointer>='0' && pointer<='9')) 
						{
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							while(pointer == ' ')
							{
									j++;
									pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer == ',')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							engine[0] = temp;
							negative=false;
	
							while(pointer == ' ')
							{
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer == '-')
							{
								negative=true;
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}
	
							if(pointer>='0' && pointer<='9')
							{
								temp = (int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(pointer>='0' && pointer<='9')
							{
								temp = 10*temp+(int)('0'-pointer);
								j++;
								pointer = loadstring.getText().charAt(i+3+j);
							}

							if(!negative) temp = -temp;
	
							engine[1] = temp;
							negative=false;
							Engine=true;
						}
	
						j++;
					}
				}
			}	
		}
}
		return true;
	}

	public int[][] getHull()
	{
		return hull;
	}

	public int getHullPoints()
	{
		return HullPoints;
	}

	public boolean getMainGun()
	{
		return MainGun;
	}

	public int[] getmaingun()
	{
		return maingun;
	}

	public int getLeftGunPoints()
	{
		return LeftGunPoints;
	}

	public int[][] getLeftGun()
	{
		return leftgun;
	}

	public int getRightGunPoints()
	{
		return RightGunPoints;
	}

	public int[][] getRightGun()
	{
		return rightgun;
	}

	public int getLeftRearGunPoints()
	{
		return LeftRearGunPoints;
	}

	public int[][] getLeftRearGun()
	{
		return leftreargun;
	}

	public int getRightRearGunPoints()
	{
		return RightRearGunPoints;
	}

	public int[][] getRightRearGun()
	{
		return rightreargun;
	}

	public int getLeftLightPoints()
	{
		return LeftLightPoints;
	}

	public int[][] getLeftLight()
	{
		return leftlight;
	}

	public int getRightLightPoints()
	{
		return RightLightPoints;
	}

	public int[][] getRightLight()
	{
		return rightlight;
	}

	public int getMissilePoints()
	{
		return MissilePoints;
	}

	public int[][] getMissile()
	{
		return missile;
	}

	public boolean getEngine()
	{
		return Engine;
	}

	public int[] getengine()
	{
		return engine;
	}

	public String getName()
	{
		return shipname;
	}

	public String getAuthor()
	{
		return author;
	}

	public boolean changed()
	{
		boolean tempbool = change;
		change = false;
		return tempbool;
	}
}




class CheckFrame extends Frame
{
	Doek doek = new Doek();

	CheckFrame()
	{
		setBackground(Color.black);

		this.setLayout(new BorderLayout());

		Panel p1 = new Panel();
		Panel p2 = new Panel();

                this.add("Center", p1);
                this.add("South", p2);

		doek.resize(400,200);
		doek.setBackground(Color.black);
		p1.add(doek);
		p2.add(new Button("   Ok   "));
	}

	public void setErrors(boolean[] temp)
	{
		doek.setErrors(temp);
	}

	public boolean action(Event e, Object arg)
	{
		this.hide();

		return true;
	}
}

class Doek extends Canvas
{
	boolean[] error = new boolean[6];

	public void setErrors(boolean[] temp)
	{
		error = temp;
	}

	public void paint(Graphics g)
	{
		g.setColor(Color.white);
		if(error[0])
		{
			g.setColor(Color.green);
			g.drawString("One hull point is at least eight points to the left of the center.",5,20);
		}
		else
		{
			g.setColor(Color.red);
			g.drawString("One hullpoint must be at at least eight points to the left from the center",5,20);
		}

		if(error[1])
		{
			g.setColor(Color.green);
			g.drawString("One hull point is at least eight points to the right of the center.",5,40);
		}
		else
		{
			g.setColor(Color.red);
			g.drawString("One hull point must be at least eight points to the right of the center.",5,40);
		}

		if(error[2])
		{
			g.setColor(Color.green);
			g.drawString("One hull point is at least eight points above the center.",5,60);
		}
		else
		{
			g.setColor(Color.red);
			g.drawString("One hull point must be at least eight points above the center.",5,60);
		}
		if(error[3])
		{
			g.setColor(Color.green);
			g.drawString("One hull point is at least eight points below the center.",5,80);
		}
		else
		{
			g.setColor(Color.red);
			g.drawString("One hull point must be at least eight points below the center.",5,80);
		}
		if(error[4])
		{
			g.setColor(Color.green);
			g.drawString("Height plus width is at least 38",5,100);
		}
		else
		{
			g.setColor(Color.red);
			g.drawString("Height plus width must be at least 38",5,100);
		}
		if(!error[5])
		{
			g.setColor(Color.green);
			g.drawString("All ship components are inside shipshape",5,120);
		}
		else
		{
			g.setColor(Color.red);
			g.drawString("All ship components must be inside shipshape",5,120);
		}
	}
}

class Message extends Canvas //implements Runnable
{
//	Thread t2 = new Thread();
//	int counter=20;
//	boolean working=true;

	Message()
	{
		this.resize(480,25);
//		t2.start();
	}

	public void paint(Graphics g)
	{
		g.drawString("Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot -- Play Xpilot", 0, 17);
//		g.drawRect(0,0,479,24);
	}
/*
	public void start()
	{
		if (t2 == null)
		{
			t2 = new Thread(this);
			t2.start();
		}
	}

	public void stop()
	{
		if (t2 != null)
		{
			t2.stop();
			t2 = null;
		}
	}

	public void pause(int duur)
	{
		try {t2.sleep(duur);}
		catch (InterruptedException e) {}
	}

	public void run()
	{
		for(;;)
		{
			counter+=2;
			if(counter > 300) counter = 0;
			repaint();
			pause(100);
		}
	}

	public boolean mouseDown(Event e, int x, int y)
	{
		counter+=2;
		if(!working) t2.resume();
		else t2.suspend();
		working = !working;
		return true;
	}
*/
}
