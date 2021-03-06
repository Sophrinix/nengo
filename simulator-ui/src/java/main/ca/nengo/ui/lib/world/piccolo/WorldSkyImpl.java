package ca.nengo.ui.lib.world.piccolo;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.UnsupportedOperationException;

import ca.nengo.ui.lib.Style.NengoStyle;
import ca.nengo.ui.lib.world.WorldSky;
import ca.nengo.ui.lib.world.handlers.KeyboardFocusHandler;
import ca.nengo.ui.lib.world.handlers.ScrollZoomHandler;
import ca.nengo.ui.lib.world.piccolo.primitives.PXCamera;
import ca.nengo.ui.lib.world.piccolo.primitives.PXEdge;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.event.PZoomEventHandler;

/**
 * A layer within a world which looks at the ground layer. This layer can also
 * contain world objects, but their positions are static during panning and
 * zooming.
 * 
 * @author Shu Wu
 */
public class WorldSkyImpl extends WorldLayerImpl implements WorldSky {

	private PXCamera myCamera;

	/**
	 * Create a new sky layer
	 * 
	 * @param world
	 *            World this layer belongs to
	 */
	public WorldSkyImpl() {
		super("Sky", new PXCamera());

		myCamera = (PXCamera) getPiccolo();
		myCamera.setPaint(NengoStyle.COLOR_BACKGROUND);

		/*
		 * Attach handlers
		 */
		PZoomEventHandler zoomHandler = new PZoomEventHandler();
		zoomHandler.setMinDragStartDistance(20);
		zoomHandler.setMinScale(MIN_ZOOM_SCALE);
		zoomHandler.setMaxScale(MAX_ZOOM_SCALE);

		myCamera.addInputEventListener(zoomHandler);

		myCamera.addInputEventListener(new KeyboardFocusHandler());

		myCamera.addInputEventListener(new ScrollZoomHandler());
	}

	public void addLayer(PLayer layer) {
		myCamera.addLayer(layer);
	}

	public void setWorld(WorldImpl world) {
		this.world = world;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.IWorldLayer#addEdge(ca.shu.ui.lib.objects.DirectedEdge)
	 */
	public void addEdge(PXEdge edge) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.IWorldLayer#getWorld()
	 */
	public WorldImpl getWorld() {
		return world;
	}

	public Point2D localToView(Point2D localPoint) {
		return myCamera.localToView(localPoint);
	}

	public Rectangle2D localToView(Rectangle2D arg0) {
		return myCamera.localToView(arg0);
	}

	public Point2D viewToLocal(Point2D arg0) {
		return myCamera.viewToLocal(arg0);
	}

	public Rectangle2D viewToLocal(Rectangle2D arg0) {
		return myCamera.viewToLocal(arg0);
	}

	public double getViewScale() {
		return myCamera.getViewScale();
	}

	public PCamera getCamera() {
		return myCamera;
	}

	public void animateViewToCenterBounds(Rectangle2D centerBounds, boolean shouldScaleToFit,
			long duration) {
		myCamera.animateViewToCenterBounds(centerBounds, shouldScaleToFit, duration);
	}

	public void setViewScale(double scale) {
		myCamera.setViewScale(scale);
	}

	public Rectangle2D getViewBounds() {
		return myCamera.getViewBounds();
	}

	public void setViewBounds(Rectangle2D centerBounds) {
		myCamera.setViewBounds(centerBounds);

	}

}
