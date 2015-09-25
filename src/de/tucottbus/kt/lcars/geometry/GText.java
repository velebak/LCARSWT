package de.tucottbus.kt.lcars.geometry;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.TextLayout;

import de.tucottbus.kt.lcars.LCARS;

/**
 * A geometry representing a text.
 * 
 * @author Matthias Wolff, Christian Borck
 */
public class GText extends AGeometry
{
  private static final long serialVersionUID = -1724627622898883028L;
  
  protected String text;
  protected int descent;
  protected int indent;
  protected FontData fontData;
  
  protected int x;
  protected int y;
  protected int width;
  protected int height;
    
  protected transient TextLayout tl;
  protected transient Font font;
  
  /**
   * Creates a new text geometry. A text geometry provides information and
   * 2D-rendering for a single line of text
   * 
   * @param text
   *          the text
   * @param pos
   *          the position to draw the text on
   * @param bounds
   *          the bounding rectangle for touch detection (can be
   *          <code>null</code> for static texts); note that upper left corner
   *          of the bounding rectangle is <em>not</em> identical with the
   *          drawing position {@link pos}
   * @param font
   *          the font to render the text with
   * @param foreground
   *          foreground/background flag
   */
  public GText(String text, Rectangle bounds, FontData fontData,
      boolean foreground)
  {    
    super(foreground);
    x = bounds.x;
    y = bounds.y;
    width = bounds.width;
    height = bounds.height;
    this.text = text;
    this.fontData = fontData;
  }
  
  public Point2D.Float getPos()
  {
    return new Point2D.Float(x, y);
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
    return new Rectangle(x,y,width,height);    
  }
  
  public String getText()
  {
    return this.text;
  }

  public int getDescent()
  {
    return this.descent;
  }

  
  public void setDescent(int descent)
  {
    this.descent = descent;
  }

  public int getIndent() {
    return indent;
  }
  
  public void setIndent(int indent) {
    this.indent = indent;
  }
  /*
   * (non-Javadoc)
   * 
   * @see de.tucottbus.kt.lcars.j2d.EGeometry2D#paint2D(java.awt.Graphics2D)
   */
  @Override
  public void paint2D(GC gc)
  {    
    if(tl == null)
      tl = LCARS.getTextLayout(font = new Font(gc.getDevice(), fontData), text);
    
    org.eclipse.swt.graphics.Rectangle clip = gc.getClipping();
    gc.setClipping(new org.eclipse.swt.graphics.Rectangle(x, y, width, height));
    tl.draw(gc, x+indent, y+descent, 0, text.length()-1, gc.getBackground(), gc.getForeground());
    gc.setClipping(clip);
    
    if (LCARS.SCREEN_DEBUG)
    {      
      Color red = gc.getDevice().getSystemColor(SWT.COLOR_RED);
      gc.setForeground(red);
      Rectangle b = getBounds();
      gc.drawRectangle(b.x, b.y, b.width-1, b.height-1);
      red.dispose();
    }
  }
    
  @Override
  protected void finalize() throws Throwable
  {    
    if (tl != null && !tl.isDisposed())
      tl.dispose();
    if (font != null && !font.isDisposed())
      font.dispose();
    super.finalize();
  }
  
  @Override
  public String toString() {
    return getClass().getSimpleName() + " text=\"" + text+"\" bounds=("+x+","+y+","+width+","+height+") fontsize=" + fontData.height + " font=\"" + fontData.getName() + "\"";
  }
  
}

// EOF
