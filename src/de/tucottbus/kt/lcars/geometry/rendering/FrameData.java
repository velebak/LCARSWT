package de.tucottbus.kt.lcars.geometry.rendering;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.BiFunction;

import de.tucottbus.kt.lcars.PanelData;
import de.tucottbus.kt.lcars.PanelState;
import de.tucottbus.kt.lcars.elements.ElementData;
import de.tucottbus.kt.lcars.elements.ElementState;
import de.tucottbus.kt.lcars.logging.Log;
import de.tucottbus.kt.lcars.swt.ImageMeta;
import de.tucottbus.kt.lcars.util.Objectt;

/**
 * Represents a context containing all data required to render a frame.
 * Instances of it can be applied each other to realize incremental and
 * selective updates.
 * 
 * @author Christian Borck
 *
 */
class FrameData
{
  public static final String CLASSKEY = "FrmDt";
  
  private boolean selectiveRepaint;
  private boolean incremental;
  private PanelState panelState;
  private ArrayList<ElementData> elements;
  private ArrayList<ElementData> elementsToPaint;
  private Shape dirtyArea;
  private boolean fullRepaint;
  private boolean bgChanged = true;

  private FrameData(PanelData panelData, boolean incremental,
      boolean selectiveRepaint)
  {
    this.incremental = incremental;
    this.selectiveRepaint = selectiveRepaint;
    this.panelState = panelData.panelState;
    this.elements = panelData.elementData;
  }

  /**
   * Creates frame context.
   * 
   * @param panelData
   * @param incremental
   * @return
   */
  public static FrameData create(PanelData panelData, boolean incremental,
      boolean selectiveRepaint)
  {
    return panelData == null || panelData.panelState == null
        || panelData.elementData == null ? null : new FrameData(panelData,
        incremental, selectiveRepaint);
  }

  /**
   * Called to update the background image of the screen.
   * 
   * @param pred
   *          - previous context
   * @return has background changed since the previous context
   */
  private boolean updateBgImage(FrameData pred)
  {
    ImageMeta thisRes = panelState.bgImage;

    if (pred != null && Objectt.equals(thisRes, pred.panelState.bgImage))
      return false;

    if (thisRes == null)
      return true;

    // Dimension d = updateData.panelState.dimension;
    // bgImg.getScaledInstance(d.width,d.height,Image.SCALE_DEFAULT);
    return true;
  }

  /**
   * Applies changes from previous to this context. See
   * {@link #apply(FrameData, BiFunction)} method.
   * 
   * @param pred
   */
  public void apply(FrameData pred)
  {
    apply(pred, incremental ? ((edu, edp) -> edu.applyUpdate(edp, false))
        : ((edu, __) -> ElementData.FLAG_MASK | ElementState.FLAG_MASK));
  }

  /**
   * Applies missing data from the previous frame context and calculates changes
   * from previous to this context.
   * 
   * @param pred
   *          - previous context
   * @param applyUpdate
   *          - Function class that applies missing data and return the changes
   *          flag. The signature is similar to
   *          "Integer applyUpdate(ElementData this, ElementData pred);"
   */
  private void apply(FrameData pred,
      BiFunction<ElementData, ElementData, Integer> applyUpdate)
  {
    if (pred == null)
    {
      dirtyArea = new Rectangle(getRenderWidth(), getRenderHeight());
      elementsToPaint = elements;
      bgChanged = updateBgImage(null);
      return;
    }
    
    bgChanged = updateBgImage(pred);
    
    this.fullRepaint = !panelState.equals(pred.panelState)
                    || !incremental
                    || !selectiveRepaint;
    //fullRepaint = true;

    int elCount = elements.size();

    // 1. Create a hash map of the current ElementData
    HashMap<Long, ElementData> hPred = createHashMap(pred.elements);

    for (ElementData edp : pred.elements)
      hPred.put(edp.serialNo, edp);

    // 2. Complete the received ElementData with the present information
    //
    if (this.fullRepaint)
    {
      elementsToPaint = elements;
      for (ElementData edu : elements)
        try
        {
          ElementData edp = hPred.get(edu.serialNo);
          if (edp != null)
            applyUpdate.apply(edu, edp);
        } catch (Exception e)
        {
          Log.err("Update failed on element #" + edu.serialNo + ": "
                  + e.getMessage());
        }
      dirtyArea = new Area(new Rectangle(getRenderWidth(), getRenderHeight()));
    } else
    {
      ArrayList<ElementData> elementsToPaint = new ArrayList<ElementData>(elCount);
      Vector<ElementData> elsWithoutChanges = new Vector<ElementData>(elCount);
      Area dirtyArea = new Area();

      for (ElementData edu : elements)
        try
        {
          ElementData edp = hPred.get(edu.serialNo);
          if (edp != null)
          {
            if (applyUpdate.apply(edu, edp) == 0)
            {
              elsWithoutChanges.addElement(edu);
              continue;
            }
            hPred.remove(edp);
            dirtyArea.add(new Area(edp.getBounds()));
          }

          dirtyArea.add(new Area(edu.getBounds()));
          elementsToPaint.add(edu);
        } catch (Exception e)
        {
          Log.err("Update failed on element #" + edu.serialNo + ": "
                  + e.getMessage());
        }
      // Add removed elements to the dirtyArea
      try
      {
        hPred
            .forEach((serialNo, edp) -> dirtyArea.add(new Area(edp.getBounds())));
      } catch (Exception e)
      {
        e.printStackTrace();
      }
      dirtyArea.intersect(new Area(new Rectangle(getRenderWidth(), getRenderHeight())));
      this.dirtyArea = dirtyArea;

      for (ElementData edu : elsWithoutChanges)
        if (dirtyArea.intersects(edu.getBounds()))
          elementsToPaint.add(edu);
      this.elementsToPaint = elementsToPaint;
    }
    
//    if(elements.size() !=elementsToPaint.size())
//      Log.warn(CLASSKEY, "Element difference " + elements.size() + ":" + elementsToPaint.size());
  }

  /**
   * Collapse this and all previous frames as they had added as one frame.
   * 
   * @param preds
   * @return Indicates if all elements has complete data
   */
  public boolean collapse(FrameData[] preds)
  {
    // TODO: bounded buffer testen
    // TODO:

    if (!incremental) // not if background changed because incremental apply
      return true;

    HashMap<Long, ElementData> hThis = createHashMapOnlyIncomplete(elements);
    if (hThis.isEmpty())
      return true;

    for (int i = preds.length - 1; i >= 0; i--)
      for (ElementData predEl : preds[i].elements)
      {
        ElementData thisEl = hThis.get(predEl.serialNo);
        if (thisEl == null) continue;
        thisEl.applyUpdate(predEl, false);
        if (thisEl.getMissing() == 0)
        {
          hThis.remove(thisEl.serialNo);
          if (hThis.isEmpty())
            return true;
        }
      }
    return hThis.isEmpty();
  }

  /**
   * 
   * @param elements
   * @return
   */
  private static HashMap<Long, ElementData> createHashMap(
      ArrayList<ElementData> elements)
  {
    HashMap<Long, ElementData> result = new HashMap<Long, ElementData>(
        elements.size());
    for (ElementData edp : elements)
      result.put(edp.serialNo, edp);
    return result;
  }

  /**
   * 
   * @param elements
   * @return
   */
  private static HashMap<Long, ElementData> createHashMapOnlyIncomplete(
      ArrayList<ElementData> elements)
  {
    HashMap<Long, ElementData> result = new HashMap<Long, ElementData>(
        elements.size());
    for (ElementData edu : elements)
      if (edu.getMissing() != 0)
        result.put(edu.serialNo, edu);
    return result;
  }

  public Shape getDirtyArea()
  {
    return dirtyArea;
  }

  public int getRenderWidth()
  {
    return panelState.width;
  }

  public int getRenderHeight()
  {
    return panelState.height;
  }
  
  public PanelState getPanelState()
  {
    return panelState;
  }

  public ArrayList<ElementData> getElementsToPaint()
  {
    return elementsToPaint;
  }

  public boolean isBgChanged()
  {
    return bgChanged;
  }
  
  public ImageMeta getBackgroundImage()
  {
    return panelState.bgImage;
  }
  
  public Boolean getFullRepaint() {
    return fullRepaint;
  }
}