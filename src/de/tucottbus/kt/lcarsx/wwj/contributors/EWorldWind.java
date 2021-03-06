package de.tucottbus.kt.lcarsx.wwj.contributors;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;

import com.jogamp.opengl.util.Animator;

import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.Screen;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.logging.Log;
import de.tucottbus.kt.lcarsx.wwj.orbits.Orbit;
import de.tucottbus.kt.lcarsx.wwj.places.Camera;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.animation.AnimationSupport;
import gov.nasa.worldwind.animation.BasicAnimator;
import gov.nasa.worldwind.awt.ViewInputHandler;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.FlyToOrbitViewAnimator;

/**
 * <p><i><b style="color:red">Experimental.</b></i></p>
 * 
 * Wraps a {@link gov.nasa.worldwind.awt.WorldWindowGLCanvas
 * WorldWindowGLCanvas} into an an {@link ElementContributor}.
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class EWorldWind extends ElementContributor implements RenderingListener
{  
  /**
   * AWT panel wrapping the WorldWind canvas.
   */
  java.awt.Panel awtPanelWwd;
  
  /**
   * The World Wind canvas.
   */
  private WorldWindowGLCanvas wwd;
  
  /**
   * The globe animator.
   */
  private Animator animator;
  
  /**
   * The orbit being followed.
   */
  private Orbit orbit;
  
  /**
   * The initial model on lazy initialization.
   */
  private Model initialModel = new BasicModel();
  
  /**
   * The initial view on lazy initialization.
   */
  private View initialView = getStdView();
  
  /**
   * The bounds of this World Wind wrapper in LCARS panel coordinates.
   */
  private Rectangle bounds;

  /**
   * The style of this World Wind wrapper. <em>-- Reserved, must be {@link LCARS#ES_NONE} --</em>
   */
  @SuppressWarnings("unused")
  private int style;

  // -- ElementContributor API --
  
  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Creates a World Wind contributor. 
   * 
   * @param x
   *          the x-coordinate of the top left corner (LCARS panel coordinates) 
   * @param y
   *          the y-coordinate of the top left corner (LCARS panel coordinates) 
   * @param w
   *          the width (LCARS panel coordinates)
   * @param h
   *          the height (LCARS panel coordinates)
   * @param style
   *          The style. <em>-- Reserved, must be {@link LCARS#ES_NONE} --</em>
   */
  public EWorldWind(int x, int y, int w, int h, int style)
  {
    super(x, y);
    this.bounds = new Rectangle(x,y,x+w,y+h);
    this.style  = style;
  }

  @Override
  public void addToPanel(Panel panel)
  {
    super.addToPanel(panel);
    
    try
    {
      Screen screen = Screen.getLocal(panel.getScreen());

      // NOTE: WorldWind needs to be embedded in an java.awt.Panel!
      if (awtPanelWwd==null)
      {
        awtPanelWwd = new java.awt.Panel(new java.awt.BorderLayout());
        awtPanelWwd.setBackground(Color.BLACK);        
      }
      awtPanelWwd.setVisible(false);

      if (wwd==null)
      {
        wwd = new WorldWindowGLCanvas();
        setModel(initialModel);
        setView(initialView);
        wwd.addRenderingListener(EWorldWind.this);
        awtPanelWwd.add(wwd, BorderLayout.CENTER);
  
        animator = new Animator();
        animator.add(wwd);
        animator.start();
      }

      LCARS.invokeLater(()->
      {
        screen.getSwtShell().getDisplay().syncExec(() ->
        {      
          screen.addAwtComponent(awtPanelWwd,bounds.x,bounds.y,bounds.width-bounds.x,bounds.height-bounds.y);
        });
        awtPanelWwd.setVisible(true);
      });
    }
    catch (ClassCastException e)
    {
      Log.err("LCARS: Function not supported on remote screens.", e);
    }
  }

  @Override
  public void removeFromPanel()
  {
    Panel panel = getPanel();
    if (panel==null) return;
    
    LCARS.getDisplay().syncExec(()->
    {
      try
      {
        Screen screen = Screen.getLocal(getPanel().getScreen());
        if (animator!=null)
        {
          animator.remove(wwd);
          animator.stop();
        }
        if (wwd!=null)
          screen.getSwtShell().getDisplay().syncExec(() ->
          {
            awtPanelWwd.remove(wwd);
            screen.removeAwtComponent(awtPanelWwd);
            wwd.destroy();
            wwd = null;
          });
      }
      catch (ClassCastException e)
      {
        Log.err("LCARS: 3D canvas wrappers not supported on remote screens.", e);
      }
    });

    super.removeFromPanel();
  }

  // -- World Wind API --
  
  public WorldWindowGLCanvas getWwd()
  {
    return wwd;
  }
  
  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Sets to {@linkplain Model model} to be displayed in this World Wind
   * Wrapper.
   * 
   * @param model
   *          The model, can be <code>null</code> which causes a basic earth
   *          model to be displayed.
   */
  public void setModel(Model model)
  {
    if (model==null)
      model = new BasicModel();
    if (wwd==null)
      initialModel = model;
    else
      wwd.setModel(model);
  }
  
  /**
   * TODO: ...
   */
  public Model getModel()
  {
    if (wwd==null) 
      return initialModel;
    else
      return wwd.getModel();
  }
  
  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   *
   * Sets the {@linkplain View view} of this World Wind Wrapper.
   * 
   * @param view
   *          The view, can be <code>null</code> which causes the standard view
   *          as obtained from {@link #getStdView()} to be displayed.
   */
  public void setView(View view)
  {
    if (view==null) view = getStdView();
    if (wwd==null)
      initialView = view;
    else
      wwd.setView(view);
  }
  
  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Returns the current {@linkplain View view} of this World Wind Wrapper.
   * 
   * @return The current view or <code>null</code> if the {@linkplain #wwd World
   *         Wind canvas} is not ready.
   */
  public View getView()
  {
    WorldWindowGLCanvas wwd = this.wwd;
    return wwd!=null ? wwd.getView() : null;
  }

  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Returns a new standard view.
   */
  public View getStdView()
  {
    return new BasicOrbitView();
  }

  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Returns the current eye position.
   */
  public Position getEyePosition()
  {
    View view = getView();
    return view != null ? view.getEyePosition() : null;
  }
  
  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Sets the current eye position.
   * 
   * @param eyePosition
   *          The position (<code>null</code> to set a default position).          
   *          Restrictions may apply if an orbit is being pursued.
   */
  public void setEyePosition(Position eyePosition)
  {
    View view;
    if (getOrbit()!=null)
      getOrbit().setEyePosition(eyePosition);
    else if ((view = getView())!=null)
      if (eyePosition!=null)
        view.setEyePosition(eyePosition);
      else
        setView(null);
  }

  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Returns the view heading.
   */
  public Angle getHeading()
  {
    View view = getView();
    return view != null ? view.getHeading() : null;
  }
  
  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Sets the view heading.
   * 
   * @param heading
   *          The heading angle (<code>null</code> to set a default position).          
   *          Restrictions may apply if an orbit is being pursued.
   */
  public void setHeading(Angle heading)
  {
    View view;
    if (getOrbit()!=null)
      getOrbit().setHeading(heading);
    else if ((view=getView())!=null)
      view.setHeading(heading!=null?heading:Angle.ZERO);
  }

  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Returns the view pitch.
   */
  public Angle getPitch()
  {
    View view = getView();
    return view != null ? view.getPitch() : null;
  }
  
  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   * 
   * Sets the view pitch.
   * 
   * @param pitch
   *          The pitch angle (<code>null</code> to set a default position).          
   *          Restrictions may apply if an orbit is being pursued.
   */
  public void setPitch(Angle pitch)
  {
    View view;
    if (getOrbit()!=null)
      getOrbit().setPitch(pitch);
    else if ((view=getView())!=null)
      view.setPitch(pitch!=null?pitch:Angle.ZERO);
  }
  
  // -- Orbiting and flying API --
  
  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   *
   * Starts or stops orbiting.
   * 
   * @param orbit
   *          The orbit to be followed or <code>null</code> to stop orbiting.
   */
  public void setOrbit(Orbit orbit)
  {
    if (orbit!=null)
      setView(orbit.getView());
    else if (this.orbit!=null)
    {
      View view = getStdView();
      try
      {
        view.copyViewState(this.orbit.getView());
      }
      catch (Exception e) 
      {
        // FIXME: Exception converting from fly views looking straight down
        try
        {
          Position eyePosition = this.orbit.getEyePosition();
          Position centerPosition = new Position(eyePosition.getLatitude(),eyePosition.getLongitude(),0);
          view.setOrientation(eyePosition, centerPosition);
        }
        catch (Exception e2)
        {
          e2.printStackTrace();
        }
      }
      setView(view);
    }
    if (wwd!=null) wwd.redrawNow();
    this.orbit = orbit;
  }

  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   *
   * Returns the currently pursued orbit.
   * 
   * @return The orbit or <code>null</code> if not orbiting.
   */
  public Orbit getOrbit()
  {
    return orbit;
  }
  
  /**
   * <p><i><b style="color:red">Experimental.</b></i></p>
   *
   * Flies to a KML look-at position.
   * 
   * @param lookAt
   *          The target.
   */
  public void flyTo(Camera lookAt) throws IllegalStateException
  {
    setOrbit(null);
    Position lookAtPosition = Position.fromDegrees(lookAt.getLatitude(), lookAt.getLongitude(), lookAt.getAltitude());
    BasicAnimator animator = null;
    long timeToMove = AnimationSupport.getScaledTimeMillisecs(
        getView().getEyePosition(), lookAtPosition, 4000, 16000);
  
    if (getView() instanceof BasicOrbitView)
    {
      BasicOrbitView orbitView = (BasicOrbitView)getView();
      animator = FlyToOrbitViewAnimator.createFlyToOrbitViewAnimator(orbitView,
          orbitView.getCenterPosition(), lookAtPosition, getView().getHeading(),
          Angle.fromDegrees(lookAt.getHeading()), getView().getPitch(), 
          Angle.fromDegrees(lookAt.getTilt()), orbitView.getZoom(), lookAt.getRange(),
          timeToMove, WorldWind.ABSOLUTE);
    }
    else
      throw new IllegalStateException("Not a BasicOrbitView or a BasicFlyView");
  
    ViewInputHandler inputHandler = getView().getViewInputHandler();
    inputHandler.stopAnimators();
    inputHandler.addAnimator(animator);
  }
  
  // -- Implementation of RenderingListener interface --
  
  @Override
  public void stageChanged(RenderingEvent event)
  {
    if (event.getStage().equals(RenderingEvent.BEFORE_RENDERING) &&
        orbit != null &&
        orbit.getView().getGlobe() != null)
      orbit.updateView();
  }
  
}
