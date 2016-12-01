/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Christian Pesch
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

import com.graphhopper.PathWrapper;
import com.graphhopper.ui.LayeredPanel;
import com.graphhopper.util.PointList;
import edu.princeton.cs.algs4.ST;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.util.JavaPreferences;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.omg.CORBA.CODESET_INCOMPATIBLE;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

public final class MapUI {
    private final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private final boolean SHOW_DEBUG_LAYERS = false;

    private final MapView MAP_VIEW;

    private final JFrame FRAME;

    private static PathWrapper mResult;
   
    private static final LatLong START = new LatLong(40.1093094, -88.2305774);
    private static final LatLong END   = new LatLong(41.9741032, -87.870702);

    private static LatLong dot_generator(LatLong origin, double maxRadius) {
        double radii = Math.random() * maxRadius;
        double angle = Math.random() * Math.PI * 2;
        LatLong result = new LatLong(origin.latitude + radii * Math.sin(angle),
                                origin.longitude+ radii * Math.cos(angle) / 2);
        return result;
    }

    /**
     * Starts the {@code Samples}.
     *
     * @param args command line args: expects the map files as multiple parameters.
     */
    /**
     * Download Data from http://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/
     * And pass in the data location as parameter
     */
    public static void main(String[] args) {

        RoutingTest routingTest = new RoutingTest(args[1]);

        //RoutingRunnable routingRunnable = new RoutingRunnable(
        //        (long)0,
        //        new GeoPoint(START),
        //        new GeoPoint(END),
        //        new Trackable<RoutingRunnable>() {
        //            public void doneCallBack(RoutingRunnable object) {
        //                mResult = object.getOutput();
        //            }

        //            public void startCallBack(RoutingRunnable object) {

        //            }
        //       },
        //       routingTest
        //);

        MapUI ui = new MapUI(args[0]);

        ui.setVisible(true);

        mResult = routingTest.calcPath(new GeoPoint(START), new GeoPoint(END));
        System.out.println("finished computation, size = " + mResult.getPoints().size());
        PointList pointList = mResult.getPoints();
        ArrayList<LatLong> points = new ArrayList<LatLong>();
        for (int i = 0; i < pointList.size(); i++) {
            points.add(new LatLong(pointList.getLatitude(i), pointList.getLongitude(i)));
        }
        ui.createPolyline(points, Color.RED, 6.0f);
        ui.createDot(START, Color.BLUE, 12.0f);
        ui.createDot(END, Color.GREEN, 12.0f);

    }

    public MapUI (String mapFileLocation) {
        // Increase read buffer limit
        ReadBuffer.setMaximumBufferSize(6500000);

        // Multithreading rendering
        MapWorkerPool.NUMBER_OF_THREADS = 2;

        List<File> mapFiles = new ArrayList<File>();
        mapFiles.add(new File(mapFileLocation));
        MAP_VIEW = createMapView();
        final MapView mapView = MAP_VIEW;
        final BoundingBox boundingBox = addLayers(mapView, mapFiles);

        final PreferencesFacade preferencesFacade = new JavaPreferences(Preferences.userNodeForPackage(MapUI.class));

        FRAME = new JFrame();
        FRAME.setTitle("MapUI");
        FRAME.add(mapView);
        FRAME.pack();
        FRAME.setSize(new Dimension(800, 600));
        FRAME.setLocationRelativeTo(null);
        FRAME.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        FRAME.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mapView.getModel().save(preferencesFacade);
                mapView.destroyAll();
                AwtGraphicFactory.clearResourceMemoryCache();
                FRAME.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }

            @Override
            public void windowOpened(WindowEvent e) {
                final Model model = mapView.getModel();
                // model.init(preferencesFacade);
                byte zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox, model.displayModel.getTileSize());
                model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
            }
        });
    }

    public void setVisible(boolean visible) {
        FRAME.setVisible(visible);
    }

    public int createDot(LatLong coordinates, Color color, float strokeWidth) {
        ArrayList<LatLong> list = new ArrayList<LatLong>();
        list.add(coordinates);
        list.add(coordinates);
        return createPolyline(list, color, strokeWidth);
    }

    public int createPolyline(List<LatLong> coordinates, Color color, float strokeWidth) {
        Paint paintStroke = GRAPHIC_FACTORY.createPaint();
        paintStroke.setStyle(Style.STROKE);
        paintStroke.setColor(color);
        paintStroke.setStrokeWidth(strokeWidth);
        Polyline pl = new Polyline(paintStroke, GRAPHIC_FACTORY);
        pl.getLatLongs().addAll(coordinates);
        MAP_VIEW.getLayerManager().getLayers().add(pl);
        MAP_VIEW.getLayerManager().redrawLayers();
        return pl.hashCode();
    }

    private BoundingBox addLayers(MapView mapView, List<File> mapFiles) {
        Layers layers = mapView.getLayerManager().getLayers();

        // Raster
        /*mapView.getModel().displayModel.setFixedTileSize(256);
        TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
        TileDownloadLayer tileDownloadLayer = createTileDownloadLayer(createTileCache(256), mapView.getModel().mapViewPosition, tileSource);
        layers.add(tileDownloadLayer);
        tileDownloadLayer.start();
        BoundingBox boundingBox = new BoundingBox(LatLongUtils.LATITUDE_MIN, LatLongUtils.LONGITUDE_MIN, LatLongUtils.LATITUDE_MAX, LatLongUtils.LONGITUDE_MAX);
        mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
        mapView.setZoomLevelMax(tileSource.getZoomLevelMax());*/

        // Vector
        mapView.getModel().displayModel.setFixedTileSize(512);
        MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        for (File file : mapFiles) {
            mapDataStore.addMapDataStore(new MapFile(file), false, false);
        }
        TileRendererLayer tileRendererLayer = createTileRendererLayer(createTileCache(64), mapDataStore, mapView.getModel().mapViewPosition);
        layers.add(tileRendererLayer);
        BoundingBox boundingBox = mapDataStore.boundingBox();

        // Debug
        if (SHOW_DEBUG_LAYERS) {
            layers.add(new TileGridLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
            layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
        }

        return boundingBox;
    }

    private MapView createMapView() {
        MapView mapView = new MapView();
        mapView.getMapScaleBar().setVisible(true);
        if (SHOW_DEBUG_LAYERS) {
            mapView.getFpsCounter().setVisible(true);
        }

        return mapView;
    }

    private TileCache createTileCache(int capacity) {
        TileCache firstLevelTileCache = new InMemoryTileCache(capacity);
        File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge");
        TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }

    private TileRendererLayer createTileRendererLayer(TileCache tileCache, MapDataStore mapDataStore, MapViewPosition mapViewPosition) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, mapViewPosition, false, false, GRAPHIC_FACTORY) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                System.out.println("Tap on: " + tapLatLong);
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
        return tileRendererLayer;
    }
}