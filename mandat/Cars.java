//Implementation of Graphical User Interface class
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2016

//Hans Henrik Lovengreen    Oct 3, 2016


import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;

@SuppressWarnings("serial")
class Tile extends JPanel { 

    final static int   edge = 30;       // Tile size
    
    // Colors
    final static Color defcolor      = Color.blue;
    final static Color symbolcolor   = new Color(200,200,200);
    final static Color blockcolor    = new Color(180,180,180);
    final static Color bgcolor       = new Color(250,250,250);  // Light grey
    final static Color slowcolor     = new Color(255,200,80);   // Amber
    final static Color bridgecolor   = new Color(210,210,255);  // Light blue
    final static Color overloadcolor = new Color(255,210,240);  // Pink
    final static Color opencolor     = new Color(0,200,0);      // Dark green
    final static Color closedcolor   = Color.red;
    final static Color barriercolor  = Color.black;
    final static Color barriercolor2 = new Color(255,70,70);    // Emphasis colour - a kind of orange
    
    final static Font f = new Font("SansSerif",Font.BOLD,12);

    final static int maxstaints = 10;
    
    static Color currentBridgeColor = bridgecolor;

    private Cars cars;

    // Model of tile status
    // Modified through event-thread (except for Car no. 0)
    
    private volatile int users = 0;    // No. of current users of tile
    private Color c = defcolor;        
    private char id = ' ';    
    private char symbol = ' ';
    private int xoffset = 0;           // -1,0,1 Horizontal offset
    private int yoffset = 0;           // -1,0,1 Vertical offset

    private boolean isblocked = false; // Tile can be used 
    private boolean hadcrash  = false; // Car crash has occurred 
    private boolean keepcrash = false; // For detecting crashes
    private boolean slowdown  = false; // Slow tile
    private boolean onbridge  = false; // Bridge tile

    private boolean isstartpos = false;    
    private int     startposno = 0;
    private boolean startposopen = false;

    private boolean barriertop = false;
    private boolean barrieractive = false;
    private boolean barrieremph = false;

    private int staintx = 0;
    private int stainty = 0;
    private int staintd = 0;


    private static boolean light (Color c) {
        return (c.getRed() + 2* c.getGreen() + c.getBlue()) > 600;
    }

    public Tile(Pos p, Cars c) {
        cars = c;
        setPreferredSize(new Dimension(edge,edge));
         setBackground(bgcolor);
        setOpaque(true);

        addMouseListener(new MouseAdapter () {
            public void mousePressed(MouseEvent e) {
                if (isstartpos) {
                    if ((e.getModifiers() & InputEvent.SHIFT_MASK) > 0)  
                        cars.removeCar(startposno);
                    else
                        if ((e.getModifiers() & InputEvent.CTRL_MASK) > 0) 
                            cars.restoreCar(startposno);
                        else
                            cars.startTileClick(startposno);
                }
            }
        });
    }

    public void enter(int xoff, int yoff, Color newc, char ch) {
        users++;
        if (users > 1 && keepcrash && !hadcrash) {
        	hadcrash = true;
            // Define a staint
            int dia = 7;
            staintx = (edge-1-dia)/2 +(int)Math.round(Math.random()*4) - 2;
            stainty = (edge-1-dia)/2 +(int)Math.round(Math.random()*4) - 2;
            staintd = dia;
        }
        c = newc;
        id = ch;
        xoffset = xoff;
        yoffset = yoff;
        // repaint();  
    }

    public void exit() {
        users--;
        // repaint();
    }

    public void clean() {
        hadcrash = false;
        // repaint();
    }

    public void setSymbol(char c) {
        symbol = c;
    }

    public void setBlocked(){
        isblocked = true;
        setBackground(blockcolor);
    }

    public void setStartPos(int no, boolean open) {
        setSymbol((char) (no + (int) '0'));
        isstartpos = true;
        startposno = no;
        startposopen=open;
        // repaint();
    }

    public void setStartPos(boolean open) {
        startposopen=open;
        // repaint();
    }

    public void showBarrier(boolean active) {
        barrieractive = active;
        // repaint();
    }

    public void emphasizeBarrier(boolean emph) {
        barrieremph = emph;
        // repaint();
    }

    public void setBarrierPos(boolean top) {
        barriertop = top;                         //  Set only once
    }

    public void setKeep(boolean keep) {
        keepcrash = keep;
        if (!keep &&  hadcrash) clean();
    }

    public void setSlow(boolean slowdown) {
        this.slowdown = slowdown;
        setBackground(slowdown? slowcolor : bgcolor);
        // repaint();
    }

    public void setBridge(boolean onbridge) {
        this.onbridge = onbridge; 
        setBackground(onbridge? currentBridgeColor : bgcolor);
        // repaint();
    }
    
    public static void setOverload(boolean overloaded) {
        currentBridgeColor = (overloaded ? overloadcolor : bridgecolor);
    }

    // This method may see transiently inconsistent states of the tile if used by Car no. 0
    // This is considered acceptable
    public void paintComponent(Graphics g) {
        g.setColor(isblocked ? blockcolor : (slowdown ? slowcolor: onbridge ? currentBridgeColor: bgcolor));
        g.fillRect(0,0,edge,edge);
        
        if (symbol !=' ') {
            g.setColor(symbolcolor);
            g.setFont(f);
            FontMetrics fm = getFontMetrics(f);
            int w = fm.charWidth(id);
            int h = fm.getHeight();
            g.drawString(""+symbol,((edge-w)/2),((edge+h/2)/2));
        }

        if (hadcrash) {
            g.setColor(Color.red); 
            g.fillOval(staintx,stainty,staintd,staintd);
        }

        if (users > 1 || (users > 0 && isblocked)) {
        	g.setColor(Color.red); 
            g.fillRect(0,0,edge,edge);
        }

        if (users < 0) {
        	System.out.println("Users : " + users);
        	g.setColor(Color.yellow); 
            g.fillRect(0,0,edge,edge);
        }

        if (users > 0) {
            g.setColor(c);
            int deltax = xoffset*(edge/2);
            int deltay = yoffset*(edge/2);
            g.fillOval(3+deltax,3+deltay,edge-7,edge-7);
            if (id != ' ') {
                if (light(c)) 
                    g.setColor(Color.black); 
                else 
                    g.setColor(Color.white); 
                g.setFont(f);
                FontMetrics fm = getFontMetrics(f);
                int w = fm.charWidth(id);
                int h = fm.getHeight();
                g.drawString(""+id,((edge-w)/2)+deltax,((edge+h/2)/2)+deltay);
            }
        }

        if (isstartpos) {
            g.setColor(startposopen ? opencolor : closedcolor);
            g.drawRect(1,1,edge-2,edge-2);

        }

        if (barrieractive) {
            if (!barrieremph) g.setColor(barriercolor); else g.setColor(barriercolor2);
            if (barriertop) 
                g.fillRect(0,0,edge,2);
            else
                g.fillRect(0,edge-2,edge,2);
        }

    }

}

@SuppressWarnings("serial")
class Ground extends JPanel {

    private final int n = Layout.ROWS;   // Rows
    private final int m = Layout.COLS;   // Columns

    private Cars cars;

    private Tile[][] area;


    // Checking for bridge overload is done in this class
    // Initial values must correspond to control panel defaults
    private int onbridge = 0;
    private boolean checkBridge = false;
    private int limit = Cars.initialBridgeLimit;

 
    public Ground(Cars  c) {
        cars = c;
        area = new Tile [n] [m];
        setLayout(new GridLayout(n,m));
        setBorder(BorderFactory.createLineBorder(new Color(180,180,180)));
        
        for (int i = 0; i < n ; i++)
            for (int j = 0; j < m; j++) {
                area [i][j] = new Tile(new Pos(i,j),cars);
                add (area [i][j]);
            }

        // Define Hut and Shed areas
        for (int i = 0; i < n ; i++)
            for (int j = 0; j < m; j++) {
            	Pos pos = new Pos(i,j);
            	
                if (Layout.isHutPos(pos))
                		area[i][j].setBlocked();
                
                if (Layout.isShedPos(pos))
                		area[i][j].setBlocked();         
            }
        
        // Set start/gate positions
        for (int no = 0; no < 9; no++) {
            Pos startpos = cars.getStartPos(no);
            area[startpos.row][startpos.col].setStartPos(no, false);
        }

        // Set barrier tiles (both adjacent tiles)
        for (int no = 0; no < 9; no++) {
        	Pos upper = Layout.getBarrierUpperPos(no);
        	Pos lower = Layout.getBarrierLowerPos(no);
            area[upper.row][upper.col].setBarrierPos(false);
            area[lower.row][lower.col].setBarrierPos(true);
         }
        
        // Use regular repaint of whole playground at 25 FPS to eliminate platform differences
        Timer t = new Timer(1000/25, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                repaint();
            }
        });
        t.start();
 
    }


    boolean isOnBridge(Pos pos) {
        return pos.col >= 1 && pos.col <= 3 && pos.row >= 9 && pos.row <= 11;
    }

    
   // The following methods are normally called through the event-thread.
   // May also be called directly by Car no. 0, but then for private tiles.
   // Hence no synchronization is necessary
    
    public void mark(Pos p, Color c, int no) {
        Tile f = area[p.row][p.col];
        f.enter(0,0,c,(char) (no + (int) '0'));
        if (isOnBridge(p)) onbridge++;
        bridgeCheck();
        // repaint();
    }

    public void mark(Pos p, Pos q, Color c, int no) {
        Tile fp = area[p.row][p.col];
        Tile fq = area[q.row][q.col];
        char marker = (char) (no + (int) '0');
        fp.enter(q.col-p.col,q.row-p.row,c,marker);
        fq.enter(p.col-q.col,p.row-q.row,c,marker);
        if (isOnBridge(p) || isOnBridge(q)) onbridge++;
        bridgeCheck();
        // repaint();
   }

    public void clear(Pos p) {
        Tile f = area[p.row][p.col];
        f.exit();
        if (isOnBridge(p)) onbridge--;
        // repaint();
    }

    public void clear(Pos p, Pos q) {
        Tile fp = area[p.row][p.col];
        Tile fq = area[q.row][q.col];
        fp.exit();
        fq.exit();
        if (isOnBridge(p) || isOnBridge(q)) onbridge--;
        // repaint();
    }

    // The following internal graphical methods are only called via the event-thread
        
    void setOpen(int no) {
        Pos p = cars.getStartPos(no);
        area[p.row][p.col].setStartPos(true);
        // repaint();
    }

    void setClosed(int no) {
        Pos p = cars.getStartPos(no);
        area[p.row][p.col].setStartPos(false);
        // repaint();
    }

    void showBarrier(boolean active) {
        for (int no = 0; no < 9; no++) {
            Pos p = cars.getBarrierPos(no);
            area[p.row][p.col].showBarrier(active);
            area[p.row + (no < 5 ? -1 : 1)][p.col].showBarrier(active);
        }
        // repaint();
    }    

    void setBarrierEmphasis(boolean emph) {
        for (int no = 0; no < 9; no++) {
            Pos p = cars.getBarrierPos(no);
            area[p.row][p.col].emphasizeBarrier(emph);
            area[p.row + (no < 5 ? -1 : 1)][p.col].emphasizeBarrier(emph);
        }
        // repaint();
    }    

    void setKeep(boolean keep) {
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++) 
                area[i][j].setKeep(keep);
    }

    void setSlow(boolean slowdown) {
        for (int i = 0; i < n; i++) 
            area[i][0].setSlow(slowdown);
        // repaint();
    }
    
    void showBridge(boolean active) {
        for (int i = 9; i < 11; i++) 
            for (int j = 1; j < 4; j++) 
                area[i][j].setBridge(active);
        checkBridge = active;
        bridgeCheck();
    }
    
    void setLimit(int max) {
        limit = max;
        bridgeCheck();
     }
    
    void bridgeCheck() {
        if (checkBridge) { 
            Tile.setOverload(onbridge > limit);
            for (int i = 9; i < 11; i++) 
                for (int j = 1; j < 4; j++) 
                    area[i][j].repaint();
        }
    }
    
}

@SuppressWarnings("serial")
class ControlPanel extends JPanel {

    private int test_count = 20;

    Cars cars;

    JPanel button_panel = new JPanel();

    JCheckBox keep = new JCheckBox("Keep crash", false);
    JCheckBox slow = new JCheckBox("Slowdown", false);

    JPanel barrier_panel = new JPanel();

    // JCheckBox barrier_on = new JCheckBox("Active", false);
 
    JButton barrier_on       = new JButton("On");
    JButton barrier_off      = new JButton("Off");
    // JButton barrier_shutdown = new JButton("Shut down");

    JPanel bridge_panel = new JPanel();   // Combined with barrier panel

    JCheckBox bridge_on = new JCheckBox("Show", false);
 
    JLabel     threshold_label = new JLabel("Threshold:");
    JComboBox<String> barrier_threshold = new JComboBox<String>();
    int currentThreshold = 9;

    // int currentLimit = Cars.initialBridgeLimit;
    // JLabel     limit_label  = new JLabel("Bridge limit:");
    // JComboBox<String>  bridge_limit = new JComboBox<String>();

    JPanel test_panel = new JPanel();

    JComboBox<String> test_choice = new JComboBox<String>();

    public ControlPanel (Cars c) {
        cars = c;

        Insets bmargin = new Insets(2,5,2,5);
        
        setLayout( new GridLayout(3,1) );
        
        JButton start_all = new JButton("Start all");
        start_all.setMargin(bmargin);
        start_all.addActionListener( new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                cars.startAll();
            }
        });

        JButton stop_all = new JButton("Stop all");
        stop_all.setMargin(bmargin);
        stop_all.addActionListener( new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                cars.stopAll();
            }
        });

        keep.addItemListener( new ItemListener () {
            public void itemStateChanged(ItemEvent e) {
                cars.setKeep(keep.isSelected());
            }
        });

        slow.addItemListener( new ItemListener () {
            public void itemStateChanged(ItemEvent e) {
                cars.setSlow(slow.isSelected());
            }
        });

        button_panel.add(start_all);
        button_panel.add(stop_all);
        button_panel.add(new JLabel("  "));
        button_panel.add(keep);
        button_panel.add(new JLabel(""));
        button_panel.add(slow);

        add(button_panel);

        barrier_panel.add(new JLabel("Barrier:"));
        
        barrier_on.setMargin(bmargin);
        barrier_off.setMargin(bmargin);
        // barrier_shutdown.setMargin(bmargin);
 
        barrier_on.addActionListener( new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                cars.barrierOn();
            }
        });

        barrier_off.addActionListener( new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                cars.barrierOff();
            }
        });

/*
        barrier_shutdown.addActionListener( new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                cars.barrierShutDown(null);
            }
        });
*/

        barrier_panel.add(barrier_on);
        barrier_panel.add(barrier_off);
        //barrier_panel.add(barrier_shutdown);
        
/*
        barrier_panel.add(barrier_on);
        
        barrier_on.addItemListener( new ItemListener () {
            public void itemStateChanged(ItemEvent e) {
                cars.barrierClicked(barrier_on.isSelected());
            }
        });
 */
 
        barrier_panel.add(new JLabel("   "));


        for (int i = 0; i <= 7; i++) 
            barrier_threshold.addItem(""+(i+2));
        barrier_threshold.setSelectedIndex(currentThreshold - 2);

        barrier_threshold.addActionListener( new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                int t = barrier_threshold.getSelectedIndex() + 2; 
                // Ignore internal changes
                if (t != currentThreshold) {
                	cars.barrierSet(t, null);
                }
            }
        });

        barrier_panel.add(threshold_label);
        barrier_panel.add(barrier_threshold);

        
/*
        barrier_panel.add(new JLabel("Bridge:"));
        barrier_panel.add(bridge_on);
 
        bridge_on.addItemListener( new ItemListener () {
            public void itemStateChanged(ItemEvent e) {
                cars.showBridge(bridge_on.isSelected());
            }
        });



       for (int i = 0; i < 6; i++) 
            bridge_limit.addItem(""+(i+1));
        bridge_limit.setSelectedIndex(currentLimit-1);

        bridge_limit.addActionListener( new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                int i = bridge_limit.getSelectedIndex();
                System.out.println("Select event " + i);
                // Ignore internal changes
                if (i+1 != currentLimit) {
                    System.out.println("Calling setLimit");
                    cars.setLimit(i+1);
                }
            }
        });
        
        barrier_panel.add(bridge_limit);
*/        
       
        add(barrier_panel);

                 
        for (int i = 0; i < test_count; i++) 
            test_choice.addItem(""+i);

        JButton run_test = new JButton("Run test no.");
        run_test.setMargin(bmargin);
        run_test.addActionListener( new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                int i = test_choice.getSelectedIndex();
                cars.runTest(i);
            }
        });

        test_panel.add(run_test);
        test_panel.add(test_choice);
        add(test_panel);

    }

    public void barrierSetBegin() {
    	// barrier_on.setEnabled(false);
       	// barrier_off.setEnabled(false);
       	barrier_threshold.setEnabled(false);
    }
    
    public void barrierSetEnd(int k) {
    	// barrier_on.setEnabled(true);
    	// barrier_off.setEnabled(true);
        if (k !=currentThreshold ) {
        	currentThreshold = k;
        	barrier_threshold.setSelectedIndex(k - 2);
        }
    	barrier_threshold.setEnabled(true);
    }
    
/*
        public void shutDownBegin() {
    	barrier_on.setEnabled(false);
       	barrier_off.setEnabled(false);
       	barrier_shutdown.setEnabled(false);
          }
    
    public void shutDownEnd() {
    	barrier_on.setEnabled(true);
    	barrier_off.setEnabled(true);
    	barrier_shutdown.setEnabled(true);
    }
    
   public void disableBridge() {
        limit_label.setEnabled(false);
        bridge_limit.setEnabled(false);
   }
    
   public void disableLimit() {
       // bridge_limit.setEnabled(false);
   }

    public void enableLimit(int k) {
        currentLimit = k;
        if (k - 1 != bridge_limit.getSelectedIndex()) bridge_limit.setSelectedIndex(k - 1 );
        // bridge_limit.setEnabled(true);
    }

    public void setBridge(boolean active) {
    	// Precaution to avoid infinite event seqeunce of events
    	if (active != bridge_on.isSelected()) {
    		bridge_on.setSelected(active);
    	}
     }
*/
    

}


@SuppressWarnings({ "serial" })
public class Cars extends JFrame implements CarDisplayI {

	static final int width      =   30;       // Width of text area
    static final int minhistory =   50;       // Min no. of lines kept

    public static final int initialBridgeLimit = 4;
    
    // Model
    private boolean[] gateopen = new boolean[9];
    private Pos[] startpos     = new Pos[9];
    private Pos[] barrierpos   = new Pos[9];

    private boolean barrieractive = false;
    private boolean bridgepresent = false;
    private volatile boolean slowdown = false;     // Flag read concurrently by isSlow()

    private CarControlI ctr;
    private CarTestWrapper testwrap;
    private Thread test;

    private Ground gnd;
    private JPanel gp;
    private ControlPanel cp;
    private JTextArea txt;
    private JScrollPane log;

    class LinePrinter implements Runnable {
        String m;

        public LinePrinter(String line) {
            m = line;
        }

        public void run() {
            int lines = txt.getLineCount();
            if (lines > 2*minhistory) {
                try {
                    int cutpos = txt.getLineStartOffset(lines/2);
                    txt.replaceRange("",0,cutpos);
                } catch (Exception e) {}
            }
           txt.append(m+"\n");
        }
    }

    /*
     * Thread to carry out barrier threshold setting since
     * it may be blocked by CarControl
     */
      
    class SetThread extends Thread {
        int newval;

        public SetThread(int newval) {
            this.newval =  newval;
        }

        public void run() {
        	try {
        		ctr.barrierSet(newval);

        		// System.out.println("Barrier set returned");
        		EventQueue.invokeLater(new Runnable() {
        			public void run() { barrierSetDone(); }}
        				);

        	} catch (Exception e) {
        		System.err.println("Exception in threshold setting thread");
        		e.printStackTrace();
        	}

        }
    }
    

/*
 *  NO blocking limit setting in this version
 *
    // Variables used during limit setting
    private SetLimitThread limitThread; 
    private Semaphore      limitDone;
    private int            limitValue;
    
    
     * Thread to carry out change of bridge limit since
     * it may be blocked by CarControl
     
    class SetLimitThread extends Thread {
        int newmax;

        public SetLimitThread(int newmax) {
            this.newmax =  newmax;

        }

        public void run() {
            // ctr.setLimit(newmax);
            
            System.out.println("SetLimit returned");
            EventQueue.invokeLater(new Runnable() {
                public void run() { endSetLimit(); }}
            );
            
        }
    }
*/
    

/*
 *  NO barrier shutdown in this version
 *     
    // Variables used during barrier shut down
    private ShutDownThread shutDownThread = null;
    private Semaphore      shutDownSem;
    
    // Thread to carry out barrier off shut down since
    // it may be blocked by CarControl

    class ShutDownThread extends Thread {
        int newmax;

        public void run() {
            try {
				ctr.barrierShutDown();
				
				//System.out.println("Shut down returned");
				EventQueue.invokeLater(new Runnable() {
				    public void run() { shutDownDone(); }}
				);
			} catch (Exception e) {
				System.err.println("Exception in shut down thread");
				e.printStackTrace();
			}
            
        }
    }
*/
    
    void buildGUI(final Cars cars) {
    	try {
    		
			EventQueue.invokeAndWait(new Runnable() {
				
				public void run() {
					gnd = new Ground(cars);
			        gp  = new JPanel();
			        cp =  new ControlPanel(cars); 
			        txt = new JTextArea("",8,width);
			        txt.setEditable(false);    
			        log = new JScrollPane(txt);
			        log.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

			        setTitle("Cars");
			        setBackground(new Color(200,200,200));

			        gp.setLayout(new FlowLayout(FlowLayout.CENTER));
			        gp.add(gnd);

			        setLayout(new BorderLayout());
			        add("North",gp);
			        add("Center",cp);
			        add("South",log);

			        addWindowListener(new WindowAdapter () {
			            public void windowClosing(WindowEvent e) {
			                System.exit(1);
			            }
			        });

			       // bridgepresent = ctr.hasBridge();
			       // gnd.showBridge(bridgepresent);
			       // cp.setBridge(bridgepresent);
			       // if (! bridgepresent) cp.disableBridge();
			        
			        pack();
			        setBounds(100,100,getWidth(),getHeight());
			        setVisible(true);
				}
			});
			
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    public Cars() {
    	
        for (int no = 0; no < 9; no++) {
        	startpos[no]   = Layout.getStartPos(no);
        	barrierpos[no] = Layout.getBarrierPos(no);
        	gateopen[no] = false;
        }
        
        buildGUI(this);
    	
        // Add control
        testwrap = new CarTestWrapper(this);

        ctr = new CarControl(this);
    }

    public static void main(String [] args) {
        new Cars();
    }

    // High-level event handling -- to be called by gui thread only
    // The test thread activates these through the event queue via the
    // CarTestWrapper

    public void barrierOn() {
        gnd.showBarrier(true); 
        barrieractive = true;
        ctr.barrierOn();
    }

    public void barrierOff() {
        ctr.barrierOff();
        gnd.showBarrier(false);
        barrieractive = false;
    }

    
/*
    void barrierShutDown(Semaphore done) {

        if (shutDownThread != null ) {
            println("WARNING: Barrier shut down already in progress");
            if (done != null) done.V();
            return;
        }

        gnd.showBarrier(2);
        cp.shutDownBegin();
        // Hold values for post-processing
        shutDownSem = done;
        shutDownThread = new ShutDownThread();
        shutDownThread.start();
    }
    
    // Called when Shut Down Thread has ended
    void shutDownDone() {

        // System.out.println(" start");
    	        cp.shutDownEnd();
 
        if (shutDownSem != null ) shutDownSem.V();
        shutDownThread = null;
        shutDownSem = null;

        gnd.showBarrier(0);
        barrieractive = false;
       // System.out.println("endSyncOff end");
    }
*/
    
    // Variables used during threshold setting
    private SetThread setThread = null;
    private Semaphore setSem = null;
    private int setVal;

    void  barrierSet(int k, Semaphore done) {

    	// System.out.println("Cars.barrierSet called");
    	
    	if (setThread != null ) {
    		println("WARNING: Threshold setting already in progress");
    		if (done != null) done.V();
    		return;
    	}

    	if (k < 2 || k > 9) {
    		println("WARNING: Threshold value out of range: " + k + " (ignored)");
    		if (done != null) done.V();
    		return;
    	}

        gnd.setBarrierEmphasis(true);
    	cp.barrierSetBegin();
    	// Hold values for post-processing
    	setSem = done;
    	setVal = k;
    	setThread = new SetThread(k);
    	setThread.start();
    }

    // Called when Set Thread has ended
    void barrierSetDone() {

    	// System.out.println("Cars.barrierSetDone called");
    	cp.barrierSetEnd(setVal);
        gnd.setBarrierEmphasis(false);

    	if (setSem != null ) setSem.V();
    	setThread = null;
    	setSem = null;
    }

    void barrierClicked(boolean on) {
        if (on) barrierOn(); else barrierOff();
    }

    public void setSlow(final boolean slowdown) {
        this.slowdown = slowdown;
        gnd.setSlow(slowdown);
    }

    public void startAll() {
        int first = barrieractive ? 0 : 1; 
        // Should not start no. 0 if no barrier
        for (int no = first; no < 9; no++) 
            startCar(no);
    }

    public void stopAll() {
        for (int no = 0; no < 9; no++) 
            stopCar(no);
    }

    void runTest(int i) {
        if (test!=null && test.isAlive()) {
            println("Test already running");
            return;
        }
        println("Run of test "+i);
        test = new CarTest(testwrap,i);
        test.start();
    }

    public void setKeep(final boolean keep) {
        gnd.setKeep(keep);
    }

    void showBridge(boolean active) {
        gnd.showBridge(active);
     }
    
    void startTileClick(int no) {
        if (gateopen[no]) 
            stopCar(no);
        else 
            startCar(no);
    }

    public void startCar(final int no) {
        if (!gateopen[no]) {
            gnd.setOpen(no);
            gateopen[no] = true;
            ctr.startCar(no);
        }
    }

    public void stopCar(final int no) {
        if (gateopen[no]) {
            ctr.stopCar(no);
            gnd.setClosed(no);
            gateopen[no] = false;
        }
    }

    public void setSpeed(int no, int speed) {
        ctr.setSpeed(no,speed);
    }

    public void setVariation(int no, int var) {
        ctr.setVariation(no,var);
    }

    public void removeCar(int no) {
        ctr.removeCar(no);
    }

    public void restoreCar(int no) {
        ctr.restoreCar(no);
    }

/*
    void setLimit(int max) {

        if (! bridgepresent) {
            println("ERROR: No bridge at this playground!");
            return;
        }
        
        if (max < 1 || max > 6) {
            println("ERROR: Illegal limit value");
            return;
        }

        cp.disableLimit();
        ctr.setLimit(max);
        gnd.setLimit(max);
        cp.enableLimit(max);
     }
*/
    
 /*
 *  No blocking of setLimit in this version
 *     
    void setLimit(int max, Semaphore done) {

        if (! bridgepresent) {
            println("ERROR: No bridge at this playground!");
            if (done != null) done.V();
            return;
        }
        
        if (max < 1 || max > 6) {
            println("ERROR: Illegal limit value");
            if (done != null) done.V();
            return;
        }

        if (limitThread != null ) {
            println("WARNING: Limit setting already in progress");
            if (done != null) done.V();
            return;
        }

        cp.disableLimit();
        // Hold values for post-processing
        limitValue = max;
        limitDone = done;
        limitThread = new SetLimitThread(max);
        limitThread.start();
    }
    
    // Called when SetLimitThread has ended
    void endSetLimit() {

        System.out.println("endSetLimit start");
        if (limitDone != null ) limitDone.V();
        
        gnd.setLimit(limitValue);
        cp.enableLimit(limitValue);
        limitThread = null;
        limitDone = null;
        System.out.println("endSetLimit end");
    }
*/    
    
    // Implementation of CarDisplayI 
    // Mark and clear requests for car no. 0 are processed directly in order not
    // to fill the event queue (with risk of transiently inconsistent graphics)

    // Mark area at position p using color c and number no.
    public void mark(final Pos p, final Color c, final int no){
        if (no != 0)
            EventQueue.invokeLater(new Runnable() {
                public void run() { gnd.mark(p,c,no); }}
            );
        else
            gnd.mark(p,c,no);
    }

    // Mark area between adjacent positions p and q 
    public void mark(final Pos p, final Pos q, final Color c, final int no){
        if (no != 0)
            EventQueue.invokeLater(new Runnable() {
                public void run() { gnd.mark(p,q,c,no); }}
            );
        else
            gnd.mark(p,q,c,no);
    }

    // Clear area at position p
   public void clear(final Pos p){
       if (! Layout.isToddlerPos(p)) 
           EventQueue.invokeLater(new Runnable() {
               public void run() { gnd.clear(p); }}
           );
       else 
           // In toddlers' yard - call directly
    	   gnd.clear(p); 
    }

    // Clear area between adjacent positions p and q.
    public void clear(final Pos p, final Pos q){
        if (! Layout.isToddlerPos(p)) 
        	EventQueue.invokeLater(new Runnable() {
                public void run() { gnd.clear(p,q); }}
            );
        else
        	// In toddlers' yard - call directly
            gnd.clear(p,q); 
     }

    public Pos getStartPos(int no) {      // Identify startposition of Car no.
        return startpos[no];
    }

    public Pos getBarrierPos(int no) {    // Identify pos. at barrier line
        return barrierpos[no];
    }
    
    public Pos nextPos(int no, Pos pos) {
    	// Get from Layout
    	return Layout.nextPos(no, pos);
   }

    public void println(String m) {
        // Print (error) message on screen 
        Runnable job = new LinePrinter(m);
        EventQueue.invokeLater(job);
    }

    public boolean isSlow(Pos pos) { 
        return Layout.isSlowPos(pos) && slowdown;
    }

}


/**
 * For the methods of the CarTestI interface this class wraps them to 
 * similar events to be processed by the gui thread.
 */
class CarTestWrapper implements CarTestingI {

    Cars cars;
    
    public CarTestWrapper(Cars cars) {
        this.cars = cars;
    }
    
    public void startCar(final int no) {
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.startCar(no); }}
        );
    }

    public void stopCar(final int no) {
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.stopCar(no); }}
        );
    }

    public void startAll() {
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.startAll(); }}
        );
    }

    public void stopAll() {
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.stopAll(); }}
        );
    }


    public void barrierOn() {
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.barrierOn(); }}
        );
    }

    public void barrierOff() {
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.barrierOff(); }}
        );
    }

/*
    // This should wait until barrier is off
    // For this, a one-time semaphore is used (as simple Future)
    public void barrierShutDown() {
        final Semaphore done = new Semaphore(0);
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.barrierShutDown(done); }}
        );
        try {
            done.P();
        } catch (InterruptedException e) {}

    }
*/  
    
    Semaphore setDoneSem;
    
    // Start setting of threshold (asynchronously) 
    public void startBarrierSet(final int k) {
    	if (setDoneSem != null) {
    		cars.println("WARNING: setting alread active when startBarrierSet(k) called");
    		return;
    	}
    	
        final Semaphore done = new Semaphore(0);
        setDoneSem = done;
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.barrierSet(k, done); }}
        );
    }
    
    public void awaitBarrierSet() {
       try {
            if (setDoneSem != null) {
            	setDoneSem.P();
            	setDoneSem = null;
            } else 
            	cars.println("WARNING: no active setting when awaitBarrierSet() called");
        } catch (InterruptedException e) {}

    }
    
/*
    public void setLimit(final int k) {
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.setLimit(k); }}
        );
    }
*/
    
     public void setSlow(final boolean slowdown) {
         EventQueue.invokeLater(new Runnable() {
             public void run() { cars.setSlow(slowdown); }}
         );
     }
 
     public void removeCar(final int no) {
         EventQueue.invokeLater(new Runnable() {
             public void run() { cars.removeCar(no); }}
         );
      }

     public void restoreCar(final int no) {
         EventQueue.invokeLater(new Runnable() {
             public void run() { cars.restoreCar(no); }}
         );
     }
     
    public void setSpeed(final int no, final int speed) {
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.setSpeed(no, speed); }}
        );
    }

    public void setVariation(final int no, final int var) {
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.setVariation(no, var); }}
        );
    }

/*
    // This should wait until limit change carried out
    // For this, a one-time semaphore is used (as simple Future)
    public void setLimit(final int k) {
        final Semaphore done = new Semaphore(0);
        EventQueue.invokeLater(new Runnable() {
            public void run() { cars.setLimit(k, done); }}
        );
        try {
            done.P();
        } catch (InterruptedException e) {}

    }
*/    
    
    // Println already wrapped in Cars
    public void println(final String message) {
        cars.println(message);
    }

 }

/*
 * The static Layout class defines the concrete topology of the playground including car tracks
 */
class Layout {
	
	public static final int ROWS = 11;
	public static final int COLS = 12;
	
	static final int upperBarRow = 5;
	static final int lowerBarRow = 6;
	
	private static Pos[][] tracksCW  = new Pos[ROWS][COLS];
	private static Pos[][] tracksCCW = new Pos[ROWS][COLS];
	
	private static Pos[] branchCW  = new Pos[4];
	private static Pos[] branchCCW = new Pos[4];
	
	static {
		// Define next position of all clockwise tracks (except branching points) 
		
		// Alley (upwards)
		for (int i = 1; i <= 10; i++) { tracksCW[i][0] = new Pos(i-1, 0); }
		// Private lanes 
		for (int no = 5; no <= 8; no++) {
			for (int i = 1; i <= 10; i++) { tracksCW[i][no+3] = new Pos(i+1, no+3); }
		}
		// Upper lane (without shed)
		for (int j = 0; j <= 10; j++) { tracksCW[0][j]  = new Pos(0, j+1); }
		// Lower lane
		for (int j = 1; j <= 11; j++) { tracksCW[10][j] = new Pos(10, j-1); }
		
		// Shed avoidance
		tracksCW[1][0] = new Pos(1,1);
		tracksCW[1][1] = new Pos(1,2);
		tracksCW[1][2] = new Pos(0,2);
				
		// CW branching points
		for (int no = 5; no <= 8; no++) { branchCW[no-5] = new Pos(1, no+3); }

		// Define next position of all counter-clockwise tracks (except branching points) 
		
		// Toddler's special path
		tracksCCW[5][2] = new Pos(6,2);
		tracksCCW[6][2] = new Pos(6,3);
		tracksCCW[6][3] = new Pos(5,3);
		tracksCCW[5][3] = new Pos(5,2);
		
		// Alley (downwards)
		for (int i = 1; i <= 8; i++) { tracksCCW[i][0] = new Pos(i+1, 0); }
		// Private lanes (some parts overwritten below)
		for (int no = 1; no <= 4; no++) {
			for (int i = 2; i <= 8; i++) { tracksCCW[i][no+3] = new Pos(i-1, no+3); }
		}
		// Upper lane (car nos. 3,4)
		for (int j = 1; j <= 7; j++) { tracksCCW[1][j] = new Pos(1, j-1); }
		// Upper lane (car nos. 1,2)
		for (int j = 1; j <= 5; j++) { tracksCCW[2][j] = new Pos(2, j-1); }
		// Lower lane
		for (int j = 0; j <= 6; j++) { tracksCCW[9][j] = new Pos(9, j+1); }
		
		// CCW branching points
		for (int no = 1; no <= 4; no++) { branchCCW[no-1] = new Pos(8, no+3); }
	}
	
	public static boolean isShedPos(Pos p) {
		return (p.row==0 && p.col < 2);  // Upper left corner
	}
	
	public static boolean isHutPos(Pos p) {
		if     (p.row < 3 || p.row > 8 || p.col < 1 || p.col > 3) return false;
		return (p.row < 5 || p.row > 6 || p.col < 2);
	}
	
	public static boolean isToddlerPos(Pos p) {
		return (p.col >= 2 || p.col <= 3 || p.row >= 5 || p.row <= 6); 
	}
		
	public static boolean isSlowPos(Pos p) {
		return (p.col ==0  && p.row >= 1);
	}
	
	public static Pos getStartPos(int no) {
		if (no == 0) return new Pos(5,2);
		if (no  < 5) return new Pos(7, 3 + no);
		return new Pos(4, 3 + no);
	}

	public static Pos getBarrierPos(int no) {
		return new Pos( no < 5 ? lowerBarRow : upperBarRow, 3 + no);
	}
	
	public static Pos getBarrierUpperPos(int no) {
		return new Pos(upperBarRow, 3 + no);
	}
	
	public static Pos getBarrierLowerPos(int no) {
		return new Pos(lowerBarRow, 3 + no);
	}
	
	public static Pos nextPos(int no, Pos pos) {

		int mycol = 3+no;
	
	    if (no < 5) {
	    	// CCW
	    	if (pos.row == 9 && pos.col == mycol) {
	    		// Branching point 
	    		return branchCCW[no - 1];
	    	} else {
	    		return tracksCCW[pos.row][pos.col];
	    	}
	    } else {
	    	// CW
	    	if (pos.row == 0 && pos.col == mycol) {
	    		// Branching point 
	    		return branchCW[no - 5];
	    	} else {
	    		return tracksCW[pos.row][pos.col];
	    	}
	    }
	}	
	
}













