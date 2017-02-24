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
import com.graphhopper.util.PointList;
import edu.princeton.cs.algs4.In;
import org.mapsforge.core.graphics.*;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.util.JavaPreferences;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.util.MapViewProjection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;

import static java.lang.Math.floor;

public final class MapUI {
    private final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private final boolean SHOW_DEBUG_LAYERS = false;

    private final MapView MAP_VIEW;

    private final JFrame FRAME;

    private final JLayeredPane PANE;

    private HashMap<Pair, OverLapCounter> mFrequencies;
    private HashMap<LatLong, Integer> mDots;
    private int mTotalPaths;
    private int mMainPathLength;

    private final float THRESHOLD = 0.8f;

    public MapUI(String mapFileLocation, String windowTitle) {
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
        PANE  = new JLayeredPane();
//        PANE.add(MAP_VIEW, 0);

//        JLabel test = new JLabel("hello");

//        PANE.add(test, 1);

        FRAME.setTitle(windowTitle);
        FRAME.add(MAP_VIEW);
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
        mFrequencies = new HashMap<Pair, OverLapCounter>();
        mDots = new HashMap<LatLong, Integer>();
        mTotalPaths  = 0;
        mMainPathLength = 1;
    }

    public void setVisible(boolean visible) {
        FRAME.setVisible(visible);
    }

    public int createDot(LatLong coordinates, int color, float strokeWidth) {
        ArrayList<LatLong> list = new ArrayList<LatLong>();
        list.add(coordinates);
        list.add(coordinates);
        return createPolyline(list, color, strokeWidth);
    }

    public int createPolyline(List<LatLong> coordinates, int color, float strokeWidth) {
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

    public int createCircle(LatLong latLong, int color, float radius) {
        Paint paintStroke = GRAPHIC_FACTORY.createPaint();
        paintStroke.setStyle(Style.STROKE);
        paintStroke.setColor(color);
        paintStroke.setStrokeWidth(3.0f);
        Circle circle = new Circle(latLong, radius, null, paintStroke);
        MAP_VIEW.getLayerManager().getLayers().add(circle);
        MAP_VIEW.getLayerManager().redrawLayers();
        return 0;
    }

    public void setMainPath(PointList path) {
        if (path.size() > 0) {
            LatLong prev = new LatLong(path.getLat(0), path.getLon(0));
            LatLong curt;
            for (int i = 1; i < path.size(); i++) {
                curt = new LatLong(path.getLat(i), path.getLon(i));
                mFrequencies.put(new Pair(prev, curt), new OverLapCounter(true));
                prev = curt;
            }
            mTotalPaths ++;
            mDots.put(new LatLong(path.getLat(0), path.getLon(0)), new java.awt.Color(0, 0, 0, 255).getRGB());
            mDots.put(new LatLong(path.getLat(path.size() - 1), path.getLon(path.size() - 1)), new java.awt.Color(0, 0, 0, 255).getRGB());
            mMainPathLength = mFrequencies.size();
        }
    }

    public void addPath(PointList path) {
        if (path.size() > 0) {
            LatLong prev = new LatLong(path.getLat(0), path.getLon(0));
            LatLong curt;
            int num_overlap = 0;
            for (int i = 1; i < path.size(); i++) {
                curt = new LatLong(path.getLat(i), path.getLon(i));
                Pair pair = new Pair(prev, curt);
                if (mFrequencies.containsKey(pair)) {
                    mFrequencies.get(pair).mNumOverlap += 1;
                    if (mFrequencies.get(pair).mMain)
                        num_overlap ++;
                } else {
                    mFrequencies.put(pair, new OverLapCounter(false));
                }
                prev = curt;
            }
            mTotalPaths ++;
            mDots.put(new LatLong(path.getLat(0), path.getLon(0)), getHeatMapColor((0.0f + num_overlap) / mMainPathLength));
            mDots.put(new LatLong(path.getLat(path.size() - 1), path.getLon(path.size() - 1)), getHeatMapColor((0.0f + num_overlap) / mMainPathLength));
        }
    }

    public void showUpdate() {
        // draw the paths
        for (Pair segment : mFrequencies.keySet()) {
            float freq = mFrequencies.get(segment).mNumOverlap / (mTotalPaths + 0.0f);
            System.out.println("freq = " + freq + "\ttotal paths = " + mTotalPaths);
            createPolyline(new ArrayList<LatLong>(Arrays.asList(segment.mDota, segment.mDotb)),
                            getHeatMapColor(freq), 6.0f);
        }
        // draw the dots
        for (LatLong key : mDots.keySet()) {
            createDot(key, mDots.get(key), 13.0f);
        }
    }

    /////// Helper Functions ///////

    private int getHeatMapColor(float value) {
        int color[][] = {{6, 0, 133, 255}, {255, 255, 0}};
        int color2[][] = {{255, 255, 0}, {255, 14, 29, 255}};
        int r, g, b;
        if (value < THRESHOLD) {
            value *= 10.0f / (THRESHOLD * 10);
            r = (int)((color[1][0] - color[0][0]) * value + color[0][0]);
            g = (int)((color[1][1] - color[0][1]) * value + color[0][1]);
            b = (int)((color[1][2] - color[0][2]) * value + color[0][2]);
        } else {
            value -= THRESHOLD;
            value *= 10.0f / ((1 - THRESHOLD) * 10);
            r = (int)((color2[1][0] - color2[0][0]) * value + color2[0][0]);
            g = (int)((color2[1][1] - color2[0][1]) * value + color2[0][1]);
            b = (int)((color2[1][2] - color2[0][2]) * value + color2[0][2]);
        }
//        if (value < 0.25f) {
//            return new java.awt.Color(6, 0, 133, 255).getRGB();
//        } else if (value < 0.5f) {
//            return new java.awt.Color(93, 255, 35, 255).getRGB();
//        } else if (value < 0.75f) {
//            return new java.awt.Color(255, 255, 0, 255).getRGB();
//        } else {
//            return new java.awt.Color(255, 14, 29, 255).getRGB();
//        }
        return new java.awt.Color(r, g, b, 255).getRGB();
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

    private class OverLapCounter {
        int mNumOverlap;
        boolean mMain;

        OverLapCounter(boolean onMain) {
            mMain = onMain;
            mNumOverlap = 0;
        }
    }

    private class MouseEvent implements MouseListener {

        private MapViewProjection mReference;

        MouseEvent() {
            mReference = new MapViewProjection(MAP_VIEW);
        }

        public void mousePressed(java.awt.event.MouseEvent e) {

        }

        public void mouseReleased(java.awt.event.MouseEvent e) {

        }

        public void mouseEntered(java.awt.event.MouseEvent e) {

        }

        public void mouseExited(java.awt.event.MouseEvent e) {

        }

        public void mouseClicked(java.awt.event.MouseEvent e) {
            System.out.println("mouse clicked at: " + e.getX() + ", " + e.getY());
            LatLong location = mReference.fromPixels(e.getX(), e.getY());
            System.out.println("Geolocation clicked at: " + location.latitude + ", " + location.longitude);
        }
    }

    private class MyLineLayer extends Polyline {
        private HashMap<LatLong, Integer> mDots;
        private List<LatLong> mPath;
        private GraphicFactory mGraphicFactory;

        /**
         *
         * @param pathPaint Paint for the path. Not for dots
         * @param graphicFactory
         * @param dotWithColor
         * @param path
         */
        public MyLineLayer(Paint pathPaint, GraphicFactory graphicFactory, HashMap<LatLong, Integer> dotWithColor,
                           List<LatLong> path) {
            super(pathPaint, graphicFactory);
            mDots = dotWithColor;
            mGraphicFactory = graphicFactory;
            mPath = path;
            super.getLatLongs().addAll(mPath);
        }

        public MyLineLayer(GraphicFactory graphicFactory, HashMap<LatLong, Integer> dotWithColor, int pathcolor,
                           float pathstrokeWidth, List<LatLong> path) {
            super(null, graphicFactory);
            Paint paintStroke = GRAPHIC_FACTORY.createPaint();
            paintStroke.setStyle(Style.STROKE);
            paintStroke.setColor(pathcolor);
            paintStroke.setStrokeWidth(pathstrokeWidth);
            this.setPaintStroke(paintStroke);
            mDots = dotWithColor;
            mGraphicFactory = graphicFactory;
            mPath = path;
            super.getLatLongs().addAll(mPath);
        }
        @Override
        public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, org.mapsforge.core.graphics.Canvas canvas, Point topLeftPoint) {
            super.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
            long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
            /** Draw the points **/
            for (LatLong latLong : mDots.keySet()) {
                int pixelX = (int) (MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize) - topLeftPoint.x);
                int pixelY = (int) (MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize) - topLeftPoint.y);
                Paint paintStroke = GRAPHIC_FACTORY.createPaint();
                paintStroke.setStyle(Style.STROKE);
                paintStroke.setColor(mDots.get(latLong));
                paintStroke.setStrokeWidth(13.0f);
                canvas.drawCircle(pixelX, pixelY, 100, paintStroke);
            }
        }

        public boolean lookUp(LatLong point) {
            return false;
        }
    }
}