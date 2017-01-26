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

import javax.swing.*;
import java.awt.*;
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

    private HashMap<Pair, Integer> mFrequencies;
    private HashMap<LatLong, Integer> mDots;
    private int mTotalPaths;

    private final float THRESHOLD = 0.8f;

    private static LatLong dot_generator(LatLong origin, double maxRadius) {
        double radii = Math.random() * maxRadius;
        double angle = Math.random() * Math.PI * 2;
        LatLong result = new LatLong(origin.latitude + radii * Math.sin(angle),
                                origin.longitude+ radii * Math.cos(angle) / 2);
        return result;
    }

    private int getHeatMapColor(float value) {
//        int NUM_COLORS = 4;
//        float color[][] = { {62, 89, 255}, {81, 207, 255}, {93, 255, 35}, {255, 14, 29} };
//        int idx1, idx2;
//        float fractBetween = 0;
//        if(value <= 0) {
//            idx1 = idx2 = 0;
//        } else if(value >= 1) {
//            idx1 = idx2 = NUM_COLORS - 1;
//        } else {
//            value = value * (NUM_COLORS-1);        // Will multiply value by 3.
//            idx1  = (int)floor(value);             // Our desired color will be after this index.
//            idx2  = idx1+1;                        // ... and before this index (inclusive).
//            fractBetween = value - idx1;           // Distance between the two indexes (0-1).
//        }
//        int r = (int)((color[idx2][0] - color[idx1][0])*fractBetween + color[idx1][0]) * 255;
//        int g = (int)((color[idx2][1] - color[idx1][1])*fractBetween + color[idx1][1]) * 255;
//        int b = (int)((color[idx2][2] - color[idx1][2])*fractBetween + color[idx1][2]) * 255;
//        return new java.awt.Color(r, g, b, 255).getRGB();
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
        FRAME.setTitle(windowTitle);
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
        mFrequencies = new HashMap<Pair, Integer>();
        mDots = new HashMap<LatLong, Integer>();
        mTotalPaths  = 0;
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
                mFrequencies.put(new Pair(prev, curt), 1);
                prev = curt;
            }
            mTotalPaths ++;
            mDots.put(new LatLong(path.getLat(0), path.getLon(0)), new java.awt.Color(0, 0, 0, 255).getRGB());
            mDots.put(new LatLong(path.getLat(path.size() - 1), path.getLon(path.size() - 1)), new java.awt.Color(0, 0, 0, 255).getRGB());
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
                    mFrequencies.put(pair, mFrequencies.get(pair) + 1);
                    num_overlap ++;
                } else {
                    mFrequencies.put(pair, 1);
                }
                prev = curt;
            }
            mTotalPaths ++;
            mDots.put(new LatLong(path.getLat(0), path.getLon(0)), getHeatMapColor((0.0f + num_overlap) / mFrequencies.size()));
            mDots.put(new LatLong(path.getLat(path.size() - 1), path.getLon(path.size() - 1)), getHeatMapColor((0.0f + num_overlap) / mFrequencies.size()));
        }
    }

    public void showUpdate() {
        // draw the dots
        for (LatLong key : mDots.keySet()) {
            createDot(key, mDots.get(key), 13.0f);
        }
        // draw the paths
        for (Pair segment : mFrequencies.keySet()) {
            float freq = mFrequencies.get(segment) / (mTotalPaths + 0.0f);
            System.out.println("freq = " + freq + "\ttotal paths = " + mTotalPaths);
            createPolyline(new ArrayList<LatLong>(Arrays.asList(segment.mDota, segment.mDotb)),
                            getHeatMapColor(freq), 6.0f);
        }
    }

    /////// Helper Functions ///////

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