package de.tucottbus.kt.lcars.j2d;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;
import org.jfree.experimental.swt.SWTUtils;

import de.tucottbus.kt.lcars.LCARS;

/**
 * A geometry representing a text.
 * 
 * @author Matthias Wolff, Christian Borck
 */
public class GText extends Geometry
{
  private static final long serialVersionUID = -1724627622898883028L;
  
  protected String text;
  protected int descent;
  protected FontData fontData;
  
  protected int x;
  protected int y;
  protected transient Rectangle bounds;
  
  protected transient TextLayout tl;
  
  /**
   * Creates a new text geometry. A text geometry provides information and
   * 2D-rendering for a single line of text
   * 
   * @param text
   *          the text
   * @param pos
   *          the position to draw the text on
   * @param touchBounds
   *          the bounding rectangle for touch detection (can be
   *          <code>null</code> for static texts); note that upper left corner
   *          of the bounding rectangle is <em>not</em> identical with the
   *          drawing position {@link pos}
   * @param font
   *          the font to render the text with
   * @param foreground
   *          foreground/background flag
   */
  public GText(String text, int x, int y, FontData fontData,
      boolean foreground)
  {    
    super(foreground);
    this.x = x;
    this.y = y;
    this.text = text;
    this.fontData = fontData;
  }
  
  public Point2D.Float getPos()
  {
    return new Point2D.Float(bounds.x, bounds.y);
  }

  @Deprecated
  public int getStyle() {
    return 0;
  }
  
  @Override
  public Area getArea()
  {
    return new Area(getBounds());
  }
  
  public Rectangle getBounds() {
    if (bounds == null)
    {
      Font font = new Font(Display.getDefault(), fontData);
      Rectangle bnds = LCARS.getTextBounds(font, text);
      bounds = new Rectangle(x,y+descent,bnds.width,bnds.height);
      font.dispose();
    }
    return new Rectangle(bounds);
    
  }
  
  public String getText()
  {
    return this.text;
  }

  public int getDescent()
  {
    return this.descent;
  }

  public void setDescent(int ascent)
  {
    this.descent = ascent;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.tucottbus.kt.lcars.j2d.EGeometry2D#paint2D(java.awt.Graphics2D)
   */
  @Override
  public void paint2D(GC gc)
  {    
    if(tl == null) {
      Font font = new Font(gc.getDevice(), fontData);      
      tl = LCARS.getTextLayout(font, text);
      //TODO: font dispose in this::finalize()?
    }
    tl.draw(gc, x, y+descent, 0, text.length()-1, gc.getBackground(), gc.getForeground());
    
    if (LCARS.SCREEN_DEBUG)
    {
      Color red = gc.getDevice().getSystemColor(SWT.COLOR_RED);
      gc.setBackground(red);
      gc.setForeground(red);
      gc.drawRectangle(SWTUtils.toSwtRectangle(getBounds()));
      red.dispose();
    }
  }
    
  @Override
  protected void finalize() throws Throwable
  {
    try
    {
      tl.dispose();
    } catch (Exception e)
    {
      // ignored
    }
          
    super.finalize();
  }
  
}

// EOF
