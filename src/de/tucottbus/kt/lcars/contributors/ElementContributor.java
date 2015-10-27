package de.tucottbus.kt.lcars.contributors;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListener;

/**
 * Contributes elements to an LCARS {@link Panel}.
 * 
 * <p><b>TODO:</b><br>Rename to <code>ElementGroup</code>!</p>
 * 
 * @author Matthias Wolff
 */
public abstract class ElementContributor implements EEventListener
{
  protected Panel                            panel;
  private   final ArrayList<EElement>        elements;
  protected final int                        x;
  protected final int                        y;
  private   final Vector<EEventListener>     listeners;
  private   Timer                            timer;
  private   final HashMap<String, TimerTask> timerTasks;
  
  // -- Constructors --
  
  public ElementContributor(int x, int y)
  {
    elements   = new ArrayList<EElement>();
    this.x     = x;
    this.y     = y;
    listeners  = new Vector<EEventListener>();
    timerTasks = new HashMap<String,TimerTask>();
  }

  // -- Element management --
  
  protected <T extends EElement> T add(T el, boolean reposition)
  {
    if (el==null) return null;
    if (reposition)
    {
      Rectangle bounds = el.getBounds();
      bounds.x += this.x;
      bounds.y += this.y;
      el.setBounds(bounds);
    }
    
    synchronized (this.elements)
    {
      this.elements.remove(el);
      this.elements.add(el);      
      Panel panel = this.panel;
      if (panel!=null)
      {
        panel.add(el);
        el.setPanel(panel);
      }
    }
    return el;
  }
  
  protected <T extends EElement> T add(T el)
  {
    return add(el,true);
  }

  private void doReposition(Collection<EElement> elements)
  {
    int x = this.x;
    int y = this.y;
    for (EElement el : elements)
    {
      Rectangle bounds = el.getBounds();
      bounds.x += x;
      bounds.y += y;
      el.setBounds(bounds);
    }
  }
  
  private void doAddAll(Collection<EElement> elements)
  {
    this.elements.removeAll(elements);
    this.elements.addAll(elements);

    Panel panel = this.panel;
    if (panel == null) return;
    panel.addAll(elements);
    for (EElement el : elements)
      el.setPanel(panel);
    
  }
  
  protected void addAll(Collection<EElement> elements, boolean reposition)
  {
    if (elements==null) return;
    
    if (reposition)
      doReposition(elements);
    
    synchronized (this.elements)
    {
      doAddAll(elements);
    }
  }
  
  protected void addAll(Collection<EElement> elements)
  {
    addAll(elements, true);
  }

  protected void remove(EElement el)
  {
    if (el==null) return;
    synchronized (this.elements)
    {
      this.elements.remove(el);
      if (this.panel!=null) this.panel.remove(el);
    }
  }
  
  protected void remove(int i)
  {
    EElement el;
    synchronized (this.elements)
    {
      el = this.elements.remove(i);
      if (this.panel!=null) this.panel.remove(el);
    }
  }
  
  private void doRemoveAll(Collection<EElement> elements)
  {
    this.elements.removeAll(elements);
    Panel panel = this.panel;
    if (panel!=null)
      panel.removeAll(elements);
    elements.clear();
  }
  
  protected void removeAll(Collection<EElement> elements)
  {
    synchronized (this.elements)
    {
      doRemoveAll(elements);
    }
  }
  
  protected void removeAll()
  {
    synchronized (this.elements)
    {
      Panel panel = this.panel;
      if (panel!=null)
        panel.removeAll(this.elements);
      this.elements.clear();
    }
  }
  
  /**
   * Removes those elements from this {@link ElementConstributor} and from the panel where the filter returns true.
   * @param filter - mapping of EElement to boolean (see {@link Predicate})
   */
  protected void removeIf(Predicate<EElement> filter)
  {
    synchronized (this.elements)
    {
      Panel panel = this.panel;
      Iterator<EElement> it = this.elements.iterator();
      
      if (panel != null)
        while(it.hasNext())
        {
          EElement el = it.next();
          if (!filter.test(el)) return;
          it.remove();          
          panel.remove(el);
        }
      else
        while(it.hasNext())
        {
          EElement el = it.next();
          if (!filter.test(el)) return;
          it.remove();
        }
    }
  }
  
  protected void removeAllAndAddAll(Collection<EElement> remove, Collection<EElement> add, boolean repositionAdd)
  {
    if(repositionAdd)
      doReposition(add);
    
    synchronized (this.elements)
    {
      doRemoveAll(remove);
      doAddAll(add);
    }
  }
  
  protected void removeAllAndAddAll(Collection<EElement> remove, Collection<EElement> add)
  {
    removeAllAndAddAll(remove, add, true);
  }
  
  protected int size()
  {
    synchronized (this.elements)
    {
      return this.elements.size();
    }
  }
  
  protected EElement getElement(int i) 
  {
    synchronized (this.elements)
    {
      return this.elements.get(i);
    }
  }
  
  protected ArrayList<EElement> createElementList()
  {
    synchronized (this.elements)
    {
      return new ArrayList<>(this.elements);
    }
  }
  
  /**
   * Iterates over the elements list in the given bounds
   * @param fromInclusive - lower bound, index is included
   * @param toExclusive - higher bound, index is excluded
   * @param action - method with the parameters (int index, EElement element)
   */
  public void forAllElements(int fromInclusive, int toExclusive, Consumer<EElement> action)
  {
    synchronized (this.elements)
    {
      if (fromInclusive < 0 || fromInclusive >= toExclusive || toExclusive > this.elements.size())
        throw new IllegalArgumentException("iteration limits out of bounds.");
      for(;fromInclusive < toExclusive; fromInclusive++)
        action.accept(this.elements.get(fromInclusive));
    }
  }
  
  /**
   * Iterates over the elements list in the given bounds
   * @param fromInclusive - lower bound, index is included
   * @param pradicate - method with the parameters (EElement element) and boolean as return type to break loop if true
   */
  public void forAllElements(int fromInclusive, Function<EElement, Boolean> action)
  {
    synchronized (this.elements)
    {
      int toExclusive = this.elements.size();
      if (fromInclusive >= toExclusive) return;
      if (fromInclusive < 0)
        throw new IllegalArgumentException("iteration limits out of bounds.");
      for(; fromInclusive < toExclusive; fromInclusive++)
        if(action.apply(this.elements.get(fromInclusive)))
          break;
    }
  }
  
  /**
   * Iterates over the elements list in the given bounds
   * @param fromInclusive - lower bound, index is included
   * @param toExclusive - higher bound, index is excluded
   * @param action - method with the parameters (EElement element)
   */
  public void forAllElements(Consumer<EElement> action)
  {
    synchronized (this.elements)
    {
      for(EElement el : this.elements)
        action.accept(el);
    }
  }
  
  /**
   * Iterates over the elements list in the given bounds
   * @param fromInclusive - lower bound, index is included
   * @param toExclusive - higher bound, index is excluded
   * @param action - method with the parameters (int index, EElement element)
   */
  public void forAllElements(BiConsumer<Integer, EElement> action)
  {
    synchronized (this.elements)
    {
      Integer i = 0;
      for(EElement el : this.elements)
        action.accept(i++, el);
    }
  }
  
  /**
   * Return true if the element list is empty, otherwise false.
   * @return
   */
  public final boolean isEmpty()
  {
    return this.elements.isEmpty();
  }
  
  public void addToPanel(Panel panel)
  {
    if (panel==null) return;
    synchronized (this.elements)
    {
      if (this.panel==panel) return;
      this.panel = panel;
      panel.addAll(this.elements);
      this.elements.forEach((el) -> {
        el.setPanel(this.panel);
      });
      this.panel.invalidate();
    }
  }
  
  public void removeFromPanel()
  {
    synchronized (this.elements)
    {
      Panel panel = this.panel;
      if (panel==null) return;
      this.panel = null;

      for (EElement el : this.elements)
        el.setPanel(null);
      panel.removeAll(this.elements);
      panel.invalidate();
    }
  }

  public boolean isDisplayed()
  {
    return this.panel!=null;
  }

  // -- EEvent handling --
  
  public void addEEventListener(EEventListener listener)
  {
    this.listeners.add(listener);
  }

  public void removeEEventListener(EEventListener listener)
  {
    this.listeners.remove(listener);
  }

  public void fireEEvent(EEvent ee)
  {
    // Dispatch event
    for (int i=0; i<listeners.size(); i++)
      switch (ee.id)
      {
      case EEvent.TOUCH_DOWN: listeners.get(i).touchDown(ee); break;
      case EEvent.TOUCH_UP  : listeners.get(i).touchUp  (ee); break;
      case EEvent.TOUCH_DRAG: listeners.get(i).touchDrag(ee); break;
      case EEvent.TOUCH_HOLD: listeners.get(i).touchHold(ee); break;
      }
  }

  public void touchDown(EEvent ee)
  {
    fireEEvent(ee);
  }

  public void touchDrag(EEvent ee)
  {
    fireEEvent(ee);
  }

  public void touchHold(EEvent ee)
  {
    fireEEvent(ee);
  }

  public void touchUp(EEvent ee)
  {
    fireEEvent(ee);
  }

  // -- Timers --
  
  public void scheduleTimerTask(TimerTask task, String name, long firstTime, long period)
  {
    synchronized (timerTasks)
    {
      if (timer==null) timer = new Timer(true);
      cancelTimerTask(name);
      if (period>0)
        timer.schedule(task,firstTime,period);
      else
        timer.schedule(task,firstTime);
      timerTasks.put(name,task);      
    }
  }
  
  public void cancelTimerTask(String name)
  {
    synchronized (timerTasks)
    {
      cancelTimerTask(timerTasks.get(name));
    }
  }
  
  public void cancelTimerTask(TimerTask task)
  {
    if (task==null) return;
    task.cancel();
    
    synchronized (timerTasks)
    {
      for (Iterator<TimerTask> i = timerTasks.values().iterator(); i.hasNext();)
      {
        TimerTask tt = i.next();
        if (tt.equals(task)) i.remove();
      }
      if (timer!=null) timer.purge();      
    }
  }
  
  public void cancelAllTimerTasks()
  {
    synchronized (timerTasks)
    {
      for (Iterator<TimerTask> i = timerTasks.values().iterator(); i.hasNext();)
      {
        TimerTask tt = i.next();
        tt.cancel();
        i.remove();
      }
      if (timer!=null) timer.purge();      
    }
  }
}
