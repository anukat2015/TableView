/**
 * @(#) $RCSfile: SpanPlot.java,v $ $Revision: 1.1 $ $Date: 2004/08/02 20:23:46 $ $Name: TableView1_3 $
 *
 * Center for Computational Genomics and Bioinformatics
 * Academic Health Center, University of Minnesota
 * Copyright (c) 2000-2002. The Regents of the University of Minnesota  
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * see: http://www.gnu.org/copyleft/gpl.html
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 */


/*
Span: [Value,Column]  [to,+-] [Value,Column]
Group By [Row DistinctValue Partition] of Column
ForwardColor
ReverseColor

-----+--------------------------------------------+
     |                                            |
     |                                            |
AAA  |   ________   __   ________                 |
     |                                            |
ABC  |       __        _____  _     ________      |
     |                                            |
BBB  |__          ___   ________  ____________    |
     |                                            |
CCC  |          __                                |
     |                                            |
-----+--------------------------------------------+
     |                                            |
     |                                            |
     | ^     ^     ^    ^    ^    ^     ^     ^   |
-----+--------------------------------------------+
*/


package edu.umn.genomics.table;

import java.io.Serializable;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import edu.umn.genomics.layout.BordersLayout;
import edu.umn.genomics.graph.*;
import edu.umn.genomics.graph.swing.*;

/**
 * Display a scatterplot of the values from two ColumnMaps. 
 * Dragging out a rectangle on the panel selects the rows in the TableModel 
 * that are mapped to the data points in the rectangle.  
 * @author       J Johnson
 * @version $Revision: 1.1 $ $Date: 2004/08/02 20:23:46 $  $Name: TableView1_3 $ 
 * @since        1.0
 * @see ColumnMap
 */
public class SpanPlot extends JPanel 
        implements Serializable, ListSelectionListener, DataModel, CleanUp {
  ColumnMap xcol; 
  ColumnMap scol;
  ColumnMap ycol;
  LineFormula rline = null;
  Insets margin = new Insets(40,80,40,10);
  Color defaultColor = Color.blue;
  Color selectColor = Color.cyan;
  Color dataAreaColor = new Color(220,220,220); //Color.white;
  Color axisColor = Color.gray;
  Color gridColor = Color.lightGray;
  Axis xAxis;
  Axis yAxis;
  Point pointArray[] = null;
  boolean selecting = false;
  boolean showAxes = true;
  boolean showGrid = true;
  Point start = null;
  Point current = null;
  ListSelectionModel lsm = null;
  Rectangle pickRect = null;
  String xTitle = "x";
  String yTitle = "y";
  String cursorLoc = null;
  // The data plot area is the insets minus the margins

  String jver = System.getProperty("java.version");
  String pattern = jver.indexOf("1.1") > 0 ? "" : ".####";
  DecimalFormat dfmt = new DecimalFormat(pattern);
  ParsePosition pp = new ParsePosition(0);

  SimpleGraph graph;
  LineDataModel lineModel;
  GraphSegments gs = new GraphSegments();
  GraphSegments gp;
  GraphRects  gr;
  JLabel xLocLbl = new JLabel("              ");
  JLabel yLocLbl = new JLabel("              ");
  JLabel yLbl = new JLabel("              ");
  JLabel xLbl = new JLabel("              ");

  CellMapListener cml = new CellMapListener() {
    public void cellMapChanged(CellMapEvent e) {
      //System.err.println(((ColumnMap)e.getSource()).getName() + " " + e.getMapState());
      if (e.getMapState() == CellMap.INVALID) {
      } else if (!e.mappingInProgress()) {
        setColumnMaps(xcol,scol,ycol);
      }
    }
  };


  /** 
   * Replot the data points whenever the size changes. 
   */
  private ComponentAdapter ca = new ComponentAdapter() {
    public void  componentResized(ComponentEvent e) {
      computePoints();
    }
  };

  /** 
   * Determine the position of the data points in the plot.
   */
  public void computePoints()  {}

   /**
   * Return arrays of the x pixel location and the y pixel location.
   * @param x the x pixel offset
   * @param y the y pixel offset
   * @param axes the axes that transform the datapoints to the pixel area
   * @param points the array of points: xpoints, ypoints
   * @return the array of points: xpoints, ypoints
   */
  public int[][] getPoints(int x, int y, Axis axes[], int points[][]) {
    int pnts[][] = points;
    if (xcol != null && scol != null && ycol != null && 
        xcol.getState() == CellMap.MAPPED && 
        scol.getState() == CellMap.MAPPED && 
        ycol.getState() == CellMap.MAPPED) {
      int w = axes[0].getSize();
      int h = axes[1].getSize();
      int np = xcol.getCount();
      if (pnts == null || pnts.length < 2) {
        pnts = new int[2][];
      }
      if (pnts[0] == null || pnts[0].length != np) {
        pnts[0] = new int[np*2];
      }
      if (pnts[1] == null || pnts[1].length != np) {
        pnts[1] = new int[np*2];
      }
      int yb = y + h;
      for ( int r = 0,xi=0,yi=0; r < np; r++) {
        pnts[0][xi++] = x + axes[0].getIntPosition(xcol.getMapValue(r));
        pnts[0][xi++] = x + axes[0].getIntPosition(scol.getMapValue(r));
        int yval = yb - axes[1].getIntPosition(ycol.getMapValue(r));
        pnts[1][yi++] = yval;
        pnts[1][yi++] = yval;
      }
//System.err.println("SpanPlot.getPoints " + np);
    }
    return pnts;
  }

  public double[] getYValues(int xi) {
    return null; // Should this be implemented?
  }
  
  private MouseAdapter ma = new MouseAdapter() {
    public void mousePressed(MouseEvent e) {
      start = e.getPoint();
      current = e.getPoint();
      selecting = true;
      repaint();
    }
    public void mouseReleased(MouseEvent e) {
      selecting = false;
      current = e.getPoint();
      // temporary ListSelectionModels to record selection in each ColumnMap
      // the selection will be the intersection of the two
      DefaultListSelectionModel xsm = new DefaultListSelectionModel();
      DefaultListSelectionModel ysm = new DefaultListSelectionModel();
      double svals[] = graph.getGraphDisplay().getValueAt(start);
      double evals[] = graph.getGraphDisplay().getValueAt(current);
      double xmin = Math.min(svals[0], evals[0]);
      double xmax = Math.max(svals[0], evals[0]);
      double ymin = Math.min(svals[1], evals[1]);
      double ymax = Math.max(svals[1], evals[1]);
      // get selected rows of x column
      xcol.selectRange(xmin, xmax, xsm);
      // get selected rows of y column
      ycol.selectRange(ymin, ymax, ysm);
      // get the intersection of the two columns
      int min = xsm.getMinSelectionIndex();
      int max = xsm.getMaxSelectionIndex();
      if (min < ysm.getMinSelectionIndex())
        min = ysm.getMinSelectionIndex();
      if (max > ysm.getMaxSelectionIndex())
        max = ysm.getMaxSelectionIndex();
      if (min > 0)
        xsm.removeSelectionInterval(0,min-1);
      int nrows = xcol.getCount();
      if (max+1 < nrows-1)
        xsm.removeSelectionInterval(max+1,nrows-1);
      for (int r = min; r <= max; r++) {
        if (xsm.isSelectedIndex(r)) {
          if (!ysm.isSelectedIndex(r)) {
            xsm.removeSelectionInterval(r,r);
          }
        }
      }
      // combine selection with the previous selection
      xcol.selectValues(xsm);
      repaint();
    }
    public void mouseClicked(MouseEvent e) {
      start = e.getPoint();
      current = e.getPoint();
      repaint();
    }
    public void mouseExited(MouseEvent e) {
      cursorLoc = null;
      xLocLbl.setText("    ");
      yLocLbl.setText("    ");
      repaint();
    }
  };
  private MouseMotionAdapter mma = new MouseMotionAdapter() {
    public void mouseDragged(MouseEvent e) {
      current = e.getPoint();
      displayLoc(e.getPoint());
      repaint();
    }
    public void mouseMoved(MouseEvent e) {
      displayLoc(e.getPoint());
      repaint();
    }
  };

  private void displayLoc(Point p) {
    double v[] = graph.getGraphDisplay().getValueAt(p);
    if (v != null && v.length > 1) {
      Object xval = xcol.getMappedValue(v[0],0);
      Object yval = ycol.getMappedValue(v[1],0);
      xLocLbl.setText(xval != null ? xval.toString() : "NULL");
      yLocLbl.setText(yval != null ? yval.toString() : "NULL");
    }  else {
      xLocLbl.setText("    ");
      yLocLbl.setText("    ");
    }
  }
  /**
   * Construct a scatterplot for the given columns.
   * @param xColumn the column to map to the x axis.
   * @param yColumn the column to map to the y axis.
   */
  public SpanPlot(ColumnMap xColumn, ColumnMap sColumn, ColumnMap yColumn) {
    setLayout(new BorderLayout());
    graph = new SimpleGraph();
    graph.getGraphDisplay().setOpaque(true);
    graph.getGraphDisplay().setBackground(Color.white);
    graph.getGraphDisplay().setGridColor(new Color(220,220,220));
    graph.showGrid(true);
    xAxis = graph.getAxisDisplay(BorderLayout.SOUTH).getAxis();
    yAxis = graph.getAxisDisplay(BorderLayout.WEST).getAxis();
    setColumnMaps(xColumn, sColumn, yColumn);
    /*
    gs = new GraphSegments();
    lineModel = new LineDataModel();
    gs.setData(lineModel);
    graph.addGraphItem(gs);
    */
    gp = new GraphSegments();
    gp.setData(this);
    gp.setColor(Color.blue);
    // gp.setLineWidth(1);
    graph.addGraphItem(gp);
    gr = new GraphRects();
    gr.setColor(Color.cyan);
    graph.addGraphItem(gr);
    
    graph.getAxisDisplay(BorderLayout.SOUTH).setZoomable(true);
    graph.getAxisDisplay(BorderLayout.WEST).setZoomable(true);
    add(graph);
    addComponentListener(ca);
    graph.getGraphDisplay().addMouseListener(ma);
    graph.getGraphDisplay().addMouseMotionListener(mma);
    JPanel lp = new JPanel(new GridLayout(1,2));
    JPanel tp = new JPanel(new BorderLayout());

    xLocLbl.setHorizontalTextPosition(JLabel.RIGHT);
    yLocLbl.setHorizontalTextPosition(JLabel.LEFT);
    xLocLbl.setFont(new Font("monospaced",Font.PLAIN, 10));
    yLocLbl.setFont(new Font("monospaced",Font.PLAIN, 10));

    lp.add(xLocLbl);
    lp.add(yLocLbl);
    tp.add(yLbl, BorderLayout.WEST);
    tp.add(lp);
    xLbl.setHorizontalTextPosition(JLabel.CENTER);
    xLbl.setHorizontalAlignment(JLabel.CENTER);
    xLbl.setForeground(Color.black);
    yLbl.setHorizontalTextPosition(JLabel.LEFT);
    yLbl.setVerticalTextPosition(JLabel.TOP);
    yLbl.setHorizontalAlignment(JLabel.LEFT);
    yLbl.setForeground(Color.black);
    add(tp,BorderLayout.NORTH);
    add(xLbl,BorderLayout.SOUTH);
  }
  /**
   * Construct a scatterplot for the given columns.
   * @param xColumn the column to map to the start of span on the x axis.
   * @param sColumn the column to map to the end of span on the x axis.
   * @param yColumn the column to map to the y axis.
   * @param selectionModel 
   */
  public SpanPlot(ColumnMap xColumn, ColumnMap sColumn, ColumnMap yColumn, 
    ListSelectionModel selectionModel) {
    this(xColumn, sColumn, yColumn);
    setSelectionModel(selectionModel);
  }
  public void setColumnMaps(ColumnMap xColumn, ColumnMap sColumn, ColumnMap yColumn) {
    if (xcol != null) {
      xcol.removeCellMapListener(cml);
    }
    if (scol != null) {
      scol.removeCellMapListener(cml);
    }
    if (ycol != null) {
      ycol.removeCellMapListener(cml);
    }
    this.xcol = xColumn;
    this.scol = sColumn;
    this.ycol = yColumn;
    xcol.addCellMapListener(cml);
    scol.addCellMapListener(cml);
    ycol.addCellMapListener(cml);
    xTitle = xcol.getName();
    yTitle = ycol.getName();
    xLbl.setText(xTitle);
    yLbl.setText(yTitle);
    if (xcol.getState() == CellMap.MAPPED) {
      if (scol.getState() == CellMap.MAPPED) {
        xAxis.setMin(Math.min(xColumn.getMin(),sColumn.getMin()));
        xAxis.setMax(Math.max(xColumn.getMax(),sColumn.getMax()));
      } else {
        xAxis.setMin(xColumn.getMin());
        xAxis.setMax(xColumn.getMax());
      }
    }
    if (ycol.getState() == CellMap.MAPPED) {
      yAxis.setMin(yColumn.getMin());
      yAxis.setMax(yColumn.getMax());
    }
    graph.getAxisDisplay(BorderLayout.SOUTH).setAxisLabeler(
                                   new ColumnMapLabeler(xColumn,20));
    graph.getAxisDisplay(BorderLayout.WEST).setAxisLabeler(
                                   new ColumnMapLabeler(yColumn,20));
//System.err.println("SpanPlot " + xcol + "," + scol + "," + ycol);
    repaint();
  }

  /**
   * Sets the row selection model for this table to newModel and registers
   * with for listener notifications from the new selection model.
   * @param newModel the new selection model
   */
  public void setSelectionModel(ListSelectionModel newModel) {
    if (newModel != null && newModel != lsm) {
      lsm = newModel;
      lsm.addListSelectionListener(this);
      if (gp != null) {
        gp.setIndexedColor(new IndexSelectColor(Color.cyan, null, lsm));
        //gp.setIndexedDrawable(new IndexSelectDrawable(new DrawableX(), null, lsm));
      }
    }
  }
  /**
   * Returns the ListSelectionModel that is used to maintain row
   * selection state.
   * @return the object that provides row selection state.
   */
  public ListSelectionModel getSelectionModel() {
    return lsm;
  }

  /** 
   * Return the component on which the data points are plotted.
   * @return the plotting component.
   */
  public Component getPlotComponent() {
    return graph.getGraphDisplay();
  }

  /**
   * Called whenever the value of the selection changes.
   * @param e the event that characterizes the change in selection.
   */
  public void valueChanged(ListSelectionEvent e) {
    repaint();
  }

  public void finalize() throws Throwable {
    cleanUp();
    super.finalize();
  }

  /**
   *  This removes this view as a listener to the TableModel
   *  and to the ListSelectionModel.  Classes overriding this
   *  method should call super.cleanUp();
   */
  public void cleanUp() {
    if (xcol != null) {
      xcol.removeCellMapListener(cml);
    }
    if (scol != null) {
      scol.removeCellMapListener(cml);
    }
    if (ycol != null) {
      ycol.removeCellMapListener(cml);
    }
    if (lsm != null) {
      lsm.removeListSelectionListener(this);
    }
  }

}
